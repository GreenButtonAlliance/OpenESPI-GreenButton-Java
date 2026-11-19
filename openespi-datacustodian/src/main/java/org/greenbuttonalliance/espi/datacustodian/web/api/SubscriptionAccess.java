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
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ElectricPowerQualitySummaryRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.ReadingTypeRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Row-level access for the usage resources (#286): which UsagePoints, MeterReadings, IntervalBlocks, summaries and
 * ReadingTypes a token may read. The URL rules in {@code SecurityConfiguration} decide which Function Blocks a
 * resource type needs; a {@link Reader} decides which rows.
 *
 * <p>Resources are addressed only in the ROOT form ({@code UsagePoint/{id}}) or the RetailCustomer XPath form
 * ({@code RetailCustomer/{retailCustomerId}/UsagePoint/{id}/MeterReading}), never through
 * {@code Subscription/{id}/...}. A Subscription is tied to one access token and Authorization, so an href through it
 * would change with every grant, and REQ.21.6.1.1.6 requires repeatable identifiers.</p>
 *
 * <ul>
 *   <li>The DataCustodian admin reads every row.</li>
 *   <li>A customer access token reads the rows of its own subscription (its {@code resourceURI}): the UsagePoints its
 *       commodity Function Blocks cover and their MeterReadings, IntervalBlocks and ReadingTypes; and the summaries of
 *       those UsagePoints, or of any UsagePoint in the subscription for a token with summary Function Blocks only.
 *       IntervalBlocks and summaries must be within the token's HistoryLength (their interval ends after now minus
 *       HistoryLength seconds), the same ceiling as {@code Batch/Subscription}.</li>
 *   <li>Any other token (no subscription, a Third Party's client access token) gets 403.</li>
 * </ul>
 * A root collection holds the reader's rows; a root entry it may not read is 404, whether it exists or not.
 */
@Component
@Transactional(readOnly = true)
public class SubscriptionAccess {

	private static final Comparator<IdentifiedObject> BY_ID = Comparator.comparing(IdentifiedObject::getId);

	private final ScopeResolver scopeResolver;
	private final SubscriptionRepository subscriptionRepository;
	private final UsagePointRepository usagePointRepository;
	private final MeterReadingRepository meterReadingRepository;
	private final IntervalBlockRepository intervalBlockRepository;
	private final UsageSummaryRepository usageSummaryRepository;
	private final ElectricPowerQualitySummaryRepository qualitySummaryRepository;
	private final ReadingTypeRepository readingTypeRepository;
	private final Clock clock = Clock.systemUTC();

	public SubscriptionAccess(ScopeResolver scopeResolver, SubscriptionRepository subscriptionRepository,
							  UsagePointRepository usagePointRepository, MeterReadingRepository meterReadingRepository,
							  IntervalBlockRepository intervalBlockRepository, UsageSummaryRepository usageSummaryRepository,
							  ElectricPowerQualitySummaryRepository qualitySummaryRepository,
							  ReadingTypeRepository readingTypeRepository) {
		this.scopeResolver = scopeResolver;
		this.subscriptionRepository = subscriptionRepository;
		this.usagePointRepository = usagePointRepository;
		this.meterReadingRepository = meterReadingRepository;
		this.intervalBlockRepository = intervalBlockRepository;
		this.usageSummaryRepository = usageSummaryRepository;
		this.qualitySummaryRepository = qualitySummaryRepository;
		this.readingTypeRepository = readingTypeRepository;
	}

	/**
	 * What the token grants: the admin everything; a customer token the UsagePoints its commodity Function Blocks
	 * cover, the summaries its summary Function Blocks grant, no further back than its HistoryLength. An FB scope that
	 * does not parse is refused (403) rather than read as "no limit".
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

	/** 403 unless the caller is the DataCustodian admin: for the customer (PII) root resources (#291). */
	public void requireAdmin(Authentication authentication) {
		if (!scopeResolver.isAdmin(authentication)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * The reader for this request: the admin, or a customer access token's own subscription. 403 for any other
	 * token, and for a token whose subscription the DC does not hold.
	 */
	public Reader reader(Authentication authentication) {
		if (scopeResolver.isAdmin(authentication)) {
			return new Reader(null, Grants.all(), clock.instant());
		}
		SubscriptionEntity subscription = scopeResolver.subscriptionId(authentication)
				.flatMap(subscriptionRepository::findById)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
		return new Reader(subscription, grants(authentication), clock.instant());
	}

	/** The rows one request may read; see {@link SubscriptionAccess}. */
	public final class Reader {

		/** The customer token's subscription; null for the admin. */
		private final SubscriptionEntity subscription;
		private final Grants grants;
		private final Instant now;

		private Reader(SubscriptionEntity subscription, Grants grants, Instant now) {
			this.subscription = subscription;
			this.grants = grants;
			this.now = now;
		}

		/** Whether this is the DataCustodian admin, who reads every row. */
		public boolean admin() {
			return subscription == null;
		}

		public Grants grants() {
			return grants;
		}

		// --- What may be read ---------------------------------------------------------------------------------

		/** A UsagePoint of the token's subscription that its commodity Function Blocks cover (admin: any). */
		public boolean canRead(UsagePointEntity usagePoint) {
			return usagePoint != null && (admin() || (inSubscription(usagePoint) && grants.covers(usagePoint)));
		}

		/**
		 * Whether the UsagePoint's summaries may be read: as {@link #canRead}, except that a token with summary
		 * Function Blocks only (no commodity FB) reads the summaries of any UsagePoint in its subscription.
		 */
		public boolean canReadSummariesOf(UsagePointEntity usagePoint) {
			return usagePoint != null && (admin() || (inSubscription(usagePoint) && (summariesOnly() || grants.covers(usagePoint))));
		}

		/** The UsagePoint's MeterReadings, by id. */
		public List<MeterReadingEntity> meterReadingsOf(UsagePointEntity usagePoint) {
			return sorted(usagePoint.getMeterReadings());
		}

		/** The MeterReading's IntervalBlocks within HistoryLength, by interval start. */
		public List<IntervalBlockEntity> intervalBlocksOf(MeterReadingEntity meterReading) {
			return withinHistory(meterReading.getIntervalBlocks(), IntervalBlockEntity::getInterval);
		}

		/** The UsagePoint's UsageSummaries within HistoryLength, by billing period start. */
		public List<UsageSummaryEntity> usageSummariesOf(UsagePointEntity usagePoint) {
			return withinHistory(usagePoint.getUsageSummaries(), UsageSummaryEntity::getBillingPeriod);
		}

		/** The UsagePoint's ElectricPowerQualitySummaries within HistoryLength, by interval start. */
		public List<ElectricPowerQualitySummaryEntity> qualitySummariesOf(UsagePointEntity usagePoint) {
			return withinHistory(usagePoint.getElectricPowerQualitySummaries(),
					ElectricPowerQualitySummaryEntity::getSummaryInterval);
		}

		/** The readable MeterReadings of this ReadingType, by id. */
		public List<MeterReadingEntity> meterReadingsUsing(ReadingTypeEntity readingType) {
			Stream<MeterReadingEntity> meterReadings = admin()
					? meterReadingRepository.findAllByReadingTypeId(readingType.getId()).stream()
					: meterReadings().stream();
			return meterReadings
					.filter(meterReading -> meterReading.getReadingType() != null
							&& readingType.getId().equals(meterReading.getReadingType().getId()))
					.sorted(BY_ID)
					.toList();
		}

		/**
		 * The RetailCustomer id of a readable UsagePoint, for its RetailCustomer XPath collections: the token's own
		 * customer, or for the admin the UsagePoint's; null if it has none.
		 */
		public Long retailCustomerIdOf(UsagePointEntity usagePoint) {
			if (!admin()) {
				return subscription.getRetailCustomer() == null ? null : subscription.getRetailCustomer().getId();
			}
			return usagePoint.getRetailCustomer() == null ? null : usagePoint.getRetailCustomer().getId();
		}

		// --- A customer token's root collections (the admin's are paged from the repositories) -----------------

		/** The UsagePoints of the token's subscription that its commodity Function Blocks cover, by id. */
		public List<UsagePointEntity> usagePoints() {
			return subscribedUsagePoints().filter(grants::covers).toList();
		}

		/** The MeterReadings of {@link #usagePoints()}, by UsagePoint then id. */
		public List<MeterReadingEntity> meterReadings() {
			return usagePoints().stream().flatMap(usagePoint -> meterReadingsOf(usagePoint).stream()).toList();
		}

		/** The IntervalBlocks of {@link #meterReadings()} within HistoryLength. */
		public List<IntervalBlockEntity> intervalBlocks() {
			return meterReadings().stream().flatMap(meterReading -> intervalBlocksOf(meterReading).stream()).toList();
		}

		/** The UsageSummaries within HistoryLength of the UsagePoints whose summaries may be read. */
		public List<UsageSummaryEntity> usageSummaries() {
			return subscribedUsagePoints().filter(this::canReadSummariesOf)
					.flatMap(usagePoint -> usageSummariesOf(usagePoint).stream()).toList();
		}

		/** The ElectricPowerQualitySummaries within HistoryLength of the UsagePoints whose summaries may be read. */
		public List<ElectricPowerQualitySummaryEntity> qualitySummaries() {
			return subscribedUsagePoints().filter(this::canReadSummariesOf)
					.flatMap(usagePoint -> qualitySummariesOf(usagePoint).stream()).toList();
		}

		/** The ReadingTypes of {@link #meterReadings()}, once each, by id. */
		public List<ReadingTypeEntity> readingTypes() {
			Map<UUID, ReadingTypeEntity> readingTypes = new LinkedHashMap<>();
			meterReadings().stream().map(MeterReadingEntity::getReadingType).filter(Objects::nonNull)
					.forEach(readingType -> readingTypes.putIfAbsent(readingType.getId(), readingType));
			return readingTypes.values().stream().sorted(BY_ID).toList();
		}

		// --- Root entries: 404 unless readable (the admin reads every row, whatever its parents) ----------------

		public UsagePointEntity usagePoint(UUID id) {
			return usagePointRepository.findById(id)
					.filter(usagePoint -> admin() || canRead(usagePoint))
					.orElseThrow(SubscriptionAccess::notFound);
		}

		public MeterReadingEntity meterReading(UUID id) {
			return meterReadingRepository.findById(id)
					.filter(meterReading -> admin() || canRead(meterReading.getUsagePoint()))
					.orElseThrow(SubscriptionAccess::notFound);
		}

		public IntervalBlockEntity intervalBlock(UUID id) {
			return intervalBlockRepository.findById(id)
					.filter(block -> admin() || (block.getMeterReading() != null
							&& canRead(block.getMeterReading().getUsagePoint())
							&& grants.withinHistory(block.getInterval(), now)))
					.orElseThrow(SubscriptionAccess::notFound);
		}

		public UsageSummaryEntity usageSummary(UUID id) {
			return usageSummaryRepository.findById(id)
					.filter(summary -> admin() || (canReadSummariesOf(summary.getUsagePoint())
							&& grants.withinHistory(summary.getBillingPeriod(), now)))
					.orElseThrow(SubscriptionAccess::notFound);
		}

		public ElectricPowerQualitySummaryEntity qualitySummary(UUID id) {
			return qualitySummaryRepository.findById(id)
					.filter(summary -> admin() || (canReadSummariesOf(summary.getUsagePoint())
							&& grants.withinHistory(summary.getSummaryInterval(), now)))
					.orElseThrow(SubscriptionAccess::notFound);
		}

		/** A ReadingType some readable MeterReading uses (admin: any). */
		public ReadingTypeEntity readingType(UUID id) {
			return readingTypeRepository.findById(id)
					.filter(readingType -> admin() || !meterReadingsUsing(readingType).isEmpty())
					.orElseThrow(SubscriptionAccess::notFound);
		}

		// --- RetailCustomer XPath collections -------------------------------------------------------------------

		/**
		 * The UsagePoint of {@code RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/...}: 403 unless the
		 * token's own retail customer (the admin: any), 404 unless the UsagePoint is that customer's and readable
		 * ({@code forSummaries}: its summaries readable).
		 */
		public UsagePointEntity usagePoint(Long retailCustomerId, UUID usagePointId, boolean forSummaries) {
			if (!admin() && (subscription.getRetailCustomer() == null
					|| !subscription.getRetailCustomer().getId().equals(retailCustomerId))) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN);
			}
			return usagePointRepository.findById(usagePointId)
					.filter(usagePoint -> retailCustomerId.equals(retailCustomerIdOf(usagePoint)))
					.filter(forSummaries ? this::canReadSummariesOf : this::canRead)
					.orElseThrow(SubscriptionAccess::notFound);
		}

		/** The MeterReading of {@code RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/MeterReading/{id}/...}. */
		public MeterReadingEntity meterReading(Long retailCustomerId, UUID usagePointId, UUID meterReadingId) {
			return meterReadingsOf(usagePoint(retailCustomerId, usagePointId, false)).stream()
					.filter(meterReading -> meterReading.getId().equals(meterReadingId))
					.findFirst()
					.orElseThrow(SubscriptionAccess::notFound);
		}

		private boolean summariesOnly() {
			return grants.serviceKinds() != null && grants.serviceKinds().isEmpty();
		}

		private boolean inSubscription(UsagePointEntity usagePoint) {
			return subscription.getUsagePoints().stream().anyMatch(member -> member.getId().equals(usagePoint.getId()));
		}

		private Stream<UsagePointEntity> subscribedUsagePoints() {
			if (admin()) {
				throw new IllegalStateException("the admin's root collections are read from the repositories");
			}
			return subscription.getUsagePoints().stream().sorted(BY_ID);
		}

		/**
		 * The items whose interval ends after now minus HistoryLength ({@link Grants#withinHistory}, the
		 * {@code Batch/Subscription} ceiling), sorted by interval start (no interval first), then id.
		 */
		private <T extends IdentifiedObject> List<T> withinHistory(List<T> items, Function<T, DateTimeInterval> interval) {
			Comparator<T> byStart = Comparator.comparing(
					item -> interval.apply(item) == null ? null : interval.apply(item).getStart(),
					Comparator.nullsFirst(Comparator.<Long>naturalOrder()));
			return items.stream()
					.filter(item -> grants.withinHistory(interval.apply(item), now))
					.sorted(byStart.thenComparing(IdentifiedObject::getId))
					.toList();
		}
	}

	private static <T extends IdentifiedObject> List<T> sorted(List<T> items) {
		return items == null ? List.of() : items.stream().sorted(BY_ID).toList();
	}

	private static ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}
}
