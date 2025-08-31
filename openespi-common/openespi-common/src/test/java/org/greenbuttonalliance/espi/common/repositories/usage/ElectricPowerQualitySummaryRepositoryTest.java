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
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ElectricPowerQualitySummaryRepository.
 * 
 * Tests all CRUD operations, 5 custom query methods, power quality metrics testing,
 * DateTimeInterval embedded object testing, and UsagePoint relationship testing.
 */
@DisplayName("ElectricPowerQualitySummary Repository Tests")
class ElectricPowerQualitySummaryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository;

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
        return usagePoint;
    }

    /**
     * Creates a valid DateTimeInterval for testing.
     */
    private DateTimeInterval createValidDateTimeInterval() {
        DateTimeInterval interval = new DateTimeInterval();
        interval.setStart(randomOffsetDateTime().toEpochSecond());
        interval.setDuration(3600L); // 1 hour
        return interval;
    }

    /**
     * Creates a valid ElectricPowerQualitySummaryEntity for testing.
     */
    private ElectricPowerQualitySummaryEntity createValidElectricPowerQualitySummary() {
        ElectricPowerQualitySummaryEntity summary = new ElectricPowerQualitySummaryEntity();
        summary.setDescription("Test Power Quality Summary - " + faker.lorem().sentence(3));
        
        // Set power quality measurements
        summary.setFlickerPlt(faker.number().numberBetween(0L, 100L));
        summary.setFlickerPst(faker.number().numberBetween(0L, 100L));
        summary.setHarmonicVoltage(faker.number().numberBetween(0L, 1000L));
        summary.setLongInterruptions(faker.number().numberBetween(0L, 10L));
        summary.setMainsVoltage(faker.number().numberBetween(110000L, 240000L)); // 110V to 240V in millivolts
        summary.setMeasurementProtocol((short) 1);
        summary.setPowerFrequency(faker.number().numberBetween(59000L, 61000L)); // 59-61 Hz in millihertz
        summary.setRapidVoltageChanges(faker.number().numberBetween(0L, 50L));
        summary.setShortInterruptions(faker.number().numberBetween(0L, 20L));
        summary.setSupplyVoltageDips(faker.number().numberBetween(0L, 15L));
        summary.setSupplyVoltageImbalance(faker.number().numberBetween(0L, 500L)); // 0-5% in hundredths
        summary.setSupplyVoltageVariations(faker.number().numberBetween(0L, 1000L));
        summary.setTempOvervoltage(faker.number().numberBetween(0L, 5L));
        
        // Set summary interval
        summary.setSummaryInterval(createValidDateTimeInterval());
        
        return summary;
    }

    /**
     * Creates a complete test setup with RetailCustomer, UsagePoint, and ElectricPowerQualitySummary.
     */
    private ElectricPowerQualitySummaryEntity createCompleteTestSetup() {
        RetailCustomerEntity customer = createValidRetailCustomer();
        RetailCustomerEntity savedCustomer = persistAndFlush(customer);

        UsagePointEntity usagePoint = createValidUsagePoint();
        usagePoint.setRetailCustomer(savedCustomer);
        UsagePointEntity savedUsagePoint = persistAndFlush(usagePoint);

        ElectricPowerQualitySummaryEntity summary = createValidElectricPowerQualitySummary();
        summary.setUsagePoint(savedUsagePoint);
        
        return summary;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve electric power quality summary successfully")
        void shouldSaveAndRetrieveElectricPowerQualitySummarySuccessfully() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            summary.setDescription("Test Power Quality Summary for CRUD");

            // Act
            ElectricPowerQualitySummaryEntity saved = electricPowerQualitySummaryRepository.save(summary);
            flushAndClear();
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Power Quality Summary for CRUD");
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
            assertThat(retrieved.get().getSummaryInterval()).isNotNull();
        }

        @Test
        @DisplayName("Should update electric power quality summary successfully")
        void shouldUpdateElectricPowerQualitySummarySuccessfully() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            // Act
            saved.setDescription("Updated Power Quality Summary Description");
            saved.setLongInterruptions(5L);
            saved.setShortInterruptions(10L);
            ElectricPowerQualitySummaryEntity updated = electricPowerQualitySummaryRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Updated Power Quality Summary Description");
            assertThat(retrieved.get().getLongInterruptions()).isEqualTo(5L);
            assertThat(retrieved.get().getShortInterruptions()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should delete electric power quality summary successfully")
        void shouldDeleteElectricPowerQualitySummarySuccessfully() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);
            UUID savedId = saved.getId();

            // Act
            electricPowerQualitySummaryRepository.deleteById(savedId);
            flushAndClear();

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(savedId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should find all electric power quality summaries")
        void shouldFindAllElectricPowerQualitySummaries() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary1 = createCompleteTestSetup();
            summary1.setDescription("First Power Quality Summary");
            ElectricPowerQualitySummaryEntity summary2 = createCompleteTestSetup();
            summary2.setDescription("Second Power Quality Summary");
            
            persistAndFlush(summary1);
            persistAndFlush(summary2);

            // Act
            List<ElectricPowerQualitySummaryEntity> allSummaries = electricPowerQualitySummaryRepository.findAll();

            // Assert
            assertThat(allSummaries).hasSizeGreaterThanOrEqualTo(2);
            assertThat(allSummaries)
                .extracting(ElectricPowerQualitySummaryEntity::getDescription)
                .contains("First Power Quality Summary", "Second Power Quality Summary");
        }

        @Test
        @DisplayName("Should count electric power quality summaries correctly")
        void shouldCountElectricPowerQualitySummariesCorrectly() {
            // Arrange
            long initialCount = electricPowerQualitySummaryRepository.count();
            ElectricPowerQualitySummaryEntity summary1 = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity summary2 = createCompleteTestSetup();
            
            persistAndFlush(summary1);
            persistAndFlush(summary2);

            // Act
            long finalCount = electricPowerQualitySummaryRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find summaries by usage point")
        void shouldFindSummariesByUsagePoint() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary1 = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity summary2 = createCompleteTestSetup();
            summary2.setUsagePoint(summary1.getUsagePoint()); // Same usage point
            
            persistAndFlush(summary1);
            persistAndFlush(summary2);

            // Act
            List<ElectricPowerQualitySummaryEntity> summaries = electricPowerQualitySummaryRepository.findByUsagePoint(summary1.getUsagePoint());

            // Assert
            assertThat(summaries).hasSize(2);
            assertThat(summaries).extracting(ElectricPowerQualitySummaryEntity::getUsagePoint)
                .allMatch(up -> up.getId().equals(summary1.getUsagePoint().getId()));
        }

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary1 = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity summary2 = createCompleteTestSetup();
            
            ElectricPowerQualitySummaryEntity saved1 = persistAndFlush(summary1);
            ElectricPowerQualitySummaryEntity saved2 = persistAndFlush(summary2);

            // Act
            List<UUID> allIds = electricPowerQualitySummaryRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all IDs by usage point ID")
        void shouldFindAllIdsByUsagePointId() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary1 = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity summary2 = createCompleteTestSetup();
            summary2.setUsagePoint(summary1.getUsagePoint()); // Same usage point
            
            ElectricPowerQualitySummaryEntity saved1 = persistAndFlush(summary1);
            ElectricPowerQualitySummaryEntity saved2 = persistAndFlush(summary2);

            // Act
            List<UUID> ids = electricPowerQualitySummaryRepository.findAllIdsByUsagePointId(summary1.getUsagePoint().getId());

            // Assert
            assertThat(ids).hasSize(2);
            assertThat(ids).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all IDs by xpath2 (retail customer and usage point)")
        void shouldFindAllIdsByXpath2() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            UUID retailCustomerId = summary.getUsagePoint().getRetailCustomer().getId();
            UUID usagePointId = summary.getUsagePoint().getId();

            // Act
            List<UUID> ids = electricPowerQualitySummaryRepository.findAllIdsByXpath2(retailCustomerId, usagePointId);

            // Assert
            assertThat(ids).contains(saved.getId());
        }

        @Test
        @DisplayName("Should find ID by xpath (retail customer, usage point, and summary ID)")
        void shouldFindIdByXpath() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            UUID retailCustomerId = summary.getUsagePoint().getRetailCustomer().getId();
            UUID usagePointId = summary.getUsagePoint().getId();
            UUID summaryId = saved.getId();

            // Act
            Optional<UUID> foundId = electricPowerQualitySummaryRepository.findIdByXpath(retailCustomerId, usagePointId, summaryId);

            // Assert
            assertThat(foundId).isPresent();
            assertThat(foundId.get()).isEqualTo(summaryId);
        }

        @Test
        @DisplayName("Should return empty when xpath parameters don't match")
        void shouldReturnEmptyWhenXpathParametersDontMatch() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            persistAndFlush(summary);

            UUID nonExistentCustomerId = UUID.randomUUID();
            UUID usagePointId = summary.getUsagePoint().getId();
            UUID summaryId = summary.getId();

            // Act
            Optional<UUID> foundId = electricPowerQualitySummaryRepository.findIdByXpath(nonExistentCustomerId, usagePointId, summaryId);

            // Assert
            assertThat(foundId).isEmpty();
        }
    }

    @Nested
    @DisplayName("DateTimeInterval Embedded Object Testing")
    class DateTimeIntervalTest {

        @Test
        @DisplayName("Should persist and retrieve DateTimeInterval correctly")
        void shouldPersistAndRetrieveDateTimeIntervalCorrectly() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            DateTimeInterval interval = new DateTimeInterval();
            interval.setStart(OffsetDateTime.now().toEpochSecond());
            interval.setDuration(7200L); // 2 hours
            summary.setSummaryInterval(interval);

            // Act
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getSummaryInterval()).isNotNull();
            assertThat(retrieved.get().getSummaryInterval().getStart()).isEqualTo(interval.getStart());
            assertThat(retrieved.get().getSummaryInterval().getDuration()).isEqualTo(7200L);
        }

        @Test
        @DisplayName("Should handle null DateTimeInterval")
        void shouldHandleNullDateTimeInterval() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            summary.setSummaryInterval(null);

            // Act
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getSummaryInterval()).isNull();
        }
    }

    @Nested
    @DisplayName("UsagePoint Relationship Testing")
    class UsagePointRelationshipTest {

        @Test
        @DisplayName("Should maintain UsagePoint relationship")
        void shouldMaintainUsagePointRelationship() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();

            // Act
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(summary.getUsagePoint().getId());
        }

        @Test
        @DisplayName("Should handle lazy loading of UsagePoint")
        void shouldHandleLazyLoadingOfUsagePoint() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            // Act - Clear persistence context to test lazy loading
            flushAndClear();
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            // Access the usage point to trigger lazy loading
            assertThat(retrieved.get().getUsagePoint().getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should allow null UsagePoint")
        void shouldAllowNullUsagePoint() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createValidElectricPowerQualitySummary();
            summary.setUsagePoint(null);

            // Act
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsagePoint()).isNull();
        }
    }

    @Nested
    @DisplayName("Power Quality Metrics Testing")
    class PowerQualityMetricsTest {

        @Test
        @DisplayName("Should persist all power quality measurements")
        void shouldPersistAllPowerQualityMeasurements() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            summary.setFlickerPlt(95L);
            summary.setFlickerPst(85L);
            summary.setHarmonicVoltage(500L);
            summary.setLongInterruptions(2L);
            summary.setMainsVoltage(120000L); // 120V in millivolts
            summary.setPowerFrequency(60000L); // 60Hz in millihertz
            summary.setShortInterruptions(5L);
            summary.setSupplyVoltageImbalance(200L); // 2% in hundredths

            // Act
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            ElectricPowerQualitySummaryEntity entity = retrieved.get();
            assertThat(entity.getFlickerPlt()).isEqualTo(95L);
            assertThat(entity.getFlickerPst()).isEqualTo(85L);
            assertThat(entity.getHarmonicVoltage()).isEqualTo(500L);
            assertThat(entity.getLongInterruptions()).isEqualTo(2L);
            assertThat(entity.getMainsVoltage()).isEqualTo(120000L);
            assertThat(entity.getPowerFrequency()).isEqualTo(60000L);
            assertThat(entity.getShortInterruptions()).isEqualTo(5L);
            assertThat(entity.getSupplyVoltageImbalance()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Should handle null power quality measurements")
        void shouldHandleNullPowerQualityMeasurements() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            summary.setFlickerPlt(null);
            summary.setFlickerPst(null);
            summary.setHarmonicVoltage(null);
            summary.setLongInterruptions(null);

            // Act
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            ElectricPowerQualitySummaryEntity entity = retrieved.get();
            assertThat(entity.getFlickerPlt()).isNull();
            assertThat(entity.getFlickerPst()).isNull();
            assertThat(entity.getHarmonicVoltage()).isNull();
            assertThat(entity.getLongInterruptions()).isNull();
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();

            // Act
            ElectricPowerQualitySummaryEntity saved = electricPowerQualitySummaryRepository.save(summary);
            flushAndClear();

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            
            ElectricPowerQualitySummaryEntity entity = retrieved.get();
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getCreated()).isNotNull();
            assertThat(entity.getUpdated()).isNotNull();
            assertThat(entity.getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should update timestamps on modification")
        void shouldUpdateTimestampsOnModification() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity saved = persistAndFlush(summary);
            
            // Wait a moment to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            saved.setDescription("Updated Description");
            ElectricPowerQualitySummaryEntity updated = electricPowerQualitySummaryRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUpdated()).isAfter(retrieved.get().getCreated());
        }

        @Test
        @DisplayName("Should generate unique IDs for different entities")
        void shouldGenerateUniqueIdsForDifferentEntities() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary1 = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity summary2 = createCompleteTestSetup();

            // Act
            ElectricPowerQualitySummaryEntity saved1 = electricPowerQualitySummaryRepository.save(summary1);
            ElectricPowerQualitySummaryEntity saved2 = electricPowerQualitySummaryRepository.save(summary2);
            flushAndClear();

            // Assert
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
            assertThat(saved1.getId()).isNotNull();
            assertThat(saved2.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            ElectricPowerQualitySummaryEntity summary1 = createCompleteTestSetup();
            ElectricPowerQualitySummaryEntity summary2 = createCompleteTestSetup();
            
            ElectricPowerQualitySummaryEntity saved1 = persistAndFlush(summary1);
            ElectricPowerQualitySummaryEntity saved2 = persistAndFlush(summary2);

            // Act & Assert
            assertThat(saved1).isNotEqualTo(saved2);
            assertThat(saved1.hashCode()).isNotEqualTo(saved2.hashCode());
            
            // Same entity should be equal to itself
            assertThat(saved1).isEqualTo(saved1);
            assertThat(saved1.hashCode()).isEqualTo(saved1.hashCode());
        }
    }
}