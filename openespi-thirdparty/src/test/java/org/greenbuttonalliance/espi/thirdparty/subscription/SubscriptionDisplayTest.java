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

package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;
import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionDisplayTest {

	private final SubscriptionDisplay display = new SubscriptionDisplay();

	/** 2026-01-01T05:00:00Z, the first reading of the real DC sample feed. */
	private static final long START = 1767243600L;

	private static ReadingTypeDto readingType(String multiplier, String uom) {
		ReadingTypeDto readingType = new ReadingTypeDto();
		readingType.setPowerOfTenMultiplier(multiplier);
		readingType.setUom(uom);
		return readingType;
	}

	@Test
	void startTimeInUtcAndInTheUsagePointsStandardOffset() {
		TimeConfigurationDto eastern = new TimeConfigurationDto(-18000L);

		assertThat(new String[] {display.utc(START), display.local(START, eastern), display.local(START, null),
				display.offset(-18000L), display.offset(19800L), display.utc(null)})
				.containsExactly("2026-01-01 05:00 UTC", "2026-01-01 00:00 (UTC-05:00)", "2026-01-01 05:00 UTC",
						"UTC-05:00", "UTC+05:30", "-");
	}

	@Test
	void anOffsetOutsideTheJavaRangeFallsBackToUtcWithANote() {
		TimeConfigurationDto broken = new TimeConfigurationDto(999_999L);

		assertThat(new String[] {display.local(START, broken), display.offset(999_999L),
				display.local(START, new TimeConfigurationDto(Long.MAX_VALUE))})
				.containsExactly("2026-01-01 05:00 UTC (tzOffset 999999 s is not a valid offset)", "999999 s (not a valid offset)",
						"2026-01-01 05:00 UTC (tzOffset " + Long.MAX_VALUE + " s is not a valid offset)");
	}

	@Test
	void anUnrepresentableTimestampIsShownRaw() {
		TimeConfigurationDto eastern = new TimeConfigurationDto(-18000L);

		assertThat(new String[] {display.utc(Long.MAX_VALUE), display.local(Long.MIN_VALUE, eastern),
				display.local(Long.MAX_VALUE, null)})
				.containsExactly(Long.MAX_VALUE + " (not a representable time)",
						Long.MIN_VALUE + " (not a representable time)", Long.MAX_VALUE + " (not a representable time)");
	}

	@ParameterizedTest
	@CsvSource({"3600, 1 h", "86400, 1 day", "2678400, 31 days", "900, 15 min", "7200, 2 h", "45, 45 s"})
	void durationInTheLargestWholeUnit(long seconds, String expected) {
		assertThat(display.duration(seconds)).isEqualTo(expected);
	}

	@Test
	void valueScaledByPowerOfTenWithItsUnit() {
		assertThat(new String[] {display.scaled(420000L, readingType("-3", "72")),
				display.scaled(15L, readingType("3", "72")), display.scaled(7L, readingType(null, "999")),
				display.scaled(420000L, null), display.scaled(5L, readingType("x", "72"))})
				.containsExactly("420 WH", "15000 WH", "7 999", "420000 (no ReadingType)", "5 WH (multiplier x unknown)");
	}

	@ParameterizedTest
	@ValueSource(strings = {"2147483647", "-2147483648", "100000000", "4", "-24"})
	void aMultiplierThatIsNotAUnitMultiplierKindIsShownRawNotApplied(String multiplier) {
		assertThat(display.scaled(5L, readingType(multiplier, "72")))
				.isEqualTo("5 WH (multiplier " + multiplier + " unknown)");
	}

	@Test
	void codesAreShownWithTheirNamesWhereTheEnumIsKnown() {
		assertThat(new String[] {display.unit("72"), display.unit("WH"), display.unit("999"), display.unit(null),
				display.multiplier("-3"), display.multiplier("MILLI"), display.commodity("1"), display.kind("12"),
				display.accumulation("4"), display.flowDirection("1"), display.currency("840"), display.currency("1")})
				.containsExactly("WH (72)", "WH (72)", "999", "-", "MILLI (-3)", "MILLI (-3)", "ELECTRICITY_SECONDARY_METERED (1)",
						"ENERGY (12)", "DELTA_DATA (4)", "FORWARD (1)", "USD (840)", "1");
	}

	@Test
	void moneyIsInHundredThousandthsOfTheCurrency() {
		assertThat(new String[] {display.money(6300L, "840"), display.money(21102000L, "840"),
				display.money(6300L, null), display.money(null, "840")})
				.containsExactly("0.063 USD", "211.02 USD", "0.063", "-");
	}

	@Test
	void summaryMeasurementScaledWithItsOwnUnit() {
		SummaryMeasurementDto consumption = new SummaryMeasurementDto();
		consumption.setPowerOfTenMultiplier("MILLI");
		consumption.setUom("WH");
		consumption.setValue(1325000000L);

		assertThat(new String[] {display.measurement(consumption), display.measurement(null)})
				.containsExactly("1325000 WH", "-");
	}

	@Test
	void historyLengthInCalendarUnits() {
		assertThat(new String[] {display.historyPeriod("FB=4_5_15;HistoryLength=34128000"),
				display.historyPeriod("FB=4_5_15;HistoryLength=604800"), display.historyPeriod("FB=4_5_15"),
				display.historyPeriod("FB=x"), display.historyPeriod(null)})
				.containsExactly("13 months (395 days)", "7 days", "no limit", "-", "-");
	}

	@Test
	void powerQualityListsOnlyTheValuesPresent() {
		ElectricPowerQualitySummaryDto summary = new ElectricPowerQualitySummaryDto();
		summary.setMainsVoltage(240000L);
		summary.setMeasurementProtocol((short) 0);

		assertThat(display.powerQuality(summary))
				.containsExactly(new SubscriptionDisplay.Measured("Mains voltage", "240000"),
						new SubscriptionDisplay.Measured("Measurement protocol", "0"));
	}

	@Test
	void blockWithoutReadingsHasAnEmptyList() {
		assertThat(display.readings(new IntervalBlockDto())).isEmpty();
	}

	@Test
	void titleFallsBackToTheAtomId() {
		assertThat(new String[] {display.title(entry("Sample residential electric service")), display.title(entry(" ")),
				display.title(entry(null))})
				.containsExactly("Sample residential electric service", "urn:uuid:1", "urn:uuid:1");
	}

	private static SubscriptionTree.Entry<Object> entry(String title) {
		return new SubscriptionTree.Entry<>("urn:uuid:1", title, "http://dc/UsagePoint/1", null, null, new Object());
	}

	@Test
	void serviceCategoryAndRulesAsText() {
		assertThat(new String[] {display.serviceCategory(ServiceCategory.ELECTRICITY), display.serviceCategory(null),
				display.hex(new byte[] {0x36, 0x0E, 0x20, 0x00}), display.hex(null)})
				.containsExactly("ELECTRICITY (0)", "-", "360E2000", "-");
	}
}
