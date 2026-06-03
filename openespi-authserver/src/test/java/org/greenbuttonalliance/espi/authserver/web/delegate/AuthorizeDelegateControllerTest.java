/*
 * Copyright 2025 Green Button Alliance, Inc.
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
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
 *   <li>When {@code redirect_uri} is omitted from the request (Spring AS's consentPage redirect
 *       doesn't always include it), falls back to the first registered redirect_uri on the
 *       {@link RegisteredClient}.</li>
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
	private RegisteredClientRepository registeredClientRepository;

	private DelegationStateService delegationStateService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		delegationStateService = new DelegationStateService();
		AuthorizeDelegateController controller = new AuthorizeDelegateController(
				handoffBuilder, dcIntegration, delegationStateService, registeredClientRepository);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		when(dcIntegration.getBaseUrl()).thenReturn("https://dc.example/DataCustodian");
		when(handoffBuilder.buildSignedToken(anyString(), anyString(), anyString()))
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

	@Test
	@DisplayName("missing redirect_uri falls back to first registered redirect_uri")
	void missingRedirectUriFallsBackToRegisteredClient() throws Exception {
		RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("tp-2")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("https://tp2.example/registered-cb")
				.scope("FB_1")
				.build();
		when(registeredClientRepository.findByClientId("tp-2")).thenReturn(client);

		mockMvc.perform(get("/authorize/delegate")
						.param("scope", "FB_1")
						.param("client_id", "tp-2")
						.param("state", "tp-state-xyz"))
				.andExpect(status().is3xxRedirection());

		assertThat(delegationStateService.consume("tp-state-xyz"))
				.isNotNull()
				.extracting(DelegationStateService.Entry::redirectUri)
				.isEqualTo("https://tp2.example/registered-cb");
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
