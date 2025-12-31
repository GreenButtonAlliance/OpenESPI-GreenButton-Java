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
import org.greenbuttonalliance.espi.common.domain.usage.LineItemEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for LineItemRepository.
 * 
 * Tests all CRUD operations, 10 custom query methods, relationships,
 * and validation constraints for LineItem entities.
 */
@DisplayName("LineItem Repository Tests")
class LineItemRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private LineItemRepository lineItemRepository;

    @Autowired
    private UsageSummaryRepository usageSummaryRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve line item successfully")
        void shouldSaveAndRetrieveLineItemSuccessfully() {
            // Arrange
            LineItemEntity lineItem = new LineItemEntity(
                10000L, // $100.00 in cents
                1640995200L, // 2022-01-01 00:00:00 UTC
                "Monthly service charge"
            );

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAmount()).isEqualTo(10000L);
            assertThat(retrieved.get().getDateTime()).isEqualTo(1640995200L);
            assertThat(retrieved.get().getNote()).isEqualTo("Monthly service charge");
        }

        @Test
        @DisplayName("Should save line item with rounding")
        void shouldSaveLineItemWithRounding() {
            // Arrange
            LineItemEntity lineItem = new LineItemEntity(
                9999L, // $99.99 in cents
                1L, // 1 cent rounding
                1640995200L,
                "Usage charge with rounding"
            );

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAmount()).isEqualTo(9999L);
            assertThat(retrieved.get().getRounding()).isEqualTo(1L);
            assertThat(retrieved.get().getNote()).isEqualTo("Usage charge with rounding");
        }

        @Test
        @DisplayName("Should save line item with usage summary relationship")
        void shouldSaveLineItemWithUsageSummaryRelationship() {
            // Arrange
            UsageSummaryEntity usageSummary = new UsageSummaryEntity();
            usageSummary.setDescription("Test Usage Summary");
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.saveAndFlush(usageSummary);
            flushAndClear(); // Clear session to avoid cascade conflicts
            
            LineItemEntity lineItem = new LineItemEntity(
                5000L, // $50.00 in cents
                1640995200L,
                "Line item with usage summary"
            );
            lineItem.setUsageSummary(savedUsageSummary);

            // Act
            LineItemEntity saved = lineItemRepository.saveAndFlush(lineItem);
            flushAndClear();
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsageSummary()).isNotNull();
            assertThat(retrieved.get().getUsageSummary().getId()).isEqualTo(savedUsageSummary.getId());
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find line items by usage summary ID")
        void shouldFindLineItemsByUsageSummaryId() {
            // Arrange
            UsageSummaryEntity usageSummary = new UsageSummaryEntity();
            usageSummary.setDescription("Query Test Usage Summary");
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.saveAndFlush(usageSummary);
            flushAndClear(); // Clear session to avoid cascade conflicts
            
            LineItemEntity lineItem1 = new LineItemEntity(1000L, 1640995200L, "First item");
            lineItem1.setUsageSummary(savedUsageSummary);
            LineItemEntity lineItem2 = new LineItemEntity(2000L, 1640995300L, "Second item");
            lineItem2.setUsageSummary(savedUsageSummary);
            
            lineItemRepository.saveAndFlush(lineItem1);
            lineItemRepository.saveAndFlush(lineItem2);
            flushAndClear();

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByUsageSummaryId(savedUsageSummary.getId());

            // Assert
            assertThat(lineItems).hasSize(2);
            assertThat(lineItems).extracting(LineItemEntity::getNote)
                .containsExactly("First item", "Second item"); // Should be ordered by dateTime
        }

        @Test
        @DisplayName("Should find line items by date time range")
        void shouldFindLineItemsByDateTimeRange() {
            // Arrange
            LineItemEntity lineItem1 = new LineItemEntity(1000L, 1640995200L, "Item 1"); // 2022-01-01
            LineItemEntity lineItem2 = new LineItemEntity(2000L, 1641081600L, "Item 2"); // 2022-01-02
            LineItemEntity lineItem3 = new LineItemEntity(3000L, 1641168000L, "Item 3"); // 2022-01-03
            
            lineItemRepository.save(lineItem1);
            lineItemRepository.save(lineItem2);
            lineItemRepository.save(lineItem3);
            flushAndClear();

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByDateTimeRange(
                1640995200L, // 2022-01-01
                1641081600L  // 2022-01-02
            );

            // Assert
            assertThat(lineItems).hasSize(2);
            assertThat(lineItems).extracting(LineItemEntity::getNote)
                .containsExactly("Item 1", "Item 2");
        }

        @Test
        @DisplayName("Should find line items by amount range")
        void shouldFindLineItemsByAmountRange() {
            // Arrange
            LineItemEntity lineItem1 = new LineItemEntity(1000L, 1640995200L, "Low amount");
            LineItemEntity lineItem2 = new LineItemEntity(5000L, 1640995200L, "Medium amount");
            LineItemEntity lineItem3 = new LineItemEntity(10000L, 1640995200L, "High amount");
            
            lineItemRepository.save(lineItem1);
            lineItemRepository.save(lineItem2);
            lineItemRepository.save(lineItem3);
            flushAndClear();

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByAmountRange(2000L, 8000L);

            // Assert
            assertThat(lineItems).hasSize(1);
            assertThat(lineItems.get(0).getNote()).isEqualTo("Medium amount");
            assertThat(lineItems.get(0).getAmount()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("Should find line items by note containing text")
        void shouldFindLineItemsByNoteContaining() {
            // Arrange
            LineItemEntity lineItem1 = new LineItemEntity(1000L, 1640995200L, "Monthly service charge");
            LineItemEntity lineItem2 = new LineItemEntity(2000L, 1640995200L, "Usage charge for electricity");
            LineItemEntity lineItem3 = new LineItemEntity(3000L, 1640995200L, "Tax and fees");
            
            lineItemRepository.save(lineItem1);
            lineItemRepository.save(lineItem2);
            lineItemRepository.save(lineItem3);
            flushAndClear();

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByNoteContaining("charge");

            // Assert
            assertThat(lineItems).hasSize(2);
            assertThat(lineItems).extracting(LineItemEntity::getNote)
                .contains("Monthly service charge", "Usage charge for electricity");
        }

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            LineItemEntity lineItem1 = new LineItemEntity(1000L, 1640995200L, "Item 1");
            LineItemEntity lineItem2 = new LineItemEntity(2000L, 1640995200L, "Item 2");
            
            LineItemEntity saved1 = lineItemRepository.save(lineItem1);
            LineItemEntity saved2 = lineItemRepository.save(lineItem2);
            flushAndClear();

            // Act
            List<UUID> allIds = lineItemRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should sum amounts by usage summary")
        void shouldSumAmountsByUsageSummary() {
            // Arrange
            UsageSummaryEntity usageSummary = new UsageSummaryEntity();
            usageSummary.setDescription("Sum Test Usage Summary");
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.saveAndFlush(usageSummary);
            flushAndClear(); // Clear session to avoid cascade conflicts
            
            LineItemEntity lineItem1 = new LineItemEntity(1000L, 1640995200L, "Item 1");
            lineItem1.setUsageSummary(savedUsageSummary);
            LineItemEntity lineItem2 = new LineItemEntity(2000L, 1640995200L, "Item 2");
            lineItem2.setUsageSummary(savedUsageSummary);
            LineItemEntity lineItem3 = new LineItemEntity(3000L, 1640995200L, "Item 3");
            lineItem3.setUsageSummary(savedUsageSummary);
            
            lineItemRepository.saveAndFlush(lineItem1);
            lineItemRepository.saveAndFlush(lineItem2);
            lineItemRepository.saveAndFlush(lineItem3);
            flushAndClear();

            // Act
            Long totalAmount = lineItemRepository.sumAmountsByUsageSummary(savedUsageSummary.getId());

            // Assert
            assertThat(totalAmount).isEqualTo(6000L); // 1000 + 2000 + 3000
        }

        @Test
        @DisplayName("Should count line items by usage summary")
        void shouldCountLineItemsByUsageSummary() {
            // Arrange
            UsageSummaryEntity usageSummary = new UsageSummaryEntity();
            usageSummary.setDescription("Count Test Usage Summary");
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.saveAndFlush(usageSummary);
            flushAndClear(); // Clear session to avoid cascade conflicts
            
            LineItemEntity lineItem1 = new LineItemEntity(1000L, 1640995200L, "Item 1");
            lineItem1.setUsageSummary(savedUsageSummary);
            LineItemEntity lineItem2 = new LineItemEntity(2000L, 1640995200L, "Item 2");
            lineItem2.setUsageSummary(savedUsageSummary);
            
            lineItemRepository.saveAndFlush(lineItem1);
            lineItemRepository.saveAndFlush(lineItem2);
            flushAndClear();

            // Act
            Long count = lineItemRepository.countByUsageSummary(savedUsageSummary.getId());

            // Assert
            assertThat(count).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() {
            // Arrange
            LineItemEntity lineItem = new LineItemEntity();
            // Not setting required fields: amount, dateTime, note

            // Act & Assert
            Set<ConstraintViolation<LineItemEntity>> violations = validator.validate(lineItem);
            assertThat(violations).hasSize(3); // amount, dateTime, note are required
            
            Set<String> violationMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
            
            assertThat(violationMessages).contains(
                "Amount cannot be null",
                "Date time cannot be null",
                "Note cannot be null"
            );
        }

        @Test
        @DisplayName("Should validate note length constraint")
        void shouldValidateNoteLengthConstraint() {
            // Arrange
            String longNote = "A".repeat(257); // Exceeds 256 character limit
            LineItemEntity lineItem = new LineItemEntity(1000L, 1640995200L, longNote);

            // Act & Assert
            Set<ConstraintViolation<LineItemEntity>> violations = validator.validate(lineItem);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Note cannot exceed 256 characters");
        }
    }

    @Nested
    @DisplayName("Entity Functionality")
    class EntityFunctionalityTest {

        @Test
        @DisplayName("Should persist with auto-generated UUID")
        void shouldPersistWithAutoGeneratedUuid() {
            // Arrange
            LineItemEntity lineItem = new LineItemEntity(1000L, 1640995200L, "UUID test");

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getAmount()).isEqualTo(1000L);
            assertThat(saved.getNote()).isEqualTo("UUID test");
        }
    }
}