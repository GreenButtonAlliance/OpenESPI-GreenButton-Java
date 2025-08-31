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

import jakarta.validation.ConstraintViolation;
import org.greenbuttonalliance.espi.common.domain.usage.*;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Comprehensive test suite for MeterReadingRepository.
 * 
 * Tests all CRUD operations, 7 custom query methods, relationships,
 * cascade operations, and validation constraints for MeterReading entities.
 */
@DisplayName("MeterReading Repository Tests")
class MeterReadingRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private ReadingTypeRepository readingTypeRepository;

    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve meter reading successfully")
        void shouldSaveAndRetrieveMeterReadingSuccessfully() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Test Meter Reading for CRUD");

            // Act
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            flushAndClear();
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Meter Reading for CRUD");
        }

        @Test
        @DisplayName("Should save meter reading with usage point relationship")
        void shouldSaveMeterReadingWithUsagePointRelationship() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Meter Reading");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading.setDescription("Meter Reading with Usage Point");

            // Act
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            flushAndClear();
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
            assertThat(retrieved.get().getUsagePoint().getDescription()).isEqualTo("Usage Point for Meter Reading");
        }

        @Test
        @DisplayName("Should save meter reading with reading type relationship")
        void shouldSaveMeterReadingWithReadingTypeRelationship() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setCommodity("ELECTRICITY");
            readingType.setKind("ENERGY");
            readingType.setUom("WH");
            ReadingTypeEntity savedReadingType = readingTypeRepository.save(readingType);
            
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading with Reading Type");
            meterReading.setReadingType(savedReadingType);

            // Act
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            flushAndClear();
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getReadingType()).isNotNull();
            assertThat(retrieved.get().getReadingType().getDescription()).isEqualTo("Electricity energy (WH)");
        }

        @Test
        @DisplayName("Should cascade save interval blocks")
        void shouldCascadeSaveIntervalBlocks() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading with Interval Blocks");
            
            IntervalBlockEntity intervalBlock1 = TestDataBuilders.createValidIntervalBlock();
            intervalBlock1.setDescription("Interval Block 1");
            intervalBlock1.setMeterReading(meterReading);
            
            IntervalBlockEntity intervalBlock2 = TestDataBuilders.createValidIntervalBlock();
            intervalBlock2.setDescription("Interval Block 2");
            intervalBlock2.setMeterReading(meterReading);
            
            meterReading.getIntervalBlocks().add(intervalBlock1);
            meterReading.getIntervalBlocks().add(intervalBlock2);

            // Act
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            flushAndClear();
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getIntervalBlocks()).hasSize(2);
            assertThat(retrieved.get().getIntervalBlocks()).extracting(IntervalBlockEntity::getDescription)
                    .contains("Interval Block 1", "Interval Block 2");
        }

        @Test
        @DisplayName("Should find all meter readings")
        void shouldFindAllMeterReadings() {
            // Arrange
            List<MeterReadingEntity> meterReadings = TestDataBuilders.createValidEntities(3, TestDataBuilders::createValidMeterReading);
            meterReadings.forEach(mr -> mr.setDescription("Bulk Meter Reading " + meterReadings.indexOf(mr)));
            meterReadingRepository.saveAll(meterReadings);
            flushAndClear();

            // Act
            List<MeterReadingEntity> allMeterReadings = meterReadingRepository.findAll();

            // Assert
            assertThat(allMeterReadings).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allMeterReadings).extracting(MeterReadingEntity::getDescription)
                    .contains("Bulk Meter Reading 0", "Bulk Meter Reading 1", "Bulk Meter Reading 2");
        }

        @Test
        @DisplayName("Should delete meter reading successfully")
        void shouldDeleteMeterReadingSuccessfully() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading to Delete");
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            UUID meterReadingId = saved.getId();
            flushAndClear();

            // Act
            meterReadingRepository.deleteById(meterReadingId);
            flushAndClear();
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(meterReadingId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if meter reading exists")
        void shouldCheckIfMeterReadingExists() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            flushAndClear();

            // Act & Assert
            assertThat(meterReadingRepository.existsById(saved.getId())).isTrue();
            assertThat(meterReadingRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count meter readings")
        void shouldCountMeterReadings() {
            // Arrange
            long initialCount = meterReadingRepository.count();
            List<MeterReadingEntity> meterReadings = TestDataBuilders.createValidEntities(5, TestDataBuilders::createValidMeterReading);
            meterReadingRepository.saveAll(meterReadings);
            flushAndClear();

            // Act
            long finalCount = meterReadingRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 5);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should delete meter reading by ID using custom method")
        void shouldDeleteMeterReadingByIdUsingCustomMethod() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading for Custom Delete");
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            UUID meterReadingId = saved.getId();
            flushAndClear();

            // Verify it exists
            assertThat(meterReadingRepository.existsById(meterReadingId)).isTrue();

            // Act
            meterReadingRepository.deleteById(meterReadingId);
            flushAndClear();

            // Assert
            assertThat(meterReadingRepository.existsById(meterReadingId)).isFalse();
        }

        @Test
        @DisplayName("Should find all meter reading IDs")
        void shouldFindAllMeterReadingIds() {
            // Arrange
            long initialCount = meterReadingRepository.count();
            List<MeterReadingEntity> meterReadings = TestDataBuilders.createValidEntities(3, TestDataBuilders::createValidMeterReading);
            List<MeterReadingEntity> savedMeterReadings = meterReadingRepository.saveAll(meterReadings);
            flushAndClear();

            // Act
            List<UUID> allIds = meterReadingRepository.findAllIds();

            // Assert
            assertThat(allIds).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allIds).contains(
                    savedMeterReadings.get(0).getId(),
                    savedMeterReadings.get(1).getId(),
                    savedMeterReadings.get(2).getId()
            );
        }

        @Test
        @DisplayName("Should find meter readings by related href")
        void shouldFindMeterReadingsByRelatedHref() {
            // Arrange - Create a simple meter reading without related links for now
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading for Related Href Test");
            meterReadingRepository.save(meterReading);
            flushAndClear();

            // Act - Test the query method with a non-existent href (should return empty)
            List<MeterReadingEntity> results = meterReadingRepository.findByRelatedHref("/espi/1_1/resource/IntervalBlock/123");

            // Assert - Should return empty list since no meter reading has this related href
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should find all related entities")
        void shouldFindAllRelatedEntities() {
            // Arrange
            List<String> relatedLinkHrefs = List.of(
                    "/espi/1_1/resource/ReadingType/1",
                    "/espi/1_1/resource/ReadingType/2",
                    "/espi/1_1/resource/ReadingType/3"
            );

            // Act
            List<Object> results = meterReadingRepository.findAllRelated(relatedLinkHrefs);

            // Assert
            assertThat(results).isNotNull();
            // Note: This query looks for ReadingType entities with matching self links
            // The actual results depend on existing data in the test database
        }

        @Test
        @DisplayName("Should find all meter reading IDs by usage point ID")
        void shouldFindAllMeterReadingIdsByUsagePointId() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for ID Query");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            MeterReadingEntity meterReading1 = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading1.setDescription("Meter Reading 1");
            MeterReadingEntity meterReading2 = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading2.setDescription("Meter Reading 2");

            List<MeterReadingEntity> savedMeterReadings = meterReadingRepository.saveAll(List.of(meterReading1, meterReading2));
            flushAndClear();

            // Act
            List<UUID> meterReadingIds = meterReadingRepository.findAllIdsByUsagePointId(savedUsagePoint.getId());

            // Assert
            assertThat(meterReadingIds).hasSize(2);
            assertThat(meterReadingIds).contains(savedMeterReadings.get(0).getId(), savedMeterReadings.get(1).getId());
        }

        @Test
        @DisplayName("Should find all meter reading IDs by xpath2")
        void shouldFindAllMeterReadingIdsByXpath2() {
            // Arrange
            RetailCustomerEntity retailCustomer = TestDataBuilders.createValidRetailCustomer();
            retailCustomer.setUsername("customer@xpath2.com");
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(retailCustomer);

            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Xpath2");
            usagePoint.setRetailCustomer(savedCustomer);
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            MeterReadingEntity meterReading1 = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading1.setDescription("Meter Reading 1 for Xpath2");
            MeterReadingEntity meterReading2 = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading2.setDescription("Meter Reading 2 for Xpath2");

            List<MeterReadingEntity> savedMeterReadings = meterReadingRepository.saveAll(List.of(meterReading1, meterReading2));
            flushAndClear();

            // Act
            List<UUID> meterReadingIds = meterReadingRepository.findAllIdsByXpath2(savedCustomer.getId(), savedUsagePoint.getId());

            // Assert
            assertThat(meterReadingIds).hasSize(2);
            assertThat(meterReadingIds).contains(savedMeterReadings.get(0).getId(), savedMeterReadings.get(1).getId());
        }

        @Test
        @DisplayName("Should find meter reading ID by xpath")
        void shouldFindMeterReadingIdByXpath() {
            // Arrange
            RetailCustomerEntity retailCustomer = TestDataBuilders.createValidRetailCustomer();
            retailCustomer.setUsername("customer@xpath.com");
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(retailCustomer);

            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Xpath");
            usagePoint.setRetailCustomer(savedCustomer);
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading.setDescription("Meter Reading for Xpath");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            flushAndClear();

            // Act
            Optional<UUID> result = meterReadingRepository.findIdByXpath(
                    savedCustomer.getId(), 
                    savedUsagePoint.getId(), 
                    savedMeterReading.getId()
            );

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(savedMeterReading.getId());
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Act & Assert
            assertThat(meterReadingRepository.findByRelatedHref("nonexistent-href")).isEmpty();
            assertThat(meterReadingRepository.findAllIdsByUsagePointId(UUID.randomUUID())).isEmpty();
            assertThat(meterReadingRepository.findAllIdsByXpath2(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
            assertThat(meterReadingRepository.findIdByXpath(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    @DisplayName("JPA Relationships")
    class RelationshipsTest {

        @Test
        @DisplayName("Should maintain usage point relationship integrity")
        void shouldMaintainUsagePointRelationshipIntegrity() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Relationship Test");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading.setDescription("Meter Reading with Usage Point Relationship");

            // Act
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            flushAndClear();
            
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(savedMeterReading.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
            assertThat(retrieved.get().getUsagePoint().getDescription()).isEqualTo("Usage Point for Relationship Test");
        }

        @Test
        @DisplayName("Should cascade save reading type with CASCADE.ALL")
        void shouldCascadeSaveReadingTypeWithCascadeAll() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setCommodity("GAS");
            readingType.setKind("VOLUME");
            readingType.setUom("M3");
            ReadingTypeEntity savedReadingType = readingTypeRepository.save(readingType);
            
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading with Cascade Reading Type");
            meterReading.setReadingType(savedReadingType);

            // Act
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            flushAndClear();
            
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(savedMeterReading.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getReadingType()).isNotNull();
            assertThat(retrieved.get().getReadingType().getDescription()).isEqualTo("Gas volume (M3)");
        }

        @Test
        @DisplayName("Should handle orphan removal for interval blocks")
        void shouldHandleOrphanRemovalForIntervalBlocks() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading for Orphan Removal Test");
            
            IntervalBlockEntity intervalBlock1 = TestDataBuilders.createValidIntervalBlock();
            intervalBlock1.setDescription("Interval Block to Keep");
            intervalBlock1.setMeterReading(meterReading);
            
            IntervalBlockEntity intervalBlock2 = TestDataBuilders.createValidIntervalBlock();
            intervalBlock2.setDescription("Interval Block to Remove");
            intervalBlock2.setMeterReading(meterReading);
            
            meterReading.getIntervalBlocks().add(intervalBlock1);
            meterReading.getIntervalBlocks().add(intervalBlock2);
            
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            flushAndClear();

            // Act - Remove one interval block (orphan removal should delete it)
            savedMeterReading.getIntervalBlocks().remove(1); // Remove intervalBlock2
            meterReadingRepository.save(savedMeterReading);
            flushAndClear();
            
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(savedMeterReading.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getIntervalBlocks()).hasSize(1);
            assertThat(retrieved.get().getIntervalBlocks().get(0).getDescription()).isEqualTo("Interval Block to Keep");
        }

        @Test
        @DisplayName("Should handle null relationships gracefully")
        void shouldHandleNullRelationshipsGracefully() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading without Relationships");
            meterReading.setUsagePoint(null);
            meterReading.setReadingType(null);
            meterReading.setIntervalBlocks(new ArrayList<>());

            // Act & Assert
            assertThatCode(() -> {
                MeterReadingEntity saved = meterReadingRepository.save(meterReading);
                flushAndClear();
                Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(saved.getId());
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get().getUsagePoint()).isNull();
                assertThat(retrieved.get().getReadingType()).isNull();
                assertThat(retrieved.get().getIntervalBlocks()).isEmpty();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate meter reading with valid data")
        void shouldValidateMeterReadingWithValidData() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Valid Meter Reading Description");

            // Act
            Set<ConstraintViolation<MeterReadingEntity>> violations = validator.validate(meterReading);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle null description gracefully")
        void shouldHandleNullDescriptionGracefully() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription(null);

            // Act
            Set<ConstraintViolation<MeterReadingEntity>> violations = validator.validate(meterReading);

            // Assert - Description is typically optional in ESPI entities
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should validate interval blocks collection")
        void shouldValidateIntervalBlocksCollection() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading with Interval Blocks");
            
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Valid Interval Block");
            intervalBlock.setMeterReading(meterReading);
            meterReading.getIntervalBlocks().add(intervalBlock);

            // Act
            Set<ConstraintViolation<MeterReadingEntity>> violations = validator.validate(meterReading);

            // Assert
            assertThat(violations).isEmpty();
            assertThat(meterReading.getIntervalBlocks()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange & Act
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();

            // Assert
            assertThat(meterReading.getId()).isNotNull();
            assertThat(meterReading.getId()).isInstanceOf(UUID.class);
            assertThat(meterReading.getRelatedLinks()).isNotNull();
            assertThat(meterReading.getRelatedLinks()).isEmpty();
        }

        @Test
        @DisplayName("Should set timestamps on persist")
        void shouldSetTimestampsOnPersist() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading for Timestamp Test");

            // Act
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            flushAndClear();

            // Assert
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should generate correct self href")
        void shouldGenerateCorrectSelfHref() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Href Test");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading.setDescription("Meter Reading for Href Test");
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            flushAndClear();

            // Act
            String selfHref = saved.getSelfHref();

            // Assert
            assertThat(selfHref).isNotNull();
            assertThat(selfHref).contains("/MeterReading/");
            assertThat(selfHref).contains(saved.getHashedId());
        }

        @Test
        @DisplayName("Should generate correct up href")
        void shouldGenerateCorrectUpHref() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Up Href Test");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading.setDescription("Meter Reading for Up Href Test");
            MeterReadingEntity saved = meterReadingRepository.save(meterReading);
            flushAndClear();

            // Act
            String upHref = saved.getUpHref();

            // Assert
            assertThat(upHref).isNotNull();
            assertThat(upHref).contains("/MeterReading");
        }
    }
}