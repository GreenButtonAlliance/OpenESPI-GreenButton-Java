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
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import org.greenbuttonalliance.espi.datacustodian.config.UsageFunctionBlockAuthorities;
import org.greenbuttonalliance.espi.datacustodian.web.api.SubscriptionFeedAssembler.Grants;
import org.greenbuttonalliance.espi.datacustodian.web.api.scope.ScopeResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Row-level access for the Subscription-scoped usage resources (#286): which UsagePoints, MeterReadings,
 * IntervalBlocks and summaries a token may read under {@code /Subscription/{subscriptionId}/UsagePoint/...}, by
 * the same rules as {@code Batch/Subscription} (#279). The URL rules in {@code SecurityConfiguration} decide
 * which Function Blocks a resource type needs; this class decides which rows.
 *
 * <ul>
 *   <li>403 unless {@link ScopeResolver#canReadSubscription}: the DataCustodian admin, or a customer access
 *       token whose {@code resourceURI} names this subscription. A foreign subscription and an unknown one are
 *       answered alike.</li>
 *   <li>404 unless the path chain holds: the UsagePoint is in the subscription and its service category is
 *       covered by one of the token's commodity Function Blocks; the MeterReading or summary belongs to that
 *       UsagePoint; the IntervalBlock to that MeterReading. IntervalBlocks and summaries must also be within the
 *       token's HistoryLength (their interval ends after now minus HistoryLength seconds), as in
 *       {@code Batch/Subscription}.</li>
 *   <li>The root collections and entries ({@code /UsagePoint}, {@code /IntervalBlock/{id}}, ...) hold every
 *       customer's data: DataCustodian admin only ({@link #requireAdmin}), until #187 serves them scoped.</li>
 * </ul>
 *
 * <p>Callers run inside a read-only transaction, so the entities' lazy collections can be read and mapped.</p>
 */
@Component
@Transactional(readOnly = true)
public class SubscriptionAccess {

	private static final Comparator<IdentifiedObject> BY_ID = Comparator.comparing(IdentifiedObject::getId);

	private final ScopeResolver scopeResolver;
	private final SubscriptionRepository subscriptionRepository;
	private final Clock clock = Clock.systemUTC();

	public SubscriptionAccess(ScopeResolver scopeResolver, SubscriptionRepository subscriptionRepository) {
		this.scopeResolver = scopeResolver;
		this.subscriptionRepository = subscriptionRepository;
	}

	/**
	 * What the token grants within its subscription: the admin everything; a customer token the UsagePoints its
	 * commodity Function Blocks cover, the summaries its summary Function Blocks grant, no further back than its
	 * HistoryLength. An FB scope that does not parse is refused (403) rather than read as "no limit".
	 */
	public Grants grants(Authentication authentication) {
		if (scopeResolver.isAdmin(authentication)) {
			return Grants.all();
		}
		Long historyLength;
		try {
			historyLength = scopeResolver.historyLengthSeconds(authentication).orElse(null);
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unreadable ESPI scope");
		}
		return new Grants(
				UsageFunctionBlockAuthorities.serviceKinds(authentication),
				UsageFunctionBlockAuthorities.hasAny(authentication, UsageFunctionBlockAuthorities.USAGE_SUMMARY),
				UsageFunctionBlockAuthorities.hasAny(authentication, UsageFunctionBlockAuthorities.POWER_QUALITY),
				historyLength);
	}

	/** 403 unless the caller is the DataCustodian admin: for the root collections and entries. */
	public void requireAdmin(Authentication authentication) {
		if (!scopeResolver.isAdmin(authentication)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

	/** The subscription's UsagePoints the token may read, by id. */
	public List<UsagePointEntity> usagePoints(Authentication authentication, UUID subscriptionId) {
		Grants grants = grants(authentication);
		return subscription(authentication, subscriptionId).getUsagePoints().stream()
				.filter(grants::covers)
				.sorted(BY_ID)
				.toList();
	}

	/** One of the subscription's UsagePoints the token may read. */
	public UsagePointEntity usagePoint(Authentication authentication, UUID subscriptionId, UUID usagePointId) {
		return usagePoints(authentication, subscriptionId).stream()
				.filter(usagePoint -> usagePoint.getId().equals(usagePointId))
				.findFirst()
				.orElseThrow(SubscriptionAccess::notFound);
	}

	/** One of the UsagePoint's MeterReadings. */
	public MeterReadingEntity meterReading(Authentication authentication, UUID subscriptionId, UUID usagePointId,
										   UUID meterReadingId) {
		return child(usagePoint(authentication, subscriptionId, usagePointId).getMeterReadings(), meterReadingId);
	}

	/** The MeterReading's IntervalBlocks within the token's HistoryLength, by id. */
	public List<IntervalBlockEntity> intervalBlocks(Authentication authentication, UUID subscriptionId,
													UUID usagePointId, UUID meterReadingId) {
		return withinHistory(authentication,
				meterReading(authentication, subscriptionId, usagePointId, meterReadingId).getIntervalBlocks(),
				IntervalBlockEntity::getInterval);
	}

	/** One of the MeterReading's IntervalBlocks, within the token's HistoryLength. */
	public IntervalBlockEntity intervalBlock(Authentication authentication, UUID subscriptionId, UUID usagePointId,
											 UUID meterReadingId, UUID intervalBlockId) {
		return child(intervalBlocks(authentication, subscriptionId, usagePointId, meterReadingId), intervalBlockId);
	}

	/** The UsagePoint's UsageSummaries whose billing period is within the token's HistoryLength, by id. */
	public List<UsageSummaryEntity> usageSummaries(Authentication authentication, UUID subscriptionId,
												   UUID usagePointId) {
		return withinHistory(authentication,
				usagePoint(authentication, subscriptionId, usagePointId).getUsageSummaries(),
				UsageSummaryEntity::getBillingPeriod);
	}

	/** One of the UsagePoint's UsageSummaries, within the token's HistoryLength. */
	public UsageSummaryEntity usageSummary(Authentication authentication, UUID subscriptionId, UUID usagePointId,
										   UUID usageSummaryId) {
		return child(usageSummaries(authentication, subscriptionId, usagePointId), usageSummaryId);
	}

	/** The UsagePoint's ElectricPowerQualitySummaries whose interval is within the token's HistoryLength, by id. */
	public List<ElectricPowerQualitySummaryEntity> qualitySummaries(Authentication authentication,
																	UUID subscriptionId, UUID usagePointId) {
		return withinHistory(authentication,
				usagePoint(authentication, subscriptionId, usagePointId).getElectricPowerQualitySummaries(),
				ElectricPowerQualitySummaryEntity::getSummaryInterval);
	}

	/** One of the UsagePoint's ElectricPowerQualitySummaries, within the token's HistoryLength. */
	public ElectricPowerQualitySummaryEntity qualitySummary(Authentication authentication, UUID subscriptionId,
															UUID usagePointId, UUID qualitySummaryId) {
		return child(qualitySummaries(authentication, subscriptionId, usagePointId), qualitySummaryId);
	}

	/**
	 * Whether the caller may read {@code /ReadingType/{readingTypeId}}, the root URL that {@code Batch/Subscription}
	 * links to: the admin, or a customer token whose subscription has a readable UsagePoint with a MeterReading of
	 * that ReadingType.
	 */
	public boolean canReadReadingType(Authentication authentication, UUID readingTypeId) {
		if (scopeResolver.isAdmin(authentication)) {
			return true;
		}
		return scopeResolver.subscriptionId(authentication)
				.flatMap(subscriptionRepository::findById)
				.map(subscription -> {
					Grants grants = grants(authentication);
					return subscription.getUsagePoints().stream()
							.filter(grants::covers)
							.flatMap(usagePoint -> usagePoint.getMeterReadings().stream())
							.anyMatch(meterReading -> meterReading.getReadingType() != null
									&& readingTypeId.equals(meterReading.getReadingType().getId()));
				})
				.orElse(false);
	}

	/**
	 * One page of a Subscription-scoped feed, read as the root collections' {@code PageRequest.of(offset, limit)}
	 * reads it: {@code offset} is the page number and {@code limit} the page size.
	 */
	public static <T> List<T> page(List<T> items, int offset, int limit) {
		if (offset < 0 || limit < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offset must be 0 or more and limit 1 or more");
		}
		return items.stream().skip((long) offset * limit).limit(limit).toList();
	}

	private SubscriptionEntity subscription(Authentication authentication, UUID subscriptionId) {
		if (!scopeResolver.canReadSubscription(authentication, subscriptionId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		return subscriptionRepository.findById(subscriptionId).orElseThrow(SubscriptionAccess::notFound);
	}

	private static <T extends IdentifiedObject> T child(List<T> children, UUID id) {
		return children.stream()
				.filter(child -> child.getId().equals(id))
				.findFirst()
				.orElseThrow(SubscriptionAccess::notFound);
	}

	/**
	 * The items whose interval ends after now minus the token's HistoryLength ({@link Grants#withinHistory}, the
	 * {@code Batch/Subscription} ceiling), by id. Older data is answered as missing (404 for an entry).
	 */
	private <T extends IdentifiedObject> List<T> withinHistory(Authentication authentication, List<T> items,
															   Function<T, DateTimeInterval> interval) {
		Grants grants = grants(authentication);
		Instant now = clock.instant();
		return items.stream()
				.filter(item -> grants.withinHistory(interval.apply(item), now))
				.sorted(BY_ID)
				.toList();
	}

	private static ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}
}
