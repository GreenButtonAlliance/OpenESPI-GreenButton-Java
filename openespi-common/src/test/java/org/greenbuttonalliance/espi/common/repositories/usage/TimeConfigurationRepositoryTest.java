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

import jakarta.validation.ConstraintViolation;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for TimeConfigurationRepository.
 *
 * Tests all CRUD operations, 2 custom query methods (index-based only),
 * time zone and DST configuration field testing, and relationships.
 */
@DisplayName("TimeConfiguration Repository Tests")
class TimeConfigurationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TimeConfigurationRepository timeConfigurationRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private org.greenbuttonalliance.espi.common.repositories.customer.CustomerRepository customerRepository;

    /**
     * Creates a valid TimeConfigurationEntity for testing.
     */
    private TimeConfigurationEntity createValidTimeConfiguration() {
        TimeConfigurationEntity timeConfig = new TimeConfigurationEntity();
        timeConfig.setDescription("Test Time Configuration");
        timeConfig.setTzOffset(-28800L); // PST: UTC-8 hours in seconds
        timeConfig.setDstOffset(3600L); // 1 hour DST offset
        
        // Create DST start rule (example: second Sunday in March at 2:00 AM)
        byte[] dstStartRule = {0x02, 0x00, 0x03, 0x02}; // Month=3, Week=2, Day=0 (Sunday), Hour=2
        timeConfig.setDstStartRule(dstStartRule);
        
        // Create DST end rule (example: first Sunday in November at 2:00 AM)
        byte[] dstEndRule = {0x01, 0x00, 0x0B, 0x02}; // Month=11, Week=1, Day=0 (Sunday), Hour=2
        timeConfig.setDstEndRule(dstEndRule);
        
        return timeConfig;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve time configuration successfully")
        void shouldSaveAndRetrieveTimeConfigurationSuccessfully() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Test Time Configuration for CRUD");

            // Act
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            flushAndClear();
            Optional<TimeConfigurationEntity> retrieved = timeConfigurationRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            // TimeConfigurationEntity generates description dynamically from timezone and DST settings
            assertThat(retrieved.get().getDescription()).isEqualTo("UTC-8.0 (DST: +1.0h) with DST rules");
            assertThat(retrieved.get().getTzOffset()).isEqualTo(-28800L);
            assertThat(retrieved.get().getDstOffset()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("Should save time configuration with DST rules")
        void shouldSaveTimeConfigurationWithDstRules() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Time Configuration with DST Rules");

            // Act
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            flushAndClear();
            Optional<TimeConfigurationEntity> retrieved = timeConfigurationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            TimeConfigurationEntity entity = retrieved.get();
            assertThat(entity.getDstStartRule()).isNotNull();
            assertThat(entity.getDstStartRule()).hasSize(4);
            assertThat(entity.getDstStartRule()[0]).isEqualTo((byte) 0x02);
            assertThat(entity.getDstEndRule()).isNotNull();
            assertThat(entity.getDstEndRule()).hasSize(4);
            assertThat(entity.getDstEndRule()[0]).isEqualTo((byte) 0x01);
        }

        @Test
        @DisplayName("Should find all time configurations")
        void shouldFindAllTimeConfigurations() {
            // Arrange
            List<TimeConfigurationEntity> timeConfigs = List.of(
                createValidTimeConfiguration(),
                createValidTimeConfiguration(),
                createValidTimeConfiguration()
            );
            // Note: Setting description manually, but TimeConfigurationEntity overrides getDescription()
            timeConfigs.get(0).setDescription("Time Config 1");
            timeConfigs.get(1).setDescription("Time Config 2");
            timeConfigs.get(2).setDescription("Time Config 3");
            timeConfigurationRepository.saveAll(timeConfigs);
            flushAndClear();

            // Act
            List<TimeConfigurationEntity> allTimeConfigs = timeConfigurationRepository.findAll();

            // Assert
            assertThat(allTimeConfigs).hasSizeGreaterThanOrEqualTo(3);
            // TimeConfigurationEntity generates description dynamically, so all will have the same description
            assertThat(allTimeConfigs).extracting(TimeConfigurationEntity::getDescription)
                    .contains("UTC-8.0 (DST: +1.0h) with DST rules");
        }

        @Test
        @DisplayName("Should delete time configuration successfully")
        void shouldDeleteTimeConfigurationSuccessfully() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Time Configuration to Delete");
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            UUID timeConfigId = saved.getId();
            flushAndClear();

            // Act
            timeConfigurationRepository.deleteById(timeConfigId);
            flushAndClear();
            Optional<TimeConfigurationEntity> retrieved = timeConfigurationRepository.findById(timeConfigId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if time configuration exists")
        void shouldCheckIfTimeConfigurationExists() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            flushAndClear();

            // Act & Assert
            assertThat(timeConfigurationRepository.existsById(saved.getId())).isTrue();
            assertThat(timeConfigurationRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count time configurations")
        void shouldCountTimeConfigurations() {
            // Arrange
            long initialCount = timeConfigurationRepository.count();
            List<TimeConfigurationEntity> timeConfigs = List.of(
                createValidTimeConfiguration(),
                createValidTimeConfiguration()
            );
            timeConfigurationRepository.saveAll(timeConfigs);
            flushAndClear();

            // Act
            long finalCount = timeConfigurationRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find all time configuration IDs")
        void shouldFindAllTimeConfigurationIds() {
            // Arrange
            List<TimeConfigurationEntity> timeConfigs = List.of(
                createValidTimeConfiguration(),
                createValidTimeConfiguration(),
                createValidTimeConfiguration()
            );
            List<TimeConfigurationEntity> savedTimeConfigs = timeConfigurationRepository.saveAll(timeConfigs);
            flushAndClear();

            // Act
            List<UUID> allIds = timeConfigurationRepository.findAllIds();

            // Assert
            assertThat(allIds)
                    .hasSizeGreaterThanOrEqualTo(3)
                    .contains(
                            savedTimeConfigs.get(0).getId(),
                            savedTimeConfigs.get(1).getId(),
                            savedTimeConfigs.get(2).getId()
                    );
        }

        @Test
        @DisplayName("Should find time configuration IDs by usage point ID")
        void shouldFindTimeConfigurationIdsByUsagePointId() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Time Config for Usage Point");
            TimeConfigurationEntity savedTimeConfig = timeConfigurationRepository.save(timeConfig);

            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point with Time Config");
            usagePoint.setLocalTimeParameters(savedTimeConfig);
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            flushAndClear();

            // Act
            List<UUID> results = timeConfigurationRepository.findAllIdsByUsagePointId(savedUsagePoint.getId());

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).isEqualTo(savedTimeConfig.getId());
        }

        @Test
        @DisplayName("Should find all distinct time configuration IDs")
        void shouldFindAllDistinctTimeConfigurationIds() {
            // Arrange
            List<TimeConfigurationEntity> timeConfigs = List.of(
                createValidTimeConfiguration(),
                createValidTimeConfiguration()
            );
            List<TimeConfigurationEntity> savedTimeConfigs = timeConfigurationRepository.saveAll(timeConfigs);
            flushAndClear();

            // Act
            List<UUID> results = timeConfigurationRepository.findAllIds();

            // Assert
            assertThat(results)
                    .hasSizeGreaterThanOrEqualTo(2)
                    .contains(
                            savedTimeConfigs.get(0).getId(),
                            savedTimeConfigs.get(1).getId()
                    );
        }

        @Test
        @DisplayName("Should verify time configuration exists by ID")
        void shouldVerifyTimeConfigurationExistsById() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Time Config for Existence Test");
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            flushAndClear();

            // Act
            boolean exists = timeConfigurationRepository.existsById(saved.getId());
            Optional<TimeConfigurationEntity> found = timeConfigurationRepository.findById(saved.getId());

            // Assert
            assertThat(exists).isTrue();
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Act & Assert
            assertThat(timeConfigurationRepository.findAllIdsByUsagePointId(UUID.randomUUID())).isEmpty();
            assertThat(timeConfigurationRepository.existsById(UUID.randomUUID())).isFalse();
        }
    }

    @Nested
    @DisplayName("Time Zone and DST Configuration Testing")
    class TimeZoneDstConfigurationTest {

        @Test
        @DisplayName("Should handle different time zone offsets")
        void shouldHandleDifferentTimeZoneOffsets() {
            // Arrange
            TimeConfigurationEntity estConfig = createValidTimeConfiguration();
            estConfig.setTzOffset(-18000L); // EST: UTC-5 hours
            estConfig.setDescription("Eastern Time Configuration");

            TimeConfigurationEntity pstConfig = createValidTimeConfiguration();
            pstConfig.setTzOffset(-28800L); // PST: UTC-8 hours
            pstConfig.setDescription("Pacific Time Configuration");

            TimeConfigurationEntity utcConfig = createValidTimeConfiguration();
            utcConfig.setTzOffset(0L); // UTC
            utcConfig.setDescription("UTC Time Configuration");

            // Act
            List<TimeConfigurationEntity> saved = timeConfigurationRepository.saveAll(
                    List.of(estConfig, pstConfig, utcConfig));
            flushAndClear();

            // Assert
            assertThat(saved).hasSize(3);
            assertThat(saved).extracting(TimeConfigurationEntity::getTzOffset)
                    .contains(-18000L, -28800L, 0L);
        }

        @Test
        @DisplayName("Should handle DST offset configurations")
        void shouldHandleDstOffsetConfigurations() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("DST Configuration Test");
            timeConfig.setDstOffset(3600L); // Standard 1-hour DST offset

            // Act
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            flushAndClear();
            Optional<TimeConfigurationEntity> retrieved = timeConfigurationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDstOffset()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("Should handle null DST rules")
        void shouldHandleNullDstRules() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Time Config without DST Rules");
            timeConfig.setDstStartRule(null);
            timeConfig.setDstEndRule(null);

            // Act
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            flushAndClear();
            Optional<TimeConfigurationEntity> retrieved = timeConfigurationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            TimeConfigurationEntity entity = retrieved.get();
            assertThat(entity.getDstStartRule()).isNull();
            assertThat(entity.getDstEndRule()).isNull();
        }

        @Test
        @DisplayName("Should properly copy DST rule arrays")
        void shouldProperlyCopyDstRuleArrays() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            byte[] originalStartRule = {0x01, 0x02, 0x03, 0x04};
            byte[] originalEndRule = {0x05, 0x06, 0x07, 0x08};
            
            timeConfig.setDstStartRule(originalStartRule);
            timeConfig.setDstEndRule(originalEndRule);

            // Act
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            flushAndClear();
            Optional<TimeConfigurationEntity> retrieved = timeConfigurationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            TimeConfigurationEntity entity = retrieved.get();
            
            // Verify arrays are copied, not referenced
            assertThat(entity.getDstStartRule()).isNotSameAs(originalStartRule);
            assertThat(entity.getDstEndRule()).isNotSameAs(originalEndRule);
            
            // Verify content is correct
            assertThat(entity.getDstStartRule()).containsExactly((byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04);
            assertThat(entity.getDstEndRule()).containsExactly((byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08);
        }
    }

    @Nested
    @DisplayName("Relationship Testing")
    class RelationshipTest {

        @Test
        @DisplayName("Should create time configuration with usage point relationship")
        void shouldCreateTimeConfigurationWithUsagePointRelationship() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Time Config with Usage Point");
            TimeConfigurationEntity savedTimeConfig = timeConfigurationRepository.save(timeConfig);

            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Usage Point with Time Config");
            usagePoint.setLocalTimeParameters(savedTimeConfig);
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            flushAndClear();

            // Act
            Optional<TimeConfigurationEntity> retrieved = timeConfigurationRepository.findById(savedTimeConfig.getId());

            // Assert
            assertThat(retrieved).isPresent();
            // Note: Due to lazy loading, we verify the relationship exists through the usage point
            Optional<UsagePointEntity> usagePointCheck = usagePointRepository.findById(savedUsagePoint.getId());
            assertThat(usagePointCheck).isPresent();
            assertThat(usagePointCheck.get().getLocalTimeParameters().getId()).isEqualTo(savedTimeConfig.getId());
        }

        @Test
        @DisplayName("Should handle time configuration without relationships")
        void shouldHandleTimeConfigurationWithoutRelationships() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Standalone Time Configuration");

            // Act
            TimeConfigurationEntity saved = timeConfigurationRepository.save(timeConfig);
            flushAndClear();
            Optional<TimeConfigurationEntity> retrieved = timeConfigurationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            TimeConfigurationEntity entity = retrieved.get();
            assertThat(entity.getUsagePoints()).isEmpty();
            assertThat(entity.getCustomer()).isNull();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate time configuration with valid data")
        void shouldValidateTimeConfigurationWithValidData() {
            // Arrange
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();

            // Act
            Set<ConstraintViolation<TimeConfigurationEntity>> violations = validator.validate(timeConfig);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle time configuration with minimal data")
        void shouldHandleTimeConfigurationWithMinimalData() {
            // Arrange
            TimeConfigurationEntity timeConfig = new TimeConfigurationEntity();
            timeConfig.setDescription("Minimal Time Configuration");

            // Act
            Set<ConstraintViolation<TimeConfigurationEntity>> violations = validator.validate(timeConfig);

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
            TimeConfigurationEntity timeConfig = createValidTimeConfiguration();
            timeConfig.setDescription("Time Configuration for Base Class Test");

            // Act
            TimeConfigurationEntity saved = persistAndFlush(timeConfig);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            TimeConfigurationEntity timeConfig1 = createValidTimeConfiguration();
            timeConfig1.setDescription("Time Configuration 1");
            TimeConfigurationEntity saved1 = timeConfigurationRepository.save(timeConfig1);
            
            TimeConfigurationEntity timeConfig2 = createValidTimeConfiguration();
            timeConfig2.setDescription("Time Configuration 2");
            TimeConfigurationEntity saved2 = timeConfigurationRepository.save(timeConfig2);
            
            flushAndClear();

            // Act & Assert
            assertThat(saved1).isNotEqualTo(saved2);
            // Note: TimeConfigurationEntity.hashCode() returns class hashCode, so all instances have same hash code
            // This is acceptable as long as equals() works correctly with different IDs

            // Same entity should be equal to itself
            assertThat(saved1)
                    .isEqualTo(saved1)
                    .hasSameHashCodeAs(saved1);
        }
    }
}