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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GrantContextSessionStore")
class GrantContextSessionStoreTest {

	private GrantContextSessionStore store;
	private MockHttpSession session;

	@BeforeEach
	void setUp() {
		store = new GrantContextSessionStore();
		session = new MockHttpSession();
	}

	@Test
	@DisplayName("put + consume round-trip returns the exact context once")
	void putThenConsumeReturnsContextOnce() {
		GrantContext ctx = GrantContexts.of("corr-1", "FB_1;FB_4_5",
				List.of(UUID.fromString("00000000-0000-5000-8000-000000000001")));
		store.put(session, ctx);

		assertThat(store.consume(session, "corr-1")).isEqualTo(ctx);
		// Single-use: a second consume returns null
		assertThat(store.consume(session, "corr-1")).isNull();
	}

	@Test
	@DisplayName("consume on a fresh session, or without an id, returns null")
	void consumeFreshSessionReturnsNull() {
		assertThat(store.consume(session, "corr-1")).isNull();
		assertThat(store.consume(null, "corr-1")).isNull();
		assertThat(store.consume(session, null)).isNull();
	}

	@Test
	@DisplayName("peek does not remove the entry")
	void peekDoesNotRemove() {
		GrantContext ctx = GrantContexts.of("corr-2", "FB_1", List.of());
		store.put(session, ctx);

		assertThat(store.peek(session, "corr-2")).isEqualTo(ctx);
		// Still consumable after peek
		assertThat(store.consume(session, "corr-2")).isEqualTo(ctx);
		assertThat(store.consume(session, "corr-2")).isNull();
	}

	@Test
	@DisplayName("#305: two delegations in one session each keep their own context")
	void twoDelegationsKeepTheirOwnContexts() {
		GrantContext monthly = GrantContexts.of("corr-a", "FB_4_5_15", List.of(UUID.randomUUID()));
		GrantContext daily = GrantContexts.of("corr-b", "FB_4_5_16", List.of(UUID.randomUUID()));
		store.put(session, monthly);
		store.put(session, daily);

		assertThat(store.consume(session, "corr-a")).isEqualTo(monthly);
		assertThat(store.consume(session, "corr-b")).isEqualTo(daily);
	}

	@Test
	@DisplayName("#305: an expired context is never returned, and is dropped by the next put")
	void expiredContextIsIgnored() {
		store.put(session, GrantContexts.of("corr-old", "tp", "reg-tp", "42", "FB_1", List.of(),
				Instant.now().minusSeconds(1)));

		assertThat(store.peek(session, "corr-old")).isNull();
		store.put(session, GrantContexts.of("corr-new", "FB_1", List.of()));
		assertThat(store.consume(session, "corr-old")).isNull();
	}

	@Test
	@DisplayName("#305: the request form takes the delegation the request names, once, and only in its session")
	void requestNamesItsDelegation() {
		GrantContext ctx = GrantContexts.of("corr-r", "FB_1", List.of());
		store.put(session, ctx);
		MockHttpServletRequest named = new MockHttpServletRequest();
		named.setSession(session);
		named.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, "corr-r");
		MockHttpServletRequest unnamed = new MockHttpServletRequest();
		unnamed.setSession(session);
		MockHttpServletRequest otherSession = new MockHttpServletRequest();
		otherSession.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, "corr-r");

		assertThat(store.consume(unnamed)).isNull();
		assertThat(store.consume(otherSession)).isNull();
		assertThat(store.consume(named)).isEqualTo(ctx);
		assertThat(store.consume(named)).as("a second request for it").isNull();
	}

	@Test
	@DisplayName("#305: the delegated context is the one kept on the request, and only there")
	void delegatedIsTheRequestAttribute() {
		GrantContext ctx = GrantContexts.of("corr-d", "FB_1", List.of());
		MockHttpServletRequest request = new MockHttpServletRequest();

		assertThat(GrantContextSessionStore.delegated(request)).isNull();
		assertThat(GrantContextSessionStore.delegated(null)).isNull();
		request.setAttribute(GrantContextSessionStore.DELEGATED_CONTEXT_ATTRIBUTE, ctx);
		assertThat(GrantContextSessionStore.delegated(request)).isEqualTo(ctx);
	}
}
