/*
 * Copyright 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-boundary tests for the {@code /espi/1_1/resource/Authorization/**} endpoints.
 *
 * <p>The {@code /Authorization} API exposes OAuth2 authorization metadata and is reachable ONLY
 * with a client-credentials token (DataCustodian admin or Third-Party admin). Customer-bearer
 * tokens carrying FB-scoped authorities &mdash; the authorization-code flow's output &mdash; MUST
 * be rejected; they grant access to the customer's energy data and customer/PII resources, not to
 * the authorization metadata itself.</p>
 *
 * <p>This test verifies <em>only</em> the security boundary. The controller bodies are stubs
 * (returning {@code null}) as of this PR; functional behavior (per-TP filtering when called with
 * {@code SCOPE_ThirdParty_Admin_Access}, etc.) is a follow-up. Tests therefore assert that the
 * security gate allows authorized callers through &mdash; the response body content is out of
 * scope here.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthorizationController security boundary")
class AuthorizationControllerTest {

	@Autowired private MockMvc mockMvc;

	@Nested
	@DisplayName("GET /espi/1_1/resource/Authorization")
	class GetAllAuthorizations {

		@Test
		@DisplayName("returns 401 when unauthenticated")
		void unauthenticated_is_401() throws Exception {
			mockMvc.perform(get("/espi/1_1/resource/Authorization"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("returns 403 with a customer FB-scoped token (energy-data scope)")
		void customerFbScope_is_403() throws Exception {
			mockMvc.perform(get("/espi/1_1/resource/Authorization")
							.with(opaqueAuthority("FB_15")))
					.andExpect(status().isForbidden());
		}

		@Test
		@DisplayName("returns 403 with an arbitrary unrelated scope")
		void unrelatedScope_is_403() throws Exception {
			mockMvc.perform(get("/espi/1_1/resource/Authorization")
							.with(opaqueAuthority("SCOPE_Some_Random_Scope")))
					.andExpect(status().isForbidden());
		}

		@Test
		@DisplayName("DataCustodian admin scope passes the security gate")
		void dcAdmin_passes() throws Exception {
			int httpStatus = mockMvc.perform(get("/espi/1_1/resource/Authorization")
							.with(opaqueAuthority("SCOPE_DataCustodian_Admin_Access")))
					.andReturn().getResponse().getStatus();
			assertPassedSecurityGate(httpStatus);
		}

		@Test
		@DisplayName("ThirdParty admin scope (client_credentials) passes the security gate")
		void tpAdmin_passes() throws Exception {
			int httpStatus = mockMvc.perform(get("/espi/1_1/resource/Authorization")
							.with(opaqueAuthority("SCOPE_ThirdParty_Admin_Access")))
					.andReturn().getResponse().getStatus();
			assertPassedSecurityGate(httpStatus);
		}
	}

	@Nested
	@DisplayName("GET /espi/1_1/resource/Authorization/{id}")
	class GetAuthorization {

		@Test
		@DisplayName("returns 401 when unauthenticated")
		void unauthenticated_is_401() throws Exception {
			mockMvc.perform(get("/espi/1_1/resource/Authorization/" + UUID.randomUUID()))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("returns 403 with a customer FB-scoped token (PII scope)")
		void customerPiiScope_is_403() throws Exception {
			mockMvc.perform(get("/espi/1_1/resource/Authorization/" + UUID.randomUUID())
							.with(opaqueAuthority("FB_54")))
					.andExpect(status().isForbidden());
		}

		@Test
		@DisplayName("DataCustodian admin scope passes the security gate")
		void dcAdmin_passes() throws Exception {
			int httpStatus = mockMvc.perform(get("/espi/1_1/resource/Authorization/" + UUID.randomUUID())
							.with(opaqueAuthority("SCOPE_DataCustodian_Admin_Access")))
					.andReturn().getResponse().getStatus();
			assertPassedSecurityGate(httpStatus);
		}

		@Test
		@DisplayName("ThirdParty admin scope (client_credentials) passes the security gate")
		void tpAdmin_passes() throws Exception {
			int httpStatus = mockMvc.perform(get("/espi/1_1/resource/Authorization/" + UUID.randomUUID())
							.with(opaqueAuthority("SCOPE_ThirdParty_Admin_Access")))
					.andReturn().getResponse().getStatus();
			assertPassedSecurityGate(httpStatus);
		}
	}

	/**
	 * Spring Security's {@code jwt()} request post-processor builds a Jwt-backed Authentication
	 * with the supplied authorities. ESPI uses opaque tokens at runtime, but {@code jwt()} is the
	 * Spring-Security-test idiomatic way to attach an authenticated principal with specific
	 * authorities to a MockMvc request — the resource server doesn't care which token format
	 * produced the authorities for {@code hasAuthority} / {@code hasAnyAuthority} checks.
	 */
	private static org.springframework.test.web.servlet.request.RequestPostProcessor opaqueAuthority(String scope) {
		return jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(scope));
	}

	/**
	 * The security gate is what's under test. Anything that isn't 401 (unauthenticated) or 403
	 * (forbidden) means the gate let the caller through. Body content / functional response codes
	 * are tested elsewhere when the controller's stub is implemented.
	 */
	private static void assertPassedSecurityGate(int httpStatus) {
		org.assertj.core.api.Assertions.assertThat(httpStatus)
				.as("Expected security gate to allow the request through (i.e. NOT 401 or 403)")
				.isNotEqualTo(401)
				.isNotEqualTo(403);
	}
}
