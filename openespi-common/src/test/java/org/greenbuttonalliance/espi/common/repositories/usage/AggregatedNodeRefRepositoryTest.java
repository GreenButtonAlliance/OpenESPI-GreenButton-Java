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

import org.greenbuttonalliance.espi.common.domain.usage.AggregatedNodeRefEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.PnodeRefEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for AggregatedNodeRefRepository.
 *
 * Tests all CRUD operations, 2 custom query methods, relationships,
 * and validation constraints for AggregatedNodeRef entities.
 */
@DisplayName("AggregatedNodeRef Repository Tests")
class AggregatedNodeRefRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AggregatedNodeRefRepository aggregatedNodeRefRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private PnodeRefRepository pnodeRefRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve aggregated node ref successfully")
        void shouldSaveAndRetrieveAggregatedNodeRefSuccessfully() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            PnodeRefEntity pnodeRef = new PnodeRefEntity("HUB", "TEST_HUB", savedUsagePoint);
            PnodeRefEntity savedPnodeRef = pnodeRefRepository.save(pnodeRef);

            AggregatedNodeRefEntity aggregatedNodeRef = new AggregatedNodeRefEntity();
            aggregatedNodeRef.setAnodeType("LOAD_ZONE");
            aggregatedNodeRef.setRef("TEST_AGGREGATE_NODE");
            aggregatedNodeRef.setPnodeRefs(List.of(savedPnodeRef));
            aggregatedNodeRef.setUsagePoint(savedUsagePoint);

            // Act
            AggregatedNodeRefEntity saved = aggregatedNodeRefRepository.save(aggregatedNodeRef);
            flushAndClear();
            Optional<AggregatedNodeRefEntity> retrieved = aggregatedNodeRefRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAnodeType()).isEqualTo("LOAD_ZONE");
            assertThat(retrieved.get().getRef()).isEqualTo("TEST_AGGREGATE_NODE");
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
        }

        @Test
        @DisplayName("Should save aggregated node ref with effective dates")
        void shouldSaveAggregatedNodeRefWithEffectiveDates() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            PnodeRefEntity pnodeRef = new PnodeRefEntity("HUB", "TEST_HUB", savedUsagePoint);
            PnodeRefEntity savedPnodeRef = pnodeRefRepository.save(pnodeRef);

            AggregatedNodeRefEntity aggregatedNodeRef = new AggregatedNodeRefEntity();
            aggregatedNodeRef.setAnodeType("TRANSMISSION_ZONE");
            aggregatedNodeRef.setRef("ZONE_AGGREGATE");
            aggregatedNodeRef.setStartEffectiveDate(1640995200L); // 2022-01-01 00:00:00 UTC
            aggregatedNodeRef.setEndEffectiveDate(1672531200L);   // 2023-01-01 00:00:00 UTC
            aggregatedNodeRef.setPnodeRefs(List.of(savedPnodeRef));
            aggregatedNodeRef.setUsagePoint(savedUsagePoint);

            // Act
            AggregatedNodeRefEntity saved = aggregatedNodeRefRepository.save(aggregatedNodeRef);
            flushAndClear();
            Optional<AggregatedNodeRefEntity> retrieved = aggregatedNodeRefRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getStartEffectiveDate()).isEqualTo(1640995200L);
            assertThat(retrieved.get().getEndEffectiveDate()).isEqualTo(1672531200L);
            assertThat(retrieved.get().getAnodeType()).isEqualTo("TRANSMISSION_ZONE");
            assertThat(retrieved.get().getRef()).isEqualTo("ZONE_AGGREGATE");
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find all IDs")
        void shouldFindAllIds() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            AggregatedNodeRefEntity agg1 = new AggregatedNodeRefEntity();
            agg1.setAnodeType("LOAD_ZONE");
            agg1.setRef("ZONE_1");
            agg1.setUsagePoint(savedUsagePoint);

            AggregatedNodeRefEntity agg2 = new AggregatedNodeRefEntity();
            agg2.setAnodeType("TRANSMISSION_ZONE");
            agg2.setRef("ZONE_2");
            agg2.setUsagePoint(savedUsagePoint);

            AggregatedNodeRefEntity saved1 = aggregatedNodeRefRepository.save(agg1);
            AggregatedNodeRefEntity saved2 = aggregatedNodeRefRepository.save(agg2);
            flushAndClear();

            // Act
            List<Long> allIds = aggregatedNodeRefRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all aggregated node refs by usage point ID")
        void shouldFindAllAggregatedNodeRefsByUsagePointId() {
            // Arrange
            UsagePointEntity usagePoint1 = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity usagePoint2 = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint1 = usagePointRepository.save(usagePoint1);
            UsagePointEntity savedUsagePoint2 = usagePointRepository.save(usagePoint2);

            AggregatedNodeRefEntity agg1 = new AggregatedNodeRefEntity();
            agg1.setAnodeType("LOAD_ZONE");
            agg1.setRef("ZONE_1");
            agg1.setUsagePoint(savedUsagePoint1);

            AggregatedNodeRefEntity agg2 = new AggregatedNodeRefEntity();
            agg2.setAnodeType("LOAD_ZONE");
            agg2.setRef("ZONE_2");
            agg2.setUsagePoint(savedUsagePoint1);

            AggregatedNodeRefEntity agg3 = new AggregatedNodeRefEntity();
            agg3.setAnodeType("LOAD_ZONE");
            agg3.setRef("ZONE_3");
            agg3.setUsagePoint(savedUsagePoint2);

            aggregatedNodeRefRepository.save(agg1);
            aggregatedNodeRefRepository.save(agg2);
            aggregatedNodeRefRepository.save(agg3);
            flushAndClear();

            // Act
            List<AggregatedNodeRefEntity> aggregatedRefs = aggregatedNodeRefRepository.findAllByUsagePointId(savedUsagePoint1.getId());

            // Assert
            assertThat(aggregatedRefs).hasSize(2);
            assertThat(aggregatedRefs).extracting(AggregatedNodeRefEntity::getRef)
                .containsExactlyInAnyOrder("ZONE_1", "ZONE_2");
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
            usagePoint.setDescription("Aggregated Node Test Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            AggregatedNodeRefEntity aggregatedNodeRef = new AggregatedNodeRefEntity();
            aggregatedNodeRef.setAnodeType("DISTRIBUTION_ZONE");
            aggregatedNodeRef.setRef("DIST_ZONE_1");
            aggregatedNodeRef.setUsagePoint(savedUsagePoint);

            // Act
            AggregatedNodeRefEntity saved = aggregatedNodeRefRepository.save(aggregatedNodeRef);
            flushAndClear();
            Optional<AggregatedNodeRefEntity> retrieved = aggregatedNodeRefRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
            assertThat(retrieved.get().getUsagePoint().getDescription()).isEqualTo("Aggregated Node Test Usage Point");
        }

        @Test
        @DisplayName("Should maintain pricing node reference relationships")
        void shouldMaintainPricingNodeReferenceRelationships() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            PnodeRefEntity pnodeRef1 = new PnodeRefEntity("HUB", "CAISO_SP15_GEN_HUB", savedUsagePoint);
            PnodeRefEntity pnodeRef2 = new PnodeRefEntity("LOAD_ZONE", "CAISO_SP15_LOAD_ZONE", savedUsagePoint);
            PnodeRefEntity savedPnodeRef1 = pnodeRefRepository.save(pnodeRef1);
            PnodeRefEntity savedPnodeRef2 = pnodeRefRepository.save(pnodeRef2);

            AggregatedNodeRefEntity aggregatedNodeRef = new AggregatedNodeRefEntity();
            aggregatedNodeRef.setAnodeType("LOAD_ZONE");
            aggregatedNodeRef.setRef("CAISO_SP15_AGGREGATE");
            aggregatedNodeRef.setPnodeRefs(List.of(savedPnodeRef1, savedPnodeRef2));
            aggregatedNodeRef.setUsagePoint(savedUsagePoint);

            // Act
            AggregatedNodeRefEntity saved = aggregatedNodeRefRepository.save(aggregatedNodeRef);
            flushAndClear();
            Optional<AggregatedNodeRefEntity> retrieved = aggregatedNodeRefRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getPnodeRefs()).isNotNull();
            assertThat(retrieved.get().getPnodeRefs()).hasSize(2);
            assertThat(retrieved.get().getPnodeRefs()).extracting(PnodeRefEntity::getRef)
                .containsExactlyInAnyOrder("CAISO_SP15_GEN_HUB", "CAISO_SP15_LOAD_ZONE");
        }
    }

    @Nested
    @DisplayName("Business Logic")
    class BusinessLogicTest {

        @Test
        @DisplayName("Should validate aggregated node ref validity")
        void shouldValidateAggregatedNodeRefValidity() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            long currentTime = System.currentTimeMillis() / 1000;
            long pastTime = currentTime - 3600;
            long futureTime = currentTime + 3600;

            AggregatedNodeRefEntity validRef = new AggregatedNodeRefEntity();
            validRef.setAnodeType("LOAD_ZONE");
            validRef.setRef("VALID_REF");
            validRef.setStartEffectiveDate(pastTime);
            validRef.setEndEffectiveDate(futureTime);
            validRef.setUsagePoint(savedUsagePoint);

            AggregatedNodeRefEntity expiredRef = new AggregatedNodeRefEntity();
            expiredRef.setAnodeType("LOAD_ZONE");
            expiredRef.setRef("EXPIRED_REF");
            expiredRef.setStartEffectiveDate(pastTime);
            expiredRef.setEndEffectiveDate(pastTime + 1800);
            expiredRef.setUsagePoint(savedUsagePoint);

            // Act & Assert
            assertThat(validRef.isValid()).isTrue();
            assertThat(expiredRef.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should generate display name correctly")
        void shouldGenerateDisplayNameCorrectly() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            AggregatedNodeRefEntity aggWithType = new AggregatedNodeRefEntity();
            aggWithType.setAnodeType("LOAD_ZONE");
            aggWithType.setRef("TEST_ZONE");
            aggWithType.setUsagePoint(savedUsagePoint);

            AggregatedNodeRefEntity aggWithoutType = new AggregatedNodeRefEntity();
            aggWithoutType.setAnodeType(null);
            aggWithoutType.setRef("TEST_REF");
            aggWithoutType.setUsagePoint(savedUsagePoint);

            // Act & Assert
            assertThat(aggWithType.getDisplayName()).isEqualTo("LOAD_ZONE:TEST_ZONE");
            assertThat(aggWithoutType.getDisplayName()).isEqualTo("TEST_REF");
        }
    }

    @Nested
    @DisplayName("Entity Persistence")
    class EntityPersistenceTest {

        @Test
        @DisplayName("Should persist and retrieve aggregated node reference")
        void shouldPersistAndRetrieveAggregatedNodeRef() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            AggregatedNodeRefEntity aggregatedNodeRef = new AggregatedNodeRefEntity();
            aggregatedNodeRef.setAnodeType("LOAD_ZONE");
            aggregatedNodeRef.setRef("PERSISTENCE_TEST");
            aggregatedNodeRef.setUsagePoint(savedUsagePoint);

            // Act
            AggregatedNodeRefEntity saved = aggregatedNodeRefRepository.save(aggregatedNodeRef);
            flushAndClear();

            // Assert
            // AggregatedNodeRef extends Object (not IdentifiedObject) in ESPI 4.0 XSD,
            // so it has Long ID but no Atom links or timestamps
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getAnodeType()).isEqualTo("LOAD_ZONE");
            assertThat(saved.getRef()).isEqualTo("PERSISTENCE_TEST");
            assertThat(saved.getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
        }
    }
}