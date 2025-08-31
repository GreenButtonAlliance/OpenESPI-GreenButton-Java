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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ServiceDeliveryPointRepository.
 * 
 * Tests all CRUD operations, 5 custom query methods, service delivery location testing,
 * validation constraints, business logic methods, and IdentifiedObject base functionality.
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
        sdp.setDescription("Test Service Delivery Point - " + faker.lorem().sentence(3));
        sdp.setMrid("SDP-" + faker.number().digits(8));
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
        sdp.setDescription("Minimal Service Delivery Point");
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
            sdp.setDescription("Test Service Delivery Point for CRUD");
            sdp.setName("123 Main Street, Anytown, USA");

            // Act
            ServiceDeliveryPointEntity saved = serviceDeliveryPointRepository.save(sdp);
            flushAndClear();
            Optional<ServiceDeliveryPointEntity> retrieved = serviceDeliveryPointRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Service Delivery Point for CRUD");
            assertThat(retrieved.get().getName()).isEqualTo("123 Main Street, Anytown, USA");
            assertThat(retrieved.get().getMrid()).isNotNull();
        }

        @Test
        @DisplayName("Should update service delivery point successfully")
        void shouldUpdateServiceDeliveryPointSuccessfully() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);

            // Act
            saved.setDescription("Updated Service Delivery Point Description");
            saved.setName("456 Updated Street, New City, USA");
            saved.setTariffProfile("UPDATED-TARIFF-123456");
            ServiceDeliveryPointEntity updated = serviceDeliveryPointRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<ServiceDeliveryPointEntity> retrieved = serviceDeliveryPointRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Updated Service Delivery Point Description");
            assertThat(retrieved.get().getName()).isEqualTo("456 Updated Street, New City, USA");
            assertThat(retrieved.get().getTariffProfile()).isEqualTo("UPDATED-TARIFF-123456");
        }

        @Test
        @DisplayName("Should delete service delivery point successfully")
        void shouldDeleteServiceDeliveryPointSuccessfully() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);
            UUID savedId = saved.getId();

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
        @DisplayName("Should find service delivery points by name")
        void shouldFindServiceDeliveryPointsByName() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            sdp1.setName("Unique Location Name");
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            sdp2.setName("Unique Location Name"); // Same name
            ServiceDeliveryPointEntity sdp3 = createValidServiceDeliveryPoint();
            sdp3.setName("Different Location Name");
            
            persistAndFlush(sdp1);
            persistAndFlush(sdp2);
            persistAndFlush(sdp3);

            // Act
            List<ServiceDeliveryPointEntity> sdps = serviceDeliveryPointRepository.findByName("Unique Location Name");

            // Assert
            assertThat(sdps).hasSize(2);
            assertThat(sdps).extracting(ServiceDeliveryPointEntity::getName)
                .allMatch(name -> name.equals("Unique Location Name"));
        }

        @Test
        @DisplayName("Should find service delivery points by tariff profile")
        void shouldFindServiceDeliveryPointsByTariffProfile() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            sdp1.setTariffProfile("RESIDENTIAL-STANDARD");
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            sdp2.setTariffProfile("RESIDENTIAL-STANDARD"); // Same tariff
            ServiceDeliveryPointEntity sdp3 = createValidServiceDeliveryPoint();
            sdp3.setTariffProfile("COMMERCIAL-BASIC");
            
            persistAndFlush(sdp1);
            persistAndFlush(sdp2);
            persistAndFlush(sdp3);

            // Act
            List<ServiceDeliveryPointEntity> sdps = serviceDeliveryPointRepository.findByTariffProfile("RESIDENTIAL-STANDARD");

            // Assert
            assertThat(sdps).hasSize(2);
            assertThat(sdps).extracting(ServiceDeliveryPointEntity::getTariffProfile)
                .allMatch(tariff -> tariff.equals("RESIDENTIAL-STANDARD"));
        }

        @Test
        @DisplayName("Should find service delivery points by customer agreement")
        void shouldFindServiceDeliveryPointsByCustomerAgreement() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            sdp1.setCustomerAgreement("AGREEMENT-12345");
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            sdp2.setCustomerAgreement("AGREEMENT-12345"); // Same agreement
            ServiceDeliveryPointEntity sdp3 = createValidServiceDeliveryPoint();
            sdp3.setCustomerAgreement("AGREEMENT-67890");
            
            persistAndFlush(sdp1);
            persistAndFlush(sdp2);
            persistAndFlush(sdp3);

            // Act
            List<ServiceDeliveryPointEntity> sdps = serviceDeliveryPointRepository.findByCustomerAgreement("AGREEMENT-12345");

            // Assert
            assertThat(sdps).hasSize(2);
            assertThat(sdps).extracting(ServiceDeliveryPointEntity::getCustomerAgreement)
                .allMatch(agreement -> agreement.equals("AGREEMENT-12345"));
        }

        @Test
        @DisplayName("Should find service delivery points by name containing text")
        void shouldFindServiceDeliveryPointsByNameContaining() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            sdp1.setName("Main Street Office Building");
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            sdp2.setName("Oak Street Residential");
            ServiceDeliveryPointEntity sdp3 = createValidServiceDeliveryPoint();
            sdp3.setName("Pine Avenue Commercial");
            
            persistAndFlush(sdp1);
            persistAndFlush(sdp2);
            persistAndFlush(sdp3);

            // Act
            List<ServiceDeliveryPointEntity> sdps = serviceDeliveryPointRepository.findByNameContaining("Street");

            // Assert
            assertThat(sdps).hasSize(2);
            assertThat(sdps).extracting(ServiceDeliveryPointEntity::getName)
                .allMatch(name -> name.toLowerCase().contains("street"));
        }

        @Test
        @DisplayName("Should count service delivery points by tariff profile")
        void shouldCountServiceDeliveryPointsByTariffProfile() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            sdp1.setTariffProfile("COMMERCIAL-PREMIUM");
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            sdp2.setTariffProfile("COMMERCIAL-PREMIUM");
            ServiceDeliveryPointEntity sdp3 = createValidServiceDeliveryPoint();
            sdp3.setTariffProfile("COMMERCIAL-PREMIUM");
            ServiceDeliveryPointEntity sdp4 = createValidServiceDeliveryPoint();
            sdp4.setTariffProfile("RESIDENTIAL-BASIC");
            
            persistAndFlush(sdp1);
            persistAndFlush(sdp2);
            persistAndFlush(sdp3);
            persistAndFlush(sdp4);

            // Act
            Long count = serviceDeliveryPointRepository.countByTariffProfile("COMMERCIAL-PREMIUM");

            // Assert
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            ServiceDeliveryPointEntity sdp1 = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity sdp2 = createValidServiceDeliveryPoint();
            
            ServiceDeliveryPointEntity saved1 = persistAndFlush(sdp1);
            ServiceDeliveryPointEntity saved2 = persistAndFlush(sdp2);

            // Act
            List<UUID> allIds = serviceDeliveryPointRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }
    }

    @Nested
    @DisplayName("Validation Testing")
    class ValidationTest {

        @Test
        @DisplayName("Should validate mRID length constraint")
        void shouldValidateMridLengthConstraint() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setMrid("x".repeat(65)); // Exceeds 64 character limit

            // Act
            Set<ConstraintViolation<ServiceDeliveryPointEntity>> violations = validator.validate(sdp);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("ServiceDeliveryPoint mRID cannot exceed 64 characters");
        }

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
            sdp.setMrid("x".repeat(64)); // Exactly 64 characters
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
            sdp.setDescription("Test SDP with null fields");
            sdp.setMrid(null);
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
            sdp.setMrid("SDP-12345");

            // Act
            String displayName = sdp.getDisplayName();

            // Assert
            assertThat(displayName).isEqualTo("123 Main Street");
        }

        @Test
        @DisplayName("Should generate display name with mRID when name is null")
        void shouldGenerateDisplayNameWithMridWhenNameIsNull() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName(null);
            sdp.setMrid("SDP-67890");

            // Act
            String displayName = sdp.getDisplayName();

            // Assert
            assertThat(displayName).isEqualTo("Service Delivery Point SDP-67890");
        }

        @Test
        @DisplayName("Should generate display name with mRID when name is empty")
        void shouldGenerateDisplayNameWithMridWhenNameIsEmpty() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("   ");
            sdp.setMrid("SDP-11111");

            // Act
            String displayName = sdp.getDisplayName();

            // Assert
            assertThat(displayName).isEqualTo("Service Delivery Point SDP-11111");
        }

        @Test
        @DisplayName("Should generate default display name when both name and mRID are null")
        void shouldGenerateDefaultDisplayNameWhenBothNameAndMridAreNull() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName(null);
            sdp.setMrid(null);

            // Act
            String displayName = sdp.getDisplayName();

            // Assert
            assertThat(displayName).isEqualTo("Service Delivery Point Unknown");
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
            sdp.setMrid(null);

            // Act
            boolean isValid = sdp.isValid();

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should validate service delivery point with mRID")
        void shouldValidateServiceDeliveryPointWithMrid() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName(null);
            sdp.setMrid("SDP-12345");

            // Act
            boolean isValid = sdp.isValid();

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should invalidate service delivery point without name or mRID")
        void shouldInvalidateServiceDeliveryPointWithoutNameOrMrid() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName(null);
            sdp.setMrid(null);

            // Act
            boolean isValid = sdp.isValid();

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should invalidate service delivery point with empty name and mRID")
        void shouldInvalidateServiceDeliveryPointWithEmptyNameAndMrid() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            sdp.setName("   ");
            sdp.setMrid("   ");

            // Act
            boolean isValid = sdp.isValid();

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
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
            assertThat(entity.getCreated()).isNotNull();
            assertThat(entity.getUpdated()).isNotNull();
            assertThat(entity.getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should update timestamps on modification")
        void shouldUpdateTimestampsOnModification() {
            // Arrange
            ServiceDeliveryPointEntity sdp = createValidServiceDeliveryPoint();
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);
            
            // Wait a moment to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            saved.setDescription("Updated Description");
            ServiceDeliveryPointEntity updated = serviceDeliveryPointRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<ServiceDeliveryPointEntity> retrieved = serviceDeliveryPointRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUpdated()).isAfter(retrieved.get().getCreated());
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
            // Note: Hibernate proxy-aware hashCode implementation returns class hashCode for different entities
            // This is expected behavior for entities with different IDs
            
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
            sdp.setMrid("SDP-12345");
            sdp.setName("Test Location");
            ServiceDeliveryPointEntity saved = persistAndFlush(sdp);

            // Act
            String toString = saved.toString();

            // Assert
            assertThat(toString).contains("ServiceDeliveryPointEntity");
            assertThat(toString).contains("id = " + saved.getId());
            assertThat(toString).contains("mrid = SDP-12345");
            assertThat(toString).contains("name = Test Location");
        }
    }
}