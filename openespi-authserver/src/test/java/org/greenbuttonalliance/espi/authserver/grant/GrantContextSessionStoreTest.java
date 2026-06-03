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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

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
		GrantContext ctx = new GrantContext(
				"corr-1", 42L, "FB_1;FB_4_5",
				List.of(UUID.fromString("00000000-0000-5000-8000-000000000001")),
				"https://dc.example/cust/42");
		store.put(session, ctx);

		assertThat(store.consume(session)).isEqualTo(ctx);
		// Single-use: a second consume returns null
		assertThat(store.consume(session)).isNull();
	}

	@Test
	@DisplayName("consume on a fresh session returns null")
	void consumeFreshSessionReturnsNull() {
		assertThat(store.consume(session)).isNull();
	}

	@Test
	@DisplayName("peek does not remove the entry")
	void peekDoesNotRemove() {
		GrantContext ctx = new GrantContext("corr-2", 99L, "FB_1", List.of(), null);
		store.put(session, ctx);

		assertThat(store.peek(session)).isEqualTo(ctx);
		// Still consumable after peek
		assertThat(store.consume(session)).isEqualTo(ctx);
		assertThat(store.consume(session)).isNull();
	}
}
