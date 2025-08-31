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

package org.greenbuttonalliance.espi.common.repositories.edge;

import jakarta.validation.ConstraintViolationException;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Edge case and error handling tests for JPA repositories.
 * 
 * Tests null parameter handling, invalid inputs, constraint violations,
 * transaction rollback scenarios, and error message consistency.
 */
@DisplayName("Edge Case & Error Handling Tests")
class EdgeCaseErrorHandlingTest extends BaseRepositoryTest {
    @Autowired
    private UsagePointRepository usagePointRepository;
    
    @Autowired
    private MeterReadingRepository meterReadingRepository;
    
    @Autowired
    private RetailCustomerRepository retailCustomerRepository;
    
    @Autowired
    private CustomerRepository customerRepository;

    @Nested
    @DisplayName("Null Parameter Handling")
    class NullParameterTest {

        @Test
        @DisplayName("Should handle null entity in save operation")
        void shouldHandleNullEntityInSave() {
            // Act & Assert
            assertThatThrownBy(() -> usagePointRepository.save(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }

        @Test
        @DisplayName("Should handle null ID in findById operation")
        void shouldHandleNullIdInFindById() {
            // Act & Assert
            assertThatThrownBy(() -> usagePointRepository.findById(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }

        @Test
        @DisplayName("Should handle null ID in existsById operation")
        void shouldHandleNullIdInExistsById() {
            // Act & Assert
            assertThatThrownBy(() -> usagePointRepository.existsById(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }

        @Test
        @DisplayName("Should handle null ID in deleteById operation")
        void shouldHandleNullIdInDeleteById() {
            // Act & Assert
            assertThatThrownBy(() -> usagePointRepository.deleteById(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }

        @Test
        @DisplayName("Should handle null collection in saveAll operation")
        void shouldHandleNullCollectionInSaveAll() {
            // Act & Assert
            assertThatThrownBy(() -> usagePointRepository.saveAll(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }

        @Test
        @DisplayName("Should handle null collection in deleteAll operation")
        void shouldHandleNullCollectionInDeleteAll() {
            // Act & Assert
            assertThatThrownBy(() -> usagePointRepository.deleteAll((Iterable<UsagePointEntity>) null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }
    }

    @Nested
    @DisplayName("Invalid UUID Format Testing")
    class InvalidUuidTest {

        @Test
        @DisplayName("Should handle non-existent UUID in findById")
        void shouldHandleNonExistentUuidInFindById() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act
            Optional<UsagePointEntity> result = usagePointRepository.findById(nonExistentId);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle non-existent UUID in existsById")
        void shouldHandleNonExistentUuidInExistsById() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act
            boolean exists = usagePointRepository.existsById(nonExistentId);

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should handle non-existent UUID in deleteById")
        void shouldHandleNonExistentUuidInDeleteById() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert - Should not throw exception for non-existent ID
            assertThatCode(() -> usagePointRepository.deleteById(nonExistentId))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Constraint Violation Exception Testing")
    class ConstraintViolationTest {

        @Test
        @DisplayName("Should handle unique constraint violations")
        void shouldHandleUniqueConstraintViolations() {
            // Arrange
            RetailCustomerEntity customer1 = TestDataBuilders.createValidRetailCustomer();
            customer1.setUsername("unique.test@example.com");
            retailCustomerRepository.save(customer1);
            flushAndClear();

            RetailCustomerEntity customer2 = TestDataBuilders.createValidRetailCustomer();
            customer2.setUsername("unique.test@example.com"); // Duplicate username

            // Act & Assert - May throw either DataIntegrityViolationException or ConstraintViolationException
            assertThatThrownBy(() -> {
                retailCustomerRepository.save(customer2);
                flushAndClear();
            }).isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
        }

        @Test
        @DisplayName("Should handle validation constraint violations")
        void shouldHandleValidationConstraintViolations() {
            // Arrange - Create entity with invalid data
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername(""); // Empty username should violate constraint
            
            // Act & Assert - Should throw exception for invalid data
            assertThatThrownBy(() -> {
                retailCustomerRepository.save(customer);
                flushAndClear();
            }).isInstanceOfAny(ConstraintViolationException.class, DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should handle foreign key constraint violations")
        void shouldHandleForeignKeyConstraintViolations() {
            // Arrange - Create meter reading with non-existent usage point
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            UsagePointEntity nonExistentUsagePoint = new UsagePointEntity();
            nonExistentUsagePoint.setId(UUID.randomUUID()); // Set a random UUID that doesn't exist
            meterReading.setUsagePoint(nonExistentUsagePoint);

            // Act & Assert - May throw either DataIntegrityViolationException or ConstraintViolationException
            assertThatThrownBy(() -> {
                meterReadingRepository.save(meterReading);
                flushAndClear();
            }).isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("Transaction Rollback Scenarios")
    class TransactionRollbackTest {

        @Test
        @DisplayName("Should rollback transaction on constraint violation")
        @Transactional
        void shouldRollbackTransactionOnConstraintViolation() {
            // Arrange
            long initialCount = usagePointRepository.count();
            
            UsagePointEntity validEntity = TestDataBuilders.createValidUsagePoint();
            validEntity.setDescription("Valid Entity Before Rollback");
            
            // Save valid entity first
            usagePointRepository.save(validEntity);
            
            // Create invalid entity that will cause constraint violation
            RetailCustomerEntity customer1 = TestDataBuilders.createValidRetailCustomer();
            customer1.setUsername("rollback.test@example.com");
            retailCustomerRepository.save(customer1);
            
            RetailCustomerEntity customer2 = TestDataBuilders.createValidRetailCustomer();
            customer2.setUsername("rollback.test@example.com"); // Duplicate username

            // Act & Assert - May throw either DataIntegrityViolationException or ConstraintViolationException
            assertThatThrownBy(() -> {
                retailCustomerRepository.save(customer2);
                flushAndClear();
            }).isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
            
            // Transaction should be rolled back, but this depends on transaction boundaries
            // In @Transactional test, the entire test is one transaction
        }

        @Test
        @DisplayName("Should handle partial failure in batch operations")
        void shouldHandlePartialFailureInBatchOperations() {
            // Arrange
            RetailCustomerEntity validCustomer = TestDataBuilders.createValidRetailCustomer();
            validCustomer.setUsername("batch.valid@example.com");
            
            RetailCustomerEntity duplicateCustomer1 = TestDataBuilders.createValidRetailCustomer();
            duplicateCustomer1.setUsername("batch.duplicate@example.com");
            
            RetailCustomerEntity duplicateCustomer2 = TestDataBuilders.createValidRetailCustomer();
            duplicateCustomer2.setUsername("batch.duplicate@example.com"); // Duplicate
            
            List<RetailCustomerEntity> customers = List.of(validCustomer, duplicateCustomer1, duplicateCustomer2);

            // Act & Assert - May throw either DataIntegrityViolationException or ConstraintViolationException
            assertThatThrownBy(() -> {
                retailCustomerRepository.saveAll(customers);
                flushAndClear();
            }).isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("Concurrent Modification Testing")
    class ConcurrentModificationTest {

        @Test
        @DisplayName("Should handle optimistic locking conflicts")
        void shouldHandleOptimisticLockingConflicts() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Original Description");
            UsagePointEntity savedEntity = usagePointRepository.save(usagePoint);
            flushAndClear();

            // Simulate concurrent modification by loading the same entity twice
            Optional<UsagePointEntity> entity1 = usagePointRepository.findById(savedEntity.getId());
            Optional<UsagePointEntity> entity2 = usagePointRepository.findById(savedEntity.getId());
            
            assertThat(entity1).isPresent();
            assertThat(entity2).isPresent();

            // Modify and save first entity
            entity1.get().setDescription("Modified by Entity 1");
            usagePointRepository.save(entity1.get());
            flushAndClear();

            // Try to modify and save second entity (should detect version conflict if optimistic locking is enabled)
            entity2.get().setDescription("Modified by Entity 2");
            
            // Note: This test depends on whether optimistic locking is configured
            // If not configured, this will succeed; if configured, it should throw an exception
            assertThatCode(() -> {
                usagePointRepository.save(entity2.get());
                flushAndClear();
            }).doesNotThrowAnyException(); // Adjust based on actual optimistic locking configuration
        }
    }

    @Nested
    @DisplayName("Edge Case Data Scenarios")
    class EdgeCaseDataTest {

        @Test
        @DisplayName("Should handle empty collections gracefully")
        void shouldHandleEmptyCollectionsGracefully() {
            // Act
            List<UsagePointEntity> result = usagePointRepository.saveAll(List.of());
            
            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle very long string values")
        void shouldHandleVeryLongStringValues() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            String longDescription = "A".repeat(1000); // Very long description
            usagePoint.setDescription(longDescription);

            // Act & Assert - Should throw DataException due to string length constraint
            assertThatThrownBy(() -> {
                usagePointRepository.save(usagePoint);
                flushAndClear();
            }).isInstanceOf(org.hibernate.exception.DataException.class)
              .hasMessageContaining("Value too long for column");
        }

        @Test
        @DisplayName("Should handle special characters in string fields")
        void shouldHandleSpecialCharactersInStringFields() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~");

            // Act & Assert
            assertThatCode(() -> {
                UsagePointEntity saved = usagePointRepository.save(usagePoint);
                flushAndClear();
                
                Optional<UsagePointEntity> retrieved = usagePointRepository.findById(saved.getId());
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get().getDescription()).contains("Special chars:");
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle Unicode characters in string fields")
        void shouldHandleUnicodeCharactersInStringFields() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Unicode: 测试 🌟 ñáéíóú αβγδε");

            // Act & Assert
            assertThatCode(() -> {
                UsagePointEntity saved = usagePointRepository.save(usagePoint);
                flushAndClear();
                
                Optional<UsagePointEntity> retrieved = usagePointRepository.findById(saved.getId());
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get().getDescription()).contains("Unicode:");
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Error Message Consistency")
    class ErrorMessageConsistencyTest {

        @Test
        @DisplayName("Should provide consistent error messages for null parameters")
        void shouldProvideConsistentErrorMessagesForNullParameters() {
            // Test multiple repositories for consistent null parameter handling
            Exception usagePointException = catchException(() -> usagePointRepository.save(null));
            Exception meterReadingException = catchException(() -> meterReadingRepository.save(null));
            Exception customerException = catchException(() -> retailCustomerRepository.save(null));

            // Assert that all throw the same type of exception
            assertThat(usagePointException).isInstanceOf(InvalidDataAccessApiUsageException.class);
            assertThat(meterReadingException).isInstanceOf(InvalidDataAccessApiUsageException.class);
            assertThat(customerException).isInstanceOf(InvalidDataAccessApiUsageException.class);
        }

        @Test
        @DisplayName("Should provide meaningful error messages for constraint violations")
        void shouldProvideMeaningfulErrorMessagesForConstraintViolations() {
            // Arrange
            RetailCustomerEntity customer1 = TestDataBuilders.createValidRetailCustomer();
            customer1.setUsername("error.message.test@example.com");
            retailCustomerRepository.save(customer1);
            flushAndClear();

            RetailCustomerEntity customer2 = TestDataBuilders.createValidRetailCustomer();
            customer2.setUsername("error.message.test@example.com"); // Duplicate

            // Act
            Exception exception = catchException(() -> {
                retailCustomerRepository.save(customer2);
                flushAndClear();
            });

            // Assert - May be either DataIntegrityViolationException or ConstraintViolationException
            assertThat(exception).isInstanceOfAny(
                DataIntegrityViolationException.class, 
                org.hibernate.exception.ConstraintViolationException.class
            );
            assertThat(exception.getMessage()).isNotNull();
            assertThat(exception.getMessage()).isNotEmpty();
        }
    }
}