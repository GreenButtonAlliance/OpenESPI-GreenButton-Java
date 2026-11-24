/*
 * Copyright 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.grant;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Test {@link GrantContext}s. */
public final class GrantContexts {

	private GrantContexts() {
	}

	/** A live context of customer 42 for client {@code tp} (registered id {@code reg-tp}). */
	public static GrantContext of(String correlationId, String approvedScope, List<UUID> usagePoints) {
		return of(correlationId, "tp", "reg-tp", "42", approvedScope, usagePoints, Instant.now().plusSeconds(300));
	}

	public static GrantContext of(String correlationId, String clientId, String registeredClientId, String principal,
								  String approvedScope, List<UUID> usagePoints, Instant expiresAt) {
		return new GrantContext(correlationId, clientId, registeredClientId, principal, Long.valueOf(principal),
				approvedScope, usagePoints, expiresAt);
	}
}
