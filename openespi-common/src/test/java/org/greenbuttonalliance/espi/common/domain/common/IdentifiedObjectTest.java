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

package org.greenbuttonalliance.espi.common.domain.common;

import jakarta.validation.ConstraintViolation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Test suite for IdentifiedObject base class functionality.
 * 
 * Tests the fundamental functionality that all ESPI entities inherit from,
 * including UUID generation, timestamp behavior, link management, and validation.
 */
@DisplayName("IdentifiedObject Base Class Tests")
class IdentifiedObjectTest extends BaseRepositoryTest {

    @Nested
    @DisplayName("UUID Generation and Management")
    class UuidGenerationTest {

        @Test
        @DisplayName("Should auto-generate UUID on entity creation")
        void shouldAutoGenerateUuidOnEntityCreation() {
            // Arrange & Act
            UsagePointEntity usagePoint = new UsagePointEntity();
            CustomerEntity customer = new CustomerEntity();

            // Assert
            assertThat(usagePoint.getId()).isNotNull();
            assertThat(usagePoint.getId()).isInstanceOf(UUID.class);
            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getId()).isInstanceOf(UUID.class);
            
            // Each entity should have a unique UUID
            assertThat(usagePoint.getId()).isNotEqualTo(customer.getId());
        }

        @Test
        @DisplayName("Should maintain UUID consistency across operations")
        void shouldMaintainUuidConsistencyAcrossOperations() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            usagePoint.setDescription("Test Usage Point");
            UUID originalId = usagePoint.getId();

            // Act
            usagePoint.setDescription("Updated Description");
            usagePoint.setStatus((short) 1);

            // Assert
            assertThat(usagePoint.getId()).isEqualTo(originalId);
        }

        @Test
        @DisplayName("Should allow manual UUID setting")
        void shouldAllowManualUuidSetting() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            UUID customId = UUID.randomUUID();

            // Act
            usagePoint.setId(customId);

            // Assert
            assertThat(usagePoint.getId()).isEqualTo(customId);
        }
    }

    @Nested
    @DisplayName("Timestamp Behavior")
    class TimestampBehaviorTest {

        @Test
        @DisplayName("Should set creation timestamp on persist")
        void shouldSetCreationTimestampOnPersist() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            usagePoint.setDescription("Test Usage Point");
            usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
            
            LocalDateTime beforePersist = LocalDateTime.now();

            // Act
            UsagePointEntity persisted = persistAndFlush(usagePoint);

            // Assert
            LocalDateTime afterPersist = LocalDateTime.now();
            assertThat(persisted.getCreated()).isNotNull();
            assertThat(persisted.getCreated()).isBetween(beforePersist, afterPersist);
        }

        @Test
        @DisplayName("Should update modification timestamp on entity update")
        void shouldUpdateModificationTimestampOnEntityUpdate() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            usagePoint.setDescription("Original Description");
            usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
            UsagePointEntity persisted = persistAndFlush(usagePoint);
            
            LocalDateTime originalUpdated = persisted.getUpdated();
            
            // Wait a small amount to ensure timestamp difference
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            persisted.setDescription("Updated Description");
            UsagePointEntity updated = mergeAndFlush(persisted);

            // Assert
            assertThat(updated.getUpdated()).isNotNull();
            assertThat(updated.getUpdated()).isAfter(originalUpdated);
        }

        @Test
        @DisplayName("Should not modify creation timestamp on update")
        void shouldNotModifyCreationTimestampOnUpdate() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            usagePoint.setDescription("Original Description");
            usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
            UsagePointEntity persisted = persistAndFlush(usagePoint);
            
            LocalDateTime originalCreated = persisted.getCreated();

            // Act
            persisted.setDescription("Updated Description");
            UsagePointEntity updated = mergeAndFlush(persisted);

            // Assert
            assertThat(updated.getCreated()).isEqualTo(originalCreated);
        }
    }

    @Nested
    @DisplayName("ESPI Link Management")
    class EspiLinkManagementTest {

        @Test
        @DisplayName("Should initialize with empty link collection")
        void shouldInitializeWithEmptyLinkCollection() {
            // Arrange & Act
            UsagePointEntity usagePoint = new UsagePointEntity();

            // Assert
            assertThat(usagePoint.getRelatedLinks()).isNotNull();
            assertThat(usagePoint.getRelatedLinks()).isEmpty();
        }

        @Test
        @DisplayName("Should add links to collection")
        void shouldAddLinksToCollection() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            LinkType link1 = new LinkType();
            link1.setHref("http://example.com/resource/1");
            link1.setRel("self");
            
            LinkType link2 = new LinkType();
            link2.setHref("http://example.com/resource/2");
            link2.setRel("related");

            // Act
            usagePoint.getRelatedLinks().add(link1);
            usagePoint.getRelatedLinks().add(link2);

            // Assert
            assertThat(usagePoint.getRelatedLinks()).hasSize(2);
            assertThat(usagePoint.getRelatedLinks()).contains(link1, link2);
        }

        @Test
        @DisplayName("Should remove links from collection")
        void shouldRemoveLinksFromCollection() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            LinkType link1 = new LinkType();
            link1.setHref("http://example.com/resource/1");
            link1.setRel("self");
            
            LinkType link2 = new LinkType();
            link2.setHref("http://example.com/resource/2");
            link2.setRel("related");
            
            usagePoint.getRelatedLinks().add(link1);
            usagePoint.getRelatedLinks().add(link2);

            // Act
            usagePoint.getRelatedLinks().remove(link1);

            // Assert
            assertThat(usagePoint.getRelatedLinks()).hasSize(1);
            assertThat(usagePoint.getRelatedLinks()).contains(link2);
            assertThat(usagePoint.getRelatedLinks()).doesNotContain(link1);
        }

        @Test
        @DisplayName("Should clear all links from collection")
        void shouldClearAllLinksFromCollection() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            LinkType link1 = new LinkType();
            link1.setHref("http://example.com/resource/1");
            link1.setRel("self");
            
            LinkType link2 = new LinkType();
            link2.setHref("http://example.com/resource/2");
            link2.setRel("related");
            
            usagePoint.getRelatedLinks().add(link1);
            usagePoint.getRelatedLinks().add(link2);

            // Act
            usagePoint.getRelatedLinks().clear();

            // Assert
            assertThat(usagePoint.getRelatedLinks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class EntityValidationTest {

        @Test
        @DisplayName("Should validate entity with valid data")
        void shouldValidateEntityWithValidData() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            usagePoint.setDescription("Valid Usage Point Description");
            usagePoint.setStatus((short) 1);
            usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);

            // Act
            Set<ConstraintViolation<UsagePointEntity>> violations = validator.validate(usagePoint);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle null description gracefully")
        void shouldHandleNullDescriptionGracefully() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            usagePoint.setDescription(null);
            usagePoint.setStatus((short) 1);
            usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);

            // Act
            Set<ConstraintViolation<UsagePointEntity>> violations = validator.validate(usagePoint);

            // Assert - Description is typically optional in ESPI entities
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCodeTest {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreTheSame() {
            // Arrange
            UUID sharedId = UUID.randomUUID();
            
            UsagePointEntity usagePoint1 = new UsagePointEntity();
            usagePoint1.setId(sharedId);
            usagePoint1.setDescription("First Description");
            
            UsagePointEntity usagePoint2 = new UsagePointEntity();
            usagePoint2.setId(sharedId);
            usagePoint2.setDescription("Second Description");

            // Act & Assert
            assertThat(usagePoint1).isEqualTo(usagePoint2);
            assertThat(usagePoint1.hashCode()).isEqualTo(usagePoint2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            // Arrange
            UsagePointEntity usagePoint1 = new UsagePointEntity();
            usagePoint1.setDescription("Same Description");
            
            UsagePointEntity usagePoint2 = new UsagePointEntity();
            usagePoint2.setDescription("Same Description");

            // Act & Assert
            assertThat(usagePoint1).isNotEqualTo(usagePoint2);
        }

        @Test
        @DisplayName("Should handle null ID comparison")
        void shouldHandleNullIdComparison() {
            // Arrange
            UsagePointEntity usagePoint1 = new UsagePointEntity();
            usagePoint1.setId(null);
            
            UsagePointEntity usagePoint2 = new UsagePointEntity();
            usagePoint2.setId(null);

            // Act & Assert
            assertThat(usagePoint1).isNotEqualTo(usagePoint2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();

            // Act & Assert
            assertThat(usagePoint).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different class")
        void shouldNotBeEqualToDifferentClass() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            CustomerEntity customer = new CustomerEntity();
            customer.setId(usagePoint.getId()); // Same ID but different class

            // Act & Assert
            assertThat(usagePoint).isNotEqualTo(customer);
        }
    }

    @Nested
    @DisplayName("ToString Method")
    class ToStringMethodTest {

        @Test
        @DisplayName("Should generate meaningful toString representation")
        void shouldGenerateMeaningfulToStringRepresentation() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            usagePoint.setDescription("Test Usage Point");
            usagePoint.setStatus((short) 1);

            // Act
            String toString = usagePoint.toString();

            // Assert
            assertThat(toString).isNotNull();
            assertThat(toString).contains("UsagePointEntity");
            assertThat(toString).contains(usagePoint.getId().toString());
        }

        @Test
        @DisplayName("Should handle null values in toString")
        void shouldHandleNullValuesInToString() {
            // Arrange
            UsagePointEntity usagePoint = new UsagePointEntity();
            usagePoint.setDescription(null);

            // Act & Assert
            assertThatCode(usagePoint::toString).doesNotThrowAnyException();
        }
    }
}