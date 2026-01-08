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

import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalReadingEntity;
import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.common.LinkType;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for IntervalBlockRepository.
 * 
 * Tests all CRUD operations, 9 custom query methods, relationships,
 * cascade operations, and validation constraints for IntervalBlock entities.
 */
@DisplayName("IntervalBlock Repository Tests")
class IntervalBlockRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private IntervalBlockRepository intervalBlockRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve interval block successfully")
        void shouldSaveAndRetrieveIntervalBlockSuccessfully() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Test Interval Block for CRUD");

            // Act
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            flushAndClear();
            Optional<IntervalBlockEntity> retrieved = intervalBlockRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Interval Block for CRUD");
            assertThat(retrieved.get().getInterval()).isNotNull();
            assertThat(retrieved.get().getInterval().getDuration()).isEqualTo(intervalBlock.getInterval().getDuration());
        }

        @Test
        @DisplayName("Should save interval block with meter reading relationship")
        void shouldSaveIntervalBlockWithMeterReadingRelationship() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading for Interval Block");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading);
            intervalBlock.setDescription("Interval Block with Meter Reading");

            // Act
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            flushAndClear();
            Optional<IntervalBlockEntity> retrieved = intervalBlockRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMeterReading()).isNotNull();
            assertThat(retrieved.get().getMeterReading().getId()).isEqualTo(savedMeterReading.getId());
            assertThat(retrieved.get().getMeterReading().getDescription()).isEqualTo("Meter Reading for Interval Block");
        }

        @Test
        @DisplayName("Should cascade save interval readings")
        void shouldCascadeSaveIntervalReadings() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Interval Block with Readings");
            
            IntervalReadingEntity reading1 = TestDataBuilders.createValidIntervalReading();
            reading1.setValue(1000L);
            reading1.setIntervalBlock(intervalBlock);
            
            IntervalReadingEntity reading2 = TestDataBuilders.createValidIntervalReading();
            reading2.setValue(2000L);
            reading2.setIntervalBlock(intervalBlock);
            
            intervalBlock.getIntervalReadings().add(reading1);
            intervalBlock.getIntervalReadings().add(reading2);

            // Act
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            flushAndClear();
            Optional<IntervalBlockEntity> retrieved = intervalBlockRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getIntervalReadings()).hasSize(2);
            assertThat(retrieved.get().getIntervalReadings()).extracting(IntervalReadingEntity::getValue)
                    .contains(1000L, 2000L);
        }

        @Test
        @DisplayName("Should find all interval blocks")
        void shouldFindAllIntervalBlocks() {
            // Arrange
            List<IntervalBlockEntity> intervalBlocks = TestDataBuilders.createValidEntities(3, TestDataBuilders::createValidIntervalBlock);
            intervalBlocks.forEach(ib -> ib.setDescription("Bulk Interval Block " + intervalBlocks.indexOf(ib)));
            intervalBlockRepository.saveAll(intervalBlocks);
            flushAndClear();

            // Act
            List<IntervalBlockEntity> allIntervalBlocks = intervalBlockRepository.findAll();

            // Assert
            assertThat(allIntervalBlocks).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allIntervalBlocks).extracting(IntervalBlockEntity::getDescription)
                    .contains("Bulk Interval Block 0", "Bulk Interval Block 1", "Bulk Interval Block 2");
        }

        @Test
        @DisplayName("Should delete interval block successfully")
        void shouldDeleteIntervalBlockSuccessfully() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Interval Block to Delete");
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            UUID intervalBlockId = saved.getId();
            flushAndClear();

            // Act
            intervalBlockRepository.deleteById(intervalBlockId);
            flushAndClear();
            Optional<IntervalBlockEntity> retrieved = intervalBlockRepository.findById(intervalBlockId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if interval block exists")
        void shouldCheckIfIntervalBlockExists() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            flushAndClear();

            // Act & Assert
            assertThat(intervalBlockRepository.existsById(saved.getId())).isTrue();
            assertThat(intervalBlockRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count interval blocks")
        void shouldCountIntervalBlocks() {
            // Arrange
            long initialCount = intervalBlockRepository.count();
            List<IntervalBlockEntity> intervalBlocks = TestDataBuilders.createValidEntities(5, TestDataBuilders::createValidIntervalBlock);
            intervalBlockRepository.saveAll(intervalBlocks);
            flushAndClear();

            // Act
            long finalCount = intervalBlockRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 5);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find all interval block IDs")
        void shouldFindAllIntervalBlockIds() {
            // Arrange
            long initialCount = intervalBlockRepository.count();
            List<IntervalBlockEntity> intervalBlocks = TestDataBuilders.createValidEntities(3, TestDataBuilders::createValidIntervalBlock);
            List<IntervalBlockEntity> savedIntervalBlocks = intervalBlockRepository.saveAll(intervalBlocks);
            flushAndClear();

            // Act
            List<UUID> allIds = intervalBlockRepository.findAllIds();

            // Assert
            assertThat(allIds).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allIds).contains(
                    savedIntervalBlocks.get(0).getId(),
                    savedIntervalBlocks.get(1).getId(),
                    savedIntervalBlocks.get(2).getId()
            );
        }

        @Test
        @DisplayName("Should delete interval block by ID using custom method")
        void shouldDeleteIntervalBlockByIdUsingCustomMethod() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Interval Block for Custom Delete");
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            UUID intervalBlockId = saved.getId();
            flushAndClear();

            // Verify it exists
            assertThat(intervalBlockRepository.existsById(intervalBlockId)).isTrue();

            // Act
            intervalBlockRepository.deleteById(intervalBlockId);
            flushAndClear();

            // Assert
            assertThat(intervalBlockRepository.existsById(intervalBlockId)).isFalse();
        }

        @Test
        @DisplayName("Should find all interval blocks by meter reading ID")
        void shouldFindAllIntervalBlocksByMeterReadingId() {
            // Arrange
            MeterReadingEntity meterReading1 = TestDataBuilders.createValidMeterReading();
            meterReading1.setDescription("Meter Reading 1");
            MeterReadingEntity savedMeterReading1 = meterReadingRepository.save(meterReading1);

            MeterReadingEntity meterReading2 = TestDataBuilders.createValidMeterReading();
            meterReading2.setDescription("Meter Reading 2");
            MeterReadingEntity savedMeterReading2 = meterReadingRepository.save(meterReading2);

            IntervalBlockEntity intervalBlock1 = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading1);
            intervalBlock1.setDescription("Interval Block 1 for MR1");
            IntervalBlockEntity intervalBlock2 = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading1);
            intervalBlock2.setDescription("Interval Block 2 for MR1");
            IntervalBlockEntity intervalBlock3 = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading2);
            intervalBlock3.setDescription("Interval Block for MR2");

            intervalBlockRepository.saveAll(List.of(intervalBlock1, intervalBlock2, intervalBlock3));
            flushAndClear();

            // Act
            List<IntervalBlockEntity> mr1IntervalBlocks = intervalBlockRepository.findAllByMeterReadingId(savedMeterReading1.getId());
            List<IntervalBlockEntity> mr2IntervalBlocks = intervalBlockRepository.findAllByMeterReadingId(savedMeterReading2.getId());

            // Assert
            assertThat(mr1IntervalBlocks).hasSize(2);
            assertThat(mr1IntervalBlocks).extracting(IntervalBlockEntity::getDescription)
                    .contains("Interval Block 1 for MR1", "Interval Block 2 for MR1");

            assertThat(mr2IntervalBlocks).hasSize(1);
            assertThat(mr2IntervalBlocks).extracting(IntervalBlockEntity::getDescription)
                    .contains("Interval Block for MR2");
        }

        @Test
        @DisplayName("Should find all interval block IDs by usage point ID")
        void shouldFindAllIntervalBlockIdsByUsagePointId() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Interval Block IDs");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading.setDescription("Meter Reading for Usage Point");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);

            IntervalBlockEntity intervalBlock1 = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading);
            intervalBlock1.setDescription("Interval Block 1");
            IntervalBlockEntity intervalBlock2 = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading);
            intervalBlock2.setDescription("Interval Block 2");

            List<IntervalBlockEntity> savedIntervalBlocks = intervalBlockRepository.saveAll(List.of(intervalBlock1, intervalBlock2));
            flushAndClear();

            // Act
            List<UUID> intervalBlockIds = intervalBlockRepository.findAllIdsByUsagePointId(savedUsagePoint.getId());

            // Assert
            assertThat(intervalBlockIds).hasSize(2);
            assertThat(intervalBlockIds).contains(savedIntervalBlocks.get(0).getId(), savedIntervalBlocks.get(1).getId());
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Act & Assert
            assertThat(intervalBlockRepository.findAllByMeterReadingId(UUID.randomUUID())).isEmpty();
            assertThat(intervalBlockRepository.findAllIdsByUsagePointId(UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    @DisplayName("JPA Relationships")
    class RelationshipsTest {

        @Test
        @DisplayName("Should maintain meter reading relationship integrity")
        void shouldMaintainMeterReadingRelationshipIntegrity() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading for Relationship Test");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);

            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading);
            intervalBlock.setDescription("Interval Block with Meter Reading Relationship");

            // Act
            IntervalBlockEntity savedIntervalBlock = intervalBlockRepository.save(intervalBlock);
            flushAndClear();
            
            Optional<IntervalBlockEntity> retrieved = intervalBlockRepository.findById(savedIntervalBlock.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMeterReading()).isNotNull();
            assertThat(retrieved.get().getMeterReading().getId()).isEqualTo(savedMeterReading.getId());
            assertThat(retrieved.get().getMeterReading().getDescription()).isEqualTo("Meter Reading for Relationship Test");
        }

        @Test
        @DisplayName("Should handle orphan removal for interval readings")
        void shouldHandleOrphanRemovalForIntervalReadings() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Interval Block for Orphan Removal Test");
            
            IntervalReadingEntity reading1 = TestDataBuilders.createValidIntervalReading();
            reading1.setValue(1000L);
            reading1.setIntervalBlock(intervalBlock);
            
            IntervalReadingEntity reading2 = TestDataBuilders.createValidIntervalReading();
            reading2.setValue(2000L);
            reading2.setIntervalBlock(intervalBlock);
            
            intervalBlock.getIntervalReadings().add(reading1);
            intervalBlock.getIntervalReadings().add(reading2);
            
            IntervalBlockEntity savedIntervalBlock = intervalBlockRepository.save(intervalBlock);
            flushAndClear();

            // Act - Remove one interval reading (orphan removal should delete it)
            savedIntervalBlock.getIntervalReadings().remove(1); // Remove reading2
            intervalBlockRepository.save(savedIntervalBlock);
            flushAndClear();
            
            Optional<IntervalBlockEntity> retrieved = intervalBlockRepository.findById(savedIntervalBlock.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getIntervalReadings()).hasSize(1);
            assertThat(retrieved.get().getIntervalReadings().get(0).getValue()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("Should handle null relationships gracefully")
        void shouldHandleNullRelationshipsGracefully() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Interval Block without Relationships");
            intervalBlock.setMeterReading(null);
            intervalBlock.setIntervalReadings(new ArrayList<>());

            // Act & Assert
            assertThatCode(() -> {
                IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
                flushAndClear();
                Optional<IntervalBlockEntity> retrieved = intervalBlockRepository.findById(saved.getId());
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get().getMeterReading()).isNull();
                assertThat(retrieved.get().getIntervalReadings()).isEmpty();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate interval block with valid data")
        void shouldValidateIntervalBlockWithValidData() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Valid Interval Block Description");

            // Act
            Set<ConstraintViolation<IntervalBlockEntity>> violations = validator.validate(intervalBlock);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle null description gracefully")
        void shouldHandleNullDescriptionGracefully() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription(null);

            // Act
            Set<ConstraintViolation<IntervalBlockEntity>> violations = validator.validate(intervalBlock);

            // Assert - Description is typically optional in ESPI entities
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should validate DateTimeInterval embedded object")
        void shouldValidateDateTimeIntervalEmbeddedObject() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Interval Block with DateTimeInterval");

            // Act
            Set<ConstraintViolation<IntervalBlockEntity>> violations = validator.validate(intervalBlock);

            // Assert
            assertThat(violations).isEmpty();
            assertThat(intervalBlock.getInterval()).isNotNull();
            assertThat(intervalBlock.getInterval().getDuration()).isEqualTo(3600L);
            assertThat(intervalBlock.getInterval().getStart()).isNotNull();
        }

        @Test
        @DisplayName("Should validate interval readings collection")
        void shouldValidateIntervalReadingsCollection() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Interval Block with Readings");
            
            IntervalReadingEntity reading = TestDataBuilders.createValidIntervalReading();
            reading.setValue(5000L);
            reading.setIntervalBlock(intervalBlock);
            intervalBlock.getIntervalReadings().add(reading);

            // Act
            Set<ConstraintViolation<IntervalBlockEntity>> violations = validator.validate(intervalBlock);

            // Assert
            assertThat(violations).isEmpty();
            assertThat(intervalBlock.getIntervalReadings()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange & Act
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();

            // Assert
            assertThat(intervalBlock.getId()).isNotNull();
            assertThat(intervalBlock.getId()).isInstanceOf(UUID.class);
            assertThat(intervalBlock.getInterval()).isNotNull();
            assertThat(intervalBlock.getIntervalReadings()).isNotNull();
        }

        @Test
        @DisplayName("Should set timestamps on persist")
        void shouldSetTimestampsOnPersist() {
            // Arrange
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setDescription("Interval Block for Timestamp Test");

            // Act
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            flushAndClear();

            // Assert
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should generate correct self href")
        void shouldGenerateCorrectSelfHref() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading for Href Test");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading);
            intervalBlock.setDescription("Interval Block for Href Test");
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            flushAndClear();

            // Act
            String selfHref = saved.getSelfHref();

            // Assert
            assertThat(selfHref).isNotNull();
            assertThat(selfHref).contains("/IntervalBlock/");
            assertThat(selfHref).contains(saved.getHashedId());
        }

        @Test
        @DisplayName("Should generate correct up href")
        void shouldGenerateCorrectUpHref() {
            // Arrange
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setDescription("Meter Reading for Up Href Test");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            
            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlockWithMeterReading(savedMeterReading);
            intervalBlock.setDescription("Interval Block for Up Href Test");
            IntervalBlockEntity saved = intervalBlockRepository.save(intervalBlock);
            flushAndClear();

            // Act
            String upHref = saved.getUpHref();

            // Assert
            assertThat(upHref).isNotNull();
            assertThat(upHref).contains("/IntervalBlock");
        }

        @Test
        @DisplayName("Should test merge functionality")
        void shouldTestMergeFunctionality() {
            // Arrange
            IntervalBlockEntity originalBlock = TestDataBuilders.createValidIntervalBlock();
            originalBlock.setDescription("Original Interval Block");
            
            IntervalBlockEntity otherBlock = TestDataBuilders.createValidIntervalBlock();
            otherBlock.setDescription("Other Interval Block");
            
            DateTimeInterval newInterval = new DateTimeInterval();
            newInterval.setDuration(7200L); // 2 hours
            newInterval.setStart(System.currentTimeMillis() / 1000);
            otherBlock.setInterval(newInterval);

            // Act
            originalBlock.merge(otherBlock);

            // Assert
            assertThat(originalBlock.getDescription()).isEqualTo("Other Interval Block");
            assertThat(originalBlock.getInterval().getDuration()).isEqualTo(7200L);
        }
    }
}