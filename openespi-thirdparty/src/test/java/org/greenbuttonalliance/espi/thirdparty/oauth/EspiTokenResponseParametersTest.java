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

package org.greenbuttonalliance.espi.thirdparty.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class EspiTokenResponseParametersTest {

	private static final String RESOURCE_URI = "https://dc.example/espi/1_1/resource/Batch/Subscription/1";
	private static final String AUTHORIZATION_URI = "https://dc.example/espi/1_1/resource/Authorization/2";

	private static OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> capturing(String tokenValue) {
		return EspiTokenResponseParameters.capturing(grantRequest -> OAuth2AccessTokenResponse.withToken(tokenValue)
				.tokenType(OAuth2AccessToken.TokenType.BEARER)
				.expiresIn(3600)
				.additionalParameters(Map.of(EspiTokenResponseParameters.RESOURCE_URI, RESOURCE_URI,
						EspiTokenResponseParameters.AUTHORIZATION_URI, AUTHORIZATION_URI))
				.build());
	}

	@Test
	@DisplayName("a token refreshed outside a web request (another thread) keeps its ESPI URIs for storage")
	void hand_over_works_without_a_request() throws Exception {
		CompletableFuture.runAsync(() -> {
			assertThat(RequestContextHolder.getRequestAttributes()).isNull();
			capturing("off-request-token").getTokenResponse(null);
		}).get();

		assertThat(EspiTokenResponseParameters.take("off-request-token"))
				.hasValue(new EspiTokenResponseParameters.EspiUris(RESOURCE_URI, AUTHORIZATION_URI, null));
	}

	@Test
	@DisplayName("the URIs are handed over once: a second take finds nothing")
	void uris_are_taken_once() {
		capturing("once-token").getTokenResponse(null);

		assertThat(EspiTokenResponseParameters.take("once-token")).isPresent();
		assertThat(EspiTokenResponseParameters.take("once-token")).isEmpty();
	}
}
