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

package org.greenbuttonalliance.espi.handoff;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

/**
 * Single-use nonce semantics for verified {@link SignedHandoff} payloads.
 *
 * <p>{@link #consume} writes a row keyed by the nonce; the PK uniqueness constraint atomically
 * detects a replay and throws {@link InvalidHandoffException}. Each {@code consume} runs in its
 * own transaction so the row commits even if a surrounding business transaction rolls back &mdash;
 * a partially-completed grant must not allow the same nonce to be reused.</p>
 *
 * <p>{@link #generate} produces a fresh 128-bit base64URL nonce for use when issuing an outbound
 * handoff. Issuers do NOT pre-insert a row &mdash; the receiver's {@code consume} is what closes
 * the loop.</p>
 */
@Service
@RequiredArgsConstructor
public class HandoffNonceService {

	private static final int NONCE_BYTES = 16;
	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

	private final HandoffNonceRepository repository;
	private final SecureRandom random = new SecureRandom();
	private final Clock clock = Clock.systemUTC();

	/**
	 * Generate a fresh 128-bit base64URL-encoded nonce. Callers attach this to a new outbound
	 * handoff payload; the receiver atomically detects replay via {@link #consume}.
	 */
	public String generate() {
		byte[] bytes = new byte[NONCE_BYTES];
		random.nextBytes(bytes);
		return URL_ENCODER.encodeToString(bytes);
	}

	/**
	 * Mark a nonce as consumed. Runs in a new transaction so the row commits independently of any
	 * surrounding business transaction &mdash; we must never allow a nonce to be re-consumable
	 * because a downstream step failed.
	 *
	 * @param nonce     the nonce from a verified handoff payload
	 * @param expiresAt the {@code expiresAt} from the same payload, recorded for the sweep job
	 * @throws InvalidHandoffException if this nonce has already been consumed (replay)
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void consume(String nonce, Instant expiresAt) {
		if (nonce == null || nonce.isBlank()) {
			throw new InvalidHandoffException("nonce is empty");
		}
		try {
			repository.save(new HandoffNonceEntity(nonce, expiresAt, clock.instant()));
			repository.flush();
		}
		catch (DataIntegrityViolationException replay) {
			throw new InvalidHandoffException("nonce already consumed (replay)", replay);
		}
	}
}
