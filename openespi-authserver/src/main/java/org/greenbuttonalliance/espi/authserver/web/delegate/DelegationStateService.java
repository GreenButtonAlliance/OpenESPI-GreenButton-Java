/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.authserver.web.delegate;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks AS-side delegation context between
 * {@link AuthorizeDelegateController} (where the customer is redirected to DC) and
 * {@link AuthorizeContinueController} (where the customer returns from DC).
 *
 * <p>The handoff codec is stateless: the outbound and return tokens are independently signed and
 * verified. But to redirect cleanly back to the third party on a deny &mdash; per RFC 6749
 * &sect;4.1.2.1, with the original {@code redirect_uri} and {@code state} &mdash; the AS needs to
 * remember those values from the original {@code /oauth2/authorize} request. This service holds
 * them keyed by the same {@code correlation_id} the outbound handoff carries.</p>
 *
 * <p><strong>Sandbox implementation:</strong> in-memory {@link ConcurrentMap} with a short TTL.
 * Production deployments would back this with Redis or a JDBC table for horizontal scaling and
 * crash resilience. Acceptable here because the only state is per-flow ephemera (5-minute
 * lifetime matching the handoff TTL).</p>
 */
@Service
public class DelegationStateService {

	private static final Duration TTL = Duration.ofMinutes(5);

	private final ConcurrentMap<String, Entry> states = new ConcurrentHashMap<>();

	/**
	 * Save the original third-party request context so it can be retrieved when the customer
	 * returns from DC via {@link #consume(String)}.
	 */
	public void save(String correlationId, String clientId, String redirectUri, String tpState) {
		states.put(correlationId, new Entry(clientId, redirectUri, tpState, Instant.now().plus(TTL)));
		// Lazy reaping: drop expired entries on each save call.
		Instant now = Instant.now();
		states.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
	}

	/**
	 * Look up and remove the saved state. Returns null if the correlation id is unknown or expired.
	 * Single-use: the caller is expected to redirect to the third party once per outbound flow.
	 */
	public Entry consume(String correlationId) {
		Entry entry = states.remove(correlationId);
		if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
			return null;
		}
		return entry;
	}

	/** Read-without-remove for cases where the caller may need to retry. */
	public Entry peek(String correlationId) {
		Entry entry = states.get(correlationId);
		if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
			return null;
		}
		return entry;
	}

	/**
	 * Per-flow context saved at {@link AuthorizeDelegateController#delegate} time.
	 *
	 * @param clientId    OAuth2 client_id of the requesting third party
	 * @param redirectUri the third party's redirect_uri from the original {@code /oauth2/authorize}
	 *                    request &mdash; the URL the customer's user-agent will be sent to with
	 *                    either {@code code=...} (allow) or {@code error=access_denied} (deny)
	 * @param tpState     the {@code state} parameter the third party supplied in its original
	 *                    request &mdash; echoed back verbatim per OAuth2 spec
	 * @param expiresAt   when this entry should be considered stale
	 */
	public record Entry(String clientId, String redirectUri, String tpState, Instant expiresAt) {}
}
