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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for AggregatedNodeRefRepository.
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
            
            AggregatedNodeRefEntity aggregatedNodeRef = new AggregatedNodeRefEntity();
            aggregatedNodeRef.setAnodeType("LOAD_ZONE");
            aggregatedNodeRef.setRef("TEST_AGGREGATE_NODE");
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
        }
    }
}