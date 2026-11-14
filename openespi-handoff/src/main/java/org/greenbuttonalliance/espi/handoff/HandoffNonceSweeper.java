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

package org.greenbuttonalliance.espi.handoff;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Periodically deletes consumed handoff nonces whose payload expired more than a grace period ago.
 *
 * <p>A row in {@code handoff_nonces} only matters while its handoff could still be presented: once
 * the payload's {@code expiresAt} has passed, {@link SignedHandoffCodec} rejects the token on
 * expiry before the nonce is ever checked. Without this sweep the table grows by one row per
 * customer authorization attempt, forever, on both the Data Custodian and the Authorization
 * Server.</p>
 *
 * <p><b>Why a grace period.</b> Deleting exactly at {@code expiresAt} opens a narrow replay race: a
 * replayed token can pass the codec's expiry check just before {@code expiresAt}, and if a sweep
 * removes the original nonce before the replay's {@code consume()} inserts, the replay would be
 * accepted. Keeping rows for {@code grace-period} (default 5 minutes) past expiry &mdash; far
 * longer than any request takes &mdash; closes that window.</p>
 *
 * <p><b>Multiple instances.</b> The delete is idempotent, so concurrent sweeps on several instances
 * sharing one database are harmless; no distributed lock is needed.</p>
 *
 * <p>Requires {@code @EnableScheduling} in the host application. Configuration
 * ({@code espi.handoff.nonce-sweep.*}):</p>
 * <ul>
 *   <li>{@code enabled} &mdash; default {@code true}</li>
 *   <li>{@code interval} &mdash; delay between sweeps, default {@code 15m}</li>
 *   <li>{@code initial-delay} &mdash; delay before the first sweep, default {@code 1m}</li>
 *   <li>{@code grace-period} &mdash; how long rows are kept past expiry, default {@code 5m}</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "espi.handoff.nonce-sweep", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class HandoffNonceSweeper {

	private final HandoffNonceRepository repository;
	private final Clock clock;
	private final Duration gracePeriod;

	@Autowired
	public HandoffNonceSweeper(HandoffNonceRepository repository,
			@Value("${espi.handoff.nonce-sweep.grace-period:5m}") Duration gracePeriod) {
		this(repository, Clock.systemUTC(), gracePeriod);
	}

	HandoffNonceSweeper(HandoffNonceRepository repository, Clock clock, Duration gracePeriod) {
		this.repository = Objects.requireNonNull(repository, "repository");
		this.clock = Objects.requireNonNull(clock, "clock");
		this.gracePeriod = Objects.requireNonNull(gracePeriod, "gracePeriod");
		if (gracePeriod.isNegative()) {
			throw new IllegalArgumentException("espi.handoff.nonce-sweep.grace-period must not be negative");
		}
	}

	/**
	 * Delete nonces that expired before {@code now - gracePeriod}.
	 *
	 * @return the number of rows deleted
	 */
	@Scheduled(fixedDelayString = "${espi.handoff.nonce-sweep.interval:15m}",
			initialDelayString = "${espi.handoff.nonce-sweep.initial-delay:1m}")
	public int sweep() {
		Instant cutoff = clock.instant().minus(gracePeriod);
		int deleted = repository.deleteByExpiresAtBefore(cutoff);
		if (deleted > 0) {
			log.info("Handoff nonce sweep deleted {} row(s) expired before {}", deleted, cutoff);
		}
		else {
			log.debug("Handoff nonce sweep found no rows expired before {}", cutoff);
		}
		return deleted;
	}
}
