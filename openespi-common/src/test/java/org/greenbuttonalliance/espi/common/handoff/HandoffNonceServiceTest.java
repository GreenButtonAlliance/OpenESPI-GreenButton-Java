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

import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link HandoffNonceService} consume-once semantics. Uses {@code @DataJpaTest}
 * + H2 + the Flyway migration to exercise the real PK uniqueness constraint that detects replay.
 */
@Import(HandoffNonceService.class)
class HandoffNonceServiceTest extends BaseRepositoryTest {

	@Autowired private HandoffNonceService service;
	@Autowired private HandoffNonceRepository repository;

	@Test
	void firstConsume_succeeds() {
		String nonce = service.generate();
		service.consume(nonce, futureExpiry());

		assertThat(repository.findById(nonce)).isPresent();
	}

	@Test
	void replayedConsume_isRejected() {
		String nonce = service.generate();
		Instant exp = futureExpiry();

		service.consume(nonce, exp);

		assertThatThrownBy(() -> service.consume(nonce, exp))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("replay");
	}

	@Test
	void differentNonces_canBothBeConsumed() {
		String n1 = service.generate();
		String n2 = service.generate();
		Instant exp = futureExpiry();

		service.consume(n1, exp);
		service.consume(n2, exp);

		// Assert by lookup rather than count — consume() uses REQUIRES_NEW so rows committed by
		// other tests in this class also exist in the DB when this test runs.
		assertThat(repository.findById(n1)).isPresent();
		assertThat(repository.findById(n2)).isPresent();
	}

	@Test
	void generatedNonces_areUniqueOver10kIterations() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 10_000; i++) {
			assertThat(seen.add(service.generate())).as("collision at iteration %d", i).isTrue();
		}
	}

	@Test
	void blankNonce_isRejected() {
		assertThatThrownBy(() -> service.consume(null, futureExpiry()))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("empty");
		assertThatThrownBy(() -> service.consume("", futureExpiry()))
				.isInstanceOf(InvalidHandoffException.class)
				.hasMessageContaining("empty");
	}

	@Test
	void deleteByExpiresAtBefore_reapsOnlyExpiredRows() {
		Instant past = Instant.now().minus(Duration.ofHours(1));
		Instant future = Instant.now().plus(Duration.ofHours(1));

		String pastA = service.generate();
		String pastB = service.generate();
		String futureNonce = service.generate();
		service.consume(pastA, past);
		service.consume(pastB, past);
		service.consume(futureNonce, future);

		repository.deleteByExpiresAtBefore(Instant.now());

		// Assert by id-existence rather than count — REQUIRES_NEW means other tests' rows persist.
		assertThat(repository.findById(pastA)).isEmpty();
		assertThat(repository.findById(pastB)).isEmpty();
		assertThat(repository.findById(futureNonce)).isPresent();
	}

	private static Instant futureExpiry() {
		return Instant.now().plus(Duration.ofMinutes(5));
	}
}
