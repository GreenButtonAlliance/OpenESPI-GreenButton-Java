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
 * ServiceLocation entity integration tests using MySQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real MySQL database.
 */
@DisplayName("ServiceLocation Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class ServiceLocationMySQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final org.testcontainers.containers.MySQLContainer<?> mysql = mysqlContainer;

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void configureMySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
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
            location.setType("Commercial Building");
            location.setAccessMethod("Security code 1234 at gate");
            location.setSiteAccessProblem("Guard dog on premises");
            location.setNeedsInspection(true);
            location.setGeoInfoReference("GEO-REF-MYSQL-001");
            location.setDirection("North side of Main Street");
            location.setOutageBlock("MYSQL-BLOCK-789");

            // Main address
            Organisation.StreetAddress mainAddress = new Organisation.StreetAddress();
            mainAddress.setStreetDetail("100 MySQL Main Street");
            mainAddress.setTownDetail("MySQL City");
            mainAddress.setStateOrProvince("CA");
            mainAddress.setPostalCode("95000");
            mainAddress.setCountry("USA");
            location.setMainAddress(mainAddress);

            // Secondary address
            Organisation.StreetAddress secondaryAddress = new Organisation.StreetAddress();
            secondaryAddress.setStreetDetail("PO Box 12345");
            secondaryAddress.setTownDetail("MySQL City");
            secondaryAddress.setStateOrProvince("CA");
            secondaryAddress.setPostalCode("95001");
            secondaryAddress.setCountry("USA");
            location.setSecondaryAddress(secondaryAddress);

            // Phone numbers
            Organisation.TelephoneNumber phone1 = new Organisation.TelephoneNumber();
            phone1.setCountryCode("1");
            phone1.setAreaCode("555");
            phone1.setLocalNumber("1234567");
            location.setPhone1(phone1);

            Organisation.TelephoneNumber phone2 = new Organisation.TelephoneNumber();
            phone2.setCountryCode("1");
            phone2.setAreaCode("555");
            phone2.setLocalNumber("7654321");
            phone2.setExt("100");
            location.setPhone2(phone2);

            // Electronic address
            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setEmail1("location@mysql.test");
            electronicAddress.setWeb("https://location.mysql.test");
            location.setElectronicAddress(electronicAddress);

            // Status
            Status status = new Status();
            status.setValue("operational");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("MySQL location test");
            location.setStatus(status);

            // Usage point hrefs
            List<String> usagePointHrefs = new ArrayList<>();
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/100001");
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/100002");
            location.setUsagePointHrefs(usagePointHrefs);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ServiceLocationEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo("Commercial Building");
            assertThat(result.getAccessMethod()).isEqualTo("Security code 1234 at gate");
            assertThat(result.getSiteAccessProblem()).isEqualTo("Guard dog on premises");
            assertThat(result.getNeedsInspection()).isTrue();
            assertThat(result.getGeoInfoReference()).isEqualTo("GEO-REF-MYSQL-001");
            assertThat(result.getDirection()).isEqualTo("North side of Main Street");
            assertThat(result.getOutageBlock()).isEqualTo("MYSQL-BLOCK-789");

            assertThat(result.getMainAddress()).isNotNull();
            assertThat(result.getMainAddress().getStreetDetail()).isEqualTo("100 MySQL Main Street");

            assertThat(result.getSecondaryAddress()).isNotNull();
            assertThat(result.getSecondaryAddress().getStreetDetail()).isEqualTo("PO Box 12345");

            assertThat(result.getPhone1()).isNotNull();
            assertThat(result.getPhone1().getAreaCode()).isEqualTo("555");
            assertThat(result.getPhone1().getLocalNumber()).isEqualTo("1234567");

            assertThat(result.getPhone2()).isNotNull();
            assertThat(result.getPhone2().getExt()).isEqualTo("100");

            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("location@mysql.test");

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("operational");

            assertThat(result.getUsagePointHrefs()).isNotNull();
            assertThat(result.getUsagePointHrefs()).hasSize(2);
            assertThat(result.getUsagePointHrefs().get(0)).contains("UsagePoint/100001");
        }

        @Test
        @DisplayName("Should update service location fields")
        void shouldUpdateServiceLocationFields() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("Original Type");
            location.setNeedsInspection(false);
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();

            // Act
            savedLocation.setType("Updated Type");
            savedLocation.setNeedsInspection(true);
            savedLocation.setAccessMethod("Updated access method");
            ServiceLocationEntity updatedLocation = serviceLocationRepository.save(savedLocation);
            flushAndClear();

            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(updatedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getType()).isEqualTo("Updated Type");
            assertThat(retrieved.get().getNeedsInspection()).isTrue();
            assertThat(retrieved.get().getAccessMethod()).isEqualTo("Updated access method");
        }

        @Test
        @DisplayName("Should delete service location")
        void shouldDeleteServiceLocation() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("Temporary Location");
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
                locations.get(i).setType("MySQL Bulk Location " + i);
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
            location.setType("MySQL Address Test");

            Organisation.StreetAddress mainAddress = new Organisation.StreetAddress();
            mainAddress.setStreetDetail("500 MySQL Complete Street");
            mainAddress.setTownDetail("Complete City");
            mainAddress.setStateOrProvince("TX");
            mainAddress.setPostalCode("75000");
            mainAddress.setCountry("USA");
            location.setMainAddress(mainAddress);

            Organisation.StreetAddress secondaryAddress = new Organisation.StreetAddress();
            secondaryAddress.setStreetDetail("PO Box 999");
            secondaryAddress.setTownDetail("Complete City");
            secondaryAddress.setStateOrProvince("TX");
            secondaryAddress.setPostalCode("75001");
            secondaryAddress.setCountry("USA");
            location.setSecondaryAddress(secondaryAddress);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMainAddress()).isNotNull();
            assertThat(retrieved.get().getMainAddress().getStreetDetail()).isEqualTo("500 MySQL Complete Street");
            assertThat(retrieved.get().getMainAddress().getPostalCode()).isEqualTo("75000");
            assertThat(retrieved.get().getSecondaryAddress()).isNotNull();
            assertThat(retrieved.get().getSecondaryAddress().getStreetDetail()).isEqualTo("PO Box 999");
        }

        @Test
        @DisplayName("Should persist Status with all fields")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("MySQL Status Test");

            Status status = new Status();
            status.setValue("maintenance");
            OffsetDateTime testDateTime = OffsetDateTime.now();
            status.setDateTime(testDateTime);
            status.setRemark("MySQL maintenance mode");
            status.setReason("Scheduled upgrade");
            location.setStatus(status);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Status retrievedStatus = retrieved.get().getStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("maintenance");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getRemark()).isEqualTo("MySQL maintenance mode");
            assertThat(retrievedStatus.getReason()).isEqualTo("Scheduled upgrade");
        }

        @Test
        @DisplayName("Should persist ElectronicAddress with all fields")
        void shouldPersistElectronicAddressWithAllFields() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("MySQL Electronic Address Test");

            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setLan("192.168.1.100");
            electronicAddress.setMac("00:1A:2B:3C:4D:5E");
            electronicAddress.setEmail1("primary@mysql-loc.test");
            electronicAddress.setEmail2("secondary@mysql-loc.test");
            electronicAddress.setWeb("https://mysql-location.test");
            electronicAddress.setRadio("RADIO-LOC-123");
            electronicAddress.setUserID("mysql-user");
            electronicAddress.setPassword("mysql-pass");
            location.setElectronicAddress(electronicAddress);

            // Act
            ServiceLocationEntity savedLocation = serviceLocationRepository.save(location);
            flushAndClear();
            Optional<ServiceLocationEntity> retrieved = serviceLocationRepository.findById(savedLocation.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Organisation.ElectronicAddress retrievedAddress = retrieved.get().getElectronicAddress();
            assertThat(retrievedAddress).isNotNull();
            assertThat(retrievedAddress.getLan()).isEqualTo("192.168.1.100");
            assertThat(retrievedAddress.getMac()).isEqualTo("00:1A:2B:3C:4D:5E");
            assertThat(retrievedAddress.getEmail1()).isEqualTo("primary@mysql-loc.test");
            assertThat(retrievedAddress.getEmail2()).isEqualTo("secondary@mysql-loc.test");
            assertThat(retrievedAddress.getRadio()).isEqualTo("RADIO-LOC-123");
        }

        @Test
        @DisplayName("Should persist UsagePoint href collection")
        void shouldPersistUsagePointHrefCollection() {
            // Arrange
            ServiceLocationEntity location = TestDataBuilders.createValidServiceLocation();
            location.setType("MySQL UsagePoint Href Test");

            List<String> usagePointHrefs = new ArrayList<>();
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/200001");
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/200002");
            usagePointHrefs.add("https://api.example.com/espi/1_1/resource/UsagePoint/200003");
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
            assertThat(retrievedHrefs).contains("https://api.example.com/espi/1_1/resource/UsagePoint/200001");
            assertThat(retrievedHrefs).contains("https://api.example.com/espi/1_1/resource/UsagePoint/200002");
            assertThat(retrievedHrefs).contains("https://api.example.com/espi/1_1/resource/UsagePoint/200003");
        }
    }
}
