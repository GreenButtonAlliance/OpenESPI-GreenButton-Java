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

import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ElectricPowerQualitySummaryMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.IntervalBlockMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.MeterReadingMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.ReadingTypeMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.TimeConfigurationMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.mapper.usage.UsageSummaryMapper;
import org.greenbuttonalliance.espi.common.service.BaseExportService.FeedEntry;
import org.greenbuttonalliance.espi.datacustodian.web.api.SubscriptionAccess.Reader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The Atom entries of the usage resources (#286, #290), the same in {@code Batch/Subscription}, in the root and
 * RetailCustomer XPath feeds and read on their own. Every link is in the ROOT form ({@code {resource}/{id}}) or the
 * RetailCustomer XPath form, never through {@code Subscription/{id}} (see {@link SubscriptionAccess}).
 *
 * <p>{@code self} is {@code espi-entry/<Resource>} at {@code {resource}/{id}} and {@code up}
 * {@code espi-feed/<Resource>} at {@code {resource}}. The {@code related} links follow the GBA UML model's
 * {@code <<link>>} associations, both ends (DataSDK {@code REQ21_atom-link-matrix.md}):</p>
 * <ul>
 *   <li>UsagePoint: its LocalTimeParameters (required); its MeterReadings, UsageSummaries and
 *       ElectricPowerQualitySummaries (optional, {@code 0..*}): each as an {@code espi-entry} link, plus an
 *       {@code espi-feed} link to the RetailCustomer XPath collection;</li>
 *   <li>MeterReading: its ReadingType and its UsagePoint (required); its IntervalBlocks as for a UsagePoint's
 *       children;</li>
 *   <li>IntervalBlock: its MeterReading (required);</li>
 *   <li>UsageSummary and ElectricPowerQualitySummary: their UsagePoint (required);</li>
 *   <li>ReadingType: the MeterReadings that use it (required);</li>
 *   <li>LocalTimeParameters: the UsagePoints that use it (required).</li>
 * </ul>
 * A reverse link (to a parent) is always an entry. Links to children name only the ones the reader may read.
 */
@Component
public class UsageEntries {

	/** The path that ends a resource base: {@code https://{host}/.../espi/1_1/resource}. */
	private static final String RESOURCE_PATH = "/espi/1_1/resource";

	private final UsagePointMapper usagePointMapper;
	private final MeterReadingMapper meterReadingMapper;
	private final IntervalBlockMapper intervalBlockMapper;
	private final UsageSummaryMapper usageSummaryMapper;
	private final ElectricPowerQualitySummaryMapper qualitySummaryMapper;
	private final ReadingTypeMapper readingTypeMapper;
	private final TimeConfigurationMapper timeConfigurationMapper;

	public UsageEntries(UsagePointMapper usagePointMapper, MeterReadingMapper meterReadingMapper,
						IntervalBlockMapper intervalBlockMapper, UsageSummaryMapper usageSummaryMapper,
						ElectricPowerQualitySummaryMapper qualitySummaryMapper, ReadingTypeMapper readingTypeMapper,
						TimeConfigurationMapper timeConfigurationMapper) {
		this.usagePointMapper = usagePointMapper;
		this.meterReadingMapper = meterReadingMapper;
		this.intervalBlockMapper = intervalBlockMapper;
		this.usageSummaryMapper = usageSummaryMapper;
		this.qualitySummaryMapper = qualitySummaryMapper;
		this.readingTypeMapper = readingTypeMapper;
		this.timeConfigurationMapper = timeConfigurationMapper;
	}

	/** The resource base ({@code https://{host}/.../espi/1_1/resource}) of a request URL under it. */
	public static String resourceBase(String href) {
		int at = href.indexOf(RESOURCE_PATH);
		if (at < 0) {
			throw new IllegalArgumentException("not an ESPI resource URL: " + href);
		}
		return href.substring(0, at + RESOURCE_PATH.length());
	}

	public FeedEntry usagePoint(Reader reader, String base, UsagePointEntity usagePoint) {
		List<LinkDto> related = new ArrayList<>();
		TimeConfigurationEntity localTime = usagePoint.getLocalTimeParameters();
		if (localTime != null) {
			related.add(entry(base, "LocalTimeParameters", localTime));
		}
		String xpath = xpath(reader, base, usagePoint);
		children(related, base, xpath, "MeterReading", reader.meterReadingsOf(usagePoint));
		boolean summaries = reader.canReadSummariesOf(usagePoint);
		if (summaries && reader.grants().usageSummaries()) {
			children(related, base, xpath, "UsageSummary", reader.usageSummariesOf(usagePoint));
		}
		if (summaries && reader.grants().powerQualitySummaries()) {
			children(related, base, xpath, "ElectricPowerQualitySummary", reader.qualitySummariesOf(usagePoint));
		}
		return root(base, "UsagePoint", usagePoint, usagePointMapper.toDto(usagePoint), related);
	}

	public FeedEntry meterReading(Reader reader, String base, MeterReadingEntity meterReading) {
		List<LinkDto> related = new ArrayList<>();
		if (meterReading.getReadingType() != null) {
			related.add(entry(base, "ReadingType", meterReading.getReadingType()));
		}
		UsagePointEntity usagePoint = meterReading.getUsagePoint();
		if (usagePoint != null) {
			related.add(entry(base, "UsagePoint", usagePoint));
			String xpath = xpath(reader, base, usagePoint);
			children(related, base, xpath == null ? null : xpath + "/MeterReading/" + meterReading.getId(),
					"IntervalBlock", reader.intervalBlocksOf(meterReading));
		}
		return root(base, "MeterReading", meterReading, meterReadingMapper.toDto(meterReading), related);
	}

	public FeedEntry intervalBlock(String base, IntervalBlockEntity block) {
		List<LinkDto> related = block.getMeterReading() == null ? List.of()
				: List.of(entry(base, "MeterReading", block.getMeterReading()));
		return root(base, "IntervalBlock", block, intervalBlockMapper.toDto(block), related);
	}

	public FeedEntry usageSummary(String base, UsageSummaryEntity summary) {
		return root(base, "UsageSummary", summary, usageSummaryMapper.toDto(summary), upLink(base, summary.getUsagePoint()));
	}

	public FeedEntry qualitySummary(String base, ElectricPowerQualitySummaryEntity summary) {
		return root(base, "ElectricPowerQualitySummary", summary, qualitySummaryMapper.toDto(summary),
				upLink(base, summary.getUsagePoint()));
	}

	public FeedEntry readingType(Reader reader, String base, ReadingTypeEntity readingType) {
		List<LinkDto> related = reader.meterReadingsUsing(readingType).stream()
				.map(meterReading -> entry(base, "MeterReading", meterReading))
				.toList();
		return root(base, "ReadingType", readingType, readingTypeMapper.toDto(readingType), related);
	}

	/** A LocalTimeParameters, related to the UsagePoints that use it. */
	public FeedEntry localTimeParameters(String base, TimeConfigurationEntity localTime, List<UsagePointEntity> usedBy) {
		List<LinkDto> related = usedBy.stream().map(usagePoint -> entry(base, "UsagePoint", usagePoint)).toList();
		return root(base, "LocalTimeParameters", localTime, timeConfigurationMapper.toDto(localTime), related);
	}

	/** The UsagePoint's RetailCustomer XPath base, {@code RetailCustomer/{id}/UsagePoint/{id}}; null without one. */
	private static String xpath(Reader reader, String base, UsagePointEntity usagePoint) {
		Long retailCustomerId = reader.retailCustomerIdOf(usagePoint);
		return retailCustomerId == null ? null
				: base + "/RetailCustomer/" + retailCustomerId + "/UsagePoint/" + usagePoint.getId();
	}

	/**
	 * Forward links to a parent's readable children ({@code 0..*}): the RetailCustomer XPath collection
	 * ({@code espi-feed}) and each child ({@code espi-entry}); none when it has no readable children.
	 */
	private static void children(List<LinkDto> related, String base, String xpathParent, String resource,
								 List<? extends IdentifiedObject> children) {
		if (children.isEmpty()) {
			return;
		}
		if (xpathParent != null) {
			related.add(new LinkDto("related", xpathParent + "/" + resource, "espi-feed/" + resource));
		}
		children.forEach(child -> related.add(entry(base, resource, child)));
	}

	private static List<LinkDto> upLink(String base, UsagePointEntity usagePoint) {
		return usagePoint == null ? List.of() : List.of(entry(base, "UsagePoint", usagePoint));
	}

	private static LinkDto entry(String base, String resource, IdentifiedObject target) {
		return new LinkDto("related", base + "/" + resource + "/" + target.getId(), "espi-entry/" + resource);
	}

	private static FeedEntry root(String base, String resource, IdentifiedObject entity, Object content,
								  List<LinkDto> related) {
		String collection = base + "/" + resource;
		return new FeedEntry(entity, content, collection + "/" + entity.getId(), collection, related);
	}
}
