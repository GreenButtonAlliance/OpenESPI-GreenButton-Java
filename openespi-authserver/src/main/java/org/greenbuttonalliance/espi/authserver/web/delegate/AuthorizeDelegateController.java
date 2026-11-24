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
import org.greenbuttonalliance.espi.authserver.config.DataCustodianIntegrationConfig;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Intermediate AS-side endpoint that Spring Authorization Server's filter chain redirects to when
 * the customer needs to authenticate or grant consent. This controller signs the request context
 * into a {@link org.greenbuttonalliance.espi.handoff.SignedHandoff.Outbound} and redirects the
 * user-agent to the Data Custodian's Authorization Screen.
 *
 * <h2>Why this endpoint exists</h2>
 * Spring Authorization Server's {@code loginPage(...)} and {@code consentPage(...)} hooks accept
 * a <em>static</em> URL. Spring AS appends fixed query parameters ({@code scope}, {@code client_id},
 * {@code state}) when it redirects. To compose a signed handoff containing those parameters
 * (instead of letting them travel as unsigned URL params to DC) we receive Spring AS's redirect
 * here, build and sign the handoff, then re-redirect to DC with the signed token.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Customer &rarr; AS {@code /oauth2/authorize?client_id=X&scope=Y&...}</li>
 *   <li>AS: customer not authenticated &rarr; redirects to {@code /authorize/delegate?scope=Y&client_id=X&state=Z}
 *       (this controller)</li>
 *   <li>This controller: builds signed outbound handoff, redirects user-agent to DC's
 *       {@code /oauth/authorize-screen?handoff=<signed>}</li>
 *   <li>DC handles login (its customer-login filter chain redirects unauthenticated users to its
 *       own {@code /login}, then back to {@code /oauth/authorize-screen}); customer makes
 *       selections, submits.</li>
 *   <li>DC redirects user-agent to AS's {@code /oauth2/authorize/continue?handoff=<signed-return>}
 *       (the return URL embedded in the outbound handoff).</li>
 *   <li>The {@code /continue} endpoint (PR C3.3) verifies the return, sets SecurityContext,
 *       resumes the {@code /oauth2/authorize} flow with the customer's effective approved scope.</li>
 * </ol>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthorizeDelegateController {

	private static final String DC_AUTHORIZE_SCREEN_PATH = "/oauth/authorize-screen";
	private static final OAuth2TokenType STATE_TOKEN_TYPE = new OAuth2TokenType(OAuth2ParameterNames.STATE);
	private static final AuthenticationTrustResolver TRUST_RESOLVER = new AuthenticationTrustResolverImpl();

	private final OutboundHandoffBuilder handoffBuilder;
	private final DataCustodianIntegrationConfig dcIntegration;
	private final DelegationStateService delegationStateService;
	private final OAuth2AuthorizationService authorizationService;
	private final RegisteredClientRepository registeredClientRepository;

	/**
	 * Receives Spring AS's redirect when the customer needs to authenticate OR consent. Builds a
	 * signed outbound handoff and redirects the user-agent to DC's Authorization Screen.
	 *
	 * <p>Spring AS's standard query params on the redirect:</p>
	 * <ul>
	 *   <li>{@code scope} &mdash; the TP-requested scope</li>
	 *   <li>{@code client_id} &mdash; the requesting TP's client_id</li>
	 *   <li>{@code state} &mdash; the TP's CSRF state, echoed back to the TP. It is not the correlation id: that
	 *       is always a random id of the AS's own (#331), so no other request can name or replace this
	 *       delegation's state</li>
	 *   <li>{@code redirect_uri} &mdash; the TP's callback, forwarded by {@link DelegateAuthorizationEntryPoint}</li>
	 *   <li>{@code code_challenge}, {@code code_challenge_method} &mdash; the TP's PKCE challenge, when it sent one,
	 *       forwarded the same way and replayed by {@link AuthorizeContinueController} (#274)</li>
	 * </ul>
	 *
	 * <p>Spring AS's consent-page redirect is different: it carries Spring's own consent {@code state} and no
	 * {@code redirect_uri}. Taken as the TP's, that state made the TP ignore the callback (#299). The TP's
	 * {@code state}, {@code redirect_uri} and {@code scope} are then read from the original request that Spring
	 * AS saved with the pending authorization under its consent state, if that authorization is the signed-in
	 * customer's; a signed-in request without one is refused (400). Since #305 a customer is signed in only on
	 * the replay of a delegation, whose consent is the approved scope ({@link DelegatedCustomerAuthenticationFilter}),
	 * so this path is kept only as a safeguard.</p>
	 */
	@GetMapping("/authorize/delegate")
	public String delegate(@RequestParam("scope") String scope,
						   @RequestParam("client_id") String clientId,
						   @RequestParam(value = "state", required = false) String state,
						   @RequestParam(value = "redirect_uri", required = false) String redirectUri,
						   @RequestParam(value = PkceParameterNames.CODE_CHALLENGE, required = false) String codeChallenge,
						   @RequestParam(value = PkceParameterNames.CODE_CHALLENGE_METHOD, required = false)
						   String codeChallengeMethod,
						   HttpServletRequest request) {

		String tpState = state;
		String requestedScope = scope;
		String resolvedRedirectUri = redirectUri;
		String pendingAuthorizationId = null;
		DelegationStateService.ProofKey proofKey = DelegationStateService.ProofKey.of(codeChallenge, codeChallengeMethod);
		if (redirectUri == null || redirectUri.isBlank()) {
			OAuth2Authorization pending = pendingConsentAuthorization(clientId, state);
			if (pending != null) {
				// The consent-page redirect: state, redirect_uri, scope and any PKCE challenge (#274) all come from
				// the TP's saved request.
				OAuth2AuthorizationRequest original = pending.getAttribute(OAuth2AuthorizationRequest.class.getName());
				tpState = original.getState();
				resolvedRedirectUri = original.getRedirectUri();
				requestedScope = String.join(" ", original.getScopes());
				pendingAuthorizationId = pending.getId();
				proofKey = DelegationStateService.ProofKey.of(
						(String) original.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE),
						(String) original.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD));
			} else if (TRUST_RESOLVER.isAuthenticated(SecurityContextHolder.getContext().getAuthentication())) {
				// Spring AS sends a signed-in customer here only from its consent page, so this state is Spring's
				// consent state and must never reach the TP as its own (#299): the request is unknown or expired.
				log.warn("AS delegation refused: client_id={} consent-page request has no pending authorization", clientId);
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authorization request expired; start again");
			}
			if (resolvedRedirectUri == null || resolvedRedirectUri.isBlank()) {
				resolvedRedirectUri = soleRegisteredRedirectUri(clientId);
			}
			if (resolvedRedirectUri == null) {
				log.warn("AS delegation refused: client_id={} sent no redirect_uri and it cannot be determined", clientId);
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirect_uri cannot be determined");
			}
		}
		// #331: always the AS's own random id, never the TP's state. Delegation state is global, and this endpoint
		// is reachable unauthenticated, so a state another TP reuses, or anyone learns, must not name an entry.
		String correlationId = UUID.randomUUID().toString();

		// Saved so AuthorizeContinueController replays /oauth2/authorize with the TP's redirect_uri, state and PKCE
		// challenge (#274), and removes the pending consent-page authorization, which a reload of this page still
		// needs until then.
		delegationStateService.save(correlationId, clientId, resolvedRedirectUri, tpState, requestedScope,
				pendingAuthorizationId, proofKey);

		String signedToken = handoffBuilder.buildSignedToken(clientId, requestedScope, correlationId);

		String url = UriComponentsBuilder.fromUriString(dcIntegration.getBaseUrl())
				.path(DC_AUTHORIZE_SCREEN_PATH)
				.queryParam("handoff", signedToken)
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();

		log.info("AS delegation: client_id={}, scope='{}', correlation_id={}, redirect_uri={}, redirecting to DC",
				clientId, scope, correlationId, resolvedRedirectUri);

		return "redirect:" + url;
	}

	/**
	 * The pending authorization Spring AS saved before its consent-page redirect, when {@code state} is its
	 * consent state, it holds this client's original request, and it is the signed-in customer's. It is not
	 * removed here: a reload of this page needs it again, and {@link AuthorizeContinueController} removes it.
	 */
	private OAuth2Authorization pendingConsentAuthorization(String clientId, String state) {
		if (state == null || state.isBlank()) {
			return null;
		}
		OAuth2Authorization pending = authorizationService.findByToken(state, STATE_TOKEN_TYPE);
		if (pending == null) {
			return null;
		}
		OAuth2AuthorizationRequest original = pending.getAttribute(OAuth2AuthorizationRequest.class.getName());
		if (original == null || !clientId.equals(original.getClientId())) {
			return null;
		}
		Authentication signedIn = SecurityContextHolder.getContext().getAuthentication();
		if (!TRUST_RESOLVER.isAuthenticated(signedIn) || !signedIn.getName().equals(pending.getPrincipalName())) {
			return null;
		}
		return pending;
	}

	/**
	 * The client's redirect_uri when it registered exactly one, which RFC 6749 3.1.2.3 lets it leave out of the
	 * request. With several there is no telling which one was meant ({@code RegisteredClient} holds them in a
	 * set), so none is guessed (#299).
	 */
	private String soleRegisteredRedirectUri(String clientId) {
		RegisteredClient client = registeredClientRepository.findByClientId(clientId);
		if (client == null || client.getRedirectUris().size() != 1) {
			return null;
		}
		return client.getRedirectUris().iterator().next();
	}
}
