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

package org.greenbuttonalliance.espi.datacustodian.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Actuator access: health (and its probe groups) is anonymous and reports status only; every other
 * exposed endpoint needs a {@code DataCustodian_Admin_Access} token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorSecurityConfigurationTest {

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest
	@ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
	void health_is_anonymous_and_shows_status_only(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/actuator", "/actuator/info", "/actuator/metrics"})
	void other_endpoints_reject_anonymous_requests(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(status().isUnauthorized());
	}

	// /actuator/prometheus is not listed below: Spring Boot tests disable metric exporters by default,
	// so the endpoint does not exist here. Its rule is the same anyRequest() admin rule.
	@ParameterizedTest
	@ValueSource(strings = {"/actuator", "/actuator/info", "/actuator/metrics"})
	void other_endpoints_reject_non_admin_tokens(String path) throws Exception {
		mockMvc.perform(get(path).with(opaqueToken().authorities(new SimpleGrantedAuthority("FB_4"))))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/actuator", "/actuator/info", "/actuator/metrics"})
	void other_endpoints_accept_admin_tokens(String path) throws Exception {
		mockMvc.perform(get(path).with(opaqueToken()
						.authorities(new SimpleGrantedAuthority(ActuatorSecurityConfiguration.ADMIN_AUTHORITY))))
				.andExpect(status().isOk());
	}
}
