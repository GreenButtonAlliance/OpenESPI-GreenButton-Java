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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.greenbuttonalliance.espi.authserver.config.AuthorizationServerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #204: stateless client_credentials bearer-token access to the AS admin APIs, with the opaque
 * token resolved in-process and only the dedicated {@code authserver.admin} scope granting ROLE_ADMIN.
 */
@TestPropertySource(properties = "espi.authserver.admin-client.secret=" + AdminBearerTokenIntegrationTest.ADMIN_SECRET)
@DisplayName("#204: admin API bearer-token authentication (espi_admin, authserver.admin)")
class AdminBearerTokenIntegrationTest extends AbstractAuthserverIntegrationTest {

	static final String ADMIN_SECRET = "it-espi-admin-secret-204";
	private static final String ADMIN_CLIENT = "espi_admin";
	private static final String ADMIN_SCOPE = "authserver.admin";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegisteredClientRepository registeredClientRepository;

	@Autowired
	private OAuth2AuthorizationService authorizationService;

	@Autowired
	private AuthorizationServerConfig authorizationServerConfig;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("espi_admin is seeded: client_credentials only, authserver.admin, opaque, 15 min, hashed secret")
	void adminClientIsSeededFromConfiguration() {
		ensureAdminClientSeeded();

		RegisteredClient admin = registeredClientRepository.findByClientId(ADMIN_CLIENT);
		assertThat(admin).isNotNull().satisfies(client -> {
			assertThat(client.getAuthorizationGrantTypes()).containsExactly(AuthorizationGrantType.CLIENT_CREDENTIALS);
			assertThat(client.getScopes()).containsExactly(ADMIN_SCOPE);
			assertThat(client.getTokenSettings())
					.extracting(ts -> ts.getAccessTokenFormat(), ts -> ts.getAccessTokenTimeToLive())
					.containsExactly(OAuth2TokenFormat.REFERENCE, Duration.ofMinutes(15));
			assertThat(client.getClientSecret())
					.startsWith("{bcrypt}")
					.doesNotContain(ADMIN_SECRET)
					.satisfies(hash -> assertThat(passwordEncoder.matches(ADMIN_SECRET, hash)).isTrue());
		});
	}

	@Test
	@DisplayName("re-running the seeding step does not re-hash an unchanged secret")
	void reseedingDoesNotRewriteSecret() {
		ensureAdminClientSeeded();
		String before = registeredClientRepository.findByClientId(ADMIN_CLIENT).getClientSecret();

		authorizationServerConfig.seedAdminClient(registeredClientRepository, passwordEncoder);

		assertThat(registeredClientRepository.findByClientId(ADMIN_CLIENT).getClientSecret()).isEqualTo(before);
	}

	@Test
	@DisplayName("espi_admin token -> GET /admin/oauth2/clients 200 and DELETE works without CSRF")
	void adminTokenReachesAdminApisWithoutCsrf() throws Exception {
		String token = adminToken();
		String victim = saveVictimClient();

		mockMvc.perform(get("/admin/oauth2/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Set-Cookie"));

		mockMvc.perform(delete("/admin/oauth2/clients/" + victim).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("deleted"));

		assertThat(registeredClientRepository.findByClientId(victim)).isNull();
	}

	@Test
	@DisplayName("espi_admin token passes the @PreAuthorize check on every admin controller")
	void adminTokenPassesMethodSecurityOnEachController() throws Exception {
		String token = adminToken();
		for (String path : new String[] {
				"/api/v1/certificates/expiring",
				"/api/v1/datacustodian/mappings/ghsa-mh65-missing"}) {
			int status = mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
					.andReturn().getResponse().getStatus();
			assertThat(status).as(path).isNotIn(401, 403);
		}
	}

	@Test
	@DisplayName("a valid token without authserver.admin (data_custodian_admin) -> 403 insufficient_scope")
	void tokenWithoutAdminScopeIsForbidden() throws Exception {
		String token = obtainToken("data_custodian_admin", "dc-secret", "DataCustodian_Admin_Access");

		mockMvc.perform(get("/admin/oauth2/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")));
	}

	@Test
	@DisplayName("an unknown token -> 401 with WWW-Authenticate: Bearer")
	void unknownTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/admin/oauth2/clients").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
	}

	@Test
	@DisplayName("an expired authserver.admin token -> 401")
	void expiredTokenIsUnauthorized() throws Exception {
		ensureAdminClientSeeded();
		RegisteredClient admin = registeredClientRepository.findByClientId(ADMIN_CLIENT);
		String value = "expired-" + UUID.randomUUID();
		Instant issued = Instant.now().minus(Duration.ofHours(1));
		authorizationService.save(OAuth2Authorization.withRegisteredClient(admin)
				.principalName(ADMIN_CLIENT)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.authorizedScopes(Set.of(ADMIN_SCOPE))
				.token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, value,
						issued, issued.plus(Duration.ofMinutes(15)), Set.of(ADMIN_SCOPE)))
				.build());

		mockMvc.perform(get("/admin/oauth2/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + value))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
	}

	@Test
	@DisplayName("a revoked authserver.admin token -> 401")
	void revokedTokenIsUnauthorized() throws Exception {
		String token = adminToken();
		mockMvc.perform(post("/oauth2/revoke").with(httpBasic(ADMIN_CLIENT, ADMIN_SECRET)).param("token", token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/admin/oauth2/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("no bearer header -> the session chain still applies (anonymous -> /login)")
	void noBearerFallsThroughToSessionChain() throws Exception {
		mockMvc.perform(get("/admin/oauth2/clients"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@DisplayName("the admin token still introspects at /oauth2/introspect")
	void adminTokenIntrospects() throws Exception {
		String token = adminToken();

		mockMvc.perform(post("/oauth2/introspect").with(httpBasic(ADMIN_CLIENT, ADMIN_SECRET)).param("token", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.scope").value(ADMIN_SCOPE));
	}

	@Test
	@DisplayName("actuator: health and its probe groups are anonymous")
	void actuatorHealthIsAnonymous() throws Exception {
		for (String path : new String[] {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
			mockMvc.perform(get(path))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("UP"));
		}
	}

	@Test
	@DisplayName("actuator: info and metrics -> 401 without a token, 403 without authserver.admin, 200 with it")
	void actuatorNonHealthEndpointsRequireAdminToken() throws Exception {
		String dcToken = obtainToken("data_custodian_admin", "dc-secret", "DataCustodian_Admin_Access");
		String adminToken = adminToken();

		for (String path : new String[] {"/actuator", "/actuator/info", "/actuator/metrics"}) {
			mockMvc.perform(get(path))
					.andExpect(status().isUnauthorized());
			mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + dcToken))
					.andExpect(status().isForbidden());
			mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
					.andExpect(status().isOk());
		}
	}

	private String adminToken() throws Exception {
		ensureAdminClientSeeded();
		return obtainToken(ADMIN_CLIENT, ADMIN_SECRET, ADMIN_SCOPE);
	}

	private String obtainToken(String clientId, String secret, String scope) throws Exception {
		MvcResult result = mockMvc.perform(post("/oauth2/token")
						.with(httpBasic(clientId, secret))
						.param("grant_type", "client_credentials")
						.param("scope", scope))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = new ObjectMapper().readTree(result.getResponse().getContentAsString());
		assertThat(body.get("expires_in").asLong()).isPositive();
		return body.get("access_token").asString();
	}

	/**
	 * The MySQL container is shared with the no-secret IT context, which may delete the espi_admin
	 * row; re-run this context's (secret-configured) seeding step so each test is order-independent.
	 */
	private void ensureAdminClientSeeded() {
		authorizationServerConfig.seedAdminClient(registeredClientRepository, passwordEncoder);
	}

	private String saveVictimClient() {
		String clientId = "pr204-victim-" + UUID.randomUUID().toString().substring(0, 8);
		registeredClientRepository.save(RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(clientId)
				.clientSecret("{noop}x")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build());
		return clientId;
	}
}
