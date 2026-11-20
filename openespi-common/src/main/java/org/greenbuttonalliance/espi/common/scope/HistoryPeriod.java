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

/**
 * A scope's {@code HistoryLength}, which is in seconds (REQ.21.6.1.2.1), in the calendar unit a
 * customer understands (#284). It carries numbers only: the screen that shows it supplies the
 * localized words.
 *
 * <ul>
 *   <li>Under one day: {@link Unit#SECONDS}, the exact count (a value this small is almost
 *       certainly a misconfiguration, so it is shown as it is rather than rounded away).</li>
 *   <li>Under 60 days: {@link Unit#DAYS}.</li>
 *   <li>A whole number of 365-day years: {@link Unit#YEARS}.</li>
 *   <li>Otherwise: {@link Unit#MONTHS}, rounded to the nearest average month (365.2425 / 12
 *       days). A month has no fixed length, so {@link #days()} keeps the exact whole-day count
 *       for the screen to show alongside it.</li>
 * </ul>
 *
 * <p>For example, 34128000 s (395 days, REQ.21.4.2.1.2.1's example) is 13 months (395 days);
 * 31536000 s is 1 year (365 days); 604800 s is 7 days.</p>
 *
 * @param unit   the unit {@link #amount()} is in
 * @param amount the length in {@link #unit()}
 * @param days   the length in whole days (rounded down)
 */
public record HistoryPeriod(Unit unit, long amount, long days) {

	/** The unit a {@link HistoryPeriod} is shown in. */
	public enum Unit { SECONDS, DAYS, MONTHS, YEARS }

	private static final long SECONDS_PER_DAY = 86_400;
	private static final long DAYS_PER_YEAR = 365;
	private static final double DAYS_PER_AVERAGE_MONTH = 365.2425 / 12;
	private static final long DAYS_ONLY_BELOW = 60;

	/**
	 * The period of a {@code HistoryLength}.
	 *
	 * @param seconds the {@code HistoryLength} in seconds, positive (as {@link EspiScope#parse} requires)
	 * @return the period in the unit described on this class
	 * @throws IllegalArgumentException if {@code seconds} is zero or negative
	 */
	public static HistoryPeriod ofSeconds(long seconds) {
		if (seconds <= 0) {
			throw new IllegalArgumentException("HistoryLength must be positive: " + seconds);
		}
		long days = seconds / SECONDS_PER_DAY;
		if (days == 0) {
			return new HistoryPeriod(Unit.SECONDS, seconds, 0);
		}
		if (days < DAYS_ONLY_BELOW) {
			return new HistoryPeriod(Unit.DAYS, days, days);
		}
		if (days % DAYS_PER_YEAR == 0) {
			return new HistoryPeriod(Unit.YEARS, days / DAYS_PER_YEAR, days);
		}
		return new HistoryPeriod(Unit.MONTHS, Math.round(days / DAYS_PER_AVERAGE_MONTH), days);
	}
}
