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
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sends the authorization response once {@code /oauth2/authorize} has minted a code, after making the
 * customer's authorization single use (#299, #276).
 *
 * <p>{@link AuthorizeContinueController} signs the customer in at the AS and saves a consent for the scope
 * the customer approved at the Data Custodian, only so that its replayed {@code /oauth2/authorize} mints the
 * code. Both outlived that replay. The next authorization in the same browser then skipped the delegation:
 * with the same scope, the stored consent satisfied Spring Authorization Server and a code was minted with no
 * ESPI provisioning (#276); with another scope, Spring's consent page was used, which reaches
 * {@code /authorize/delegate} with Spring's own {@code state} and no {@code redirect_uri}, so the Third Party
 * could not match the callback (#299).</p>
 *
 * <p>Here, after the code is minted, the consent is removed and a customer's sign-in is cleared from the
 * session, so every customer authorization goes through {@link DelegateAuthorizationEntryPoint} and the Data
 * Custodian's Authorization Screen with the Third Party's own {@code state} and {@code redirect_uri}. The
 * customer's consent to a Subscription is held by the Data Custodian, not here. A non-customer sign-in (an AS
 * administrator's) is left as it is.</p>
 *
 * <p>The response itself is Spring's default ({@code OAuth2AuthorizationEndpointFilter#sendAuthorizationResponse}):
 * a redirect to the request's {@code redirect_uri} with {@code code} and, when present, {@code state}.</p>
 */
@Slf4j
public class SingleUseCustomerAuthorizationResponseHandler implements AuthenticationSuccessHandler {

	static final String CUSTOMER_AUTHORITY = "ROLE_CUSTOMER";

	private final OAuth2AuthorizationConsentService consentService;
	private final RegisteredClientRepository registeredClientRepository;
	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	public SingleUseCustomerAuthorizationResponseHandler(OAuth2AuthorizationConsentService consentService,
														  RegisteredClientRepository registeredClientRepository) {
		this.consentService = consentService;
		this.registeredClientRepository = registeredClientRepository;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException {
		OAuth2AuthorizationCodeRequestAuthenticationToken codeRequest =
				(OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
		Authentication principal = (Authentication) codeRequest.getPrincipal();

		removeConsent(codeRequest.getClientId(), principal.getName());
		if (isCustomer(principal)) {
			SecurityContextHolder.clearContext();
			securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
		}

		sendAuthorizationResponse(request, response, codeRequest);
	}

	private void removeConsent(String clientId, String principalName) {
		RegisteredClient client = registeredClientRepository.findByClientId(clientId);
		if (client == null) {
			return;
		}
		OAuth2AuthorizationConsent consent = consentService.findById(client.getId(), principalName);
		if (consent != null) {
			consentService.remove(consent);
			log.debug("Removed the single-use consent of {} for client_id={}", principalName, clientId);
		}
	}

	private static boolean isCustomer(Authentication principal) {
		return principal.getAuthorities().stream().anyMatch(a -> CUSTOMER_AUTHORITY.equals(a.getAuthority()));
	}

	/** As {@code OAuth2AuthorizationEndpointFilter#sendAuthorizationResponse} in Spring Authorization Server 7.1. */
	private void sendAuthorizationResponse(HttpServletRequest request, HttpServletResponse response,
										   OAuth2AuthorizationCodeRequestAuthenticationToken codeRequest)
			throws IOException {
		UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(codeRequest.getRedirectUri())
				.queryParam(OAuth2ParameterNames.CODE, codeRequest.getAuthorizationCode().getTokenValue());
		if (StringUtils.hasText(codeRequest.getState())) {
			uri.queryParam(OAuth2ParameterNames.STATE, UriUtils.encode(codeRequest.getState(), StandardCharsets.UTF_8));
		}
		// build(true): the components are explicitly encoded
		redirectStrategy.sendRedirect(request, response, uri.build(true).toUriString());
	}
}
