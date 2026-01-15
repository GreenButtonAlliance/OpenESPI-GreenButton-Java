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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting BatchListEntity to BatchListDto.
 *
 * <p>This is an output-only mapper that creates DTOs for BatchList data.
 * BatchList is a simple operational entity containing a list of resource URIs
 * for batch processing operations.</p>
 *
 * <p>Note: This mapper does not support DTO-to-Entity conversion as
 * BatchList DTOs are output-only for XML marshalling.</p>
 */
@Component
public class BatchListMapper {

    /**
     * Converts a BatchListEntity to a BatchListDto.
     *
     * <p>The mapping is straightforward - only the resources list is transferred
     * from entity to DTO. The entity's id and resourceCount fields are not included
     * in the DTO per ESPI 4.0 XSD specification.</p>
     *
     * @param entity the batch list entity to convert
     * @return the batch list DTO, or null if entity is null
     */
    public BatchListDto toDto(BatchListEntity entity) {
        if (entity == null) {
            return null;
        }

        // Create defensive copy of resources list
        List<String> resources = entity.getResources() != null
            ? new ArrayList<>(entity.getResources())
            : new ArrayList<>();

        return new BatchListDto(resources);
    }

    /**
     * Converts a list of BatchListEntity objects to BatchListDto objects.
     *
     * <p>This is a convenience method for batch conversions.</p>
     *
     * @param entities the list of batch list entities to convert
     * @return the list of batch list DTOs, empty list if input is null or empty
     */
    public List<BatchListDto> toDtoList(List<BatchListEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        return entities.stream()
            .map(this::toDto)
            .toList();
    }
}
