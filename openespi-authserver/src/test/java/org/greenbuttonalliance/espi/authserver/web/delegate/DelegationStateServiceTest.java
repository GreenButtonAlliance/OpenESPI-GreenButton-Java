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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DelegationStateService")
class DelegationStateServiceTest {

	private DelegationStateService service;

	@BeforeEach
	void setUp() {
		service = new DelegationStateService();
	}

	@Test
	@DisplayName("save+consume round-trip returns the saved entry exactly once")
	void saveThenConsumeReturnsEntryOnce() {
		String cid = "corr-1";
		service.save(cid, "client-x", "https://tp.example/cb", "tp-state");

		DelegationStateService.Entry first = service.consume(cid);
		assertThat(first).isNotNull()
				.extracting(DelegationStateService.Entry::clientId,
						DelegationStateService.Entry::redirectUri,
						DelegationStateService.Entry::tpState)
				.containsExactly("client-x", "https://tp.example/cb", "tp-state");

		// Single-use: a second consume returns null
		assertThat(service.consume(cid)).isNull();
	}

	@Test
	@DisplayName("consume returns null for unknown correlation id")
	void consumeUnknownReturnsNull() {
		assertThat(service.consume("never-saved")).isNull();
	}

	@Test
	@DisplayName("peek returns entry without removing it")
	void peekReturnsWithoutRemoving() {
		service.save("corr-peek", "client-x", "https://tp.example/cb", "tp-state");

		assertThat(service.peek("corr-peek")).isNotNull();
		// Still consumable after peek
		assertThat(service.consume("corr-peek")).isNotNull();
		// And then gone
		assertThat(service.consume("corr-peek")).isNull();
	}

	@Test
	@DisplayName("save preserves null tpState (TP omitted state param)")
	void saveAcceptsNullTpState() {
		service.save("corr-2", "client-x", "https://tp.example/cb", null);

		assertThat(service.consume("corr-2"))
				.isNotNull()
				.extracting(DelegationStateService.Entry::tpState)
				.isNull();
	}
}
