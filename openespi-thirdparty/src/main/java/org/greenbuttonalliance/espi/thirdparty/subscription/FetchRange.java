package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.scope.EspiScope;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * The {@code published-min}/{@code published-max} window of a Subscription fetch (#89). Both instants
 * are whole seconds: the Data Custodian accepts only UTC {@code ...Z} timestamps.
 */
public record FetchRange(Instant publishedMin, Instant publishedMax) {

	/**
	 * The earliest {@code published-min}: the default without a HistoryLength. A range is saved with the fetch,
	 * so it must fit every database's timestamp column, and must be refused before the Data Custodian is called.
	 */
	public static final Instant EARLIEST = Instant.EPOCH;
	/** The latest {@code published-max} (MySQL {@code DATETIME} ends with the year 9999). */
	public static final Instant LATEST = Instant.parse("9999-12-31T00:00:00Z");

	/**
	 * Both dates null: the scope's {@code HistoryLength} up to now (the whole epoch when the scope
	 * has none). A date is a UTC day; {@code to} is inclusive, so the window ends at the midnight after it.
	 *
	 * @throws IllegalArgumentException if {@code from} is after {@code to}
	 */
	public static FetchRange of(LocalDate from, LocalDate to, String scope, Clock clock) {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		if (from != null && to != null && from.isAfter(to)) {
			throw new IllegalArgumentException(INVERTED);
		}
		Instant min;
		Instant max;
		try {
			min = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : defaultMin(now, scope);
			max = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : now;
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(OUT_OF_RANGE, e);
		}
		if (min.isBefore(EARLIEST) || max.isAfter(LATEST)) {
			throw new IllegalArgumentException(OUT_OF_RANGE);
		}
		if (min.isAfter(max)) {
			throw new IllegalArgumentException(from == null ? BEFORE_HISTORY : INVERTED);
		}
		return new FetchRange(min, max);
	}

	private static final String INVERTED = "The start date must be on or before the end date.";
	private static final String BEFORE_HISTORY = "The end date is before the history this authorization allows.";
	private static final String OUT_OF_RANGE = "A date is outside the range of dates that can be requested.";

	private static Instant defaultMin(Instant now, String scope) {
		try {
			Integer history = EspiScope.parse(scope).historyLength();
			return history == null ? Instant.EPOCH : now.minusSeconds(history);
		} catch (IllegalArgumentException e) {
			return Instant.EPOCH;
		}
	}
}
