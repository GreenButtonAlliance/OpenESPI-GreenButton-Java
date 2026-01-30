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
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.EndDeviceEntity;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
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
 * EndDevice entity integration tests using MySQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real MySQL database.
 */
@DisplayName("EndDevice Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class EndDeviceMySQLIntegrationTest extends BaseTestContainersTest {

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
    private EndDeviceRepository endDeviceRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve end device with all fields")
        void shouldSaveAndRetrieveEndDeviceWithAllFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setType("Smart Meter");
            endDevice.setSerialNumber("MYSQL-SN-123456789");
            endDevice.setUtcNumber("MYSQL-UTC-987654321");
            endDevice.setLotNumber("LOT-MYSQL-001");
            endDevice.setPurchasePrice(50000L);
            endDevice.setCritical(true);
            endDevice.setInitialCondition("New");
            endDevice.setInitialLossOfLife(BigDecimal.ZERO);
            endDevice.setIsVirtual(false);
            endDevice.setIsPan(true);
            endDevice.setInstallCode("INSTALL-MYSQL-XYZ");
            endDevice.setAmrSystem("AMR-MYSQL-SYSTEM");

            // Electronic address
            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("device@mysql.test");
            electronicAddress.setMac("AA:BB:CC:DD:EE:FF");
            electronicAddress.setWeb("https://mysql-device.test");
            endDevice.setElectronicAddress(electronicAddress);

            // Lifecycle dates
            Asset.LifecycleDate lifecycle = new Asset.LifecycleDate();
            lifecycle.setManufacturedDate(OffsetDateTime.now().minusYears(2));
            lifecycle.setPurchaseDate(OffsetDateTime.now().minusYears(1));
            lifecycle.setInstallationDate(OffsetDateTime.now().minusMonths(6));
            endDevice.setLifecycle(lifecycle);

            // Acceptance test
            Asset.AcceptanceTest acceptanceTest = new Asset.AcceptanceTest();
            acceptanceTest.setSuccess(true);
            acceptanceTest.setDateTime(OffsetDateTime.now().minusMonths(6));
            acceptanceTest.setType("Field Test");
            endDevice.setAcceptanceTest(acceptanceTest);

            // Status
            Status status = new Status();
            status.setValue("operational");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("MySQL device test");
            endDevice.setStatus(status);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            EndDeviceEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo("Smart Meter");
            assertThat(result.getSerialNumber()).isEqualTo("MYSQL-SN-123456789");
            assertThat(result.getUtcNumber()).isEqualTo("MYSQL-UTC-987654321");
            assertThat(result.getLotNumber()).isEqualTo("LOT-MYSQL-001");
            assertThat(result.getPurchasePrice()).isEqualTo(50000L);
            assertThat(result.getCritical()).isTrue();
            assertThat(result.getInitialCondition()).isEqualTo("New");
            assertThat(result.getIsVirtual()).isFalse();
            assertThat(result.getIsPan()).isTrue();
            assertThat(result.getInstallCode()).isEqualTo("INSTALL-MYSQL-XYZ");
            assertThat(result.getAmrSystem()).isEqualTo("AMR-MYSQL-SYSTEM");

            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("device@mysql.test");
            assertThat(result.getElectronicAddress().getMac()).isEqualTo("AA:BB:CC:DD:EE:FF");

            assertThat(result.getLifecycle()).isNotNull();
            assertThat(result.getLifecycle().getManufacturedDate()).isNotNull();
            assertThat(result.getLifecycle().getInstallationDate()).isNotNull();

            assertThat(result.getAcceptanceTest()).isNotNull();
            assertThat(result.getAcceptanceTest().getSuccess()).isTrue();
            assertThat(result.getAcceptanceTest().getType()).isEqualTo("Field Test");

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("operational");
        }

        @Test
        @DisplayName("Should update end device fields")
        void shouldUpdateEndDeviceFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("ORIGINAL-SN-001");
            endDevice.setIsVirtual(false);
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();

            // Act
            savedDevice.setSerialNumber("UPDATED-SN-002");
            savedDevice.setIsVirtual(true);
            savedDevice.setInstallCode("UPDATED-INSTALL-CODE");
            EndDeviceEntity updatedDevice = endDeviceRepository.save(savedDevice);
            flushAndClear();

            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(updatedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getSerialNumber()).isEqualTo("UPDATED-SN-002");
            assertThat(retrieved.get().getIsVirtual()).isTrue();
            assertThat(retrieved.get().getInstallCode()).isEqualTo("UPDATED-INSTALL-CODE");
        }

        @Test
        @DisplayName("Should delete end device")
        void shouldDeleteEndDevice() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("DELETE-ME-SN");
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
                devices.get(i).setSerialNumber("MYSQL-BULK-SN-" + i);
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
            endDevice.setSerialNumber("MYSQL-LIFECYCLE-TEST");

            Asset.LifecycleDate lifecycle = new Asset.LifecycleDate();
            lifecycle.setManufacturedDate(OffsetDateTime.now().minusYears(5));
            lifecycle.setPurchaseDate(OffsetDateTime.now().minusYears(4));
            lifecycle.setReceivedDate(OffsetDateTime.now().minusYears(3));
            lifecycle.setInstallationDate(OffsetDateTime.now().minusYears(2));
            lifecycle.setRemovalDate(OffsetDateTime.now().minusYears(1));
            lifecycle.setRetirementDate(OffsetDateTime.now().minusMonths(6));
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
            endDevice.setSerialNumber("MYSQL-ACCEPTANCE-TEST");

            Asset.AcceptanceTest acceptanceTest = new Asset.AcceptanceTest();
            acceptanceTest.setSuccess(false);
            acceptanceTest.setDateTime(OffsetDateTime.now().minusMonths(1));
            acceptanceTest.setType("Factory Test");
            endDevice.setAcceptanceTest(acceptanceTest);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Asset.AcceptanceTest retrievedTest = retrieved.get().getAcceptanceTest();
            assertThat(retrievedTest).isNotNull();
            assertThat(retrievedTest.getSuccess()).isFalse();
            assertThat(retrievedTest.getDateTime()).isNotNull();
            assertThat(retrievedTest.getType()).isEqualTo("Factory Test");
        }

        @Test
        @DisplayName("Should persist Status with all fields")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("MYSQL-STATUS-TEST");

            Status status = new Status();
            status.setValue("maintenance");
            OffsetDateTime testDateTime = OffsetDateTime.now();
            status.setDateTime(testDateTime);
            status.setRemark("MySQL device maintenance");
            status.setReason("Scheduled maintenance");
            endDevice.setStatus(status);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Status retrievedStatus = retrieved.get().getStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("maintenance");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getRemark()).isEqualTo("MySQL device maintenance");
            assertThat(retrievedStatus.getReason()).isEqualTo("Scheduled maintenance");
        }

        @Test
        @DisplayName("Should persist ElectronicAddress with all fields")
        void shouldPersistElectronicAddressWithAllFields() {
            // Arrange
            EndDeviceEntity endDevice = TestDataBuilders.createValidEndDevice();
            endDevice.setSerialNumber("MYSQL-ELECTRONIC-TEST");

            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setLan("192.168.100.50");
            electronicAddress.setMac("11:22:33:44:55:66");
            electronicAddress.setEmail1("device1@mysql.test");
            electronicAddress.setEmail2("device2@mysql.test");
            electronicAddress.setWeb("https://device.mysql.test");
            electronicAddress.setRadio("RADIO-DEVICE-789");
            electronicAddress.setUserID("device-user");
            electronicAddress.setPassword("device-pass");
            endDevice.setElectronicAddress(electronicAddress);

            // Act
            EndDeviceEntity savedDevice = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(savedDevice.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ElectronicAddress retrievedAddress = retrieved.get().getElectronicAddress();
            assertThat(retrievedAddress).isNotNull();
            assertThat(retrievedAddress.getLan()).isEqualTo("192.168.100.50");
            assertThat(retrievedAddress.getMac()).isEqualTo("11:22:33:44:55:66");
            assertThat(retrievedAddress.getEmail1()).isEqualTo("device1@mysql.test");
            assertThat(retrievedAddress.getEmail2()).isEqualTo("device2@mysql.test");
            assertThat(retrievedAddress.getRadio()).isEqualTo("RADIO-DEVICE-789");
        }
    }
}