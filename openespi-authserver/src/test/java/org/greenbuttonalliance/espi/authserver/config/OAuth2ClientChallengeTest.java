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

package org.greenbuttonalliance.espi.authserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

import static org.assertj.core.api.Assertions.assertThat;

/** The RFC 6749 §5.2 client error responses of {@link OAuth2ClientChallenge} (#225). */
@DisplayName("OAuth2ClientChallenge: invalid_client with a Basic challenge (#225)")
class OAuth2ClientChallengeTest {

	private final OAuth2ClientChallenge challenge = new OAuth2ClientChallenge();

	@Test
	@DisplayName("no credentials: 401 invalid_client with the Basic challenge")
	void noCredentials() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		challenge.commence(new MockHttpServletRequest("POST", "/oauth2/token"), response,
				new InsufficientAuthenticationException("Full authentication is required"));

		assertThat(response).satisfies(
				r -> assertThat(r.getStatus()).as("status").isEqualTo(401),
				r -> assertThat(r.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate")
						.containsExactly("Basic realm=\"oauth2\""),
				r -> assertThat(r.getContentAsString()).as("body").isEqualTo("{\"error\":\"invalid_client\"}"));
	}

	@Test
	@DisplayName("failed credentials: 401 invalid_client with the error code only, not the description")
	void failedCredentials() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		challenge.onAuthenticationFailure(new MockHttpServletRequest("POST", "/oauth2/token"), response,
				new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT,
						"Client authentication failed: client_secret", null)));

		assertThat(response).satisfies(
				r -> assertThat(r.getStatus()).as("status").isEqualTo(401),
				r -> assertThat(r.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate")
						.containsExactly("Basic realm=\"oauth2\""),
				r -> assertThat(r.getContentAsString()).as("body").isEqualTo("{\"error\":\"invalid_client\"}"));
	}

	@Test
	@DisplayName("another client authentication error (e.g. invalid_request): 400 without a challenge, as Spring answers")
	void otherErrorIs400() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		challenge.onAuthenticationFailure(new MockHttpServletRequest("POST", "/oauth2/token"), response,
				new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST));

		assertThat(response).satisfies(
				r -> assertThat(r.getStatus()).as("status").isEqualTo(400),
				r -> assertThat(r.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate").isEmpty(),
				r -> assertThat(r.getContentAsString()).as("body").isEqualTo("{\"error\":\"invalid_request\"}"));
	}

	@Test
	@DisplayName("a failure that is not an OAuth 2.0 error is invalid_client")
	void nonOAuth2FailureIsInvalidClient() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		challenge.onAuthenticationFailure(new MockHttpServletRequest("POST", "/oauth2/token"), response,
				new BadCredentialsException("Bad credentials"));

		assertThat(response).satisfies(
				r -> assertThat(r.getStatus()).as("status").isEqualTo(401),
				r -> assertThat(r.getContentAsString()).as("body").isEqualTo("{\"error\":\"invalid_client\"}"));
	}
}
