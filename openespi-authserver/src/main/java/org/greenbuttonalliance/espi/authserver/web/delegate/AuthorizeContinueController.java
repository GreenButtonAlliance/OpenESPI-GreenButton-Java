/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.authserver.web.delegate;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.authserver.grant.DelegatedConsentService;
import org.greenbuttonalliance.espi.authserver.grant.EspiGrantScopePolicy;
import org.greenbuttonalliance.espi.authserver.grant.GrantContext;
import org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore;
import org.greenbuttonalliance.espi.handoff.HandoffNonceService;
import org.greenbuttonalliance.espi.handoff.InvalidHandoffException;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Receives the customer's redirect from DC after they've authenticated and completed the
 * Authorization Screen. Verifies the signed return handoff, consumes the nonce, then either
 * resumes the OAuth2 flow (allow) or redirects to the third party with
 * {@code error=access_denied} per RFC 6749 &sect;4.1.2.1 (deny).
 *
 * <h2>Allow path</h2>
 * <ol>
 *   <li>Decode + verify the signed return handoff (signature, expiry, direction, nonce).</li>
 *   <li>Consume the nonce single-use (replay protection).</li>
 *   <li>Keep the delegation's {@link GrantContext} (client, customer, approved scope, selected UsagePoints) in
 *       the session under its correlation id, for {@link GrantContextSessionStore#TTL} (#305).</li>
 *   <li>Redirect to {@code /oauth2/authorize} with the original third-party request parameters
 *       (looked up from {@link DelegationStateService}) and the delegation's id
 *       ({@link GrantContextSessionStore#DELEGATION_PARAMETER}). On that request only, the customer is signed
 *       in ({@link DelegatedCustomerAuthenticationFilter}) and has consented to the approved scope
 *       ({@link DelegatedConsentService}); Spring AS mints the code and redirects to the third party's
 *       {@code redirect_uri}.</li>
 *   <li>Nothing is saved that another authorization could use: no AS sign-in in the session and no stored
 *       consent (#305; before, both were saved and cleared after the response, #299, #276). Each authorization
 *       in the browser is delegated to the DC.</li>
 * </ol>
 *
 * <h2>Deny path</h2>
 * <ol>
 *   <li>Decode + verify the signed return handoff.</li>
 *   <li>Consume the nonce.</li>
 *   <li>Look up the third party's {@code redirect_uri} and original {@code state} via
 *       {@link DelegationStateService}.</li>
 *   <li>Build a {@code {redirect_uri}?error=access_denied&state={state}} URL and 302 to it.</li>
 * </ol>
 *
 * <h2>Failure semantics</h2>
 * <p>Any {@link InvalidHandoffException} from the codec or nonce service surfaces as HTTP 400
 * via the local {@link ExceptionHandler}. The internal log captures the specific sub-cause;
 * the user-agent sees a uniform error page with no information leak (same security contract as
 * the DC-side Authorization Screen).</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthorizeContinueController {

	private static final String AUTHORIZE_PATH = "/oauth2/authorize";

	private final SignedHandoffCodec codec;
	private final HandoffNonceService nonceService;
	private final DelegationStateService delegationStateService;
	private final RegisteredClientRepository registeredClientRepository;
	private final GrantContextSessionStore grantContextSessionStore;
	private final OAuth2AuthorizationService authorizationService;

	@GetMapping("/oauth2/authorize/continue")
	public String continueAfterDelegation(@RequestParam("handoff") String handoffToken,
										  HttpServletRequest request) {
		SignedHandoff.Return payload = codec.decodeReturn(handoffToken);
		nonceService.consume(payload.nonce(), payload.expiresAt());

		DelegationStateService.Entry state = delegationStateService.consume(payload.correlationId());
		if (state == null) {
			throw new InvalidHandoffException("no delegation state for correlation_id " + payload.correlationId());
		}
		removePendingConsentAuthorization(state.pendingAuthorizationId());

		if (SignedHandoff.Return.CONSENT_ALLOW.equals(payload.consent())
				&& payload.approvedScope() != null && !payload.approvedScope().isBlank()) {
			return handleAllow(payload, state, request);
		}
		return handleDeny(state);
	}

	/**
	 * Removes the pending authorization Spring AS saved before its consent-page redirect (#299). The replay
	 * below creates its own, and the purge job never removes a row that holds only a state.
	 */
	private void removePendingConsentAuthorization(String pendingAuthorizationId) {
		if (pendingAuthorizationId == null) {
			return;
		}
		OAuth2Authorization pending = authorizationService.findById(pendingAuthorizationId);
		if (pending != null) {
			authorizationService.remove(pending);
		}
	}

	private String handleAllow(SignedHandoff.Return payload,
							   DelegationStateService.Entry state,
							   HttpServletRequest request) {
		RegisteredClient client = registeredClientRepository.findByClientId(state.clientId());
		if (client == null) {
			throw new InvalidHandoffException("registered client disappeared: " + state.clientId());
		}
		requireNarrowingOfRequest(payload.approvedScope(), state.requestedScope());
		Long retailCustomerId = parseRetailCustomerId(payload.principal());

		// #305: the delegation's own context, under its correlation id. It signs the customer in and is the
		// consent on the replay below only (a request with exactly these parameters), and
		// GrantContextEnrichingAuthorizationService stamps it onto the OAuth2Authorization Spring AS creates there.
		DelegationStateService.ProofKey proofKey = state.proofKey();
		grantContextSessionStore.put(request.getSession(true), new GrantContext(
				payload.correlationId(),
				client.getClientId(),
				client.getId(),
				payload.principal(),
				new GrantContext.Replay(state.redirectUri(), state.tpState(),
						proofKey != null ? proofKey.codeChallenge() : null,
						proofKey != null ? proofKey.codeChallengeMethod() : null),
				retailCustomerId,
				payload.approvedScope(),
				payload.selectedUsagePointIds(),
				Instant.now().plus(GrantContextSessionStore.TTL)));

		// Redirect back to /oauth2/authorize with the original TP query params and the delegation's id.
		UriComponentsBuilder replay = UriComponentsBuilder.fromPath(AUTHORIZE_PATH)
				.queryParam("response_type", "code")
				.queryParam("client_id", state.clientId())
				.queryParam("redirect_uri", state.redirectUri())
				.queryParam("scope", payload.approvedScope())
				.queryParam("state", state.tpState() != null ? state.tpState() : "")
				.queryParam(GrantContextSessionStore.DELEGATION_PARAMETER, payload.correlationId());
		// #274: the TP's PKCE challenge, so the code is minted with it and the TP's code_verifier is checked at
		// /oauth2/token. Without it Spring AS mints the code without a challenge and refuses the verifier.
		if (proofKey != null) {
			replay.queryParam(PkceParameterNames.CODE_CHALLENGE, proofKey.codeChallenge());
			if (proofKey.codeChallengeMethod() != null) {
				replay.queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, proofKey.codeChallengeMethod());
			}
		}
		String url = replay.encode(StandardCharsets.UTF_8).build().toUriString();

		log.info("Continue: allow path, client_id={}, customer={}, correlation_id={}",
				state.clientId(), payload.principal(), payload.correlationId());

		return "redirect:" + url;
	}

	private String handleDeny(DelegationStateService.Entry state) {
		if (state.redirectUri() == null || state.redirectUri().isBlank()) {
			throw new InvalidHandoffException("cannot deliver access_denied: no redirect_uri for client " + state.clientId());
		}

		String url = UriComponentsBuilder.fromUriString(state.redirectUri())
				.queryParam("error", "access_denied")
				.queryParam("error_description", "The customer did not authorize the request.")
				.queryParamIfPresent("state", state.tpState() != null && !state.tpState().isBlank()
						? java.util.Optional.of(state.tpState())
						: java.util.Optional.empty())
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();

		log.info("Continue: deny path, client_id={}, redirecting TP to {} with access_denied",
				state.clientId(), state.redirectUri());

		return "redirect:" + url;
	}

	/**
	 * Split the approved scope into Spring AS scope tokens using the same rule Spring AS applies to
	 * the replayed {@code scope} parameter: space-delimited per RFC 6749 &sect;3.3. An ESPI scope
	 * string such as {@code FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000}
	 * is a <em>single</em> scope token &mdash; its {@code ;} separates that scope's parameters, not
	 * scopes &mdash; so it must be kept whole. Splitting on {@code ;} made the consent set
	 * disjoint from the requested set, and Spring AS looped back to {@code /authorize/delegate}
	 * (#210).
	 */
	private static String[] parseScopes(String approvedScope) {
		return StringUtils.tokenizeToStringArray(approvedScope, " ");
	}

	/**
	 * The customer may narrow the scope the third party requested, never widen it (#211): every
	 * approved scope must be one of the requested scopes or an
	 * {@linkplain EspiGrantScopePolicy#isNarrowingOf ESPI narrowing} of one. A Return handoff that
	 * widens the request is a DC contract violation, rejected before any grant context is kept.
	 */
	private static void requireNarrowingOfRequest(String approvedScope, String requestedScope) {
		List<String> requested = List.of(parseScopes(requestedScope != null ? requestedScope : ""));
		for (String approved : parseScopes(approvedScope)) {
			boolean narrows = requested.contains(approved)
					|| requested.stream().anyMatch(r -> EspiGrantScopePolicy.isNarrowingOf(approved, r));
			if (!narrows) {
				throw new InvalidHandoffException("approved scope '" + approved
						+ "' is not a narrowing of the requested scope '" + requestedScope + "'");
			}
		}
	}

	/**
	 * Parse the Return handoff's opaque principal into the numeric customer id that DC's
	 * back-channel ({@code SubscriptionProvisionRequest.retailCustomerId: Long}) requires.
	 * A non-numeric principal indicates a contract violation between DC and AS — the DC sets
	 * {@code String.valueOf(customer.getId())} on the Return handoff. Reject loudly rather than
	 * silently substituting; the operator can see the cause in logs and fix the DC side.
	 */
	private static Long parseRetailCustomerId(String principal) {
		try {
			return Long.parseLong(principal);
		} catch (NumberFormatException e) {
			throw new InvalidHandoffException(
					"non-numeric principal '" + principal + "'; cannot map to retail_customer_id");
		}
	}

	@ExceptionHandler(InvalidHandoffException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleInvalidHandoff(InvalidHandoffException e, HttpServletRequest request) {
		log.warn("/oauth2/authorize/continue handoff rejected: cause='{}', remoteAddr={}, userAgent='{}'",
				e.getMessage(), request.getRemoteAddr(), request.getHeader("User-Agent"));
		return "error/400";
	}
}
