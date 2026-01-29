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

package org.greenbuttonalliance.espi.common.repositories.integration;

import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.ServiceLocationEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.repositories.customer.ServiceLocationRepository;
import org.greenbuttonalliance.espi.common.test.BaseTestContainersTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceLocation entity integration tests using PostgreSQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real PostgreSQL database.
 */
@DisplayName("ServiceLocation Integration Tests - PostgreSQL")
@ActiveProfiles({"test", "test-postgresql"})
class ServiceLocationPostgreSQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final org.testcontainers.containers.PostgreSQLContainer<?> postgres = postgresqlContainer;

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configurePostgreSQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private ServiceLocationRepository serviceLocationRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve service location with all fields")
        void shouldSaveAndRetrieveServiceLocationWithAllFields() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("Residential Property");
            location.setAccessMethod("Key under mat");
            location.setSiteAccessProblem("Narrow driveway");
            location.setNeedsInspection(false);
            location.setGeoInfoReference("GEO-REF-PG-002");
            location.setDirection("South side of Elm Street");
            location.setOutageBlock("POSTGRES-BLOCK-456");

            // Main address
            Organisation.StreetAddress mainAddress = new Organisation.StreetAddress();
            mainAddress.setStreetDetail("200 PostgreSQL Avenue");
            mainAddress.setTownDetail("Postgres Town");
            mainAddress.setStateOrProvince("WA");
            mainAddress.setPostalCode("98000");
            mainAddress.setCountry("USA");
            location.setMainAddress(mainAddress);

            // Secondary address
            Organisation.StreetAddress secondaryAddress = new Organisation.StreetAddress();
            secondaryAddress.setStreetDetail("PO Box 54321");
            secondaryAddress.setTownDetail("Postgres Town");
            secondaryAddress.setStateOrProvince("WA");
            secondaryAddress.setPostalCode("98001");
            secondaryAddress.setCountry("USA");
            location.setSecondaryAddress(secondaryAddress);

            // Phone numbers
            Organisation.TelephoneNumber phone1 = new Organisation.TelephoneNumber();
            phone1.setCountryCode("1");
            phone1.setAreaCode("206");
            phone1.setLocalNumber("9876543");
            location.setPhone1(phone1);

            Organisation.TelephoneNumber phone2 = new Organisation.TelephoneNumber();
            phone2.setCountryCode("1");
            phone2.setAreaCode("206");
            phone2.setLocalNumber("3456789");
            phone2.setExt("200");
            location.setPhone2(phone2);

            // Electronic address
            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setEmail1("location@postgres.test");
            electronicAddress.setWeb("https://location.postgres.test");
            location.setElectronicAddress(electronicAddress);

            // Status
            Status status = new Status();
            status.setValue("active");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("PostgreSQL location test");
            location.setStatus(status);

            // Usage point hrefs
            List<String> usagePointHrefs = new ArrayList<>();
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/300001");
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/300002");
            location.setUsagePointHrefs(usagePointHrefs);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ServiceLocationEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo("Residential Property");
            assertThat(result.getAccessMethod()).isEqualTo("Key under mat");
            assertThat(result.getSiteAccessProblem()).isEqualTo("Narrow driveway");
            assertThat(result.getNeedsInspection()).isFalse();
            assertThat(result.getGeoInfoReference()).isEqualTo("GEO-REF-PG-002");
            assertThat(result.getDirection()).isEqualTo("South side of Elm Street");
            assertThat(result.getOutageBlock()).isEqualTo("POSTGRES-BLOCK-456");

            assertThat(result.getMainAddress()).isNotNull();
            assertThat(result.getMainAddress().getStreetDetail()).isEqualTo("200 PostgreSQL Avenue");

            assertThat(result.getSecondaryAddress()).isNotNull();
            assertThat(result.getSecondaryAddress().getStreetDetail()).isEqualTo("PO Box 54321");

            assertThat(result.getPhone1()).isNotNull();
            assertThat(result.getPhone1().getAreaCode()).isEqualTo("206");
            assertThat(result.getPhone1().getLocalNumber()).isEqualTo("9876543");

            assertThat(result.getPhone2()).isNotNull();
            assertThat(result.getPhone2().getExt()).isEqualTo("200");

            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("location@postgres.test");

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("active");

            assertThat(result.getUsagePointHrefs()).isNotNull();
            assertThat(result.getUsagePointHrefs()).hasSize(2);
            assertThat(result.getUsagePointHrefs().get(0)).contains("UsagePoint/300001");
        }

        @Test
        @DisplayName("Should update service location fields")
        void shouldUpdateServiceLocationFields() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("Original PostgreSQL Type");
            location.setNeedsInspection(true);
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();

            // Act
            savedLocation.setType("Updated PostgreSQL Type");
            savedLocation.setNeedsInspection(false);
            savedLocation.setAccessMethod("Updated PostgreSQL access method");
            ServiceLocationEntity updatedLocation = serviceLocationRepository.save(savedLocation);
            flushAndClear();

            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(updatedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getType()).isEqualTo("Updated PostgreSQL Type");
            assertThat(retrieved.get().getNeedsInspection()).isFalse();
            assertThat(retrieved.get().getAccessMethod()).isEqualTo("Updated PostgreSQL access method");
        }

        @Test
        @DisplayName("Should delete service location")
        void shouldDeleteServiceLocation() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("Temporary PostgreSQL Location");
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();

            // Act
            serviceLocationRepository.deleteById(savedLocation.getId());
            flushAndClear();

            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isEmpty();
        }
    }

    @Nested
    @DisplayName("Bulk Operations")
    class BulkOperationsTest {

        @Test
        @DisplayName("Should handle bulk save operations")
        void shouldHandleBulkSaveOperations() {
            // Arrange
            List<ServiceLocationEntity> locations = TestDataBuilders.createValidEntities(5,
                TestDataBuilders::createValidServiceLocation);

            for (int i = 0; i < locations.size(); i++) {
                locations.get(i).setType("PostgreSQL Bulk Location " + i);
            }

            // Act
            List<ServiceLocationEntity> savedLocations = serviceLocationRepository.saveAll(locations);
            flushAndClear();

            // Assert
            assertThat(savedLocations).hasSize(5);
            assertThat(savedLocations).allMatch(location -> location.getId() != null);

            long count = serviceLocationRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should handle bulk delete operations")
        void shouldHandleBulkDeleteOperations() {
            // Arrange
            List<ServiceLocationEntity> locations = TestDataBuilders.createValidEntities(3,
                TestDataBuilders::createValidServiceLocation);

            List<ServiceLocationEntity> savedLocations = serviceLocationRepository.saveAll(locations);
            long initialCount = serviceLocationRepository.count();
            flushAndClear();

            // Act
            serviceLocationRepository.deleteAll(savedLocations);
            flushAndClear();

            // Assert
            long finalCount = serviceLocationRepository.count();
            assertThat(finalCount).isEqualTo(initialCount - 3);
        }
    }

    @Nested
    @DisplayName("Embedded Objects Persistence")
    class EmbeddedObjectsTest {

        @Test
        @DisplayName("Should persist and retrieve addresses with all fields")
        void shouldPersistAddressesWithAllFields() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("PostgreSQL Address Test");

            Organisation.StreetAddress mainAddress = new Organisation.StreetAddress();
            mainAddress.setStreetDetail("600 PostgreSQL Complete Boulevard");
            mainAddress.setTownDetail("Complete Town");
            mainAddress.setStateOrProvince("OR");
            mainAddress.setPostalCode("97000");
            mainAddress.setCountry("USA");
            location.setMainAddress(mainAddress);

            Organisation.StreetAddress secondaryAddress = new Organisation.StreetAddress();
            secondaryAddress.setStreetDetail("PO Box 111");
            secondaryAddress.setTownDetail("Complete Town");
            secondaryAddress.setStateOrProvince("OR");
            secondaryAddress.setPostalCode("97001");
            secondaryAddress.setCountry("USA");
            location.setSecondaryAddress(secondaryAddress);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMainAddress()).isNotNull();
            assertThat(retrieved.get().getMainAddress().getStreetDetail()).isEqualTo("600 PostgreSQL Complete Boulevard");
            assertThat(retrieved.get().getMainAddress().getPostalCode()).isEqualTo("97000");
            assertThat(retrieved.get().getSecondaryAddress()).isNotNull();
            assertThat(retrieved.get().getSecondaryAddress().getStreetDetail()).isEqualTo("PO Box 111");
        }

        @Test
        @DisplayName("Should persist Status with all fields")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("PostgreSQL Status Test");

            Status status = new Status();
            status.setValue("inactive");
            OffsetDateTime testDateTime = OffsetDateTime.now();
            status.setDateTime(testDateTime);
            status.setRemark("PostgreSQL inactive mode");
            status.setReason("Seasonal closure");
            location.setStatus(status);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Status retrievedStatus = retrieved.get().getStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("inactive");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getRemark()).isEqualTo("PostgreSQL inactive mode");
            assertThat(retrievedStatus.getReason()).isEqualTo("Seasonal closure");
        }

        @Test
        @DisplayName("Should persist ElectronicAddress with all fields")
        void shouldPersistElectronicAddressWithAllFields() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("PostgreSQL Electronic Address Test");

            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setLan("10.0.0.100");
            electronicAddress.setMac("AA:BB:CC:DD:EE:FF");
            electronicAddress.setEmail1("primary@postgres-loc.test");
            electronicAddress.setEmail2("secondary@postgres-loc.test");
            electronicAddress.setWeb("https://postgres-location.test");
            electronicAddress.setRadio("RADIO-PG-LOC-456");
            electronicAddress.setUserID("postgres-user");
            electronicAddress.setPassword("postgres-pass");
            location.setElectronicAddress(electronicAddress);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Organisation.ElectronicAddress retrievedAddress = retrieved.get().getElectronicAddress();
            assertThat(retrievedAddress).isNotNull();
            assertThat(retrievedAddress.getLan()).isEqualTo("10.0.0.100");
            assertThat(retrievedAddress.getMac()).isEqualTo("AA:BB:CC:DD:EE:FF");
            assertThat(retrievedAddress.getEmail1()).isEqualTo("primary@postgres-loc.test");
            assertThat(retrievedAddress.getEmail2()).isEqualTo("secondary@postgres-loc.test");
            assertThat(retrievedAddress.getRadio()).isEqualTo("RADIO-PG-LOC-456");
        }

        @Test
        @DisplayName("Should persist UsagePoint href collection")
        void shouldPersistUsagePointHrefCollection() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("PostgreSQL UsagePoint Href Test");

            List<String> usagePointHrefs = new ArrayList<>();
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/400001");
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/400002");
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/400003");
            location.setUsagePointHrefs(usagePointHrefs);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            List<String> retrievedHrefs = retrieved.get().getUsagePointHrefs();
            assertThat(retrievedHrefs).isNotNull();
            assertThat(retrievedHrefs).hasSize(3);
            assertThat(retrievedHrefs).contains("https://api.example.com/espi/1_1/resource/UsagePoint/400001");
            assertThat(retrievedHrefs).contains("https://api.example.com/espi/1_1/resource/UsagePoint/400002");
            assertThat(retrievedHrefs).contains("https://api.example.com/espi/1_1/resource/UsagePoint/400003");
        }
    }
}