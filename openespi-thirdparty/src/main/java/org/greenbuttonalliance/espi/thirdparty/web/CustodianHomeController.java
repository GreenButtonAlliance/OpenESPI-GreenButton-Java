/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.thirdparty.web;

import org.greenbuttonalliance.espi.thirdparty.oauth.DataCustodianRegistrations;
import org.greenbuttonalliance.espi.thirdparty.oauth.EspiAuthorizationOAuth2AuthorizedClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;

/**
 * The Third Party administrator's landing page after login (#249), with a check of the TP's ESPI
 * client access token (#146): obtained from the Authorization Server with client_credentials
 * ({@code datacustodian-admin}), stored in the {@code authorizations} table with no customer, and
 * shown by scope, expiry, any ESPI URIs, and the first characters of the token only.
 */
@Controller
@RequestMapping("/custodian/home")
@PreAuthorize("hasRole('ROLE_CUSTODIAN')")
public class CustodianHomeController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(CustodianHomeController.class);

	private final OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient;
	private final EspiAuthorizationOAuth2AuthorizedClientService authorizedClientService;
	private final ClientRegistrationRepository clientRegistrationRepository;

	public CustodianHomeController(
			OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient,
			EspiAuthorizationOAuth2AuthorizedClientService authorizedClientService,
			ClientRegistrationRepository clientRegistrationRepository) {
		this.clientCredentialsTokenResponseClient = clientCredentialsTokenResponseClient;
		this.authorizedClientService = authorizedClientService;
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@GetMapping
	public String index(Authentication authentication, Model model) {
		authorizedClientService.findAuthorization(DataCustodianRegistrations.CLIENT_ACCESS, authentication.getName())
				.map(authorization -> AuthorizationView.of(authorization, clientRegistrationRepository))
				.ifPresent(view -> model.addAttribute("clientToken", view));
		return "custodian/home";
	}

	/**
	 * Obtains a new client access token from the Authorization Server, even while a valid one is
	 * stored, and replaces the stored one only once the new one has been issued and saved. If the AS
	 * is unreachable or refuses, or the save fails, the token already held stays in use.
	 */
	@PostMapping("/client-token")
	public String obtainClientToken(Authentication authentication) {
		ClientRegistration registration = clientRegistrationRepository
				.findByRegistrationId(DataCustodianRegistrations.CLIENT_ACCESS);
		try {
			OAuth2AccessTokenResponse tokenResponse = clientCredentialsTokenResponseClient
					.getTokenResponse(new OAuth2ClientCredentialsGrantRequest(registration));
			authorizedClientService.saveAuthorizedClient(new OAuth2AuthorizedClient(registration,
					authentication.getName(), tokenResponse.getAccessToken(), tokenResponse.getRefreshToken()),
					authentication);
			return "redirect:/custodian/home?client_token";
		} catch (OAuth2AuthorizationException | RestClientException | DataAccessException ex) {
			logger.warn("Could not obtain the TP client access token: {}", ex.getMessage());
			return "redirect:/custodian/home?client_token_error";
		}
	}
}
