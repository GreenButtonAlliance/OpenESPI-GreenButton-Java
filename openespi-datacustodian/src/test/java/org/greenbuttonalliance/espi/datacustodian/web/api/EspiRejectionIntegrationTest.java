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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The rejections no resource test covers (#316): every 401 and 403 carries a {@code WWW-Authenticate: Bearer}
 * challenge (RFC 6750 §3, REQ.21.6.1.2), as OpenESPI-GreenButtonCMDTest checks, with the error code only and no
 * description; a 400 carries its reason. The row-level 403s are asserted where they are tested
 * ({@code SubscriptionResourceScopingIntegrationTest}, {@code BatchSubscriptionIntegrationTest}). The reason the
 * client is not told is logged on the server, without the token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ESPI rejections say why (#316)")
@ExtendWith(OutputCaptureExtension.class)
class EspiRejectionIntegrationTest {

	private static final String USAGE_POINT = "/espi/1_1/resource/UsagePoint";

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private OpaqueTokenIntrospector introspector;

	@Test
	@DisplayName("no token: 401 with a Bearer challenge and no error code (RFC 6750 §3.1)")
	void noToken() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get(USAGE_POINT).accept(MediaType.APPLICATION_ATOM_XML))
				.andReturn().getResponse();

		assertThat(response).satisfies(
				r -> assertThat(r.getStatus()).as("status").isEqualTo(401),
				r -> assertThat(r.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate")
						.containsExactly("Bearer"));
	}

	@Test
	@DisplayName("an inactive token: 401 with error=\"invalid_token\" only, not the introspection's reason")
	void inactiveToken(CapturedOutput output) throws Exception {
		when(introspector.introspect(anyString())).thenThrow(new BadOpaqueTokenException("Provided token isn't active"));

		MockHttpServletResponse response = mockMvc.perform(get(USAGE_POINT)
						.header(HttpHeaders.AUTHORIZATION, "Bearer inactive-token")
						.accept(MediaType.APPLICATION_ATOM_XML))
				.andReturn().getResponse();

		assertThat(response).satisfies(
				r -> assertThat(r.getStatus()).as("status").isEqualTo(401),
				r -> assertThat(r.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate")
						.containsExactly("Bearer error=\"invalid_token\""));
		assertThat(output.getOut()).as("server log")
				.contains("GET " + USAGE_POINT + ": invalid_token (Provided token isn't active)")
				.doesNotContain("inactive-token");
	}

	@Test
	@DisplayName("a row-level 403: the client gets insufficient_scope only; the server logs the client_id and the reason")
	void rowLevelReasonIsLoggedNotSent(CapturedOutput output) throws Exception {
		mockMvc.perform(get("/espi/1_1/resource/UsageSummary").accept(MediaType.APPLICATION_ATOM_XML)
						.with(opaqueToken().attributes(a -> a.put("client_id", "probing-client"))
								.authorities(new SimpleGrantedAuthority("FB_15"))))
				.andExpect(EspiErrorResponses.insufficientScope());

		assertThat(output.getOut()).as("server log").contains(
				"GET /espi/1_1/resource/UsageSummary: insufficient_scope for client_id probing-client"
						+ " (No subscription for this token)");
	}

	@Test
	@DisplayName("a token without the endpoint's Function Blocks: 403 insufficient_scope from the URL rule")
	void missingFunctionBlock() throws Exception {
		mockMvc.perform(get(USAGE_POINT).accept(MediaType.APPLICATION_ATOM_XML)
						.with(opaqueToken().authorities(new SimpleGrantedAuthority("FB_15"))))
				.andExpect(EspiErrorResponses.insufficientScope());
	}

	@Test
	@DisplayName("an entry id that is not a UUID: 400 naming the parameter, as text/plain to an Atom-only client")
	void malformedEntryId() throws Exception {
		mockMvc.perform(get(USAGE_POINT + "/not-a-uuid").accept(MediaType.APPLICATION_ATOM_XML)
						.with(opaqueToken().authorities(new SimpleGrantedAuthority("SCOPE_DataCustodian_Admin_Access"))))
				.andExpect(EspiErrorResponses.badRequest("usagePointId is not a valid UUID"));
	}
}
