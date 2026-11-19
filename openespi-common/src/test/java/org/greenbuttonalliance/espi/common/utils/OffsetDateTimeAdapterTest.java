/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OffsetDateTimeAdapter: timestamps are written in UTC, ending with Z")
class OffsetDateTimeAdapterTest {

	private final OffsetDateTimeAdapter adapter = new OffsetDateTimeAdapter();

	@Test
	@DisplayName("any offset is written as the same instant in UTC, ending with Z and nothing after it")
	void marshalsUtcWithZ() {
		OffsetDateTime eastern = OffsetDateTime.of(2026, 11, 19, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

		assertThat(adapter.marshal(eastern)).isEqualTo("2026-11-19T15:00:00Z");
		assertThat(adapter.marshal(eastern.withOffsetSameInstant(ZoneOffset.UTC))).isEqualTo("2026-11-19T15:00:00Z");
		assertThat(adapter.marshal(null)).isNull();
	}

	@Test
	@DisplayName("a written timestamp reads back as the same instant")
	void roundTrips() {
		OffsetDateTime value = OffsetDateTime.of(2026, 11, 19, 15, 0, 0, 0, ZoneOffset.UTC);

		assertThat(adapter.unmarshal(adapter.marshal(value))).isEqualTo(value);
	}
}
