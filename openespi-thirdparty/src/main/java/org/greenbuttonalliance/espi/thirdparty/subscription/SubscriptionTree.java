package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A {@code Batch/Subscription} feed as a tree: UsagePoints with their MeterReadings, IntervalBlocks, summaries and
 * LocalTimeParameters, linked by the entries' {@code related} links. {@code linkingProblems} names entries that
 * could not be placed.
 */
public record SubscriptionTree(String feedId, String title, OffsetDateTime updated, int entryCount,
		List<UsagePointNode> usagePoints, List<String> linkingProblems) {

	/** One Atom entry: its id, its own href, its dates and the resource it carries. */
	public record Entry<T>(String id, String selfHref, OffsetDateTime published, OffsetDateTime updated, T resource) {
	}

	/** {@code localTimeParameters} may be null. */
	public record UsagePointNode(Entry<UsagePointDto> usagePoint, Entry<TimeConfigurationDto> localTimeParameters,
			List<MeterReadingNode> meterReadings, List<Entry<UsageSummaryDto>> usageSummaries,
			List<Entry<ElectricPowerQualitySummaryDto>> powerQualitySummaries) {
	}

	/** {@code readingType} may be null; {@code intervalBlocks} are in interval start order. */
	public record MeterReadingNode(Entry<MeterReadingDto> meterReading, Entry<ReadingTypeDto> readingType,
			List<Entry<IntervalBlockDto>> intervalBlocks) {
	}
}
