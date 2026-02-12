/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.common.test;

import net.datafaker.Faker;
import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.customer.entity.*;
import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;
import org.greenbuttonalliance.espi.common.domain.usage.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Minimal utility class for creating test data entities.
 * 
 * Provides only essential methods needed for repository testing.
 */
public class TestDataBuilders {

    private static final Faker faker = new Faker();

    /**
     * Creates a valid CustomerEntity for testing.
     */
    public static CustomerEntity createValidCustomer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setDescription(faker.lorem().sentence(5, 10));
        customer.setCustomerName(faker.company().name());
        customer.setKind(CustomerKind.RESIDENTIAL);
        customer.setPucNumber(faker.number().digits(10));
        customer.setSpecialNeed("NONE");
        customer.setVip(false);
        customer.setLocale("en_US");
        return customer;
    }

    /**
     * Creates a valid StatementEntity for testing.
     */
    public static StatementEntity createValidStatement() {
        StatementEntity statement = new StatementEntity();
        statement.setDescription(faker.lorem().sentence(3, 8));
        statement.setIssueDateTime(OffsetDateTime.from(faker.timeAndDate().
                past(30, java.util.concurrent.TimeUnit.DAYS).atOffset(ZoneOffset.UTC)));
        return statement;
    }

    /**
     * Creates a valid StatementEntity with customer relationship.
     */
    public static StatementEntity createValidStatementWithCustomer(CustomerEntity customer) {
        StatementEntity statement = createValidStatement();
        statement.setCustomer(customer);
        return statement;
    }

    /**
     * Creates a valid UsagePointEntity for testing.
     */
    public static UsagePointEntity createValidUsagePoint() {
        UsagePointEntity usagePoint = new UsagePointEntity();
        usagePoint.setDescription(faker.lorem().sentence(4, 8));
        usagePoint.setStatus((short) faker.number().numberBetween(1, 5));
        usagePoint.setRoleFlags(new byte[]{0x01, 0x02, 0x04});
        
        // ServiceCategory is required (@NotNull constraint)
        usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
        
        return usagePoint;
    }

    /**
     * Creates a valid MeterReadingEntity for testing.
     */
    public static MeterReadingEntity createValidMeterReading() {
        MeterReadingEntity meterReading = new MeterReadingEntity();
        meterReading.setDescription(faker.lorem().sentence(3, 6));
        return meterReading;
    }

    /**
     * Creates a valid MeterReadingEntity with UsagePoint relationship.
     */
    public static MeterReadingEntity createValidMeterReadingWithUsagePoint(UsagePointEntity usagePoint) {
        MeterReadingEntity meterReading = createValidMeterReading();
        meterReading.setUsagePoint(usagePoint);
        return meterReading;
    }

    /**
     * Creates a valid IntervalBlockEntity for testing.
     */
    public static IntervalBlockEntity createValidIntervalBlock() {
        IntervalBlockEntity intervalBlock = new IntervalBlockEntity();
        intervalBlock.setDescription(faker.lorem().sentence(3, 6));
        
        // Add basic DateTimeInterval
        DateTimeInterval interval = new DateTimeInterval();
        interval.setDuration(3600L); // 1 hour
        interval.setStart(faker.timeAndDate().past(7, java.util.concurrent.TimeUnit.DAYS).getEpochSecond());
        intervalBlock.setInterval(interval);
        
        return intervalBlock;
    }

    /**
     * Creates a valid IntervalBlockEntity with MeterReading relationship.
     */
    public static IntervalBlockEntity createValidIntervalBlockWithMeterReading(MeterReadingEntity meterReading) {
        IntervalBlockEntity intervalBlock = createValidIntervalBlock();
        intervalBlock.setMeterReading(meterReading);
        return intervalBlock;
    }

    /**
     * Creates a valid IntervalReadingEntity for testing.
     */
    public static IntervalReadingEntity createValidIntervalReading() {
        IntervalReadingEntity intervalReading = new IntervalReadingEntity();
        intervalReading.setCost(faker.number().numberBetween(100000L, 999999L));
        intervalReading.setValue(faker.number().numberBetween(10000L, 99999L));
        
        // Add basic DateTimeInterval for time period
        DateTimeInterval timePeriod = new DateTimeInterval();
        timePeriod.setDuration(900L); // 15 minutes
        timePeriod.setStart(faker.timeAndDate().past(1, java.util.concurrent.TimeUnit.DAYS).getEpochSecond());
        intervalReading.setTimePeriod(timePeriod);
        
        return intervalReading;
    }

    /**
     * Creates a valid ReadingTypeEntity for testing.
     */
    public static ReadingTypeEntity createValidReadingType() {
        ReadingTypeEntity readingType = new ReadingTypeEntity();
        readingType.setDescription(faker.lorem().sentence(4, 8));
        return readingType;
    }

    /**
     * Creates a valid RetailCustomerEntity for testing.
     * Note: RetailCustomer is an application-specific correlation table (not part of ESPI standard).
     */
    public static RetailCustomerEntity createValidRetailCustomer() {
        RetailCustomerEntity retailCustomer = new RetailCustomerEntity();

        // Ensure enabled is set properly (@NotNull constraint)
        retailCustomer.setEnabled(true);
        
        // Ensure first name meets validation constraints (@NotEmpty, max 30 chars)
        String firstName = faker.name().firstName();
        if (firstName == null || firstName.trim().isEmpty()) {
            firstName = "TestFirst";
        }
        if (firstName.length() > 30) {
            firstName = firstName.substring(0, 30);
        }
        retailCustomer.setFirstName(firstName);
        
        // Ensure last name meets validation constraints (@NotEmpty, max 30 chars)
        String lastName = faker.name().lastName();
        if (lastName == null || lastName.trim().isEmpty()) {
            lastName = "TestLast";
        }
        if (lastName.length() > 30) {
            lastName = lastName.substring(0, 30);
        }
        retailCustomer.setLastName(lastName);
        
        // Create a password that meets the complex validation pattern
        // Pattern: ^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$
        // Must have: lowercase, uppercase, digit, special char, min 8 chars
        retailCustomer.setPassword("TestPass123!");
        
        // Ensure role meets validation constraints (@NotEmpty)
        retailCustomer.setRole("ROLE_USER");
        
        // Ensure username meets validation constraints (@NotEmpty, 4-30 chars, unique)
        // Generate a shorter, more predictable username to avoid validation issues
        String baseUsername = "user" + faker.number().digits(6);
        String username = baseUsername + "@test.com";
        if (username.length() > 30) {
            // If still too long, use just the base username
            username = baseUsername;
        }
        // Ensure minimum length of 4
        if (username.length() < 4) {
            username = "test" + username;
        }
        retailCustomer.setUsername(username);
        
        return retailCustomer;
    }

    /**
     * Creates a valid BatchListEntity for testing.
     */
    public static BatchListEntity createValidBatchList() {
        BatchListEntity batchList = new BatchListEntity();
        batchList.addResource("/espi/1_1/resource/UsagePoint/" + faker.number().numberBetween(1, 1000));
        return batchList;
    }

    /**
     * Creates a valid TimeConfigurationEntity for testing.
     */
    public static TimeConfigurationEntity createValidTimeConfiguration() {
        TimeConfigurationEntity timeConfig = new TimeConfigurationEntity();
        timeConfig.setDescription(faker.lorem().sentence(3, 6));
        timeConfig.setTzOffset(-28800L); // PST: UTC-8 hours in seconds
        timeConfig.setDstOffset(3600L); // 1 hour DST offset
        
        // Create DST start rule (example: second Sunday in March at 2:00 AM)
        byte[] dstStartRule = {0x02, 0x00, 0x03, 0x02}; // Month=3, Week=2, Day=0 (Sunday), Hour=2
        timeConfig.setDstStartRule(dstStartRule);
        
        // Create DST end rule (example: first Sunday in November at 2:00 AM)
        byte[] dstEndRule = {0x01, 0x00, 0x0B, 0x02}; // Month=11, Week=1, Day=0 (Sunday), Hour=2
        timeConfig.setDstEndRule(dstEndRule);
        
        return timeConfig;
    }

    /**
     * Creates a valid SubscriptionEntity for testing.
     * Note: Subscription is an application-specific entity (NOT an ESPI resource),
     * so it does not extend IdentifiedObject and requires UUID to be set before persisting.
     */
    public static SubscriptionEntity createValidSubscription() {
        SubscriptionEntity subscription = new SubscriptionEntity(UUID.randomUUID());
        subscription.setHashedId("hashed-" + faker.internet().uuid());
        return subscription;
    }

    /**
     * Creates a valid ApplicationInformationEntity for testing.
     */
    public static ApplicationInformationEntity createValidApplicationInformation() {
        ApplicationInformationEntity app = new ApplicationInformationEntity();
        app.setDescription(faker.lorem().sentence(3, 6));
        
        // Ensure clientId meets validation constraints (2-64 chars, @NotEmpty)
        String clientId = "test-client-" + faker.number().digits(8);
        if (clientId.length() > 64) {
            clientId = clientId.substring(0, 64);
        }
        app.setClientId(clientId);

        app.setClientSecret(faker.regexify("[a-zA-Z0-9]{16,32}"));

        // Ensure dataCustodianId meets validation constraints (2-64 chars if present)
        String dataCustodianId = "test-datacustodian-" + faker.number().digits(6);
        if (dataCustodianId.length() > 64) {
            dataCustodianId = dataCustodianId.substring(0, 64);
        }
        app.setDataCustodianId(dataCustodianId);
        
        java.util.Set<org.greenbuttonalliance.espi.common.domain.common.enums.GrantType> grantTypes = new java.util.HashSet<>();
        grantTypes.add(org.greenbuttonalliance.espi.common.domain.common.enums.GrantType.AUTHORIZATION_CODE);
        app.setGrantTypes(grantTypes);
        
        return app;
    }

    /**
     * Creates a valid AuthorizationEntity for testing.
     */
    public static AuthorizationEntity createValidAuthorization() {
        AuthorizationEntity auth = new AuthorizationEntity();
        auth.setDescription(faker.lorem().sentence(3, 6));
        auth.setAccessToken("test-token-" + faker.internet().uuid());
        auth.setStatus(AuthorizationEntity.STATUS_ACTIVE);
        return auth;
    }

    /**
     * Creates a list of valid entities for bulk testing.
     */
    public static <T> List<T> createValidEntities(int count, java.util.function.Supplier<T> entitySupplier) {
        List<T> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entities.add(entitySupplier.get());
        }
        return entities;
    }

    /**
     * Creates a random OffsetDateTime for testing.
     */
    public static OffsetDateTime randomOffsetDateTime() {
        return faker.timeAndDate().past(365, java.util.concurrent.TimeUnit.DAYS)
                .atOffset(java.time.ZoneOffset.UTC);
    }

    /**
     * Creates a random LocalDateTime for testing.
     */
    public static LocalDateTime randomLocalDateTime() {
        return faker.timeAndDate().past(365, java.util.concurrent.TimeUnit.DAYS)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Creates a valid CustomerAccountEntity for testing.
     */
    public static CustomerAccountEntity createValidCustomerAccount() {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setDescription(faker.lorem().sentence(4, 8));
        account.setType("Residential Account");
        account.setBillingCycle(String.valueOf(faker.number().numberBetween(1, 31)));
        account.setBudgetBill("Standard");
        account.setAccountId("ACCT-" + faker.number().digits(10));
        account.setIsPrePay(false);
        return account;
    }

    /**
     * Creates a valid ServiceLocationEntity for testing.
     */
    public static ServiceLocationEntity createValidServiceLocation() {
        ServiceLocationEntity location = new ServiceLocationEntity();
        location.setDescription(faker.lorem().sentence(4, 8));
        location.setType("Residential");
        location.setAccessMethod("Front door key in lockbox");
        location.setNeedsInspection(false);
        location.setOutageBlock("BLOCK-" + faker.number().digits(6));
        return location;
    }

    /**
     * Creates a valid CustomerAgreementEntity for testing.
     */
    public static CustomerAgreementEntity createValidCustomerAgreement() {
        CustomerAgreementEntity agreement = new CustomerAgreementEntity();
        agreement.setDescription(faker.lorem().sentence(4, 8));
        agreement.setType("Service Agreement");
        agreement.setSignDate(OffsetDateTime.now().minusDays(faker.number().numberBetween(1, 365)));
        agreement.setIsPrePay(false);
        agreement.setCurrency("USD");
        agreement.setAgreementId("AGR-" + faker.number().digits(10));
        return agreement;
    }

    /**
     * Creates a valid EndDeviceEntity for testing.
     */
    public static EndDeviceEntity createValidEndDevice() {
        EndDeviceEntity endDevice = new EndDeviceEntity();
        endDevice.setDescription(faker.lorem().sentence(4, 8));
        endDevice.setType("Electric Meter");
        endDevice.setSerialNumber("SN-" + faker.number().digits(12));
        endDevice.setUtcNumber("UTC-" + faker.number().digits(8));
        endDevice.setIsVirtual(false);
        endDevice.setIsPan(false);
        endDevice.setAmrSystem("AMR-" + faker.lorem().word());
        return endDevice;
    }

    /**
     * Creates a valid MeterEntity for testing.
     */
    public static MeterEntity createValidMeter() {
        MeterEntity meter = new MeterEntity();
        meter.setDescription(faker.lorem().sentence(4, 8));
        meter.setType("Electric Meter");
        meter.setSerialNumber("SN-" + faker.number().digits(12));
        meter.setUtcNumber("UTC-" + faker.number().digits(8));
        meter.setFormNumber("FORM-" + faker.number().numberBetween(1, 10));
        meter.setIntervalLength(900L); // 15 minutes default
        meter.setIsVirtual(false);
        meter.setIsPan(false);
        meter.setAmrSystem("AMR-" + faker.lorem().word());
        return meter;
    }
}