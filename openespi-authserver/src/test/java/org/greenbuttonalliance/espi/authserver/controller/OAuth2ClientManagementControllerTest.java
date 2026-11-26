/*
 * Copyright 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * #332: the client-management API accepts only the authentication methods a client created through it can use,
 * gives a public client no secret, and refuses fields it does not have instead of dropping them.
 */
@DisplayName("OAuth2ClientManagementController (#332)")
class OAuth2ClientManagementControllerTest {

	private static final String CLIENTS = "/api/v1/oauth2/clients";

	private final RegisteredClientRepository clients = mock(RegisteredClientRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		when(passwordEncoder.encode(anyString())).thenAnswer(call -> "{encoded}" + call.getArgument(0));
		mockMvc = MockMvcBuilders.standaloneSetup(
				new OAuth2ClientManagementController(clients, mock(JdbcTemplate.class), passwordEncoder)).build();
	}

	private MockHttpServletResponse create(String json) throws Exception {
		return mockMvc.perform(post(CLIENTS).contentType(MediaType.APPLICATION_JSON).content(json))
				.andReturn().getResponse();
	}

	private RegisteredClient saved() {
		ArgumentCaptor<RegisteredClient> client = ArgumentCaptor.forClass(RegisteredClient.class);
		verify(clients).save(client.capture());
		return client.getValue();
	}

	@Test
	@DisplayName("a public client (authentication method none only) gets no secret and requires PKCE")
	void publicClientGetsNoSecret() throws Exception {
		MockHttpServletResponse response = create("""
				{"client_name":"Public TP","authentication_methods":["none"],
				 "redirect_uris":["https://tp.example/cb"]}
				""");

		assertThat(response.getStatus()).isEqualTo(201);
		assertThat(response.getContentAsString()).contains("\"client_id\"").doesNotContain("client_secret");
		assertThat(saved()).satisfies(c -> assertThat(c.getClientSecret()).isNull())
				.satisfies(c -> assertThat(c.getClientAuthenticationMethods())
						.containsExactly(ClientAuthenticationMethod.NONE))
				.extracting(RegisteredClient::getClientSettings).extracting(ClientSettings::isRequireProofKey)
				.isEqualTo(true);
		verify(passwordEncoder, never()).encode(anyString());
	}

	@Test
	@DisplayName("a confidential client gets a secret, returned once and stored encoded")
	void confidentialClientGetsASecret() throws Exception {
		MockHttpServletResponse response = create("""
				{"client_name":"Confidential TP","authentication_methods":["client_secret_post"],
				 "redirect_uris":["https://tp.example/cb"]}
				""");

		assertThat(response.getStatus()).isEqualTo(201);
		assertThat(response.getContentAsString()).contains("\"client_secret\"");
		assertThat(saved()).satisfies(c -> assertThat(c.getClientSecret()).startsWith("{encoded}"))
				.extracting(RegisteredClient::getClientAuthenticationMethods).asInstanceOf(
						org.assertj.core.api.InstanceOfAssertFactories.collection(ClientAuthenticationMethod.class))
				.containsExactly(ClientAuthenticationMethod.CLIENT_SECRET_POST);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"client_secret_basik", "tls_client_auth", "self_signed_tls_client_auth",
			"private_key_jwt", "client_secret_jwt", ""})
	@DisplayName("an authentication method a client created here cannot use is refused, and nothing is saved")
	void unsupportedAuthenticationMethodIsRefused(String method) throws Exception {
		MockHttpServletResponse response = create("""
				{"client_name":"TP","authentication_methods":["client_secret_basic","%s"]}
				""".formatted(method));

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getContentAsString()).contains("invalid_request").contains("authentication_methods");
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("a public client cannot have the client_credentials grant: it has nothing to authenticate with")
	void publicClientCannotUseClientCredentials() throws Exception {
		MockHttpServletResponse response = create("""
				{"client_name":"TP","authentication_methods":["none"],"grant_types":["client_credentials"]}
				""");

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getContentAsString()).contains("client_credentials");
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("a create request naming a field it does not have (the old spec's camelCase) is refused, not dropped")
	void createWithUnknownFieldIsRefused() throws Exception {
		MockHttpServletResponse response = create("""
				{"client_name":"TP","clientSettings":{"requireProofKey":true},
				 "clientAuthenticationMethods":["client_secret_basic"]}
				""");

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getContentAsString()).contains("clientAuthenticationMethods").contains("clientSettings");
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("an update naming a field it does not apply (settings, authentication methods) is refused, not ignored")
	void updateWithUnknownFieldIsRefused() throws Exception {
		when(clients.findByClientId("tp")).thenReturn(client());

		MockHttpServletResponse response = mockMvc.perform(put(CLIENTS + "/tp").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"client_name":"Renamed","authentication_methods":["none"],"require_proof_key":true}
						""")).andReturn().getResponse();

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getContentAsString()).contains("authentication_methods").contains("require_proof_key");
		verify(clients, never()).save(any());
	}

	@Test
	@DisplayName("an update with only the fields it applies still succeeds")
	void updateWithKnownFieldsSucceeds() throws Exception {
		when(clients.findByClientId("tp")).thenReturn(client());

		MockHttpServletResponse response = mockMvc.perform(put(CLIENTS + "/tp").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"client_name":"Renamed","redirect_uris":["https://tp.example/new-cb"]}
						""")).andReturn().getResponse();

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(saved()).satisfies(c -> assertThat(c.getClientName()).isEqualTo("Renamed"))
				.extracting(RegisteredClient::getRedirectUris).asInstanceOf(
						org.assertj.core.api.InstanceOfAssertFactories.collection(String.class))
				.containsExactly("https://tp.example/new-cb");
	}

	private static RegisteredClient client() {
		return RegisteredClient.withId("id-tp").clientId("tp").clientSecret("{noop}secret")
				.clientIdIssuedAt(Instant.now())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("https://tp.example/cb").scope("FB=4_5_15").build();
	}
}
