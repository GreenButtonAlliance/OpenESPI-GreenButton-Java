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

import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.common.RationalNumber;
import org.greenbuttonalliance.espi.common.domain.common.ReadingInterharmonic;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;

import java.math.BigInteger;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
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
 * Comprehensive test suite for ReadingTypeRepository.
 * 
 * Tests all CRUD operations, complex embedded object handling,
 * measurement specification validation, and MeterReading relationships.
 */
@DisplayName("ReadingType Repository Tests")
class ReadingTypeRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ReadingTypeRepository readingTypeRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve reading type successfully")
        void shouldSaveAndRetrieveReadingTypeSuccessfully() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setAccumulationBehaviour("DELTAOUTPUT"); // Delta data
            readingType.setCommodity("ELECTRICITY"); // Electricity secondary metered

            // Act
            ReadingTypeEntity saved = readingTypeRepository.save(readingType);
            flushAndClear();
            Optional<ReadingTypeEntity> retrieved = readingTypeRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            // ReadingTypeEntity generates description dynamically from commodity
            assertThat(retrieved.get().getDescription()).isEqualTo("Electricity");
            assertThat(retrieved.get().getAccumulationBehaviour()).isEqualTo("DELTAOUTPUT");
            assertThat(retrieved.get().getCommodity()).isEqualTo("ELECTRICITY");
        }

        @Test
        @DisplayName("Should save reading type with measurement specifications")
        void shouldSaveReadingTypeWithMeasurementSpecifications() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Reading Type with Specifications");
            readingType.setKind("ENERGY"); // Energy
            readingType.setUom("WH"); // Wh
            readingType.setPowerOfTenMultiplier("KILO"); // kilo
            readingType.setFlowDirection("FORWARD"); // Forward
            readingType.setPhase("THREE_PHASE"); // Three phase
            readingType.setDataQualifier("AVERAGE"); // Average
            readingType.setDefaultQuality("VALID"); // Valid
            readingType.setIntervalLength(3600L); // 1 hour
            readingType.setTimeAttribute("NONE"); // None

            // Act
            ReadingTypeEntity saved = readingTypeRepository.save(readingType);
            flushAndClear();
            Optional<ReadingTypeEntity> retrieved = readingTypeRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ReadingTypeEntity entity = retrieved.get();
            assertThat(entity.getKind()).isEqualTo("ENERGY");
            assertThat(entity.getUom()).isEqualTo("WH");
            assertThat(entity.getPowerOfTenMultiplier()).isEqualTo("KILO");
            assertThat(entity.getFlowDirection()).isEqualTo("FORWARD");
            assertThat(entity.getPhase()).isEqualTo("THREE_PHASE");
            assertThat(entity.getDataQualifier()).isEqualTo("AVERAGE");
            assertThat(entity.getDefaultQuality()).isEqualTo("VALID");
            assertThat(entity.getIntervalLength()).isEqualTo(3600L);
            assertThat(entity.getTimeAttribute()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("Should find all reading types")
        void shouldFindAllReadingTypes() {
            // Arrange
            List<ReadingTypeEntity> readingTypes = TestDataBuilders.createValidEntities(3, TestDataBuilders::createValidReadingType);
            // Set commodity to generate meaningful descriptions since ReadingTypeEntity overrides getDescription()
            readingTypes.get(0).setCommodity("ELECTRICITY");
            readingTypes.get(1).setCommodity("GAS");
            readingTypes.get(2).setCommodity("WATER");
            readingTypeRepository.saveAll(readingTypes);
            flushAndClear();

            // Act
            List<ReadingTypeEntity> allReadingTypes = readingTypeRepository.findAll();

            // Assert
            assertThat(allReadingTypes).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allReadingTypes).extracting(ReadingTypeEntity::getDescription)
                    .contains("Electricity", "Gas", "Water");
        }

        @Test
        @DisplayName("Should delete reading type successfully")
        void shouldDeleteReadingTypeSuccessfully() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Reading Type to Delete");
            ReadingTypeEntity saved = readingTypeRepository.save(readingType);
            UUID readingTypeId = saved.getId();
            flushAndClear();

            // Act
            readingTypeRepository.deleteById(readingTypeId);
            flushAndClear();
            Optional<ReadingTypeEntity> retrieved = readingTypeRepository.findById(readingTypeId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if reading type exists")
        void shouldCheckIfReadingTypeExists() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            ReadingTypeEntity saved = readingTypeRepository.save(readingType);
            flushAndClear();

            // Act & Assert
            assertThat(readingTypeRepository.existsById(saved.getId())).isTrue();
            assertThat(readingTypeRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count reading types")
        void shouldCountReadingTypes() {
            // Arrange
            long initialCount = readingTypeRepository.count();
            List<ReadingTypeEntity> readingTypes = TestDataBuilders.createValidEntities(5, TestDataBuilders::createValidReadingType);
            readingTypeRepository.saveAll(readingTypes);
            flushAndClear();

            // Act
            long finalCount = readingTypeRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 5);
        }
    }

    @Nested
    @DisplayName("Complex Embedded Object Testing")
    class EmbeddedObjectTest {

        @Test
        @DisplayName("Should save reading type with RationalNumber embedded objects")
        void shouldSaveReadingTypeWithRationalNumberEmbeddedObjects() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Reading Type with RationalNumber");
            
            // Create RationalNumber for argument
            RationalNumber argument = new RationalNumber();
            argument.setNumerator(java.math.BigInteger.valueOf(1L));
            argument.setDenominator(java.math.BigInteger.valueOf(2L));
            readingType.setArgument(argument);

            // Act
            ReadingTypeEntity saved = readingTypeRepository.save(readingType);
            flushAndClear();
            Optional<ReadingTypeEntity> retrieved = readingTypeRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ReadingTypeEntity entity = retrieved.get();
            assertThat(entity.getArgument()).isNotNull();
            assertThat(entity.getArgument().getNumerator()).isEqualTo(java.math.BigInteger.valueOf(1L));
            assertThat(entity.getArgument().getDenominator()).isEqualTo(java.math.BigInteger.valueOf(2L));
        }

        @Test
        @DisplayName("Should save reading type with ReadingInterharmonic embedded objects")
        void shouldSaveReadingTypeWithReadingInterharmonicEmbeddedObjects() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Reading Type with ReadingInterharmonic");
            
            // Create ReadingInterharmonic for interharmonic
            ReadingInterharmonic interharmonic = new ReadingInterharmonic();

            // Set BigInteger values directly
            interharmonic.setNumerator(BigInteger.valueOf(50L));
            interharmonic.setDenominator(BigInteger.valueOf(1L));
            
            readingType.setInterharmonic(interharmonic);

            // Act
            ReadingTypeEntity saved = readingTypeRepository.save(readingType);
            flushAndClear();
            Optional<ReadingTypeEntity> retrieved = readingTypeRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ReadingTypeEntity entity = retrieved.get();
            assertThat(entity.getInterharmonic()).isNotNull();
            assertThat(entity.getInterharmonic().getNumerator()).isEqualTo(50L);
            assertThat(entity.getInterharmonic().getDenominator()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Measurement Specification Field Validation")
    class MeasurementSpecificationTest {

        @Test
        @DisplayName("Should validate measurement specification fields")
        void shouldValidateMeasurementSpecificationFields() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Reading Type with Full Specifications");
            
            // Set comprehensive measurement specifications
            readingType.setAccumulationBehaviour("DELTAOUTPUT"); // Delta data
            readingType.setCommodity("ELECTRICITY"); // Electricity secondary metered
            readingType.setDataQualifier("AVERAGE"); // Average
            readingType.setDefaultQuality("VALID"); // Valid
            readingType.setFlowDirection("FORWARD"); // Forward
            readingType.setIntervalLength(3600L); // 1 hour
            readingType.setKind("ENERGY"); // Energy
            readingType.setPhase("THREE_PHASE"); // Three phase
            readingType.setPowerOfTenMultiplier("KILO"); // kilo
            readingType.setTimeAttribute("NONE"); // None
            readingType.setUom("WH"); // Wh
            readingType.setCurrency("USD"); // US Dollar
            readingType.setConsumptionTier("TIER1"); // Tier 1

            // Act
            ReadingTypeEntity saved = readingTypeRepository.save(readingType);
            flushAndClear();
            Optional<ReadingTypeEntity> retrieved = readingTypeRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ReadingTypeEntity entity = retrieved.get();
            assertThat(entity.getAccumulationBehaviour()).isEqualTo("DELTAOUTPUT");
            assertThat(entity.getCommodity()).isEqualTo("ELECTRICITY");
            assertThat(entity.getDataQualifier()).isEqualTo("AVERAGE");
            assertThat(entity.getDefaultQuality()).isEqualTo("VALID");
            assertThat(entity.getFlowDirection()).isEqualTo("FORWARD");
            assertThat(entity.getIntervalLength()).isEqualTo(3600L);
            assertThat(entity.getKind()).isEqualTo("ENERGY");
            assertThat(entity.getPhase()).isEqualTo("THREE_PHASE");
            assertThat(entity.getPowerOfTenMultiplier()).isEqualTo("KILO");
            assertThat(entity.getTimeAttribute()).isEqualTo("NONE");
            assertThat(entity.getUom()).isEqualTo("WH");
            assertThat(entity.getCurrency()).isEqualTo("USD");
            assertThat(entity.getConsumptionTier()).isEqualTo("TIER1");
        }

        @Test
        @DisplayName("Should handle null measurement specification fields")
        void shouldHandleNullMeasurementSpecificationFields() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Reading Type with Null Fields");
            
            // Set some fields to null
            readingType.setAccumulationBehaviour(null);
            readingType.setCommodity(null);
            readingType.setDataQualifier(null);
            readingType.setDefaultQuality(null);
            readingType.setFlowDirection(null);

            // Act
            ReadingTypeEntity saved = readingTypeRepository.save(readingType);
            flushAndClear();
            Optional<ReadingTypeEntity> retrieved = readingTypeRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ReadingTypeEntity entity = retrieved.get();
            assertThat(entity.getAccumulationBehaviour()).isNull();
            assertThat(entity.getCommodity()).isNull();
            assertThat(entity.getDataQualifier()).isNull();
            assertThat(entity.getDefaultQuality()).isNull();
            assertThat(entity.getFlowDirection()).isNull();
        }
    }

    @Nested
    @DisplayName("MeterReading Relationship Testing")
    class MeterReadingRelationshipTest {

        @Test
        @DisplayName("Should create reading type and associate with meter reading")
        void shouldCreateReadingTypeAndAssociateWithMeterReading() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Reading Type Test");
            usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Reading Type for MeterReading");
            readingType.setKind("ENERGY");
            readingType.setUom("WH");
            ReadingTypeEntity savedReadingType = readingTypeRepository.save(readingType);
            
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading.setDescription("Meter Reading with Reading Type");
            meterReading.setReadingType(savedReadingType);

            // Act
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            flushAndClear();
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(savedMeterReading.getId());

            // Assert
            assertThat(retrieved).isPresent();
            MeterReadingEntity entity = retrieved.get();
            assertThat(entity.getReadingType()).isNotNull();
            assertThat(entity.getReadingType().getId()).isEqualTo(savedReadingType.getId());
            // ReadingTypeEntity generates description dynamically from kind and uom
            assertThat(entity.getReadingType().getDescription()).isEqualTo("energy (WH)");
            assertThat(entity.getReadingType().getKind()).isEqualTo("ENERGY");
            assertThat(entity.getReadingType().getUom()).isEqualTo("WH");
        }

        @Test
        @DisplayName("Should handle meter reading without reading type")
        void shouldHandleMeterReadingWithoutReadingType() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point for Reading Type Test");
            usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReadingWithUsagePoint(savedUsagePoint);
            meterReading.setDescription("Meter Reading without Reading Type");
            meterReading.setReadingType(null);

            // Act
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            flushAndClear();
            Optional<MeterReadingEntity> retrieved = meterReadingRepository.findById(savedMeterReading.getId());

            // Assert
            assertThat(retrieved).isPresent();
            MeterReadingEntity entity = retrieved.get();
            assertThat(entity.getReadingType()).isNull();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate reading type with valid data")
        void shouldValidateReadingTypeWithValidData() {
            // Arrange
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Valid Reading Type");
            readingType.setKind("ENERGY");
            readingType.setUom("WH");

            // Act
            Set<ConstraintViolation<ReadingTypeEntity>> violations = validator.validate(readingType);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle reading type with minimal data")
        void shouldHandleReadingTypeWithMinimalData() {
            // Arrange
            ReadingTypeEntity readingType = new ReadingTypeEntity();
            readingType.setDescription("Minimal Reading Type");

            // Act
            Set<ConstraintViolation<ReadingTypeEntity>> violations = validator.validate(readingType);

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
            ReadingTypeEntity readingType = TestDataBuilders.createValidReadingType();
            readingType.setDescription("Reading Type for Base Class Test");

            // Act
            ReadingTypeEntity saved = persistAndFlush(readingType);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            ReadingTypeEntity readingType1 = TestDataBuilders.createValidReadingType();
            readingType1.setDescription("Reading Type 1");
            readingType1.setKind("ENERGY");
            ReadingTypeEntity saved1 = readingTypeRepository.save(readingType1);
            
            ReadingTypeEntity readingType2 = TestDataBuilders.createValidReadingType();
            readingType2.setDescription("Reading Type 2");
            readingType2.setKind("POWER");
            ReadingTypeEntity saved2 = readingTypeRepository.save(readingType2);
            
            flushAndClear();

            // Act & Assert
            assertThat(saved1).isNotEqualTo(saved2);
            // Note: ReadingTypeEntity.hashCode() returns class hashCode, so all instances have same hash code
            // This is acceptable as long as equals() works correctly with different IDs
            
            // Same entity should be equal to itself
            assertThat(saved1).isEqualTo(saved1);
            assertThat(saved1.hashCode()).isEqualTo(saved1.hashCode());
        }
    }
}