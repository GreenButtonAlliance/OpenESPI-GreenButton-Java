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

package org.greenbuttonalliance.espi.datacustodian.config;

import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Data Custodian has no H2 console (#271): the path is refused before any handler, anonymous or with the
 * DataCustodian admin token. It used to be {@code permitAll()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("H2 console is refused on the Data Custodian (#271)")
class H2ConsoleRefusedTest {

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest
	@ValueSource(strings = {"/h2-console", "/h2-console/", "/h2-console/login.do"})
	@DisplayName("anonymous: 401")
	void anonymousIsRefused(String path) throws Exception {
		mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/h2-console", "/h2-console/", "/h2-console/login.do"})
	@DisplayName("the DataCustodian admin token: 403")
	void adminTokenIsRefused(String path) throws Exception {
		mockMvc.perform(get(path).with(opaqueToken()
						.authorities(new SimpleGrantedAuthority("SCOPE_DataCustodian_Admin_Access"))))
				.andExpect(status().isForbidden());
	}
}
