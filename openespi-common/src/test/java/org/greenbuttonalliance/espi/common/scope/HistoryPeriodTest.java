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

package org.greenbuttonalliance.espi.common.scope;

import org.greenbuttonalliance.espi.common.scope.HistoryPeriod.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HistoryPeriod: HistoryLength seconds in a customer's calendar unit (#284)")
class HistoryPeriodTest {

	@ParameterizedTest(name = "{0} s -> {2} {1} ({3} days)")
	@CsvSource({
			"34128000, MONTHS, 13, 395",
			"31536000, YEARS,   1, 365",
			"63072000, YEARS,   2, 730",
			"7776000,  MONTHS,  3,  90",
			"5184000,  MONTHS,  2,  60",
			"5097600,  DAYS,   59,  59",
			"2592000,  DAYS,   30,  30",
			"604800,   DAYS,    7,   7",
			"86400,    DAYS,    1,   1",
			"86399,    SECONDS, 86399, 0",
			"13,       SECONDS, 13,  0",
			"1,        SECONDS,  1,  0"})
	void convertsSecondsToCalendarUnit(long seconds, Unit unit, long amount, long days) {
		assertThat(HistoryPeriod.ofSeconds(seconds)).isEqualTo(new HistoryPeriod(unit, amount, days));
	}

	@ParameterizedTest(name = "{0} s is rejected")
	@ValueSource(longs = {0, -1})
	void rejectsZeroOrNegativeSeconds(long seconds) {
		assertThatThrownBy(() -> HistoryPeriod.ofSeconds(seconds))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must be positive: " + seconds);
	}
}
