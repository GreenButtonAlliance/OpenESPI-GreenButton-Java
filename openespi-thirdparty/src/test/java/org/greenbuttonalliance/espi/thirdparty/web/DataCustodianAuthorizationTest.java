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

import com.sun.net.httpserver.HttpServer;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.thirdparty.oauth.DataCustodianRegistrations;
import org.greenbuttonalliance.espi.thirdparty.oauth.EspiAuthorizationOAuth2AuthorizedClientService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A TP customer authorizing the Third Party at a Data Custodian with {@code oauth2Client} (#146), and
 * the TP's client access token. The Authorization Server's token endpoint is a stub that answers as
 * the real one does, with the REQ.21 ESPI URIs next to the token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataCustodianAuthorizationTest {

	private static final String CUSTOMER = "tp-oauth-customer";
	private static final String ADMIN = "tp-oauth-admin";
	private static final String TP_BASE = "http://localhost:8082/ThirdParty";
	private static final String CALLBACK = "/login/oauth2/code/" + DataCustodianRegistrations.MONTHLY;
	private static final String RESOURCE_URI = "http://localhost:8081/DataCustodian/espi/1_1/resource/Subscription/5/UsagePoint";
	private static final String AUTHORIZATION_URI = "http://localhost:8081/DataCustodian/espi/1_1/resource/Authorization/7";
	private static final String CUSTOMER_RESOURCE_URI = "http://localhost:8081/DataCustodian/espi/1_1/resource/Batch/RetailCustomer/3";
	// The client access token's URIs name the Bulk feeds: energy usage and Customer (PII) data (#202).
	private static final String BULK_RESOURCE_URI = "http://localhost:8081/DataCustodian/espi/1_1/resource/Batch/Bulk/11";
	private static final String BULK_AUTHORIZATION_URI = "http://localhost:8081/DataCustodian/espi/1_1/resource/Authorization/12";
	private static final String BULK_CUSTOMER_RESOURCE_URI = "http://localhost:8081/DataCustodian/espi/1_1/resource/Batch/Bulk/13";
	private static final String MONTHLY_SCOPE ="FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";

	/** HttpSessionRequestCache's session attribute: the request saved for after sign-in. */
	private static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST";

	private static HttpServer tokenEndpoint;

	/** The form parameters of each token request the stub received, by grant type. */
	private static final Map<String, String> tokenRequests = new ConcurrentHashMap<>();

	/** When set, the stub token endpoint answers 401 invalid_client, as an AS that refuses the TP. */
	private static volatile boolean tokenEndpointRefuses;

	@BeforeAll
	static void startTokenEndpoint() throws IOException {
		tokenEndpoint = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		tokenEndpoint.createContext("/oauth2/token", exchange -> {
			String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			String grantType = UriComponentsBuilder.newInstance().query(form).build().getQueryParams().getFirst("grant_type");
			tokenRequests.put(grantType, form);
			if (tokenEndpointRefuses) {
				byte[] error = "{\"error\":\"invalid_client\"}".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
				exchange.sendResponseHeaders(401, error.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(error);
				}
				return;
			}
			String body = "client_credentials".equals(grantType)
					? "{\"access_token\":\"cc-token-1234567890\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
							+ "\"scope\":\"FB=34_35\",\"resourceURI\":\"" + BULK_RESOURCE_URI + "\","
							+ "\"authorizationURI\":\"" + BULK_AUTHORIZATION_URI + "\","
							+ "\"customerResourceURI\":\"" + BULK_CUSTOMER_RESOURCE_URI + "\"}"
					: "{\"access_token\":\"customer-token-1234\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
							+ "\"refresh_token\":\"customer-refresh\",\"scope\":\"" + MONTHLY_SCOPE + "\","
							+ "\"resourceURI\":\"" + RESOURCE_URI + "\",\"authorizationURI\":\"" + AUTHORIZATION_URI + "\","
							+ "\"customerResourceURI\":\"" + CUSTOMER_RESOURCE_URI + "\"}";
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		tokenEndpoint.start();
	}

	@AfterAll
	static void stopTokenEndpoint() {
		tokenEndpoint.stop(0);
	}

	@DynamicPropertySource
	static void tokenUri(DynamicPropertyRegistry registry) {
		registry.add("spring.security.oauth2.client.provider.datacustodian.token-uri",
				() -> "http://127.0.0.1:" + tokenEndpoint.getAddress().getPort() + "/oauth2/token");
		registry.add("espi.thirdparty.base-url", () -> TP_BASE);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RetailCustomerRepository retailCustomerRepository;

	@Autowired
	private AuthorizationRepository authorizationRepository;

	@Autowired
	private EspiAuthorizationOAuth2AuthorizedClientService authorizedClientService;

	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;

	@BeforeEach
	void createAccounts() {
		cleanUp();
		retailCustomerRepository.save(account(CUSTOMER, RetailCustomerEntity.ROLE_USER));
		retailCustomerRepository.save(account(ADMIN, RetailCustomerEntity.ROLE_CUSTODIAN));
	}

	@AfterEach
	void cleanUp() {
		authorizationRepository.findAll().stream()
				.filter(authorization -> authorization.getClientRegistrationId() != null)
				.forEach(authorizationRepository::delete);
		List.of(CUSTOMER, ADMIN).forEach(name ->
				retailCustomerRepository.findByUsername(name).ifPresent(retailCustomerRepository::delete));
		tokenRequests.clear();
		tokenEndpointRefuses = false;
	}

	private static RetailCustomerEntity account(String username, String role) {
		RetailCustomerEntity entity = new RetailCustomerEntity(username, "Test", "Account", username + "@example.local", role);
		entity.setPassword("{noop}unused");
		entity.setEnabled(Boolean.TRUE);
		entity.setAccountLocked(Boolean.FALSE);
		return entity;
	}

	/** A session in which the TP customer is signed in with form login, as after /login. */
	private static MockHttpSession signedInSession(String username, String role) {
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(username, null,
				AuthorityUtils.createAuthorityList(role));
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
				new SecurityContextImpl(authentication));
		return session;
	}

	@Test
	@DisplayName("a TP customer is sent to the AS authorize endpoint with the ESPI scope and the TP callback")
	void customer_starts_authorization() throws Exception {
		String location = mockMvc.perform(get("/oauth2/authorization/" + DataCustodianRegistrations.MONTHLY)
						.session(signedInSession(CUSTOMER, RetailCustomerEntity.ROLE_USER)))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getRedirectedUrl();

		assertThat(UriComponentsBuilder.fromUriString(location).build())
				.satisfies(uri -> assertThat(uri.toUriString()).startsWith("http://localhost:9999/oauth2/authorize"))
				.extracting(uri -> uri.getQueryParams().toSingleValueMap())
				.satisfies(params -> assertThat(params)
						.containsEntry("client_id", "third_party")
						.containsEntry("response_type", "code")
						.containsKey("state")
						// No PKCE until the AS keeps code_challenge through the DC delegation (#274)
						.doesNotContainKeys("nonce", "code_challenge", "code_challenge_method"))
				.satisfies(params -> assertThat(decode(params.get("redirect_uri"))).isEqualTo(TP_BASE + CALLBACK))
				.satisfies(params -> assertThat(decode(params.get("scope"))).isEqualTo(MONTHLY_SCOPE));
	}

	@Test
	@DisplayName("an anonymous visitor is sent to the TP login page, not to the Data Custodian")
	void anonymous_cannot_start_authorization() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/" + DataCustodianRegistrations.MONTHLY))
				.andExpect(redirectedUrl("/login"));
		mockMvc.perform(get("/oauth2/authorize/monthly"))
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@DisplayName("a TP administrator may not start a customer authorization; a client_credentials registration is no redirect")
	void only_customers_start_authorization_code_flows() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/" + DataCustodianRegistrations.MONTHLY)
						.session(signedInSession(ADMIN, RetailCustomerEntity.ROLE_CUSTODIAN)))
				.andExpect(status().isForbidden());
		// Spring's own resolver answers 500 for a client_credentials registration.
		mockMvc.perform(get("/oauth2/authorization/" + DataCustodianRegistrations.CLIENT_ACCESS)
						.session(signedInSession(CUSTOMER, RetailCustomerEntity.ROLE_USER)))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("the callback stores the token and ESPI URIs for the TP customer, who stays signed in to the TP")
	void callback_stores_token_and_keeps_tp_sign_in() throws Exception {
		MockHttpSession session = signedInSession(CUSTOMER, RetailCustomerEntity.ROLE_USER);
		String state = startAuthorization(session);

		MvcResult callback = mockMvc.perform(get(TP_BASE + CALLBACK).queryParam("code", "the-code")
						.queryParam("state", state).contextPath("/ThirdParty").session(session))
				.andExpect(redirectedUrl(TP_BASE + CALLBACK))
				.andReturn();

		assertThat(tokenRequests.get("authorization_code"))
				.contains("code=the-code", "redirect_uri=" + encode(TP_BASE + CALLBACK))
				.doesNotContain("code_verifier");
		assertThat(authorizationRepository.findFirstByClientRegistrationIdAndRetailCustomer_UsernameOrderByUpdatedDesc(
						DataCustodianRegistrations.MONTHLY, CUSTOMER))
				.hasValueSatisfying(authorization -> assertThat(authorization)
						.extracting(AuthorizationEntity::getAccessToken, AuthorizationEntity::getRefreshToken,
								AuthorizationEntity::getScope, AuthorizationEntity::getStatus,
								AuthorizationEntity::getResourceURI, AuthorizationEntity::getAuthorizationURI,
								AuthorizationEntity::getCustomerResourceURI, AuthorizationEntity::getThirdParty,
								stored -> stored.getId().version())
						.containsExactly("customer-token-1234", "customer-refresh", MONTHLY_SCOPE,
								AuthorizationEntity.STATUS_ACTIVE, RESOURCE_URI, AUTHORIZATION_URI,
								CUSTOMER_RESOURCE_URI, "third_party", 5));

		// Still the TP customer (ROLE_USER, form login), not a Data Custodian principal.
		assertThat(((SecurityContext) callback.getRequest().getSession()
				.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).getAuthentication())
				.satisfies(authentication -> assertThat(authentication.getName()).isEqualTo(CUSTOMER))
				.satisfies(authentication -> assertThat(AuthorityUtils.authorityListToSet(authentication.getAuthorities()))
						.containsExactly(RetailCustomerEntity.ROLE_USER));

		mockMvc.perform(get(TP_BASE + CALLBACK).contextPath("/ThirdParty").session(session))
				.andExpect(redirectedUrl("/ThirdParty/?authorized"));
		assertThat(mockMvc.perform(get("/").param("authorized", "").session(session))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.contains("The Data Custodian authorized this Third Party", "Your authorizations",
						"Data Custodian (Monthly)", RESOURCE_URI, AUTHORIZATION_URI, CUSTOMER_RESOURCE_URI)
				.doesNotContain("customer-token-1234", "customer-refresh");
	}

	@Test
	@DisplayName("a visitor who starts signed out signs in, consents once, and lands on the callback, not on a second authorization")
	void start_signed_out_then_consent_once() throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(get("/oauth2/authorization/" + DataCustodianRegistrations.MONTHLY).session(session))
				.andExpect(redirectedUrl("/login"));
		assertThat(session.getAttribute(SAVED_REQUEST)).as("the request saved for after sign-in").isNotNull();

		// Signed in by form login, which resumes the saved /oauth2/authorization request.
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
				new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(CUSTOMER, null,
						AuthorityUtils.createAuthorityList(RetailCustomerEntity.ROLE_USER))));
		String state = startAuthorization(session);
		assertThat(session.getAttribute(SAVED_REQUEST)).as("the saved request, used up").isNull();

		mockMvc.perform(get(TP_BASE + CALLBACK).queryParam("code", "the-code")
						.queryParam("state", state).contextPath("/ThirdParty").session(session))
				.andExpect(redirectedUrl(TP_BASE + CALLBACK));
		mockMvc.perform(get(TP_BASE + CALLBACK).contextPath("/ThirdParty").session(session))
				.andExpect(redirectedUrl("/ThirdParty/?authorized"));
	}

	@Test
	@DisplayName("consent refused at the Data Custodian: nothing is stored and the customer gets the home page's message")
	void refused_consent_stores_nothing() throws Exception {
		MockHttpSession session = signedInSession(CUSTOMER, RetailCustomerEntity.ROLE_USER);
		String state = startAuthorization(session);

		String location = mockMvc.perform(get(TP_BASE + CALLBACK).queryParam("error", "access_denied")
						.queryParam("state", state).contextPath("/ThirdParty").session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getRedirectedUrl();
		assertThat(location).startsWith(TP_BASE + CALLBACK + "?error=access_denied");

		mockMvc.perform(get(location).contextPath("/ThirdParty").session(session))
				.andExpect(redirectedUrl("/ThirdParty/?authorization_error"));
		assertThat(tokenRequests).isEmpty();
		assertThat(authorizedClientService.findAuthorization(DataCustodianRegistrations.MONTHLY, CUSTOMER)).isEmpty();
	}

	@Test
	@DisplayName("a callback with no authorization in progress in this session is not accepted")
	void forged_callback_is_rejected() throws Exception {
		mockMvc.perform(get(TP_BASE + CALLBACK + "?code=stolen&state=guessed").contextPath("/ThirdParty")
						.session(signedInSession(CUSTOMER, RetailCustomerEntity.ROLE_USER)))
				.andExpect(redirectedUrl("/ThirdParty/?authorization_error"));
		assertThat(tokenRequests).isEmpty();
	}

	@Test
	@DisplayName("a refreshed token keeps the ESPI URIs when the refresh response does not repeat them")
	void refresh_keeps_espi_uris() throws Exception {
		MockHttpSession session = signedInSession(CUSTOMER, RetailCustomerEntity.ROLE_USER);
		mockMvc.perform(get(TP_BASE + CALLBACK).queryParam("code", "the-code")
						.queryParam("state", startAuthorization(session)).contextPath("/ThirdParty").session(session))
				.andExpect(redirectedUrl(TP_BASE + CALLBACK));

		OAuth2AccessToken refreshed = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "refreshed-token",
				Instant.now(), Instant.now().plusSeconds(3600), Set.of(MONTHLY_SCOPE));
		authorizedClientService.saveAuthorizedClient(new OAuth2AuthorizedClient(
						clientRegistrationRepository.findByRegistrationId(DataCustodianRegistrations.MONTHLY), CUSTOMER,
						refreshed, null),
				UsernamePasswordAuthenticationToken.authenticated(CUSTOMER, null,
						AuthorityUtils.createAuthorityList(RetailCustomerEntity.ROLE_USER)));

		assertThat(authorizedClientService.findAuthorization(DataCustodianRegistrations.MONTHLY, CUSTOMER))
				.hasValueSatisfying(authorization -> assertThat(authorization)
						.extracting(AuthorizationEntity::getAccessToken, AuthorizationEntity::getResourceURI,
								AuthorizationEntity::getAuthorizationURI)
						.containsExactly("refreshed-token", RESOURCE_URI, AUTHORIZATION_URI));
		assertThat(authorizationRepository.findAllByRetailCustomerId(
				retailCustomerRepository.findByUsername(CUSTOMER).orElseThrow().getId())).hasSize(1);
	}

	@Test
	@DisplayName("the TP administrator obtains the client access token (client_credentials) and its Bulk URIs; it belongs to no customer")
	void admin_obtains_client_access_token() throws Exception {
		MockHttpSession session = signedInSession(ADMIN, RetailCustomerEntity.ROLE_CUSTODIAN);
		mockMvc.perform(post("/custodian/home/client-token").with(csrf()).session(session))
				.andExpect(redirectedUrl("/custodian/home?client_token"));

		assertThat(tokenRequests.get("client_credentials")).contains("scope=FB%3D34_35");
		assertThat(authorizationRepository.findFirstByClientRegistrationIdAndRetailCustomerIsNullOrderByUpdatedDesc(
						DataCustodianRegistrations.CLIENT_ACCESS))
				.hasValueSatisfying(authorization -> assertThat(authorization)
						.extracting(AuthorizationEntity::getAccessToken, AuthorizationEntity::getScope,
								AuthorizationEntity::getRetailCustomer, AuthorizationEntity::getStatus,
								AuthorizationEntity::getResourceURI, AuthorizationEntity::getAuthorizationURI,
								AuthorizationEntity::getCustomerResourceURI)
						.containsExactly("cc-token-1234567890", "FB=34_35", null, AuthorizationEntity.STATUS_ACTIVE,
								BULK_RESOURCE_URI, BULK_AUTHORIZATION_URI, BULK_CUSTOMER_RESOURCE_URI));

		assertThat(mockMvc.perform(get("/custodian/home").param("client_token", "").session(session))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.contains("A new client access token was obtained", "FB=34_35", "cc-token…",
						BULK_RESOURCE_URI, BULK_AUTHORIZATION_URI, BULK_CUSTOMER_RESOURCE_URI)
				.doesNotContain("cc-token-1234567890");
	}

	@Test
	@DisplayName("when the AS refuses a new client access token, the one already held stays in use")
	void refused_client_token_keeps_the_current_one() throws Exception {
		MockHttpSession session = signedInSession(ADMIN, RetailCustomerEntity.ROLE_CUSTODIAN);
		mockMvc.perform(post("/custodian/home/client-token").with(csrf()).session(session))
				.andExpect(redirectedUrl("/custodian/home?client_token"));

		tokenEndpointRefuses = true;
		mockMvc.perform(post("/custodian/home/client-token").with(csrf()).session(session))
				.andExpect(redirectedUrl("/custodian/home?client_token_error"));

		assertThat(authorizedClientService.<OAuth2AuthorizedClient>loadAuthorizedClient(
						DataCustodianRegistrations.CLIENT_ACCESS, ADMIN))
				.isNotNull()
				.extracting(client -> client.getAccessToken().getTokenValue())
				.isEqualTo("cc-token-1234567890");
	}

	@Test
	@DisplayName("the TP's client access token never counts as a customer's authorization on the callback page")
	void callback_page_ignores_the_client_access_token() throws Exception {
		mockMvc.perform(post("/custodian/home/client-token").with(csrf())
						.session(signedInSession(ADMIN, RetailCustomerEntity.ROLE_CUSTODIAN)))
				.andExpect(redirectedUrl("/custodian/home?client_token"));

		mockMvc.perform(get("/login/oauth2/code/" + DataCustodianRegistrations.CLIENT_ACCESS)
						.session(signedInSession(CUSTOMER, RetailCustomerEntity.ROLE_USER)))
				.andExpect(redirectedUrl("/?authorization_error"));
	}

	@Test
	@DisplayName("a TP customer may not obtain the client access token")
	void customer_cannot_obtain_client_access_token() throws Exception {
		mockMvc.perform(post("/custodian/home/client-token").with(csrf()).with(user(CUSTOMER).roles("USER")))
				.andExpect(status().isForbidden());
		assertThat(tokenRequests).isEmpty();
	}

	/** Starts the monthly authorization in this session and returns the state the TP sent to the AS. */
	private String startAuthorization(MockHttpSession session) throws Exception {
		String location = mockMvc.perform(get("/oauth2/authorization/" + DataCustodianRegistrations.MONTHLY)
						.session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getRedirectedUrl();
		// Spring's state ends in Base64 '=' padding, which arrives URL-encoded: decode it, and send it
		// back with queryParam (which encodes it once).
		return decode(UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("state"));
	}

	private static String decode(String value) {
		return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static String encode(String value) {
		return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
