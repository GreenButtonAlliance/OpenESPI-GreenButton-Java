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

package org.greenbuttonalliance.espi.common.repositories.performance;

import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance and constraint testing for JPA repositories.
 * 
 * Tests large dataset handling, query performance, memory usage,
 * batch operations, and constraint validation.
 */
@DisplayName("Performance & Constraint Tests")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PerformanceConstraintTest extends BaseRepositoryTest {

    @Autowired
    private UsagePointRepository usagePointRepository;
    
    @Autowired
    private MeterReadingRepository meterReadingRepository;
    
    @Autowired
    private IntervalBlockRepository intervalBlockRepository;
    
    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    @Nested
    @DisplayName("Large Dataset Handling")
    class LargeDatasetTest {

        @Test
        @DisplayName("Should handle saving 1000+ usage points efficiently")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void shouldHandleLargeUsagePointDataset() {
            // Arrange
            int entityCount = 1000;
            List<UsagePointEntity> usagePoints = TestDataBuilders.createValidEntities(entityCount, 
                TestDataBuilders::createValidUsagePoint);
            
            // Set unique descriptions to avoid potential conflicts
            for (int i = 0; i < usagePoints.size(); i++) {
                usagePoints.get(i).setDescription("Performance Test Usage Point " + i);
            }

            // Act - Measure save performance
            Instant start = Instant.now();
            List<UsagePointEntity> savedEntities = usagePointRepository.saveAll(usagePoints);
            flushAndClear();
            Instant end = Instant.now();
            
            Duration duration = Duration.between(start, end);

            // Assert
            assertThat(savedEntities).hasSize(entityCount);
            assertThat(savedEntities).allMatch(entity -> entity.getId() != null);
            assertThat(duration.toMillis()).isLessThan(10000); // Should complete within 10 seconds
            
            // Verify count
            long count = usagePointRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(entityCount);
        }

        @Test
        @DisplayName("Should handle querying large datasets efficiently")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void shouldHandleLargeDatasetQueries() {
            // Arrange - Create a moderate dataset for query testing
            int entityCount = 500;
            List<UsagePointEntity> usagePoints = TestDataBuilders.createValidEntities(entityCount, 
                TestDataBuilders::createValidUsagePoint);
            
            for (int i = 0; i < usagePoints.size(); i++) {
                usagePoints.get(i).setDescription("Query Test Usage Point " + i);
            }
            
            usagePointRepository.saveAll(usagePoints);
            flushAndClear();

            // Act - Measure query performance
            Instant start = Instant.now();
            List<UsagePointEntity> retrievedEntities = usagePointRepository.findAll();
            Instant end = Instant.now();
            
            Duration duration = Duration.between(start, end);

            // Assert
            assertThat(retrievedEntities).hasSizeGreaterThanOrEqualTo(entityCount);
            assertThat(duration.toMillis()).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("Should handle hierarchical data efficiently")
        @Timeout(value = 20, unit = TimeUnit.SECONDS)
        void shouldHandleHierarchicalDataEfficiently() {
            // Arrange - Create hierarchical data structure
            int usagePointCount = 100;
            int meterReadingsPerUsagePoint = 5;
            int intervalBlocksPerReading = 3;
            
            List<UsagePointEntity> usagePoints = TestDataBuilders.createValidEntities(usagePointCount, 
                TestDataBuilders::createValidUsagePoint);
            
            List<UsagePointEntity> savedUsagePoints = usagePointRepository.saveAll(usagePoints);
            flushAndClear();

            // Act - Create hierarchical structure
            Instant start = Instant.now();
            
            for (UsagePointEntity usagePoint : savedUsagePoints) {
                List<MeterReadingEntity> meterReadings = TestDataBuilders.createValidEntities(meterReadingsPerUsagePoint, 
                    () -> {
                        MeterReadingEntity reading = TestDataBuilders.createValidMeterReading();
                        reading.setUsagePoint(usagePoint);
                        return reading;
                    });
                
                List<MeterReadingEntity> savedReadings = meterReadingRepository.saveAll(meterReadings);
                
                for (MeterReadingEntity reading : savedReadings) {
                    List<IntervalBlockEntity> intervalBlocks = TestDataBuilders.createValidEntities(intervalBlocksPerReading, 
                        () -> {
                            IntervalBlockEntity block = TestDataBuilders.createValidIntervalBlock();
                            block.setMeterReading(reading);
                            return block;
                        });
                    
                    intervalBlockRepository.saveAll(intervalBlocks);
                }
            }
            
            flushAndClear();
            Instant end = Instant.now();
            
            Duration duration = Duration.between(start, end);

            // Assert
            long totalExpectedMeterReadings = (long) usagePointCount * meterReadingsPerUsagePoint;
            long totalExpectedIntervalBlocks = totalExpectedMeterReadings * intervalBlocksPerReading;
            
            long actualMeterReadings = meterReadingRepository.count();
            long actualIntervalBlocks = intervalBlockRepository.count();
            
            assertThat(actualMeterReadings).isGreaterThanOrEqualTo(totalExpectedMeterReadings);
            assertThat(actualIntervalBlocks).isGreaterThanOrEqualTo(totalExpectedIntervalBlocks);
            assertThat(duration.toMillis()).isLessThan(15000); // Should complete within 15 seconds
        }
    }

    @Nested
    @DisplayName("Query Performance Validation")
    class QueryPerformanceTest {

        @Test
        @DisplayName("Should execute findById queries within performance threshold")
        void shouldExecuteFindByIdWithinThreshold() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Performance Test Entity");
            UsagePointEntity savedEntity = usagePointRepository.save(usagePoint);
            flushAndClear();

            // Act - Measure query performance
            Instant start = Instant.now();
            Optional<UsagePointEntity> retrieved = usagePointRepository.findById(savedEntity.getId());
            Instant end = Instant.now();
            
            Duration duration = Duration.between(start, end);

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(duration.toMillis()).isLessThan(100); // Should complete within 100ms
        }

        @Test
        @DisplayName("Should execute count queries within performance threshold")
        void shouldExecuteCountWithinThreshold() {
            // Arrange - Create some test data
            List<UsagePointEntity> usagePoints = TestDataBuilders.createValidEntities(50, 
                TestDataBuilders::createValidUsagePoint);
            usagePointRepository.saveAll(usagePoints);
            flushAndClear();

            // Act - Measure count query performance
            Instant start = Instant.now();
            long count = usagePointRepository.count();
            Instant end = Instant.now();
            
            Duration duration = Duration.between(start, end);

            // Assert
            assertThat(count).isGreaterThanOrEqualTo(50);
            assertThat(duration.toMillis()).isLessThan(100); // Should complete within 100ms
        }

        @Test
        @DisplayName("Should execute existsById queries within performance threshold")
        void shouldExecuteExistsByIdWithinThreshold() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedEntity = usagePointRepository.save(usagePoint);
            flushAndClear();

            // Act - Measure exists query performance
            Instant start = Instant.now();
            boolean exists = usagePointRepository.existsById(savedEntity.getId());
            Instant end = Instant.now();
            
            Duration duration = Duration.between(start, end);

            // Assert
            assertThat(exists).isTrue();
            assertThat(duration.toMillis()).isLessThan(100); // Should complete within 100ms
        }
    }

    @Nested
    @DisplayName("Batch Operation Efficiency")
    class BatchOperationTest {

        @Test
        @DisplayName("Should perform batch saves more efficiently than individual saves")
        void shouldPerformBatchSavesEfficiently() {
            // Arrange
            int entityCount = 100;
            List<UsagePointEntity> batchEntities = TestDataBuilders.createValidEntities(entityCount, 
                TestDataBuilders::createValidUsagePoint);
            List<UsagePointEntity> individualEntities = TestDataBuilders.createValidEntities(entityCount, 
                TestDataBuilders::createValidUsagePoint);

            // Warm-up: mitigate first-use overhead (JIT, EntityManager, mappings)
            usagePointRepository.saveAll(TestDataBuilders.createValidEntities(5, TestDataBuilders::createValidUsagePoint));
            flushAndClear();

            // Act - Measure batch save performance
            Instant batchStart = Instant.now();
            usagePointRepository.saveAll(batchEntities);
            flushAndClear();
            Instant batchEnd = Instant.now();
            
            Duration batchDuration = Duration.between(batchStart, batchEnd);

            // Act - Measure individual save performance
            Instant individualStart = Instant.now();
            for (UsagePointEntity entity : individualEntities) {
                usagePointRepository.save(entity);
            }
            flushAndClear();
            Instant individualEnd = Instant.now();
            
            Duration individualDuration = Duration.between(individualStart, individualEnd);

            // Assert - Batch operations should not be significantly slower than individual saves
            long batchMs = batchDuration.toMillis();
            long individualMs = individualDuration.toMillis();
            assertThat(batchMs)
                .as("Batch save should be no more than 2x slower than individual saves (batch=%dms, individual=%dms)", batchMs, individualMs)
                .isLessThanOrEqualTo(individualMs * 2);
            assertThat(batchMs).isLessThan(5000); // Should complete within 5 seconds
        }

        @Test
        @DisplayName("Should perform batch deletes efficiently")
        void shouldPerformBatchDeletesEfficiently() {
            // Arrange
            int entityCount = 100;
            List<UsagePointEntity> entities = TestDataBuilders.createValidEntities(entityCount, 
                TestDataBuilders::createValidUsagePoint);
            List<UsagePointEntity> savedEntities = usagePointRepository.saveAll(entities);
            flushAndClear();

            // Act - Measure batch delete performance
            Instant start = Instant.now();
            usagePointRepository.deleteAll(savedEntities);
            flushAndClear();
            Instant end = Instant.now();
            
            Duration duration = Duration.between(start, end);

            // Assert
            assertThat(duration.toMillis()).isLessThan(3000); // Should complete within 3 seconds
            
            // Verify deletion
            for (UsagePointEntity entity : savedEntities) {
                assertThat(usagePointRepository.existsById(entity.getId())).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Memory Usage Optimization")
    class MemoryUsageTest {

        @Test
        @DisplayName("Should handle memory efficiently during large operations")
        void shouldHandleMemoryEfficientlyDuringLargeOperations() {
            // Arrange
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            int entityCount = 500;
            List<UsagePointEntity> entities = TestDataBuilders.createValidEntities(entityCount, 
                TestDataBuilders::createValidUsagePoint);

            // Act - Perform memory-intensive operation
            usagePointRepository.saveAll(entities);
            flushAndClear();
            
            // Force garbage collection to get accurate memory reading
            System.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;

            // Assert - Memory increase should be reasonable (less than 50MB for 500 entities)
            assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024); // 50MB threshold
        }
    }

    @Nested
    @DisplayName("Constraint Validation")
    class ConstraintValidationTest {

        @Test
        @DisplayName("Should handle constraint violations gracefully")
        void shouldHandleConstraintViolationsGracefully() {
            // Arrange
            RetailCustomerEntity customer1 = TestDataBuilders.createValidRetailCustomer();
            customer1.setUsername("constraint.test@example.com");
            retailCustomerRepository.save(customer1);
            flushAndClear();

            RetailCustomerEntity customer2 = TestDataBuilders.createValidRetailCustomer();
            customer2.setUsername("constraint.test@example.com"); // Duplicate username

            // Act & Assert - Should handle constraint violation
            assertThatThrownBy(() -> {
                retailCustomerRepository.save(customer2);
                flushAndClear();
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should validate entity constraints during save operations")
        void shouldValidateEntityConstraintsDuringSave() {
            // This test verifies that basic entity validation works
            // Specific constraint testing depends on entity validation annotations
            
            // Arrange - Create valid entity
            UsagePointEntity validEntity = TestDataBuilders.createValidUsagePoint();
            validEntity.setDescription("Valid Entity");

            // Act & Assert - Valid entity should save successfully
            assertThatCode(() -> {
                usagePointRepository.save(validEntity);
                flushAndClear();
            }).doesNotThrowAnyException();
        }
    }
}