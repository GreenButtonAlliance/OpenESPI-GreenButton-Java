package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.junit.jupiter.api.Test;

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
}
