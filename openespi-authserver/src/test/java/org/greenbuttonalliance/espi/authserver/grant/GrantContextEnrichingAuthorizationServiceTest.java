/*
 * Copyright 2025 Green Button Alliance, Inc.
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
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Verifies that the wrapper stamps grant context onto OAuth2Authorization.attributes when the HTTP
 * session carries a {@link GrantContext}, is transparent otherwise, and is idempotent across
 * multiple saves of the same authorization (Spring AS calls save() at code issue and again at
 * token exchange).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GrantContextEnrichingAuthorizationService")
class GrantContextEnrichingAuthorizationServiceTest {

	private static final UUID UP_1 = UUID.fromString("00000000-0000-5000-8000-000000000001");
	private static final UUID UP_2 = UUID.fromString("00000000-0000-5000-8000-000000000002");

	@Mock
	private OAuth2AuthorizationService delegate;

	private GrantContextSessionStore sessionStore;
	private GrantContextEnrichingAuthorizationService service;
	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		sessionStore = new GrantContextSessionStore();
		service = new GrantContextEnrichingAuthorizationService(delegate, sessionStore);
		request = new MockHttpServletRequest();
		// Spring AS calls save() inside an HTTP request thread; emulate that here so the wrapper
		// can resolve the current session via RequestContextHolder.
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	@DisplayName("enriches authorization with grant context from session, then delegates save")
	void enrichesFromSessionOnFirstSave() {
		MockHttpSession session = new MockHttpSession();
		request.setSession(session);
		GrantContext ctx = new GrantContext(
				"corr-1", 42L, "FB_1;FB_4_5", List.of(UP_1, UP_2));
		sessionStore.put(session, ctx);

		service.save(blankAuthorization("auth-1"));

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
		MockHttpSession session = new MockHttpSession();
		request.setSession(session);
		GrantContext ctx = new GrantContext("corr-2", 1L, "FB_1", List.of());
		sessionStore.put(session, ctx);

		service.save(blankAuthorization("auth-2a"));
		service.save(blankAuthorization("auth-2b"));

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
		MockHttpSession session = new MockHttpSession();
		request.setSession(session);
		// Session has fresh context...
		sessionStore.put(session, new GrantContext("corr-3", 1L, "FB_1", List.of()));

		// ...but the incoming authorization already has the marker attribute, so the wrapper
		// must leave it alone.
		OAuth2Authorization pre = OAuth2Authorization.from(blankAuthorization("auth-3"))
				.attribute(GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE, "FB_99")
				.build();
		service.save(pre);

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		assertThat(cap.getValue().<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE))
				.isEqualTo("FB_99");
		// Session context remains untouched
		assertThat(sessionStore.peek(session)).isNotNull();
	}

	@Test
	@DisplayName("no current HTTP request (M2M call) leaves authorization unchanged")
	void noRequestPassesThrough() {
		RequestContextHolder.resetRequestAttributes();

		service.save(blankAuthorization("auth-4"));

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		assertThat(cap.getValue().<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE)).isNull();
	}

	@Test
	@DisplayName("request without a session leaves authorization unchanged")
	void noSessionPassesThrough() {
		// MockHttpServletRequest with no session created
		service.save(blankAuthorization("auth-5"));

		ArgumentCaptor<OAuth2Authorization> cap = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(delegate).save(cap.capture());
		assertThat(cap.getValue().<String>getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE)).isNull();
	}

	@Test
	@DisplayName("parseUuids handles null/empty/blank inputs")
	void parseUuidsBoundaryInputs() {
		assertThat(GrantContextEnrichingAuthorizationService.parseUuids(null)).isEmpty();
		assertThat(GrantContextEnrichingAuthorizationService.parseUuids("")).isEmpty();
		assertThat(GrantContextEnrichingAuthorizationService.parseUuids("   ")).isEmpty();
	}

	private static OAuth2Authorization blankAuthorization(String id) {
		RegisteredClient client = RegisteredClient.withId("registered-client-id")
				.clientId("tp-1")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("https://tp.example/cb")
				.scope("FB_1")
				.build();
		return OAuth2Authorization.withRegisteredClient(client)
				.id(id)
				.principalName("42")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.build();
	}
}
