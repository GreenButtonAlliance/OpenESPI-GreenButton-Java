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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The usage resource GETs (#286, #290) with real data and the real security rules. Resources are addressed only in
 * the ROOT form ({@code UsagePoint/{id}}) or the RetailCustomer XPath form, never through
 * {@code Subscription/{id}/...}: a Subscription is per grant, so such an href would not be repeatable.
 *
 * <p>A customer token's root collections hold only its own subscription's rows (the UsagePoints its commodity
 * Function Blocks cover, within its HistoryLength); any other row is 404. Every entry has the GBA model's links
 * ({@link UsageEntries}), the same in a feed and read on its own.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Usage resource GETs scoped to the token's subscription (#286)")
class SubscriptionResourceScopingIntegrationTest {

	private static final String BASE = "http://localhost/espi/1_1/resource";
	private static final long DAY = 86_400L;
	private static final Pattern LINK = Pattern.compile("<atom:link\\b[^>]*/>");

	private static final UUID SUBSCRIPTION_A = UUID.fromString("5e1f2a3b-4c5d-5e6f-8a7b-9c0d1e2f3a01");
	private static final UUID SUBSCRIPTION_B = UUID.fromString("5e1f2a3b-4c5d-5e6f-8a7b-9c0d1e2f3a02");
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
	private static final UUID SUMMARY_A = UUID.fromString("a36e7f80-91a2-53b4-9fc0-4b5c6d7e8f01");
	private static final UUID SUMMARY_B = UUID.fromString("a36e7f80-91a2-53b4-9fc0-4b5c6d7e8f02");
	private static final UUID OLD_SUMMARY_A = UUID.fromString("a36e7f80-91a2-53b4-9fc0-4b5c6d7e8f03");
	private static final UUID GAS_SUMMARY_A = UUID.fromString("a36e7f80-91a2-53b4-9fc0-4b5c6d7e8f04");
	private static final UUID QUALITY_A = UUID.fromString("b47f8091-a2b3-54c5-a0d1-5c6d7e8f9001");
	private static final UUID QUALITY_B = UUID.fromString("b47f8091-a2b3-54c5-a0d1-5c6d7e8f9002");
	private static final UUID OLD_QUALITY_A = UUID.fromString("b47f8091-a2b3-54c5-a0d1-5c6d7e8f9003");
	private static final UUID LOCAL_TIME_A = UUID.fromString("c5809102-b3c4-55d6-b1e2-6d7e8f90a101");

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

	/** Subscription A's and B's RetailCustomer ids, for the RetailCustomer XPath paths. */
	private Long retailCustomerA;
	private Long retailCustomerB;

	@BeforeEach
	void setUp() {
		long now = Instant.now().getEpochSecond();
		UsagePointEntity usagePointA = usagePoint(USAGE_POINT_A, ServiceCategory.ELECTRICITY,
				METER_READING_A, READING_TYPE_A, BLOCK_A, SUMMARY_A, QUALITY_A, now);
		// Older than a 30-day HistoryLength: a block from 40 days ago, summaries that ended 60 days ago.
		MeterReadingEntity meterReadingA = usagePointA.getMeterReadings().getFirst();
		meterReadingA.getIntervalBlocks().add(block(OLD_BLOCK_A, meterReadingA, now - 40 * DAY));
		usagePointA.getUsageSummaries().add(usageSummary(OLD_SUMMARY_A, usagePointA, now - 90 * DAY, now));
		usagePointA.getElectricPowerQualitySummaries().add(qualitySummary(OLD_QUALITY_A, usagePointA, now - 90 * DAY));
		usagePointA.setLocalTimeParameters(localTime(LOCAL_TIME_A));
		usagePointA = usagePointRepository.save(usagePointA);
		UsagePointEntity gasPointA = bareUsagePoint(GAS_POINT_A, ServiceCategory.GAS);
		gasPointA.getUsageSummaries().add(usageSummary(GAS_SUMMARY_A, gasPointA, now - 30 * DAY, now));
		gasPointA = usagePointRepository.save(gasPointA);
		UsagePointEntity usagePointB = usagePointRepository.save(usagePoint(USAGE_POINT_B, ServiceCategory.ELECTRICITY,
				METER_READING_B, READING_TYPE_B, BLOCK_B, SUMMARY_B, QUALITY_B, now));

		retailCustomerA = subscriptionRepository.save(subscription(SUBSCRIPTION_A, List.of(usagePointA, gasPointA)))
				.getRetailCustomer().getId();
		retailCustomerB = subscriptionRepository.saveAndFlush(subscription(SUBSCRIPTION_B, List.of(usagePointB)))
				.getRetailCustomer().getId();
	}

	@Test
	@DisplayName("a customer token's root collections hold only its own subscription's rows")
	void root_collections_hold_only_the_tokens_rows() throws Exception {
		RequestPostProcessor token = customerA();

		assertThat(read("/UsagePoint", token)).contains(USAGE_POINT_A.toString())
				.doesNotContain(USAGE_POINT_B.toString(), GAS_POINT_A.toString());
		assertThat(read("/MeterReading", token)).contains(METER_READING_A.toString())
				.doesNotContain(METER_READING_B.toString());
		assertThat(read("/IntervalBlock", token)).contains(BLOCK_A.toString(), OLD_BLOCK_A.toString())
				.doesNotContain(BLOCK_B.toString());
		assertThat(read("/UsageSummary", token)).contains(SUMMARY_A.toString())
				.doesNotContain(SUMMARY_B.toString(), GAS_SUMMARY_A.toString());
		assertThat(read("/ElectricPowerQualitySummary", token)).contains(QUALITY_A.toString())
				.doesNotContain(QUALITY_B.toString());
		assertThat(read("/ReadingType", token)).contains(READING_TYPE_A.toString())
				.doesNotContain(READING_TYPE_B.toString());
	}

	@Test
	@DisplayName("a customer token reads its own root entries; another customer's, an uncovered commodity's and unknown ids are 404 alike")
	void root_entries_are_the_tokens_only() throws Exception {
		RequestPostProcessor token = customerA();

		for (String own : List.of("/UsagePoint/" + USAGE_POINT_A, "/MeterReading/" + METER_READING_A,
				"/IntervalBlock/" + BLOCK_A, "/UsageSummary/" + SUMMARY_A, "/ElectricPowerQualitySummary/" + QUALITY_A,
				"/ReadingType/" + READING_TYPE_A)) {
			expect(own, token, 200);
		}
		UUID unknown = UUID.randomUUID();
		for (String other : List.of("/UsagePoint/" + USAGE_POINT_B, "/MeterReading/" + METER_READING_B,
				"/IntervalBlock/" + BLOCK_B, "/UsageSummary/" + SUMMARY_B, "/ElectricPowerQualitySummary/" + QUALITY_B,
				"/ReadingType/" + READING_TYPE_B, "/UsagePoint/" + GAS_POINT_A, "/UsageSummary/" + GAS_SUMMARY_A,
				"/UsagePoint/" + unknown, "/MeterReading/" + unknown, "/IntervalBlock/" + unknown)) {
			expect(other, token, 404);
		}
	}

	@Test
	@DisplayName("RetailCustomer XPath collections: the token's own customer only (403 otherwise), and only its readable UsagePoints (404 otherwise)")
	void retail_customer_collections() throws Exception {
		RequestPostProcessor token = customerA();
		String upA = "/RetailCustomer/" + retailCustomerA + "/UsagePoint/" + USAGE_POINT_A;

		assertThat(read(upA + "/MeterReading", token)).contains(METER_READING_A.toString())
				.doesNotContain(METER_READING_B.toString());
		assertThat(read(upA + "/MeterReading/" + METER_READING_A + "/IntervalBlock", token))
				.contains(BLOCK_A.toString()).doesNotContain(BLOCK_B.toString());
		assertThat(read(upA + "/UsageSummary", token)).contains(SUMMARY_A.toString())
				.doesNotContain(SUMMARY_B.toString());
		assertThat(read(upA + "/ElectricPowerQualitySummary", token)).contains(QUALITY_A.toString());

		expect("/RetailCustomer/" + retailCustomerB + "/UsagePoint/" + USAGE_POINT_B + "/MeterReading", token, 403);
		expect("/RetailCustomer/" + retailCustomerB + "/UsagePoint/" + USAGE_POINT_B + "/UsageSummary", token, 403);
		expect("/RetailCustomer/" + retailCustomerA + "/UsagePoint/" + USAGE_POINT_B + "/MeterReading", token, 404);
		expect("/RetailCustomer/" + retailCustomerA + "/UsagePoint/" + GAS_POINT_A + "/MeterReading", token, 404);
		expect(upA + "/MeterReading/" + METER_READING_B + "/IntervalBlock", token, 404);
	}

	@Test
	@DisplayName("summaries follow the commodity FBs, as in Batch/Subscription, unless the token has summary FBs only")
	void summaries_follow_the_commodity_function_blocks() throws Exception {
		String gasSummaries = "/RetailCustomer/" + retailCustomerA + "/UsagePoint/" + GAS_POINT_A + "/UsageSummary";

		expect(gasSummaries, customerA(), 404);
		expect("/UsageSummary/" + GAS_SUMMARY_A, customerA(), 404);
		RequestPostProcessor gas = token(SUBSCRIPTION_A, "FB_4", "FB_10", "FB_16");
		RequestPostProcessor summariesOnly = token(SUBSCRIPTION_A, "FB_16", "FB_17");
		assertThat(List.of(read(gasSummaries, gas), read(gasSummaries, summariesOnly),
				read("/UsageSummary/" + GAS_SUMMARY_A, summariesOnly)))
				.allSatisfy(xml -> assertThat(xml).contains(GAS_SUMMARY_A.toString()));
		// Summary FBs only: every summary of its own subscription, nothing of another's, and no UsagePoint.
		assertThat(read("/UsageSummary", summariesOnly)).contains(SUMMARY_A.toString(), GAS_SUMMARY_A.toString())
				.doesNotContain(SUMMARY_B.toString());
		expect("/UsageSummary/" + SUMMARY_B, summariesOnly, 404);
		expect("/UsagePoint/" + USAGE_POINT_A, summariesOnly, 403);
		expect("/IntervalBlock", summariesOnly, 403);
	}

	@Test
	@DisplayName("HistoryLength limits the IntervalBlocks and summaries: older ones are left out of feeds and 404 as entries")
	void history_length_is_a_ceiling() throws Exception {
		RequestPostProcessor monthToken = token(SUBSCRIPTION_A, 30 * DAY, "FB_4", "FB_5", "FB_16", "FB_17");

		assertThat(read("/IntervalBlock", monthToken)).contains(BLOCK_A.toString()).doesNotContain(OLD_BLOCK_A.toString());
		assertThat(read("/UsageSummary", monthToken)).contains(SUMMARY_A.toString()).doesNotContain(OLD_SUMMARY_A.toString());
		assertThat(read("/ElectricPowerQualitySummary", monthToken)).contains(QUALITY_A.toString())
				.doesNotContain(OLD_QUALITY_A.toString());
		// Links name only what the token may read, so the MeterReading no longer links the old block.
		assertThat(read("/MeterReading/" + METER_READING_A, monthToken)).doesNotContain(OLD_BLOCK_A.toString());
		for (String old : List.of("/IntervalBlock/" + OLD_BLOCK_A, "/UsageSummary/" + OLD_SUMMARY_A,
				"/ElectricPowerQualitySummary/" + OLD_QUALITY_A)) {
			expect(old, monthToken, 404);
		}
		expect("/IntervalBlock/" + OLD_BLOCK_A, admin(), 200);
	}

	@Test
	@DisplayName("IntervalBlock and summary feeds are in interval-start order, as in Batch/Subscription")
	void feeds_are_chronological() throws Exception {
		String blocks = read("/IntervalBlock", customerA());
		String summaries = read("/UsageSummary", customerA());

		assertThat(blocks.indexOf(OLD_BLOCK_A.toString())).isNotNegative().isLessThan(blocks.indexOf(BLOCK_A.toString()));
		assertThat(summaries.indexOf(OLD_SUMMARY_A.toString())).isNotNegative()
				.isLessThan(summaries.indexOf(SUMMARY_A.toString()));
	}

	@Test
	@DisplayName("every entry has the GBA model's links in ROOT form, the same in its feeds and read on its own")
	void entries_have_the_model_links() throws Exception {
		String upXPath = "/RetailCustomer/" + retailCustomerA + "/UsagePoint/" + USAGE_POINT_A;
		String mrXPath = upXPath + "/MeterReading/" + METER_READING_A;

		// The customer token and the admin get the same entries, with the same hrefs (REQ.21.6.1.1.6).
		for (RequestPostProcessor reader : List.of(customerA(), admin())) {
			assertLinks("UsagePoint", USAGE_POINT_A, reader, List.of(),
					entry("LocalTimeParameters", LOCAL_TIME_A),
					feed(upXPath + "/MeterReading", "MeterReading"), entry("MeterReading", METER_READING_A),
					feed(upXPath + "/UsageSummary", "UsageSummary"),
					entry("UsageSummary", OLD_SUMMARY_A), entry("UsageSummary", SUMMARY_A),
					feed(upXPath + "/ElectricPowerQualitySummary", "ElectricPowerQualitySummary"),
					entry("ElectricPowerQualitySummary", OLD_QUALITY_A), entry("ElectricPowerQualitySummary", QUALITY_A));
			assertLinks("MeterReading", METER_READING_A, reader, List.of(upXPath + "/MeterReading"),
					entry("ReadingType", READING_TYPE_A), entry("UsagePoint", USAGE_POINT_A),
					feed(mrXPath + "/IntervalBlock", "IntervalBlock"),
					entry("IntervalBlock", OLD_BLOCK_A), entry("IntervalBlock", BLOCK_A));
			assertLinks("IntervalBlock", BLOCK_A, reader, List.of(mrXPath + "/IntervalBlock"),
					entry("MeterReading", METER_READING_A));
			assertLinks("UsageSummary", SUMMARY_A, reader, List.of(upXPath + "/UsageSummary"),
					entry("UsagePoint", USAGE_POINT_A));
			assertLinks("ElectricPowerQualitySummary", QUALITY_A, reader,
					List.of(upXPath + "/ElectricPowerQualitySummary"), entry("UsagePoint", USAGE_POINT_A));
			// For the admin, the MeterReadings come from MeterReadingRepository.findAllByReadingTypeId.
			assertLinks("ReadingType", READING_TYPE_A, reader, List.of(), entry("MeterReading", METER_READING_A));
		}
		// No summary Function Blocks: no links to summaries the token could not read.
		assertLinks("UsagePoint", USAGE_POINT_A, token(SUBSCRIPTION_A, "FB_4", "FB_5"), List.of(),
				entry("LocalTimeParameters", LOCAL_TIME_A),
				feed(upXPath + "/MeterReading", "MeterReading"), entry("MeterReading", METER_READING_A));
		// The other customer's entries name their own RetailCustomer, for the admin as for that customer.
		String upB = "/RetailCustomer/" + retailCustomerB + "/UsagePoint/" + USAGE_POINT_B;
		assertThat(List.of(links(read("/UsagePoint/" + USAGE_POINT_B, admin())),
				links(read("/UsagePoint/" + USAGE_POINT_B, token(SUBSCRIPTION_B, "FB_4", "FB_5")))))
				.allSatisfy(links -> assertThat(links).contains(feed(upB + "/MeterReading", "MeterReading")));
	}

	@Test
	@DisplayName("no entry link and no served path goes through /Subscription/{id}/: a Subscription is per grant")
	void nothing_is_addressed_through_a_subscription() throws Exception {
		String upXPath = "/RetailCustomer/" + retailCustomerA + "/UsagePoint/" + USAGE_POINT_A;
		for (String path : List.of("/UsagePoint", "/MeterReading", "/IntervalBlock", "/UsageSummary",
				"/ElectricPowerQualitySummary", "/ReadingType", upXPath + "/MeterReading",
				upXPath + "/MeterReading/" + METER_READING_A + "/IntervalBlock", "/Batch/Subscription/" + SUBSCRIPTION_A)) {
			assertThat(links(read(path, customerA()))).as("links of %s", path)
					.isNotEmpty()
					.noneSatisfy(link -> assertThat(link).contains("/Subscription/" + SUBSCRIPTION_A + "/"));
		}
		for (String path : List.of("/Subscription/" + SUBSCRIPTION_A + "/UsagePoint",
				"/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + USAGE_POINT_A,
				"/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + USAGE_POINT_A + "/MeterReading",
				"/Subscription/" + SUBSCRIPTION_A + "/UsagePoint/" + USAGE_POINT_A + "/UsageSummary")) {
			expect(path, customerA(), 404);
		}
	}

	@Test
	@DisplayName("the admin reads every row, at the root and through any retail customer")
	void admin_reads_everything() throws Exception {
		assertThat(read("/UsagePoint", admin()))
				.contains(USAGE_POINT_A.toString(), USAGE_POINT_B.toString(), GAS_POINT_A.toString());
		expect("/IntervalBlock/" + BLOCK_B, admin(), 200);
		assertThat(read("/RetailCustomer/" + retailCustomerB + "/UsagePoint/" + USAGE_POINT_B + "/MeterReading", admin()))
				.contains(METER_READING_B.toString());
		expect("/RetailCustomer/" + retailCustomerA + "/UsagePoint/" + USAGE_POINT_B + "/MeterReading", admin(), 404);
	}

	@Test
	@DisplayName("the PII root collections and entries are admin only (#291)")
	void customer_roots_are_admin_only() throws Exception {
		RequestPostProcessor token = token(SUBSCRIPTION_A, "FB_53", "FB_54", "FB_56");
		UUID any = UUID.randomUUID();

		for (String path : List.of("/Customer", "/Customer/" + any, "/CustomerAccount", "/CustomerAccount/" + any)) {
			expect(path, token, 403);
		}
	}

	@Test
	@DisplayName("a Third Party client access token, and a token without a subscription, read nothing")
	void tokens_without_a_subscription_are_refused() throws Exception {
		RequestPostProcessor clientToken = opaqueToken()
				.attributes(attributes -> {
					attributes.put("resourceURI", BASE + "/Batch/Subscription/" + SUBSCRIPTION_A);
					attributes.put("scope", List.of("FB=34_35"));
				})
				.authorities(authorities(EspiScopeOpaqueTokenIntrospector.CLIENT_ACCESS_TOKEN,
						"FB_4", "FB_5", "FB_15", "FB_17"));
		RequestPostProcessor noSubscription = opaqueToken().authorities(authorities("FB_4", "FB_5", "FB_16"));

		for (RequestPostProcessor token : List.of(clientToken, noSubscription)) {
			expect("/UsagePoint", token, 403);
			expect("/UsagePoint/" + USAGE_POINT_A, token, 403);
			expect("/UsageSummary", token, 403);
		}
	}

	// --- helpers ------------------------------------------------------------------------------------------

	/**
	 * The resource's links are exactly {@code self} and {@code up} in ROOT form and {@code related}, in its root feed,
	 * read on its own (a schema-valid Atom entry) and in each RetailCustomer XPath feed of {@code xpathFeeds}.
	 */
	private void assertLinks(String resource, UUID id, RequestPostProcessor token, List<String> xpathFeeds,
							 String... related) throws Exception {
		List<String> expected = new ArrayList<>(List.of(
				"<atom:link rel=\"self\" href=\"" + BASE + "/" + resource + "/" + id + "\" type=\"espi-entry/" + resource + "\"/>",
				"<atom:link rel=\"up\" href=\"" + BASE + "/" + resource + "\" type=\"espi-feed/" + resource + "\"/>"));
		expected.addAll(List.of(related));
		String entry = read("/" + resource + "/" + id, token);
		List<List<String>> found = new ArrayList<>();
		found.add(links(entry));
		// limit: the admin's root feed is paged (a customer token's is not); one page holds every test row.
		found.add(links(entryOf(read("/" + resource + "?limit=10000", token), id)));
		for (String xpathFeed : xpathFeeds) {
			found.add(links(entryOf(read(xpathFeed, token), id)));
		}

		assertThat(found).as("links of %s %s", resource, id)
				.allSatisfy(links -> assertThat(links).containsExactlyElementsOf(expected));
		assertThat(entry)
				.contains("<atom:entry", "<atom:id>urn:uuid:" + id + "</atom:id>")
				.containsPattern("<atom:content>\\s*<espi:" + resource)
				.doesNotContain("<atom:feed");
		EspiXmlSchemaValidator.Result validation = EspiXmlSchemaValidator.instance().validate(entry);
		// The test UsagePoints' ServiceCategory is incomplete, as in BatchSubscriptionIntegrationTest.
		assertThat(validation.issues())
				.as(String.join("\n", EspiXmlSchemaValidator.describe(validation)))
				.filteredOn(issue -> issue.severity() != EspiXmlSchemaValidator.Severity.WARNING)
				.allSatisfy(issue -> assertThat(issue.message()).contains("ServiceCategory"));
	}

	/** One entry of a feed: from its atom:id to its end. */
	private static String entryOf(String feed, UUID id) {
		String from = feed.substring(feed.indexOf("<atom:id>urn:uuid:" + id));
		return from.substring(0, from.indexOf("</atom:entry>"));
	}

	private static List<String> links(String xml) {
		return LINK.matcher(xml).results().map(MatchResult::group).toList();
	}

	private static String entry(String resource, UUID id) {
		return "<atom:link rel=\"related\" href=\"" + BASE + "/" + resource + "/" + id + "\" type=\"espi-entry/" + resource + "\"/>";
	}

	private static String feed(String path, String resource) {
		return "<atom:link rel=\"related\" href=\"" + BASE + path + "\" type=\"espi-feed/" + resource + "\"/>";
	}

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

	/** A subscription of a new RetailCustomer, who also becomes its UsagePoints' customer. */
	private SubscriptionEntity subscription(UUID id, List<UsagePointEntity> usagePoints) {
		String key = id.toString().substring(id.toString().length() - 4);
		RetailCustomerEntity customer = new RetailCustomerEntity("scope" + key, "Sample", "Customer",
				"scope" + key + "@example.com", RetailCustomerEntity.ROLE_USER);
		customer.setPassword("not-used");
		customer.setEnabled(Boolean.TRUE);
		customer = retailCustomerRepository.save(customer);
		for (UsagePointEntity usagePoint : usagePoints) {
			usagePoint.setRetailCustomer(customer);
		}

		ApplicationInformationEntity application = new ApplicationInformationEntity();
		application.setClientId("scope-client-" + key);
		application.setDataCustodianId("data_custodian");
		application.setGrantTypes(new HashSet<>(Set.of(GrantType.AUTHORIZATION_CODE)));

		AuthorizationEntity authorization = new AuthorizationEntity();
		authorization.setAccessToken("token-" + id);
		authorization.setStatus(AuthorizationEntity.STATUS_ACTIVE);

		SubscriptionEntity subscription = new SubscriptionEntity(id);
		subscription.setRetailCustomer(customer);
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
		usagePoint.getElectricPowerQualitySummaries().add(qualitySummary(qualityId, usagePoint, now - 30 * DAY));
		return usagePoint;
	}

	/** Eastern time, with US daylight saving rules. */
	private static TimeConfigurationEntity localTime(UUID id) {
		TimeConfigurationEntity localTime = new TimeConfigurationEntity();
		localTime.setId(id);
		localTime.setTzOffset(-18_000L);
		localTime.setDstOffset(3_600L);
		localTime.setDstStartRule(new byte[]{0x36, 0x0E, 0x20, 0x00});
		localTime.setDstEndRule(new byte[]{(byte) 0xB4, 0x0E, 0x20, 0x00});
		return localTime;
	}

	/** A 30-day power quality summary starting at {@code start}. */
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
