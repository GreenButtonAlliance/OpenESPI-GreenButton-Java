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
import org.springframework.mock.web.MockHttpSession;
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
import static org.mockito.Mockito.verify;

/**
 * Verifies that the wrapper stamps the grant context of the delegation the request names onto the
 * OAuth2Authorization carrying the code, is transparent otherwise, and is idempotent across
 * multiple saves of the same authorization (Spring AS calls save() at code issue and again at
 * token exchange).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GrantContextEnrichingAuthorizationService")
class GrantContextEnrichingAuthorizationServiceTest {

	private static final UUID UP_1 = UUID.fromString("00000000-0000-5000-8000-000000000001");
	private static final UUID UP_2 = UUID.fromString("00000000-0000-5000-8000-000000000002");
	private static final String REGISTERED_CLIENT_ID = "registered-client-id";

	@Mock
	private OAuth2AuthorizationService delegate;

	private GrantContextSessionStore sessionStore;
	private GrantContextEnrichingAuthorizationService service;
	private MockHttpServletRequest request;
	private MockHttpSession session;

	@BeforeEach
	void setUp() {
		sessionStore = new GrantContextSessionStore();
		service = new GrantContextEnrichingAuthorizationService(delegate, sessionStore);
		request = new MockHttpServletRequest();
		session = new MockHttpSession();
		// Spring AS calls save() inside an HTTP request thread; emulate that here so the wrapper
		// can resolve the current request via RequestContextHolder.
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	/** The replay of delegation {@code correlationId}, in the session. */
	private void replayOf(String correlationId) {
		request.setSession(session);
		request.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, correlationId);
	}

	private static GrantContext context(String correlationId, String principal, String scope, List<UUID> ups) {
		return GrantContexts.of(correlationId, "tp-1", REGISTERED_CLIENT_ID, principal, scope, ups,
				Instant.now().plusSeconds(300));
	}

	@Test
	@DisplayName("enriches authorization with grant context from session, then delegates save")
	void enrichesFromSessionOnFirstSave() {
		replayOf("corr-1");
		sessionStore.put(session, context("corr-1", "42", "FB_1;FB_4_5", List.of(UP_1, UP_2)));

		service.save(codeAuthorization("auth-1"));

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		OAuth2Authorization saved = cap.getValue();
		assertThat(saved.<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_RETAIL_CUSTOMER_ID))
				.isEqualTo("42");
		assertThat(saved.<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE))
				.isEqualTo("FB_1;FB_4_5");
		assertThat(saved.<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_CORRELATION_ID))
				.isEqualTo("corr-1");

		// Selected UPs round-trip via parseUuids
		String serialized = saved.getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_SELECTED_USAGE_POINT_IDS);
		assertThat(GrantContextEnrichingAuthorizationService.parseUuids(serialized))
				.containsExactly(UP_1, UP_2);
	}

	@Test
	@DisplayName("session consumed single-use: subsequent save does not re-enrich")
	void sessionConsumedSingleUse() {
		replayOf("corr-2");
		sessionStore.put(session, context("corr-2", "42", "FB_1", List.of()));

		service.save(codeAuthorization("auth-2a"));
		service.save(codeAuthorization("auth-2b"));

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate, org.mockito.Mockito.times(2)).save(cap.capture());
		List<OAuth2Authorization> saved = cap.getAllValues();
		assertThat(saved.get(0).<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE))
				.isEqualTo("FB_1");
		assertThat(saved.get(1).<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE))
				.isNull();
	}

	@Test
	@DisplayName("already-enriched authorization passes through unchanged")
	void alreadyEnrichedNotDoubled() {
		replayOf("corr-3");
		// Session has fresh context...
		sessionStore.put(session, context("corr-3", "42", "FB_1", List.of()));

		// ...but the incoming authorization already has the marker attribute, so the wrapper
		// must leave it alone.
		OAuth2Authorization pre = OAuth2Authorization.from(codeAuthorization("auth-3"))
				.attribute(GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE, "FB_99")
				.build();
		service.save(pre);

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		assertThat(cap.getValue().<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE))
				.isEqualTo("FB_99");
		// Session context remains untouched
		assertThat(sessionStore.peek(session, "corr-3")).isNotNull();
	}

	@Test
	@DisplayName("#305: two delegations in one session: each replay takes its own context")
	void eachReplayTakesItsOwnDelegationsContext() {
		sessionStore.put(session, context("corr-a", "42", "FB_4_5_15", List.of(UP_1)));
		sessionStore.put(session, context("corr-b", "42", "FB_4_5_16", List.of(UP_2)));

		replayOf("corr-a");
		service.save(codeAuthorization("auth-a"));

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		assertThat(cap.getValue().<String>getAttribute(GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE))
				.isEqualTo("FB_4_5_15");
		assertThat(sessionStore.peek(session, "corr-b")).as("the other delegation keeps its context").isNotNull();
	}

	@Test
	@DisplayName("#305: a request naming no delegation takes no context")
	void requestNamingNoDelegationIsNotEnriched() {
		request.setSession(session);
		sessionStore.put(session, context("corr-4", "42", "FB_1", List.of()));

		service.save(codeAuthorization("auth-4"));

		assertNotEnriched();
		assertThat(sessionStore.peek(session, "corr-4")).isNotNull();
	}

	@Test
	@DisplayName("#305: a state-only authorization (consent-page redirect) does not take the context")
	void authorizationWithoutCodeIsNotEnriched() {
		replayOf("corr-5");
		sessionStore.put(session, context("corr-5", "42", "FB_1", List.of()));

		service.save(authorization("auth-5").build());

		assertNotEnriched();
		assertThat(sessionStore.peek(session, "corr-5")).isNotNull();
	}

	@Test
	@DisplayName("#305: another customer's context is not stamped")
	void anotherCustomersContextIsNotStamped() {
		replayOf("corr-6");
		sessionStore.put(session, context("corr-6", "77", "FB_1", List.of()));

		service.save(codeAuthorization("auth-6"));

		assertNotEnriched();
	}

	@Test
	@DisplayName("no current HTTP request (M2M call) leaves authorization unchanged")
	void noRequestPassesThrough() {
		RequestContextHolder.resetRequestAttributes();

		service.save(codeAuthorization("auth-7"));

		assertNotEnriched();
	}

	@Test
	@DisplayName("request without a session leaves authorization unchanged")
	void noSessionPassesThrough() {
		request.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, "corr-8");

		service.save(codeAuthorization("auth-8"));

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
