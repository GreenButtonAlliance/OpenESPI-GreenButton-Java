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

import org.greenbuttonalliance.espi.common.domain.usage.LineItemEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintViolation;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for LineItemRepository.
 * 
 * Tests all CRUD operations, 10 custom query methods, billing line item field testing,
 * amount and quantity validation testing, and UsageSummary relationship testing.
 */
@DisplayName("LineItem Repository Tests")
class LineItemRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private LineItemRepository lineItemRepository;

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
        return usagePoint;
    }

    /**
     * Creates a valid UsageSummaryEntity for testing.
     */
    private UsageSummaryEntity createValidUsageSummary() {
        UsageSummaryEntity summary = new UsageSummaryEntity();
        summary.setDescription("Test Usage Summary - " + faker.lorem().sentence(3));
        summary.setBillingPeriodStart(randomOffsetDateTime().toEpochSecond());
        summary.setBillingPeriodDuration(2592000L); // 30 days
        summary.setBillLastPeriod(faker.number().numberBetween(5000L, 50000L)); // $50-$500
        summary.setBillToDate(faker.number().numberBetween(1000L, 10000L)); // $10-$100
        summary.setCostAdditionalLastPeriod(faker.number().numberBetween(500L, 5000L)); // $5-$50
        return summary;
    }

    /**
     * Creates a valid LineItemEntity for testing.
     */
    private LineItemEntity createValidLineItem() {
        LineItemEntity lineItem = new LineItemEntity();
        lineItem.setDescription("Test Line Item - " + faker.lorem().sentence(3));
        lineItem.setAmount(faker.number().numberBetween(100L, 10000L)); // $1-$100
        lineItem.setDateTime(randomOffsetDateTime().toEpochSecond());
        lineItem.setNote(faker.lorem().sentence(5));
        lineItem.setRounding(faker.number().numberBetween(-5L, 5L)); // Small rounding adjustment
        return lineItem;
    }

    /**
     * Creates a complete test setup with RetailCustomer, UsagePoint, UsageSummary, and LineItem.
     */
    private LineItemEntity createCompleteTestSetup() {
        RetailCustomerEntity customer = createValidRetailCustomer();
        RetailCustomerEntity savedCustomer = persistAndFlush(customer);

        UsagePointEntity usagePoint = createValidUsagePoint();
        usagePoint.setRetailCustomer(savedCustomer);
        UsagePointEntity savedUsagePoint = persistAndFlush(usagePoint);

        UsageSummaryEntity usageSummary = createValidUsageSummary();
        usageSummary.setUsagePoint(savedUsagePoint);
        UsageSummaryEntity savedUsageSummary = persistAndFlush(usageSummary);

        LineItemEntity lineItem = createValidLineItem();
        lineItem.setUsageSummary(savedUsageSummary);
        
        return lineItem;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve line item successfully")
        void shouldSaveAndRetrieveLineItemSuccessfully() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            lineItem.setDescription("Test Line Item for CRUD");
            lineItem.setNote("Test billing charge");

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Line Item for CRUD");
            assertThat(retrieved.get().getNote()).isEqualTo("Test billing charge");
            assertThat(retrieved.get().getUsageSummary()).isNotNull();
        }

        @Test
        @DisplayName("Should update line item successfully")
        void shouldUpdateLineItemSuccessfully() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            LineItemEntity saved = persistAndFlush(lineItem);

            // Act
            saved.setDescription("Updated Line Item Description");
            saved.setAmount(5000L); // $50.00
            saved.setNote("Updated billing charge");
            LineItemEntity updated = lineItemRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Updated Line Item Description");
            assertThat(retrieved.get().getAmount()).isEqualTo(5000L);
            assertThat(retrieved.get().getNote()).isEqualTo("Updated billing charge");
        }

        @Test
        @DisplayName("Should delete line item successfully")
        void shouldDeleteLineItemSuccessfully() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            LineItemEntity saved = persistAndFlush(lineItem);
            UUID savedId = saved.getId();

            // Act
            lineItemRepository.deleteById(savedId);
            flushAndClear();

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(savedId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should find all line items")
        void shouldFindAllLineItems() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            lineItem1.setNote("First line item");
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setNote("Second line item");
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);

            // Act
            List<LineItemEntity> allLineItems = lineItemRepository.findAll();

            // Assert
            assertThat(allLineItems).hasSizeGreaterThanOrEqualTo(2);
            assertThat(allLineItems)
                .extracting(LineItemEntity::getNote)
                .contains("First line item", "Second line item");
        }

        @Test
        @DisplayName("Should count line items correctly")
        void shouldCountLineItemsCorrectly() {
            // Arrange
            long initialCount = lineItemRepository.count();
            LineItemEntity lineItem1 = createCompleteTestSetup();
            LineItemEntity lineItem2 = createCompleteTestSetup();
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);

            // Act
            long finalCount = lineItemRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find line items by usage summary ID")
        void shouldFindLineItemsByUsageSummaryId() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setUsageSummary(lineItem1.getUsageSummary()); // Same usage summary
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByUsageSummaryId(lineItem1.getUsageSummary().getId());

            // Assert
            assertThat(lineItems).hasSize(2);
            assertThat(lineItems).extracting(LineItemEntity::getUsageSummary)
                .allMatch(us -> us.getId().equals(lineItem1.getUsageSummary().getId()));
        }

        @Test
        @DisplayName("Should find line items by electric power usage summary ID")
        void shouldFindLineItemsByElectricPowerUsageSummaryId() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setUsageSummary(lineItem1.getUsageSummary()); // Same usage summary
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByElectricPowerUsageSummaryId(lineItem1.getUsageSummary().getId());

            // Assert
            assertThat(lineItems).hasSize(2);
            assertThat(lineItems).extracting(LineItemEntity::getUsageSummary)
                .allMatch(us -> us.getId().equals(lineItem1.getUsageSummary().getId()));
        }

        @Test
        @DisplayName("Should find line items by date time range")
        void shouldFindLineItemsByDateTimeRange() {
            // Arrange
            long baseTime = OffsetDateTime.now().toEpochSecond();
            LineItemEntity lineItem1 = createCompleteTestSetup();
            lineItem1.setDateTime(baseTime);
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setDateTime(baseTime + 3600); // 1 hour later
            LineItemEntity lineItem3 = createCompleteTestSetup();
            lineItem3.setDateTime(baseTime + 7200); // 2 hours later
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);
            persistAndFlush(lineItem3);

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByDateTimeRange(baseTime - 1, baseTime + 3601);

            // Assert
            assertThat(lineItems).hasSize(2);
            assertThat(lineItems).extracting(LineItemEntity::getDateTime)
                .allMatch(dt -> dt >= baseTime - 1 && dt <= baseTime + 3601);
        }

        @Test
        @DisplayName("Should find line items by amount range")
        void shouldFindLineItemsByAmountRange() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            lineItem1.setAmount(1000L); // $10.00
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setAmount(2000L); // $20.00
            LineItemEntity lineItem3 = createCompleteTestSetup();
            lineItem3.setAmount(5000L); // $50.00
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);
            persistAndFlush(lineItem3);

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByAmountRange(1500L, 3000L);

            // Assert
            assertThat(lineItems).hasSize(1);
            assertThat(lineItems.get(0).getAmount()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("Should find line items by note containing text")
        void shouldFindLineItemsByNoteContaining() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            lineItem1.setNote("Monthly service charge");
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setNote("Energy usage charge");
            LineItemEntity lineItem3 = createCompleteTestSetup();
            lineItem3.setNote("Tax and fees");
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);
            persistAndFlush(lineItem3);

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findByNoteContaining("charge");

            // Assert
            assertThat(lineItems).hasSize(2);
            assertThat(lineItems).extracting(LineItemEntity::getNote)
                .allMatch(note -> note.toLowerCase().contains("charge"));
        }

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            LineItemEntity lineItem2 = createCompleteTestSetup();
            
            LineItemEntity saved1 = persistAndFlush(lineItem1);
            LineItemEntity saved2 = persistAndFlush(lineItem2);

            // Act
            List<UUID> allIds = lineItemRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should sum amounts by usage summary")
        void shouldSumAmountsByUsageSummary() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            lineItem1.setAmount(1000L); // $10.00
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setAmount(2000L); // $20.00
            lineItem2.setUsageSummary(lineItem1.getUsageSummary()); // Same usage summary
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);

            // Act
            Long totalAmount = lineItemRepository.sumAmountsByUsageSummary(lineItem1.getUsageSummary().getId());

            // Assert
            assertThat(totalAmount).isEqualTo(3000L); // $30.00
        }

        @Test
        @DisplayName("Should sum amounts by electric power usage summary")
        void shouldSumAmountsByElectricPowerUsageSummary() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            lineItem1.setAmount(1500L); // $15.00
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setAmount(2500L); // $25.00
            lineItem2.setUsageSummary(lineItem1.getUsageSummary()); // Same usage summary
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);

            // Act
            Long totalAmount = lineItemRepository.sumAmountsByElectricPowerUsageSummary(lineItem1.getUsageSummary().getId());

            // Assert
            assertThat(totalAmount).isEqualTo(4000L); // $40.00
        }

        @Test
        @DisplayName("Should count line items by usage summary")
        void shouldCountLineItemsByUsageSummary() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            LineItemEntity lineItem2 = createCompleteTestSetup();
            LineItemEntity lineItem3 = createCompleteTestSetup();
            lineItem2.setUsageSummary(lineItem1.getUsageSummary()); // Same usage summary
            lineItem3.setUsageSummary(lineItem1.getUsageSummary()); // Same usage summary
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);
            persistAndFlush(lineItem3);

            // Act
            Long count = lineItemRepository.countByUsageSummary(lineItem1.getUsageSummary().getId());

            // Assert
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should count line items by electric power usage summary")
        void shouldCountLineItemsByElectricPowerUsageSummary() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            LineItemEntity lineItem2 = createCompleteTestSetup();
            lineItem2.setUsageSummary(lineItem1.getUsageSummary()); // Same usage summary
            
            persistAndFlush(lineItem1);
            persistAndFlush(lineItem2);

            // Act
            Long count = lineItemRepository.countByElectricPowerUsageSummary(lineItem1.getUsageSummary().getId());

            // Assert
            assertThat(count).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Amount and Quantity Validation Testing")
    class AmountValidationTest {

        @Test
        @DisplayName("Should validate amount not null constraint")
        void shouldValidateAmountNotNullConstraint() {
            // Arrange
            LineItemEntity lineItem = createValidLineItem();
            lineItem.setAmount(null);

            // Act
            Set<ConstraintViolation<LineItemEntity>> violations = validator.validate(lineItem);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Amount cannot be null");
        }

        @Test
        @DisplayName("Should accept positive amounts (charges)")
        void shouldAcceptPositiveAmounts() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            lineItem.setAmount(5000L); // $50.00 charge

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAmount()).isEqualTo(5000L);
            assertThat(retrieved.get().isCharge()).isTrue();
            assertThat(retrieved.get().isCredit()).isFalse();
        }

        @Test
        @DisplayName("Should accept negative amounts (credits)")
        void shouldAcceptNegativeAmounts() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            lineItem.setAmount(-2000L); // $20.00 credit

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAmount()).isEqualTo(-2000L);
            assertThat(retrieved.get().isCredit()).isTrue();
            assertThat(retrieved.get().isCharge()).isFalse();
        }

        @Test
        @DisplayName("Should accept zero amounts")
        void shouldAcceptZeroAmounts() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            lineItem.setAmount(0L);

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAmount()).isEqualTo(0L);
            assertThat(retrieved.get().isZeroAmount()).isTrue();
        }

        @Test
        @DisplayName("Should handle rounding adjustments")
        void shouldHandleRoundingAdjustments() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            lineItem.setAmount(1234L); // $12.34
            lineItem.setRounding(-4L); // -$0.04 rounding

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAmount()).isEqualTo(1234L);
            assertThat(retrieved.get().getRounding()).isEqualTo(-4L);
            assertThat(retrieved.get().getTotalAmount()).isEqualTo(1230L); // $12.30
            assertThat(retrieved.get().hasRounding()).isTrue();
        }

        @Test
        @DisplayName("Should handle null rounding")
        void shouldHandleNullRounding() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            lineItem.setAmount(1000L);
            lineItem.setRounding(null);

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getRounding()).isNull();
            assertThat(retrieved.get().getTotalAmount()).isEqualTo(1000L);
            assertThat(retrieved.get().hasRounding()).isFalse();
        }
    }

    @Nested
    @DisplayName("Note Validation Testing")
    class NoteValidationTest {

        @Test
        @DisplayName("Should validate note not null constraint")
        void shouldValidateNoteNotNullConstraint() {
            // Arrange
            LineItemEntity lineItem = createValidLineItem();
            lineItem.setNote(null);

            // Act
            Set<ConstraintViolation<LineItemEntity>> violations = validator.validate(lineItem);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Note cannot be null");
        }

        @Test
        @DisplayName("Should validate note length constraint")
        void shouldValidateNoteLengthConstraint() {
            // Arrange
            LineItemEntity lineItem = createValidLineItem();
            lineItem.setNote("x".repeat(257)); // Exceeds 256 character limit

            // Act
            Set<ConstraintViolation<LineItemEntity>> violations = validator.validate(lineItem);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Note cannot exceed 256 characters");
        }

        @Test
        @DisplayName("Should accept valid note length")
        void shouldAcceptValidNoteLength() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            lineItem.setNote("x".repeat(256)); // Exactly 256 characters

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getNote()).hasSize(256);
        }
    }

    @Nested
    @DisplayName("DateTime Validation Testing")
    class DateTimeValidationTest {

        @Test
        @DisplayName("Should validate dateTime not null constraint")
        void shouldValidateDateTimeNotNullConstraint() {
            // Arrange
            LineItemEntity lineItem = createValidLineItem();
            lineItem.setDateTime(null);

            // Act
            Set<ConstraintViolation<LineItemEntity>> violations = validator.validate(lineItem);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Date time cannot be null");
        }

        @Test
        @DisplayName("Should accept valid Unix timestamps")
        void shouldAcceptValidUnixTimestamps() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            long currentTime = OffsetDateTime.now().toEpochSecond();
            lineItem.setDateTime(currentTime);

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDateTime()).isEqualTo(currentTime);
        }
    }

    @Nested
    @DisplayName("UsageSummary Relationship Testing")
    class UsageSummaryRelationshipTest {

        @Test
        @DisplayName("Should maintain UsageSummary relationship")
        void shouldMaintainUsageSummaryRelationship() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsageSummary()).isNotNull();
            assertThat(retrieved.get().getUsageSummary().getId()).isEqualTo(lineItem.getUsageSummary().getId());
        }

        @Test
        @DisplayName("Should handle lazy loading of UsageSummary")
        void shouldHandleLazyLoadingOfUsageSummary() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            LineItemEntity saved = persistAndFlush(lineItem);

            // Act - Clear persistence context to test lazy loading
            flushAndClear();
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            // Access the usage summary to trigger lazy loading
            assertThat(retrieved.get().getUsageSummary().getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should allow null UsageSummary")
        void shouldAllowNullUsageSummary() {
            // Arrange
            LineItemEntity lineItem = createValidLineItem();
            lineItem.setUsageSummary(null);

            // Act
            LineItemEntity saved = persistAndFlush(lineItem);

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsageSummary()).isNull();
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            
            LineItemEntity entity = retrieved.get();
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getCreated()).isNotNull();
            assertThat(entity.getUpdated()).isNotNull();
            assertThat(entity.getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should update timestamps on modification")
        void shouldUpdateTimestampsOnModification() {
            // Arrange
            LineItemEntity lineItem = createCompleteTestSetup();
            LineItemEntity saved = persistAndFlush(lineItem);
            
            // Wait a moment to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            saved.setDescription("Updated Description");
            LineItemEntity updated = lineItemRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUpdated()).isAfter(retrieved.get().getCreated());
        }

        @Test
        @DisplayName("Should generate unique IDs for different entities")
        void shouldGenerateUniqueIdsForDifferentEntities() {
            // Arrange
            LineItemEntity lineItem1 = createCompleteTestSetup();
            LineItemEntity lineItem2 = createCompleteTestSetup();

            // Act
            LineItemEntity saved1 = lineItemRepository.save(lineItem1);
            LineItemEntity saved2 = lineItemRepository.save(lineItem2);
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
            LineItemEntity lineItem1 = createCompleteTestSetup();
            LineItemEntity lineItem2 = createCompleteTestSetup();
            
            LineItemEntity saved1 = persistAndFlush(lineItem1);
            LineItemEntity saved2 = persistAndFlush(lineItem2);

            // Act & Assert
            assertThat(saved1).isNotEqualTo(saved2);
            assertThat(saved1.hashCode()).isNotEqualTo(saved2.hashCode());
            
            // Same entity should be equal to itself
            assertThat(saved1).isEqualTo(saved1);
            assertThat(saved1.hashCode()).isEqualTo(saved1.hashCode());
        }
    }
}