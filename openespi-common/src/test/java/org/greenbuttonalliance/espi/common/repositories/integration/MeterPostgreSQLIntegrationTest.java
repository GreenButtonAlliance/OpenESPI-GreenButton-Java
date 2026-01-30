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
import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.MeterMultiplier;
import org.greenbuttonalliance.espi.common.repositories.customer.MeterRepository;
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
 * Meter entity integration tests using PostgreSQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real PostgreSQL database.
 */
@DisplayName("Meter Integration Tests - PostgreSQL")
@ActiveProfiles({"test", "test-postgresql"})
class MeterPostgreSQLIntegrationTest extends BaseTestContainersTest {

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
    private MeterRepository meterRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve meter with all fields")
        void shouldSaveAndRetrieveMeterWithAllFields() {
            // Arrange
            MeterEntity meter = TestDataBuilders.createValidMeter();
            meter.setType("Electric Meter");
            meter.setSerialNumber("PGSQL-METER-SN-123456789");
            meter.setUtcNumber("PGSQL-UTC-987654321");
            meter.setLotNumber("LOT-PGSQL-M001");
            meter.setPurchasePrice(25000L);
            meter.setCritical(true);
            meter.setInitialCondition("New");
            meter.setInitialLossOfLife(BigDecimal.ZERO);
            meter.setIsVirtual(false);
            meter.setIsPan(true);
            meter.setInstallCode("INSTALL-PGSQL-METER-XYZ");
            meter.setAmrSystem("AMR-PGSQL-METER-SYSTEM");

            // Meter-specific fields
            meter.setFormNumber("FORM-2A");
            meter.setIntervalLength(900L); // 15 minutes

            // MeterMultipliers collection
            MeterMultiplier multiplier1 = new MeterMultiplier("voltage", new BigDecimal("120.0"));
            MeterMultiplier multiplier2 = new MeterMultiplier("kH", new BigDecimal("7.2"));
            meter.setMeterMultipliers(List.of(multiplier1, multiplier2));

            // Electronic address
            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("meter@pgsql.test");
            electronicAddress.setMac("11:22:33:44:55:66");
            electronicAddress.setWeb("https://pgsql-meter.test");
            meter.setElectronicAddress(electronicAddress);

            // Lifecycle dates
            Asset.LifecycleDate lifecycle = new Asset.LifecycleDate();
            lifecycle.setManufacturedDate(OffsetDateTime.now().minusYears(2));
            lifecycle.setPurchaseDate(OffsetDateTime.now().minusYears(1));
            lifecycle.setInstallationDate(OffsetDateTime.now().minusMonths(6));
            meter.setLifecycle(lifecycle);

            // Acceptance test
            Asset.AcceptanceTest acceptanceTest = new Asset.AcceptanceTest();
            acceptanceTest.setSuccess(true);
            acceptanceTest.setDateTime(OffsetDateTime.now().minusMonths(6));
            acceptanceTest.setType("Field Test");
            meter.setAcceptanceTest(acceptanceTest);

            // Status
            Status status = new Status();
            status.setValue("operational");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("PostgreSQL meter test");
            meter.setStatus(status);

            // Act
            MeterEntity savedMeter = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(savedMeter.getId());

            // Assert
            assertThat(retrieved).isPresent();
            MeterEntity result = retrieved.get();

            // EndDevice inherited fields
            assertThat(result.getType()).isEqualTo("Electric Meter");
            assertThat(result.getSerialNumber()).isEqualTo("PGSQL-METER-SN-123456789");
            assertThat(result.getUtcNumber()).isEqualTo("PGSQL-UTC-987654321");
            assertThat(result.getLotNumber()).isEqualTo("LOT-PGSQL-M001");
            assertThat(result.getPurchasePrice()).isEqualTo(25000L);
            assertThat(result.getCritical()).isTrue();
            assertThat(result.getInitialCondition()).isEqualTo("New");
            assertThat(result.getIsVirtual()).isFalse();
            assertThat(result.getIsPan()).isTrue();
            assertThat(result.getInstallCode()).isEqualTo("INSTALL-PGSQL-METER-XYZ");
            assertThat(result.getAmrSystem()).isEqualTo("AMR-PGSQL-METER-SYSTEM");

            // Meter-specific fields
            assertThat(result.getFormNumber()).isEqualTo("FORM-2A");
            assertThat(result.getIntervalLength()).isEqualTo(900L);

            // MeterMultipliers collection
            assertThat(result.getMeterMultipliers()).hasSize(2);
            assertThat(result.getMeterMultipliers().get(0).getKind()).isEqualTo("voltage");
            assertThat(result.getMeterMultipliers().get(0).getValue()).isEqualByComparingTo(new BigDecimal("120.0"));
            assertThat(result.getMeterMultipliers().get(1).getKind()).isEqualTo("kH");
            assertThat(result.getMeterMultipliers().get(1).getValue()).isEqualByComparingTo(new BigDecimal("7.2"));

            // Embedded objects
            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("meter@pgsql.test");
            assertThat(result.getElectronicAddress().getMac()).isEqualTo("11:22:33:44:55:66");

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
        @DisplayName("Should update meter fields")
        void shouldUpdateMeterFields() {
            // Arrange
            MeterEntity meter = TestDataBuilders.createValidMeter();
            meter.setSerialNumber("ORIGINAL-METER-SN-001");
            meter.setFormNumber("FORM-1");
            meter.setIntervalLength(600L);
            meter.setIsVirtual(false);
            MeterEntity savedMeter = meterRepository.save(meter);
            flushAndClear();

            // Act
            savedMeter.setSerialNumber("UPDATED-METER-SN-002");
            savedMeter.setFormNumber("FORM-3B");
            savedMeter.setIntervalLength(1800L);
            savedMeter.setIsVirtual(true);
            savedMeter.setInstallCode("UPDATED-INSTALL-CODE");
            MeterEntity updatedMeter = meterRepository.save(savedMeter);
            flushAndClear();

            Optional<MeterEntity> retrieved = meterRepository.findById(updatedMeter.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getSerialNumber()).isEqualTo("UPDATED-METER-SN-002");
            assertThat(retrieved.get().getFormNumber()).isEqualTo("FORM-3B");
            assertThat(retrieved.get().getIntervalLength()).isEqualTo(1800L);
            assertThat(retrieved.get().getIsVirtual()).isTrue();
            assertThat(retrieved.get().getInstallCode()).isEqualTo("UPDATED-INSTALL-CODE");
        }

        @Test
        @DisplayName("Should delete meter")
        void shouldDeleteMeter() {
            // Arrange
            MeterEntity meter = TestDataBuilders.createValidMeter();
            meter.setSerialNumber("DELETE-ME-METER-SN");
            MeterEntity savedMeter = meterRepository.save(meter);
            flushAndClear();

            // Act
            meterRepository.deleteById(savedMeter.getId());
            flushAndClear();

            Optional<MeterEntity> retrieved = meterRepository.findById(savedMeter.getId());

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
            List<MeterEntity> meters = TestDataBuilders.createValidEntities(5,
                TestDataBuilders::createValidMeter);

            for (int i = 0; i < meters.size(); i++) {
                meters.get(i).setSerialNumber("PGSQL-BULK-METER-SN-" + i);
                meters.get(i).setFormNumber("FORM-BULK-" + i);
            }

            // Act
            List<MeterEntity> savedMeters = meterRepository.saveAll(meters);
            flushAndClear();

            // Assert
            assertThat(savedMeters).hasSize(5);
            assertThat(savedMeters).allMatch(meter -> meter.getId() != null);

            long count = meterRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should handle bulk delete operations")
        void shouldHandleBulkDeleteOperations() {
            // Arrange
            List<MeterEntity> meters = TestDataBuilders.createValidEntities(3,
                TestDataBuilders::createValidMeter);

            List<MeterEntity> savedMeters = meterRepository.saveAll(meters);
            long initialCount = meterRepository.count();
            flushAndClear();

            // Act
            meterRepository.deleteAll(savedMeters);
            flushAndClear();

            // Assert
            long finalCount = meterRepository.count();
            assertThat(finalCount).isEqualTo(initialCount - 3);
        }
    }

    @Nested
    @DisplayName("MeterMultiplier Collection Persistence")
    class MeterMultiplierPersistenceTest {

        @Test
        @DisplayName("Should persist meter with multiple multipliers")
        void shouldPersistMeterWithMultipleMultipliers() {
            // Arrange
            MeterEntity meter = TestDataBuilders.createValidMeter();
            meter.setSerialNumber("PGSQL-MULT-SN-001");

            MeterMultiplier mult1 = new MeterMultiplier("voltage", new BigDecimal("240.0"));
            MeterMultiplier mult2 = new MeterMultiplier("current", new BigDecimal("5.0"));
            MeterMultiplier mult3 = new MeterMultiplier("kH", new BigDecimal("1.8"));
            meter.setMeterMultipliers(List.of(mult1, mult2, mult3));

            // Act
            MeterEntity savedMeter = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(savedMeter.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMeterMultipliers()).hasSize(3);
            assertThat(retrieved.get().getMeterMultipliers())
                    .extracting(MeterMultiplier::getKind)
                    .containsExactly("voltage", "current", "kH");
            // BigDecimal assertions use isEqualByComparingTo() for cross-platform precision tolerance
            assertThat(retrieved.get().getMeterMultipliers().get(0).getValue())
                    .isEqualByComparingTo(new BigDecimal("240.0"));
            assertThat(retrieved.get().getMeterMultipliers().get(1).getValue())
                    .isEqualByComparingTo(new BigDecimal("5.0"));
            assertThat(retrieved.get().getMeterMultipliers().get(2).getValue())
                    .isEqualByComparingTo(new BigDecimal("1.8"));
        }

        @Test
        @DisplayName("Should update meter multipliers collection")
        void shouldUpdateMeterMultipliersCollection() {
            // Arrange
            MeterEntity meter = TestDataBuilders.createValidMeter();
            meter.setSerialNumber("PGSQL-MULT-UPDATE-001");
            meter.setMeterMultipliers(List.of(
                new MeterMultiplier("voltage", new BigDecimal("120.0"))
            ));
            MeterEntity savedMeter = meterRepository.save(meter);
            flushAndClear();

            // Act
            savedMeter.setMeterMultipliers(List.of(
                new MeterMultiplier("voltage", new BigDecimal("240.0")),
                new MeterMultiplier("current", new BigDecimal("10.0"))
            ));
            meterRepository.save(savedMeter);
            flushAndClear();

            Optional<MeterEntity> retrieved = meterRepository.findById(savedMeter.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMeterMultipliers()).hasSize(2);
            assertThat(retrieved.get().getMeterMultipliers().get(0).getKind()).isEqualTo("voltage");
            assertThat(retrieved.get().getMeterMultipliers().get(0).getValue())
                    .isEqualByComparingTo(new BigDecimal("240.0"));
            assertThat(retrieved.get().getMeterMultipliers().get(1).getKind()).isEqualTo("current");
            assertThat(retrieved.get().getMeterMultipliers().get(1).getValue())
                    .isEqualByComparingTo(new BigDecimal("10.0"));
        }

        @Test
        @DisplayName("Should handle empty meter multipliers collection")
        void shouldHandleEmptyMeterMultipliersCollection() {
            // Arrange
            MeterEntity meter = TestDataBuilders.createValidMeter();
            meter.setSerialNumber("PGSQL-MULT-EMPTY-001");
            meter.setMeterMultipliers(List.of());

            // Act
            MeterEntity savedMeter = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(savedMeter.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMeterMultipliers()).isEmpty();
        }
    }
}
