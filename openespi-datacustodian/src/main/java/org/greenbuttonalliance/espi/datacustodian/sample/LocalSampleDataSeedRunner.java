/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.datacustodian.sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.common.SummaryMeasurement;
import org.greenbuttonalliance.espi.common.domain.common.enums.GrantType;
import org.greenbuttonalliance.espi.common.domain.common.enums.ResponseType;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.ServiceLocationEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.enums.UnitMultiplierKind;
import org.greenbuttonalliance.espi.common.domain.usage.enums.UnitSymbolKind;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAccountRepository;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAgreementRepository;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.customer.MeterRepository;
import org.greenbuttonalliance.espi.common.repositories.customer.ServiceLocationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds a small, deterministic ESPI sample dataset on the {@code local} profile (#253), so the customer
 * consent flow and every ESPI feed can be tested by hand right after startup (see docs/LOCAL_TESTING.md).
 *
 * <p><strong>LOCAL ONLY.</strong> The {@code customer}/{@code customer} login is public by virtue of this file.</p>
 *
 * <p><b>Where the values come from.</b> The ESPI schema values (reading type, interval layout, local time
 * parameters, summary and power-quality shapes, customer enumerations) are the ones most common across the
 * Green Button Connect My Data certification test feeds. Every identifying value (names, addresses, phone
 * numbers, e-mail, account / agreement / serial numbers, tokens, URLs) is synthetic: {@code example.com}
 * addresses and {@code 555-01xx} numbers, never data copied from a certification record.</p>
 *
 * <p>Ids are UUID v5 of fixed sample hrefs ({@link EspiIdGeneratorService}), so they are stable across
 * restarts and the runbook URLs keep working. Idempotent: does nothing when the sample UsagePoint exists.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(100) // after DevSandboxAdminSeedRunner
@RequiredArgsConstructor
public class LocalSampleDataSeedRunner implements CommandLineRunner {

    public static final String CUSTOMER_USERNAME = "customer";
    private static final String CUSTOMER_PASSWORD = "customer";

    /** The AS-seeded local Third Party client (openespi-authserver, local profile). */
    public static final String THIRD_PARTY_CLIENT_ID = "third_party";

    /** 2026-01-01T00:00 US Eastern (UTC-5): start of the sample usage. */
    static final long START = 1_767_243_600L;
    static final long HOUR = 3_600L;
    static final long DAY = 86_400L;
    static final int DAYS = 2;

    /** Hourly consumption in Wh: a residential day (night base load, morning and evening peaks). */
    static final long[] HOURLY_WH = {
            420, 390, 380, 375, 390, 450, 620, 810, 760, 640, 590, 570,
            560, 580, 600, 690, 880, 1150, 1320, 1240, 1050, 860, 650, 500};

    private final RetailCustomerRepository retailCustomerRepository;
    private final UsagePointRepository usagePointRepository;
    private final ApplicationInformationRepository applicationInformationRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerAgreementRepository customerAgreementRepository;
    private final ServiceLocationRepository serviceLocationRepository;
    private final MeterRepository meterRepository;
    private final EspiIdGeneratorService idGenerator;
    private final PasswordEncoder customerPasswordEncoder;

    @Value("${espi.datacustodian.base-url:http://localhost:8081/DataCustodian}")
    private String dataCustodianBaseUrl;

    @Value("${espi.thirdparty.base-url:http://localhost:8082/ThirdParty}")
    private String thirdPartyBaseUrl;

    @Value("${espi.authorization-server.issuer-uri:http://localhost:9999}")
    private String authorizationServerUri;

    @Override
    @Transactional
    public void run(String... args) {
        UUID usagePointId = id("UsagePoint/1");
        if (usagePointRepository.existsById(usagePointId)) {
            log.debug("Local sample data already present; skipping seed.");
            return;
        }

        RetailCustomerEntity customer = retailCustomerRepository.findByUsername(CUSTOMER_USERNAME)
                .orElseGet(() -> retailCustomerRepository.save(sampleRetailCustomer()));

        usagePointRepository.save(sampleUsagePoint(usagePointId, customer));
        applicationInformationRepository.save(sampleThirdPartyApplication());

        CustomerEntity customerResource = customerRepository.save(sampleCustomer());
        customerAccountRepository.save(sampleCustomerAccount(customerResource));
        customerAgreementRepository.save(sampleCustomerAgreement());
        serviceLocationRepository.save(sampleServiceLocation());
        meterRepository.save(sampleMeter());

        log.warn("Seeded LOCAL sample ESPI data: customer login '{}'/'{}', UsagePoint {} with {} days of hourly"
                        + " readings, customer resources, ApplicationInformation for '{}' - DO NOT USE IN PRODUCTION",
                CUSTOMER_USERNAME, CUSTOMER_PASSWORD, usagePointId, DAYS, THIRD_PARTY_CLIENT_ID);
    }

    // --- usage domain -------------------------------------------------------------------------------------

    private RetailCustomerEntity sampleRetailCustomer() {
        RetailCustomerEntity customer = new RetailCustomerEntity(CUSTOMER_USERNAME, "Sample", "Customer",
                "customer@example.com", RetailCustomerEntity.ROLE_USER);
        customer.setPassword(customerPasswordEncoder.encode(CUSTOMER_PASSWORD));
        customer.setEnabled(Boolean.TRUE);
        customer.setAccountLocked(Boolean.FALSE);
        return customer;
    }

    private UsagePointEntity sampleUsagePoint(UUID usagePointId, RetailCustomerEntity customer) {
        UsagePointEntity usagePoint = new UsagePointEntity();
        usagePoint.setId(usagePointId);
        usagePoint.setDescription("Sample residential electric service");
        usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
        usagePoint.setStatus((short) 1);
        usagePoint.setRetailCustomer(customer);
        usagePoint.setLocalTimeParameters(sampleLocalTimeParameters());

        MeterReadingEntity meterReading = new MeterReadingEntity();
        meterReading.setId(id("MeterReading/1"));
        meterReading.setDescription("Hourly electricity consumption");
        meterReading.setUsagePoint(usagePoint);
        meterReading.setReadingType(sampleReadingType());
        for (int day = 0; day < DAYS; day++) {
            meterReading.getIntervalBlocks().add(sampleIntervalBlock(meterReading, day));
        }
        usagePoint.getMeterReadings().add(meterReading);

        usagePoint.getUsageSummaries().add(sampleUsageSummary(usagePoint));
        usagePoint.getElectricPowerQualitySummaries().add(samplePowerQualitySummary(usagePoint));
        return usagePoint;
    }

    /** US Eastern: UTC-5, DST +1 h, the ESPI DST rules the certification feeds use most. */
    private TimeConfigurationEntity sampleLocalTimeParameters() {
        TimeConfigurationEntity time = new TimeConfigurationEntity();
        time.setId(id("LocalTimeParameters/1"));
        time.setDescription("US Eastern");
        time.setTzOffset(-18_000L);
        time.setDstOffset(3_600L);
        time.setDstStartRule(new byte[]{0x36, 0x0E, 0x20, 0x00});        // 360E2000
        time.setDstEndRule(new byte[]{(byte) 0xB4, 0x0E, 0x20, 0x00});   // B40E2000
        return time;
    }

    /** Hourly real energy delivered, Wh x 10^-3, USD: the most common certified kWh ReadingType. */
    private ReadingTypeEntity sampleReadingType() {
        ReadingTypeEntity readingType = new ReadingTypeEntity();
        readingType.setId(id("ReadingType/1"));
        readingType.setDescription("Energy Delivered (kWh), hourly");
        readingType.setAccumulationBehaviour("4");   // deltaData
        readingType.setCommodity("1");               // electricity, secondary metered
        readingType.setCurrency("840");              // USD
        readingType.setFlowDirection("1");           // forward (delivered)
        readingType.setIntervalLength(HOUR);
        readingType.setKind("12");                   // energy
        readingType.setPhase("0");
        readingType.setPowerOfTenMultiplier("-3");
        readingType.setUom("72");                    // Wh
        return readingType;
    }

    /** One day of 24 hourly readings, as most certification feeds block them. */
    private IntervalBlockEntity sampleIntervalBlock(MeterReadingEntity meterReading, int day) {
        IntervalBlockEntity block = new IntervalBlockEntity();
        block.setId(id("IntervalBlock/" + (day + 1)));
        block.setMeterReading(meterReading);
        long dayStart = START + day * DAY;
        block.setInterval(interval(dayStart, DAY));
        for (int hour = 0; hour < HOURLY_WH.length; hour++) {
            long wh = HOURLY_WH[hour] + day * 15L;   // second day slightly higher, so the blocks differ
            IntervalReadingEntity reading = new IntervalReadingEntity();
            reading.setIntervalBlock(block);
            reading.setTimePeriod(interval(dayStart + hour * HOUR, HOUR));
            reading.setValue(wh * 1_000);            // Wh x 10^-3
            reading.setCost(wh * 15);                // USD x 10^-5 at 0.15 USD/kWh
            block.getIntervalReadings().add(reading);
        }
        return block;
    }

    private UsageSummaryEntity sampleUsageSummary(UsagePointEntity usagePoint) {
        UsageSummaryEntity summary = new UsageSummaryEntity();
        summary.setId(id("UsageSummary/1"));
        summary.setDescription("January 2026 billing period");
        summary.setUsagePoint(usagePoint);
        summary.setBillingPeriod(interval(START, 31 * DAY));
        summary.setBillLastPeriod(21_102_000L);      // USD x 10^-5: 211.02
        summary.setBillToDate(21_102_000L);
        summary.setCostAdditionalLastPeriod(9_310_000L);
        summary.setCurrency("840");
        summary.setOverallConsumptionLastPeriod(energy(1_325_000_000L));          // 1,325 kWh
        summary.setCurrentBillingPeriodOverAllConsumption(energy(1_325_000_000L));
        summary.setQualityOfReading("17");           // validated
        summary.setStatusTimeStamp(START + 31 * DAY);
        summary.setCommodity(1);
        return summary;
    }

    private ElectricPowerQualitySummaryEntity samplePowerQualitySummary(UsagePointEntity usagePoint) {
        ElectricPowerQualitySummaryEntity quality = new ElectricPowerQualitySummaryEntity();
        quality.setId(id("ElectricPowerQualitySummary/1"));
        quality.setDescription("January 2026 power quality");
        quality.setUsagePoint(usagePoint);
        quality.setSummaryInterval(interval(START, 31 * DAY));
        quality.setMainsVoltage(240_000L);
        quality.setMeasurementProtocol((short) 0);
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

    /** The local Third Party, matching the AS-seeded {@code third_party} client and the TP local profile. */
    private ApplicationInformationEntity sampleThirdPartyApplication() {
        ApplicationInformationEntity app = new ApplicationInformationEntity();
        app.setId(id("ApplicationInformation/1"));
        app.setDescription("Local Third Party (sample)");
        app.setClientId(THIRD_PARTY_CLIENT_ID);
        app.setClientName("ThirdParty Application");
        app.setDataCustodianId("data_custodian");
        app.setDataCustodianApplicationStatus("1");
        app.setThirdPartyApplicationDescription("Local manual-test Third Party");
        app.setThirdPartyApplicationStatus("1");
        app.setThirdPartyApplicationType("1");
        app.setThirdPartyApplicationUse("1");
        app.setClientUri(thirdPartyBaseUrl);
        app.setRedirectUri(thirdPartyBaseUrl + "/login/oauth2/code/datacustodian-monthly");
        app.setThirdPartyNotifyUri(thirdPartyBaseUrl + "/espi/1_1/Notification");
        app.setAuthorizationServerUri(authorizationServerUri);
        app.setAuthorizationServerAuthorizationEndpoint(authorizationServerUri + "/oauth2/authorize");
        app.setAuthorizationServerTokenEndpoint(authorizationServerUri + "/oauth2/token");
        app.setDataCustodianResourceEndpoint(dataCustodianBaseUrl + "/espi/1_1/resource");
        app.setDataCustodianBulkRequestURI(dataCustodianBaseUrl + "/espi/1_1/resource/Batch/Bulk");
        app.setContacts("thirdparty@example.com");
        app.setTokenEndpointAuthMethod("client_secret_basic");
        app.setClientIdIssuedAt(START);
        // HistoryLength is in seconds (REQ.21.6.1.2.1): 34128000 = 395 days, as in REQ.21.4.2.1.2.1's example.
        app.setScope(Set.of(
                "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000",
                "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000"));
        app.setGrantTypes(Set.of(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.CLIENT_CREDENTIALS));
        app.setResponseTypes(ResponseType.CODE);
        return app;
    }

    // --- customer domain (synthetic identity) ---------------------------------------------------------

    private CustomerEntity sampleCustomer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(id("Customer/1"));
        customer.setDescription("Sample residential customer");
        customer.setCustomerName("Sample Customer");
        customer.setKind(CustomerKind.RESIDENTIAL);
        customer.setLocale("en_US");
        customer.setSpecialNeed("n/a");
        customer.setVip(Boolean.FALSE);
        CustomerEntity.Status customerStatus = new CustomerEntity.Status();
        customerStatus.setValue("Good");
        customerStatus.setDateTime(OffsetDateTime.ofInstant(Instant.ofEpochSecond(START), ZoneOffset.UTC));
        customer.setStatus(customerStatus);
        customer.setOrganisation(organisation("Sample Customer", "customer@example.com"));
        return customer;
    }

    private CustomerAccountEntity sampleCustomerAccount(CustomerEntity customer) {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setId(id("CustomerAccount/1"));
        account.setDescription("Sample residential account");
        account.setCustomer(customer);
        account.setType("Residential");
        account.setAccountId("SAMPLE-ACCT-0001");
        account.setBillingCycle("007");
        account.setLastBillAmount(21_102_000L);
        account.setIsPrePay(Boolean.FALSE);
        account.setContactInfo(organisation("Sample Customer", "customer@example.com"));
        return account;
    }

    private CustomerAgreementEntity sampleCustomerAgreement() {
        CustomerAgreementEntity agreement = new CustomerAgreementEntity();
        agreement.setId(id("CustomerAgreement/1"));
        agreement.setDescription("Sample residential service agreement");
        agreement.setType("Service Agreement");
        agreement.setAgreementId("SAMPLE-AGR-0001");
        agreement.setCurrency("840");
        agreement.setIsPrePay(Boolean.FALSE);
        agreement.setSignDate(OffsetDateTime.ofInstant(Instant.ofEpochSecond(START), ZoneOffset.UTC));
        agreement.setStatus(status("Active"));
        return agreement;
    }

    private ServiceLocationEntity sampleServiceLocation() {
        ServiceLocationEntity location = new ServiceLocationEntity();
        location.setId(id("ServiceLocation/1"));
        location.setDescription("Sample service location");
        location.setType("Residential");
        location.setMainAddress(address());
        location.setAccessMethod("Meter on the north wall");
        location.setNeedsInspection(Boolean.FALSE);
        location.setStatus(status("Normal"));
        return location;
    }

    private MeterEntity sampleMeter() {
        MeterEntity meter = new MeterEntity();
        meter.setId(id("Meter/1"));
        meter.setDescription("Sample smart meter");
        meter.setType("Smart Meter");
        meter.setSerialNumber("SAMPLE-MTR-0001");
        meter.setAmrSystem("AMI");
        meter.setFormNumber("2S");
        meter.setIntervalLength(900L);
        meter.setIsVirtual(Boolean.FALSE);
        meter.setIsPan(Boolean.FALSE);
        return meter;
    }

    // --- helpers -----------------------------------------------------------------------------------------

    /** UUID v5 of a fixed sample href, so the id is the same on every start. */
    UUID id(String resourcePath) {
        return idGenerator.generateEspiId(dataCustodianBaseUrl + "/espi/1_1/resource/" + resourcePath);
    }

    private static DateTimeInterval interval(long start, long duration) {
        DateTimeInterval interval = new DateTimeInterval();
        interval.setStart(start);
        interval.setDuration(duration);
        return interval;
    }

    private static SummaryMeasurement energy(long milliWattHours) {
        SummaryMeasurement measurement = new SummaryMeasurement();
        measurement.setPowerOfTenMultiplier(UnitMultiplierKind.MILLI);
        measurement.setUom(UnitSymbolKind.WH);
        measurement.setValue(milliWattHours);
        measurement.setTimeStamp(START + 31 * DAY);
        return measurement;
    }

    private static Status status(String value) {
        Status status = new Status();
        status.setValue(value);
        status.setDateTime(OffsetDateTime.ofInstant(Instant.ofEpochSecond(START), ZoneOffset.UTC));
        return status;
    }

    private static StreetAddress address() {
        StreetAddress address = new StreetAddress();
        address.setStreetDetail("100 Example Street");
        address.setTownDetail("Sampletown");
        address.setStateOrProvince("NY");
        address.setPostalCode("00000");
        address.setCountry("US");
        return address;
    }

    private static Organisation organisation(String name, String email) {
        TelephoneNumber phone = new TelephoneNumber();
        phone.setCountryCode("1");
        phone.setAreaCode("555");
        phone.setCityCode("555");
        phone.setLocalNumber("0100");
        ElectronicAddress electronic = new ElectronicAddress();
        electronic.setEmail1(email);
        Organisation organisation = new Organisation();
        organisation.setOrganisationName(name);
        organisation.setStreetAddress(address());
        organisation.setPhone1(phone);
        organisation.setElectronicAddress(electronic);
        return organisation;
    }

    /** All sample resource ids, for tests and the runbook. */
    List<UUID> sampleResourceIds() {
        return List.of(id("UsagePoint/1"), id("MeterReading/1"), id("ReadingType/1"), id("IntervalBlock/1"),
                id("UsageSummary/1"), id("ElectricPowerQualitySummary/1"), id("ApplicationInformation/1"),
                id("Customer/1"), id("CustomerAccount/1"));
    }
}
