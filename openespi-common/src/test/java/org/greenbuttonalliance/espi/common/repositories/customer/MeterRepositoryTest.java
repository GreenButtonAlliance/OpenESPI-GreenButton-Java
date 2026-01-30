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
import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.MeterMultiplier;
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
 * Comprehensive test suite for MeterRepository.
 *
 * Tests all CRUD operations and validation constraints for Meter entities.
 * Per ESPI 4.0 API specification, only default JpaRepository methods are supported.
 */
@DisplayName("Meter Repository Tests")
class MeterRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MeterRepository meterRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve meter successfully")
        void shouldSaveAndRetrieveMeterSuccessfully() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setDescription("Test Electric Meter");
            meter.setSerialNumber("SM-12345");

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m)
                            .extracting(
                                    MeterEntity::getDescription,
                                    MeterEntity::getSerialNumber,
                                    MeterEntity::getIsVirtual
                            )
                            .containsExactly("Test Electric Meter", "SM-12345", false));
        }

        @Test
        @DisplayName("Should find all meters")
        void shouldFindAllMeters() {
            // Arrange
            MeterEntity meter1 = createValidMeter();
            meter1.setSerialNumber("METER-001");
            MeterEntity meter2 = createValidMeter();
            meter2.setSerialNumber("METER-002");
            MeterEntity meter3 = createValidMeter();
            meter3.setSerialNumber("METER-003");

            meterRepository.saveAll(List.of(meter1, meter2, meter3));
            flushAndClear();

            // Act
            List<MeterEntity> allMeters = meterRepository.findAll();

            // Assert
            assertThat(allMeters)
                    .hasSizeGreaterThanOrEqualTo(3)
                    .extracting(MeterEntity::getSerialNumber)
                    .contains("METER-001", "METER-002", "METER-003");
        }

        @Test
        @DisplayName("Should delete meter successfully")
        void shouldDeleteMeterSuccessfully() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setSerialNumber("METER-DELETE");
            MeterEntity saved = meterRepository.save(meter);
            UUID meterId = saved.getId();
            flushAndClear();

            // Act
            meterRepository.deleteById(meterId);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(meterId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if meter exists")
        void shouldCheckIfMeterExists() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setSerialNumber("EXIST-CHECK");
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();

            // Act & Assert
            assertThat(meterRepository.existsById(saved.getId())).isTrue();
            assertThat(meterRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count meters")
        void shouldCountMeters() {
            // Arrange
            long initialCount = meterRepository.count();
            meterRepository.saveAll(List.of(
                    createValidMeter(),
                    createValidMeter(),
                    createValidMeter()
            ));
            flushAndClear();

            // Act & Assert
            assertThat(meterRepository.count()).isEqualTo(initialCount + 3);
        }
    }

    @Nested
    @DisplayName("Meter Specific Field Persistence")
    class MeterSpecificFieldPersistenceTest {

        @Test
        @DisplayName("Should persist all Meter-specific fields correctly")
        void shouldPersistAllMeterSpecificFields() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setFormNumber("FORM-2A");
            meter.setIntervalLength(900L); // 15 minutes

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m)
                            .extracting(
                                    MeterEntity::getFormNumber,
                                    MeterEntity::getIntervalLength
                            )
                            .containsExactly("FORM-2A", 900L));
        }

        @Test
        @DisplayName("Should persist MeterMultiplier collection")
        void shouldPersistMeterMultiplierCollection() {
            // Arrange
            MeterMultiplier multiplier1 = new MeterMultiplier("voltage", new BigDecimal("120.5"));
            MeterMultiplier multiplier2 = new MeterMultiplier("kH", new BigDecimal("7.2"));

            MeterEntity meter = createValidMeter();
            meter.setMeterMultipliers(List.of(multiplier1, multiplier2));

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> {
                        assertThat(m.getMeterMultipliers()).hasSize(2);
                        // BigDecimal assertions use isEqualByComparingTo() for cross-platform precision tolerance
                        assertThat(m.getMeterMultipliers().get(0).getKind()).isEqualTo("voltage");
                        assertThat(m.getMeterMultipliers().get(0).getValue())
                                .isEqualByComparingTo(new BigDecimal("120.5"));
                        assertThat(m.getMeterMultipliers().get(1).getKind()).isEqualTo("kH");
                        assertThat(m.getMeterMultipliers().get(1).getValue())
                                .isEqualByComparingTo(new BigDecimal("7.2"));
                    });
        }

        @Test
        @DisplayName("Should handle empty MeterMultipliers collection")
        void shouldHandleEmptyMeterMultipliersCollection() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setMeterMultipliers(List.of());

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m.getMeterMultipliers()).isEmpty());
        }
    }

    @Nested
    @DisplayName("EndDevice Inherited Field Persistence")
    class EndDeviceInheritedFieldPersistenceTest {

        @Test
        @DisplayName("Should persist all Asset fields inherited through EndDevice")
        void shouldPersistAllAssetFields() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setType("ElectricMeter");
            meter.setUtcNumber("UTC-54321");
            meter.setSerialNumber("SN-ASSET-001");
            meter.setLotNumber("LOT-2025-Q1");
            meter.setPurchasePrice(25000L);
            meter.setCritical(true);
            meter.setInitialCondition("NEW");
            meter.setInitialLossOfLife(BigDecimal.ZERO);

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m)
                            .extracting(
                                    MeterEntity::getType,
                                    MeterEntity::getUtcNumber,
                                    MeterEntity::getSerialNumber,
                                    MeterEntity::getLotNumber,
                                    MeterEntity::getPurchasePrice,
                                    MeterEntity::getCritical,
                                    MeterEntity::getInitialCondition
                            )
                            .containsExactly("ElectricMeter", "UTC-54321", "SN-ASSET-001",
                                    "LOT-2025-Q1", 25000L, true, "NEW"))
                    .hasValueSatisfying(m ->
                            assertThat(m.getInitialLossOfLife()).isEqualByComparingTo(BigDecimal.ZERO));
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

            MeterEntity meter = createValidMeter();
            meter.setLifecycle(lifecycle);

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m.getLifecycle())
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

            MeterEntity meter = createValidMeter();
            meter.setAcceptanceTest(acceptanceTest);

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m.getAcceptanceTest())
                            .isNotNull()
                            .extracting(
                                    Asset.AcceptanceTest::getSuccess,
                                    Asset.AcceptanceTest::getType
                            )
                            .containsExactly(true, "FIELD_TEST"))
                    .hasValueSatisfying(m ->
                            assertThat(m.getAcceptanceTest().getDateTime()).isNotNull());
        }

        @Test
        @DisplayName("Should persist status correctly")
        void shouldPersistStatusCorrectly() {
            // Arrange
            Status status = new Status();
            status.setValue("ACTIVE");
            status.setDateTime(OffsetDateTime.now());
            status.setRemark("Meter operational");
            status.setReason("Installation complete");

            MeterEntity meter = createValidMeter();
            meter.setStatus(status);

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m.getStatus())
                            .isNotNull()
                            .extracting(
                                    Status::getValue,
                                    Status::getRemark,
                                    Status::getReason
                            )
                            .containsExactly("ACTIVE", "Meter operational", "Installation complete"))
                    .hasValueSatisfying(m ->
                            assertThat(m.getStatus().getDateTime()).isNotNull());
        }

        @Test
        @DisplayName("Should persist electronic address correctly")
        void shouldPersistElectronicAddressCorrectly() {
            // Arrange
            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setLan("192.168.1.100");
            electronicAddress.setMac("00:1A:2B:3C:4D:5E");
            electronicAddress.setEmail1("meter@utility.com");
            electronicAddress.setWeb("https://meter.utility.com");

            MeterEntity meter = createValidMeter();
            meter.setElectronicAddress(electronicAddress);

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m.getElectronicAddress())
                            .isNotNull()
                            .extracting(
                                    ElectronicAddress::getLan,
                                    ElectronicAddress::getMac,
                                    ElectronicAddress::getEmail1,
                                    ElectronicAddress::getWeb
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
            MeterEntity virtualMeter = createValidMeter();
            virtualMeter.setIsVirtual(true);

            // Act
            MeterEntity saved = meterRepository.save(virtualMeter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m.getIsVirtual()).isTrue());
        }

        @Test
        @DisplayName("Should persist PAN device flag")
        void shouldPersistPanDeviceFlag() {
            // Arrange
            MeterEntity panMeter = createValidMeter();
            panMeter.setIsPan(true);

            // Act
            MeterEntity saved = meterRepository.save(panMeter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m.getIsPan()).isTrue());
        }

        @Test
        @DisplayName("Should persist install code and AMR system")
        void shouldPersistInstallCodeAndAmrSystem() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setInstallCode("INSTALL-CODE-12345");
            meter.setAmrSystem("ZigBee Smart Energy 2.0");

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(m -> assertThat(m)
                            .extracting(
                                    MeterEntity::getInstallCode,
                                    MeterEntity::getAmrSystem
                            )
                            .containsExactly("INSTALL-CODE-12345", "ZigBee Smart Energy 2.0"));
        }
    }

    /**
     * Helper method to create a valid MeterEntity for testing.
     */
    private MeterEntity createValidMeter() {
        MeterEntity meter = new MeterEntity();
        meter.setSerialNumber("DEFAULT-SN-" + UUID.randomUUID().toString().substring(0, 8));
        meter.setFormNumber("FORM-1");
        meter.setIntervalLength(900L); // 15 minutes default
        meter.setIsVirtual(false);
        meter.setIsPan(false);
        return meter;
    }
}
