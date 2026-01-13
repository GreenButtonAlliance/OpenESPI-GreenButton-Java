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
import org.greenbuttonalliance.espi.common.domain.common.SummaryMeasurement;
import org.greenbuttonalliance.espi.common.domain.usage.LineItemEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for LineItemRepository.
 *
 * Tests all CRUD operations, 2 custom query methods, relationships,
 * and validation constraints for LineItem entities.
 */
@DisplayName("LineItem Repository Tests")
class LineItemRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private LineItemRepository lineItemRepository;

    @Autowired
    private UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve line item successfully")
        void shouldSaveAndRetrieveLineItemSuccessfully() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            UsageSummaryEntity usageSummary = new UsageSummaryEntity(
                new DateTimeInterval(1640995200L, 2592000L),
                500000L,
                250000L
            );
            usageSummary.setUsagePoint(savedUsagePoint);
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.save(usageSummary);

            LineItemEntity lineItem = new LineItemEntity();
            lineItem.setAmount(10000L);
            lineItem.setRounding(5L);
            lineItem.setDateTime(1641000000L);
            lineItem.setNote("Energy delivery charge");
            lineItem.setItemKind(2); // Energy Delivery Fee
            lineItem.setUsageSummary(savedUsageSummary);

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAmount()).isEqualTo(10000L);
            assertThat(retrieved.get().getRounding()).isEqualTo(5L);
            assertThat(retrieved.get().getDateTime()).isEqualTo(1641000000L);
            assertThat(retrieved.get().getNote()).isEqualTo("Energy delivery charge");
            assertThat(retrieved.get().getItemKind()).isEqualTo(2);
            assertThat(retrieved.get().getUsageSummary().getId()).isEqualTo(savedUsageSummary.getId());
        }

        @Test
        @DisplayName("Should save line item with all optional fields")
        void shouldSaveLineItemWithAllOptionalFields() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            UsageSummaryEntity usageSummary = new UsageSummaryEntity(
                new DateTimeInterval(1640995200L, 2592000L),
                500000L,
                250000L
            );
            usageSummary.setUsagePoint(savedUsagePoint);
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.save(usageSummary);

            SummaryMeasurement measurement = new SummaryMeasurement("3", 1641000000L, "Wh", 15000L, null);
            DateTimeInterval itemPeriod = new DateTimeInterval(1640995200L, 86400L);

            LineItemEntity lineItem = new LineItemEntity();
            lineItem.setAmount(15000L);
            lineItem.setRounding(10L);
            lineItem.setDateTime(1641000000L);
            lineItem.setNote("Peak usage charge");
            lineItem.setMeasurement(measurement);
            lineItem.setItemKind(1); // Energy Generation Fee
            lineItem.setUnitCost(150L);
            lineItem.setItemPeriod(itemPeriod);
            lineItem.setUsageSummary(savedUsageSummary);

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMeasurement()).isNotNull();
            assertThat(retrieved.get().getMeasurement().getValue()).isEqualTo(15000L);
            assertThat(retrieved.get().getUnitCost()).isEqualTo(150L);
            assertThat(retrieved.get().getItemPeriod()).isNotNull();
            assertThat(retrieved.get().getItemPeriod().getStart()).isEqualTo(1640995200L);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            UsageSummaryEntity usageSummary = new UsageSummaryEntity(
                new DateTimeInterval(1640995200L, 2592000L),
                500000L,
                250000L
            );
            usageSummary.setUsagePoint(savedUsagePoint);
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.save(usageSummary);

            LineItemEntity lineItem1 = new LineItemEntity(10000L, 1641000000L, "Charge 1", 1);
            lineItem1.setUsageSummary(savedUsageSummary);

            LineItemEntity lineItem2 = new LineItemEntity(20000L, 1641086400L, "Charge 2", 2);
            lineItem2.setUsageSummary(savedUsageSummary);

            LineItemEntity saved1 = lineItemRepository.save(lineItem1);
            LineItemEntity saved2 = lineItemRepository.save(lineItem2);
            flushAndClear();

            // Act
            List<Long> allIds = lineItemRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all line items by usage summary ID")
        void shouldFindAllLineItemsByUsageSummaryId() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            UsageSummaryEntity usageSummary1 = new UsageSummaryEntity(
                new DateTimeInterval(1640995200L, 2592000L),
                500000L,
                250000L
            );
            usageSummary1.setUsagePoint(savedUsagePoint);
            UsageSummaryEntity savedUsageSummary1 = usageSummaryRepository.save(usageSummary1);

            UsageSummaryEntity usageSummary2 = new UsageSummaryEntity(
                new DateTimeInterval(1643587200L, 2592000L),
                600000L,
                300000L
            );
            usageSummary2.setUsagePoint(savedUsagePoint);
            UsageSummaryEntity savedUsageSummary2 = usageSummaryRepository.save(usageSummary2);

            LineItemEntity lineItem1 = new LineItemEntity(10000L, 1641000000L, "Summary 1 - Item 1", 1);
            lineItem1.setUsageSummary(savedUsageSummary1);

            LineItemEntity lineItem2 = new LineItemEntity(20000L, 1641086400L, "Summary 1 - Item 2", 2);
            lineItem2.setUsageSummary(savedUsageSummary1);

            LineItemEntity lineItem3 = new LineItemEntity(30000L, 1643600000L, "Summary 2 - Item 1", 1);
            lineItem3.setUsageSummary(savedUsageSummary2);

            lineItemRepository.save(lineItem1);
            lineItemRepository.save(lineItem2);
            lineItemRepository.save(lineItem3);
            flushAndClear();

            // Act
            List<LineItemEntity> lineItems = lineItemRepository.findAllByUsageSummaryId(savedUsageSummary1.getId());

            // Assert
            assertThat(lineItems).hasSize(2);
            assertThat(lineItems).extracting(LineItemEntity::getNote)
                .containsExactlyInAnyOrder("Summary 1 - Item 1", "Summary 1 - Item 2");
        }
    }

    @Nested
    @DisplayName("JPA Relationships")
    class RelationshipsTest {

        @Test
        @DisplayName("Should maintain usage summary relationship")
        void shouldMaintainUsageSummaryRelationship() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            UsageSummaryEntity usageSummary = new UsageSummaryEntity(
                new DateTimeInterval(1640995200L, 2592000L),
                500000L,
                250000L
            );
            usageSummary.setDescription("Test Summary");
            usageSummary.setUsagePoint(savedUsagePoint);
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.save(usageSummary);

            LineItemEntity lineItem = new LineItemEntity(10000L, 1641000000L, "Test Charge", 1);
            lineItem.setUsageSummary(savedUsageSummary);

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();
            Optional<LineItemEntity> retrieved = lineItemRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsageSummary()).isNotNull();
            assertThat(retrieved.get().getUsageSummary().getId()).isEqualTo(savedUsageSummary.getId());
            assertThat(retrieved.get().getUsageSummary().getDescription()).isEqualTo("Test Summary");
        }
    }

    @Nested
    @DisplayName("Business Logic")
    class BusinessLogicTest {

        @Test
        @DisplayName("Should validate line item correctly")
        void shouldValidateLineItemCorrectly() {
            // Arrange
            LineItemEntity validLineItem = new LineItemEntity(10000L, 1641000000L, "Valid charge", 1);
            LineItemEntity invalidLineItem1 = new LineItemEntity(10000L, 1641000000L, null, 1); // null note
            LineItemEntity invalidLineItem2 = new LineItemEntity(10000L, 1641000000L, "  ", 1); // blank note

            // Act & Assert
            assertThat(validLineItem.isValid()).isTrue();
            assertThat(invalidLineItem1.isValid()).isFalse();
            assertThat(invalidLineItem2.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify charges and credits")
        void shouldCorrectlyIdentifyChargesAndCredits() {
            // Arrange
            LineItemEntity charge = new LineItemEntity(10000L, 1641000000L, "Charge", 1);
            LineItemEntity credit = new LineItemEntity(-5000L, 1641000000L, "Credit", 3);
            LineItemEntity zeroAmount = new LineItemEntity(0L, 1641000000L, "Zero", 1);

            // Act & Assert
            assertThat(charge.isCharge()).isTrue();
            assertThat(charge.isCredit()).isFalse();

            assertThat(credit.isCharge()).isFalse();
            assertThat(credit.isCredit()).isTrue();

            assertThat(zeroAmount.isCharge()).isFalse();
            assertThat(zeroAmount.isCredit()).isFalse();
            assertThat(zeroAmount.isZeroAmount()).isTrue();
        }

        @Test
        @DisplayName("Should calculate total amount with rounding")
        void shouldCalculateTotalAmountWithRounding() {
            // Arrange
            LineItemEntity withRounding = new LineItemEntity(10000L, 15L, 1641000000L, "With rounding", 1);
            LineItemEntity withoutRounding = new LineItemEntity(10000L, 1641000000L, "Without rounding", 1);

            // Act & Assert
            assertThat(withRounding.getTotalAmount()).isEqualTo(10015L);
            assertThat(withRounding.hasRounding()).isTrue();

            assertThat(withoutRounding.getTotalAmount()).isEqualTo(10000L);
            assertThat(withoutRounding.hasRounding()).isFalse();
        }
    }

    @Nested
    @DisplayName("Entity Persistence")
    class EntityPersistenceTest {

        @Test
        @DisplayName("Should persist and retrieve line item")
        void shouldPersistAndRetrieveLineItem() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            UsageSummaryEntity usageSummary = new UsageSummaryEntity(
                new DateTimeInterval(1640995200L, 2592000L),
                500000L,
                250000L
            );
            usageSummary.setUsagePoint(savedUsagePoint);
            UsageSummaryEntity savedUsageSummary = usageSummaryRepository.save(usageSummary);

            LineItemEntity lineItem = new LineItemEntity(10000L, 1641000000L, "Persistence Test", 1);
            lineItem.setUsageSummary(savedUsageSummary);

            // Act
            LineItemEntity saved = lineItemRepository.save(lineItem);
            flushAndClear();

            // Assert
            // LineItem extends Object (not IdentifiedObject) in ESPI 4.0 XSD,
            // so it has Long ID but no Atom links or timestamps
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getAmount()).isEqualTo(10000L);
            assertThat(saved.getDateTime()).isEqualTo(1641000000L);
            assertThat(saved.getNote()).isEqualTo("Persistence Test");
            assertThat(saved.getItemKind()).isEqualTo(1);
            assertThat(saved.getUsageSummary().getId()).isEqualTo(savedUsageSummary.getId());
        }
    }
}
