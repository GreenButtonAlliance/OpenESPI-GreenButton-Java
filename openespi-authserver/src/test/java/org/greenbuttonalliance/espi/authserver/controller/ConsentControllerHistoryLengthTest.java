/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
 *
 *    Portions (c) 2013-2018 EnergyOS.org
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

package org.greenbuttonalliance.espi.authserver.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The consent page reads {@code HistoryLength} as seconds (REQ.21.6.1.2.1) and shows it in months, years or
 * days (#284), by the same rule as openespi-common's {@code HistoryPeriod}.
 */
class ConsentControllerHistoryLengthTest {

	@ParameterizedTest(name = "HistoryLength={0} -> {1}")
	@DisplayName("HistoryLength seconds are shown in months, years or days, or as seconds below a day")
	@CsvSource(delimiter = '|', value = {
			"34128000 | 13 months (395 days)",
			"31536000 | 1 year (365 days)",
			"63072000 | 2 years (730 days)",
			"5184000  | 2 months (60 days)",
			"5097600  | 59 days",
			"604800   | 7 days",
			"86400    | 1 day",
			"13       | 13 seconds",
			"1        | 1 second",
			"abc      | abc seconds"})
	void formatsHistoryLengthInSeconds(String historyLength, String expected) {
		assertThat(ConsentController.formatHistoryLength(historyLength)).isEqualTo(expected);
	}
}
