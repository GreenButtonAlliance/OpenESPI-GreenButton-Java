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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.thirdparty.security.RetailCustomerUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Third Party username/password login against its own {@code retail_customers} accounts (#249). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FormLoginTest {

	private static final String CUSTOMER = "tp-login-customer";
	private static final String ADMIN = "tp-login-admin";
	private static final String LOCKED = "tp-login-locked";
	private static final String PASSWORD = "s3cret-pw";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RetailCustomerRepository repository;

	@Autowired
	private PasswordEncoder customerPasswordEncoder;

	@MockitoSpyBean
	private RetailCustomerUserDetailsService userDetailsService;

	@BeforeEach
	void createAccounts() {
		cleanUp();
		repository.save(account(CUSTOMER, RetailCustomerEntity.ROLE_USER, false));
		repository.save(account(ADMIN, RetailCustomerEntity.ROLE_CUSTODIAN, false));
		repository.save(account(LOCKED, RetailCustomerEntity.ROLE_USER, true));
	}

	@AfterEach
	void cleanUp() {
		List.of(CUSTOMER, ADMIN, LOCKED).forEach(name -> repository.findByUsername(name).ifPresent(repository::delete));
	}

	private RetailCustomerEntity account(String username, String role, boolean locked) {
		RetailCustomerEntity entity = new RetailCustomerEntity(username, "Test", "Account", username + "@example.local", role);
		entity.setPassword(customerPasswordEncoder.encode(PASSWORD));
		entity.setEnabled(Boolean.TRUE);
		entity.setAccountLocked(locked);
		return entity;
	}

	@Test
	@DisplayName("stored passwords are BCrypt hashes")
	void passwords_are_bcrypt() {
		assertThat(repository.findByUsername(CUSTOMER))
				.hasValueSatisfying(c -> assertThat(c.getPassword()).startsWith("$2").isNotEqualTo(PASSWORD));
	}

	@Test
	@DisplayName("the login page is public and is a username/password form with a CSRF token")
	void login_page_is_a_public_form() throws Exception {
		String html = mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(html)
				.contains("Login to Third Party", "gba-card-header", "action=\"/login\"", "method=\"post\"",
						"name=\"_csrf\"", "id=\"username\"", "id=\"password\"", "btn btn-primary w-100",
						"Secure Access", "Green Button Standard", "Customer Control")
				.doesNotContain("/oauth2/authorization/datacustodian\"", "Sign in with Data Custodian",
						"name=\"return_to\"");
	}

	@Test
	@DisplayName("the login page shows the error and logout messages")
	void login_page_messages() throws Exception {
		assertThat(mockMvc.perform(get("/login").param("error", "")).andReturn().getResponse().getContentAsString())
				.contains("Invalid username or password");
		assertThat(mockMvc.perform(get("/login").param("logout", "")).andReturn().getResponse().getContentAsString())
				.contains("You have been successfully logged out");
	}

	@Test
	@DisplayName("a same-origin return_to is carried in the form; an off-site one is dropped")
	void login_page_return_to() throws Exception {
		assertThat(mockMvc.perform(get("/login").param("return_to", "/custodian/home"))
				.andReturn().getResponse().getContentAsString())
				.contains("name=\"return_to\"", "value=\"/custodian/home\"");
		assertThat(mockMvc.perform(get("/login").param("return_to", "//evil.example"))
				.andReturn().getResponse().getContentAsString())
				.doesNotContain("name=\"return_to\"", "evil.example");
	}

	@Test
	@DisplayName("a TP customer (ROLE_USER) lands on the customer home with the Data Access Authorization cards")
	void customer_login_lands_on_customer_home() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login").with(csrf())
						.param("username", CUSTOMER).param("password", PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andExpect(authenticated().withUsername(CUSTOMER).withRoles("USER"))
				.andReturn().getRequest().getSession(false);

		assertThat(mockMvc.perform(get("/").session(session)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString())
				.contains("Data Access Authorization", "/oauth2/authorize/monthly", "/oauth2/authorize/daily");
	}

	@Test
	@DisplayName("a TP administrator (ROLE_CUSTODIAN) lands on the admin page")
	void admin_login_lands_on_admin_page() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login").with(csrf())
						.param("username", ADMIN).param("password", PASSWORD))
				.andExpect(redirectedUrl("/custodian/home"))
				.andExpect(authenticated().withUsername(ADMIN).withRoles("CUSTODIAN"))
				.andReturn().getRequest().getSession(false);

		assertThat(mockMvc.perform(get("/custodian/home").session(session)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString())
				.contains("Third Party Administration", ADMIN, "gba-navbar", "data-gba-app=\"thirdparty\"");
		mockMvc.perform(get("/").session(session)).andExpect(redirectedUrl("/custodian/home"));
	}

	@Test
	@DisplayName("a same-origin return_to wins over the role landing page; an off-site one is ignored")
	void login_honors_safe_return_to_only() throws Exception {
		mockMvc.perform(post("/login").with(csrf())
						.param("username", CUSTOMER).param("password", PASSWORD).param("return_to", "/about"))
				.andExpect(redirectedUrl("/about"));
		mockMvc.perform(post("/login").with(csrf())
						.param("username", CUSTOMER).param("password", PASSWORD)
						.param("return_to", "https://evil.example/"))
				.andExpect(redirectedUrl("/"));
	}

	@Test
	@DisplayName("the request the login page interrupted is resumed after login")
	void login_resumes_the_interrupted_request() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/custodian/home"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"))
				.andReturn().getRequest().getSession();

		mockMvc.perform(post("/login").session(session).with(csrf())
						.param("username", ADMIN).param("password", PASSWORD))
				.andExpect(redirectedUrl("http://localhost/custodian/home?continue"));
	}

	@Test
	@DisplayName("a wrong password, an unknown user or a locked account returns to /login?error")
	void invalid_login_fails() throws Exception {
		mockMvc.perform(post("/login").with(csrf()).param("username", CUSTOMER).param("password", "wrong"))
				.andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
		mockMvc.perform(post("/login").with(csrf()).param("username", "nobody").param("password", PASSWORD))
				.andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
		mockMvc.perform(post("/login").with(csrf()).param("username", LOCKED).param("password", PASSWORD))
				.andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
	}

	@Test
	@DisplayName("a login POST without a CSRF token is rejected with 403")
	void login_without_csrf_is_forbidden() throws Exception {
		mockMvc.perform(post("/login").param("username", CUSTOMER).param("password", PASSWORD))
				.andExpect(status().isForbidden())
				.andExpect(unauthenticated());
	}

	@Test
	@DisplayName("a TP customer may not open the admin page")
	void customer_cannot_open_admin_page() throws Exception {
		mockMvc.perform(get("/custodian/home").with(user(CUSTOMER).roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("logout is a POST with CSRF and returns to /login?logout")
	void logout_returns_to_login() throws Exception {
		mockMvc.perform(post("/logout").with(user(CUSTOMER).roles("USER")).with(csrf()))
				.andExpect(redirectedUrl("/login?logout"))
				.andExpect(unauthenticated());
	}

	@Test
	@DisplayName("a wrong password is checked once, not again by a second authentication provider")
	void wrong_password_is_checked_once() throws Exception {
		clearInvocations(userDetailsService);

		mockMvc.perform(post("/login").with(csrf()).param("username", CUSTOMER).param("password", "wrong"))
				.andExpect(redirectedUrl("/login?error"));

		verify(userDetailsService, times(1)).loadUserByUsername(CUSTOMER);
	}

	@Test
	@DisplayName("a failed Data Custodian authorization returns home with its own message, not the login error")
	void failed_dc_authorization_has_its_own_message() throws Exception {
		// No saved authorization request, so the OAuth2 callback fails (authorization_request_not_found).
		mockMvc.perform(get("/login/oauth2/code/datacustodian").with(user(CUSTOMER).roles("USER"))
						.param("error", "access_denied").param("state", "unknown"))
				.andExpect(redirectedUrl("/?authorization_error"));

		assertThat(mockMvc.perform(get("/").param("authorization_error", "").with(user(CUSTOMER).roles("USER")))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.contains("The Data Custodian authorization did not complete")
				.doesNotContain("Invalid username or password");
	}

	@Test
	@DisplayName("the H2 console requires a signed-in TP administrator and works without a CSRF token")
	void h2_console_is_administrator_only() throws Exception {
		mockMvc.perform(get("/h2-console/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
		mockMvc.perform(get("/h2-console/").with(user(CUSTOMER).roles("USER")))
				.andExpect(status().isForbidden());
		// The console is not enabled on the test profile, so an administrator gets past security
		// to a 404 rather than a 403 - including on its tokenless login POST.
		mockMvc.perform(get("/h2-console/").with(user(ADMIN).roles("CUSTODIAN")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/h2-console/login.do").with(user(ADMIN).roles("CUSTODIAN")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("the DC's Notification POST is exempt from CSRF")
	void notification_post_is_not_blocked_by_csrf() throws Exception {
		int statusCode = mockMvc.perform(post("/espi/1_1/Notification").contentType("application/atom+xml")
						.content("<BatchList xmlns=\"http://naesb.org/espi\"/>"))
				.andReturn().getResponse().getStatus();
		assertThat(statusCode).isNotEqualTo(403);
	}
}
