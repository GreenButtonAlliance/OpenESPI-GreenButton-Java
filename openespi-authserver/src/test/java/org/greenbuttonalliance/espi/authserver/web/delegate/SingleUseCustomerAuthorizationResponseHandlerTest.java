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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SingleUseCustomerAuthorizationResponseHandler (#299, #276, #305)")
class SingleUseCustomerAuthorizationResponseHandlerTest {

	private static final String SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";

	private final SingleUseCustomerAuthorizationResponseHandler handler = new SingleUseCustomerAuthorizationResponseHandler();
	private final Authentication customer = UsernamePasswordAuthenticationToken.authenticated("42", null,
			List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

	private static OAuth2AuthorizationCodeRequestAuthenticationToken minted(Authentication principal, String state) {
		OAuth2AuthorizationCode code = new OAuth2AuthorizationCode("the-code", Instant.now(), Instant.now().plusSeconds(300));
		return new OAuth2AuthorizationCodeRequestAuthenticationToken("https://as.example/oauth2/authorize", "tp-1",
				principal, code, "https://tp.example/cb", state, Set.of(SCOPE));
	}

	@Test
	@DisplayName("a code goes to the Third Party with its state")
	void codeGoesToTheThirdParty() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, minted(customer, "tp state+&="));

		assertThat(response.getRedirectedUrl()).isEqualTo("https://tp.example/cb?code=the-code&state=tp%20state%2B%26%3D");
	}

	@Test
	@DisplayName("a code without state is sent without state")
	void noStateNoStateParameter() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, minted(customer, null));

		assertThat(response.getRedirectedUrl()).isEqualTo("https://tp.example/cb?code=the-code");
	}

	private static OAuth2AuthorizationCodeRequestAuthenticationException refused(Authentication principal,
																				 String redirectUri) {
		OAuth2AuthorizationCodeRequestAuthenticationToken request = new OAuth2AuthorizationCodeRequestAuthenticationToken(
				"https://as.example/oauth2/authorize", "tp-1", principal, redirectUri, "tp state", Set.of(SCOPE),
				Map.of());
		return new OAuth2AuthorizationCodeRequestAuthenticationException(
				new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE, "scope not allowed", null), request);
	}

	@Test
	@DisplayName("a replay ending in an error: the error goes to the Third Party with its state")
	void errorGoesToTheThirdParty() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationFailure(new MockHttpServletRequest(), response, refused(customer, "https://tp.example/cb"));

		assertThat(response.getRedirectedUrl()).isEqualTo(
				"https://tp.example/cb?error=invalid_scope&error_description=scope%20not%20allowed&state=tp%20state");
	}

	@Test
	@DisplayName("an error without a redirect_uri is a 400, as Spring's default")
	void errorWithoutRedirectUriIs400() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
				List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

		handler.onAuthenticationFailure(new MockHttpServletRequest(), response, refused(anonymous, null));

		assertThat(response.getStatus()).isEqualTo(400);
	}
}
