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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fast binding/validation checks for {@link AuthorizationPurgeProperties} (#192), no Spring context.
 */
@DisplayName("AuthorizationPurgeProperties")
class AuthorizationPurgePropertiesTest {

	private static final String PREFIX = "espi.authserver.authorization-purge";

	@Test
	@DisplayName("binds the documented defaults when nothing is configured")
	void bindsDefaults() {
		assertThat(bind(Map.of()))
				.extracting(AuthorizationPurgeProperties::enabled, AuthorizationPurgeProperties::interval,
						AuthorizationPurgeProperties::initialDelay, AuthorizationPurgeProperties::gracePeriod,
						AuthorizationPurgeProperties::batchSize, AuthorizationPurgeProperties::auditRetention)
				.containsExactly(true, Duration.ofHours(1), Duration.ofMinutes(5), Duration.ofDays(7), 500,
						Duration.ofDays(395));
	}

	@Test
	@DisplayName("binds kebab-case overrides")
	void bindsOverrides() {
		assertThat(bind(Map.of(PREFIX + ".enabled", "false", PREFIX + ".grace-period", "14d",
				PREFIX + ".batch-size", "1", PREFIX + ".audit-retention", "2555d")))
				.extracting(AuthorizationPurgeProperties::enabled, AuthorizationPurgeProperties::gracePeriod,
						AuthorizationPurgeProperties::batchSize, AuthorizationPurgeProperties::auditRetention)
				.containsExactly(false, Duration.ofDays(14), 1, Duration.ofDays(2555));
	}

	@Test
	@DisplayName("rejects a non-positive batch size, negative durations and a zero interval")
	void rejectsInvalidValues() {
		Map.of("batch-size", "0", "grace-period", "-1d", "initial-delay", "-1m", "audit-retention", "-1d",
						"interval", "0s")
				.forEach((name, value) -> assertThatThrownBy(() -> bind(Map.of(PREFIX + "." + name, value)))
						.isInstanceOf(BindException.class)
						.rootCause()
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessageContaining(PREFIX + "." + name));
	}

	private static AuthorizationPurgeProperties bind(Map<String, String> properties) {
		return new Binder(new MapConfigurationPropertySource(properties))
				.bindOrCreate(PREFIX, Bindable.of(AuthorizationPurgeProperties.class));
	}
}
