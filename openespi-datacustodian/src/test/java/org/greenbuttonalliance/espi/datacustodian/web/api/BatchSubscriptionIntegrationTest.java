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
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.xml.EspiXmlSchemaValidator;
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
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /Batch/Subscription/{id}} (#279) with real data, the real export service and the real security
 * rules: exactly the token's subscription, its Function Blocks and HistoryLength, and an XSD-valid feed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Batch/Subscription feed (#279)")
class BatchSubscriptionIntegrationTest {

	private static final String BASE = "http://localhost/espi/1_1/resource";
	private static final long DAY = 86_400L;
	/** Interval metering + interval electricity metering: the URL rule's minimum for Batch. */
	private static final String[] ENERGY_FBS = {"FB_4", "FB_5"};

	private static final UUID SUBSCRIPTION_A = UUID.fromString("3c9d0e6b-6a56-5f11-9a3e-0d1c3b1c2a01");
	private static final UUID SUBSCRIPTION_B = UUID.fromString("3c9d0e6b-6a56-5f11-9a3e-0d1c3b1c2a02");
	private static final UUID EMPTY_SUBSCRIPTION = UUID.fromString("3c9d0e6b-6a56-5f11-9a3e-0d1c3b1c2a03");
	private static final UUID USAGE_POINT_A = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f501");
	private static final UUID USAGE_POINT_B = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f502");
	private static final UUID METER_READING_A = UUID.fromString("41a6bd77-3d03-59c4-9ab2-65f2cb09b101");
	private static final UUID READING_TYPE_A = UUID.fromString("6a4ad1c1-8f0b-5d55-a0a0-2d1c3e4f5a01");
	private static final UUID RECENT_BLOCK = UUID.fromString("7b5be2d2-9a1c-5e66-b1b1-3e2d4f5a6b01");
	private static final UUID OLD_BLOCK = UUID.fromString("7b5be2d2-9a1c-5e66-b1b1-3e2d4f5a6b02");
	private static final UUID USAGE_SUMMARY_A = UUID.fromString("8c6cf3e3-ab2d-5f77-82c2-4f3e5a6b7c01");
	private static final UUID QUALITY_SUMMARY_A = UUID.fromString("9d7d04f4-bc3e-5088-93d3-504f6b7c8d01");
	private static final UUID LOCAL_TIME_A = UUID.fromString("ae8e1505-cd4f-5199-a4e4-61507c8d9e01");

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
		UsagePointEntity usagePointA = usagePointRepository.save(usagePointA(now));
		UsagePointEntity usagePointB = new UsagePointEntity();
		usagePointB.setId(USAGE_POINT_B);
		usagePointB.setDescription("Another customer's meter");
		usagePointB.setServiceCategory(ServiceCategory.ELECTRICITY);
		usagePointB = usagePointRepository.save(usagePointB);

		subscriptionRepository.save(subscription(SUBSCRIPTION_A, List.of(usagePointA)));
		subscriptionRepository.save(subscription(SUBSCRIPTION_B, List.of(usagePointB)));
		subscriptionRepository.saveAndFlush(subscription(EMPTY_SUBSCRIPTION, List.of()));
	}

	@Test
	@DisplayName("a customer token gets exactly its subscription's resources, in certified order, as valid ESPI XML")
	void customer_token_gets_its_subscription() throws Exception {
		String xml = fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5_16_17", "FB_16", "FB_17"));
		String subscription = BASE + "/Subscription/" + SUBSCRIPTION_A;
		String usagePoint = subscription + "/UsagePoint/" + USAGE_POINT_A;
		String meterReading = usagePoint + "/MeterReading/" + METER_READING_A;

		assertThat(xml)
				.contains("rel=\"self\" href=\"" + BASE + "/Batch/Subscription/" + SUBSCRIPTION_A
						+ "\" type=\"espi-feed/Subscription\"")
				.contains("href=\"" + usagePoint + "\" type=\"espi-entry/UsagePoint\"",
						"rel=\"up\" href=\"" + subscription + "/UsagePoint\" type=\"espi-feed/UsagePoint\"",
						"rel=\"related\" href=\"" + usagePoint + "/MeterReading\" type=\"espi-feed/MeterReading\"",
						"rel=\"related\" href=\"" + BASE + "/LocalTimeParameters/" + LOCAL_TIME_A
								+ "\" type=\"espi-entry/LocalTimeParameters\"")
				.contains("href=\"" + meterReading + "\" type=\"espi-entry/MeterReading\"",
						"rel=\"related\" href=\"" + BASE + "/ReadingType/" + READING_TYPE_A
								+ "\" type=\"espi-entry/ReadingType\"",
						"href=\"" + meterReading + "/IntervalBlock/" + RECENT_BLOCK + "\" type=\"espi-entry/IntervalBlock\"",
						"href=\"" + usagePoint + "/UsageSummary/" + USAGE_SUMMARY_A + "\"",
						"href=\"" + usagePoint + "/ElectricPowerQualitySummary/" + QUALITY_SUMMARY_A + "\"",
						"<espi:LocalTimeParameters>")
				.doesNotContain(USAGE_POINT_B.toString(), "<atom:published/>", "xsi:type");
		assertThat(entryResources(xml)).containsExactly("UsagePoint", "MeterReading", "ReadingType",
				"IntervalBlock", "IntervalBlock", "UsageSummary", "ElectricPowerQualitySummary", "LocalTimeParameters");
		assertValid(xml);
	}

	@Test
	@DisplayName("without FB_16 and FB_17 there are no summaries, and no related links to them")
	void summaries_need_their_function_blocks() throws Exception {
		String xml = fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5"));

		assertThat(xml)
				.doesNotContain("<espi:UsageSummary", "<espi:ElectricPowerQualitySummary", "/UsageSummary",
						"/ElectricPowerQualitySummary")
				.contains("<espi:UsagePoint", "<espi:IntervalBlock");
		assertValid(xml);
	}

	@Test
	@DisplayName("HistoryLength (seconds) leaves out IntervalBlocks that ended before now minus that length")
	void history_length_limits_interval_blocks() throws Exception {
		String xml = fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5;HistoryLength=" + (7 * DAY)));

		assertThat(xml).contains(RECENT_BLOCK.toString()).doesNotContain(OLD_BLOCK.toString());
	}

	@Test
	@DisplayName("a token for subscription A gets 403 for B and for an unknown subscription, alike")
	void other_subscriptions_are_forbidden() throws Exception {
		RequestPostProcessor tokenA = customer(SUBSCRIPTION_A, "FB=1_3_4_5");

		mockMvc.perform(get(BASE + "/Batch/Subscription/" + SUBSCRIPTION_B).with(tokenA))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(BASE + "/Batch/Subscription/" + UUID.randomUUID()).with(tokenA))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("a Third Party's client access token is refused, even with energy FBs and a matching resourceURI")
	void client_access_token_is_refused() throws Exception {
		RequestPostProcessor clientToken = opaqueToken()
				.attributes(attributes -> attributes.put("resourceURI", BASE + "/Batch/Subscription/" + SUBSCRIPTION_A))
				.authorities(authorities(EspiScopeOpaqueTokenIntrospector.CLIENT_ACCESS_TOKEN, "FB_4", "FB_5"));

		mockMvc.perform(get(BASE + "/Batch/Subscription/" + SUBSCRIPTION_A).with(clientToken))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("the DataCustodian admin reads any subscription, and gets 404 for an unknown one")
	void admin_reads_any_subscription() throws Exception {
		assertThat(fetch(SUBSCRIPTION_B, admin())).contains(USAGE_POINT_B.toString())
				.doesNotContain(USAGE_POINT_A.toString());
		mockMvc.perform(get(BASE + "/Batch/Subscription/" + UUID.randomUUID()).with(admin()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("a subscription with no UsagePoints is a valid empty feed, typed espi-feed/Subscription")
	void empty_subscription_is_an_empty_feed() throws Exception {
		String xml = fetch(EMPTY_SUBSCRIPTION, customer(EMPTY_SUBSCRIPTION, "FB=1_3_4_5"));

		assertThat(xml)
				.contains("href=\"" + BASE + "/Batch/Subscription/" + EMPTY_SUBSCRIPTION + "\" type=\"espi-feed/Subscription\"")
				.doesNotContain("<atom:entry", "espi-feed/" + EMPTY_SUBSCRIPTION);
		assertValid(xml);
	}

	// --- helpers ------------------------------------------------------------------------------------------

	private String fetch(UUID subscriptionId, RequestPostProcessor token) throws Exception {
		return mockMvc.perform(get(BASE + "/Batch/Subscription/" + subscriptionId).with(token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	/** The entries' resource names, in feed order (from each entry's self link type). */
	private static List<String> entryResources(String xml) {
		return Pattern.compile("rel=\"self\" href=\"[^\"]*\" type=\"espi-entry/(\\w+)\"").matcher(xml)
				.results().map(match -> match.group(1)).toList();
	}

	/**
	 * Valid against atom.xsd + espi.xsd, apart from #278: ServiceCategory is written as the Java constant
	 * name ({@code ELECTRICITY}) instead of espi.xsd's {@code <kind>0</kind>}. Remove the exception with #278.
	 */
	private static void assertValid(String xml) {
		EspiXmlSchemaValidator.Result result = EspiXmlSchemaValidator.instance().validate(xml);
		assertThat(result.issues())
				.as(String.join("\n", EspiXmlSchemaValidator.describe(result)))
				.filteredOn(issue -> issue.severity() != EspiXmlSchemaValidator.Severity.WARNING)
				.allSatisfy(issue -> assertThat(issue.message()).contains("ServiceCategory"));
	}

	/** A customer access token for the subscription, with interval metering + electricity and the extra FBs. */
	private static RequestPostProcessor customer(UUID subscriptionId, String scope, String... extraFbs) {
		String[] fbs = Arrays.copyOf(ENERGY_FBS, ENERGY_FBS.length + extraFbs.length);
		System.arraycopy(extraFbs, 0, fbs, ENERGY_FBS.length, extraFbs.length);
		return opaqueToken()
				.attributes(attributes -> {
					attributes.put("resourceURI", BASE + "/Batch/Subscription/" + subscriptionId);
					attributes.put("scope", List.of(scope));
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
		RetailCustomerEntity customer = new RetailCustomerEntity("batch" + key, "Sample", "Customer",
				"batch" + key + "@example.com", RetailCustomerEntity.ROLE_USER);
		customer.setPassword("not-used");
		customer.setEnabled(Boolean.TRUE);

		ApplicationInformationEntity application = new ApplicationInformationEntity();
		application.setClientId("batch-client-" + key);
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

	/** One UsagePoint with every child kind; one IntervalBlock from yesterday, one from 30 days ago. */
	private static UsagePointEntity usagePointA(long now) {
		UsagePointEntity usagePoint = new UsagePointEntity();
		usagePoint.setId(USAGE_POINT_A);
		usagePoint.setDescription("Sample residential electric service");
		usagePoint.setStatus((short) 1);
		usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);

		TimeConfigurationEntity localTime = new TimeConfigurationEntity();
		localTime.setId(LOCAL_TIME_A);
		localTime.setTzOffset(-18_000L);
		localTime.setDstOffset(3_600L);
		localTime.setDstStartRule(new byte[]{0x36, 0x0E, 0x20, 0x00});
		localTime.setDstEndRule(new byte[]{(byte) 0xB4, 0x0E, 0x20, 0x00});
		usagePoint.setLocalTimeParameters(localTime);

		ReadingTypeEntity readingType = new ReadingTypeEntity();
		readingType.setId(READING_TYPE_A);
		readingType.setAccumulationBehaviour("4");
		readingType.setCommodity("1");
		readingType.setFlowDirection("1");
		readingType.setIntervalLength(3_600L);
		readingType.setKind("12");
		readingType.setPowerOfTenMultiplier("-3");
		readingType.setUom("72");

		MeterReadingEntity meterReading = new MeterReadingEntity();
		meterReading.setId(METER_READING_A);
		meterReading.setUsagePoint(usagePoint);
		meterReading.setReadingType(readingType);
		meterReading.getIntervalBlocks().add(block(RECENT_BLOCK, meterReading, now - DAY));
		meterReading.getIntervalBlocks().add(block(OLD_BLOCK, meterReading, now - 30 * DAY));
		usagePoint.getMeterReadings().add(meterReading);

		UsageSummaryEntity summary = new UsageSummaryEntity();
		summary.setId(USAGE_SUMMARY_A);
		summary.setUsagePoint(usagePoint);
		summary.setBillingPeriod(new DateTimeInterval(30 * DAY, now - 30 * DAY));
		summary.setStatusTimeStamp(now);
		usagePoint.getUsageSummaries().add(summary);

		ElectricPowerQualitySummaryEntity quality = new ElectricPowerQualitySummaryEntity();
		quality.setId(QUALITY_SUMMARY_A);
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
}
