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

package org.greenbuttonalliance.espi.authserver.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.greenbuttonalliance.espi.authserver.admin.AdminUser;
import org.greenbuttonalliance.espi.authserver.admin.AdminUserBootstrap;
import org.greenbuttonalliance.espi.authserver.admin.AdminUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.greenbuttonalliance.espi.authserver.integration.AdminMfaTestSupport.adminWithBothFactors;
import static org.greenbuttonalliance.espi.authserver.integration.AdminMfaTestSupport.adminWithPasswordOnly;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #205: admin user store, env bootstrap, form login + passkey MFA scoped to the admin paths, and the
 * {@code /api/v1/admin/users} API.
 *
 * <p>The passkey factor cannot be produced by a real WebAuthn ceremony in MockMvc, so it is simulated
 * by an authentication carrying {@code FactorGrantedAuthority} values. The password factor is
 * exercised for real through {@code POST /login}.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("#205: admin user store, bootstrap, password + passkey MFA, admin-user API")
class AdminUserStoreIntegrationTest extends AbstractAuthserverIntegrationTest {

	private static final String BOOTSTRAP_USER = "root-admin";
	private static final String BOOTSTRAP_PASSWORD = "bootstrap-password-205";
	private static final String BOOTSTRAP_HASH = new BCryptPasswordEncoder().encode(BOOTSTRAP_PASSWORD);
	private static final String ADMIN_CLIENT_SECRET = "it-espi-admin-secret-205";

	@DynamicPropertySource
	static void adminProperties(DynamicPropertyRegistry registry) {
		// Bare $2a$ hash (no {bcrypt} prefix) to prove the bootstrap normalizes it.
		registry.add("espi.authserver.admin.bootstrap.username", () -> BOOTSTRAP_USER);
		registry.add("espi.authserver.admin.bootstrap.password-hash", () -> BOOTSTRAP_HASH);
		registry.add("espi.authserver.admin-client.secret", () -> ADMIN_CLIENT_SECRET);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminUserBootstrap adminUserBootstrap;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PublicKeyCredentialUserEntityRepository userEntities;

	@Autowired
	private UserCredentialRepository userCredentials;

	@Autowired
	private org.greenbuttonalliance.espi.authserver.config.AuthorizationServerConfig authorizationServerConfig;

	@Autowired
	private org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository registeredClientRepository;

	@Autowired
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/** Restore the post-startup state: only the bootstrapped admin, no passkeys. */
	@AfterEach
	void resetToBootstrapState() {
		jdbcTemplate.update("DELETE FROM user_credentials");
		jdbcTemplate.update("DELETE FROM user_entities");
		jdbcTemplate.update("DELETE FROM admin_user_roles");
		jdbcTemplate.update("DELETE FROM admin_users");
		adminUserBootstrap.bootstrap();
	}

	// ---------------------------------------------------------------- bootstrap

	@Test
	@Order(1)
	@DisplayName("startup bootstrap created the first admin (ROLE_ADMIN, {bcrypt}-prefixed configured hash)")
	void startupBootstrapCreatedFirstAdmin() {
		assertThat(adminUserRepository.findAll())
				.singleElement()
				.satisfies(user -> assertThat(user)
						.extracting(AdminUser::username, AdminUser::enabled, AdminUser::roles)
						.containsExactly(BOOTSTRAP_USER, true, java.util.Set.of("ROLE_ADMIN")));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT password_hash FROM admin_users WHERE username = ?", String.class, BOOTSTRAP_USER))
				.isEqualTo("{bcrypt}" + BOOTSTRAP_HASH);
	}

	@Test
	@DisplayName("bootstrap does nothing when admins already exist")
	void bootstrapIsNoOpWhenAdminsExist() {
		assertThat(adminUserBootstrap.bootstrap()).isFalse();
		assertThat(adminUserRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("bootstrap creates the admin when the table is empty")
	void bootstrapCreatesWhenEmpty() {
		jdbcTemplate.update("DELETE FROM admin_user_roles");
		jdbcTemplate.update("DELETE FROM admin_users");

		assertThat(adminUserBootstrap.bootstrap()).isTrue();
		assertThat(adminUserRepository.findAll()).extracting(AdminUser::username).containsExactly(BOOTSTRAP_USER);
	}

	@Test
	@DisplayName("bootstrap rejects a plaintext (non-bcrypt) password hash and inserts nothing")
	void bootstrapRejectsPlaintext() {
		jdbcTemplate.update("DELETE FROM admin_user_roles");
		jdbcTemplate.update("DELETE FROM admin_users");
		AdminUserBootstrap plaintext = new AdminUserBootstrap(adminUserRepository, "plain-admin", "hunter2-plaintext");
		AdminUserBootstrap otherScheme = new AdminUserBootstrap(adminUserRepository, "plain-admin", "{noop}hunter2-plaintext");

		assertThatThrownBy(plaintext::bootstrap)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("bcrypt");
		assertThatThrownBy(otherScheme::bootstrap).isInstanceOf(IllegalStateException.class);
		assertThat(adminUserRepository.count()).isZero();
	}

	// ---------------------------------------------------------------- MFA on the session chain

	@Test
	@DisplayName("real password login without a passkey -> sent to passkey registration; admin API needs the passkey step")
	void passwordLoginWithoutPasskeyOnlyReachesRegistration() throws Exception {
		MvcResult login = mockMvc.perform(post("/login").with(csrf())
						.param("username", BOOTSTRAP_USER).param("password", BOOTSTRAP_PASSWORD))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/webauthn/register"))
				.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		SecurityContext context = (SecurityContext) session.getAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		assertThat(context.getAuthentication().getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.contains("ROLE_ADMIN", "FACTOR_PASSWORD")
				.doesNotContain("FACTOR_WEBAUTHN");

		mockMvc.perform(get("/webauthn/register").session(session))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/admin/users").session(session))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login?factor.type=webauthn&factor.reason=missing"));
		mockMvc.perform(get("/admin/oauth2/clients").session(session))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login?factor.type=webauthn&factor.reason=missing"));

		assertThat(adminUserRepository.findAll())
				.singleElement()
				.extracting(AdminUser::lastLoginAt)
				.isNotNull();
	}

	@Test
	@DisplayName("password login of an admin WITH a passkey resumes normally (no forced registration)")
	void passwordLoginWithPasskeyDoesNotForceRegistration() throws Exception {
		registerFakePasskey(BOOTSTRAP_USER);

		mockMvc.perform(post("/login").with(csrf())
						.param("username", BOOTSTRAP_USER).param("password", BOOTSTRAP_PASSWORD))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	@DisplayName("a retail-customer id cannot password-login on the AS (admin store only)")
	void customerCannotFormLogin() throws Exception {
		mockMvc.perform(post("/login").with(csrf()).param("username", "42").param("password", "anything-at-all"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	@DisplayName("password-only (simulated) -> passkey step; both factors -> admin-user API 200, no hashes")
	void bothFactorsRequired() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users").with(adminWithPasswordOnly(BOOTSTRAP_USER, "ROLE_ADMIN")))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login?factor.type=webauthn&factor.reason=missing"));

		MvcResult result = mockMvc.perform(get("/api/v1/admin/users").with(adminWithBothFactors(BOOTSTRAP_USER, "ROLE_ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value(BOOTSTRAP_USER))
				.andReturn();
		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("password")
				.doesNotContain("$2a$");
	}

	@Test
	@DisplayName("a passkey-only session (no password factor) is also refused")
	void passkeyOnlyIsRefused() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users").with(AdminMfaTestSupport.adminWithFactors(
						BOOTSTRAP_USER, "ROLE_ADMIN", "FACTOR_WEBAUTHN")))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login?factor.type=password&factor.reason=missing"));
	}

	@Test
	@DisplayName("a delegated customer session (ROLE_CUSTOMER) -> 403 on the admin-user API, not a passkey prompt")
	void customerSessionIsForbidden() throws Exception {
		List<GrantedAuthority> customer = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
		mockMvc.perform(get("/api/v1/admin/users")
						.with(authentication(UsernamePasswordAuthenticationToken.authenticated("42", null, customer))))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("DC_ADMIN with both factors passes the URL rule but not the ROLE_ADMIN-only user API")
	void dcAdminCannotManageAdmins() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users").with(adminWithBothFactors("dc-ops", "ROLE_DC_ADMIN")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("anonymous -> login redirect")
	void anonymousIsRedirected() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@DisplayName("espi_admin bearer token -> admin-user API allowed, no MFA, no CSRF")
	void bearerTokenBypassesMfa() throws Exception {
		authorizationServerConfig.seedAdminClient(registeredClientRepository, passwordEncoder);
		String token = bearerToken();

		mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("username", "ops-bearer", "password", "a-long-enough-pw"))))
				.andExpect(status().isCreated());
	}

	// ---------------------------------------------------------------- admin-user API

	@Test
	@DisplayName("create: hashed server-side, never returned, policy enforced, duplicates rejected")
	void createUser() throws Exception {
		mockMvc.perform(post("/api/v1/admin/users").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("username", "alice", "password", "correct-horse-battery",
								"roles", List.of("DC_ADMIN")))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_DC_ADMIN"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT password_hash FROM admin_users WHERE username = 'alice'", String.class))
				.startsWith("{bcrypt}")
				.doesNotContain("correct-horse-battery");

		mockMvc.perform(createRequest("bob", "too-short"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(createRequest("12345", "long-enough-password"))
				.andExpect(status().isBadRequest()); // numeric = DC customer-principal shape
		mockMvc.perform(createRequest("alice", "another-long-password"))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/v1/admin/users").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("username", "carol", "password", "long-enough-password",
								"roles", List.of("ROLE_CUSTOMER")))))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("last-admin and self guards: cannot disable/delete/demote self or the last enabled admin")
	void lastAdminAndSelfGuards() throws Exception {
		String rootId = idOf(BOOTSTRAP_USER);

		// self (session admin = root-admin)
		mockMvc.perform(put("/api/v1/admin/users/" + rootId + "/enabled").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isConflict());
		mockMvc.perform(delete("/api/v1/admin/users/" + rootId).with(csrf()).with(rootAdmin()))
				.andExpect(status().isConflict());

		// last enabled admin, acted on by a different principal (the bearer client)
		authorizationServerConfig.seedAdminClient(registeredClientRepository, passwordEncoder);
		String token = bearerToken();
		mockMvc.perform(put("/api/v1/admin/users/" + rootId + "/enabled").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isConflict());
		mockMvc.perform(put("/api/v1/admin/users/" + rootId + "/roles").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"DC_ADMIN\"]}"))
				.andExpect(status().isConflict());
		mockMvc.perform(delete("/api/v1/admin/users/" + rootId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isConflict());

		// with a second enabled admin, root-admin can disable and re-enable the other one
		mockMvc.perform(createRequest("second-admin", "second-admin-password")).andExpect(status().isCreated());
		String secondId = idOf("second-admin");
		mockMvc.perform(put("/api/v1/admin/users/" + secondId + "/enabled").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));
		// ...and now root-admin is the last enabled admin again
		mockMvc.perform(put("/api/v1/admin/users/" + rootId + "/enabled").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isConflict());
		mockMvc.perform(put("/api/v1/admin/users/" + secondId + "/enabled").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/v1/admin/users/" + secondId).with(csrf()).with(rootAdmin()))
				.andExpect(status().isNoContent());
		assertThat(adminUserRepository.existsByUsername("second-admin")).isFalse();
	}

	@Test
	@DisplayName("a disabled admin cannot password-login")
	void disabledAdminCannotLogin() throws Exception {
		mockMvc.perform(createRequest("disabled-admin", "disabled-admin-password")).andExpect(status().isCreated());
		mockMvc.perform(put("/api/v1/admin/users/" + idOf("disabled-admin") + "/enabled").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/login").with(csrf())
						.param("username", "disabled-admin").param("password", "disabled-admin-password"))
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	@DisplayName("reset password: new password works at /login, old one does not; policy enforced")
	void resetPassword() throws Exception {
		String rootId = idOf(BOOTSTRAP_USER);
		mockMvc.perform(put("/api/v1/admin/users/" + rootId + "/password").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"short\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/v1/admin/users/" + rootId + "/password").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"a-brand-new-password\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/login").with(csrf()).param("username", BOOTSTRAP_USER).param("password", BOOTSTRAP_PASSWORD))
				.andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(post("/login").with(csrf()).param("username", BOOTSTRAP_USER).param("password", "a-brand-new-password"))
				.andExpect(redirectedUrl("/webauthn/register"));
	}

	@Test
	@DisplayName("reset passkeys deletes the user's WebAuthn credentials and user entity")
	void resetPasskeys() throws Exception {
		registerFakePasskey(BOOTSTRAP_USER);
		PublicKeyCredentialUserEntity entity = userEntities.findByUsername(BOOTSTRAP_USER);
		assertThat(userCredentials.findByUserId(entity.getId())).hasSize(1);

		mockMvc.perform(delete("/api/v1/admin/users/" + idOf(BOOTSTRAP_USER) + "/passkeys").with(csrf()).with(rootAdmin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.removed").value(1));

		assertThat(userCredentials.findByUserId(entity.getId())).isEmpty();
		assertThat(userEntities.findByUsername(BOOTSTRAP_USER)).isNull();
	}

	@Test
	@DisplayName("set roles normalizes ADMIN/DC_ADMIN and rejects an empty set")
	void setRoles() throws Exception {
		mockMvc.perform(createRequest("role-admin", "role-admin-password")).andExpect(status().isCreated());
		String id = idOf("role-admin");

		mockMvc.perform(put("/api/v1/admin/users/" + id + "/roles").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"admin\",\"dc_admin\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles.length()").value(2));
		mockMvc.perform(put("/api/v1/admin/users/" + id + "/roles").with(csrf()).with(rootAdmin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("unknown user id -> 404")
	void unknownUser() throws Exception {
		mockMvc.perform(delete("/api/v1/admin/users/does-not-exist/passkeys").with(csrf()).with(rootAdmin()))
				.andExpect(status().isNotFound());
	}

	// ---------------------------------------------------------------- unaffected flows

	@Test
	@DisplayName("protocol chain unaffected: client_credentials token + introspection need no MFA")
	void protocolEndpointsUnaffected() throws Exception {
		MvcResult token = mockMvc.perform(post("/oauth2/token").with(httpBasic("data_custodian_admin", "dc-secret"))
						.param("grant_type", "client_credentials").param("scope", "DataCustodian_Admin_Access"))
				.andExpect(status().isOk())
				.andReturn();
		String value = objectMapper.readTree(token.getResponse().getContentAsString()).get("access_token").asText();

		mockMvc.perform(post("/oauth2/introspect").with(httpBasic("data_custodian_admin", "dc-secret")).param("token", value))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));
	}

	// ---------------------------------------------------------------- helpers

	private org.springframework.test.web.servlet.request.RequestPostProcessor rootAdmin() {
		return adminWithBothFactors(BOOTSTRAP_USER, "ROLE_ADMIN");
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createRequest(
			String username, String password) throws Exception {
		return post("/api/v1/admin/users").with(csrf()).with(rootAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("username", username, "password", password)));
	}

	private String idOf(String username) {
		return adminUserRepository.findAll().stream()
				.filter(user -> user.username().equals(username))
				.map(AdminUser::id)
				.findFirst()
				.orElseThrow();
	}

	private String bearerToken() throws Exception {
		MvcResult result = mockMvc.perform(post("/oauth2/token").with(httpBasic("espi_admin", ADMIN_CLIENT_SECRET))
						.param("grant_type", "client_credentials").param("scope", "authserver.admin"))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
	}

	private void registerFakePasskey(String username) {
		PublicKeyCredentialUserEntity entity = ImmutablePublicKeyCredentialUserEntity.builder()
				.name(username).id(Bytes.random()).displayName(username).build();
		userEntities.save(entity);
		userCredentials.save(ImmutableCredentialRecord.builder()
				.credentialId(Bytes.random())
				.userEntityUserId(entity.getId())
				.publicKey(new ImmutablePublicKeyCose(new byte[] {1, 2, 3}))
				.credentialType(PublicKeyCredentialType.PUBLIC_KEY)
				.signatureCount(0)
				.uvInitialized(true)
				.backupEligible(false)
				.backupState(false)
				.transports(java.util.Set.of())
				.created(Instant.now())
				.lastUsed(Instant.now())
				.label("test passkey")
				.build());
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
