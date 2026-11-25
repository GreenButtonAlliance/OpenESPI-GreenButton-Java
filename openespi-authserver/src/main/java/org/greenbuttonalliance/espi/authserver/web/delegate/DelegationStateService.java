/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
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
	public void save(String correlationId, String clientId, String redirectUri, String tpState,
					 String requestedScope) {
		save(correlationId, clientId, redirectUri, tpState, requestedScope, null);
	}

	/**
	 * As {@link #save(String, String, String, String, String)}, also naming the pending consent-page
	 * authorization the delegation came from (#299), for {@link AuthorizeContinueController} to remove.
	 */
	public void save(String correlationId, String clientId, String redirectUri, String tpState,
					 String requestedScope, String pendingAuthorizationId) {
		save(correlationId, clientId, redirectUri, tpState, requestedScope, pendingAuthorizationId, null);
	}

	/**
	 * As {@link #save(String, String, String, String, String, String)}, also keeping the Third Party's PKCE
	 * challenge (#274), or null when it sent none.
	 */
	public void save(String correlationId, String clientId, String redirectUri, String tpState,
					 String requestedScope, String pendingAuthorizationId, ProofKey proofKey) {
		save(correlationId, clientId, redirectUri, tpState, requestedScope, pendingAuthorizationId, proofKey, true);
	}

	/**
	 * As {@link #save(String, String, String, String, String, String, ProofKey)}, also recording whether the Third
	 * Party sent {@code redirect_uri} itself (#335), or left it out and {@code redirectUri} is its sole registered one.
	 */
	public void save(String correlationId, String clientId, String redirectUri, String tpState,
					 String requestedScope, String pendingAuthorizationId, ProofKey proofKey, boolean redirectUriSent) {
		// #331: never replace a pending delegation's entry (its client, redirect URI or PKCE challenge).
		Entry previous = states.putIfAbsent(correlationId, new Entry(clientId, redirectUri, tpState, requestedScope,
				pendingAuthorizationId, proofKey, redirectUriSent, Instant.now().plus(TTL)));
		if (previous != null) {
			throw new IllegalStateException("delegation state already exists for correlation_id " + correlationId);
		}
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
	 * @param requestedScope the scope the third party requested; the customer's approved scope
	 *                    must narrow it, never widen it (#211)
	 * @param pendingAuthorizationId the id of the pending authorization Spring AS saved before its
	 *                    consent-page redirect, when the delegation came from there (#299); otherwise null
	 * @param proofKey    the Third Party's PKCE challenge from its original request (#274); null when it sent
	 *                    none
	 * @param redirectUriSent whether the Third Party sent {@code redirect_uri} in that request (#335). When it left
	 *                    it out, {@code redirectUri} is its sole registered one: used for {@code access_denied}, but
	 *                    left out of the replay, so the code is not bound to a {@code redirect_uri} that RFC 6749
	 *                    4.1.3 would then require on the token request
	 * @param expiresAt   when this entry should be considered stale
	 */
	public record Entry(String clientId, String redirectUri, String tpState, String requestedScope,
						String pendingAuthorizationId, ProofKey proofKey, boolean redirectUriSent, Instant expiresAt) {}

	/**
	 * A Third Party's PKCE challenge (RFC 7636) from its original {@code /oauth2/authorize} request, replayed
	 * verbatim after the Data Custodian's consent so the code is minted with it (#274). REQ.21 does not require
	 * PKCE; RFC 9700 2.1.1, which updates RFC 6749, has an AS support it and enforce it when a client sends it.
	 *
	 * @param codeChallenge       the {@code code_challenge}
	 * @param codeChallengeMethod the {@code code_challenge_method}, or null when the request had none (Spring AS
	 *                            validates the pair, on the original request and again on the replay)
	 */
	public record ProofKey(String codeChallenge, String codeChallengeMethod) {

		/** The proof key, or null when there is no {@code code_challenge}. */
		public static ProofKey of(String codeChallenge, String codeChallengeMethod) {
			return codeChallenge == null || codeChallenge.isBlank()
					? null
					: new ProofKey(codeChallenge, codeChallengeMethod);
		}
	}
}
