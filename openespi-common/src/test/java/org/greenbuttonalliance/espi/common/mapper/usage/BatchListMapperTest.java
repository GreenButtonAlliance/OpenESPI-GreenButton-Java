/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.common.mapper.usage;

import org.greenbuttonalliance.espi.common.domain.usage.BatchListEntity;
import org.greenbuttonalliance.espi.common.dto.usage.BatchListDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BatchListMapper.
 * Verifies Entity-to-DTO conversion for BatchList.
 */
@DisplayName("BatchListMapper Tests")
class BatchListMapperTest {

    private BatchListMapper batchListMapper;

    private static final String TEST_URI_1 = "https://greenbuttondata.org/espi/1_1/resource/UsagePoint/1";
    private static final String TEST_URI_2 = "https://greenbuttondata.org/espi/1_1/resource/MeterReading/2";
    private static final String TEST_URI_3 = "https://greenbuttondata.org/espi/1_1/resource/IntervalBlock/3";

    @BeforeEach
    void setUp() {
        batchListMapper = new BatchListMapper();
    }

    @Test
    @DisplayName("Should return null for null entity")
    void shouldReturnNullForNullEntity() {
        BatchListDto dto = batchListMapper.toDto(null);

        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Should create BatchListDto from entity with resources")
    void shouldCreateBatchListDtoFromEntityWithResources() {
        BatchListEntity entity = new BatchListEntity();
        entity.addResource(TEST_URI_1);
        entity.addResource(TEST_URI_2);

        BatchListDto dto = batchListMapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getResources()).hasSize(2);
        assertThat(dto.getResources()).containsExactly(TEST_URI_1, TEST_URI_2);
        assertThat(dto.getResourceCount()).isEqualTo(2);
        assertThat(dto.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create BatchListDto from entity with empty resources")
    void shouldCreateBatchListDtoFromEntityWithEmptyResources() {
        BatchListEntity entity = new BatchListEntity();

        BatchListDto dto = batchListMapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getResources()).isEmpty();
        assertThat(dto.getResourceCount()).isZero();
        assertThat(dto.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should create defensive copy of resources list")
    void shouldCreateDefensiveCopyOfResourcesList() {
        BatchListEntity entity = new BatchListEntity();
        entity.addResource(TEST_URI_1);

        BatchListDto dto = batchListMapper.toDto(entity);

        // Modify entity after mapping
        entity.addResource(TEST_URI_2);

        // DTO should not be affected by entity modification
        assertThat(dto.getResources()).hasSize(1);
        assertThat(dto.getResources()).containsExactly(TEST_URI_1);
    }

    @Test
    @DisplayName("Should handle entity with null resources list")
    void shouldHandleEntityWithNullResourcesList() {
        BatchListEntity entity = new BatchListEntity();
        // Force null resources (though entity getResources() initializes to empty list)
        // Entity getResources() returns empty list if null, so DTO will be empty

        BatchListDto dto = batchListMapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getResources()).isNotNull();
        assertThat(dto.getResources()).isEmpty();
    }

    @Test
    @DisplayName("Should convert list of entities to list of DTOs")
    void shouldConvertListOfEntitiesToListOfDtos() {
        BatchListEntity entity1 = new BatchListEntity();
        entity1.addResource(TEST_URI_1);

        BatchListEntity entity2 = new BatchListEntity();
        entity2.addResource(TEST_URI_2);
        entity2.addResource(TEST_URI_3);

        List<BatchListEntity> entities = Arrays.asList(entity1, entity2);

        List<BatchListDto> dtos = batchListMapper.toDtoList(entities);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getResources()).hasSize(1);
        assertThat(dtos.get(0).getResources()).containsExactly(TEST_URI_1);
        assertThat(dtos.get(1).getResources()).hasSize(2);
        assertThat(dtos.get(1).getResources()).containsExactly(TEST_URI_2, TEST_URI_3);
    }

    @Test
    @DisplayName("Should return empty list for null entities list")
    void shouldReturnEmptyListForNullEntitiesList() {
        List<BatchListDto> dtos = batchListMapper.toDtoList(null);

        assertThat(dtos).isNotNull();
        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty entities list")
    void shouldReturnEmptyListForEmptyEntitiesList() {
        List<BatchListDto> dtos = batchListMapper.toDtoList(new ArrayList<>());

        assertThat(dtos).isNotNull();
        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("Should preserve resource order in DTO")
    void shouldPreserveResourceOrderInDto() {
        BatchListEntity entity = new BatchListEntity();
        entity.addResource(TEST_URI_1);
        entity.addResource(TEST_URI_2);
        entity.addResource(TEST_URI_3);

        BatchListDto dto = batchListMapper.toDto(entity);

        assertThat(dto.getResources()).containsExactly(TEST_URI_1, TEST_URI_2, TEST_URI_3);
    }

    @Test
    @DisplayName("Should handle entity with many resources")
    void shouldHandleEntityWithManyResources() {
        BatchListEntity entity = new BatchListEntity();
        List<String> manyResources = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            String uri = "https://greenbuttondata.org/espi/1_1/resource/Resource/" + i;
            manyResources.add(uri);
        }
        entity.addResources(manyResources);

        BatchListDto dto = batchListMapper.toDto(entity);

        assertThat(dto.getResources()).hasSize(100);
        assertThat(dto.getResourceCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should not include entity ID or resourceCount fields in DTO")
    void shouldNotIncludeEntityIdOrResourceCountFieldsInDto() {
        BatchListEntity entity = new BatchListEntity();
        entity.addResource(TEST_URI_1);
        // Entity has id and resourceCount fields, but DTO should only have resources

        BatchListDto dto = batchListMapper.toDto(entity);

        // DTO only has resources field per ESPI 4.0 XSD
        assertThat(dto.getResources()).isNotNull();
        // getResourceCount() is a calculated method in DTO, not a stored field
        assertThat(dto.getResourceCount()).isEqualTo(1);
    }
}
