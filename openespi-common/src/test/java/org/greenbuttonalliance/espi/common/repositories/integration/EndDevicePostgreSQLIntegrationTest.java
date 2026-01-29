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

import org.greenbuttonalliance.espi.common.domain.customer.entity.Asset;
import org.greenbuttonalliance.espi.common.domain.customer.entity.EndDeviceEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.repositories.customer.EndDeviceRepository;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EndDevice entity integration tests using PostgreSQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real PostgreSQL database.
 */
@DisplayName("EndDevice Integration Tests - PostgreSQL")
@ActiveProfiles({"test", "test-postgresql"})
class EndDevicePostgreSQLIntegrationTest extends BaseTestContainersTest {

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
    private EndDeviceRepository endDeviceRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve end device with all fields")
        void shouldSaveAndRetrieveEndDeviceWithAllFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setType("Advanced Metering Infrastructure");
            endDevice.setSerialNumber("POSTGRES-SN-987654321");
            endDevice.setUtcNumber("POSTGRES-UTC-123456789");
            endDevice.setLotNumber("LOT-POSTGRES-002");
            endDevice.setPurchasePrice(75000L);
            endDevice.setCritical(false);
            endDevice.setInitialCondition("Refurbished");
            endDevice.setInitialLossOfLife(BigDecimal.valueOf(0.15));
            endDevice.setIsVirtual(true);
            endDevice.setIsPan(false);
            endDevice.setInstallCode("INSTALL-PG-ABC");
            endDevice.setAmrSystem("AMR-POSTGRES-SYSTEM");

            // Electronic address
            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setEmail1("device@postgres.test");
            electronicAddress.setMac("00:11:22:33:44:55");
            electronicAddress.setWeb("https://postgres-device.test");
            endDevice.setElectronicAddress(electronicAddress);

            // Lifecycle dates
            Asset.LifecycleDate lifecycle = new Asset.LifecycleDate();
            lifecycle.setManufacturedDate(OffsetDateTime.now().minusYears(3));
            lifecycle.setPurchaseDate(OffsetDateTime.now().minusYears(2));
            lifecycle.setInstallationDate(OffsetDateTime.now().minusMonths(3));
            endDevice.setLifecycle(lifecycle);

            // Acceptance test
            Asset.AcceptanceTest acceptanceTest = new Asset.AcceptanceTest();
            acceptanceTest.setSuccess(true);
            acceptanceTest.setDateTime(OffsetDateTime.now().minusMonths(3));
            acceptanceTest.setType("Integration Test");
            endDevice.setAcceptanceTest(acceptanceTest);

            // Status
            Status status = new Status();
            status.setValue("active");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("PostgreSQL device test");
            endDevice.setStatus(status);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            EndDeviceEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo("Advanced Metering Infrastructure");
            assertThat(result.getSerialNumber()).isEqualTo("POSTGRES-SN-987654321");
            assertThat(result.getUtcNumber()).isEqualTo("POSTGRES-UTC-123456789");
            assertThat(result.getLotNumber()).isEqualTo("LOT-POSTGRES-002");
            assertThat(result.getPurchasePrice()).isEqualTo(75000L);
            assertThat(result.getCritical()).isFalse();
            assertThat(result.getInitialCondition()).isEqualTo("Refurbished");
            assertThat(result.getIsVirtual()).isTrue();
            assertThat(result.getIsPan()).isFalse();
            assertThat(result.getInstallCode()).isEqualTo("INSTALL-PG-ABC");
            assertThat(result.getAmrSystem()).isEqualTo("AMR-POSTGRES-SYSTEM");

            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("device@postgres.test");
            assertThat(result.getElectronicAddress().getMac()).isEqualTo("00:11:22:33:44:55");

            assertThat(result.getLifecycle()).isNotNull();
            assertThat(result.getLifecycle().getManufacturedDate()).isNotNull();
            assertThat(result.getLifecycle().getInstallationDate()).isNotNull();

            assertThat(result.getAcceptanceTest()).isNotNull();
            assertThat(result.getAcceptanceTest().getSuccess()).isTrue();
            assertThat(result.getAcceptanceTest().getType()).isEqualTo("Integration Test");

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("active");
        }

        @Test
        @DisplayName("Should update end device fields")
        void shouldUpdateEndDeviceFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("ORIGINAL-PG-SN-001");
            endDevice.setIsPan(false);
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();

            // Act
            savedDevice.setSerialNumber("UPDATED-PG-SN-002");
            savedDevice.setIsPan(true);
            savedDevice.setInstallCode("UPDATED-PG-INSTALL-CODE");
            EndDeviceEntity updatedDevice = endDeviceRepository.save(savedDevice);
            flushAndClear();

            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(updatedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getSerialNumber()).isEqualTo("UPDATED-PG-SN-002");
            assertThat(retrieved.get().getIsPan()).isTrue();
            assertThat(retrieved.get().getInstallCode()).isEqualTo("UPDATED-PG-INSTALL-CODE");
        }

        @Test
        @DisplayName("Should delete end device")
        void shouldDeleteEndDevice() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("DELETE-ME-PG-SN");
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();

            // Act
            endDeviceRepository.deleteById(savedDevice.getId());
            flushAndClear();

            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

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
            List<EndDeviceEntity> devices = TestDataBuilders.createValidEntities(5,
                TestDataBuilders::createValidEndDevice);

            for (int i = 0; i < devices.size(); i++) {
                devices.get(i).setSerialNumber("POSTGRES-BULK-SN-" + i);
            }

            // Act
            List<EndDeviceEntity> savedDevices = endDeviceRepository.saveAll(devices);
            flushAndClear();

            // Assert
            assertThat(savedDevices).hasSize(5);
            assertThat(savedDevices).allMatch(device -> device.getId() != null);

            long count = endDeviceRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should handle bulk delete operations")
        void shouldHandleBulkDeleteOperations() {
            // Arrange
            List<EndDeviceEntity> devices = TestDataBuilders.createValidEntities(3,
                TestDataBuilders::createValidEndDevice);

            List<EndDeviceEntity> savedDevices = endDeviceRepository.saveAll(devices);
            long initialCount = endDeviceRepository.count();
            flushAndClear();

            // Act
            endDeviceRepository.deleteAll(savedDevices);
            flushAndClear();

            // Assert
            long finalCount = endDeviceRepository.count();
            assertThat(finalCount).isEqualTo(initialCount - 3);
        }
    }

    @Nested
    @DisplayName("Embedded Objects Persistence")
    class EmbeddedObjectsTest {

        @Test
        @DisplayName("Should persist LifecycleDate with all fields")
        void shouldPersistLifecycleDateWithAllFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("POSTGRES-LIFECYCLE-TEST");

            Asset.LifecycleDate lifecycle = new Asset.LifecycleDate();
            lifecycle.setManufacturedDate(OffsetDateTime.now().minusYears(6));
            lifecycle.setPurchaseDate(OffsetDateTime.now().minusYears(5));
            lifecycle.setReceivedDate(OffsetDateTime.now().minusYears(4));
            lifecycle.setInstallationDate(OffsetDateTime.now().minusYears(3));
            lifecycle.setRemovalDate(OffsetDateTime.now().minusYears(2));
            lifecycle.setRetirementDate(OffsetDateTime.now().minusYears(1));
            endDevice.setLifecycle(lifecycle);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Asset.LifecycleDate retrievedLifecycle = retrieved.get().getLifecycle();
            assertThat(retrievedLifecycle).isNotNull();
            assertThat(retrievedLifecycle.getManufacturedDate()).isNotNull();
            assertThat(retrievedLifecycle.getPurchaseDate()).isNotNull();
            assertThat(retrievedLifecycle.getReceivedDate()).isNotNull();
            assertThat(retrievedLifecycle.getInstallationDate()).isNotNull();
            assertThat(retrievedLifecycle.getRemovalDate()).isNotNull();
            assertThat(retrievedLifecycle.getRetirementDate()).isNotNull();
        }

        @Test
        @DisplayName("Should persist AcceptanceTest with all fields")
        void shouldPersistAcceptanceTestWithAllFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("POSTGRES-ACCEPTANCE-TEST");

            Asset.AcceptanceTest acceptanceTest = new Asset.AcceptanceTest();
            acceptanceTest.setSuccess(true);
            acceptanceTest.setDateTime(OffsetDateTime.now().minusWeeks(2));
            acceptanceTest.setType("Quality Assurance Test");
            endDevice.setAcceptanceTest(acceptanceTest);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Asset.AcceptanceTest retrievedTest = retrieved.get().getAcceptanceTest();
            assertThat(retrievedTest).isNotNull();
            assertThat(retrievedTest.getSuccess()).isTrue();
            assertThat(retrievedTest.getDateTime()).isNotNull();
            assertThat(retrievedTest.getType()).isEqualTo("Quality Assurance Test");
        }

        @Test
        @DisplayName("Should persist Status with all fields")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("POSTGRES-STATUS-TEST");

            Status status = new Status();
            status.setValue("retired");
            OffsetDateTime testDateTime = OffsetDateTime.now();
            status.setDateTime(testDateTime);
            status.setRemark("PostgreSQL device retirement");
            status.setReason("End of service life");
            endDevice.setStatus(status);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Status retrievedStatus = retrieved.get().getStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("retired");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getRemark()).isEqualTo("PostgreSQL device retirement");
            assertThat(retrievedStatus.getReason()).isEqualTo("End of service life");
        }

        @Test
        @DisplayName("Should persist ElectronicAddress with all fields")
        void shouldPersistElectronicAddressWithAllFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("POSTGRES-ELECTRONIC-TEST");

            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setLan("10.10.10.100");
            electronicAddress.setMac("FF:EE:DD:CC:BB:AA");
            electronicAddress.setEmail1("device1@postgres.test");
            electronicAddress.setEmail2("device2@postgres.test");
            electronicAddress.setWeb("https://device.postgres.test");
            electronicAddress.setRadio("RADIO-PG-DEVICE-123");
            electronicAddress.setUserID("postgres-device-user");
            electronicAddress.setPassword("postgres-device-pass");
            endDevice.setElectronicAddress(electronicAddress);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Organisation.ElectronicAddress retrievedAddress = retrieved.get().getElectronicAddress();
            assertThat(retrievedAddress).isNotNull();
            assertThat(retrievedAddress.getLan()).isEqualTo("10.10.10.100");
            assertThat(retrievedAddress.getMac()).isEqualTo("FF:EE:DD:CC:BB:AA");
            assertThat(retrievedAddress.getEmail1()).isEqualTo("device1@postgres.test");
            assertThat(retrievedAddress.getEmail2()).isEqualTo("device2@postgres.test");
            assertThat(retrievedAddress.getRadio()).isEqualTo("RADIO-PG-DEVICE-123");
        }
    }
}