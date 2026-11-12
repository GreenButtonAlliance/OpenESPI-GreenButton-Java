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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link HandoffNonceSweeper} against H2 + the real Flyway migration.
 *
 * <p>The sweeper runs with a fixed clock in the year 2000, so rows committed by other test classes
 * sharing this cached context (whose expiries are around the real "now") can never fall before the
 * cutoff &mdash; which keeps the deleted-row count exact.</p>
 */
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ContextConfiguration(classes = HandoffNonceServiceTest.HandoffTestApplication.class)
class HandoffNonceSweeperTest {

	private static final Instant NOW = Instant.parse("2000-01-01T12:00:00Z");
	private static final Duration GRACE = Duration.ofMinutes(5);

	@Autowired private HandoffNonceRepository repository;

	@Test
	void sweep_deletesOnlyRowsExpiredBeforeTheGracePeriod() {
		String longExpired = persist(NOW.minus(Duration.ofHours(1)));
		String withinGrace = persist(NOW.minus(Duration.ofMinutes(2)));
		String notExpired = persist(NOW.plus(Duration.ofMinutes(30)));

		int deleted = sweeper(GRACE).sweep();

		assertThat(deleted).isEqualTo(1);
		assertThat(repository.findAllById(List.of(longExpired, withinGrace, notExpired)))
				.extracting(HandoffNonceEntity::getNonce)
				.containsExactlyInAnyOrder(withinGrace, notExpired);
	}

	@Test
	void sweep_withZeroGrace_deletesEverythingAlreadyExpired() {
		String justExpired = persist(NOW.minus(Duration.ofSeconds(1)));
		String notExpired = persist(NOW.plus(Duration.ofSeconds(1)));

		int deleted = sweeper(Duration.ZERO).sweep();

		assertThat(deleted).isEqualTo(1);
		assertThat(repository.findAllById(List.of(justExpired, notExpired)))
				.extracting(HandoffNonceEntity::getNonce)
				.containsExactly(notExpired);
	}

	@Test
	void sweep_withNothingToDelete_returnsZero() {
		String notExpired = persist(NOW.plus(Duration.ofMinutes(5)));

		assertThat(sweeper(GRACE).sweep()).isZero();
		assertThat(repository.findById(notExpired)).isPresent();
	}

	@Test
	void negativeGracePeriod_isRejected() {
		assertThatThrownBy(() -> sweeper(Duration.ofMinutes(-1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("grace-period");
	}

	private HandoffNonceSweeper sweeper(Duration grace) {
		return new HandoffNonceSweeper(repository, Clock.fixed(NOW, ZoneOffset.UTC), grace);
	}

	private String persist(Instant expiresAt) {
		String nonce = UUID.randomUUID().toString();
		repository.saveAndFlush(new HandoffNonceEntity(nonce, expiresAt, expiresAt.minus(Duration.ofMinutes(5))));
		return nonce;
	}
}
