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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link CustomerLoginSecurityConfiguration}'s SecurityFilterChain — the
 * customer / custodian form login surface. Covers: unauthenticated GET renders the form, good
 * customer / admin POSTs authenticate, bad creds fail, CSRF is required, the {@code return_to}
 * parameter is honored on success.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerLoginSecurityConfigurationTest {

	private static final String ADMIN_USERNAME = "test-admin";
	private static final String CUSTOMER_USERNAME = "test-customer";
	private static final String PASSWORD = "secret123";

	@Autowired private MockMvc mockMvc;
	@Autowired private RetailCustomerRepository repository;
	@Autowired private PasswordEncoder customerPasswordEncoder;

	@BeforeEach
	void seedTestUsers() {
		if (repository.findByUsername(ADMIN_USERNAME).isEmpty()) {
			repository.save(buildUser(ADMIN_USERNAME, RetailCustomerEntity.ROLE_CUSTODIAN));
		}
		if (repository.findByUsername(CUSTOMER_USERNAME).isEmpty()) {
			repository.save(buildUser(CUSTOMER_USERNAME, RetailCustomerEntity.ROLE_USER));
		}
	}

	@Test
	void unauthenticated_get_login_renders_form() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"username\"")))
				.andExpect(content().string(containsString("name=\"password\"")));
	}

	@Test
	void good_admin_credentials_authenticate() throws Exception {
		// Note: Spring Security 7 augments the Authentication with a FactorGrantedAuthority
		// (FACTOR_PASSWORD) beyond the role authorities. Don't assert exact authorities; verify
		// authenticated state + redirect, and assert the role-authority membership separately.
		mockMvc.perform(formLogin("/login").user(ADMIN_USERNAME).password(PASSWORD))
				.andExpect(authenticated().withUsername(ADMIN_USERNAME))
				.andExpect(authenticated().withAuthentication(auth ->
						org.assertj.core.api.Assertions.assertThat(auth.getAuthorities())
								.extracting(Object::toString)
								.contains(RetailCustomerEntity.ROLE_CUSTODIAN)))
				.andExpect(redirectedUrl("/custodian/home"));
	}

	@Test
	void good_customer_credentials_authenticate() throws Exception {
		mockMvc.perform(formLogin("/login").user(CUSTOMER_USERNAME).password(PASSWORD))
				.andExpect(authenticated().withUsername(CUSTOMER_USERNAME))
				.andExpect(redirectedUrl("/custodian/home"));
	}

	@Test
	void bad_credentials_fail() throws Exception {
		mockMvc.perform(formLogin("/login").user(ADMIN_USERNAME).password("wrong"))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void post_login_without_csrf_is_rejected() throws Exception {
		mockMvc.perform(post("/login")
						.param("username", ADMIN_USERNAME)
						.param("password", PASSWORD))
				.andExpect(status().isForbidden())
				.andExpect(unauthenticated());
	}

	@Test
	void return_to_path_honored_on_successful_authentication() throws Exception {
		mockMvc.perform(post("/login")
						.with(csrf())
						.param("username", ADMIN_USERNAME)
						.param("password", PASSWORD)
						.param("return_to", "/custodian/retailcustomers/form"))
				.andExpect(authenticated())
				.andExpect(redirectedUrl("/custodian/retailcustomers/form"));
	}

	@Test
	void return_to_open_redirect_is_rejected() throws Exception {
		mockMvc.perform(post("/login")
						.with(csrf())
						.param("username", ADMIN_USERNAME)
						.param("password", PASSWORD)
						.param("return_to", "//evil.example.com/steal"))
				.andExpect(authenticated())
				.andExpect(redirectedUrl("/custodian/home"));
	}

	private RetailCustomerEntity buildUser(String username, String role) {
		RetailCustomerEntity u = new RetailCustomerEntity(username, "First", "Last");
		u.setPassword(customerPasswordEncoder.encode(PASSWORD));
		u.setRole(role);
		u.setEnabled(Boolean.TRUE);
		u.setAccountLocked(Boolean.FALSE);
		return u;
	}
}
