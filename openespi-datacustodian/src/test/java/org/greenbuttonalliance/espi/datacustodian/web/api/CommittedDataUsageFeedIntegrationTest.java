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
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The usage feeds against committed data, outside any test transaction, as in production. The other integration
 * tests are {@code @Transactional}: every repository call shares the test's one session, so a lazy collection read
 * after its own transaction has ended still loads there. With {@code open-in-view: false}, a request reads lazy
 * collections only inside its own transaction, and this test is the one that would see a
 * {@code LazyInitializationException} (#290: {@code Batch/Subscription} read the token's subscription after the
 * transaction that loaded it had ended).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Usage feeds against committed data, outside a test transaction (#290)")
class CommittedDataUsageFeedIntegrationTest {

	private static final String BASE = "http://localhost/espi/1_1/resource";
	private static final long DAY = 86_400L;

	private static final UUID SUBSCRIPTION = UUID.fromString("0d1e2f3a-4b5c-5d6e-8f70-81920a1b2c01");
	private static final UUID USAGE_POINT = UUID.fromString("0d1e2f3a-4b5c-5d6e-8f70-81920a1b2c02");
	private static final UUID METER_READING = UUID.fromString("0d1e2f3a-4b5c-5d6e-8f70-81920a1b2c03");
	private static final UUID READING_TYPE = UUID.fromString("0d1e2f3a-4b5c-5d6e-8f70-81920a1b2c04");
	private static final UUID BLOCK = UUID.fromString("0d1e2f3a-4b5c-5d6e-8f70-81920a1b2c05");
	private static final UUID SUMMARY = UUID.fromString("0d1e2f3a-4b5c-5d6e-8f70-81920a1b2c06");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PlatformTransactionManager transactionManager;

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

	private Long retailCustomerId;
	private UUID applicationId;
	private UUID authorizationId;

	@BeforeEach
	void commitFixture() {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			RetailCustomerEntity customer = new RetailCustomerEntity("committed01", "Sample", "Customer",
					"committed01@example.com", RetailCustomerEntity.ROLE_USER);
			customer.setPassword("not-used");
			customer.setEnabled(Boolean.TRUE);
			customer = retailCustomerRepository.save(customer);
			retailCustomerId = customer.getId();

			UsagePointEntity usagePoint = usagePoint(Instant.now().getEpochSecond());
			usagePoint.setRetailCustomer(customer);
			usagePoint = usagePointRepository.save(usagePoint);

			ApplicationInformationEntity application = new ApplicationInformationEntity();
			application.setClientId("committed-client-01");
			application.setDataCustodianId("data_custodian");
			application.setGrantTypes(new HashSet<>(Set.of(GrantType.AUTHORIZATION_CODE)));
			application = applicationInformationRepository.save(application);
			applicationId = application.getId();

			AuthorizationEntity authorization = new AuthorizationEntity();
			authorization.setAccessToken("token-committed-01");
			authorization.setStatus(AuthorizationEntity.STATUS_ACTIVE);
			authorization = authorizationRepository.save(authorization);
			authorizationId = authorization.getId();

			SubscriptionEntity subscription = new SubscriptionEntity(SUBSCRIPTION);
			subscription.setRetailCustomer(customer);
			subscription.setApplicationInformation(application);
			subscription.setAuthorization(authorization);
			subscription.setUsagePoints(List.of(usagePoint));
			subscriptionRepository.save(subscription);
		});
	}

	@AfterEach
	void deleteFixture() {
		// The UsagePoint's delete cascades to its MeterReading (and that to its ReadingType and IntervalBlock) and
		// its UsageSummary; the subscription, authorization, application and customer go separately.
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			subscriptionRepository.deleteById(SUBSCRIPTION);
			usagePointRepository.deleteById(USAGE_POINT);
			authorizationRepository.deleteById(authorizationId);
			applicationInformationRepository.deleteById(applicationId);
			retailCustomerRepository.deleteById(retailCustomerId);
		});
	}

	@Test
	@DisplayName("a customer token reads Batch/Subscription, the root feeds and entries, and an XPath feed: 200, not a lazy-loading 500")
	void customer_token_reads_committed_data() throws Exception {
		RequestPostProcessor token = customer();
		String xpath = "/RetailCustomer/" + retailCustomerId + "/UsagePoint/" + USAGE_POINT;

		for (String path : List.of("/Batch/Subscription/" + SUBSCRIPTION, "/UsagePoint", "/UsagePoint/" + USAGE_POINT,
				"/MeterReading/" + METER_READING, "/IntervalBlock", "/UsageSummary", "/ReadingType/" + READING_TYPE,
				xpath + "/MeterReading", xpath + "/MeterReading/" + METER_READING + "/IntervalBlock")) {
			var response = mockMvc.perform(get(BASE + path).with(token)).andReturn().getResponse();
			assertThat(response.getStatus()).as("GET %s", path).isEqualTo(200);
			// Every one of these responses holds the UsagePoint, or a link to it or to its MeterReading.
			assertThat(response.getContentAsString()).as("GET %s", path)
					.containsAnyOf(BASE + "/UsagePoint/" + USAGE_POINT, BASE + "/MeterReading/" + METER_READING);
		}
	}

	@Test
	@DisplayName("the admin reads Batch/Subscription and the root entries against committed data")
	void admin_reads_committed_data() throws Exception {
		RequestPostProcessor admin = opaqueToken().authorities(new SimpleGrantedAuthority("SCOPE_DataCustodian_Admin_Access"));

		for (String path : List.of("/Batch/Subscription/" + SUBSCRIPTION, "/UsagePoint/" + USAGE_POINT,
				"/ReadingType/" + READING_TYPE)) {
			assertThat(mockMvc.perform(get(BASE + path).with(admin)).andReturn().getResponse().getStatus())
					.as("GET %s", path).isEqualTo(200);
		}
	}

	/** The subscription's customer token: interval metering, electricity, a usage summary. */
	private static RequestPostProcessor customer() {
		String[] fbs = {"FB_4", "FB_5", "FB_16"};
		return opaqueToken()
				.attributes(attributes -> {
					attributes.put("resourceURI", BASE + "/Batch/Subscription/" + SUBSCRIPTION);
					attributes.put("scope", List.of("FB=4_5_16;HistoryLength=31536000"));
				})
				.authorities(Arrays.stream(fbs).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
	}

	/** An electricity UsagePoint with one MeterReading (its ReadingType and a block from yesterday) and a summary. */
	private static UsagePointEntity usagePoint(long now) {
		UsagePointEntity usagePoint = new UsagePointEntity();
		usagePoint.setId(USAGE_POINT);
		usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
		usagePoint.setStatus((short) 1);

		ReadingTypeEntity readingType = new ReadingTypeEntity();
		readingType.setId(READING_TYPE);
		readingType.setAccumulationBehaviour("4");
		readingType.setCommodity("1");
		readingType.setFlowDirection("1");
		readingType.setIntervalLength(3_600L);
		readingType.setKind("12");
		readingType.setPowerOfTenMultiplier("-3");
		readingType.setUom("72");

		MeterReadingEntity meterReading = new MeterReadingEntity();
		meterReading.setId(METER_READING);
		meterReading.setUsagePoint(usagePoint);
		meterReading.setReadingType(readingType);

		IntervalBlockEntity block = new IntervalBlockEntity();
		block.setId(BLOCK);
		block.setMeterReading(meterReading);
		block.setInterval(new DateTimeInterval(DAY, now - DAY));
		IntervalReadingEntity reading = new IntervalReadingEntity();
		reading.setIntervalBlock(block);
		reading.setTimePeriod(new DateTimeInterval(3_600L, now - DAY));
		reading.setValue(1_250_000L);
		block.getIntervalReadings().add(reading);
		meterReading.getIntervalBlocks().add(block);
		usagePoint.getMeterReadings().add(meterReading);

		UsageSummaryEntity summary = new UsageSummaryEntity();
		summary.setId(SUMMARY);
		summary.setUsagePoint(usagePoint);
		summary.setBillingPeriod(new DateTimeInterval(30 * DAY, now - 30 * DAY));
		summary.setStatusTimeStamp(now);
		usagePoint.getUsageSummaries().add(summary);
		return usagePoint;
	}
}
