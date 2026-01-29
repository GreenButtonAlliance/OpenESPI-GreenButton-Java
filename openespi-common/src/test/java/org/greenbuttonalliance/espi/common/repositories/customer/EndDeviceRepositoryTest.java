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

import org.greenbuttonalliance.espi.common.domain.customer.entity.Asset;
import org.greenbuttonalliance.espi.common.domain.customer.entity.EndDeviceEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for EndDeviceRepository.
 *
 * Tests all CRUD operations and validation constraints for EndDevice entities.
 * Per ESPI 4.0 API specification, only default JpaRepository methods are supported.
 */
@DisplayName("EndDevice Repository Tests")
class EndDeviceRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private EndDeviceRepository endDeviceRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve end device successfully")
        void shouldSaveAndRetrieveEndDeviceSuccessfully() {
            // Arrange
            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setDescription("Test Smart Meter");
            endDevice.setSerialNumber("SM-12345");

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device)
                            .extracting(
                                    EndDeviceEntity::getDescription,
                                    EndDeviceEntity::getSerialNumber,
                                    EndDeviceEntity::getIsVirtual
                            )
                            .containsExactly("Test Smart Meter", "SM-12345", false));
        }

        @Test
        @DisplayName("Should find all end devices")
        void shouldFindAllEndDevices() {
            // Arrange
            EndDeviceEntity device1 = createValidEndDevice();
            device1.setSerialNumber("METER-001");
            EndDeviceEntity device2 = createValidEndDevice();
            device2.setSerialNumber("METER-002");
            EndDeviceEntity device3 = createValidEndDevice();
            device3.setSerialNumber("METER-003");

            endDeviceRepository.saveAll(List.of(device1, device2, device3));
            flushAndClear();

            // Act
            List<EndDeviceEntity> allDevices = endDeviceRepository.findAll();

            // Assert
            assertThat(allDevices)
                    .hasSizeGreaterThanOrEqualTo(3)
                    .extracting(EndDeviceEntity::getSerialNumber)
                    .contains("METER-001", "METER-002", "METER-003");
        }

        @Test
        @DisplayName("Should delete end device successfully")
        void shouldDeleteEndDeviceSuccessfully() {
            // Arrange
            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setSerialNumber("DEVICE-DELETE");
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            UUID deviceId = saved.getId();
            flushAndClear();

            // Act
            endDeviceRepository.deleteById(deviceId);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(deviceId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if end device exists")
        void shouldCheckIfEndDeviceExists() {
            // Arrange
            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setSerialNumber("EXIST-CHECK");
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            flushAndClear();

            // Act & Assert
            assertThat(endDeviceRepository.existsById(saved.getId())).isTrue();
            assertThat(endDeviceRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count end devices")
        void shouldCountEndDevices() {
            // Arrange
            long initialCount = endDeviceRepository.count();
            endDeviceRepository.saveAll(List.of(
                    createValidEndDevice(),
                    createValidEndDevice(),
                    createValidEndDevice()
            ));
            flushAndClear();

            // Act & Assert
            assertThat(endDeviceRepository.count()).isEqualTo(initialCount + 3);
        }
    }

    @Nested
    @DisplayName("Asset Field Persistence")
    class AssetFieldPersistenceTest {

        @Test
        @DisplayName("Should persist all Asset fields correctly")
        void shouldPersistAllAssetFields() {
            // Arrange
            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setType("SmartMeter");
            endDevice.setUtcNumber("UTC-98765");
            endDevice.setSerialNumber("SN-ASSET-001");
            endDevice.setLotNumber("LOT-2025-Q1");
            endDevice.setPurchasePrice(15000L);
            endDevice.setCritical(true);
            endDevice.setInitialCondition("NEW");
            endDevice.setInitialLossOfLife(BigDecimal.ZERO);

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device)
                            .extracting(
                                    EndDeviceEntity::getType,
                                    EndDeviceEntity::getUtcNumber,
                                    EndDeviceEntity::getSerialNumber,
                                    EndDeviceEntity::getLotNumber,
                                    EndDeviceEntity::getPurchasePrice,
                                    EndDeviceEntity::getCritical,
                                    EndDeviceEntity::getInitialCondition
                            )
                            .containsExactly("SmartMeter", "UTC-98765", "SN-ASSET-001",
                                    "LOT-2025-Q1", 15000L, true, "NEW"))
                    .hasValueSatisfying(device ->
                            assertThat(device.getInitialLossOfLife()).isEqualByComparingTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should persist lifecycle dates correctly")
        void shouldPersistLifecycleDatesCorrectly() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            Asset.LifecycleDate lifecycle = new Asset.LifecycleDate();
            lifecycle.setInstallationDate(now.minusDays(30));
            lifecycle.setManufacturedDate(now.minusDays(90));
            lifecycle.setPurchaseDate(now.minusDays(60));
            lifecycle.setReceivedDate(now.minusDays(45));

            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setLifecycle(lifecycle);

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device.getLifecycle())
                            .isNotNull()
                            .extracting(
                                    Asset.LifecycleDate::getInstallationDate,
                                    Asset.LifecycleDate::getManufacturedDate,
                                    Asset.LifecycleDate::getPurchaseDate,
                                    Asset.LifecycleDate::getReceivedDate
                            )
                            .doesNotContainNull());
        }

        @Test
        @DisplayName("Should persist acceptance test correctly")
        void shouldPersistAcceptanceTestCorrectly() {
            // Arrange
            Asset.AcceptanceTest acceptanceTest = new Asset.AcceptanceTest();
            acceptanceTest.setDateTime(OffsetDateTime.now().minusDays(7));
            acceptanceTest.setSuccess(true);
            acceptanceTest.setType("FIELD_TEST");

            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setAcceptanceTest(acceptanceTest);

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device.getAcceptanceTest())
                            .isNotNull()
                            .extracting(
                                    Asset.AcceptanceTest::getSuccess,
                                    Asset.AcceptanceTest::getType
                            )
                            .containsExactly(true, "FIELD_TEST"))
                    .hasValueSatisfying(device ->
                            assertThat(device.getAcceptanceTest().getDateTime()).isNotNull());
        }

        @Test
        @DisplayName("Should persist status correctly")
        void shouldPersistStatusCorrectly() {
            // Arrange
            Status status = new Status();
            status.setValue("ACTIVE");
            status.setDateTime(OffsetDateTime.now());
            status.setRemark("Device operational");
            status.setReason("Installation complete");

            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setStatus(status);

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device.getStatus())
                            .isNotNull()
                            .extracting(
                                    Status::getValue,
                                    Status::getRemark,
                                    Status::getReason
                            )
                            .containsExactly("ACTIVE", "Device operational", "Installation complete"))
                    .hasValueSatisfying(device ->
                            assertThat(device.getStatus().getDateTime()).isNotNull());
        }

        @Test
        @DisplayName("Should persist electronic address correctly")
        void shouldPersistElectronicAddressCorrectly() {
            // Arrange
            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setLan("192.168.1.100");
            electronicAddress.setMac("00:1A:2B:3C:4D:5E");
            electronicAddress.setEmail1("meter@utility.com");
            electronicAddress.setWeb("https://meter.utility.com");

            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setElectronicAddress(electronicAddress);

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device.getElectronicAddress())
                            .isNotNull()
                            .extracting(
                                    Organisation.ElectronicAddress::getLan,
                                    Organisation.ElectronicAddress::getMac,
                                    Organisation.ElectronicAddress::getEmail1,
                                    Organisation.ElectronicAddress::getWeb
                            )
                            .containsExactly("192.168.1.100", "00:1A:2B:3C:4D:5E",
                                    "meter@utility.com", "https://meter.utility.com"));
        }
    }

    @Nested
    @DisplayName("EndDevice Specific Fields")
    class EndDeviceSpecificFieldsTest {

        @Test
        @DisplayName("Should persist virtual device flag")
        void shouldPersistVirtualDeviceFlag() {
            // Arrange
            EndDeviceEntity virtualDevice = createValidEndDevice();
            virtualDevice.setIsVirtual(true);

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(virtualDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device.getIsVirtual()).isTrue());
        }

        @Test
        @DisplayName("Should persist PAN device flag")
        void shouldPersistPanDeviceFlag() {
            // Arrange
            EndDeviceEntity panDevice = createValidEndDevice();
            panDevice.setIsPan(true);

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(panDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device.getIsPan()).isTrue());
        }

        @Test
        @DisplayName("Should persist install code and AMR system")
        void shouldPersistInstallCodeAndAmrSystem() {
            // Arrange
            EndDeviceEntity endDevice = createValidEndDevice();
            endDevice.setInstallCode("INSTALL-CODE-12345");
            endDevice.setAmrSystem("ZigBee Smart Energy 2.0");

            // Act
            EndDeviceEntity saved = endDeviceRepository.save(endDevice);
            flushAndClear();
            Optional<EndDeviceEntity> retrieved = endDeviceRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(device -> assertThat(device)
                            .extracting(
                                    EndDeviceEntity::getInstallCode,
                                    EndDeviceEntity::getAmrSystem
                            )
                            .containsExactly("INSTALL-CODE-12345", "ZigBee Smart Energy 2.0"));
        }
    }

    /**
     * Helper method to create a valid EndDeviceEntity for testing.
     */
    private EndDeviceEntity createValidEndDevice() {
        EndDeviceEntity endDevice = new EndDeviceEntity();
        endDevice.setSerialNumber("DEFAULT-SN-" + UUID.randomUUID().toString().substring(0, 8));
        endDevice.setIsVirtual(false);
        endDevice.setIsPan(false);
        return endDevice;
    }
}
