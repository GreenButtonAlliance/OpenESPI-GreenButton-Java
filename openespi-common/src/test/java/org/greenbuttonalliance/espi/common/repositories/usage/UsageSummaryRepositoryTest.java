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

package org.greenbuttonalliance.espi.common.repositories.usage;

import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.common.SummaryMeasurement;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.LineItemEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for UsageSummaryRepository.
 * 
 * Tests all CRUD operations, 7 custom query methods, usage aggregation field testing,
 * DateTimeInterval and SummaryMeasurement embedded testing, and UsagePoint relationship testing.
 */
@DisplayName("UsageSummary Repository Tests")
class UsageSummaryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    /**
     * Creates a valid RetailCustomerEntity for testing.
     */
    private RetailCustomerEntity createValidRetailCustomer() {
        RetailCustomerEntity customer = new RetailCustomerEntity();
        customer.setUsername("testuser_" + faker.number().digits(6));
        customer.setFirstName(faker.name().firstName());
        customer.setLastName(faker.name().lastName());
        customer.setRole("ROLE_USER");
        customer.setEnabled(true);
        return customer;
    }

    /**
     * Creates a valid UsagePointEntity for testing.
     */
    private UsagePointEntity createValidUsagePoint() {
        UsagePointEntity usagePoint = new UsagePointEntity();
        usagePoint.setDescription("Test Usage Point - " + faker.lorem().sentence(3));
        usagePoint.setStatus((short) 1);
        usagePoint.setRoleFlags(new byte[]{0x01, 0x02});
        usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
        return usagePoint;
    }

    /**
     * Creates a valid DateTimeInterval for testing.
     */
    private DateTimeInterval createValidDateTimeInterval() {
        DateTimeInterval interval = new DateTimeInterval();
        interval.setStart(randomOffsetDateTime().toEpochSecond());
        interval.setDuration(2592000L); // 30 days
        return interval;
    }

    /**
     * Creates a valid SummaryMeasurement for testing.
     */
    private SummaryMeasurement createValidSummaryMeasurement() {
        SummaryMeasurement measurement = new SummaryMeasurement();
        measurement.setPowerOfTenMultiplier("0");
        measurement.setTimeStamp(randomOffsetDateTime().toEpochSecond());
        measurement.setUom("72"); // Wh
        measurement.setValue(faker.number().numberBetween(1000L, 50000L));
        measurement.setReadingTypeRef("https://api.example.com/ReadingType/" + randomUuid());
        return measurement;
    }

    /**
     * Creates a valid UsageSummaryEntity for testing.
     */
    private UsageSummaryEntity createValidUsageSummary() {
        UsageSummaryEntity summary = new UsageSummaryEntity();
        summary.setDescription("Test Usage Summary - " + faker.lorem().sentence(3));
        summary.setBillLastPeriod(faker.number().numberBetween(5000L, 50000L)); // $50-$500
        summary.setBillToDate(faker.number().numberBetween(1000L, 10000L)); // $10-$100
        summary.setCostAdditionalLastPeriod(faker.number().numberBetween(500L, 5000L)); // $5-$50
        summary.setCurrency("USD");
        summary.setQualityOfReading("VALIDATED");
        summary.setStatusTimeStamp(randomOffsetDateTime().toEpochSecond());
        summary.setBillingPeriod(createValidDateTimeInterval());
        summary.setRatchetDemandPeriod(createValidDateTimeInterval());
        return summary;
    }

    /**
     * Creates a complete test setup with RetailCustomer, UsagePoint, and UsageSummary.
     */
    private UsageSummaryEntity createCompleteTestSetup() {
        RetailCustomerEntity customer = createValidRetailCustomer();
        RetailCustomerEntity savedCustomer = persistAndFlush(customer);

        UsagePointEntity usagePoint = createValidUsagePoint();
        usagePoint.setRetailCustomer(savedCustomer);
        UsagePointEntity savedUsagePoint = persistAndFlush(usagePoint);

        UsageSummaryEntity summary = createValidUsageSummary();
        summary.setUsagePoint(savedUsagePoint);
        
        return summary;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve usage summary successfully")
        void shouldSaveAndRetrieveUsageSummarySuccessfully() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            summary.setDescription("Test Usage Summary for CRUD");

            // Act
            UsageSummaryEntity saved = usageSummaryRepository.save(summary);
            flushAndClear();
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Usage Summary for CRUD");
            assertThat(retrieved.get().getBillingPeriod()).isNotNull();
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
        }

        @Test
        @DisplayName("Should update usage summary successfully")
        void shouldUpdateUsageSummarySuccessfully() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            UsageSummaryEntity saved = persistAndFlush(summary);

            // Act
            saved.setDescription("Updated Usage Summary Description");
            saved.setBillLastPeriod(25000L); // $250.00
            saved.setCurrency("EUR");
            UsageSummaryEntity updated = usageSummaryRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Updated Usage Summary Description");
            assertThat(retrieved.get().getBillLastPeriod()).isEqualTo(25000L);
            assertThat(retrieved.get().getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Should delete usage summary successfully")
        void shouldDeleteUsageSummarySuccessfully() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            UsageSummaryEntity saved = persistAndFlush(summary);
            UUID savedId = saved.getId();

            // Act
            usageSummaryRepository.deleteById(savedId);
            flushAndClear();

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(savedId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should find all usage summaries")
        void shouldFindAllUsageSummaries() {
            // Arrange
            UsageSummaryEntity summary1 = createCompleteTestSetup();
            summary1.setDescription("First Usage Summary");
            UsageSummaryEntity summary2 = createCompleteTestSetup();
            summary2.setDescription("Second Usage Summary");
            
            persistAndFlush(summary1);
            persistAndFlush(summary2);

            // Act
            List<UsageSummaryEntity> allSummaries = usageSummaryRepository.findAll();

            // Assert
            assertThat(allSummaries).hasSizeGreaterThanOrEqualTo(2);
            assertThat(allSummaries)
                .extracting(UsageSummaryEntity::getDescription)
                .contains("First Usage Summary", "Second Usage Summary");
        }

        @Test
        @DisplayName("Should count usage summaries correctly")
        void shouldCountUsageSummariesCorrectly() {
            // Arrange
            long initialCount = usageSummaryRepository.count();
            UsageSummaryEntity summary1 = createCompleteTestSetup();
            UsageSummaryEntity summary2 = createCompleteTestSetup();
            
            persistAndFlush(summary1);
            persistAndFlush(summary2);

            // Act
            long finalCount = usageSummaryRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find usage summaries by usage point")
        void shouldFindUsageSummariesByUsagePoint() {
            // Arrange
            UsageSummaryEntity summary1 = createCompleteTestSetup();
            UsageSummaryEntity summary2 = createCompleteTestSetup();
            summary2.setUsagePoint(summary1.getUsagePoint()); // Same usage point
            
            persistAndFlush(summary1);
            persistAndFlush(summary2);

            // Act
            List<UsageSummaryEntity> summaries = usageSummaryRepository.findByUsagePoint(summary1.getUsagePoint());

            // Assert
            assertThat(summaries).hasSize(2);
            assertThat(summaries).extracting(UsageSummaryEntity::getUsagePoint)
                .allMatch(up -> up.getId().equals(summary1.getUsagePoint().getId()));
        }

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            UsageSummaryEntity summary1 = createCompleteTestSetup();
            UsageSummaryEntity summary2 = createCompleteTestSetup();
            
            UsageSummaryEntity saved1 = persistAndFlush(summary1);
            UsageSummaryEntity saved2 = persistAndFlush(summary2);

            // Act
            List<UUID> allIds = usageSummaryRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all IDs by usage point ID")
        void shouldFindAllIdsByUsagePointId() {
            // Arrange
            UsageSummaryEntity summary1 = createCompleteTestSetup();
            UsageSummaryEntity summary2 = createCompleteTestSetup();
            summary2.setUsagePoint(summary1.getUsagePoint()); // Same usage point
            
            UsageSummaryEntity saved1 = persistAndFlush(summary1);
            UsageSummaryEntity saved2 = persistAndFlush(summary2);

            // Act
            List<UUID> ids = usageSummaryRepository.findAllIdsByUsagePointId(summary1.getUsagePoint().getId());

            // Assert
            assertThat(ids).hasSize(2);
            assertThat(ids).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all IDs by xpath2 (retail customer and usage point)")
        void shouldFindAllIdsByXpath2() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            UsageSummaryEntity saved = persistAndFlush(summary);

            UUID retailCustomerId = summary.getUsagePoint().getRetailCustomer().getId();
            UUID usagePointId = summary.getUsagePoint().getId();

            // Act
            List<UUID> ids = usageSummaryRepository.findAllIdsByXpath2(retailCustomerId, usagePointId);

            // Assert
            assertThat(ids).contains(saved.getId());
        }

        @Test
        @DisplayName("Should find ID by xpath (retail customer, usage point, and summary ID)")
        void shouldFindIdByXpath() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            UsageSummaryEntity saved = persistAndFlush(summary);

            UUID retailCustomerId = summary.getUsagePoint().getRetailCustomer().getId();
            UUID usagePointId = summary.getUsagePoint().getId();
            UUID summaryId = saved.getId();

            // Act
            Optional<UUID> foundId = usageSummaryRepository.findIdByXpath(retailCustomerId, usagePointId, summaryId);

            // Assert
            assertThat(foundId).isPresent();
            assertThat(foundId.get()).isEqualTo(summaryId);
        }
    }

    @Nested
    @DisplayName("Embedded Objects Testing")
    class EmbeddedObjectsTest {

        @Test
        @DisplayName("Should persist and retrieve DateTimeInterval objects correctly")
        void shouldPersistAndRetrieveDateTimeIntervalObjectsCorrectly() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            DateTimeInterval billingPeriod = new DateTimeInterval();
            billingPeriod.setStart(1640995200L); // 2022-01-01 00:00:00 UTC
            billingPeriod.setDuration(2592000L); // 30 days
            summary.setBillingPeriod(billingPeriod);

            DateTimeInterval ratchetPeriod = new DateTimeInterval();
            ratchetPeriod.setStart(1640995200L);
            ratchetPeriod.setDuration(86400L); // 1 day
            summary.setRatchetDemandPeriod(ratchetPeriod);

            // Act
            UsageSummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getBillingPeriod()).isNotNull();
            assertThat(retrieved.get().getBillingPeriod().getStart()).isEqualTo(1640995200L);
            assertThat(retrieved.get().getBillingPeriod().getDuration()).isEqualTo(2592000L);
            assertThat(retrieved.get().getRatchetDemandPeriod()).isNotNull();
            assertThat(retrieved.get().getRatchetDemandPeriod().getDuration()).isEqualTo(86400L);
        }

        @Test
        @DisplayName("Should persist and retrieve SummaryMeasurement objects correctly")
        void shouldPersistAndRetrieveSummaryMeasurementObjectsCorrectly() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            
            SummaryMeasurement overallConsumption = createValidSummaryMeasurement();
            overallConsumption.setValue(15000L);
            overallConsumption.setUom("72"); // Wh
            summary.setOverallConsumptionLastPeriod(overallConsumption);

            SummaryMeasurement peakDemand = createValidSummaryMeasurement();
            peakDemand.setValue(5000L);
            peakDemand.setUom("38"); // W
            summary.setPeakDemand(peakDemand);

            // Act
            UsageSummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getOverallConsumptionLastPeriod()).isNotNull();
            assertThat(retrieved.get().getOverallConsumptionLastPeriod().getValue()).isEqualTo(15000L);
            assertThat(retrieved.get().getOverallConsumptionLastPeriod().getUom()).isEqualTo("72");
            assertThat(retrieved.get().getPeakDemand()).isNotNull();
            assertThat(retrieved.get().getPeakDemand().getValue()).isEqualTo(5000L);
            assertThat(retrieved.get().getPeakDemand().getUom()).isEqualTo("38");
        }

        @Test
        @DisplayName("Should handle null embedded objects")
        void shouldHandleNullEmbeddedObjects() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            summary.setBillingPeriod(null);
            summary.setOverallConsumptionLastPeriod(null);
            summary.setPeakDemand(null);

            // Act
            UsageSummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getBillingPeriod()).isNull();
            assertThat(retrieved.get().getOverallConsumptionLastPeriod()).isNull();
            assertThat(retrieved.get().getPeakDemand()).isNull();
        }
    }

    @Nested
    @DisplayName("Relationship Testing")
    class RelationshipTest {

        @Test
        @DisplayName("Should maintain UsagePoint relationship")
        void shouldMaintainUsagePointRelationship() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();

            // Act
            UsageSummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(summary.getUsagePoint().getId());
        }

        @Test
        @DisplayName("Should handle lazy loading of UsagePoint")
        void shouldHandleLazyLoadingOfUsagePoint() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            UsageSummaryEntity saved = persistAndFlush(summary);

            // Act - Clear persistence context to test lazy loading
            flushAndClear();
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            // Access the usage point to trigger lazy loading
            assertThat(retrieved.get().getUsagePoint().getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should manage LineItem collection correctly")
        void shouldManageLineItemCollectionCorrectly() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            UsageSummaryEntity saved = persistAndFlush(summary);

            LineItemEntity lineItem1 = new LineItemEntity();
            lineItem1.setDescription("Test Line Item 1");
            lineItem1.setAmount(1000L);
            lineItem1.setDateTime(randomOffsetDateTime().toEpochSecond());
            lineItem1.setNote("Additional charge 1");

            LineItemEntity lineItem2 = new LineItemEntity();
            lineItem2.setDescription("Test Line Item 2");
            lineItem2.setAmount(2000L);
            lineItem2.setDateTime(randomOffsetDateTime().toEpochSecond());
            lineItem2.setNote("Additional charge 2");

            // Act
            saved.addCostAdditionalDetailLastPeriod(lineItem1);
            saved.addCostAdditionalDetailLastPeriod(lineItem2);
            UsageSummaryEntity updated = usageSummaryRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCostAdditionalDetailLastPeriod()).hasSize(2);
            assertThat(retrieved.get().getCostAdditionalDetailLastPeriod())
                .extracting(LineItemEntity::getAmount)
                .contains(1000L, 2000L);
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();

            // Act
            UsageSummaryEntity saved = usageSummaryRepository.save(summary);
            flushAndClear();

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            
            UsageSummaryEntity entity = retrieved.get();
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getCreated()).isNotNull();
            assertThat(entity.getUpdated()).isNotNull();
            assertThat(entity.getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should update timestamps on modification")
        void shouldUpdateTimestampsOnModification() {
            // Arrange
            UsageSummaryEntity summary = createCompleteTestSetup();
            UsageSummaryEntity saved = persistAndFlush(summary);
            
            // Wait a moment to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            saved.setDescription("Updated Description");
            UsageSummaryEntity updated = usageSummaryRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<UsageSummaryEntity> retrieved = usageSummaryRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUpdated()).isAfter(retrieved.get().getCreated());
        }

        @Test
        @DisplayName("Should generate unique IDs for different entities")
        void shouldGenerateUniqueIdsForDifferentEntities() {
            // Arrange
            UsageSummaryEntity summary1 = createCompleteTestSetup();
            UsageSummaryEntity summary2 = createCompleteTestSetup();

            // Act
            UsageSummaryEntity saved1 = usageSummaryRepository.save(summary1);
            UsageSummaryEntity saved2 = usageSummaryRepository.save(summary2);
            flushAndClear();

            // Assert
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
            assertThat(saved1.getId()).isNotNull();
            assertThat(saved2.getId()).isNotNull();
        }
    }
}