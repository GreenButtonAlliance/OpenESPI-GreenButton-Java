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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The admin's "Obtain a new client access token" action never ends in a 500: whatever fails, the token
 * already held stays in use and the admin gets the error message (#275 review).
 */
class CustodianHomeControllerClientTokenTest {

	private static final String ERROR = "redirect:/custodian/home?client_token_error";

	@SuppressWarnings("unchecked")
	private final OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> tokenResponseClient =
			mock(OAuth2AccessTokenResponseClient.class);
	private final EspiAuthorizationOAuth2AuthorizedClientService authorizedClientService =
			mock(EspiAuthorizationOAuth2AuthorizedClientService.class);
	private final ClientRegistrationRepository registrations = mock(ClientRegistrationRepository.class);
	private final CustodianHomeController controller =
			new CustodianHomeController(tokenResponseClient, authorizedClientService, registrations);
	private final Authentication admin = new TestingAuthenticationToken("admin", null, "ROLE_CUSTODIAN");

	@Test
	@DisplayName("no client_credentials registration configured: the error message, no token request")
	void missing_registration() {
		assertThat(controller.obtainClientToken(admin)).isEqualTo(ERROR);
		verifyNoInteractions(tokenResponseClient, authorizedClientService);
	}

	@Test
	@DisplayName("a failure outside the OAuth2 exceptions (an unreadable response) still gives the error message")
	void unexpected_failure() {
		when(registrations.findByRegistrationId(DataCustodianRegistrations.CLIENT_ACCESS)).thenReturn(
				ClientRegistration.withRegistrationId(DataCustodianRegistrations.CLIENT_ACCESS)
						.clientId("third_party")
						.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
						.tokenUri("http://localhost:9999/oauth2/token")
						.build());
		when(tokenResponseClient.getTokenResponse(any())).thenThrow(new IllegalArgumentException("no access_token"));

		assertThat(controller.obtainClientToken(admin)).isEqualTo(ERROR);
		verifyNoInteractions(authorizedClientService);
	}
}
