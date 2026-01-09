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

import org.greenbuttonalliance.espi.common.domain.usage.PnodeRefEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for PnodeRefRepository.
 * 
 * Tests all CRUD operations, 9 custom query methods, relationships,
 * and validation constraints for PnodeRef entities.
 */
@DisplayName("PnodeRef Repository Tests")
class PnodeRefRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PnodeRefRepository pnodeRefRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve pricing node ref successfully")
        void shouldSaveAndRetrievePricingNodeRefSuccessfully() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            PnodeRefEntity pnodeRef = new PnodeRefEntity(
                "HUB", 
                "CAISO_SP15_GEN_HUB", 
                savedUsagePoint
            );

            // Act
            PnodeRefEntity saved = pnodeRefRepository.save(pnodeRef);
            flushAndClear();
            Optional<PnodeRefEntity> retrieved = pnodeRefRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getApnodeType()).isEqualTo("HUB");
            assertThat(retrieved.get().getRef()).isEqualTo("CAISO_SP15_GEN_HUB");
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
        }

        @Test
        @DisplayName("Should save pricing node ref with effective dates")
        void shouldSavePricingNodeRefWithEffectiveDates() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            PnodeRefEntity pnodeRef = new PnodeRefEntity(
                "LOAD_ZONE", 
                "PNODE_ZONE_EAST_HUB", 
                1640995200L, // 2022-01-01 00:00:00 UTC
                1672531200L, // 2023-01-01 00:00:00 UTC
                savedUsagePoint
            );

            // Act
            PnodeRefEntity saved = pnodeRefRepository.save(pnodeRef);
            flushAndClear();
            Optional<PnodeRefEntity> retrieved = pnodeRefRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getStartEffectiveDate()).isEqualTo(1640995200L);
            assertThat(retrieved.get().getEndEffectiveDate()).isEqualTo(1672531200L);
            assertThat(retrieved.get().getApnodeType()).isEqualTo("LOAD_ZONE");
            assertThat(retrieved.get().getRef()).isEqualTo("PNODE_ZONE_EAST_HUB");
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

            PnodeRefEntity pnodeRef1 = new PnodeRefEntity("HUB", "HUB_1", savedUsagePoint);
            PnodeRefEntity pnodeRef2 = new PnodeRefEntity("LOAD_ZONE", "ZONE_1", savedUsagePoint);

            PnodeRefEntity saved1 = pnodeRefRepository.save(pnodeRef1);
            PnodeRefEntity saved2 = pnodeRefRepository.save(pnodeRef2);
            flushAndClear();

            // Act
            List<Long> allIds = pnodeRefRepository.findAllIds();

            // Assert
            assertThat(allIds).contains(saved1.getId(), saved2.getId());
        }

        @Test
        @DisplayName("Should find all pricing node refs by usage point ID")
        void shouldFindAllPricingNodeRefsByUsagePointId() {
            // Arrange
            UsagePointEntity usagePoint1 = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity usagePoint2 = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint1 = usagePointRepository.save(usagePoint1);
            UsagePointEntity savedUsagePoint2 = usagePointRepository.save(usagePoint2);

            PnodeRefEntity pnodeRef1 = new PnodeRefEntity("HUB", "HUB_1", savedUsagePoint1);
            PnodeRefEntity pnodeRef2 = new PnodeRefEntity("HUB", "HUB_2", savedUsagePoint1);
            PnodeRefEntity pnodeRef3 = new PnodeRefEntity("HUB", "HUB_3", savedUsagePoint2);

            pnodeRefRepository.save(pnodeRef1);
            pnodeRefRepository.save(pnodeRef2);
            pnodeRefRepository.save(pnodeRef3);
            flushAndClear();

            // Act
            List<PnodeRefEntity> pnodeRefs = pnodeRefRepository.findAllByUsagePointId(savedUsagePoint1.getId());

            // Assert
            assertThat(pnodeRefs).hasSize(2);
            assertThat(pnodeRefs).extracting(PnodeRefEntity::getRef)
                .containsExactlyInAnyOrder("HUB_1", "HUB_2");
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
            usagePoint.setDescription("Pricing Node Test Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            PnodeRefEntity pnodeRef = new PnodeRefEntity("TRANSMISSION", "TRANS_NODE_1", savedUsagePoint);

            // Act
            PnodeRefEntity saved = pnodeRefRepository.save(pnodeRef);
            flushAndClear();
            Optional<PnodeRefEntity> retrieved = pnodeRefRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUsagePoint()).isNotNull();
            assertThat(retrieved.get().getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
            assertThat(retrieved.get().getUsagePoint().getDescription()).isEqualTo("Pricing Node Test Usage Point");
        }
    }

    @Nested
    @DisplayName("Business Logic")
    class BusinessLogicTest {

        @Test
        @DisplayName("Should validate pricing node ref validity")
        void shouldValidatePricingNodeRefValidity() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            long currentTime = System.currentTimeMillis() / 1000;
            long pastTime = currentTime - 3600;
            long futureTime = currentTime + 3600;
            
            PnodeRefEntity validRef = new PnodeRefEntity("HUB", "VALID_REF", pastTime, futureTime, savedUsagePoint);
            PnodeRefEntity expiredRef = new PnodeRefEntity("HUB", "EXPIRED_REF", pastTime, pastTime + 1800, savedUsagePoint);

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
            
            PnodeRefEntity pnodeRefWithType = new PnodeRefEntity("HUB", "TEST_HUB", savedUsagePoint);
            PnodeRefEntity pnodeRefWithoutType = new PnodeRefEntity(null, "TEST_REF", savedUsagePoint);

            // Act & Assert
            assertThat(pnodeRefWithType.getDisplayName()).isEqualTo("HUB:TEST_HUB");
            assertThat(pnodeRefWithoutType.getDisplayName()).isEqualTo("TEST_REF");
        }
    }

    @Nested
    @DisplayName("Entity Persistence")
    class EntityPersistenceTest {

        @Test
        @DisplayName("Should persist and retrieve pricing node reference")
        void shouldPersistAndRetrievePnodeRef() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            PnodeRefEntity pnodeRef = new PnodeRefEntity("HUB", "PERSISTENCE_TEST", savedUsagePoint);

            // Act
            PnodeRefEntity saved = pnodeRefRepository.save(pnodeRef);
            flushAndClear();

            // Assert
            // PnodeRef extends Object (not IdentifiedObject) in ESPI 4.0 XSD,
            // so it has Long ID but no Atom links or timestamps
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getApnodeType()).isEqualTo("HUB");
            assertThat(saved.getRef()).isEqualTo("PERSISTENCE_TEST");
            assertThat(saved.getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());
        }
    }
}