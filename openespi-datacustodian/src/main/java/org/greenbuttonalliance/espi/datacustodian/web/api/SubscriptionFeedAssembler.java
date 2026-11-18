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
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ElectricPowerQualitySummaryMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.IntervalBlockMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.MeterReadingMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.ReadingTypeMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.TimeConfigurationMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.UsageSummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import org.greenbuttonalliance.espi.common.service.BaseExportService.FeedEntry;
import org.greenbuttonalliance.espi.common.service.impl.UsageExportService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Builds {@code Batch/Subscription/{subscriptionId}} (#279): one Atom feed with the energy usage data of a
 * subscription. The caller has already checked who may read it ({@code ScopeResolver#canReadSubscription}).
 *
 * <p>Entries, in the order of certified feeds, for each UsagePoint the customer selected:</p>
 * <ol>
 *   <li>the UsagePoint;</li>
 *   <li>each MeterReading, its ReadingType (once in the feed, even if shared) and its IntervalBlocks;</li>
 *   <li>the UsageSummaries and ElectricPowerQualitySummaries, when the token's Function Blocks grant them;</li>
 *   <li>its LocalTimeParameters.</li>
 * </ol>
 *
 * <p>Hrefs are subscription-scoped ({@code .../Subscription/{id}/UsagePoint/{id}/...}); ReadingType and
 * LocalTimeParameters are root resources. Each entry has {@code self} and {@code up}, plus {@code related}
 * links to the resources it refers to that are in the feed.</p>
 *
 * <p>{@code HistoryLength} (REQ.21: "the amount of historical information available from the Data
 * Custodian in seconds") drops IntervalBlocks, UsageSummaries and ElectricPowerQualitySummaries whose interval
 * ended before now minus that length. {@code BlockDuration}
 * describes the blocks and selects nothing.</p>
 */
@Component
public class SubscriptionFeedAssembler {

	/** What the token grants, beyond reading the subscription at all. */
	public record Grants(boolean usageSummaries, boolean powerQualitySummaries, Long historyLengthSeconds) {

		public static Grants all() {
			return new Grants(true, true, null);
		}
	}

	private final SubscriptionRepository subscriptionRepository;
	private final UsageExportService usageExportService;
	private final UsagePointMapper usagePointMapper;
	private final MeterReadingMapper meterReadingMapper;
	private final ReadingTypeMapper readingTypeMapper;
	private final IntervalBlockMapper intervalBlockMapper;
	private final UsageSummaryMapper usageSummaryMapper;
	private final ElectricPowerQualitySummaryMapper powerQualitySummaryMapper;
	private final TimeConfigurationMapper timeConfigurationMapper;
	private Clock clock = Clock.systemUTC();

	public SubscriptionFeedAssembler(SubscriptionRepository subscriptionRepository,
									 UsageExportService usageExportService,
									 UsagePointMapper usagePointMapper,
									 MeterReadingMapper meterReadingMapper,
									 ReadingTypeMapper readingTypeMapper,
									 IntervalBlockMapper intervalBlockMapper,
									 UsageSummaryMapper usageSummaryMapper,
									 ElectricPowerQualitySummaryMapper powerQualitySummaryMapper,
									 TimeConfigurationMapper timeConfigurationMapper) {
		this.subscriptionRepository = subscriptionRepository;
		this.usageExportService = usageExportService;
		this.usagePointMapper = usagePointMapper;
		this.meterReadingMapper = meterReadingMapper;
		this.readingTypeMapper = readingTypeMapper;
		this.intervalBlockMapper = intervalBlockMapper;
		this.usageSummaryMapper = usageSummaryMapper;
		this.powerQualitySummaryMapper = powerQualitySummaryMapper;
		this.timeConfigurationMapper = timeConfigurationMapper;
	}

	/** For tests: the clock {@code HistoryLength} is measured from. */
	void setClock(Clock clock) {
		this.clock = clock;
	}

	/**
	 * The subscription's feed, or empty if there is no such subscription.
	 *
	 * @param resourceBase the DC's resource base, {@code https://{host}/.../espi/1_1/resource}
	 * @param feedHref     the request's absolute URL (the feed's self link)
	 */
	@Transactional(readOnly = true)
	public Optional<AtomFeedDto> build(UUID subscriptionId, String resourceBase, String feedHref, Grants grants) {
		return subscriptionRepository.findById(subscriptionId)
				.map(subscription -> usageExportService.buildFeedOf(feedHref, "Subscription",
						"Green Button Subscription Feed", entries(subscription, resourceBase, grants)));
	}

	private List<FeedEntry> entries(SubscriptionEntity subscription, String resourceBase, Grants grants) {
		String subscriptionHref = resourceBase + "/Subscription/" + subscription.getId();
		Long earliestEnd = grants.historyLengthSeconds() == null ? null
				: clock.instant().getEpochSecond() - grants.historyLengthSeconds();
		Set<UUID> readingTypesInFeed = new HashSet<>();
		List<FeedEntry> entries = new ArrayList<>();

		for (UsagePointEntity usagePoint : subscription.getUsagePoints()) {
			String usagePointFeed = subscriptionHref + "/UsagePoint";
			String usagePointHref = usagePointFeed + "/" + usagePoint.getId();
			List<UsageSummaryEntity> summaries = !grants.usageSummaries() ? List.of()
					: usagePoint.getUsageSummaries().stream()
							.filter(summary -> withinHistory(summary.getBillingPeriod(), earliestEnd))
							.toList();
			List<ElectricPowerQualitySummaryEntity> qualitySummaries = !grants.powerQualitySummaries() ? List.of()
					: usagePoint.getElectricPowerQualitySummaries().stream()
							.filter(summary -> withinHistory(summary.getSummaryInterval(), earliestEnd))
							.toList();
			TimeConfigurationEntity localTime = usagePoint.getLocalTimeParameters();

			List<LinkDto> usagePointRelated = new ArrayList<>();
			if (!usagePoint.getMeterReadings().isEmpty()) {
				usagePointRelated.add(related(usagePointHref + "/MeterReading", "espi-feed/MeterReading"));
			}
			if (!summaries.isEmpty()) {
				usagePointRelated.add(related(usagePointHref + "/UsageSummary", "espi-feed/UsageSummary"));
			}
			if (!qualitySummaries.isEmpty()) {
				usagePointRelated.add(related(usagePointHref + "/ElectricPowerQualitySummary",
						"espi-feed/ElectricPowerQualitySummary"));
			}
			if (localTime != null) {
				usagePointRelated.add(related(resourceBase + "/LocalTimeParameters/" + localTime.getId(),
						"espi-entry/LocalTimeParameters"));
			}
			entries.add(new FeedEntry(usagePoint, usagePointMapper.toDto(usagePoint), usagePointHref,
					usagePointFeed, usagePointRelated));

			for (MeterReadingEntity meterReading : usagePoint.getMeterReadings()) {
				addMeterReading(entries, meterReading, usagePointHref, resourceBase, earliestEnd, readingTypesInFeed);
			}
			for (UsageSummaryEntity summary : summaries) {
				String feed = usagePointHref + "/UsageSummary";
				entries.add(new FeedEntry(summary, usageSummaryMapper.toDto(summary), feed + "/" + summary.getId(),
						feed, List.of()));
			}
			for (ElectricPowerQualitySummaryEntity summary : qualitySummaries) {
				String feed = usagePointHref + "/ElectricPowerQualitySummary";
				entries.add(new FeedEntry(summary, powerQualitySummaryMapper.toDto(summary),
						feed + "/" + summary.getId(), feed, List.of()));
			}
			if (localTime != null) {
				String feed = resourceBase + "/LocalTimeParameters";
				entries.add(new FeedEntry(localTime, timeConfigurationMapper.toDto(localTime),
						feed + "/" + localTime.getId(), feed, List.of()));
			}
		}
		return entries;
	}

	private void addMeterReading(List<FeedEntry> entries, MeterReadingEntity meterReading, String usagePointHref,
								 String resourceBase, Long earliestEnd, Set<UUID> readingTypesInFeed) {
		String meterReadingFeed = usagePointHref + "/MeterReading";
		String meterReadingHref = meterReadingFeed + "/" + meterReading.getId();
		ReadingTypeEntity readingType = meterReading.getReadingType();
		List<IntervalBlockEntity> blocks = meterReading.getIntervalBlocks().stream()
				.filter(block -> withinHistory(block.getInterval(), earliestEnd))
				.toList();

		List<LinkDto> meterReadingRelated = new ArrayList<>();
		if (readingType != null) {
			meterReadingRelated.add(related(resourceBase + "/ReadingType/" + readingType.getId(), "espi-entry/ReadingType"));
		}
		if (!blocks.isEmpty()) {
			meterReadingRelated.add(related(meterReadingHref + "/IntervalBlock", "espi-feed/IntervalBlock"));
		}
		entries.add(new FeedEntry(meterReading, meterReadingMapper.toDto(meterReading), meterReadingHref,
				meterReadingFeed, meterReadingRelated));

		if (readingType != null && readingTypesInFeed.add(readingType.getId())) {
			String feed = resourceBase + "/ReadingType";
			entries.add(new FeedEntry(readingType, readingTypeMapper.toDto(readingType),
					feed + "/" + readingType.getId(), feed, List.of()));
		}
		for (IntervalBlockEntity block : blocks) {
			String feed = meterReadingHref + "/IntervalBlock";
			entries.add(new FeedEntry(block, intervalBlockMapper.toDto(block), feed + "/" + block.getId(), feed,
					List.of()));
		}
	}

	/**
	 * Whether a resource covering {@code interval} (an IntervalBlock's interval, a UsageSummary's billing period
	 * or an ElectricPowerQualitySummary's summary interval) ends within {@code HistoryLength}. One rule for every
	 * time-bounded resource in the feed. A resource without an interval is kept: there is nothing to measure it
	 * against.
	 */
	private static boolean withinHistory(DateTimeInterval interval, Long earliestEnd) {
		if (earliestEnd == null || interval == null || interval.getStart() == null) {
			return true;
		}
		long end = interval.getStart() + (interval.getDuration() == null ? 0 : interval.getDuration());
		return end > earliestEnd;
	}

	private static LinkDto related(String href, String type) {
		return new LinkDto("related", href, type);
	}
}
