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

import org.greenbuttonalliance.espi.common.domain.usage.ServiceDeliveryPointEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintViolation;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ServiceDeliveryPointRepository.
 * <p>
 * ServiceDeliveryPoint extends Object (not IdentifiedObject) in ESPI 4.0 XSD,
 * so tests focus on business data fields only (no Atom links or timestamps).
 *
 * Tests all CRUD operations, custom query methods, validation constraints,
 * and business logic methods.
 */
@DisplayName("ServiceDeliveryPoint Repository Tests")
class ServiceDeliveryPointRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ServiceDeliveryPointRepository serviceDeliveryPointRepository;

    /**
     * Creates a valid ServiceDeliveryPointEntity for testing.
     */
    private ServiceDeliveryPointEntity createValidServiceDeliveryPoint() {
        ServiceDeliveryPointEntity sdp = new ServiceDeliveryPointEntity();
        sdp.setName(faker.address().fullAddress());
        sdp.setTariffProfile("TARIFF-" + faker.number().digits(6));
        sdp.setCustomerAgreement("AGREEMENT-" + faker.number().digits(8));
        return sdp;
    }

    /**
     * Creates a ServiceDeliveryPointEntity with minimal required fields.
     */
    private ServiceDeliveryPointEntity createMinimalServiceDeliveryPoint() {
        ServiceDeliveryPointEntity sdp = new ServiceDeliveryPointEntity();
        sdp.setName("Basic Location");
        return sdp;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve service delivery point successfully")
        void shouldSaveAndRetrieveServiceDeliveryPointSuccessfully() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("123 Main Street, Anytown, USA");

            // Act
            ServiceDeliveryPointEntity saved = serviceDeliveryPointRepository.save(sdp);
            flushAndClear();
            Optional<ServiceDeliveryPointEntity> retrieved = serviceDeliveryPointRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getName()).isEqualTo("123 Main Street, Anytown, USA");
        }

        @Test
        @DisplayName("Should update service delivery point successfully")
        void shouldUpdateServiceDeliveryPointSuccessfully() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);

            // Act
            saved.setName("456 Updated Street, New City, USA");
            saved.setTariffProfile("UPDATED-TARIFF-123456");
            ServiceDeliveryPointEntity updated = serviceDeliveryPointRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<ServiceDeliveryPointEntity> retrieved = serviceDeliveryPointRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getName()).isEqualTo("456 Updated Street, New City, USA");
            assertThat(retrieved.get().getTariffProfile()).isEqualTo("UPDATED-TARIFF-123456");
        }

        @Test
        @DisplayName("Should delete service delivery point successfully")
        void shouldDeleteServiceDeliveryPointSuccessfully() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);
            Long savedId = saved.getId();

            // Act
            serviceDeliveryPointRepository.deleteById(savedId);
            flushAndClear();

            // Assert
            Optional<ServiceDeliveryPointEntity> retrieved = serviceDeliveryPointRepository.findById(savedId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should find all service delivery points")
        void shouldFindAllServiceDeliveryPoints() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            sdp1.setName("First Location");
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            sdp2.setName("Second Location");

            persistAndFlush(sdp1);
            persistAndFlush(sdp2);

            // Act
            List<ServiceDeliveryPointEntity> allSdps = serviceDeliveryPointRepository.findAll();

            // Assert
            assertThat(allSdps).hasSizeGreaterThanOrEqualTo(2);
            assertThat(allSdps)
                .extracting(ServiceDeliveryPointEntity::getName)
                .contains("First Location", "Second Location");
        }

        @Test
        @DisplayName("Should count service delivery points correctly")
        void shouldCountServiceDeliveryPointsCorrectly() {
            // Arrange
            long initialCount = serviceDeliveryPointRepository.count();
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();

            persistAndFlush(sdp1);
            persistAndFlush(sdp2);

            // Act
            long finalCount = serviceDeliveryPointRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();

            ServiceDeliveryPointEntity saved1 = persistAndFlush(sdp1);
            ServiceDeliveryPointEntity saved2 = persistAndFlush(sdp2);

            // Act
            List<Long> allIds = serviceDeliveryPointRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }
    }

    @Nested
    @DisplayName("Validation Testing")
    class ValidationTest {

        @Test
        @DisplayName("Should validate name length constraint")
        void shouldValidateNameLengthConstraint() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("x".repeat(257)); // Exceeds 256 character limit

            // Act
            Set<ConstraintViolation<ServiceDeliveryPointEntity>> violations = validator.validate(sdp);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Service delivery point name cannot exceed 256 characters");
        }

        @Test
        @DisplayName("Should validate tariff profile length constraint")
        void shouldValidateTariffProfileLengthConstraint() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setTariffProfile("x".repeat(257)); // Exceeds 256 character limit

            // Act
            Set<ConstraintViolation<ServiceDeliveryPointEntity>> violations = validator.validate(sdp);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Tariff profile cannot exceed 256 characters");
        }

        @Test
        @DisplayName("Should validate customer agreement length constraint")
        void shouldValidateCustomerAgreementLengthConstraint() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setCustomerAgreement("x".repeat(257)); // Exceeds 256 character limit

            // Act
            Set<ConstraintViolation<ServiceDeliveryPointEntity>> violations = validator.validate(sdp);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Customer agreement cannot exceed 256 characters");
        }

        @Test
        @DisplayName("Should accept valid field lengths")
        void shouldAcceptValidFieldLengths() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("x".repeat(256)); // Exactly 256 characters
            sdp.setTariffProfile("x".repeat(256)); // Exactly 256 characters
            sdp.setCustomerAgreement("x".repeat(256)); // Exactly 256 characters

            // Act
            Set<ConstraintViolation<ServiceDeliveryPointEntity>> violations = validator.validate(sdp);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept null optional fields")
        void shouldAcceptNullOptionalFields() {
            // Arrange
            ServiceDeliveryPointEntity sdp = new ServiceDeliveryPointEntity();
            sdp.setName("Valid Name");
            sdp.setTariffProfile(null);
            sdp.setCustomerAgreement(null);

            // Act
            Set<ConstraintViolation<ServiceDeliveryPointEntity>> violations = validator.validate(sdp);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Business Logic Methods")
    class BusinessLogicTest {

        @Test
        @DisplayName("Should generate correct display name with name")
        void shouldGenerateCorrectDisplayNameWithName() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("  123 Main Street  ");

            // Act
            String displayName = sdp.getDisplayName();

            // Assert
            assertThat(displayName).isEqualTo("123 Main Street");
        }

        @Test
        @DisplayName("Should generate display name with ID when name is null")
        void shouldGenerateDisplayNameWithIdWhenNameIsNull() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName(null);
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);

            // Act
            String displayName = saved.getDisplayName();

            // Assert
            assertThat(displayName).isEqualTo("Service Delivery Point " + saved.getId());
        }

        @Test
        @DisplayName("Should generate display name with ID when name is empty")
        void shouldGenerateDisplayNameWithIdWhenNameIsEmpty() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("   ");
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);

            // Act
            String displayName = saved.getDisplayName();

            // Assert
            assertThat(displayName).contains("Service Delivery Point");
        }

        @Test
        @DisplayName("Should correctly identify when tariff profile is present")
        void shouldCorrectlyIdentifyWhenTariffProfileIsPresent() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            sdp1.setTariffProfile("RESIDENTIAL-STANDARD");

            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            sdp2.setTariffProfile(null);

            ServiceDeliveryPointEntity sdp3 = createValidServiceDeliveryPoint();
            sdp3.setTariffProfile("   ");

            // Act & Assert
            assertThat(sdp1.hasTariffProfile()).isTrue();
            assertThat(sdp2.hasTariffProfile()).isFalse();
            assertThat(sdp3.hasTariffProfile()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify when customer agreement is present")
        void shouldCorrectlyIdentifyWhenCustomerAgreementIsPresent() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            sdp1.setCustomerAgreement("AGREEMENT-12345");

            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            sdp2.setCustomerAgreement(null);

            ServiceDeliveryPointEntity sdp3 = createValidServiceDeliveryPoint();
            sdp3.setCustomerAgreement("   ");

            // Act & Assert
            assertThat(sdp1.hasCustomerAgreement()).isTrue();
            assertThat(sdp2.hasCustomerAgreement()).isFalse();
            assertThat(sdp3.hasCustomerAgreement()).isFalse();
        }

        @Test
        @DisplayName("Should validate service delivery point with name")
        void shouldValidateServiceDeliveryPointWithName() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("Valid Location");

            // Act
            boolean isValid = sdp.isValid();

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should invalidate service delivery point without name")
        void shouldInvalidateServiceDeliveryPointWithoutName() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName(null);

            // Act
            boolean isValid = sdp.isValid();

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should invalidate service delivery point with empty name")
        void shouldInvalidateServiceDeliveryPointWithEmptyName() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("   ");

            // Act
            boolean isValid = sdp.isValid();

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Entity Persistence")
    class EntityPersistenceTest {

        @Test
        @DisplayName("Should persist and retrieve by ID")
        void shouldPersistAndRetrieveById() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();

            // Act
            ServiceDeliveryPointEntity saved = serviceDeliveryPointRepository.save(sdp);
            flushAndClear();

            // Assert
            Optional<ServiceDeliveryPointEntity> retrieved = serviceDeliveryPointRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();

            ServiceDeliveryPointEntity entity = retrieved.get();
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getName()).isNotNull();
        }

        @Test
        @DisplayName("Should generate unique IDs for different entities")
        void shouldGenerateUniqueIdsForDifferentEntities() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();

            // Act
            ServiceDeliveryPointEntity saved1 = serviceDeliveryPointRepository.save(sdp1);
            ServiceDeliveryPointEntity saved2 = serviceDeliveryPointRepository.save(sdp2);
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
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();

            ServiceDeliveryPointEntity saved1 = persistAndFlush(sdp1);
            ServiceDeliveryPointEntity saved2 = persistAndFlush(sdp2);

            // Act & Assert
            assertThat(saved1).isNotEqualTo(saved2);

            // Same entity should be equal to itself
            assertThat(saved1).isEqualTo(saved1);
            assertThat(saved1.hashCode()).isEqualTo(saved1.hashCode());

            // Different entities with different IDs should not be equal
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        }

        @Test
        @DisplayName("Should generate meaningful toString representation")
        void shouldGenerateMeaningfulToStringRepresentation() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("Test Location");
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);

            // Act
            String toString = saved.toString();

            // Assert
            assertThat(toString).contains("ServiceDeliveryPointEntity");
            assertThat(toString).contains("id = " + saved.getId());
            assertThat(toString).contains("name = Test Location");
        }
    }
}
