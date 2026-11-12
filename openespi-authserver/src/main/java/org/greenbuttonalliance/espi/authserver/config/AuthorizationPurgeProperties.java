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

package org.greenbuttonalliance.espi.authserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings for the scheduled purge of fully-expired {@code oauth2_authorization} rows (#192).
 *
 * <p>Bound from {@code espi.authserver.authorization-purge.*}. Every value has a default, so the
 * job runs with sensible settings when nothing is configured. Invalid values fail application
 * startup (the compact constructor runs during binding) rather than producing a job that never
 * runs or busy-loops.</p>
 *
 * @param enabled        whether the purge job is registered at all (default {@code true})
 * @param interval       delay between the end of one run and the start of the next (default
 *                       {@code 1h}); must be positive
 * @param initialDelay   delay after startup before the first run (default {@code 5m})
 * @param gracePeriod    how long a row is kept after its <em>latest</em> token expiry, for
 *                       troubleshooting (default {@code 7d})
 * @param batchSize      rows selected, deleted and audited per transaction (default {@code 500});
 *                       must be greater than zero
 * @param auditRetention how long {@code AUTHORIZATION_PURGED} audit rows are kept (default
 *                       {@code 395d}, about 13 months); a utility may extend it to its regulatory
 *                       requirement
 */
@ConfigurationProperties(prefix = "espi.authserver.authorization-purge")
public record AuthorizationPurgeProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("1h") Duration interval,
		@DefaultValue("5m") Duration initialDelay,
		@DefaultValue("7d") Duration gracePeriod,
		@DefaultValue("500") int batchSize,
		@DefaultValue("395d") Duration auditRetention) {

	public AuthorizationPurgeProperties {
		requirePositive("interval", interval);
		requireNonNegative("initial-delay", initialDelay);
		requireNonNegative("grace-period", gracePeriod);
		requireNonNegative("audit-retention", auditRetention);
		if (batchSize <= 0) {
			throw new IllegalArgumentException(
					"espi.authserver.authorization-purge.batch-size must be greater than 0, but was " + batchSize);
		}
	}

	private static void requireNonNegative(String name, Duration value) {
		if (value == null || value.isNegative()) {
			throw new IllegalArgumentException(
					"espi.authserver.authorization-purge." + name + " must be a non-negative duration, but was " + value);
		}
	}

	private static void requirePositive(String name, Duration value) {
		if (value == null || value.isNegative() || value.isZero()) {
			throw new IllegalArgumentException(
					"espi.authserver.authorization-purge." + name + " must be a positive duration, but was " + value);
		}
	}
}
