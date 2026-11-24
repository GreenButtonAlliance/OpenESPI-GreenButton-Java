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

import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelRequest;
import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelResponse;
import org.greenbuttonalliance.espi.authserver.backchannel.DataCustodianBackchannelClient;
import org.greenbuttonalliance.espi.authserver.web.delegate.DelegationStateService;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 2.0 end-to-end orchestration test (issue #148, closes #122).
 *
 * <p>Drives the AS-side half of the ESPI customer OAuth2 authorization-code flow through a real
 * Spring Authorization Server context against a Testcontainers MySQL, with only the Data Custodian
 * back-channel mocked. It simulates the customer's return from DC (a signed Return handoff) and
 * verifies the AS:</p>
 * <ol>
 *   <li>accepts the return handoff at {@code /oauth2/authorize/continue}, authenticates the
 *       customer, seeds consent, and resumes the flow;</li>
 *   <li>mints an authorization code at {@code /oauth2/authorize} (no second delegation round);</li>
 *   <li>at {@code /oauth2/token}, calls the DC back-channel (mocked) and augments the opaque
 *       token response with the canonical ESPI URIs (the C4 augmentation).</li>
 * </ol>
 *
 * <p>The flow is exercised with a real ESPI scope string ({@code FB=...;IntervalDuration=...;...})
 * registered on a test client created per-run. Its {@code ;}-separated parameters belong to one
 * scope token; this test guards the consent-loop regression where they were split apart (#210).</p>
 */
@DisplayName("Phase 2.0 auth-code-flow orchestration (AS-side, DC back-channel mocked)")
class AuthCodeFlowOrchestrationIntegrationTest extends AbstractAuthserverIntegrationTest {

	private static final String REDIRECT_URI = "https://tp.example/cb";
	private static final String SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";
	private static final String CUSTOMER_ID = "42";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegisteredClientRepository registeredClientRepository;

	@Autowired
	private DelegationStateService delegationStateService;

	@Autowired
	private SignedHandoffCodec codec;

	@MockitoBean
	private DataCustodianBackchannelClient backchannelClient;

	@Test
	@DisplayName("return handoff -> code -> token, token response augmented with back-channel URIs")
	void fullAuthCodeFlowAugmentsTokenResponse() throws Exception {
		// Unique identifiers so the test is safe under singleton-container reuse across runs.
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-d-tp-" + suffix;
		String clientSecret = "pr-d-secret-" + suffix;
		String correlationId = "corr-" + suffix;
		UUID selectedUsagePoint = UUID.randomUUID();

		RegisteredClient testClient = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(clientId)
				.clientName("PR-D Test ThirdParty")
				.clientSecret("{noop}" + clientSecret)
				.clientIdIssuedAt(Instant.now())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri(REDIRECT_URI)
				.scope(SCOPE)
				.clientSettings(ClientSettings.builder()
						.requireAuthorizationConsent(true)
						.requireProofKey(false) // PKCE accepted, not required (#274)
						.build())
				.tokenSettings(TokenSettings.builder()
						.accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI: opaque
						.accessTokenTimeToLive(Duration.ofMinutes(360))
						.build())
				.build();
		registeredClientRepository.save(testClient);

		// The outbound leg already happened in real life; seed the delegation state it would have left.
		delegationStateService.save(correlationId, clientId, REDIRECT_URI, "tp-state-" + suffix, SCOPE);

		// What the DC back-channel will return once the token is minted.
		UUID authorizationId = UUID.randomUUID();
		UUID resourceSubscriptionId = UUID.randomUUID();
		UUID customerSubscriptionId = UUID.randomUUID();
		String resourceUri = "https://dc.example/Subscription/" + resourceSubscriptionId;
		String authorizationUri = "https://dc.example/Authorization/" + authorizationId;
		String customerResourceUri = "https://dc.example/RetailCustomer/42/Customer/" + customerSubscriptionId;
		when(backchannelClient.provision(any())).thenReturn(new BackchannelResponse(
				authorizationId, resourceSubscriptionId, customerSubscriptionId,
				resourceUri, authorizationUri, customerResourceUri));

		// Mint a valid signed Return handoff (same signing key as the context, via the autowired codec).
		Instant now = Instant.now();
		String handoff = codec.encode(SignedHandoff.Return.of(
				correlationId, now, now.plusSeconds(300),
				UUID.randomUUID().toString().replace("-", ""), // single-use nonce
				CUSTOMER_ID,
				List.of(selectedUsagePoint),
				SignedHandoff.Return.CONSENT_ALLOW,
				SCOPE));

		// 1) Return from DC: /oauth2/authorize/continue -> 302 to /oauth2/authorize. Keep the session.
		MvcResult continueResult = mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", handoff))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.startsWith("/oauth2/authorize")))
				.andReturn();
		MockHttpSession session = (MockHttpSession) continueResult.getRequest().getSession(false);
		assertThat(session).isNotNull();
		String authorizeUrl = continueResult.getResponse().getHeader("Location");

		// 2) Resume /oauth2/authorize on the authenticated, consent-seeded session -> 302 to TP with code.
		// The Location is already percent-encoded; pass it as a URI so MockMvc doesn't encode it again.
		MvcResult authorizeResult = mockMvc.perform(get(java.net.URI.create(authorizeUrl)).session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		String location = authorizeResult.getResponse().getHeader("Location");
		assertThat(location)
				.as("authorize should redirect to the TP callback carrying an authorization code")
				.startsWith(REDIRECT_URI)
				.contains("code=");
		String code = extractQueryParam(location, "code");
		assertThat(code).isNotBlank();

		// 3) Exchange the code at /oauth2/token -> back-channel fires, response augmented with URIs.
		MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
						.with(httpBasic(clientId, clientSecret))
						.param("grant_type", "authorization_code")
						.param("code", code)
						.param("redirect_uri", REDIRECT_URI))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").isNotEmpty())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.resourceURI").value(resourceUri))
				.andExpect(jsonPath("$.authorizationURI").value(authorizationUri))
				.andExpect(jsonPath("$.customerResourceURI").value(customerResourceUri))
				// ESPI 4.0 token response carries only the three canonical URIs; the *_id claims were
				// non-standard and removed in #160.
				.andExpect(jsonPath("$.authorization_id").doesNotExist())
				.andExpect(jsonPath("$.resource_subscription_id").doesNotExist())
				.andExpect(jsonPath("$.customer_subscription_id").doesNotExist())
				.andReturn();
		String accessToken = new tools.jackson.databind.ObjectMapper()
				.readTree(tokenResult.getResponse().getContentAsString()).get("access_token").asString();

		// The back-channel was called with the customer-selection context carried through the flow.
		ArgumentCaptor<BackchannelRequest> captor = ArgumentCaptor.forClass(BackchannelRequest.class);
		verify(backchannelClient).provision(captor.capture());
		BackchannelRequest sent = captor.getValue();
		assertThat(sent.correlationId()).isEqualTo(correlationId);
		assertThat(sent.clientId()).isEqualTo(clientId);
		assertThat(sent.grantedScope()).isEqualTo(SCOPE);
		assertThat(sent.retailCustomerId()).isEqualTo(42L);
		assertThat(sent.selectedUsagePointIds()).containsExactly(selectedUsagePoint);

		// 4) Introspection (#160 follow-up): /oauth2/introspect must surface the SAME ESPI URI claims
		//    + active + FB-grammar scope, and must NOT carry the removed *_id fields. (Verifies the C4
		//    claims actually reach introspection, not just the /oauth2/token response.)
		mockMvc.perform(post("/oauth2/introspect")
						.with(httpBasic(clientId, clientSecret))
						.param("token", accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.scope").value(SCOPE))
				.andExpect(jsonPath("$.resourceURI").value(resourceUri))
				.andExpect(jsonPath("$.authorizationURI").value(authorizationUri))
				.andExpect(jsonPath("$.customerResourceURI").value(customerResourceUri))
				.andExpect(jsonPath("$.authorization_id").doesNotExist())
				.andExpect(jsonPath("$.resource_subscription_id").doesNotExist())
				.andExpect(jsonPath("$.customer_subscription_id").doesNotExist());
	}

	/**
	 * Registered {@code FB=4_5_15}. Either the Third Party narrowed it in its authorization request, or the
	 * customer narrowed the request on the DC Authorization Screen, or both.
	 */
	@ParameterizedTest(name = "requested {0}, customer approved {1}")
	@CsvSource(delimiter = '|', value = {
			// customer drops FB 15 from the registered scope
			"FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000"
					+ "|FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000",
			// Third Party requests FB 4, 5 only; the customer approves all of it
			"FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000"
					+ "|FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000",
			// Third Party requests FB 4, 5; the customer narrows further to FB 5
			"FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000"
					+ "|FB=5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000"})
	@DisplayName("narrowed scope (#211): code, token, introspection and back-channel carry the narrowed scope")
	void narrowedScopeCompletesWithTheNarrowedScope(String requested, String narrowed) throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-211-tp-" + suffix;
		String clientSecret = "pr-211-secret-" + suffix;
		String correlationId = "corr-211-" + suffix;
		registerClient(clientId, clientSecret);
		delegationStateService.save(correlationId, clientId, REDIRECT_URI, "tp-state-" + suffix, requested);
		when(backchannelClient.provision(any())).thenReturn(new BackchannelResponse(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				"https://dc.example/Subscription/211", "https://dc.example/Authorization/211", null));

		Instant now = Instant.now();
		String handoff = codec.encode(SignedHandoff.Return.of(
				correlationId, now, now.plusSeconds(300),
				UUID.randomUUID().toString().replace("-", ""),
				CUSTOMER_ID,
				List.of(UUID.randomUUID()),
				SignedHandoff.Return.CONSENT_ALLOW,
				narrowed));
		MvcResult continueResult = mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", handoff))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) continueResult.getRequest().getSession(false);

		String location = mockMvc.perform(get(java.net.URI.create(
								continueResult.getResponse().getHeader("Location"))).session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");
		assertThat(location)
				.as("the narrowed scope must mint a code, not invalid_scope")
				.startsWith(REDIRECT_URI)
				.contains("code=")
				.doesNotContain("error=");

		String body = mockMvc.perform(post("/oauth2/token")
						.with(httpBasic(clientId, clientSecret))
						.param("grant_type", "authorization_code")
						.param("code", extractQueryParam(location, "code"))
						.param("redirect_uri", REDIRECT_URI))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scope").value(narrowed))
				.andReturn().getResponse().getContentAsString();
		String accessToken = new tools.jackson.databind.ObjectMapper().readTree(body).get("access_token").asString();

		ArgumentCaptor<BackchannelRequest> captor = ArgumentCaptor.forClass(BackchannelRequest.class);
		verify(backchannelClient).provision(captor.capture());
		assertThat(captor.getValue().grantedScope()).isEqualTo(narrowed);

		mockMvc.perform(post("/oauth2/introspect")
						.with(httpBasic(clientId, clientSecret))
						.param("token", accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.scope").value(narrowed));
	}

	@Test
	@DisplayName("Third Party requests a narrowing of its registered scope (#211): delegated to the customer")
	void thirdPartyNarrowedRequestIsDelegatedToTheCustomer() throws Exception {
		assertThat(authorizeLocation("FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000"))
				.contains("/authorize/delegate")
				.doesNotContain("error=");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"FB=1_4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000", // adds FB 1
			"FB=4_5;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000"})      // changes a parameter
	@DisplayName("Third Party requests a scope that is not a narrowing of its registration (#211): invalid_scope")
	void thirdPartyWidenedRequestIsInvalidScope(String scope) throws Exception {
		assertThat(authorizeLocation(scope)).startsWith(REDIRECT_URI).contains("error=invalid_scope");
	}

	/** Where an unauthenticated {@code /oauth2/authorize} for a freshly registered client redirects. */
	private String authorizeLocation(String scope) throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-211-direct-" + suffix;
		registerClient(clientId, "pr-211-secret-" + suffix);

		String authorizeUri = org.springframework.web.util.UriComponentsBuilder.fromPath("/oauth2/authorize")
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("scope", scope)
				.queryParam("redirect_uri", REDIRECT_URI)
				.queryParam("state", "s-" + suffix)
				.encode(java.nio.charset.StandardCharsets.UTF_8)
				.build().toUriString();
		return mockMvc.perform(get(java.net.URI.create(authorizeUri))
						.accept(org.springframework.http.MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");
	}

	@Test
	@DisplayName("refresh_token grant (#202): no re-provisioning, refreshed response carries the SAME URIs")
	void refreshDoesNotReprovisionAndKeepsUris() throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-202-tp-" + suffix;
		String clientSecret = "pr-202-secret-" + suffix;
		String correlationId = "corr-202-" + suffix;
		registerClient(clientId, clientSecret);
		delegationStateService.save(correlationId, clientId, REDIRECT_URI, "tp-state-" + suffix, SCOPE);

		UUID authorizationId = UUID.randomUUID();
		UUID resourceSubscriptionId = UUID.randomUUID();
		String resourceUri = "https://dc.example/Batch/Subscription/" + resourceSubscriptionId;
		String authorizationUri = "https://dc.example/Authorization/" + authorizationId;
		String customerResourceUri = "https://dc.example/Batch/RetailCustomer/42";
		// A second provision (if the bug re-triggers it) would hand back DIFFERENT resources — exactly
		// what a non-idempotent DC does — so the refreshed URIs would visibly change.
		when(backchannelClient.provision(any()))
				.thenReturn(new BackchannelResponse(authorizationId, resourceSubscriptionId, null,
						resourceUri, authorizationUri, customerResourceUri))
				.thenReturn(new BackchannelResponse(UUID.randomUUID(), UUID.randomUUID(), null,
						"https://dc.example/Batch/Subscription/second",
						"https://dc.example/Authorization/second",
						"https://dc.example/Batch/RetailCustomer/42"));

		String code = obtainAuthorizationCode(correlationId, UUID.randomUUID());

		MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
						.with(httpBasic(clientId, clientSecret))
						.param("grant_type", "authorization_code")
						.param("code", code)
						.param("redirect_uri", REDIRECT_URI))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.refresh_token").isNotEmpty())
				.andExpect(jsonPath("$.resourceURI").value(resourceUri))
				.andReturn();
		String refreshToken = com.jayway.jsonpath.JsonPath.read(
				tokenResult.getResponse().getContentAsString(), "$.refresh_token");

		MvcResult refreshResult = mockMvc.perform(post("/oauth2/token")
						.with(httpBasic(clientId, clientSecret))
						.param("grant_type", "refresh_token")
						.param("refresh_token", refreshToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").isNotEmpty())
				.andExpect(jsonPath("$.resourceURI").value(resourceUri))
				.andExpect(jsonPath("$.authorizationURI").value(authorizationUri))
				.andExpect(jsonPath("$.customerResourceURI").value(customerResourceUri))
				.andReturn();
		String refreshedAccessToken = com.jayway.jsonpath.JsonPath.read(
				refreshResult.getResponse().getContentAsString(), "$.access_token");

		// Exactly one back-channel provision for the whole grant: the refresh must not create another
		// ESPI Authorization/Subscription at the Data Custodian.
		verify(backchannelClient, times(1)).provision(any());

		// Introspection of the refreshed token stays consistent with the refresh response.
		mockMvc.perform(post("/oauth2/introspect")
						.with(httpBasic(clientId, clientSecret))
						.param("token", refreshedAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.resourceURI").value(resourceUri))
				.andExpect(jsonPath("$.authorizationURI").value(authorizationUri))
				.andExpect(jsonPath("$.customerResourceURI").value(customerResourceUri));
	}

	@Test
	@DisplayName("outbound leg (#232): unauthenticated /oauth2/authorize -> /authorize/delegate -> signed DC handoff")
	void unauthenticatedAuthorizeDelegatesToDataCustodian() throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-232-tp-" + suffix;
		String tpState = "tp-state-" + suffix;
		registerClient(clientId, "pr-232-secret-" + suffix);

		// 1) The customer's browser arrives unauthenticated -> 302 to the delegate WITH the request parameters.
		// A real query string (not .param()): Spring AS reads a GET authorization request from it.
		java.net.URI authorizeUri = org.springframework.web.util.UriComponentsBuilder.fromPath("/oauth2/authorize")
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("scope", SCOPE)
				.queryParam("redirect_uri", REDIRECT_URI)
				.queryParam("state", tpState)
				.encode()
				.build()
				.toUri();
		MvcResult authorizeResult = mockMvc.perform(get(authorizeUri)
						.accept(org.springframework.http.MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		String delegateUrl = authorizeResult.getResponse().getHeader("Location");
		assertThat(java.net.URI.create(delegateUrl).getPath()).isEqualTo("/authorize/delegate");

		// 2) The delegate signs the outbound handoff and redirects to the DC Authorization Screen.
		MvcResult delegateResult = mockMvc.perform(get(java.net.URI.create(delegateUrl)))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		String dcUrl = delegateResult.getResponse().getHeader("Location");
		assertThat(java.net.URI.create(dcUrl).getPath()).endsWith("/oauth/authorize-screen");
		SignedHandoff.Outbound outbound = codec.decodeOutbound(extractQueryParam(dcUrl, "handoff"));
		assertThat(outbound)
				.satisfies(o -> assertThat(o.clientId()).isEqualTo(clientId))
				.satisfies(o -> assertThat(o.grantedScope()).isEqualTo(SCOPE))
				.satisfies(o -> assertThat(o.correlationId()).as("#331: the AS's own id, never the TP's state")
						.isNotEqualTo(tpState));

		// 3) The delegation state the return leg needs was saved under the correlation id, with the TP's state.
		assertThat(delegationStateService.consume(tpState)).isNull();
		assertThat(delegationStateService.consume(outbound.correlationId()))
				.extracting(DelegationStateService.Entry::clientId, DelegationStateService.Entry::redirectUri,
						DelegationStateService.Entry::tpState)
				.containsExactly(clientId, REDIRECT_URI, tpState);
	}

	/**
	 * PKCE through the Data Custodian delegation (#274). REQ.21 does not require PKCE, but a Third Party that
	 * sends a {@code code_challenge} must have it enforced at the token endpoint (RFC 9700 2.1.1, which updates
	 * RFC 6749): the challenge travels from the original {@code /oauth2/authorize} through
	 * {@code /authorize/delegate} and {@code /oauth2/authorize/continue} into the replayed request, so the code
	 * is minted with it. The client does not require PKCE ({@code requireProofKey(false)}).
	 */
	@ParameterizedTest(name = "code_verifier {0}: {1}")
	@CsvSource({"matching, 200", "wrong, 400", "missing, 400"})
	@DisplayName("PKCE (#274): code_challenge survives the DC delegation; only the matching code_verifier gets a token")
	void proofKeyIsCarriedThroughTheDelegation(String verifierCase, int expectedStatus) throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-274-tp-" + suffix;
		String clientSecret = "pr-274-secret-" + suffix;
		String tpState = "tp-state-274-" + suffix;
		registerClient(clientId, clientSecret);
		String verifier = "verifier-" + UUID.randomUUID() + "-" + UUID.randomUUID();
		String challenge = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
				java.security.MessageDigest.getInstance("SHA-256")
						.digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
		when(backchannelClient.provision(any())).thenReturn(new BackchannelResponse(UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), "https://dc.example/Subscription/1",
				"https://dc.example/Authorization/1", "https://dc.example/RetailCustomer/42/Customer/1"));

		// 1) Outbound: the Third Party's request, with its S256 challenge, is delegated with the challenge.
		java.net.URI authorizeUri = org.springframework.web.util.UriComponentsBuilder.fromPath("/oauth2/authorize")
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("scope", SCOPE)
				.queryParam("redirect_uri", REDIRECT_URI)
				.queryParam("state", tpState)
				.queryParam("code_challenge", challenge)
				.queryParam("code_challenge_method", "S256")
				.encode()
				.build()
				.toUri();
		String delegateUrl = mockMvc.perform(get(authorizeUri).accept(org.springframework.http.MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");
		assertThat(new String[] {extractQueryParam(delegateUrl, "code_challenge"),
				extractQueryParam(delegateUrl, "code_challenge_method")})
				.as("the entry point forwards the challenge to the delegate")
				.containsExactly(challenge, "S256");
		String correlationId = delegateToTheDataCustodian(delegateUrl, null);

		// 2) Return from the DC: /continue replays /oauth2/authorize with the challenge; the code is minted with it.
		Instant now = Instant.now();
		String handoff = codec.encode(SignedHandoff.Return.of(correlationId, now, now.plusSeconds(300),
				UUID.randomUUID().toString().replace("-", ""), CUSTOMER_ID, List.of(UUID.randomUUID()),
				SignedHandoff.Return.CONSENT_ALLOW, SCOPE));
		MvcResult continueResult = mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", handoff))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		String replay = continueResult.getResponse().getHeader("Location");
		assertThat(extractQueryParam(replay, "code_challenge")).as("the replay carries the challenge").isEqualTo(challenge);
		MockHttpSession session = (MockHttpSession) continueResult.getRequest().getSession(false);
		String callback = mockMvc.perform(get(java.net.URI.create(replay)).session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");
		assertThat(callback).startsWith(REDIRECT_URI).contains("code=", "state=" + tpState);

		// 3) The token endpoint checks the code_verifier against the challenge the code was minted with.
		org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder token = post("/oauth2/token")
				.with(httpBasic(clientId, clientSecret))
				.param("grant_type", "authorization_code")
				.param("code", extractQueryParam(callback, "code"))
				.param("redirect_uri", REDIRECT_URI);
		switch (verifierCase) {
			case "matching" -> token.param("code_verifier", verifier);
			case "wrong" -> token.param("code_verifier", verifier + "-not-it");
			default -> { }
		}
		org.springframework.test.web.servlet.ResultActions result = mockMvc.perform(token)
				.andExpect(status().is(expectedStatus));
		if (expectedStatus == 200) {
			result.andExpect(jsonPath("$.access_token").isNotEmpty());
		} else {
			result.andExpect(jsonPath("$.error").value("invalid_grant"));
		}
	}

	@Test
	@DisplayName("PKCE downgrade (#274, RFC 9700 4.8.2): a code_verifier for a code minted without a challenge is refused")
	void codeVerifierWithoutChallengeIsRefused() throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-274-dg-" + suffix;
		String clientSecret = "pr-274-dg-secret-" + suffix;
		String correlationId = "corr-274-dg-" + suffix;
		registerClient(clientId, clientSecret);
		delegationStateService.save(correlationId, clientId, REDIRECT_URI, "tp-state-" + suffix, SCOPE);

		String code = obtainAuthorizationCode(correlationId, UUID.randomUUID());

		mockMvc.perform(post("/oauth2/token")
						.with(httpBasic(clientId, clientSecret))
						.param("grant_type", "authorization_code")
						.param("code", code)
						.param("redirect_uri", REDIRECT_URI)
						.param("code_verifier", "verifier-" + UUID.randomUUID() + "-" + UUID.randomUUID()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_grant"));
	}

	@Test
	@DisplayName("PKCE support is advertised (#274, RFC 9700 2.1.1): code_challenge_methods_supported is S256")
	void metadataAdvertisesPkce() throws Exception {
		mockMvc.perform(get("/.well-known/oauth-authorization-server"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code_challenge_methods_supported").value(
						org.hamcrest.Matchers.contains("S256")));
	}

	/**
	 * A customer who has just authorized one registration starts another in the same browser session
	 * (#299: a different scope; #276: the same scope again). The second request must be delegated to the
	 * Data Custodian like the first, through the entry point and carrying the Third Party's own
	 * {@code state} and {@code redirect_uri}: not Spring's consent state (#299), and not a code minted
	 * from the consent left by the first authorization (#276).
	 */
	@ParameterizedTest(name = "second authorization: {0}")
	@CsvSource({"'Daily (a different scope, #299)', DAILY", "'Monthly again (the same scope, #276)', MONTHLY"})
	@DisplayName("a second authorization in the same session is delegated with the Third Party's state and redirect_uri")
	void secondAuthorizationInTheSameSessionIsDelegatedWithTheThirdPartysStateAndRedirectUri(
			String description, String second) throws Exception {
		String daily = "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000";
		String monthlyCallback = "https://tp.example/cb/monthly";
		String dailyCallback = "https://tp.example/cb/daily";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-299-tp-" + suffix;
		String clientSecret = "pr-299-secret-" + suffix;
		registeredClientRepository.save(RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(clientId)
				.clientName("#299 Test ThirdParty")
				.clientSecret("{noop}" + clientSecret)
				.clientIdIssuedAt(Instant.now())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri(monthlyCallback)
				.redirectUri(dailyCallback)
				.scope(SCOPE)
				.scope(daily)
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).requireProofKey(false).build())
				.tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.REFERENCE).build())
				.build());

		// 1) The first authorization (Monthly) completes: continue -> authorize -> code at the Monthly callback.
		String correlationId = "corr-299-" + suffix;
		delegationStateService.save(correlationId, clientId, monthlyCallback, "tp-state-1-" + suffix, SCOPE);
		Instant now = Instant.now();
		String handoff = codec.encode(SignedHandoff.Return.of(correlationId, now, now.plusSeconds(300),
				UUID.randomUUID().toString().replace("-", ""), CUSTOMER_ID, List.of(UUID.randomUUID()),
				SignedHandoff.Return.CONSENT_ALLOW, SCOPE));
		MvcResult continueResult = mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", handoff))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) continueResult.getRequest().getSession(false);
		String firstLocation = mockMvc.perform(get(java.net.URI.create(
						continueResult.getResponse().getHeader("Location"))).session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");
		assertThat(firstLocation).startsWith(monthlyCallback).contains("code=", "state=tp-state-1-" + suffix);

		// 2) In the same session, the Third Party starts the second authorization with its own state.
		boolean isDaily = "DAILY".equals(second);
		String secondScope = isDaily ? daily : SCOPE;
		String secondCallback = isDaily ? dailyCallback : monthlyCallback;
		String secondState = "tp-state-2-" + suffix;
		java.net.URI authorizeUri = org.springframework.web.util.UriComponentsBuilder.fromPath("/oauth2/authorize")
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("scope", secondScope)
				.queryParam("redirect_uri", secondCallback)
				.queryParam("state", secondState)
				.encode()
				.build()
				.toUri();
		String delegateUrl = mockMvc.perform(get(authorizeUri).session(session)
						.accept(org.springframework.http.MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");

		assertThat(delegateUrl)
				.as("the second authorization is delegated to the DC, not answered with a code")
				.doesNotContain("code=")
				.satisfies(url -> assertThat(java.net.URI.create(url).getPath()).isEqualTo("/authorize/delegate"))
				.satisfies(url -> assertThat(new String[] {extractQueryParam(url, "state"),
						extractQueryParam(url, "redirect_uri")})
						.as("the Third Party's own state and redirect_uri reach the delegate")
						.containsExactly(java.net.URLEncoder.encode(secondState, java.nio.charset.StandardCharsets.UTF_8),
								java.net.URLEncoder.encode(secondCallback, java.nio.charset.StandardCharsets.UTF_8)));

		// 3) The delegate saves the return leg with the Third Party's state and the callback it asked for.
		assertThat(delegationStateService.consume(delegateToTheDataCustodian(delegateUrl, session)))
				.isNotNull()
				.extracting(DelegationStateService.Entry::redirectUri, DelegationStateService.Entry::tpState,
						DelegationStateService.Entry::requestedScope)
				.containsExactly(secondCallback, secondState, secondScope);
	}

	@Test
	@DisplayName("seeded third_party client does not require PKCE (REQ.21 does not; a challenge sent is enforced, #274)")
	void seededThirdPartyClientDoesNotRequireProofKey() {
		RegisteredClient thirdParty = registeredClientRepository.findByClientId("third_party");
		assertThat(thirdParty)
				.as("default third_party client must be seeded")
				.isNotNull();
		assertThat(thirdParty.getClientSettings().isRequireProofKey())
				.as("REQ.21 does not require PKCE; the customer-flow client must not require code_challenge")
				.isFalse();
	}

	/** Follows the redirect to {@code /authorize/delegate}; returns the correlation id of the DC handoff. */
	private String delegateToTheDataCustodian(String delegateUrl, MockHttpSession session) throws Exception {
		org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder delegate =
				get(java.net.URI.create(delegateUrl));
		if (session != null) {
			delegate.session(session);
		}
		String dcUrl = mockMvc.perform(delegate)
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");
		return codec.decodeOutbound(extractQueryParam(dcUrl, "handoff")).correlationId();
	}

	/**
	 * #305: two authorizations of one customer run at once in one browser session (Monthly in one tab, Daily in
	 * another). Both reach {@code /oauth2/authorize/continue} before either replay is followed, which is when
	 * one slot of grant context and one shared consent mixed them up. Each replay must get a code (neither is
	 * sent back to the DC), and each token its own approved scope and UsagePoints.
	 */
	@Test
	@DisplayName("#305: two delegations at once in one session: each token gets its own scope and UsagePoints")
	void concurrentDelegationsInOneSessionEachGetTheirOwnGrant() throws Exception {
		String daily = "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-305-tp-" + suffix;
		String clientSecret = "pr-305-secret-" + suffix;
		registeredClientRepository.save(RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(clientId)
				.clientName("#305 Test ThirdParty")
				.clientSecret("{noop}" + clientSecret)
				.clientIdIssuedAt(Instant.now())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(REDIRECT_URI)
				.scope(SCOPE)
				.scope(daily)
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).requireProofKey(false).build())
				.tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.REFERENCE).build())
				.build());
		String monthlyId = "corr-305-m-" + suffix;
		String dailyId = "corr-305-d-" + suffix;
		UUID monthlyUsagePoint = UUID.randomUUID();
		UUID dailyUsagePoint = UUID.randomUUID();
		delegationStateService.save(monthlyId, clientId, REDIRECT_URI, "tp-m-" + suffix, SCOPE);
		delegationStateService.save(dailyId, clientId, REDIRECT_URI, "tp-d-" + suffix, daily);
		when(backchannelClient.provision(any())).thenReturn(new BackchannelResponse(UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), "https://dc.example/Subscription/1",
				"https://dc.example/Authorization/1", "https://dc.example/RetailCustomer/42/Customer/1"));

		// Both tabs return from the DC before either follows its replay.
		MvcResult monthlyContinue = mockMvc.perform(get("/oauth2/authorize/continue")
						.param("handoff", allowHandoff(monthlyId, SCOPE, monthlyUsagePoint)))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) monthlyContinue.getRequest().getSession(false);
		String dailyReplay = mockMvc.perform(get("/oauth2/authorize/continue").session(session)
						.param("handoff", allowHandoff(dailyId, daily, dailyUsagePoint)))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");
		String monthlyReplay = monthlyContinue.getResponse().getHeader("Location");

		// Monthly's replay first, then Daily's: each mints a code at the TP callback, with its own state.
		String monthlyCallback = followReplay(monthlyReplay, session);
		String dailyCallback = followReplay(dailyReplay, session);
		assertThat(monthlyCallback).startsWith(REDIRECT_URI).contains("code=", "state=tp-m-" + suffix);
		assertThat(dailyCallback).as("Daily's replay is not sent back to the DC")
				.startsWith(REDIRECT_URI).contains("code=", "state=tp-d-" + suffix);

		// Each code is provisioned with its own delegation's scope and UsagePoints.
		for (String callback : List.of(monthlyCallback, dailyCallback)) {
			mockMvc.perform(post("/oauth2/token")
							.with(httpBasic(clientId, clientSecret))
							.param("grant_type", "authorization_code")
							.param("code", extractQueryParam(callback, "code"))
							.param("redirect_uri", REDIRECT_URI))
					.andExpect(status().isOk());
		}
		ArgumentCaptor<BackchannelRequest> captor = ArgumentCaptor.forClass(BackchannelRequest.class);
		verify(backchannelClient, org.mockito.Mockito.times(2)).provision(captor.capture());
		assertThat(captor.getAllValues())
				.extracting(BackchannelRequest::correlationId, BackchannelRequest::grantedScope,
						BackchannelRequest::selectedUsagePointIds)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(monthlyId, SCOPE, List.of(monthlyUsagePoint)),
						org.assertj.core.groups.Tuple.tuple(dailyId, daily, List.of(dailyUsagePoint)));
	}

	/**
	 * #305: the browser never follows {@code /continue}'s replay (the tab is closed). Nothing it left may let a
	 * later authorization in that session skip the DC: no sign-in, no consent, and a grant context only its own
	 * replay can use.
	 */
	@Test
	@DisplayName("#305: a replay never followed lets no later authorization in the session skip the DC")
	void unfollowedReplayLetsNoLaterAuthorizationSkipTheDataCustodian() throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-305-u-" + suffix;
		registerClient(clientId, "pr-305-u-secret-" + suffix);
		String correlationId = "corr-305-u-" + suffix;
		delegationStateService.save(correlationId, clientId, REDIRECT_URI, "tp-u-" + suffix, SCOPE);
		MvcResult continueResult = mockMvc.perform(get("/oauth2/authorize/continue")
						.param("handoff", allowHandoff(correlationId, SCOPE, UUID.randomUUID())))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) continueResult.getRequest().getSession(false);
		String replay = continueResult.getResponse().getHeader("Location");

		// A new authorization with the same scope in the same session: delegated, not answered with a code.
		java.net.URI later = org.springframework.web.util.UriComponentsBuilder.fromPath("/oauth2/authorize")
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("scope", SCOPE)
				.queryParam("redirect_uri", REDIRECT_URI)
				.queryParam("state", "tp-later-" + suffix)
				.encode()
				.build()
				.toUri();
		assertDelegated(mockMvc.perform(get(later).session(session).accept(org.springframework.http.MediaType.TEXT_HTML))
				.andReturn());

		// The replay URL with another delegation's id, or in another browser session: delegated, no code.
		String forged = replay.replace(correlationId, "corr-305-unknown-" + suffix);
		assertDelegated(mockMvc.perform(get(java.net.URI.create(forged)).session(session)
				.accept(org.springframework.http.MediaType.TEXT_HTML)).andReturn());
		assertDelegated(mockMvc.perform(get(java.net.URI.create(replay))
				.accept(org.springframework.http.MediaType.TEXT_HTML)).andReturn());

		// Its own replay still completes, once.
		assertThat(followReplay(replay, session)).startsWith(REDIRECT_URI).contains("code=");
		assertDelegated(mockMvc.perform(get(java.net.URI.create(replay)).session(session)
				.accept(org.springframework.http.MediaType.TEXT_HTML)).andReturn());
	}

	/**
	 * #305 review: a Third Party {@code state} with {@code +}, {@code /} and {@code =} (standard Base64) must survive
	 * the replay. A raw {@code +} is read back as a space, so the replay would not match its delegation, the
	 * customer would be sent through the DC again, and the TP would get a state it never issued.
	 */
	@Test
	@DisplayName("#305 review: a state with '+', '/' and '=' gets a code on the first replay and comes back unchanged")
	void stateWithReservedCharactersComesBackUnchanged() throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String clientId = "pr-305-p-" + suffix;
		registerClient(clientId, "pr-305-p-secret-" + suffix);
		String correlationId = "corr-305-p-" + suffix;
		String tpState = "a+b/c==" + suffix;
		delegationStateService.save(correlationId, clientId, REDIRECT_URI, tpState, SCOPE);

		MvcResult continueResult = mockMvc.perform(get("/oauth2/authorize/continue")
						.param("handoff", allowHandoff(correlationId, SCOPE, UUID.randomUUID())))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		String callback = followReplay(continueResult.getResponse().getHeader("Location"),
				(MockHttpSession) continueResult.getRequest().getSession(false));

		assertThat(callback).startsWith(REDIRECT_URI).contains("code=")
				.satisfies(url -> assertThat(java.net.URLDecoder.decode(extractQueryParam(url, "state"),
						java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(tpState));
	}

	private static void assertDelegated(MvcResult result) {
		assertThat(result.getResponse())
				.satisfies(r -> assertThat(r.getStatus()).isBetween(300, 399))
				.satisfies(r -> assertThat(r.getHeader("Location"))
						.as("delegated to the DC, not answered with a code")
						.doesNotContain("code=")
						.satisfies(url -> assertThat(java.net.URI.create(url).getPath()).isEqualTo("/authorize/delegate")));
	}

	private String allowHandoff(String correlationId, String approvedScope, UUID usagePoint) {
		Instant now = Instant.now();
		return codec.encode(SignedHandoff.Return.of(correlationId, now, now.plusSeconds(300),
				UUID.randomUUID().toString().replace("-", ""), CUSTOMER_ID, List.of(usagePoint),
				SignedHandoff.Return.CONSENT_ALLOW, approvedScope));
	}

	/** Follows a /continue replay in {@code session}; returns where it redirects. */
	private String followReplay(String replay, MockHttpSession session) throws Exception {
		return mockMvc.perform(get(java.net.URI.create(replay)).session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn().getResponse().getHeader("Location");
	}

	/** Registers a confidential ESPI test client (auth-code + refresh, opaque tokens, no PKCE). */
	private void registerClient(String clientId, String clientSecret) {
		registeredClientRepository.save(RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(clientId)
				.clientName("#202 Test ThirdParty")
				.clientSecret("{noop}" + clientSecret)
				.clientIdIssuedAt(Instant.now())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri(REDIRECT_URI)
				.scope(SCOPE)
				.clientSettings(ClientSettings.builder()
						.requireAuthorizationConsent(true)
						.requireProofKey(false)
						.build())
				.tokenSettings(TokenSettings.builder()
						.accessTokenFormat(OAuth2TokenFormat.REFERENCE)
						.accessTokenTimeToLive(Duration.ofMinutes(360))
						.build())
				.build());
	}

	/** Simulates the DC return handoff and resumes /oauth2/authorize; returns the issued code. */
	private String obtainAuthorizationCode(String correlationId, UUID selectedUsagePoint) throws Exception {
		Instant now = Instant.now();
		String handoff = codec.encode(SignedHandoff.Return.of(
				correlationId, now, now.plusSeconds(300),
				UUID.randomUUID().toString().replace("-", ""),
				CUSTOMER_ID,
				List.of(selectedUsagePoint),
				SignedHandoff.Return.CONSENT_ALLOW,
				SCOPE));
		MvcResult continueResult = mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", handoff))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) continueResult.getRequest().getSession(false);
		// The Location is already percent-encoded; pass it as a URI so MockMvc doesn't encode it again.
		MvcResult authorizeResult = mockMvc.perform(
						get(java.net.URI.create(continueResult.getResponse().getHeader("Location"))).session(session))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return extractQueryParam(authorizeResult.getResponse().getHeader("Location"), "code");
	}

	private static String extractQueryParam(String url, String name) {
		return org.springframework.web.util.UriComponentsBuilder.fromUriString(url)
				.build().getQueryParams().getFirst(name);
	}
}
