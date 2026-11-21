/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.web.delegate;

import org.greenbuttonalliance.espi.authserver.config.DataCustodianIntegrationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the {@code /authorize/delegate} endpoint that bridges Spring Authorization Server's
 * loginPage/consentPage redirects to the DC's Authorization Screen.
 *
 * <p>Key behaviors covered:</p>
 * <ul>
 *   <li>Builds a signed outbound handoff and 302s to {@code {dc.baseUrl}/oauth/authorize-screen}
 *       with the signed token as a query param.</li>
 *   <li>Saves delegation state (clientId, redirectUri, tpState) in {@link DelegationStateService}
 *       under the correlation id so {@link AuthorizeContinueController} can later look it up.</li>
 *   <li>Spring AS's consent-page redirect carries Spring's own consent {@code state} and no
 *       {@code redirect_uri} (#299): the Third Party's {@code state} and {@code redirect_uri} are taken
 *       from the original request saved with the pending authorization under that state. Without one
 *       the request is refused (400), never sent to a guessed registered redirect_uri.</li>
 *   <li>When {@code state} is omitted, generates a fresh correlation id rather than using a blank.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizeDelegateController")
class AuthorizeDelegateControllerTest {

	@Mock
	private OutboundHandoffBuilder handoffBuilder;
	@Mock
	private DataCustodianIntegrationConfig dcIntegration;
	@Mock
	private OAuth2AuthorizationService authorizationService;
	@Mock
	private RegisteredClientRepository registeredClientRepository;

	private DelegationStateService delegationStateService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		delegationStateService = new DelegationStateService();
		AuthorizeDelegateController controller = new AuthorizeDelegateController(
				handoffBuilder, dcIntegration, delegationStateService, authorizationService, registeredClientRepository);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		lenient().when(dcIntegration.getBaseUrl()).thenReturn("https://dc.example/DataCustodian");
		lenient().when(handoffBuilder.buildSignedToken(anyString(), anyString(), anyString()))
				.thenReturn("signed-token-stub");
	}

	@Test
	@DisplayName("delegate with full params redirects to DC and saves state under TP state as correlation id")
	void delegateRedirectsAndSavesState() throws Exception {
		mockMvc.perform(get("/authorize/delegate")
						.param("scope", "FB_1;FB_4_5")
						.param("client_id", "tp-1")
						.param("state", "tp-state-abc")
						.param("redirect_uri", "https://tp.example/cb"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern(
						"https://dc.example/DataCustodian/oauth/authorize-screen?handoff=*"));

		// State was saved under the TP state value (= correlation id)
		DelegationStateService.Entry saved = delegationStateService.consume("tp-state-abc");
		assertThat(saved).isNotNull()
				.extracting(DelegationStateService.Entry::clientId,
						DelegationStateService.Entry::redirectUri,
						DelegationStateService.Entry::tpState)
				.containsExactly("tp-1", "https://tp.example/cb", "tp-state-abc");

		// Builder was called with the matching correlation id
		ArgumentCaptor<String> cidCap = ArgumentCaptor.forClass(String.class);
		verify(handoffBuilder).buildSignedToken(eq("tp-1"), eq("FB_1;FB_4_5"), cidCap.capture());
		assertThat(cidCap.getValue()).isEqualTo("tp-state-abc");
	}

	/** A pending authorization as Spring AS saves it before redirecting to the consent page. */
	private static OAuth2Authorization pendingConsent(String clientId, String consentState, String tpState,
													  String redirectUri) {
		RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(clientId)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				// two callbacks: the one used must be the request's, not one picked from the registration
				.redirectUri("https://tp2.example/monthly-cb")
				.redirectUri("https://tp2.example/daily-cb")
				.scope("FB_1")
				.build();
		OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
				.authorizationUri("https://as.example/oauth2/authorize")
				.clientId(clientId)
				.redirectUri(redirectUri)
				.scope("FB_1")
				.state(tpState)
				.build();
		return OAuth2Authorization.withRegisteredClient(client)
				.principalName("42")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.attribute(OAuth2ParameterNames.STATE, consentState)
				.attribute(OAuth2AuthorizationRequest.class.getName(), request)
				.build();
	}

	@Test
	@DisplayName("consent-page entry (#299): the Third Party's state and redirect_uri come from the pending authorization")
	void consentPageEntryUsesTheThirdPartysStateAndRedirectUri() throws Exception {
		OAuth2Authorization pending = pendingConsent("tp-2", "spring-consent-state", "tp-state-xyz",
				"https://tp2.example/daily-cb");
		when(authorizationService.findByToken("spring-consent-state", new OAuth2TokenType(OAuth2ParameterNames.STATE)))
				.thenReturn(pending);

		mockMvc.perform(get("/authorize/delegate")
						.param("scope", "FB_1")
						.param("client_id", "tp-2")
						.param("state", "spring-consent-state"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("https://dc.example/DataCustodian/oauth/authorize-screen?handoff=*"));

		assertThat(delegationStateService.consume("tp-state-xyz"))
				.isNotNull()
				.extracting(DelegationStateService.Entry::clientId, DelegationStateService.Entry::redirectUri,
						DelegationStateService.Entry::tpState)
				.containsExactly("tp-2", "https://tp2.example/daily-cb", "tp-state-xyz");
		verify(handoffBuilder).buildSignedToken("tp-2", "FB_1", "tp-state-xyz");
		// the abandoned consent-page authorization is not left behind
		verify(authorizationService).remove(pending);
	}

	private static RegisteredClient clientWithCallbacks(String clientId, String... redirectUris) {
		RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(clientId)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.scope("FB_1");
		for (String uri : redirectUris) {
			builder.redirectUri(uri);
		}
		return builder.build();
	}

	@Test
	@DisplayName("no redirect_uri from a client with one registered redirect_uri (RFC 6749 3.1.2.3): that one is used")
	void noRedirectUriFromAClientWithOneRegisteredRedirectUriUsesIt() throws Exception {
		when(registeredClientRepository.findByClientId("tp-2"))
				.thenReturn(clientWithCallbacks("tp-2", "https://tp2.example/registered-cb"));

		mockMvc.perform(get("/authorize/delegate")
						.param("scope", "FB_1")
						.param("client_id", "tp-2")
						.param("state", "tp-state-xyz"))
				.andExpect(status().is3xxRedirection());

		assertThat(delegationStateService.consume("tp-state-xyz"))
				.isNotNull()
				.extracting(DelegationStateService.Entry::redirectUri, DelegationStateService.Entry::tpState)
				.containsExactly("https://tp2.example/registered-cb", "tp-state-xyz");
	}

	@Test
	@DisplayName("no redirect_uri, no pending authorization and two registered redirect_uris: 400, never a guess")
	void noRedirectUriAndNoPendingAuthorizationIsRefused() throws Exception {
		when(registeredClientRepository.findByClientId("tp-2"))
				.thenReturn(clientWithCallbacks("tp-2", "https://tp2.example/monthly-cb", "https://tp2.example/daily-cb"));

		mockMvc.perform(get("/authorize/delegate")
						.param("scope", "FB_1")
						.param("client_id", "tp-2")
						.param("state", "unknown-state"))
				.andExpect(status().isBadRequest());

		assertThat(delegationStateService.consume("unknown-state")).isNull();
		verify(handoffBuilder, never()).buildSignedToken(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("a pending authorization of another client is not used: 400")
	void pendingAuthorizationOfAnotherClientIsRefused() throws Exception {
		when(authorizationService.findByToken("spring-consent-state", new OAuth2TokenType(OAuth2ParameterNames.STATE)))
				.thenReturn(pendingConsent("tp-other", "spring-consent-state", "tp-state-xyz", "https://tp2.example/daily-cb"));

		mockMvc.perform(get("/authorize/delegate")
						.param("scope", "FB_1")
						.param("client_id", "tp-2")
						.param("state", "spring-consent-state"))
				.andExpect(status().isBadRequest());

		assertThat(delegationStateService.consume("tp-state-xyz")).isNull();
		verify(handoffBuilder, never()).buildSignedToken(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("missing state still triggers redirect; correlation id is generated")
	void missingStateGeneratesCorrelationId() throws Exception {
		mockMvc.perform(get("/authorize/delegate")
						.param("scope", "FB_1")
						.param("client_id", "tp-3")
						.param("redirect_uri", "https://tp3.example/cb"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern(
						"https://dc.example/DataCustodian/oauth/authorize-screen?handoff=*"));

		ArgumentCaptor<String> cidCap = ArgumentCaptor.forClass(String.class);
		verify(handoffBuilder).buildSignedToken(eq("tp-3"), eq("FB_1"), cidCap.capture());
		assertThat(cidCap.getValue()).isNotBlank();
	}
}
