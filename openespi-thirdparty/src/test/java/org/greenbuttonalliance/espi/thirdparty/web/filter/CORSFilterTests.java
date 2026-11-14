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

package org.greenbuttonalliance.espi.thirdparty.web.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CORSFilterTests {

	private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String MAX_AGE = "Access-Control-Max-Age";
	private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";

	private CORSFilter corsFilter;
	private MockFilterConfig filterConfig;
	private FilterChain filterChain;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		corsFilter = new CORSFilter();
		filterConfig = new MockFilterConfig();
		filterChain = mock(FilterChain.class);
		response = new MockHttpServletResponse();
	}

	@Test
	void preflight_withAllowedOrigin_setsCorsHeadersAndStopsChain() throws Exception {
		filterConfig.addInitParameter("allow.origin", "*");
		filterConfig.addInitParameter("header:X-Custom", "custom-value");
		corsFilter.init(filterConfig);
		MockHttpServletRequest request = request("OPTIONS", "https://tp.example.com");

		corsFilter.doFilter(request, response, filterChain);

		assertThat(response)
				.extracting(r -> r.getHeader(ALLOW_ORIGIN), r -> r.getHeader(ALLOW_METHODS),
						r -> r.getHeader(ALLOW_HEADERS), r -> r.getHeader(MAX_AGE), r -> r.getHeader("X-Custom"))
				.containsExactly("*", "GET, POST, PUT, DELETE, OPTIONS",
						"Origin, Authorization, Accept, Content-Type", "1800", "custom-value");
		verifyNoInteractions(filterChain);
	}

	@Test
	void preflight_withJUnitTestOrigin_setsDefaultCorsHeaders() throws Exception {
		corsFilter.init(filterConfig);
		MockHttpServletRequest request = request("OPTIONS", "JUnit_Test");

		corsFilter.doFilter(request, response, filterChain);

		assertThat(response)
				.extracting(r -> r.getHeader(ALLOW_ORIGIN), r -> r.getHeader(ALLOW_METHODS),
						r -> r.getHeader(ALLOW_HEADERS), r -> r.getHeader(MAX_AGE))
				.containsExactly("*", "GET, POST, PUT, DELETE, OPTIONS",
						"Origin, Authorization, Accept, Content-Type", "1800");
		verifyNoInteractions(filterChain);
	}

	@Test
	void preflight_withDisallowedOrigin_setsNoCorsHeaders() throws Exception {
		filterConfig.addInitParameter("allow.origin", "https://dc.example.com");
		corsFilter.init(filterConfig);
		MockHttpServletRequest request = request("OPTIONS", "https://evil.example.org");

		corsFilter.doFilter(request, response, filterChain);

		assertThat(response.getHeaderNames()).isEmpty();
		verifyNoInteractions(filterChain);
	}

	@Test
	void actualRequest_withAllowedOrigin_setsOriginAndExposeHeadersAndContinuesChain() throws Exception {
		filterConfig.addInitParameter("allow.origin", "https://dc.example.com");
		filterConfig.addInitParameter("expose.headers", "Location");
		corsFilter.init(filterConfig);
		MockHttpServletRequest request = request("GET", "https://dc.example.com");

		corsFilter.doFilter(request, response, filterChain);

		assertThat(response)
				.extracting(r -> r.getHeader(ALLOW_ORIGIN), r -> r.getHeader(EXPOSE_HEADERS),
						r -> r.getHeader(ALLOW_METHODS))
				.containsExactly("*", "Location", null);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void actualRequest_withRegexOriginAndCredentials_echoesOrigin() throws Exception {
		filterConfig.addInitParameter("allow.origin.regex", "https://.*\\.example\\.com");
		filterConfig.addInitParameter("allow.credentials", "true");
		corsFilter.init(filterConfig);
		MockHttpServletRequest request = request("GET", "https://TP.Example.com");

		corsFilter.doFilter(request, response, filterChain);

		assertThat(response)
				.extracting(r -> r.getHeader(ALLOW_ORIGIN), r -> r.getHeader(ALLOW_CREDENTIALS))
				.containsExactly("https://TP.Example.com", "true");
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void actualRequest_withoutOrigin_setsNoCorsHeadersAndContinuesChain() throws Exception {
		filterConfig.addInitParameter("allow.origin", "*");
		corsFilter.init(filterConfig);
		MockHttpServletRequest request = request("GET", null);

		corsFilter.doFilter(request, response, filterChain);

		assertThat(response.getHeaderNames()).isEmpty();
		verify(filterChain).doFilter(request, response);
	}

	private static MockHttpServletRequest request(String method, String origin) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, "/ThirdParty");
		if (origin != null) {
			request.addHeader("Origin", origin);
		}
		return request;
	}
}
