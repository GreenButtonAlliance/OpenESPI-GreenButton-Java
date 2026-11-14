/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
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

import org.greenbuttonalliance.espi.authserver.config.AuthorizationServerConfig;
import org.greenbuttonalliance.espi.authserver.controller.OAuthAdminController;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.greenbuttonalliance.espi.authserver.web.delegate.DelegationStateService;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.greenbuttonalliance.espi.authserver.integration.AdminMfaTestSupport.adminWithBothFactors;
import static org.greenbuttonalliance.espi.authserver.integration.AdminMfaTestSupport.adminWithPasswordOnly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GHSA-mh65-6r98-p6q8: the authorization-server admin APIs must only be reachable by an
 * ADMIN / DC_ADMIN principal.
 *
 * <p>The customer session is obtained exactly the way a real retail customer gets one: a signed
 * Return handoff from the Data Custodian is replayed at {@code /oauth2/authorize/continue}, which
 * writes a {@code ROLE_CUSTOMER} SecurityContext into the HTTP session. Before the fix, that session
 * reached every admin controller (the class-level {@code @PreAuthorize} checks were inert because
 * method security was never enabled, and the session chain only required {@code authenticated()}).</p>
 *
 * <p>Covers one read-only and one CSRF-protected state-changing endpoint on each admin controller.</p>
 */
@DisplayName("GHSA-mh65: admin API authorization (customer / anonymous / admin)")
class AdminEndpointAuthorizationIntegrationTest extends AbstractAuthserverIntegrationTest {

	private static final String REDIRECT_URI = "https://tp.example/cb";
	private static final String SCOPE = "FB_1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegisteredClientRepository registeredClientRepository;

	@Autowired
	private DelegationStateService delegationStateService;

	@Autowired
	private SignedHandoffCodec codec;

	@Autowired
	private OAuthAdminController oauthAdminController;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AuthorizationServerConfig authorizationServerConfig;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/** Client the customer flow authorizes against; also the target of the destructive admin call. */
	private String victimClientId;
	private String victimClientSecret;

	@BeforeEach
	void seedVictimClient() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		victimClientId = "ghsa-mh65-tp-" + suffix;
		victimClientSecret = "ghsa-mh65-secret-" + suffix;
		registeredClientRepository.save(RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(victimClientId)
				.clientName("GHSA-mh65 victim ThirdParty")
				.clientSecret("{noop}" + victimClientSecret)
				.clientIdIssuedAt(Instant.now())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.redirectUri(REDIRECT_URI)
				.scope(SCOPE)
				.clientSettings(ClientSettings.builder()
						.requireAuthorizationConsent(true)
						.requireProofKey(false)
						.build())
				.tokenSettings(TokenSettings.builder()
						.accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI: opaque (no JWK on the AS)
						.build())
				.build());
	}

	/** One admin call: a label for the report and a factory producing a fresh request for it. */
	record AdminCall(String label, Function<String, AbstractMockHttpServletRequestBuilder<?>> request) {
		@Override
		public String toString() {
			return label;
		}
	}

	static Stream<AdminCall> adminCalls() {
		return Stream.of(
				// OAuthAdminController (/admin/oauth2)
				new AdminCall("GET /admin/oauth2/clients",
						victim -> get("/admin/oauth2/clients")),
				new AdminCall("DELETE /admin/oauth2/clients/{victim} (csrf)",
						victim -> delete("/admin/oauth2/clients/" + victim).with(csrf())),
				// OAuth2ClientManagementController (/api/v1/oauth2/clients)
				new AdminCall("GET /api/v1/oauth2/clients",
						victim -> get("/api/v1/oauth2/clients")),
				new AdminCall("POST /api/v1/oauth2/clients (csrf)",
						victim -> post("/api/v1/oauth2/clients").with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"client_name":"ghsa-mh65 attacker client",
										 "grant_types":["client_credentials"],
										 "scopes":["DataCustodian_Admin_Access"],
										 "require_proof_key":false,
										 "require_authorization_consent":false}
										""")),
				// CertificateManagementController (/api/v1/certificates)
				new AdminCall("GET /api/v1/certificates/expiring",
						victim -> get("/api/v1/certificates/expiring")),
				new AdminCall("POST /api/v1/certificates/validate (csrf)",
						victim -> multipart("/api/v1/certificates/validate")
								.file(new MockMultipartFile("certificate", "c.pem",
										"application/x-pem-file", new byte[0]))
								.with(csrf())),
				// DataCustodianIntegrationController (/api/v1/datacustodian)
				new AdminCall("GET /api/v1/datacustodian/mappings/{id}",
						victim -> get("/api/v1/datacustodian/mappings/ghsa-mh65-missing")),
				new AdminCall("DELETE /api/v1/datacustodian/grants/{id} (csrf)",
						victim -> delete("/api/v1/datacustodian/grants/ghsa-mh65-grant").with(csrf())),
				// /connect/** — handler removed in #196 (RFC 7591 /oauth2/register replaces it); the
				// admin-only rule is kept as defense in depth, so a customer still gets 403, not 404
				new AdminCall("POST /connect/register (csrf)",
						victim -> registerClientRequest("ghsa-mh65 registered client")));
	}

	private static AbstractMockHttpServletRequestBuilder<?> registerClientRequest(String clientName) {
		return post("/connect/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"client_name":"%s",
						 "redirect_uris":["https://attacker.example/cb"],
						 "grant_types":["authorization_code"],
						 "scope":"FB_1"}
						""".formatted(clientName));
	}

	private Integer countClientsNamed(String clientName) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM oauth2_registered_client WHERE client_name = ?",
				Integer.class, clientName);
	}

	@Test
	@DisplayName("a customer session cannot register an OAuth2 client via /connect/register")
	void customerSessionCannotRegisterClient() throws Exception {
		String clientName = "ghsa-mh65 customer-registered " + UUID.randomUUID();
		MockHttpSession customerSession = establishCustomerSession();

		mockMvc.perform(registerClientRequest(clientName).session(customerSession))
				.andExpect(status().isForbidden());

		assertThat(countClientsNamed(clientName)).as("no client may be created").isZero();
	}

	@Test
	@DisplayName("an anonymous /connect/register is redirected to /login and creates nothing")
	void anonymousCannotRegisterClient() throws Exception {
		String clientName = "ghsa-mh65 anonymous-registered " + UUID.randomUUID();

		mockMvc.perform(registerClientRequest(clientName))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));

		assertThat(countClientsNamed(clientName)).as("no client may be created").isZero();
	}

	@ParameterizedTest(name = "customer session -> 403: {0}")
	@MethodSource("adminCalls")
	@DisplayName("a delegated retail-customer session (ROLE_CUSTOMER) is forbidden from admin APIs")
	void customerSessionIsForbidden(AdminCall call) throws Exception {
		MockHttpSession customerSession = establishCustomerSession();

		int status = mockMvc.perform(call.request().apply(victimClientId).session(customerSession))
				.andReturn().getResponse().getStatus();

		assertThat(status).as("%s as ROLE_CUSTOMER", call.label()).isEqualTo(403);
		assertThat(registeredClientRepository.findByClientId(victimClientId))
				.as("victim client must survive a customer's admin call")
				.isNotNull();
	}

	@ParameterizedTest(name = "anonymous -> 401/login: {0}")
	@MethodSource("adminCalls")
	@DisplayName("an anonymous caller is challenged on admin APIs")
	void anonymousIsChallenged(AdminCall call) throws Exception {
		MvcResult result = mockMvc.perform(call.request().apply(victimClientId)).andReturn();

		assertThat(result.getResponse())
				.as("%s anonymously", call.label())
				.satisfiesAnyOf(
						response -> assertThat(response.getStatus()).isEqualTo(401),
						response -> assertThat(response)
								.extracting(MockHttpServletResponse::getStatus, MockHttpServletResponse::getRedirectedUrl)
								.containsExactly(302, "/login"));
	}

	@ParameterizedTest(name = "ADMIN (password + passkey) -> allowed: {0}")
	@MethodSource("adminCalls")
	@DisplayName("an ADMIN principal passes both the URL rule and the @PreAuthorize check")
	void adminIsAllowed(AdminCall call) throws Exception {
		int status = mockMvc.perform(call.request().apply(victimClientId)
						.with(adminWithBothFactors("ghsa-admin", "ROLE_ADMIN")))
				.andReturn().getResponse().getStatus();

		assertThat(status).as("%s as ROLE_ADMIN", call.label()).isNotIn(401, 403, 302);
	}

	@ParameterizedTest(name = "DC_ADMIN (password + passkey) -> allowed: {0}")
	@MethodSource("adminCalls")
	@DisplayName("a DC_ADMIN principal passes both the URL rule and the @PreAuthorize check")
	void dcAdminIsAllowed(AdminCall call) throws Exception {
		int status = mockMvc.perform(call.request().apply(victimClientId)
						.with(adminWithBothFactors("ghsa-dc-admin", "ROLE_DC_ADMIN")))
				.andReturn().getResponse().getStatus();

		assertThat(status).as("%s as ROLE_DC_ADMIN", call.label()).isNotIn(401, 403, 302);
	}

	@ParameterizedTest(name = "ADMIN password-only -> passkey step: {0}")
	@MethodSource("adminCalls")
	@DisplayName("#205: an admin session without the passkey factor is sent to the WebAuthn login step")
	void passwordOnlyAdminIsSentToPasskeyStep(AdminCall call) throws Exception {
		MvcResult result = mockMvc.perform(call.request().apply(victimClientId)
						.with(adminWithPasswordOnly("ghsa-admin", "ROLE_ADMIN")))
				.andReturn();

		assertThat(result.getResponse())
				.as("%s as ROLE_ADMIN with FACTOR_PASSWORD only", call.label())
				.extracting(MockHttpServletResponse::getStatus, MockHttpServletResponse::getRedirectedUrl)
				.containsExactly(302, "/login?factor.type=webauthn&factor.reason=missing");
		assertThat(registeredClientRepository.findByClientId(victimClientId))
				.as("no admin effect without the second factor")
				.isNotNull();
	}

	@Test
	@DisplayName("#204: with no admin-client secret configured, espi_admin is not seeded")
	void adminClientNotSeededWithoutSecret() {
		// The singleton MySQL container is shared with the bearer-token IT context (which does
		// configure a secret), so clear any row it left and re-run THIS context's seeding step.
		jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", "espi_admin");

		authorizationServerConfig.seedAdminClient(registeredClientRepository, passwordEncoder);

		assertThat(registeredClientRepository.findByClientId("espi_admin")).isNull();
	}

	@Test
	@DisplayName("@PreAuthorize is enforced on the admin controllers (method security enabled)")
	void methodSecurityIsEnforcedIndependentlyOfUrlRules() {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
				"42", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
		SecurityContextHolder.setContext(context);
		try {
			assertThatThrownBy(() -> oauthAdminController.listClients())
					.isInstanceOf(AuthorizationDeniedException.class);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	@DisplayName("ADMIN delete actually removes the client (the rule gates a real effect)")
	void adminDeleteRemovesClient() throws Exception {
		mockMvc.perform(delete("/admin/oauth2/clients/" + victimClientId).with(csrf())
						.with(adminWithBothFactors("ghsa-admin", "ROLE_ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("deleted"));

		assertThat(registeredClientRepository.findByClientId(victimClientId)).isNull();
	}

	@Test
	@DisplayName("the delegated customer flow still authenticates the session and resumes /oauth2/authorize")
	void delegatedCustomerFlowStillWorks() throws Exception {
		MockHttpSession session = establishCustomerSession();

		SecurityContext context = (SecurityContext) session.getAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		assertThat(context)
				.isNotNull()
				.extracting(SecurityContext::getAuthentication)
				.satisfies(auth -> assertThat(auth.getAuthorities())
						.extracting(Object::toString)
						.containsExactly("ROLE_CUSTOMER"));
	}

	@Test
	@DisplayName("client_credentials token + introspection still work (protocol chain untouched)")
	void tokenAndIntrospectionStillWork() throws Exception {
		MvcResult token = mockMvc.perform(post("/oauth2/token")
						.with(httpBasic(victimClientId, victimClientSecret))
						.param("grant_type", "client_credentials")
						.param("scope", SCOPE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").isNotEmpty())
				.andReturn();
		String accessToken = new tools.jackson.databind.ObjectMapper()
				.readTree(token.getResponse().getContentAsString()).get("access_token").asString();

		mockMvc.perform(post("/oauth2/introspect")
						.with(httpBasic(victimClientId, victimClientSecret))
						.param("token", accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));
	}

	/**
	 * Replay a signed DC Return handoff at {@code /oauth2/authorize/continue}; the controller writes a
	 * ROLE_CUSTOMER SecurityContext into the session (AuthorizeContinueController#authenticateCustomer).
	 */
	private MockHttpSession establishCustomerSession() throws Exception {
		String correlationId = "corr-" + UUID.randomUUID();
		delegationStateService.save(correlationId, victimClientId, REDIRECT_URI, "tp-state");
		Instant now = Instant.now();
		String handoff = codec.encode(SignedHandoff.Return.of(
				correlationId, now, now.plusSeconds(300),
				UUID.randomUUID().toString().replace("-", ""),
				"42",
				List.of(UUID.randomUUID()),
				SignedHandoff.Return.CONSENT_ALLOW,
				SCOPE));

		MvcResult result = mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", handoff))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		return session;
	}
}
