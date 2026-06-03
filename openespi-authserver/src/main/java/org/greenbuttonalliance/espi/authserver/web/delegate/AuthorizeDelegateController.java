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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.authserver.config.DataCustodianIntegrationConfig;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	private final OutboundHandoffBuilder handoffBuilder;
	private final DataCustodianIntegrationConfig dcIntegration;
	private final DelegationStateService delegationStateService;
	private final RegisteredClientRepository registeredClientRepository;

	/**
	 * Receives Spring AS's redirect when the customer needs to authenticate OR consent. Builds a
	 * signed outbound handoff and redirects the user-agent to DC's Authorization Screen.
	 *
	 * <p>Spring AS's standard query params on the redirect:</p>
	 * <ul>
	 *   <li>{@code scope} &mdash; the TP-requested scope</li>
	 *   <li>{@code client_id} &mdash; the requesting TP's client_id</li>
	 *   <li>{@code state} &mdash; the TP's CSRF state, used here as the correlation id so the
	 *       AS-side and DC-side logs join cleanly</li>
	 * </ul>
	 */
	@GetMapping("/authorize/delegate")
	public String delegate(@RequestParam("scope") String scope,
						   @RequestParam("client_id") String clientId,
						   @RequestParam(value = "state", required = false) String state,
						   @RequestParam(value = "redirect_uri", required = false) String redirectUri,
						   HttpServletRequest request) {

		String correlationId = state != null && !state.isBlank() ? state : UUID.randomUUID().toString();

		// Save delegation state so AuthorizeContinueController can find the third party's
		// redirect_uri and TP state on the return trip. redirect_uri is optional in the request
		// (Spring AS's consentPage redirect doesn't always include it); when absent we fall back
		// to the FIRST configured redirect_uri on the RegisteredClient — adequate for most ESPI
		// third parties which register a single URI, documented as a limitation for the rest.
		String resolvedRedirectUri = redirectUri != null && !redirectUri.isBlank()
				? redirectUri
				: firstRegisteredRedirectUri(clientId);
		delegationStateService.save(correlationId, clientId, resolvedRedirectUri, state);

		String signedToken = handoffBuilder.buildSignedToken(clientId, scope, correlationId);

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

	private String firstRegisteredRedirectUri(String clientId) {
		RegisteredClient client = registeredClientRepository.findByClientId(clientId);
		if (client == null || client.getRedirectUris().isEmpty()) {
			return null;
		}
		return client.getRedirectUris().iterator().next();
	}
}
