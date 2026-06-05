/*
 * Copyright 2025 Green Button Alliance, Inc.
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.authserver.grant.GrantContext;
import org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore;
import org.greenbuttonalliance.espi.handoff.HandoffNonceService;
import org.greenbuttonalliance.espi.handoff.InvalidHandoffException;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
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
 *   <li>Authenticate the customer in the AS session: write a {@link UsernamePasswordAuthenticationToken}
 *       to the SecurityContext and save to the HTTP session via
 *       {@link HttpSessionSecurityContextRepository}.</li>
 *   <li>Pre-populate {@link OAuth2AuthorizationConsentService} with the customer's effective
 *       approved scope so Spring Authorization Server's downstream consent check passes
 *       transparently.</li>
 *   <li>Redirect to {@code /oauth2/authorize} with the original third-party request parameters
 *       (looked up from {@link DelegationStateService}). Spring AS sees an authenticated user
 *       with pre-existing consent, mints the authorization code, and redirects to the third
 *       party's {@code redirect_uri}.</li>
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
	private final OAuth2AuthorizationConsentService consentService;
	private final RegisteredClientRepository registeredClientRepository;
	private final GrantContextSessionStore grantContextSessionStore;
	private final SecurityContextRepository securityContextRepository =
			new HttpSessionSecurityContextRepository();

	@GetMapping("/oauth2/authorize/continue")
	public String continueAfterDelegation(@RequestParam("handoff") String handoffToken,
										  HttpServletRequest request,
										  HttpServletResponse response) {
		SignedHandoff.Return payload = codec.decodeReturn(handoffToken);
		nonceService.consume(payload.nonce(), payload.expiresAt());

		DelegationStateService.Entry state = delegationStateService.consume(payload.correlationId());
		if (state == null) {
			throw new InvalidHandoffException("no delegation state for correlation_id " + payload.correlationId());
		}

		if (SignedHandoff.Return.CONSENT_ALLOW.equals(payload.consent())
				&& payload.approvedScope() != null && !payload.approvedScope().isBlank()) {
			return handleAllow(payload, state, request, response);
		}
		return handleDeny(state);
	}

	private String handleAllow(SignedHandoff.Return payload,
							   DelegationStateService.Entry state,
							   HttpServletRequest request,
							   HttpServletResponse response) {
		authenticateCustomer(payload.principal(), request, response);

		RegisteredClient client = registeredClientRepository.findByClientId(state.clientId());
		if (client == null) {
			throw new InvalidHandoffException("registered client disappeared: " + state.clientId());
		}

		// Pre-populate the consent store with the customer's effective approved scope so Spring
		// AS's consent check passes when /oauth2/authorize is replayed.
		OAuth2AuthorizationConsent.Builder consentBuilder = OAuth2AuthorizationConsent
				.withId(client.getId(), payload.principal());
		for (String fbTerm : parseScopes(payload.approvedScope())) {
			consentBuilder.scope(fbTerm);
		}
		consentService.save(consentBuilder.build());

		// Stash the grant context in the HTTP session so GrantContextEnrichingAuthorizationService
		// can stamp it onto the OAuth2Authorization Spring AS creates when /oauth2/authorize is
		// replayed below. Lifetime is the brief window between this redirect and Spring AS's
		// auth-code persistence; consumed single-use by the enricher.
		Long retailCustomerId = parseRetailCustomerId(payload.principal());
		HttpSession session = request.getSession(true);
		grantContextSessionStore.put(session, new GrantContext(
				payload.correlationId(),
				retailCustomerId,
				payload.approvedScope(),
				payload.selectedUsagePointIds()));

		// Redirect back to /oauth2/authorize with the original TP query params. Spring AS finds
		// the saved authorization request (still in session), sees authenticated user + matching
		// consent in the store, mints the code, redirects to the TP.
		String url = UriComponentsBuilder.fromPath(AUTHORIZE_PATH)
				.queryParam("response_type", "code")
				.queryParam("client_id", state.clientId())
				.queryParam("redirect_uri", state.redirectUri())
				.queryParam("scope", payload.approvedScope())
				.queryParam("state", state.tpState() != null ? state.tpState() : "")
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();

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
	 * Establish the AS-side {@link SecurityContext} from the customer identity DC just verified.
	 *
	 * <p>Note: this principal is opaque to the AS (a string id assigned by DC). The AS does not
	 * have a {@code UserDetailsService} for retail customers and does not need one &mdash; the AS
	 * only needs the user to be authenticated for Spring AS's code-mint logic. The principal
	 * value will appear in {@code OAuth2Authorization.principalName} and downstream
	 * introspection responses.</p>
	 */
	private void authenticateCustomer(String principal, HttpServletRequest request, HttpServletResponse response) {
		Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
				principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}

	/**
	 * Split an ESPI scope string into Spring AS scope tokens. ESPI's grammar uses
	 * {@code ;}-delimited terms; Spring AS's consent store stores them as a {@code Set<String>}.
	 * Storing each term separately means Spring AS's consent check (which compares scope sets
	 * for subset membership) does the right thing.
	 */
	private static List<String> parseScopes(String espiScope) {
		return List.of(espiScope.split(";"));
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
