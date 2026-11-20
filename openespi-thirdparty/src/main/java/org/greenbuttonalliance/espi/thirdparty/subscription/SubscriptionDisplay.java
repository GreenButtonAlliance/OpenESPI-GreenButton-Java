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
import org.greenbuttonalliance.espi.common.domain.common.enums.Currency;
import org.greenbuttonalliance.espi.common.domain.usage.enums.AccumulationKind;
import org.greenbuttonalliance.espi.common.domain.usage.enums.CommodityKind;
import org.greenbuttonalliance.espi.common.domain.usage.enums.FlowDirectionKind;
import org.greenbuttonalliance.espi.common.domain.usage.enums.MeasurementKind;
import org.greenbuttonalliance.espi.common.domain.usage.enums.UnitMultiplierKind;
import org.greenbuttonalliance.espi.common.domain.usage.enums.UnitSymbolKind;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;
import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto;
import org.greenbuttonalliance.espi.common.dto.usage.IntervalReadingDto;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto;
import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.greenbuttonalliance.espi.common.scope.HistoryPeriod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Turns the values of a Subscription feed into text for the Subscription page (#89): times, durations,
 * scaled readings, money and ESPI code values with their names. Kept out of the template so it can be
 * tested on its own. Every method accepts null and then returns {@code "-"} (or the raw value).
 *
 * <p>Local time uses the UsagePoint's LocalTimeParameters {@code tzOffset} only (standard time). The DST
 * rules are shown as received but not applied: decoding the ESPI DST rule bytes is left to a follow-up.</p>
 */
public class SubscriptionDisplay {

	private static final String NONE = "-";
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	/** ESPI amounts (cost, bills) are in hundred-thousandths of the currency (REQ.21 usage.xsd). */
	private static final int MONEY_SCALE = 5;

	/** {@code 2026-01-01 05:00 UTC}. */
	public String utc(Long epochSeconds) {
		return epochSeconds == null ? NONE : TIME.format(Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC)) + " UTC";
	}

	/** {@code 2026-01-01 00:00 (UTC-05:00)} at the LocalTimeParameters' standard offset; UTC without one. */
	public String local(Long epochSeconds, TimeConfigurationDto localTime) {
		if (epochSeconds == null) {
			return NONE;
		}
		if (localTime == null || localTime.getTzOffset() == null) {
			return utc(epochSeconds);
		}
		ZoneOffset offset = ZoneOffset.ofTotalSeconds(Math.toIntExact(localTime.getTzOffset()));
		return TIME.format(Instant.ofEpochSecond(epochSeconds).atOffset(offset)) + " (" + offset(localTime.getTzOffset()) + ")";
	}

	/** {@code UTC-05:00} for an offset in seconds. */
	public String offset(Long seconds) {
		if (seconds == null) {
			return NONE;
		}
		ZoneOffset offset = ZoneOffset.ofTotalSeconds(Math.toIntExact(seconds));
		return "UTC" + (offset.getTotalSeconds() == 0 ? "+00:00" : offset.getId());
	}

	/** A duration in seconds in its largest whole unit: {@code 1 h}, {@code 31 days}, {@code 15 min}. */
	public String duration(Long seconds) {
		if (seconds == null) {
			return NONE;
		}
		long s = seconds;
		if (s != 0 && s % 86_400 == 0) {
			return plural(s / 86_400, "day");
		}
		if (s != 0 && s % 3_600 == 0) {
			return s / 3_600 + " h";
		}
		if (s != 0 && s % 60 == 0) {
			return s / 60 + " min";
		}
		return s + " s";
	}

	/** A reading's value times 10^powerOfTenMultiplier with the ReadingType's unit: {@code 420 WH}. */
	public String scaled(Long value, ReadingTypeDto readingType) {
		if (value == null) {
			return NONE;
		}
		if (readingType == null) {
			return value + " (no ReadingType)";
		}
		return scaled(value, readingType.getPowerOfTenMultiplier(), readingType.getUom());
	}

	/** A summary measurement with its own multiplier and unit. */
	public String measurement(SummaryMeasurementDto measurement) {
		if (measurement == null || measurement.getValue() == null) {
			return NONE;
		}
		return scaled(measurement.getValue(), measurement.getPowerOfTenMultiplier(), measurement.getUom());
	}

	private static String scaled(long value, String multiplierCode, String uomCode) {
		String unit = uomCode == null ? "" : " " + name(uomCode, UnitSymbolKind::fromValue, UnitSymbolKind.class, UnitSymbolKind::getValue, false);
		Integer power = multiplierCode == null ? Integer.valueOf(0) : power(multiplierCode);
		if (power == null) {
			return value + unit + " (multiplier " + multiplierCode + " unknown)";
		}
		return BigDecimal.valueOf(value).scaleByPowerOfTen(power).stripTrailingZeros().toPlainString() + unit;
	}

	private static Integer power(String code) {
		try {
			return Integer.parseInt(code.trim());
		} catch (NumberFormatException notNumeric) {
			try {
				return UnitMultiplierKind.valueOf(code.trim()).getValue();
			} catch (IllegalArgumentException unknown) {
				return null;
			}
		}
	}

	/** An amount in hundred-thousandths of the currency: {@code 0.063 USD}. */
	public String money(Long amount, String currencyCode) {
		if (amount == null) {
			return NONE;
		}
		String value = BigDecimal.valueOf(amount, MONEY_SCALE).stripTrailingZeros().toPlainString();
		return currencyCode == null ? value
				: value + " " + name(currencyCode, Currency::fromValue, Currency.class, Currency::getValue, false);
	}

	public String unit(String code) {
		return name(code, UnitSymbolKind::fromValue, UnitSymbolKind.class, UnitSymbolKind::getValue, true);
	}

	public String multiplier(String code) {
		return name(code, UnitMultiplierKind::fromValue, UnitMultiplierKind.class, UnitMultiplierKind::getValue, true);
	}

	public String commodity(String code) {
		return name(code, CommodityKind::fromValue, CommodityKind.class, CommodityKind::getValue, true);
	}

	public String kind(String code) {
		return name(code, MeasurementKind::fromValue, MeasurementKind.class, MeasurementKind::getValue, true);
	}

	public String accumulation(String code) {
		return name(code, AccumulationKind::fromValue, AccumulationKind.class, AccumulationKind::getValue, true);
	}

	public String flowDirection(String code) {
		return name(code, FlowDirectionKind::fromValue, FlowDirectionKind.class, FlowDirectionKind::getValue, true);
	}

	public String currency(String code) {
		return name(code, Currency::fromValue, Currency.class, Currency::getValue, true);
	}

	public String serviceCategory(ServiceCategory category) {
		return category == null ? NONE : category.name() + " (" + category.getValue() + ")";
	}

	/** A DST rule as received, in hex. */
	public String hex(byte[] bytes) {
		return bytes == null ? NONE : HexFormat.of().withUpperCase().formatHex(bytes);
	}

	/** The scope's HistoryLength in calendar units: {@code 13 months (395 days)}; {@code no limit} without one. */
	public String historyPeriod(String scope) {
		EspiScope parsed;
		try {
			parsed = EspiScope.parse(scope);
		} catch (IllegalArgumentException notAScope) {
			return NONE;
		}
		if (parsed.historyLength() == null) {
			return "no limit";
		}
		HistoryPeriod period = HistoryPeriod.ofSeconds(parsed.historyLength());
		return switch (period.unit()) {
			case SECONDS -> plural(period.amount(), "second");
			case DAYS -> plural(period.amount(), "day");
			case MONTHS -> plural(period.amount(), "month") + " (" + plural(period.days(), "day") + ")";
			case YEARS -> plural(period.amount(), "year") + " (" + plural(period.days(), "day") + ")";
		};
	}

	/** An IntervalBlock's readings, empty when it has none. */
	public List<IntervalReadingDto> readings(IntervalBlockDto block) {
		return block == null || block.getIntervalReadings() == null ? List.of() : block.getIntervalReadings();
	}

	/** A label and its value as text. */
	public record Measured(String label, String value) {
	}

	/** The measured values an ElectricPowerQualitySummary carries, in schema order; absent ones are left out. */
	public List<Measured> powerQuality(ElectricPowerQualitySummaryDto summary) {
		if (summary == null) {
			return List.of();
		}
		List<Measured> measured = new ArrayList<>();
		add(measured, "Flicker Plt", summary.getFlickerPlt());
		add(measured, "Flicker Pst", summary.getFlickerPst());
		add(measured, "Harmonic voltage", summary.getHarmonicVoltage());
		add(measured, "Long interruptions", summary.getLongInterruptions());
		add(measured, "Mains voltage", summary.getMainsVoltage());
		add(measured, "Measurement protocol", summary.getMeasurementProtocol());
		add(measured, "Power frequency", summary.getPowerFrequency());
		add(measured, "Rapid voltage changes", summary.getRapidVoltageChanges());
		add(measured, "Short interruptions", summary.getShortInterruptions());
		add(measured, "Supply voltage dips", summary.getSupplyVoltageDips());
		add(measured, "Supply voltage imbalance", summary.getSupplyVoltageImbalance());
		add(measured, "Supply voltage variations", summary.getSupplyVoltageVariations());
		add(measured, "Temporary overvoltage", summary.getTempOvervoltage());
		return List.copyOf(measured);
	}

	private static void add(List<Measured> measured, String label, Number value) {
		if (value != null) {
			measured.add(new Measured(label, value.toString()));
		}
	}

	private static String plural(long amount, String unit) {
		return amount + " " + unit + (amount == 1 ? "" : "s");
	}

	/**
	 * A code value (numeric, as the schema has it, or an enum name, as #278 still writes some) with its
	 * name where the openespi-common enum knows it: {@code WH (72)}. An unknown code is returned as is.
	 * With {@code withCode} false only the name is returned.
	 */
	private static <E extends Enum<E>> String name(String code, IntFunction<E> fromValue, Class<E> type,
			ToIntFunction<E> valueOf, boolean withCode) {
		if (code == null || code.isBlank()) {
			return NONE;
		}
		String trimmed = code.trim();
		E constant;
		try {
			constant = fromValue.apply(Integer.parseInt(trimmed));
		} catch (IllegalArgumentException notNumericOrUnknown) {
			try {
				constant = Enum.valueOf(type, trimmed);
			} catch (IllegalArgumentException unknown) {
				return trimmed;
			}
		}
		if (constant == null) {
			return trimmed;
		}
		return withCode ? constant.name() + " (" + valueOf.applyAsInt(constant) + ")" : constant.name();
	}
}
