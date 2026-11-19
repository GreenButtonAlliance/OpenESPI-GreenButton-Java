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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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
	private static final UUID OLD_USAGE_SUMMARY = UUID.fromString("8c6cf3e3-ab2d-5f77-82c2-4f3e5a6b7c02");
	private static final UUID OLD_QUALITY_SUMMARY = UUID.fromString("9d7d04f4-bc3e-5088-93d3-504f6b7c8d02");
	private static final UUID LOCAL_TIME_A = UUID.fromString("ae8e1505-cd4f-5199-a4e4-61507c8d9e01");
	private static final UUID SHARED_SUBSCRIPTION = UUID.fromString("3c9d0e6b-6a56-5f11-9a3e-0d1c3b1c2a04");
	private static final UUID SHARED_POINT_1 = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f503");
	private static final UUID SHARED_POINT_2 = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f504");
	private static final UUID SHARED_LOCAL_TIME = UUID.fromString("ae8e1505-cd4f-5199-a4e4-61507c8d9e02");
	private static final UUID MIXED_SUBSCRIPTION = UUID.fromString("3c9d0e6b-6a56-5f11-9a3e-0d1c3b1c2a05");
	private static final UUID ELECTRIC_POINT = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f505");
	private static final UUID GAS_POINT = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f506");
	private static final UUID WATER_POINT = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f507");
	private static final UUID LATEST_SUBSCRIPTION = UUID.fromString("3c9d0e6b-6a56-5f11-9a3e-0d1c3b1c2a06");
	private static final UUID LATEST_POINT = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f508");
	private static final UUID LATEST_QUALITY = UUID.fromString("9d7d04f4-bc3e-5088-93d3-504f6b7c8d03");

	/** When the fixtures were made, in epoch seconds. */
	private long now;

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
		now = Instant.now().getEpochSecond();
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
	@DisplayName("without query parameters: the last 24 hours, plus the latest summaries, in certified order, as valid ESPI XML")
	void customer_token_gets_its_subscription() throws Exception {
		String xml = fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5_16_17", "FB_16", "FB_17"));
		// ROOT-form hrefs: never through the Subscription, whose id changes with every grant (#290).
		String usagePoint = BASE + "/UsagePoint/" + USAGE_POINT_A;
		String meterReading = BASE + "/MeterReading/" + METER_READING_A;

		assertThat(xml)
				.contains("rel=\"self\" href=\"" + BASE + "/Batch/Subscription/" + SUBSCRIPTION_A
						+ "\" type=\"espi-feed/Subscription\"")
				.contains("rel=\"self\" href=\"" + usagePoint + "\" type=\"espi-entry/UsagePoint\"",
						"rel=\"up\" href=\"" + BASE + "/UsagePoint\" type=\"espi-feed/UsagePoint\"",
						"/UsagePoint/" + USAGE_POINT_A + "/MeterReading\" type=\"espi-feed/MeterReading\"",
						"rel=\"related\" href=\"" + meterReading + "\" type=\"espi-entry/MeterReading\"",
						"rel=\"related\" href=\"" + BASE + "/LocalTimeParameters/" + LOCAL_TIME_A
								+ "\" type=\"espi-entry/LocalTimeParameters\"")
				.contains("rel=\"self\" href=\"" + meterReading + "\" type=\"espi-entry/MeterReading\"",
						"rel=\"related\" href=\"" + BASE + "/ReadingType/" + READING_TYPE_A
								+ "\" type=\"espi-entry/ReadingType\"",
						"rel=\"self\" href=\"" + BASE + "/IntervalBlock/" + RECENT_BLOCK + "\" type=\"espi-entry/IntervalBlock\"",
						"rel=\"self\" href=\"" + BASE + "/UsageSummary/" + USAGE_SUMMARY_A + "\"",
						"rel=\"self\" href=\"" + BASE + "/ElectricPowerQualitySummary/" + QUALITY_SUMMARY_A + "\"",
						"<espi:LocalTimeParameters>")
				// A summary's up is its collection; its required reverse link is to the UsagePoint.
				.contains("rel=\"up\" href=\"" + BASE + "/UsageSummary\" type=\"espi-feed/UsageSummary\"",
						"rel=\"up\" href=\"" + BASE + "/ElectricPowerQualitySummary\""
								+ " type=\"espi-feed/ElectricPowerQualitySummary\"",
						"rel=\"related\" href=\"" + usagePoint + "\" type=\"espi-entry/UsagePoint\"")
				.doesNotContain(USAGE_POINT_B.toString(), "<atom:published/>", "xsi:type", "/Subscription/" + SUBSCRIPTION_A + "/",
						"espi-entry/Subscription")
				// The range leaves these entries out (their ids may still be linked as readable children).
				.doesNotContain("<atom:id>urn:uuid:" + OLD_BLOCK, "<atom:id>urn:uuid:" + OLD_USAGE_SUMMARY,
						"<atom:id>urn:uuid:" + OLD_QUALITY_SUMMARY);
		// The UsageSummary's billing period runs to now (overlap); the power quality summary ended 5 days ago and
		// is there as the UsagePoint's latest.
		assertThat(entryResources(xml)).containsExactly("UsagePoint", "MeterReading", "ReadingType",
				"IntervalBlock", "UsageSummary", "ElectricPowerQualitySummary", "LocalTimeParameters");
		assertValid(xml);
	}

	@Test
	@DisplayName("published-min/max select by interval overlap, with no latest-summary exception")
	void published_range_selects_by_interval() throws Exception {
		String xml = fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5_16_17", "FB_16", "FB_17"),
				"published-min", ago(100 * DAY), "published-max", ago(70 * DAY));

		assertThat(xml)
				.contains("<atom:id>urn:uuid:" + OLD_USAGE_SUMMARY, "<atom:id>urn:uuid:" + OLD_QUALITY_SUMMARY)
				// The range leaves these entries out (their ids may still be linked as readable children).
				.doesNotContain("<atom:id>urn:uuid:" + RECENT_BLOCK, "<atom:id>urn:uuid:" + OLD_BLOCK,
						"<atom:id>urn:uuid:" + USAGE_SUMMARY_A, "<atom:id>urn:uuid:" + QUALITY_SUMMARY_A);
		assertValid(xml);
	}

	@Test
	@DisplayName("a wide range returns every block and summary, sorted by interval start")
	void entries_are_chronological() throws Exception {
		String xml = fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5_16_17", "FB_16", "FB_17"),
				"published-min", ago(100 * DAY));

		// The fixtures add the newer one first.
		assertThat(List.of(
				xml.indexOf(OLD_BLOCK.toString()) < xml.indexOf(RECENT_BLOCK.toString()),
				xml.indexOf(OLD_USAGE_SUMMARY.toString()) < xml.indexOf(USAGE_SUMMARY_A.toString()),
				xml.indexOf(OLD_QUALITY_SUMMARY.toString()) < xml.indexOf(QUALITY_SUMMARY_A.toString())))
				.containsOnly(true);
		assertThat(entryResources(xml)).containsExactly("UsagePoint", "MeterReading", "ReadingType",
				"IntervalBlock", "IntervalBlock", "UsageSummary", "UsageSummary", "ElectricPowerQualitySummary",
				"ElectricPowerQualitySummary", "LocalTimeParameters");
		assertValid(xml);
	}

	@Test
	@DisplayName("updated-min/max select by atom:updated")
	void updated_range_selects_by_atom_updated() throws Exception {
		String xml = fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5_16_17", "FB_16", "FB_17"),
				"published-min", ago(100 * DAY), "updated-max", "2000-01-01T00:00:00Z");

		assertThat(entryResources(xml)).containsExactly("UsagePoint", "MeterReading", "ReadingType",
				"LocalTimeParameters");
	}

	@Test
	@DisplayName("updated-min alone drops the 24-hour default, and the query is part of the feed's self link")
	void updated_min_alone_reaches_older_data() throws Exception {
		// In the URL, as a client sends it (MockMvc's param() would not set the query string).
		String xml = mockMvc.perform(get(BASE + "/Batch/Subscription/" + SUBSCRIPTION_A
								+ "?updated-min=2000-01-01T00:00:00Z")
						.with(customer(SUBSCRIPTION_A, "FB=1_3_4_5_16_17", "FB_16", "FB_17")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(xml)
				.contains(OLD_BLOCK.toString(), RECENT_BLOCK.toString(), OLD_USAGE_SUMMARY.toString(),
						OLD_QUALITY_SUMMARY.toString())
				.contains("href=\"" + BASE + "/Batch/Subscription/" + SUBSCRIPTION_A + "?updated-min=");
		assertValid(xml);
	}

	@Test
	@DisplayName("a query timestamp that is not UTC ending in Z is a 400")
	void malformed_query_parameter_is_bad_request() throws Exception {
		// A non-epoch timestamp must be UTC and end with Z, with nothing after it.
		for (String value : List.of("yesterday", "2026-11-01T00:00:00", "2026-11-01T00:00:00+00:00",
				"2026-11-01T00:00:00-05:00", "2026-11-01T00:00:00Z ", "2026-11-01", "1761955200")) {
			mockMvc.perform(get(BASE + "/Batch/Subscription/" + SUBSCRIPTION_A)
							.param("published-min", value)
							.with(customer(SUBSCRIPTION_A, "FB=1_3_4_5")))
					.andExpect(status().isBadRequest());
		}
		fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5"), "published-min", "2026-11-01T00:00:00Z");
		fetch(SUBSCRIPTION_A, customer(SUBSCRIPTION_A, "FB=1_3_4_5"), "published-min", "2026-11-01T00:00:00.5Z");
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
	@DisplayName("HistoryLength (seconds) is a ceiling: nothing that ended before now minus it, whatever the range asks for")
	void history_length_is_a_ceiling() throws Exception {
		String thirtyDays = fetch(SUBSCRIPTION_A,
				customer(SUBSCRIPTION_A, "FB=1_3_4_5_16_17;HistoryLength=" + (30 * DAY), "FB_16", "FB_17"),
				"published-min", ago(100 * DAY));
		String oneYear = fetch(SUBSCRIPTION_A,
				customer(SUBSCRIPTION_A, "FB=1_3_4_5_16_17;HistoryLength=31536000", "FB_16", "FB_17"),
				"published-min", ago(100 * DAY));

		// The old block ended 39 days ago, the old summaries 60 days ago.
		assertThat(thirtyDays)
				.contains(RECENT_BLOCK.toString(), USAGE_SUMMARY_A.toString(), QUALITY_SUMMARY_A.toString())
				.doesNotContain(OLD_BLOCK.toString(), OLD_USAGE_SUMMARY.toString(), OLD_QUALITY_SUMMARY.toString());
		assertThat(oneYear)
				.contains(OLD_BLOCK.toString(), OLD_USAGE_SUMMARY.toString(), OLD_QUALITY_SUMMARY.toString());
		assertValid(thirtyDays);
	}

	@Test
	@DisplayName("by default the latest summary is added, but never past the HistoryLength ceiling")
	void latest_summary_respects_the_ceiling() throws Exception {
		UsagePointEntity usagePoint = usagePoint(LATEST_POINT, ServiceCategory.ELECTRICITY);
		usagePoint.getElectricPowerQualitySummaries().add(qualitySummary(LATEST_QUALITY, usagePoint, now - 90 * DAY));
		subscriptionRepository.saveAndFlush(
				subscription(LATEST_SUBSCRIPTION, List.of(usagePointRepository.save(usagePoint))));

		// Its only power quality summary ended 60 days ago.
		String thirtyDays = fetch(LATEST_SUBSCRIPTION,
				customer(LATEST_SUBSCRIPTION, "FB=1_3_4_5_17;HistoryLength=" + (30 * DAY), "FB_17"));
		String oneYear = fetch(LATEST_SUBSCRIPTION,
				customer(LATEST_SUBSCRIPTION, "FB=1_3_4_5_17;HistoryLength=31536000", "FB_17"));

		assertThat(thirtyDays).contains(LATEST_POINT.toString()).doesNotContain(LATEST_QUALITY.toString());
		assertThat(oneYear).contains(LATEST_QUALITY.toString());
	}

	@Test
	@DisplayName("a -min after its -max is a 400")
	void reversed_range_is_bad_request() throws Exception {
		for (String[] range : List.of(new String[]{"published-min", ago(DAY), "published-max", ago(2 * DAY)},
				new String[]{"updated-min", ago(DAY), "updated-max", ago(2 * DAY)})) {
			mockMvc.perform(get(BASE + "/Batch/Subscription/" + SUBSCRIPTION_A)
							.param(range[0], range[1]).param(range[2], range[3])
							.with(customer(SUBSCRIPTION_A, "FB=1_3_4_5")))
					.andExpect(status().isBadRequest());
		}
	}

	@Test
	@DisplayName("an FB scope that does not parse is refused, not read as 'no HistoryLength'")
	void unreadable_scope_is_refused() throws Exception {
		mockMvc.perform(get(BASE + "/Batch/Subscription/" + SUBSCRIPTION_A)
						.with(customer(SUBSCRIPTION_A, "FB=4_x;HistoryLength=13")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("UsagePoints by commodity FB: FB_5 electricity, FB_10 gas, FB_11 water; FB_4 needs one of FB_5-FB_11")
	void usage_points_need_their_commodity_function_block() throws Exception {
		List<UsagePointEntity> usagePoints = new ArrayList<>();
		for (UsagePointEntity usagePoint : List.of(
				usagePoint(ELECTRIC_POINT, ServiceCategory.ELECTRICITY),
				usagePoint(GAS_POINT, ServiceCategory.GAS),
				usagePoint(WATER_POINT, ServiceCategory.WATER))) {
			usagePoints.add(usagePointRepository.save(usagePoint));
		}
		subscriptionRepository.saveAndFlush(subscription(MIXED_SUBSCRIPTION, usagePoints));

		String electricity = fetch(MIXED_SUBSCRIPTION, token(MIXED_SUBSCRIPTION, "FB=1_3_4_5", "FB_4", "FB_5"));
		String gasAndWater = fetch(MIXED_SUBSCRIPTION,
				token(MIXED_SUBSCRIPTION, "FB=1_3_4_10_11", "FB_4", "FB_10", "FB_11"));

		assertThat(electricity).contains(ELECTRIC_POINT.toString())
				.doesNotContain(GAS_POINT.toString(), WATER_POINT.toString());
		assertThat(gasAndWater).contains(GAS_POINT.toString(), WATER_POINT.toString())
				.doesNotContain(ELECTRIC_POINT.toString());
		assertValid(gasAndWater);
		// FB_4 alone, FB_4 with only temperature (FB_29, #283), and a commodity FB without FB_4 are refused.
		for (RequestPostProcessor refused : List.of(
				token(MIXED_SUBSCRIPTION, "FB=1_3_4", "FB_4"),
				token(MIXED_SUBSCRIPTION, "FB=1_3_4_29", "FB_4", "FB_29"),
				token(MIXED_SUBSCRIPTION, "FB=1_3_5", "FB_5"))) {
			mockMvc.perform(get(BASE + "/Batch/Subscription/" + MIXED_SUBSCRIPTION).with(refused))
					.andExpect(status().isForbidden());
		}
		assertThat(fetch(MIXED_SUBSCRIPTION, admin()))
				.contains(ELECTRIC_POINT.toString(), GAS_POINT.toString(), WATER_POINT.toString());
	}

	@Test
	@DisplayName("a LocalTimeParameters shared by two UsagePoints is one entry, linked from both and related to both")
	void shared_local_time_parameters_is_one_entry() throws Exception {
		TimeConfigurationEntity shared = localTime(SHARED_LOCAL_TIME);
		List<UsagePointEntity> usagePoints = new ArrayList<>();
		for (UUID id : List.of(SHARED_POINT_1, SHARED_POINT_2)) {
			UsagePointEntity usagePoint = usagePoint(id, ServiceCategory.ELECTRICITY);
			usagePoint.setLocalTimeParameters(shared);
			usagePoints.add(usagePointRepository.save(usagePoint));
		}
		subscriptionRepository.saveAndFlush(subscription(SHARED_SUBSCRIPTION, usagePoints));

		String xml = fetch(SHARED_SUBSCRIPTION, admin());

		// It closes the first UsagePoint's group; the second UsagePoint only links to it.
		assertThat(entryResources(xml)).containsExactly("UsagePoint", "LocalTimeParameters", "UsagePoint");
		assertThat(Pattern.compile("rel=\"related\" href=\"" + BASE + "/LocalTimeParameters/" + SHARED_LOCAL_TIME)
				.matcher(xml).results().count()).isEqualTo(2);
		// The GBA CMD model: a LocalTimeParameters is related to the UsagePoint(s) that use it.
		String usagePointHref = BASE + "/UsagePoint/";
		String localTimeEntry = xml.substring(xml.indexOf("<atom:id>urn:uuid:" + SHARED_LOCAL_TIME));
		assertThat(localTimeEntry.substring(0, localTimeEntry.indexOf("</atom:entry>")))
				.contains("rel=\"up\" href=\"" + BASE + "/LocalTimeParameters\" type=\"espi-feed/LocalTimeParameters\"",
						"rel=\"related\" href=\"" + usagePointHref + SHARED_POINT_1 + "\" type=\"espi-entry/UsagePoint\"",
						"rel=\"related\" href=\"" + usagePointHref + SHARED_POINT_2 + "\" type=\"espi-entry/UsagePoint\"");
		assertValid(xml);
	}

	@Test
	@DisplayName("a token for subscription A gets 403 for B and for an unknown subscription, alike")
	void other_subscriptions_are_forbidden() throws Exception {
		RequestPostProcessor tokenA = customer(SUBSCRIPTION_A, "FB=1_3_4_5");

		mockMvc.perform(get(BASE + "/Batch/Subscription/" + SUBSCRIPTION_B).with(tokenA))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(BASE + "/Batch/Subscription/" + UUID.randomUUID()).with(tokenA))
				.andExpect(status().isForbidden());
		// Access is checked before the query parameters: still 403, not 400.
		mockMvc.perform(get(BASE + "/Batch/Subscription/" + SUBSCRIPTION_B).param("published-min", "bad").with(tokenA))
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

	/** GET the subscription's feed, expecting 200; {@code params} are query parameter name/value pairs. */
	private String fetch(UUID subscriptionId, RequestPostProcessor token, String... params) throws Exception {
		MockHttpServletRequestBuilder request = get(BASE + "/Batch/Subscription/" + subscriptionId).with(token);
		for (int i = 0; i + 1 < params.length; i += 2) {
			request.param(params[i], params[i + 1]);
		}
		return mockMvc.perform(request)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	/** {@code seconds} before the fixtures' now, as an xs:dateTime. */
	private String ago(long seconds) {
		return Instant.ofEpochSecond(now - seconds).toString();
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
		return token(subscriptionId, scope, fbs);
	}

	/** A customer access token for the subscription with exactly these FB authorities. */
	private static RequestPostProcessor token(UUID subscriptionId, String scope, String... fbs) {
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

	/** One UsagePoint with every child kind; one IntervalBlock from yesterday, one from 40 days ago; one
	 *  UsageSummary ending now and one ElectricPowerQualitySummary ending 5 days ago, and one of each ending
	 *  60 days ago. Each collection gets its newer item first, so the feed's order comes from sorting. */
	private static UsagePointEntity usagePointA(long now) {
		UsagePointEntity usagePoint = new UsagePointEntity();
		usagePoint.setId(USAGE_POINT_A);
		usagePoint.setDescription("Sample residential electric service");
		usagePoint.setStatus((short) 1);
		usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);

		usagePoint.setLocalTimeParameters(localTime(LOCAL_TIME_A));

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
		meterReading.getIntervalBlocks().add(block(OLD_BLOCK, meterReading, now - 40 * DAY));
		usagePoint.getMeterReadings().add(meterReading);

		usagePoint.getUsageSummaries().add(usageSummary(USAGE_SUMMARY_A, usagePoint, now - 30 * DAY, now));
		usagePoint.getUsageSummaries().add(usageSummary(OLD_USAGE_SUMMARY, usagePoint, now - 90 * DAY, now));
		usagePoint.getElectricPowerQualitySummaries().add(qualitySummary(QUALITY_SUMMARY_A, usagePoint, now - 35 * DAY));
		usagePoint.getElectricPowerQualitySummaries().add(qualitySummary(OLD_QUALITY_SUMMARY, usagePoint, now - 90 * DAY));
		return usagePoint;
	}

	private static UsagePointEntity usagePoint(UUID id, ServiceCategory serviceCategory) {
		UsagePointEntity usagePoint = new UsagePointEntity();
		usagePoint.setId(id);
		usagePoint.setServiceCategory(serviceCategory);
		return usagePoint;
	}

	private static TimeConfigurationEntity localTime(UUID id) {
		TimeConfigurationEntity localTime = new TimeConfigurationEntity();
		localTime.setId(id);
		localTime.setTzOffset(-18_000L);
		localTime.setDstOffset(3_600L);
		localTime.setDstStartRule(new byte[]{0x36, 0x0E, 0x20, 0x00});
		localTime.setDstEndRule(new byte[]{(byte) 0xB4, 0x0E, 0x20, 0x00});
		return localTime;
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

	/** A 30-day summary interval starting at {@code start}. */
	private static ElectricPowerQualitySummaryEntity qualitySummary(UUID id, UsagePointEntity usagePoint, long start) {
		ElectricPowerQualitySummaryEntity quality = new ElectricPowerQualitySummaryEntity();
		quality.setId(id);
		quality.setUsagePoint(usagePoint);
		quality.setSummaryInterval(new DateTimeInterval(30 * DAY, start));
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
		return quality;
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
