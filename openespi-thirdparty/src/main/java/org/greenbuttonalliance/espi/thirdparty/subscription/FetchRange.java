package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.scope.EspiScope;

import java.time.Clock;
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
	 * Both dates null: the scope's {@code HistoryLength} up to now (the whole epoch when the scope
	 * has none). A date is a UTC day; {@code to} is inclusive, so the window ends at the midnight after it.
	 *
	 * @throws IllegalArgumentException if {@code from} is after {@code to}
	 */
	public static FetchRange of(LocalDate from, LocalDate to, String scope, Clock clock) {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		if (from != null && to != null && from.isAfter(to)) {
			throw new IllegalArgumentException("The start date must be on or before the end date.");
		}
		Instant min = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : defaultMin(now, scope);
		Instant max = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : now;
		return new FetchRange(min, max);
	}

	private static Instant defaultMin(Instant now, String scope) {
		try {
			Integer history = EspiScope.parse(scope).historyLength();
			return history == null ? Instant.EPOCH : now.minusSeconds(history);
		} catch (IllegalArgumentException e) {
			return Instant.EPOCH;
		}
	}
}
