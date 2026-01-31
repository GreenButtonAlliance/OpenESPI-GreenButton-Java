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

package org.greenbuttonalliance.espi.common.repositories.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.*;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintViolation;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ServiceLocationRepository.
 * 
 * Tests service location management, all custom query methods,
 * embedded object relationships, and validation constraints.
 */
@DisplayName("Service Location Repository Tests")
class ServiceLocationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ServiceLocationRepository serviceLocationRepository;

    /**
     * Creates a valid ServiceLocationEntity for testing.
     */
    private ServiceLocationEntity createValidServiceLocation() {
        ServiceLocationEntity serviceLocation = new ServiceLocationEntity();
        serviceLocation.setDescription("Test Service Location");
        serviceLocation.setType("Residential");
        serviceLocation.setAccessMethod("Key under mat");
        serviceLocation.setDirection("North side of building");
        serviceLocation.setGeoInfoReference("GPS-" + faker.number().digits(8));
        serviceLocation.setOutageBlock("BLOCK-" + faker.number().digits(4));
        serviceLocation.setNeedsInspection(false);
        
        // Create main address
        StreetAddress mainAddress = new StreetAddress();
        mainAddress.setStreetDetail(faker.address().streetAddress());
        mainAddress.setTownDetail(faker.address().city());
        mainAddress.setStateOrProvince(faker.address().state());
        mainAddress.setPostalCode(faker.address().zipCode());
        mainAddress.setCountry("US");
        serviceLocation.setMainAddress(mainAddress);
        
        return serviceLocation;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve service location successfully")
        void shouldSaveAndRetrieveServiceLocationSuccessfully() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();

            // Act
            ServiceLocationEntity saved = serviceLocationRepository.save(serviceLocation);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Service Location");
            assertThat(retrieved.get().getType()).isEqualTo("Residential");
            assertThat(retrieved.get().getAccessMethod()).isEqualTo("Key under mat");
        }

        @Test
        @DisplayName("Should save service location with embedded addresses")
        void shouldSaveServiceLocationWithEmbeddedAddresses() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();
            
            // Add secondary address
            StreetAddress secondaryAddress = new StreetAddress();
            secondaryAddress.setStreetDetail("PO Box 123");
            secondaryAddress.setTownDetail(faker.address().city());
            secondaryAddress.setStateOrProvince(faker.address().state());
            secondaryAddress.setPostalCode(faker.address().zipCode());
            secondaryAddress.setCountry("US");
            serviceLocation.setSecondaryAddress(secondaryAddress);

            // Act
            ServiceLocationEntity saved = serviceLocationRepository.save(serviceLocation);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ServiceLocationEntity entity = retrieved.get();
            assertThat(entity.getMainAddress()).isNotNull();
            assertThat(entity.getMainAddress().getStreetDetail()).isNotNull();
            assertThat(entity.getSecondaryAddress()).isNotNull();
            assertThat(entity.getSecondaryAddress().getStreetDetail()).isEqualTo("PO Box 123");
        }

        @Test
        @DisplayName("Should save service location with basic fields")
        void shouldSaveServiceLocationWithBasicFields() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();
            serviceLocation.setDescription("Service Location with Basic Fields");
            serviceLocation.setSiteAccessProblem("Gate locked after hours");
            serviceLocation.setNeedsInspection(true);

            // Act
            ServiceLocationEntity saved = serviceLocationRepository.save(serviceLocation);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ServiceLocationEntity entity = retrieved.get();
            assertThat(entity.getDescription()).isEqualTo("Service Location with Basic Fields");
            assertThat(entity.getSiteAccessProblem()).isEqualTo("Gate locked after hours");
            assertThat(entity.getNeedsInspection()).isTrue();
        }

        @Test
        @DisplayName("Should find all service locations")
        void shouldFindAllServiceLocations() {
            // Arrange
            List<ServiceLocationEntity> serviceLocations = List.of(
                createValidServiceLocation(),
                createValidServiceLocation(),
                createValidServiceLocation()
            );
            
            for (int i = 0; i < serviceLocations.size(); i++) {
                serviceLocations.get(i).setDescription("Service Location " + (i + 1));
            }
            
            serviceLocationRepository.saveAll(serviceLocations);
            flushAndClear();

            // Act
            List<ServiceLocationEntity> allServiceLocations = serviceLocationRepository.findAll();

            // Assert
            assertThat(allServiceLocations).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allServiceLocations).extracting(ServiceLocationEntity::getDescription)
                    .contains("Service Location 1", "Service Location 2", "Service Location 3");
        }

        @Test
        @DisplayName("Should delete service location successfully")
        void shouldDeleteServiceLocationSuccessfully() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();
            serviceLocation.setDescription("Service Location to Delete");
            
            ServiceLocationEntity saved = serviceLocationRepository.save(serviceLocation);
            UUID serviceLocationId = saved.getId();
            flushAndClear();

            // Act
            serviceLocationRepository.deleteById(serviceLocationId);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(serviceLocationId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if service location exists")
        void shouldCheckIfServiceLocationExists() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();
            ServiceLocationEntity saved = serviceLocationRepository.save(serviceLocation);
            flushAndClear();

            // Act & Assert
            assertThat(serviceLocationRepository.existsById(saved.getId())).isTrue();
            assertThat(serviceLocationRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count service locations")
        void shouldCountServiceLocations() {
            // Arrange
            long initialCount = serviceLocationRepository.count();
            
            List<ServiceLocationEntity> serviceLocations = List.of(
                createValidServiceLocation(),
                createValidServiceLocation()
            );
            
            serviceLocationRepository.saveAll(serviceLocations);
            flushAndClear();

            // Act
            long finalCount = serviceLocationRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find service locations by outage block")
        void shouldFindServiceLocationsByOutageBlock() {
            // Arrange
            String outageBlock = "BLOCK-TEST-" + faker.number().digits(4);
            
            ServiceLocationEntity location1 = createValidServiceLocation();
            location1.setOutageBlock(outageBlock);
            location1.setDescription("Location in Outage Block 1");
            
            ServiceLocationEntity location2 = createValidServiceLocation();
            location2.setOutageBlock(outageBlock);
            location2.setDescription("Location in Outage Block 2");
            
            ServiceLocationEntity location3 = createValidServiceLocation();
            location3.setOutageBlock("DIFFERENT-BLOCK");
            location3.setDescription("Location in Different Block");

            serviceLocationRepository.saveAll(List.of(location1, location2, location3));
            flushAndClear();

            // Act
            List<ServiceLocationEntity> results = serviceLocationRepository.findByOutageBlock(outageBlock);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ServiceLocationEntity::getDescription)
                    .contains("Location in Outage Block 1", "Location in Outage Block 2");
            assertThat(results).allMatch(loc -> loc.getOutageBlock().equals(outageBlock));
        }

        @Test
        @DisplayName("Should find service locations by type")
        void shouldFindServiceLocationsByType() {
            // Arrange
            ServiceLocationEntity residential1 = createValidServiceLocation();
            residential1.setType("Residential");
            residential1.setDescription("Residential Location 1");
            
            ServiceLocationEntity residential2 = createValidServiceLocation();
            residential2.setType("Residential");
            residential2.setDescription("Residential Location 2");
            
            ServiceLocationEntity commercial = createValidServiceLocation();
            commercial.setType("Commercial");
            commercial.setDescription("Commercial Location");

            serviceLocationRepository.saveAll(List.of(residential1, residential2, commercial));
            flushAndClear();

            // Act
            List<ServiceLocationEntity> results = serviceLocationRepository.findByType("Residential");

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ServiceLocationEntity::getDescription)
                    .contains("Residential Location 1", "Residential Location 2");
            assertThat(results).allMatch(loc -> loc.getType().equals("Residential"));
        }

        @Test
        @DisplayName("Should find service locations by access method")
        void shouldFindServiceLocationsByAccessMethod() {
            // Arrange
            ServiceLocationEntity location1 = createValidServiceLocation();
            location1.setAccessMethod("Key under mat");
            location1.setDescription("Location with Key Access 1");
            
            ServiceLocationEntity location2 = createValidServiceLocation();
            location2.setAccessMethod("Key under mat");
            location2.setDescription("Location with Key Access 2");
            
            ServiceLocationEntity location3 = createValidServiceLocation();
            location3.setAccessMethod("Ring doorbell");
            location3.setDescription("Location with Doorbell Access");

            serviceLocationRepository.saveAll(List.of(location1, location2, location3));
            flushAndClear();

            // Act - Using a simple findAll and filter since there's no specific query method for access method
            List<ServiceLocationEntity> allLocations = serviceLocationRepository.findAll();
            List<ServiceLocationEntity> results = allLocations.stream()
                    .filter(loc -> "Key under mat".equals(loc.getAccessMethod()))
                    .toList();

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ServiceLocationEntity::getDescription)
                    .contains("Location with Key Access 1", "Location with Key Access 2");
        }

        @Test
        @DisplayName("Should find service locations by geo info reference")
        void shouldFindServiceLocationsByGeoInfoReference() {
            // Arrange
            String geoRef = "GPS-TEST-" + faker.number().digits(6);
            
            ServiceLocationEntity location1 = createValidServiceLocation();
            location1.setGeoInfoReference(geoRef);
            location1.setDescription("Location with Geo Reference");
            
            ServiceLocationEntity location2 = createValidServiceLocation();
            location2.setGeoInfoReference("GPS-DIFFERENT-123456");
            location2.setDescription("Location with Different Geo Reference");

            serviceLocationRepository.saveAll(List.of(location1, location2));
            flushAndClear();

            // Act
            List<ServiceLocationEntity> results = serviceLocationRepository.findByGeoInfoReference(geoRef);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDescription()).isEqualTo("Location with Geo Reference");
            assertThat(results.get(0).getGeoInfoReference()).isEqualTo(geoRef);
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Act & Assert
            assertThat(serviceLocationRepository.findByOutageBlock("nonexistent-block")).isEmpty();
            assertThat(serviceLocationRepository.findByType("NonexistentType")).isEmpty();
            assertThat(serviceLocationRepository.findByGeoInfoReference("nonexistent-ref")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Embedded Object Testing")
    class EmbeddedObjectTest {

        @Test
        @DisplayName("Should handle service location with electronic address")
        void shouldHandleServiceLocationWithElectronicAddress() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();
            
            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("test@example.com");
            electronicAddress.setEmail2("backup@example.com");
            electronicAddress.setWeb("https://example.com");
            electronicAddress.setRadio("RADIO-123");
            serviceLocation.setElectronicAddress(electronicAddress);

            // Act
            ServiceLocationEntity saved = serviceLocationRepository.save(serviceLocation);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ServiceLocationEntity entity = retrieved.get();
            assertThat(entity.getElectronicAddress()).isNotNull();
            assertThat(entity.getElectronicAddress().getEmail1()).isEqualTo("test@example.com");
            assertThat(entity.getElectronicAddress().getWeb()).isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("Should handle service location with status")
        void shouldHandleServiceLocationWithStatus() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();
            
            Status status = new Status();
            status.setValue("Active");
            status.setReason("Normal operation");
            serviceLocation.setStatus(status);

            // Act
            ServiceLocationEntity saved = serviceLocationRepository.save(serviceLocation);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ServiceLocationEntity entity = retrieved.get();
            assertThat(entity.getStatus()).isNotNull();
            assertThat(entity.getStatus().getValue()).isEqualTo("Active");
            assertThat(entity.getStatus().getReason()).isEqualTo("Normal operation");
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate service location with valid data")
        void shouldValidateServiceLocationWithValidData() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();

            // Act
            Set<ConstraintViolation<ServiceLocationEntity>> violations = validator.validate(serviceLocation);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle service location with minimal data")
        void shouldHandleServiceLocationWithMinimalData() {
            // Arrange
            ServiceLocationEntity serviceLocation = new ServiceLocationEntity();
            serviceLocation.setDescription("Minimal Service Location");

            // Act
            Set<ConstraintViolation<ServiceLocationEntity>> violations = validator.validate(serviceLocation);

            // Assert - Should be valid with minimal data since most fields are optional
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();
            serviceLocation.setDescription("Service Location for Base Class Test");

            // Act
            ServiceLocationEntity saved = persistAndFlush(serviceLocation);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            ServiceLocationEntity serviceLocation = createValidServiceLocation();

            // Act & Assert - Test basic equals/hashCode functionality
            assertThat(serviceLocation).isEqualTo(serviceLocation); // Same instance should equal itself
            assertThat(serviceLocation.hashCode()).isEqualTo(serviceLocation.hashCode()); // HashCode should be consistent
            assertThat(serviceLocation).isNotEqualTo(null); // Should not equal null
            assertThat(serviceLocation).isNotEqualTo("not a ServiceLocationEntity"); // Should not equal different type
        }
    }
}