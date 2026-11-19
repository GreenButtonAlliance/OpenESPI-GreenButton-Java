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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.usage.enums.ServiceKind;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import org.greenbuttonalliance.espi.common.service.BaseExportService.FeedEntry;
import org.greenbuttonalliance.espi.common.service.impl.UsageExportService;
import org.greenbuttonalliance.espi.datacustodian.web.api.SubscriptionAccess.Reader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Builds {@code Batch/Subscription/{subscriptionId}} (#279): one Atom feed with the energy usage data of a
 * subscription. The caller has already checked who may read it ({@code ScopeResolver#canReadSubscription}).
 *
 * <p>UsagePoints: those the customer selected whose {@code ServiceCategory} a granted commodity Function Block
 * covers (FB_5 to FB_9 electricity, FB_10 gas, FB_11 water; the URL rule requires one of them with FB_4).
 * Temperature (FB_29) is not yet defined (#283).</p>
 *
 * <p>Entries, in the order of certified feeds, for each of those UsagePoints (UsagePoints and MeterReadings
 * by id, time-bounded resources by interval start):</p>
 * <ol>
 *   <li>the UsagePoint;</li>
 *   <li>each MeterReading, its ReadingType (once in the feed, even if shared) and its IntervalBlocks;</li>
 *   <li>the UsageSummaries and ElectricPowerQualitySummaries, when the token's Function Blocks grant them;</li>
 *   <li>its LocalTimeParameters (once in the feed, even if shared).</li>
 * </ol>
 *
 * <p>Hrefs are ROOT form ({@code UsagePoint/{id}}, {@code MeterReading/{id}}, ...), never through the
 * Subscription, whose id changes with every grant; each entry is the one {@link UsageEntries} builds for the root
 * feeds. Each entry has {@code self} and {@code up}, plus {@code related}
 * links to the resources it refers to that are in the feed.</p>
 *
 * <p>Which IntervalBlocks, UsageSummaries and ElectricPowerQualitySummaries are returned (the "time-bounded"
 * resources, by their interval, billing period or summary interval, whose end is start + duration):</p>
 * <ul>
 *   <li>{@code published-min}/{@code published-max} select those whose interval overlaps the range;</li>
 *   <li>with none of the four range parameters, the default is the last 24 hours, by overlap, plus, for each
 *       UsagePoint, its most
 *       recent UsageSummary and its most recent ElectricPowerQualitySummary (summary intervals rarely line up
 *       with a 24-hour window);</li>
 *   <li>{@code updated-min}/{@code updated-max} select by {@code atom:updated}; they apply to these
 *       time-bounded resources only, since the UsagePoints, MeterReadings, ReadingTypes and LocalTimeParameters
 *       are the parents the selected entries need;</li>
 *   <li>{@code HistoryLength} (REQ.21.6.1.2.1: "The amount of historical information available from the Data
 *       Custodian in seconds") is a ceiling, not a selection: nothing that ended before now minus that many
 *       seconds is returned, whatever the range asks for.</li>
 * </ul>
 * <p>A resource without an interval is kept: there is nothing to measure it against. {@code BlockDuration}
 * describes the blocks and selects nothing.</p>
 */
@Component
public class SubscriptionFeedAssembler {

	/** The default range when the request has neither {@code published-min} nor {@code published-max}. */
	static final long DEFAULT_WINDOW_SECONDS = 24 * 60 * 60L;

	/**
	 * What the token grants, beyond reading the subscription at all: the UsagePoint service kinds its
	 * commodity Function Blocks cover, the summaries, and the HistoryLength ceiling in seconds.
	 *
	 * @param serviceKinds null for every UsagePoint (the admin)
	 */
	public record Grants(Set<ServiceKind> serviceKinds, boolean usageSummaries,
						 boolean powerQualitySummaries, Long historyLengthSeconds) {

		public static Grants all() {
			return new Grants(null, true, true, null);
		}

		boolean covers(UsagePointEntity usagePoint) {
			return serviceKinds == null
					|| ServiceKind.of(usagePoint.getServiceCategory()).map(serviceKinds::contains).orElse(false);
		}

		/**
		 * Whether a time-bounded resource is within HistoryLength: its interval ends after {@code now} minus
		 * HistoryLength seconds. Always true without a HistoryLength, or for a resource with no interval start.
		 * The same rule as this feed's ceiling, for the per-resource GETs (#286).
		 */
		boolean withinHistory(DateTimeInterval interval, Instant now) {
			return historyLengthSeconds == null
					|| Selection.endsAfter(interval, now.getEpochSecond() - historyLengthSeconds);
		}
	}

	/** The request's ESPI query parameters (REQ.21.6.2.8); each may be null. */
	public record Range(Instant publishedMin, Instant publishedMax, Instant updatedMin, Instant updatedMax) {

		public static Range none() {
			return new Range(null, null, null, null);
		}
	}

	private static final Comparator<IdentifiedObject> BY_ID = Comparator.comparing(IdentifiedObject::getId);

	private final SubscriptionRepository subscriptionRepository;
	private final UsageExportService usageExportService;
	private final UsageEntries usageEntries;
	private final Clock clock = Clock.systemUTC();

	public SubscriptionFeedAssembler(SubscriptionRepository subscriptionRepository,
									 UsageExportService usageExportService, UsageEntries usageEntries) {
		this.subscriptionRepository = subscriptionRepository;
		this.usageExportService = usageExportService;
		this.usageEntries = usageEntries;
	}

	/**
	 * The subscription's feed, or empty if there is no such subscription.
	 *
	 * @param resourceBase the DC's resource base, {@code https://{host}/.../espi/1_1/resource}
	 * @param feedHref     the request's absolute URL (the feed's self link)
	 */
	@Transactional(readOnly = true)
	public Optional<AtomFeedDto> build(UUID subscriptionId, String resourceBase, String feedHref, Reader reader,
									   Range range) {
		return subscriptionRepository.findById(subscriptionId)
				.map(subscription -> usageExportService.buildFeedOf(feedHref, "Subscription",
						"Green Button Subscription Feed", entries(subscription, resourceBase, reader,
								Selection.of(range, reader.grants(), clock.instant()))));
	}

	/**
	 * The feed's entries, each the same entry (and links) as in the root feeds ({@link UsageEntries}). The range
	 * selects which time-bounded entries the feed carries; the links name what the token may read.
	 */
	private List<FeedEntry> entries(SubscriptionEntity subscription, String base, Reader reader, Selection selection) {
		Grants grants = reader.grants();
		Set<UUID> readingTypesInFeed = new HashSet<>();
		Set<UUID> localTimesInFeed = new HashSet<>();
		List<FeedEntry> entries = new ArrayList<>();

		List<UsagePointEntity> usagePoints = sorted(subscription.getUsagePoints(), BY_ID).stream()
				.filter(grants::covers)
				.toList();
		for (UsagePointEntity usagePoint : usagePoints) {
			List<UsageSummaryEntity> summaries = !grants.usageSummaries() ? List.of()
					: selection.summaries(usagePoint.getUsageSummaries(), UsageSummaryEntity::getBillingPeriod);
			List<ElectricPowerQualitySummaryEntity> qualitySummaries = !grants.powerQualitySummaries() ? List.of()
					: selection.summaries(usagePoint.getElectricPowerQualitySummaries(),
							ElectricPowerQualitySummaryEntity::getSummaryInterval);
			TimeConfigurationEntity localTime = usagePoint.getLocalTimeParameters();

			entries.add(usageEntries.usagePoint(reader, base, usagePoint));
			for (MeterReadingEntity meterReading : sorted(usagePoint.getMeterReadings(), BY_ID)) {
				entries.add(usageEntries.meterReading(reader, base, meterReading));
				ReadingTypeEntity readingType = meterReading.getReadingType();
				if (readingType != null && readingTypesInFeed.add(readingType.getId())) {
					entries.add(usageEntries.readingType(reader, base, readingType));
				}
				for (IntervalBlockEntity block : selection.blocks(meterReading.getIntervalBlocks())) {
					entries.add(usageEntries.intervalBlock(base, block));
				}
			}
			summaries.forEach(summary -> entries.add(usageEntries.usageSummary(base, summary)));
			qualitySummaries.forEach(summary -> entries.add(usageEntries.qualitySummary(base, summary)));
			if (localTime != null && localTimesInFeed.add(localTime.getId())) {
				// Related to every UsagePoint of the feed that uses it
				entries.add(usageEntries.localTimeParameters(base, localTime, usagePoints.stream()
						.filter(other -> other.getLocalTimeParameters() != null
								&& localTime.getId().equals(other.getLocalTimeParameters().getId()))
						.toList()));
			}
		}
		return entries;
	}

	private static <T> List<T> sorted(List<T> items, Comparator<? super T> order) {
		return items == null ? List.of() : items.stream().sorted(order).toList();
	}

	/**
	 * Which time-bounded resources a request gets, in epoch seconds. {@code from}/{@code to} bound the overlap
	 * (null = open); {@code ceiling} is now minus {@code HistoryLength} seconds (null = no limit).
	 */
	private record Selection(Long from, Long to, Long ceiling, Long updatedMin, Long updatedMax,
							 boolean defaultWindow) {

		static Selection of(Range range, Grants grants, Instant now) {
			long nowSeconds = now.getEpochSecond();
			Long ceiling = grants.historyLengthSeconds() == null ? null : nowSeconds - grants.historyLengthSeconds();
			// The 24-hour default only when the request gives no range at all: an updated-min/max poll for
			// corrections must reach data of any age (still under the HistoryLength ceiling).
			boolean defaultWindow = range.publishedMin() == null && range.publishedMax() == null
					&& range.updatedMin() == null && range.updatedMax() == null;
			Long from = defaultWindow ? Long.valueOf(nowSeconds - DEFAULT_WINDOW_SECONDS) : seconds(range.publishedMin());
			return new Selection(from, defaultWindow ? null : seconds(range.publishedMax()), ceiling,
					seconds(range.updatedMin()), seconds(range.updatedMax()), defaultWindow);
		}

		/** The blocks in the range, by interval start. */
		List<IntervalBlockEntity> blocks(List<IntervalBlockEntity> blocks) {
			return available(blocks, IntervalBlockEntity::getInterval).stream()
					.filter(block -> overlaps(block.getInterval()))
					.toList();
		}

		/** The summaries in the range, by interval start; by default also the most recent one available. */
		<T extends IdentifiedObject> List<T> summaries(List<T> summaries, Function<T, DateTimeInterval> interval) {
			List<T> available = available(summaries, interval);
			List<T> selected = new ArrayList<>(available.stream().filter(s -> overlaps(interval.apply(s))).toList());
			if (defaultWindow && !available.isEmpty() && !selected.contains(available.getLast())) {
				selected.add(available.getLast());
			}
			return selected;
		}

		/** Within HistoryLength and the updated-min/max range, sorted by interval start (no interval first). */
		private <T extends IdentifiedObject> List<T> available(List<T> items, Function<T, DateTimeInterval> interval) {
			Comparator<T> byStart = Comparator.comparing(item -> start(interval.apply(item)),
					Comparator.nullsFirst(Comparator.naturalOrder()));
			return sorted(items, byStart.thenComparing(IdentifiedObject::getId)).stream()
					.filter(item -> endsAfter(interval.apply(item), ceiling) && updatedInRange(item.getUpdated()))
					.toList();
		}

		private boolean overlaps(DateTimeInterval interval) {
			if (start(interval) == null) {
				return true;
			}
			return endsAfter(interval, from) && (to == null || interval.getStart() <= to);
		}

		private boolean updatedInRange(LocalDateTime updated) {
			if (updated == null) {
				return updatedMin == null && updatedMax == null;
			}
			long seconds = updated.toEpochSecond(ZoneOffset.UTC);
			return (updatedMin == null || seconds >= updatedMin) && (updatedMax == null || seconds <= updatedMax);
		}

		private static boolean endsAfter(DateTimeInterval interval, Long limit) {
			if (limit == null || start(interval) == null) {
				return true;
			}
			return interval.getStart() + (interval.getDuration() == null ? 0 : interval.getDuration()) > limit;
		}

		private static Long start(DateTimeInterval interval) {
			return interval == null ? null : interval.getStart();
		}

		private static Long seconds(Instant instant) {
			return instant == null ? null : instant.getEpochSecond();
		}
	}
}
