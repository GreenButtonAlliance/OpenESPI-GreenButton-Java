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
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.common.enums.GrantType;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.datacustodian.config.EspiScopeOpaqueTokenIntrospector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The per-resource GETs (#286) with real data and the real security rules: a customer token reads only its own
 * subscription, only the UsagePoints its commodity Function Blocks cover, and only along a path whose chain holds
 * (UsagePoint in the subscription, MeterReading and summaries under the UsagePoint, IntervalBlock under the
 * MeterReading). The root collections and entries are admin only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Subscription-scoped resource GETs (#286)")
class SubscriptionResourceScopingIntegrationTest {

	private static final String BASE = "http://localhost/espi/1_1/resource";
	private static final long DAY = 86_400L;

	private static final UUID SUBSCRIPTION_A = UUID.fromString("5e1f2a3b-4c5d-5e6f-8a7b-9c0d1e2f3a01");
	private static final UUID SUBSCRIPTION_B = UUID.fromString("5e1f2a3b-4c5d-5e6f-8a7b-9c0d1e2f3a02");
	private static final UUID UNKNOWN_SUBSCRIPTION = UUID.fromString("5e1f2a3b-4c5d-5e6f-8a7b-9c0d1e2f3a09");
	private static final UUID USAGE_POINT_A = UUID.fromString("6f2a3b4c-5d6e-5f70-9b8c-0d1e2f3a4b01");
	private static final UUID GAS_POINT_A = UUID.fromString("6f2a3b4c-5d6e-5f70-9b8c-0d1e2f3a4b03");
	private static final UUID USAGE_POINT_B = UUID.fromString("6f2a3b4c-5d6e-5f70-9b8c-0d1e2f3a4b02");
	private static final UUID METER_READING_A = UUID.fromString("703b4c5d-6e7f-5081-ac9d-1e2f3a4b5c01");
	private static final UUID METER_READING_B = UUID.fromString("703b4c5d-6e7f-5081-ac9d-1e2f3a4b5c02");
	private static final UUID READING_TYPE_A = UUID.fromString("814c5d6e-7f80-5192-bdae-2f3a4b5c6d01");
	private static final UUID READING_TYPE_B = UUID.fromString("814c5d6e-7f80-5192-bdae-2f3a4b5c6d02");
	private static final UUID BLOCK_A = UUID.fromString("925d6e7f-8091-52a3-8ebf-3a4b5c6d7e01");
	private static final UUID BLOCK_B = UUID.fromString("925d6e7f-8091-52a3-8ebf-3a4b5c6d7e02");
	private static final UUID OLD_BLOCK_A = UUID.fromString("925d6e7f-8091-52a3-8ebf-3a4b5c6d7e03");
	private static final UUID OLD_SUMMARY_A = UUID.fromString("a36e7f80-91a2-53b4-9fc0-4b5c6d7e8f03");
	private static final UUID SUMMARY_A =UUID.fromString("a36e7f80-91a2-53b4-9fc0-4b5c6d7e8f01");
	private static final UUID SUMMARY_B = UUID.fromString("a36e7f80-91a2-53b4-9fc0-4b5c6d7e8f02");
	private static final UUID QUALITY_A = UUID.fromString("b47f8091-a2b3-54c5-a0d1-5c6d7e8f9001");
	private static final UUID QUALITY_B = UUID.fromString("b47f8091-a2b3-54c5-a0d1-5c6d7e8f9002");

	private static final String UP_A = "/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + USAGE_POINT_A;
	private static final String MR_A = UP_A + "/MeterReading/" + METER_READING_A;
	private static final String UP_B = "/Subscription/" + SUBSCRIPTION_B + "/UsagePoint/" + USAGE_POINT_B;
	private static final String MR_B = UP_B + "/MeterReading/" + METER_READING_B;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthorizationRepository authorizationRepository;

	@Autowired
	private RetailCustomerRepository retailCustomerRepository;

	@Autowired
	private ApplicationInformationRepository applicationInformationRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private UsagePointRepository usagePointRepository;

	@BeforeEach
	void setUp() {
		long now = Instant.now().getEpochSecond();
		UsagePointEntity usagePointA = usagePoint(USAGE_POINT_A, ServiceCategory.ELECTRICITY,
				METER_READING_A, READING_TYPE_A, BLOCK_A, SUMMARY_A, QUALITY_A, now);
		// Older than a 30-day HistoryLength: a block from 40 days ago, a billing period that ended 60 days ago.
		MeterReadingEntity meterReadingA = usagePointA.getMeterReadings().getFirst();
		meterReadingA.getIntervalBlocks().add(block(OLD_BLOCK_A, meterReadingA, now - 40 * DAY));
		usagePointA.getUsageSummaries().add(usageSummary(OLD_SUMMARY_A, usagePointA, now - 90 * DAY, now));
		usagePointA = usagePointRepository.save(usagePointA);
		UsagePointEntity gasPointA = usagePointRepository.save(bareUsagePoint(GAS_POINT_A, ServiceCategory.GAS));
		UsagePointEntity usagePointB = usagePointRepository.save(usagePoint(USAGE_POINT_B, ServiceCategory.ELECTRICITY,
				METER_READING_B, READING_TYPE_B, BLOCK_B, SUMMARY_B, QUALITY_B, now));

		subscriptionRepository.save(subscription(SUBSCRIPTION_A, List.of(usagePointA, gasPointA)));
		subscriptionRepository.saveAndFlush(subscription(SUBSCRIPTION_B, List.of(usagePointB)));
	}

	@Test
	@DisplayName("a customer token reads its own UsagePoints, MeterReading's IntervalBlocks and summaries")
	void own_subscription_is_readable() throws Exception {
		RequestPostProcessor token = customerA();

		assertThat(read("/Subscription/" + SUBSCRIPTION_A + "/UsagePoint", token))
				.contains(USAGE_POINT_A.toString())
				.doesNotContain(USAGE_POINT_B.toString(), GAS_POINT_A.toString());
		assertThat(read(UP_A, token)).contains("<espi:UsagePoint");
		assertThat(read(MR_A + "/IntervalBlock", token))
				.contains(BLOCK_A.toString())
				.doesNotContain(BLOCK_B.toString());
		assertThat(read(MR_A + "/IntervalBlock/" + BLOCK_A, token)).contains("<espi:IntervalBlock");
		assertThat(read(UP_A + "/UsageSummary", token))
				.contains(SUMMARY_A.toString())
				.doesNotContain(SUMMARY_B.toString());
		assertThat(read(UP_A + "/UsageSummary/" + SUMMARY_A, token)).contains("<espi:UsageSummary");
		assertThat(read(UP_A + "/ElectricPowerQualitySummary", token))
				.contains(QUALITY_A.toString())
				.doesNotContain(QUALITY_B.toString());
		assertThat(read(UP_A + "/ElectricPowerQualitySummary/" + QUALITY_A, token))
				.contains("<espi:ElectricPowerQualitySummary");
	}

	@Test
	@DisplayName("the MeterReading feed and entry Batch/Subscription links to: the token's own only")
	void meter_readings_follow_the_subscription() throws Exception {
		RequestPostProcessor token = customerA();

		assertThat(read(UP_A + "/MeterReading", token))
				.contains(METER_READING_A.toString())
				.doesNotContain(METER_READING_B.toString());
		assertThat(read(MR_A, token)).contains("<espi:MeterReading");
		expect(UP_B + "/MeterReading", token, 403);
		expect(MR_B, token, 403);
		expect(UP_A + "/MeterReading/" + METER_READING_B, token, 404);
		expect("/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + GAS_POINT_A + "/MeterReading", token, 404);
	}

	@Test
	@DisplayName("a token with summary FBs only reads its own subscription's summaries, and nothing else")
	void summary_only_token_reads_only_its_own_summaries() throws Exception {
		RequestPostProcessor token = token(SUBSCRIPTION_A, "FB_16", "FB_17");

		assertThat(read(UP_A + "/UsageSummary", token))
				.contains(SUMMARY_A.toString())
				.doesNotContain(SUMMARY_B.toString());
		assertThat(read(UP_A + "/UsageSummary/" + SUMMARY_A, token)).contains("<espi:UsageSummary");
		assertThat(read(UP_A + "/ElectricPowerQualitySummary", token))
				.contains(QUALITY_A.toString())
				.doesNotContain(QUALITY_B.toString());
		assertThat(read(UP_A + "/ElectricPowerQualitySummary/" + QUALITY_A, token))
				.contains("<espi:ElectricPowerQualitySummary");

		expect(UP_B + "/UsageSummary", token, 403);
		expect(UP_B + "/ElectricPowerQualitySummary/" + QUALITY_B, token, 403);
		expect("/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + USAGE_POINT_B + "/UsageSummary", token, 404);
		expect(UP_A + "/UsageSummary/" + SUMMARY_B, token, 404);
		// No interval metering + commodity: the usage-data URL rule refuses the UsagePoint and its readings.
		expect(UP_A, token, 403);
		expect(MR_A + "/IntervalBlock", token, 403);
	}

	@Test
	@DisplayName("IntervalBlock and summary feeds are in interval-start order, as in Batch/Subscription")
	void feeds_are_chronological() throws Exception {
		String blocks = read(MR_A + "/IntervalBlock", customerA());
		String summaries = read(UP_A + "/UsageSummary", customerA());

		assertThat(blocks.indexOf(OLD_BLOCK_A.toString())).isNotNegative().isLessThan(blocks.indexOf(BLOCK_A.toString()));
		assertThat(summaries.indexOf(OLD_SUMMARY_A.toString())).isNotNegative()
				.isLessThan(summaries.indexOf(SUMMARY_A.toString()));
	}

	@Test
	@DisplayName("HistoryLength limits the IntervalBlocks and summaries: older ones are left out of feeds and 404 as entries")
	void history_length_is_a_ceiling() throws Exception {
		RequestPostProcessor yearToken = customerA();
		RequestPostProcessor monthToken = token(SUBSCRIPTION_A, 30 * DAY, "FB_4", "FB_5", "FB_16", "FB_17");

		assertThat(read(MR_A + "/IntervalBlock", yearToken)).contains(BLOCK_A.toString(), OLD_BLOCK_A.toString());
		assertThat(read(UP_A + "/UsageSummary", yearToken)).contains(SUMMARY_A.toString(), OLD_SUMMARY_A.toString());

		assertThat(read(MR_A + "/IntervalBlock", monthToken))
				.contains(BLOCK_A.toString())
				.doesNotContain(OLD_BLOCK_A.toString());
		assertThat(read(UP_A + "/UsageSummary", monthToken))
				.contains(SUMMARY_A.toString())
				.doesNotContain(OLD_SUMMARY_A.toString());
		expect(MR_A + "/IntervalBlock/" + OLD_BLOCK_A, monthToken, 404);
		expect(UP_A + "/UsageSummary/" + OLD_SUMMARY_A, monthToken, 404);
		assertThat(read(MR_A + "/IntervalBlock/" + OLD_BLOCK_A, admin())).contains("<espi:IntervalBlock");
	}

	@Test
	@DisplayName("another customer's subscription is 403 on every Subscription-scoped path, as is an unknown one")
	void other_subscription_is_forbidden() throws Exception {
		RequestPostProcessor token = customerA();

		for (String path : List.of(
				"/Subscription/" + SUBSCRIPTION_B + "/UsagePoint",
				UP_B,
				MR_B + "/IntervalBlock",
				MR_B + "/IntervalBlock/" + BLOCK_B,
				UP_B + "/UsageSummary",
				UP_B + "/UsageSummary/" + SUMMARY_B,
				UP_B + "/ElectricPowerQualitySummary",
				UP_B + "/ElectricPowerQualitySummary/" + QUALITY_B,
				"/Subscription/" + UNKNOWN_SUBSCRIPTION + "/UsagePoint")) {
			expect(path, token, 403);
		}
	}

	@Test
	@DisplayName("under its own subscription, another customer's ids are 404: the path chain must hold")
	void foreign_ids_under_own_subscription_are_not_found() throws Exception {
		RequestPostProcessor token = customerA();

		for (String path : List.of(
				"/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + USAGE_POINT_B,
				UP_A + "/MeterReading/" + METER_READING_B + "/IntervalBlock",
				MR_A + "/IntervalBlock/" + BLOCK_B,
				UP_A + "/UsageSummary/" + SUMMARY_B,
				UP_A + "/ElectricPowerQualitySummary/" + QUALITY_B,
				"/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + USAGE_POINT_B + "/UsageSummary")) {
			expect(path, token, 404);
		}
	}

	@Test
	@DisplayName("a UsagePoint whose commodity the token's FBs do not cover is 404; FB_10 (gas) makes it readable")
	void usage_points_follow_the_commodity_function_blocks() throws Exception {
		String gasPoint = "/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + GAS_POINT_A;

		expect(gasPoint, customerA(), 404);
		assertThat(read(gasPoint, token(SUBSCRIPTION_A, "FB_4", "FB_10"))).contains("<espi:UsagePoint");
		assertThat(read("/Subscription/" + SUBSCRIPTION_A + "/UsagePoint", token(SUBSCRIPTION_A, "FB_4", "FB_10")))
				.contains(GAS_POINT_A.toString())
				.doesNotContain(USAGE_POINT_A.toString());
	}

	@Test
	@DisplayName("the root collections and entries hold every customer's data: 403 for a customer token, 200 for the admin")
	void root_resources_are_admin_only() throws Exception {
		RequestPostProcessor token = customerA();

		for (String path : List.of(
				"/UsagePoint", "/UsagePoint/" + USAGE_POINT_A,
				"/MeterReading", "/MeterReading/" + METER_READING_A,
				"/IntervalBlock", "/IntervalBlock/" + BLOCK_A,
				"/UsageSummary", "/UsageSummary/" + SUMMARY_A,
				"/ElectricPowerQualitySummary", "/ElectricPowerQualitySummary/" + QUALITY_A,
				"/ReadingType")) {
			expect(path, token, 403);
		}
		assertThat(read("/UsagePoint", admin())).contains(USAGE_POINT_A.toString(), USAGE_POINT_B.toString());
		assertThat(read("/IntervalBlock/" + BLOCK_B, admin())).contains("<espi:IntervalBlock");
	}

	@Test
	@DisplayName("the PII root collections and entries are admin only too")
	void customer_roots_are_admin_only() throws Exception {
		RequestPostProcessor token = token(SUBSCRIPTION_A, "FB_53", "FB_54", "FB_56");
		UUID any = UUID.randomUUID();

		for (String path : List.of("/Customer", "/Customer/" + any, "/CustomerAccount", "/CustomerAccount/" + any)) {
			expect(path, token, 403);
		}
	}

	@Test
	@DisplayName("/ReadingType/{id}, which Batch/Subscription links to, is readable for the token's own ReadingTypes only")
	void reading_type_entry_follows_the_subscription() throws Exception {
		assertThat(read("/ReadingType/" + READING_TYPE_A, customerA())).contains("<espi:ReadingType");
		expect("/ReadingType/" + READING_TYPE_B, customerA(), 404);
		assertThat(read("/ReadingType/" + READING_TYPE_B, admin())).contains("<espi:ReadingType");
	}

	@Test
	@DisplayName("the admin reads any subscription's resources; an unknown subscription is 404")
	void admin_reads_any_subscription() throws Exception {
		assertThat(read(MR_B + "/IntervalBlock", admin())).contains(BLOCK_B.toString());
		assertThat(read(UP_B + "/UsageSummary/" + SUMMARY_B, admin())).contains("<espi:UsageSummary");
		expect("/Subscription/" + UNKNOWN_SUBSCRIPTION + "/UsagePoint", admin(), 404);
	}

	@Test
	@DisplayName("a Third Party client access token never reads a subscription's resources")
	void client_access_token_is_refused() throws Exception {
		RequestPostProcessor clientToken = opaqueToken()
				.attributes(attributes -> {
					attributes.put("resourceURI", BASE + "/Batch/Subscription/" + SUBSCRIPTION_A);
					attributes.put("scope", List.of("FB=34_35"));
				})
				.authorities(authorities(EspiScopeOpaqueTokenIntrospector.CLIENT_ACCESS_TOKEN,
						"FB_4", "FB_5", "FB_15", "FB_17"));

		expect(UP_A, clientToken, 403);
		expect(UP_A + "/UsageSummary", clientToken, 403);
	}

	// --- helpers ------------------------------------------------------------------------------------------

	private String read(String path, RequestPostProcessor token) throws Exception {
		return mockMvc.perform(get(BASE + path).with(token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private void expect(String path, RequestPostProcessor token, int status) throws Exception {
		int actual = mockMvc.perform(get(BASE + path).with(token)).andReturn().getResponse().getStatus();
		assertThat(actual).as("GET %s", path).isEqualTo(status);
	}

	/** Subscription A's customer token: interval metering, electricity, a usage summary and power quality. */
	private static RequestPostProcessor customerA() {
		return token(SUBSCRIPTION_A, "FB_4", "FB_5", "FB_16", "FB_17");
	}

	/** A customer access token for the subscription with these FB authorities and a one-year HistoryLength. */
	private static RequestPostProcessor token(UUID subscriptionId, String... fbs) {
		return token(subscriptionId, 31_536_000L, fbs);
	}

	private static RequestPostProcessor token(UUID subscriptionId, long historyLengthSeconds, String... fbs) {
		return opaqueToken()
				.attributes(attributes -> {
					attributes.put("resourceURI", BASE + "/Batch/Subscription/" + subscriptionId);
					attributes.put("scope", List.of("FB=" + String.join("_",
							Arrays.stream(fbs).map(fb -> fb.substring(3)).toList())
							+ ";HistoryLength=" + historyLengthSeconds));
				})
				.authorities(authorities(fbs));
	}

	private static RequestPostProcessor admin() {
		return opaqueToken().authorities(new SimpleGrantedAuthority("SCOPE_DataCustodian_Admin_Access"));
	}

	private static List<GrantedAuthority> authorities(String... names) {
		return Arrays.stream(names).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
	}

	private SubscriptionEntity subscription(UUID id, List<UsagePointEntity> usagePoints) {
		String key = id.toString().substring(id.toString().length() - 4);
		RetailCustomerEntity customer = new RetailCustomerEntity("scope" + key, "Sample", "Customer",
				"scope" + key + "@example.com", RetailCustomerEntity.ROLE_USER);
		customer.setPassword("not-used");
		customer.setEnabled(Boolean.TRUE);

		ApplicationInformationEntity application = new ApplicationInformationEntity();
		application.setClientId("scope-client-" + key);
		application.setDataCustodianId("data_custodian");
		application.setGrantTypes(new HashSet<>(Set.of(GrantType.AUTHORIZATION_CODE)));

		AuthorizationEntity authorization = new AuthorizationEntity();
		authorization.setAccessToken("token-" + id);
		authorization.setStatus(AuthorizationEntity.STATUS_ACTIVE);

		SubscriptionEntity subscription = new SubscriptionEntity(id);
		subscription.setRetailCustomer(retailCustomerRepository.save(customer));
		subscription.setApplicationInformation(applicationInformationRepository.save(application));
		subscription.setAuthorization(authorizationRepository.save(authorization));
		subscription.setUsagePoints(usagePoints);
		return subscription;
	}

	private static UsagePointEntity bareUsagePoint(UUID id, ServiceCategory serviceCategory) {
		UsagePointEntity usagePoint = new UsagePointEntity();
		usagePoint.setId(id);
		usagePoint.setServiceCategory(serviceCategory);
		return usagePoint;
	}

	/** A UsagePoint with one MeterReading (its ReadingType and one IntervalBlock) and one summary of each kind. */
	private static UsagePointEntity usagePoint(UUID id, ServiceCategory serviceCategory, UUID meterReadingId,
											   UUID readingTypeId, UUID blockId, UUID summaryId, UUID qualityId,
											   long now) {
		UsagePointEntity usagePoint = bareUsagePoint(id, serviceCategory);
		usagePoint.setStatus((short) 1);

		ReadingTypeEntity readingType = new ReadingTypeEntity();
		readingType.setId(readingTypeId);
		readingType.setAccumulationBehaviour("4");
		readingType.setCommodity("1");
		readingType.setFlowDirection("1");
		readingType.setIntervalLength(3_600L);
		readingType.setKind("12");
		readingType.setPowerOfTenMultiplier("-3");
		readingType.setUom("72");

		MeterReadingEntity meterReading = new MeterReadingEntity();
		meterReading.setId(meterReadingId);
		meterReading.setUsagePoint(usagePoint);
		meterReading.setReadingType(readingType);

		meterReading.getIntervalBlocks().add(block(blockId, meterReading, now - DAY));
		usagePoint.getMeterReadings().add(meterReading);

		usagePoint.getUsageSummaries().add(usageSummary(summaryId, usagePoint, now - 30 * DAY, now));

		ElectricPowerQualitySummaryEntity quality = new ElectricPowerQualitySummaryEntity();
		quality.setId(qualityId);
		quality.setUsagePoint(usagePoint);
		quality.setSummaryInterval(new DateTimeInterval(30 * DAY, now - 30 * DAY));
		quality.setMeasurementProtocol((short) 0);
		quality.setMainsVoltage(240_000L);
		quality.setFlickerPlt(0L);
		quality.setFlickerPst(0L);
		quality.setHarmonicVoltage(0L);
		quality.setLongInterruptions(0L);
		quality.setPowerFrequency(0L);
		quality.setRapidVoltageChanges(0L);
		quality.setShortInterruptions(0L);
		quality.setSupplyVoltageDips(0L);
		quality.setSupplyVoltageImbalance(0L);
		quality.setSupplyVoltageVariations(0L);
		quality.setTempOvervoltage(0L);
		usagePoint.getElectricPowerQualitySummaries().add(quality);
		return usagePoint;
	}

	/** A one-day IntervalBlock starting at {@code start}, with one hourly reading. */
	private static IntervalBlockEntity block(UUID id, MeterReadingEntity meterReading, long start) {
		IntervalBlockEntity block = new IntervalBlockEntity();
		block.setId(id);
		block.setMeterReading(meterReading);
		block.setInterval(new DateTimeInterval(DAY, start));
		IntervalReadingEntity reading = new IntervalReadingEntity();
		reading.setIntervalBlock(block);
		reading.setTimePeriod(new DateTimeInterval(3_600L, start));
		reading.setValue(1_250_000L);
		block.getIntervalReadings().add(reading);
		return block;
	}

	/** A 30-day billing period starting at {@code start}. */
	private static UsageSummaryEntity usageSummary(UUID id, UsagePointEntity usagePoint, long start, long now) {
		UsageSummaryEntity summary = new UsageSummaryEntity();
		summary.setId(id);
		summary.setUsagePoint(usagePoint);
		summary.setBillingPeriod(new DateTimeInterval(30 * DAY, start));
		summary.setStatusTimeStamp(now);
		return summary;
	}
}
