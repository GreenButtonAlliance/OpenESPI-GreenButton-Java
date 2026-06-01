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

package org.greenbuttonalliance.espi.common.handoff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;

/**
 * Tracks a single-use nonce from a verified {@link SignedHandoff} on the receiver side. A row in
 * this table means "this nonce has been consumed; further attempts must be rejected as replay."
 *
 * <p>{@link #nonce} is the primary key; an attempt to insert a duplicate fails the transaction and
 * surfaces as a replay rejection. The receiver is expected to delete rows past {@link #expiresAt}
 * via a periodic sweep (out of scope for PR C1).</p>
 */
@Entity
@Table(name = "handoff_nonces")
@Getter
@Setter
@NoArgsConstructor
public class HandoffNonceEntity implements Persistable<String>, Serializable {

	private static final long serialVersionUID = 1L;

	/** The base64URL-encoded nonce from the verified handoff payload. */
	@Id
	@Column(name = "nonce", length = 64, nullable = false, updatable = false)
	private String nonce;

	/**
	 * The {@code expiresAt} from the verified handoff payload. Used by a sweep job (not in scope
	 * here) to reap expired rows. NOT consulted on consume &mdash; the codec already verified
	 * expiry before this row was written.
	 */
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	/** When this nonce was consumed; recorded for audit. */
	@Column(name = "consumed_at", nullable = false, updatable = false)
	private Instant consumedAt;

	public HandoffNonceEntity(String nonce, Instant expiresAt, Instant consumedAt) {
		this.nonce = nonce;
		this.expiresAt = expiresAt;
		this.consumedAt = consumedAt;
	}

	@Override
	public String getId() {
		return nonce;
	}

	/**
	 * Always {@code true} so {@code JpaRepository.save()} routes to
	 * {@code entityManager.persist()} (INSERT) instead of {@code merge()} (UPSERT). A duplicate
	 * nonce must surface as a PK violation, not silently update an existing row.
	 */
	@Override
	@Transient
	public boolean isNew() {
		return true;
	}
}
