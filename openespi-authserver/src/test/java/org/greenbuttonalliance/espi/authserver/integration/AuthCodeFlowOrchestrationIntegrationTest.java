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

import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelRequest;
import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelResponse;
import org.greenbuttonalliance.espi.authserver.backchannel.DataCustodianBackchannelClient;
import org.greenbuttonalliance.espi.authserver.web.delegate.DelegationStateService;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * <p>The flow is exercised with a single-term ESPI scope ({@code FB_1}) registered on a test client
 * created per-run, so the orchestration is verified deterministically without depending on the
 * seeded {@code third_party} client's multi-term scope.</p>
 */
@DisplayName("Phase 2.0 auth-code-flow orchestration (AS-side, DC back-channel mocked)")
class AuthCodeFlowOrchestrationIntegrationTest extends AbstractAuthserverIntegrationTest {

	private static final String REDIRECT_URI = "https://tp.example/cb";
	private static final String SCOPE = "FB_1";
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
						.requireProofKey(false) // ESPI does not use PKCE
						.build())
				.tokenSettings(TokenSettings.builder()
						.accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI: opaque
						.accessTokenTimeToLive(Duration.ofMinutes(360))
						.build())
				.build();
		registeredClientRepository.save(testClient);

		// The outbound leg already happened in real life; seed the delegation state it would have left.
		delegationStateService.save(correlationId, clientId, REDIRECT_URI, "tp-state-" + suffix);

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
		MvcResult authorizeResult = mockMvc.perform(get(authorizeUrl).session(session))
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
		String accessToken = new com.fasterxml.jackson.databind.ObjectMapper()
				.readTree(tokenResult.getResponse().getContentAsString()).get("access_token").asText();

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

	@Test
	@DisplayName("seeded third_party client has PKCE disabled (ESPI does not support PKCE)")
	void seededThirdPartyClientDoesNotRequireProofKey() {
		RegisteredClient thirdParty = registeredClientRepository.findByClientId("third_party");
		assertThat(thirdParty)
				.as("default third_party client must be seeded")
				.isNotNull();
		assertThat(thirdParty.getClientSettings().isRequireProofKey())
				.as("ESPI does not support PKCE; the customer-flow client must not require code_challenge")
				.isFalse();
	}

	private static String extractQueryParam(String url, String name) {
		return org.springframework.web.util.UriComponentsBuilder.fromUriString(url)
				.build().getQueryParams().getFirst(name);
	}
}
