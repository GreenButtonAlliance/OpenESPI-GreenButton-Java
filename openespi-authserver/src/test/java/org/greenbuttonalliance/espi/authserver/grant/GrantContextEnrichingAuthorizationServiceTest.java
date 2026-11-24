/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.grant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies that the wrapper stamps the grant context the replay of its delegation took onto the
 * OAuth2Authorization carrying the code, is transparent otherwise, never saves a code with another
 * client's or customer's context, and is idempotent across multiple saves of the same authorization
 * (Spring AS calls save() at code issue and again at token exchange).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GrantContextEnrichingAuthorizationService")
class GrantContextEnrichingAuthorizationServiceTest {

	private static final UUID UP_1 = UUID.fromString("00000000-0000-5000-8000-000000000001");
	private static final UUID UP_2 = UUID.fromString("00000000-0000-5000-8000-000000000002");
	private static final String REGISTERED_CLIENT_ID = "registered-client-id";

	@Mock
	private OAuth2AuthorizationService delegate;

	private GrantContextEnrichingAuthorizationService service;
	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		service = new GrantContextEnrichingAuthorizationService(delegate);
		request = new MockHttpServletRequest();
		// Spring AS calls save() inside an HTTP request thread; emulate that here so the wrapper
		// can resolve the current request via RequestContextHolder.
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	/** This request is the replay of a delegation: the filter took {@code ctx} from the session. */
	private void replayOf(GrantContext ctx) {
		request.setAttribute(GrantContextSessionStore.DELEGATED_CONTEXT_ATTRIBUTE, ctx);
	}

	private static GrantContext context(String correlationId, String principal, String scope, List<UUID> ups) {
		return GrantContexts.of(correlationId, "tp-1", REGISTERED_CLIENT_ID, principal, scope, ups,
				Instant.now().plusSeconds(300));
	}

	@Test
	@DisplayName("enriches the code's authorization with the replay's grant context, then delegates save")
	void enrichesFromTheReplaysContext() {
		replayOf(context("corr-1", "42", "FB_1;FB_4_5", List.of(UP_1, UP_2)));

		service.save(codeAuthorization("auth-1"));

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		assertThat(cap.getValue())
				.satisfies(saved -> assertThat(saved.<String>getAttribute(
						GrantContextEnrichingAuthorizationService.ATTR_RETAIL_CUSTOMER_ID)).isEqualTo("42"))
				.satisfies(saved -> assertThat(saved.<String>getAttribute(
						GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE)).isEqualTo("FB_1;FB_4_5"))
				.satisfies(saved -> assertThat(saved.<String>getAttribute(
						GrantContextEnrichingAuthorizationService.ATTR_CORRELATION_ID)).isEqualTo("corr-1"))
				// Selected UPs round-trip via parseUuids
				.satisfies(saved -> assertThat(GrantContextEnrichingAuthorizationService.parseUuids(saved.getAttribute(
						GrantContextEnrichingAuthorizationService.ATTR_SELECTED_USAGE_POINT_IDS)))
						.containsExactly(UP_1, UP_2));
	}

	@Test
	@DisplayName("already-enriched authorization passes through unchanged")
	void alreadyEnrichedNotDoubled() {
		replayOf(context("corr-3", "42", "FB_1", List.of()));

		// The incoming authorization already has the marker attribute, so the wrapper must leave it alone.
		OAuth2Authorization pre = OAuth2Authorization.from(codeAuthorization("auth-3"))
				.attribute(GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE, "FB_99")
				.build();
		service.save(pre);

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		assertThat(cap.getValue().<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE))
				.isEqualTo("FB_99");
	}

	@Test
	@DisplayName("#305: a request that is not a delegation's replay is not enriched")
	void requestWithoutADelegatedContextIsNotEnriched() {
		service.save(codeAuthorization("auth-4"));

		assertNotEnriched();
	}

	@Test
	@DisplayName("#305: a state-only authorization (consent-page redirect) is not enriched")
	void authorizationWithoutCodeIsNotEnriched() {
		replayOf(context("corr-5", "42", "FB_1", List.of()));

		service.save(authorization("auth-5").build());

		assertNotEnriched();
	}

	@Test
	@DisplayName("#305 review: a code is never saved with another customer's or client's context")
	void anotherCustomersContextFailsTheRequest() {
		replayOf(context("corr-6", "77", "FB_1", List.of()));

		assertThatThrownBy(() -> service.save(codeAuthorization("auth-6")))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(delegate);
	}

	@Test
	@DisplayName("no current HTTP request (M2M call) leaves authorization unchanged")
	void noRequestPassesThrough() {
		RequestContextHolder.resetRequestAttributes();

		service.save(codeAuthorization("auth-7"));

		assertNotEnriched();
	}

	@Test
	@DisplayName("parseUuids handles null/empty/blank inputs")
	void parseUuidsBoundaryInputs() {
		assertThat(GrantContextEnrichingAuthorizationService.parseUuids(null)).isEmpty();
		assertThat(GrantContextEnrichingAuthorizationService.parseUuids("")).isEmpty();
		assertThat(GrantContextEnrichingAuthorizationService.parseUuids("   ")).isEmpty();
	}

	private void assertNotEnriched() {
		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		assertThat(cap.getValue().<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE)).isNull();
	}

	private static OAuth2Authorization codeAuthorization(String id) {
		Instant now = Instant.now();
		return authorization(id)
				.token(new OAuth2AuthorizationCode("code-" + id, now, now.plusSeconds(300)))
				.build();
	}

	private static OAuth2Authorization.Builder authorization(String id) {
		RegisteredClient client = RegisteredClient.withId(REGISTERED_CLIENT_ID)
				.clientId("tp-1")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("https://tp.example/cb")
				.scope("FB_1")
				.build();
		return OAuth2Authorization.withRegisteredClient(client)
				.id(id)
				.principalName("42")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
	}
}
