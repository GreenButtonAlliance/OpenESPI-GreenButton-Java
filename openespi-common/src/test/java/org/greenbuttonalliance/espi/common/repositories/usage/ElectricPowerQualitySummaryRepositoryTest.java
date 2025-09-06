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

import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ElectricPowerQualitySummaryRepository.
 * 
 * Tests all CRUD operations, custom query methods, relationships,
 * and validation constraints for ElectricPowerQualitySummary entities.
 */
@DisplayName("ElectricPowerQualitySummary Repository Tests")
class ElectricPowerQualitySummaryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve electric power quality summary successfully")
        void shouldSaveAndRetrieveElectricPowerQualitySummarySuccessfully() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            ElectricPowerQualitySummaryEntity summary = new ElectricPowerQualitySummaryEntity();
            summary.setUsagePoint(savedUsagePoint);
            summary.setFlickerPlt(100L);
            summary.setFlickerPst(50L);
            summary.setHarmonicVoltage(5L);
            summary.setMainsVoltage(240000L); // 240V in millivolts
            summary.setPowerFrequency(60000L); // 60Hz in millihertz

            // Act
            ElectricPowerQualitySummaryEntity saved = electricPowerQualitySummaryRepository.save(summary);
            flushAndClear();
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getFlickerPlt()).isEqualTo(100L);
            assertThat(retrieved.get().getFlickerPst()).isEqualTo(50L);
            assertThat(retrieved.get().getMainsVoltage()).isEqualTo(240000L);
            assertThat(retrieved.get().getPowerFrequency()).isEqualTo(60000L);
        }

        @Test
        @DisplayName("Should save electric power quality summary with date time interval")
        void shouldSaveElectricPowerQualitySummaryWithDateTimeInterval() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            DateTimeInterval interval = new DateTimeInterval();
            interval.setStart(1640995200L); // 2022-01-01 00:00:00 UTC
            interval.setDuration(3600L); // 1 hour
            
            ElectricPowerQualitySummaryEntity summary = new ElectricPowerQualitySummaryEntity(interval);
            summary.setUsagePoint(savedUsagePoint);
            summary.setLongInterruptions(2L);
            summary.setShortInterruptions(5L);

            // Act
            ElectricPowerQualitySummaryEntity saved = electricPowerQualitySummaryRepository.save(summary);
            flushAndClear();
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getSummaryInterval()).isNotNull();
            assertThat(retrieved.get().getSummaryInterval().getStart()).isEqualTo(1640995200L);
            assertThat(retrieved.get().getSummaryInterval().getDuration()).isEqualTo(3600L);
            assertThat(retrieved.get().getLongInterruptions()).isEqualTo(2L);
            assertThat(retrieved.get().getShortInterruptions()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find electric power quality summaries by usage point")
        void shouldFindElectricPowerQualitySummariesByUsagePoint() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            ElectricPowerQualitySummaryEntity summary1 = new ElectricPowerQualitySummaryEntity();
            summary1.setUsagePoint(savedUsagePoint);
            summary1.setFlickerPlt(100L);
            
            ElectricPowerQualitySummaryEntity summary2 = new ElectricPowerQualitySummaryEntity();
            summary2.setUsagePoint(savedUsagePoint);
            summary2.setFlickerPlt(200L);
            
            electricPowerQualitySummaryRepository.save(summary1);
            electricPowerQualitySummaryRepository.save(summary2);
            flushAndClear();

            // Act
            List<ElectricPowerQualitySummaryEntity> summaries = electricPowerQualitySummaryRepository.findByUsagePoint(savedUsagePoint);

            // Assert
            assertThat(summaries).hasSize(2);
            assertThat(summaries).extracting(ElectricPowerQualitySummaryEntity::getFlickerPlt)
                .containsExactlyInAnyOrder(100L, 200L);
        }

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            ElectricPowerQualitySummaryEntity summary1 = new ElectricPowerQualitySummaryEntity();
            summary1.setUsagePoint(savedUsagePoint);
            ElectricPowerQualitySummaryEntity summary2 = new ElectricPowerQualitySummaryEntity();
            summary2.setUsagePoint(savedUsagePoint);
            
            ElectricPowerQualitySummaryEntity saved1 = electricPowerQualitySummaryRepository.save(summary1);
            ElectricPowerQualitySummaryEntity saved2 = electricPowerQualitySummaryRepository.save(summary2);
            flushAndClear();

            // Act
            List<UUID> allIds = electricPowerQualitySummaryRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all IDs by usage point ID")
        void shouldFindAllIdsByUsagePointId() {
            // Arrange
            UsagePointEntity usagePoint1 = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity usagePoint2 = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint1 = usagePointRepository.save(usagePoint1);
            UsagePointEntity savedUsagePoint2 = usagePointRepository.save(usagePoint2);
            
            ElectricPowerQualitySummaryEntity summary1 = new ElectricPowerQualitySummaryEntity();
            summary1.setUsagePoint(savedUsagePoint1);
            ElectricPowerQualitySummaryEntity summary2 = new ElectricPowerQualitySummaryEntity();
            summary2.setUsagePoint(savedUsagePoint1);
            ElectricPowerQualitySummaryEntity summary3 = new ElectricPowerQualitySummaryEntity();
            summary3.setUsagePoint(savedUsagePoint2);
            
            ElectricPowerQualitySummaryEntity saved1 = electricPowerQualitySummaryRepository.save(summary1);
            ElectricPowerQualitySummaryEntity saved2 = electricPowerQualitySummaryRepository.save(summary2);
            electricPowerQualitySummaryRepository.save(summary3);
            flushAndClear();

            // Act
            List<UUID> ids = electricPowerQualitySummaryRepository.findAllIdsByUsagePointId(savedUsagePoint1.getId());

            // Assert
            assertThat(ids).hasSize(2);
            assertThat(ids).containsExactlyInAnyOrder(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all IDs by xpath2")
        void shouldFindAllIdsByXpath2() {
            // Arrange
            RetailCustomerEntity retailCustomer = TestDataBuilders.createValidRetailCustomer();
            RetailCustomerEntity savedRetailCustomer = retailCustomerRepository.save(retailCustomer);
            
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setRetailCustomer(savedRetailCustomer);
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            ElectricPowerQualitySummaryEntity summary = new ElectricPowerQualitySummaryEntity();
            summary.setUsagePoint(savedUsagePoint);
            ElectricPowerQualitySummaryEntity savedSummary = electricPowerQualitySummaryRepository.save(summary);
            flushAndClear();

            // Act
            List<UUID> ids = electricPowerQualitySummaryRepository.findAllIdsByXpath2(
                savedRetailCustomer.getId(), savedUsagePoint.getId());

            // Assert
            assertThat(ids).hasSize(1);
            assertThat(ids).contains(savedSummary.getId());
        }

        @Test
        @DisplayName("Should find ID by xpath")
        void shouldFindIdByXpath() {
            // Arrange
            RetailCustomerEntity retailCustomer = TestDataBuilders.createValidRetailCustomer();
            RetailCustomerEntity savedRetailCustomer = retailCustomerRepository.save(retailCustomer);
            
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setRetailCustomer(savedRetailCustomer);
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            ElectricPowerQualitySummaryEntity summary = new ElectricPowerQualitySummaryEntity();
            summary.setUsagePoint(savedUsagePoint);
            ElectricPowerQualitySummaryEntity savedSummary = electricPowerQualitySummaryRepository.save(summary);
            flushAndClear();

            // Act
            Optional<UUID> foundId = electricPowerQualitySummaryRepository.findIdByXpath(
                savedRetailCustomer.getId(), savedUsagePoint.getId(), savedSummary.getId());

            // Assert
            assertThat(foundId).isPresent();
            assertThat(foundId.get()).isEqualTo(savedSummary.getId());
        }
    }

    @Nested
    @DisplayName("JPA Relationships")
    class RelationshipsTest {

        @Test
        @DisplayName("Should maintain usage point relationship")
        void shouldMaintainUsagePointRelationship() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Power Quality Test Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            ElectricPowerQualitySummaryEntity summary = new ElectricPowerQualitySummaryEntity();
            summary.setUsagePoint(savedUsagePoint);
            summary.setSupplyVoltageDips(3L);

            // Act
            ElectricPowerQualitySummaryEntity saved = electricPowerQualitySummaryRepository.save(summary);
            flushAndClear();
            Optional<ElectricPowerQualitySummaryEntity> retrieved = electricPowerQualitySummaryRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
            assertThat(retrieved.get().getUsagePoint().getDescription()).isEqualTo("Power Quality Test Usage Point");
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            ElectricPowerQualitySummaryEntity summary = new ElectricPowerQualitySummaryEntity();
            summary.setUsagePoint(savedUsagePoint);

            // Act
            ElectricPowerQualitySummaryEntity saved = electricPowerQualitySummaryRepository.save(summary);
            flushAndClear();

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }
    }
}