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

package org.greenbuttonalliance.espi.datacustodian.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The challenge {@link EspiBearerChallenge} sends for each kind of OAuth 2.0 authentication failure (#316): a
 * {@code BearerTokenError} keeps its own RFC 6750 code and status, so the two always agree, and only the code is sent.
 */
@DisplayName("EspiBearerChallenge: the RFC 6750 code and status agree, with nothing else (#316)")
@ExtendWith(OutputCaptureExtension.class)
class EspiBearerChallengeTest {

	static Stream<Arguments> failures() {
		return Stream.of(
				Arguments.of(new OAuth2AuthenticationException(BearerTokenErrors.invalidToken("Provided token isn't active")),
						401, "Bearer error=\"invalid_token\""),
				Arguments.of(new OAuth2AuthenticationException(BearerTokenErrors.invalidRequest("Found multiple bearer tokens")),
						400, "Bearer error=\"invalid_request\""),
				Arguments.of(new OAuth2AuthenticationException(BearerTokenErrors.insufficientScope("Needs FB_4", "FB_4")),
						403, "Bearer error=\"insufficient_scope\""),
				Arguments.of(new OAuth2AuthenticationException(new OAuth2Error("server_error", "AS unreachable", null)),
						401, "Bearer error=\"invalid_token\""));
	}

	@ParameterizedTest(name = "{1} {2}")
	@MethodSource("failures")
	@DisplayName("an OAuth 2.0 failure gets its RFC 6750 code and status, and no description, URI or scope")
	void challenge(OAuth2AuthenticationException failure, int status, String challenge) {
		MockHttpServletResponse response = new MockHttpServletResponse();

		new EspiBearerChallenge().commence(new MockHttpServletRequest("GET", "/espi/1_1/resource/UsagePoint"),
				response, failure);

		assertThat(response).satisfies(
				r -> assertThat(r.getStatus()).as("status").isEqualTo(status),
				r -> assertThat(r.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate")
						.containsExactly(challenge));
	}

	@Test
	@DisplayName("a 403 from an authentication failure logs the token's client_id, as an AccessDeniedException's does")
	void insufficientScopeFailureLogsClientId(CapturedOutput output) {
		OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "secret-token-value",
				Instant.now(), Instant.now().plusSeconds(60));
		OAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(
				Map.of("client_id", "tp-client"), List.of());
		SecurityContextHolder.getContext().setAuthentication(
				new BearerTokenAuthentication(principal, token, List.of()));
		try {
			new EspiBearerChallenge().commence(new MockHttpServletRequest("GET", "/espi/1_1/resource/UsagePoint"),
					new MockHttpServletResponse(),
					new OAuth2AuthenticationException(BearerTokenErrors.insufficientScope("Needs FB_4", "FB_4")));
		} finally {
			SecurityContextHolder.clearContext();
		}

		assertThat(output.getOut()).as("server log")
				.contains("GET /espi/1_1/resource/UsagePoint: insufficient_scope for client_id tp-client")
				.doesNotContain("secret-token-value");
	}
}
