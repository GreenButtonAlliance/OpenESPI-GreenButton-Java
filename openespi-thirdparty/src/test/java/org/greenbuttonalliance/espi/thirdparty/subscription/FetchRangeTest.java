package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FetchRangeTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T12:00:00.789Z"), ZoneOffset.UTC);

	@Test
	void defaultsToHistoryLength() {
		FetchRange range = FetchRange.of(null, null, "FB=4_5_13;IntervalDuration=3600;HistoryLength=34128000", CLOCK);

		assertThat(range).isEqualTo(new FetchRange(Instant.parse("2025-10-21T12:00:00Z"), Instant.parse("2026-11-20T12:00:00Z")));
	}

	@Test
	void defaultsToEpochWithoutHistoryLength() {
		FetchRange range = FetchRange.of(null, null, "FB=4_5_13", CLOCK);

		assertThat(range).isEqualTo(new FetchRange(Instant.EPOCH, Instant.parse("2026-11-20T12:00:00Z")));
	}

	@Test
	void defaultsToEpochWhenScopeDoesNotParse() {
		FetchRange range = FetchRange.of(null, null, "HistoryLength=abc", CLOCK);

		assertThat(range.publishedMin()).isEqualTo(Instant.EPOCH);
	}

	@Test
	void datesAreUtcDaysInclusive() {
		FetchRange range = FetchRange.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "FB=4", CLOCK);

		assertThat(range).isEqualTo(new FetchRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-03T00:00:00Z")));
	}

	@Test
	void rejectsFromAfterTo() {
		assertThatThrownBy(() -> FetchRange.of(LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 2), "FB=4", CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The start date must be on or before the end date.");
	}

	@Test
	void rejectsAFromAfterNow() {
		assertThatThrownBy(() -> FetchRange.of(LocalDate.of(2027, 1, 1), null, "FB=4", CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The start date must be on or before the end date.");
	}

	@Test
	void rejectsAToBeforeTheHistoryLengthDefault() {
		assertThatThrownBy(() -> FetchRange.of(null, LocalDate.of(2020, 1, 1), "FB=4;HistoryLength=34128000", CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The end date is before the history this authorization allows.");
	}

	@ParameterizedTest
	@CsvSource({"1969-12-31, 2026-01-01", "2026-01-01, 9999-12-31", "-0001-01-01, ", ", +10000-01-01"})
	void aDateOutsideWhatEveryDatabaseStoresIsRejectedBeforeTheFetch(LocalDate from, LocalDate to) {
		assertThatThrownBy(() -> FetchRange.of(from, to, "FB=4", CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("A date is outside the range of dates that can be requested.");
	}

	@Test
	void theEarliestAndLatestDatesAreAccepted() {
		FetchRange range = FetchRange.of(LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 30), "FB=4", CLOCK);

		assertThat(range).isEqualTo(new FetchRange(FetchRange.EARLIEST, FetchRange.LATEST));
	}

	@Test
	void aDateBeyondWhatJavaCanRepresentIsRejectedNotAnError() {
		assertThatThrownBy(() -> FetchRange.of(null, LocalDate.of(999999999, 12, 31), "FB=4", CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("A date is outside the range of dates that can be requested.");
	}
}
