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

import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for MeterRepository.
 * 
 * Tests all CRUD operations, 10 custom query methods, meter device field testing,
 * EndDeviceEntity inheritance testing, and IdentifiedObject base functionality.
 */
@DisplayName("Meter Repository Tests")
class MeterRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MeterRepository meterRepository;

    /**
     * Creates a valid MeterEntity for testing.
     */
    private MeterEntity createValidMeter() {
        MeterEntity meter = new MeterEntity();
        meter.setDescription("Test Meter - " + faker.lorem().sentence(3));
        
        // MeterEntity specific fields
        meter.setFormNumber("FORM-" + faker.number().digits(4));
        meter.setIntervalLength(faker.number().numberBetween(300L, 3600L)); // 5 minutes to 1 hour
        
        // EndDeviceEntity inherited fields
        meter.setType("ELECTRIC_METER");
        meter.setUtcNumber("UTC-" + faker.number().digits(8));
        meter.setSerialNumber("SN-" + faker.number().digits(10));
        meter.setLotNumber("LOT-" + faker.number().digits(6));
        meter.setPurchasePrice(faker.number().numberBetween(50000L, 500000L)); // $500-$5000
        meter.setCritical(faker.bool().bool());
        meter.setInitialCondition("NEW");
        meter.setInitialLossOfLife(BigDecimal.ZERO);
        meter.setIsVirtual(false);
        meter.setIsPan(faker.bool().bool());
        meter.setInstallCode("INSTALL-" + faker.number().digits(8));
        meter.setAmrSystem("AMR-" + faker.company().name());
        
        return meter;
    }

    /**
     * Creates a virtual meter for testing.
     */
    private MeterEntity createVirtualMeter() {
        MeterEntity meter = createValidMeter();
        meter.setIsVirtual(true);
        meter.setType("VIRTUAL_METER");
        meter.setSerialNumber("VIRTUAL-" + faker.number().digits(8));
        return meter;
    }

    /**
     * Creates a PAN device meter for testing.
     */
    private MeterEntity createPanMeter() {
        MeterEntity meter = createValidMeter();
        meter.setIsPan(true);
        meter.setType("PAN_DEVICE");
        meter.setInstallCode("PAN-" + faker.number().digits(8));
        return meter;
    }

    /**
     * Creates a critical meter for testing.
     */
    private MeterEntity createCriticalMeter() {
        MeterEntity meter = createValidMeter();
        meter.setCritical(true);
        meter.setType("CRITICAL_METER");
        return meter;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve meter successfully")
        void shouldSaveAndRetrieveMeterSuccessfully() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setDescription("Test Meter for CRUD");
            meter.setSerialNumber("CRUD-TEST-12345");

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Meter for CRUD");
            assertThat(retrieved.get().getSerialNumber()).isEqualTo("CRUD-TEST-12345");
        }

        @Test
        @DisplayName("Should update meter successfully")
        void shouldUpdateMeterSuccessfully() {
            // Arrange
            MeterEntity meter = createValidMeter();
            MeterEntity saved = persistAndFlush(meter);

            // Act
            saved.setDescription("Updated Meter Description");
            saved.setFormNumber("UPDATED-FORM-9999");
            saved.setIntervalLength(1800L); // 30 minutes
            MeterEntity updated = meterRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<MeterEntity> retrieved = meterRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Updated Meter Description");
            assertThat(retrieved.get().getFormNumber()).isEqualTo("UPDATED-FORM-9999");
            assertThat(retrieved.get().getIntervalLength()).isEqualTo(1800L);
        }

        @Test
        @DisplayName("Should delete meter successfully")
        void shouldDeleteMeterSuccessfully() {
            // Arrange
            MeterEntity meter = createValidMeter();
            MeterEntity saved = persistAndFlush(meter);
            UUID savedId = saved.getId();

            // Act
            meterRepository.deleteById(savedId);
            flushAndClear();

            // Assert
            Optional<MeterEntity> retrieved = meterRepository.findById(savedId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should find all meters")
        void shouldFindAllMeters() {
            // Arrange
            MeterEntity meter1 = createValidMeter();
            meter1.setSerialNumber("METER-001");
            MeterEntity meter2 = createValidMeter();
            meter2.setSerialNumber("METER-002");
            
            persistAndFlush(meter1);
            persistAndFlush(meter2);

            // Act
            List<MeterEntity> allMeters = meterRepository.findAll();

            // Assert
            assertThat(allMeters).hasSizeGreaterThanOrEqualTo(2);
            assertThat(allMeters)
                .extracting(MeterEntity::getSerialNumber)
                .contains("METER-001", "METER-002");
        }

        @Test
        @DisplayName("Should count meters correctly")
        void shouldCountMetersCorrectly() {
            // Arrange
            long initialCount = meterRepository.count();
            MeterEntity meter1 = createValidMeter();
            MeterEntity meter2 = createValidMeter();
            
            persistAndFlush(meter1);
            persistAndFlush(meter2);

            // Act
            long finalCount = meterRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find meter by serial number")
        void shouldFindMeterBySerialNumber() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setSerialNumber("UNIQUE-SERIAL-12345");
            MeterEntity saved = persistAndFlush(meter);

            // Act
            Optional<MeterEntity> found = meterRepository.findBySerialNumber("UNIQUE-SERIAL-12345");

            // Assert
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getSerialNumber()).isEqualTo("UNIQUE-SERIAL-12345");
        }

        @Test
        @DisplayName("Should find meters by form number")
        void shouldFindMetersByFormNumber() {
            // Arrange
            MeterEntity meter1 = createValidMeter();
            meter1.setFormNumber("FORM-A123");
            MeterEntity meter2 = createValidMeter();
            meter2.setFormNumber("FORM-A123");
            MeterEntity meter3 = createValidMeter();
            meter3.setFormNumber("FORM-B456");
            
            persistAndFlush(meter1);
            persistAndFlush(meter2);
            persistAndFlush(meter3);

            // Act
            List<MeterEntity> formAMeters = meterRepository.findByFormNumber("FORM-A123");

            // Assert
            assertThat(formAMeters).hasSize(2);
            assertThat(formAMeters).extracting(MeterEntity::getFormNumber)
                .allMatch(form -> form.equals("FORM-A123"));
        }

        @Test
        @DisplayName("Should find meters by UTC number")
        void shouldFindMetersByUtcNumber() {
            // Arrange
            MeterEntity meter1 = createValidMeter();
            meter1.setUtcNumber("UTC-999888");
            MeterEntity meter2 = createValidMeter();
            meter2.setUtcNumber("UTC-999888");
            MeterEntity meter3 = createValidMeter();
            meter3.setUtcNumber("UTC-777666");
            
            persistAndFlush(meter1);
            persistAndFlush(meter2);
            persistAndFlush(meter3);

            // Act
            List<MeterEntity> utcMeters = meterRepository.findByUtcNumber("UTC-999888");

            // Assert
            assertThat(utcMeters).hasSize(2);
            assertThat(utcMeters).extracting(MeterEntity::getUtcNumber)
                .allMatch(utc -> utc.equals("UTC-999888"));
        }

        @Test
        @DisplayName("Should find virtual meters")
        void shouldFindVirtualMeters() {
            // Arrange
            MeterEntity virtualMeter1 = createVirtualMeter();
            MeterEntity virtualMeter2 = createVirtualMeter();
            MeterEntity physicalMeter = createValidMeter();
            physicalMeter.setIsVirtual(false);
            
            persistAndFlush(virtualMeter1);
            persistAndFlush(virtualMeter2);
            persistAndFlush(physicalMeter);

            // Act
            List<MeterEntity> virtualMeters = meterRepository.findVirtualMeters();

            // Assert
            assertThat(virtualMeters).hasSize(2);
            assertThat(virtualMeters).extracting(MeterEntity::getIsVirtual)
                .allMatch(isVirtual -> isVirtual.equals(true));
        }

        @Test
        @DisplayName("Should find physical meters")
        void shouldFindPhysicalMeters() {
            // Arrange
            MeterEntity physicalMeter1 = createValidMeter();
            physicalMeter1.setIsVirtual(false);
            MeterEntity physicalMeter2 = createValidMeter();
            physicalMeter2.setIsVirtual(null); // Should be considered physical
            MeterEntity virtualMeter = createVirtualMeter();
            
            persistAndFlush(physicalMeter1);
            persistAndFlush(physicalMeter2);
            persistAndFlush(virtualMeter);

            // Act
            List<MeterEntity> physicalMeters = meterRepository.findPhysicalMeters();

            // Assert
            assertThat(physicalMeters).hasSize(2);
            assertThat(physicalMeters).extracting(MeterEntity::getIsVirtual)
                .allMatch(isVirtual -> isVirtual == null || isVirtual.equals(false));
        }

        @Test
        @DisplayName("Should find PAN devices")
        void shouldFindPanDevices() {
            // Arrange
            MeterEntity panMeter1 = createPanMeter();
            MeterEntity panMeter2 = createPanMeter();
            MeterEntity regularMeter = createValidMeter();
            regularMeter.setIsPan(false);
            
            persistAndFlush(panMeter1);
            persistAndFlush(panMeter2);
            persistAndFlush(regularMeter);

            // Act
            List<MeterEntity> panDevices = meterRepository.findPanDevices();

            // Assert
            assertThat(panDevices).hasSize(2);
            assertThat(panDevices).extracting(MeterEntity::getIsPan)
                .allMatch(isPan -> isPan.equals(true));
        }

        @Test
        @DisplayName("Should find meters by AMR system")
        void shouldFindMetersByAmrSystem() {
            // Arrange
            MeterEntity meter1 = createValidMeter();
            meter1.setAmrSystem("AMR-SYSTEM-ALPHA");
            MeterEntity meter2 = createValidMeter();
            meter2.setAmrSystem("AMR-SYSTEM-ALPHA");
            MeterEntity meter3 = createValidMeter();
            meter3.setAmrSystem("AMR-SYSTEM-BETA");
            
            persistAndFlush(meter1);
            persistAndFlush(meter2);
            persistAndFlush(meter3);

            // Act
            List<MeterEntity> alphaMeters = meterRepository.findByAmrSystem("AMR-SYSTEM-ALPHA");

            // Assert
            assertThat(alphaMeters).hasSize(2);
            assertThat(alphaMeters).extracting(MeterEntity::getAmrSystem)
                .allMatch(amr -> amr.equals("AMR-SYSTEM-ALPHA"));
        }

        @Test
        @DisplayName("Should find meters by install code")
        void shouldFindMetersByInstallCode() {
            // Arrange
            MeterEntity meter1 = createValidMeter();
            meter1.setInstallCode("INSTALL-CODE-XYZ");
            MeterEntity meter2 = createValidMeter();
            meter2.setInstallCode("INSTALL-CODE-XYZ");
            MeterEntity meter3 = createValidMeter();
            meter3.setInstallCode("INSTALL-CODE-ABC");
            
            persistAndFlush(meter1);
            persistAndFlush(meter2);
            persistAndFlush(meter3);

            // Act
            List<MeterEntity> xyzMeters = meterRepository.findByInstallCode("INSTALL-CODE-XYZ");

            // Assert
            assertThat(xyzMeters).hasSize(2);
            assertThat(xyzMeters).extracting(MeterEntity::getInstallCode)
                .allMatch(code -> code.equals("INSTALL-CODE-XYZ"));
        }

        @Test
        @DisplayName("Should find meters by interval length greater than")
        void shouldFindMetersByIntervalLengthGreaterThan() {
            // Arrange
            MeterEntity meter1 = createValidMeter();
            meter1.setIntervalLength(300L); // 5 minutes
            MeterEntity meter2 = createValidMeter();
            meter2.setIntervalLength(1800L); // 30 minutes
            MeterEntity meter3 = createValidMeter();
            meter3.setIntervalLength(3600L); // 60 minutes
            
            persistAndFlush(meter1);
            persistAndFlush(meter2);
            persistAndFlush(meter3);

            // Act
            List<MeterEntity> longIntervalMeters = meterRepository.findByIntervalLengthGreaterThan(900L); // > 15 minutes

            // Assert
            assertThat(longIntervalMeters).hasSize(2);
            assertThat(longIntervalMeters).extracting(MeterEntity::getIntervalLength)
                .allMatch(interval -> interval > 900L);
        }

        @Test
        @DisplayName("Should find critical meters")
        void shouldFindCriticalMeters() {
            // Arrange
            MeterEntity criticalMeter1 = createCriticalMeter();
            MeterEntity criticalMeter2 = createCriticalMeter();
            MeterEntity regularMeter = createValidMeter();
            regularMeter.setCritical(false);
            
            persistAndFlush(criticalMeter1);
            persistAndFlush(criticalMeter2);
            persistAndFlush(regularMeter);

            // Act
            List<MeterEntity> criticalMeters = meterRepository.findCriticalMeters();

            // Assert
            assertThat(criticalMeters).hasSize(2);
            assertThat(criticalMeters).extracting(MeterEntity::getCritical)
                .allMatch(critical -> critical.equals(true));
        }

        @Test
        @DisplayName("Should return empty results when no matches found")
        void shouldReturnEmptyResultsWhenNoMatchesFound() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setSerialNumber("EXISTING-METER");
            persistAndFlush(meter);

            // Act
            Optional<MeterEntity> notFound = meterRepository.findBySerialNumber("NON-EXISTENT");
            List<MeterEntity> emptyList = meterRepository.findByFormNumber("NON-EXISTENT-FORM");

            // Assert
            assertThat(notFound).isEmpty();
            assertThat(emptyList).isEmpty();
        }
    }

    @Nested
    @DisplayName("Meter Device Field Testing")
    class MeterDeviceFieldTest {

        @Test
        @DisplayName("Should persist all meter specific fields correctly")
        void shouldPersistAllMeterSpecificFieldsCorrectly() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setFormNumber("SPECIAL-FORM-12345");
            meter.setIntervalLength(2700L); // 45 minutes

            // Act
            MeterEntity saved = persistAndFlush(meter);

            // Assert
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            MeterEntity entity = retrieved.get();
            assertThat(entity.getFormNumber()).isEqualTo("SPECIAL-FORM-12345");
            assertThat(entity.getIntervalLength()).isEqualTo(2700L);
        }

        @Test
        @DisplayName("Should persist all inherited EndDevice fields correctly")
        void shouldPersistAllInheritedEndDeviceFieldsCorrectly() {
            // Arrange
            MeterEntity meter = createValidMeter();
            meter.setType("SMART_METER");
            meter.setUtcNumber("UTC-SPECIAL-999");
            meter.setSerialNumber("SN-SPECIAL-888777");
            meter.setLotNumber("LOT-SPECIAL-666");
            meter.setPurchasePrice(125000L); // $1250.00
            meter.setCritical(true);
            meter.setInitialCondition("REFURBISHED");
            meter.setInitialLossOfLife(new BigDecimal("0.15"));
            meter.setIsVirtual(false);
            meter.setIsPan(true);
            meter.setInstallCode("SPECIAL-INSTALL-CODE");
            meter.setAmrSystem("SPECIAL-AMR-SYSTEM");

            // Act
            MeterEntity saved = persistAndFlush(meter);

            // Assert
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            MeterEntity entity = retrieved.get();
            assertThat(entity.getType()).isEqualTo("SMART_METER");
            assertThat(entity.getUtcNumber()).isEqualTo("UTC-SPECIAL-999");
            assertThat(entity.getSerialNumber()).isEqualTo("SN-SPECIAL-888777");
            assertThat(entity.getLotNumber()).isEqualTo("LOT-SPECIAL-666");
            assertThat(entity.getPurchasePrice()).isEqualTo(125000L);
            assertThat(entity.getCritical()).isTrue();
            assertThat(entity.getInitialCondition()).isEqualTo("REFURBISHED");
            assertThat(entity.getInitialLossOfLife()).isEqualTo(new BigDecimal("0.15"));
            assertThat(entity.getIsVirtual()).isFalse();
            assertThat(entity.getIsPan()).isTrue();
            assertThat(entity.getInstallCode()).isEqualTo("SPECIAL-INSTALL-CODE");
            assertThat(entity.getAmrSystem()).isEqualTo("SPECIAL-AMR-SYSTEM");
        }

        @Test
        @DisplayName("Should handle null optional fields correctly")
        void shouldHandleNullOptionalFieldsCorrectly() {
            // Arrange
            MeterEntity meter = new MeterEntity();
            meter.setDescription("Minimal Meter");
            meter.setFormNumber(null);
            meter.setIntervalLength(null);
            meter.setType(null);
            meter.setUtcNumber(null);
            meter.setSerialNumber(null);
            meter.setLotNumber(null);
            meter.setPurchasePrice(null);
            meter.setCritical(null);
            meter.setInitialCondition(null);
            meter.setInitialLossOfLife(null);
            meter.setIsVirtual(null);
            meter.setIsPan(null);
            meter.setInstallCode(null);
            meter.setAmrSystem(null);

            // Act
            MeterEntity saved = persistAndFlush(meter);

            // Assert
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            MeterEntity entity = retrieved.get();
            assertThat(entity.getFormNumber()).isNull();
            assertThat(entity.getIntervalLength()).isNull();
            assertThat(entity.getType()).isNull();
            assertThat(entity.getUtcNumber()).isNull();
            assertThat(entity.getSerialNumber()).isNull();
            assertThat(entity.getLotNumber()).isNull();
            assertThat(entity.getPurchasePrice()).isNull();
            assertThat(entity.getCritical()).isNull();
            assertThat(entity.getInitialCondition()).isNull();
            assertThat(entity.getInitialLossOfLife()).isNull();
            assertThat(entity.getIsVirtual()).isNull();
            assertThat(entity.getIsPan()).isNull();
            assertThat(entity.getInstallCode()).isNull();
            assertThat(entity.getAmrSystem()).isNull();
        }
    }

    @Nested
    @DisplayName("Inheritance Testing")
    class InheritanceTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality through EndDeviceEntity")
        void shouldInheritIdentifiedObjectFunctionalityThroughEndDeviceEntity() {
            // Arrange
            MeterEntity meter = createValidMeter();

            // Act
            MeterEntity saved = meterRepository.save(meter);
            flushAndClear();

            // Assert
            Optional<MeterEntity> retrieved = meterRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            
            MeterEntity entity = retrieved.get();
            // IdentifiedObject fields
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getCreated()).isNotNull();
            assertThat(entity.getUpdated()).isNotNull();
            assertThat(entity.getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should update timestamps on modification")
        void shouldUpdateTimestampsOnModification() {
            // Arrange
            MeterEntity meter = createValidMeter();
            MeterEntity saved = persistAndFlush(meter);
            
            // Wait a moment to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            saved.setDescription("Updated Description");
            MeterEntity updated = meterRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<MeterEntity> retrieved = meterRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUpdated()).isAfter(retrieved.get().getCreated());
        }

        @Test
        @DisplayName("Should generate unique IDs for different entities")
        void shouldGenerateUniqueIdsForDifferentEntities() {
            // Arrange
            MeterEntity meter1 = createValidMeter();
            MeterEntity meter2 = createValidMeter();

            // Act
            MeterEntity saved1 = meterRepository.save(meter1);
            MeterEntity saved2 = meterRepository.save(meter2);
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
            MeterEntity meter1 = createValidMeter();
            MeterEntity meter2 = createValidMeter();
            
            MeterEntity saved1 = persistAndFlush(meter1);
            MeterEntity saved2 = persistAndFlush(meter2);

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
            MeterEntity meter = createValidMeter();
            meter.setSerialNumber("TEST-SERIAL-12345");
            meter.setFormNumber("TEST-FORM-67890");
            MeterEntity saved = persistAndFlush(meter);

            // Act
            String toString = saved.toString();

            // Assert
            assertThat(toString).contains("MeterEntity");
            assertThat(toString).contains("id = " + saved.getId());
            assertThat(toString).contains("serialNumber = TEST-SERIAL-12345");
            assertThat(toString).contains("formNumber = TEST-FORM-67890");
        }
    }
}