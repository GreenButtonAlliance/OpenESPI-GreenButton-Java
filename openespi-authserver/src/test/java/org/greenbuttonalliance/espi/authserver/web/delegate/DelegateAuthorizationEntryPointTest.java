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
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DelegateAuthorizationEntryPoint (#232)")
class DelegateAuthorizationEntryPointTest {

	private static final String SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";

	private final DelegateAuthorizationEntryPoint entryPoint = new DelegateAuthorizationEntryPoint();

	@Test
	@DisplayName("redirects to /authorize/delegate carrying client_id, scope, state and redirect_uri")
	void forwardsAuthorizationRequestParameters() throws Exception {
		MockHttpServletRequest request = authorizeRequest();
		request.setParameter("client_id", "third_party");
		request.setParameter("scope", SCOPE);
		request.setParameter("state", "a+b&c=d");
		request.setParameter("redirect_uri", "http://localhost:8082/ThirdParty/espi/1_1/OAuthCallBack");
		request.setParameter("response_type", "code");
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(request, response, new InsufficientAuthenticationException("unauthenticated"));

		UriComponents redirect = UriComponentsBuilder.fromUriString(response.getRedirectedUrl()).build();
		assertThat(redirect)
				.extracting(UriComponents::getPath,
						r -> decoded(r, "client_id"), r -> decoded(r, "scope"),
						r -> decoded(r, "state"), r -> decoded(r, "redirect_uri"),
						r -> r.getQueryParams().containsKey("response_type"))
				.containsExactly("/authorize/delegate",
						"third_party", SCOPE, "a+b&c=d", "http://localhost:8082/ThirdParty/espi/1_1/OAuthCallBack",
						false);
	}

	@Test
	@DisplayName("omits parameters the authorization request did not carry")
	void omitsAbsentParameters() throws Exception {
		MockHttpServletRequest request = authorizeRequest();
		request.setParameter("client_id", "third_party");
		request.setParameter("scope", SCOPE);
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(request, response, new InsufficientAuthenticationException("unauthenticated"));

		assertThat(UriComponentsBuilder.fromUriString(response.getRedirectedUrl()).build().getQueryParams().keySet())
				.containsExactly("client_id", "scope");
	}

	private static MockHttpServletRequest authorizeRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
		request.setServerName("localhost");
		request.setServerPort(9999);
		return request;
	}

	private static String decoded(UriComponents uri, String name) {
		return URLDecoder.decode(uri.getQueryParams().getFirst(name), StandardCharsets.UTF_8);
	}
}
