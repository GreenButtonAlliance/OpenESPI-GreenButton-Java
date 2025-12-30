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

import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto;
import org.greenbuttonalliance.espi.common.mapper.BaseIdentifiedObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between TimeConfigurationEntity and TimeConfigurationDto.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 *
 * Maps timezone configuration including DST rules and offsets while preserving
 * proper byte array handling and field order compliance with ESPI 4.0 XSD.
 */
@Mapper(componentModel = "spring")
public interface TimeConfigurationMapper extends BaseIdentifiedObjectMapper {

    /**
     * Converts a TimeConfigurationEntity to a TimeConfigurationDto.
     * Maps timezone offset, DST rules, and DST offset.
     * Byte arrays are properly cloned to prevent external modification.
     *
     * @param entity the time configuration entity
     * @return the time configuration DTO
     */
    @Mapping(target = "id", ignore = true) // DTO id field not used
    @Mapping(target = "uuid", source = "id") // Map entity ID to DTO uuid for XML mRID
    TimeConfigurationDto toDto(TimeConfigurationEntity entity);

    /**
     * Converts a TimeConfigurationDto to a TimeConfigurationEntity.
     * Maps timezone offset, DST rules, and DST offset.
     * Byte arrays are properly cloned to prevent external modification.
     *
     * @param dto the time configuration DTO
     * @return the time configuration entity
     */
    @Mapping(target = "id", ignore = true) // ID set by persistence layer
    @Mapping(target = "usagePoints", ignore = true) // Collection managed separately
    @Mapping(target = "customer", ignore = true) // Relationship managed separately
    @Mapping(target = "created", ignore = true) // Audit field managed by persistence
    @Mapping(target = "updated", ignore = true) // Audit field managed by persistence
    @Mapping(target = "published", ignore = true) // Audit field managed by persistence
    @Mapping(target = "selfLink", ignore = true) // Link managed separately
    @Mapping(target = "upLink", ignore = true) // Link managed separately
    @Mapping(target = "relatedLinks", ignore = true) // Links managed separately
    @Mapping(target = "description", ignore = true) // Generated dynamically by entity
    TimeConfigurationEntity toEntity(TimeConfigurationDto dto);

    /**
     * Updates an existing TimeConfigurationEntity with data from a TimeConfigurationDto.
     * Useful for merge operations where entity values need to be updated without
     * creating a new instance.
     *
     * Preserves existing relationships and audit fields while updating time configuration data.
     *
     * @param dto the source DTO
     * @param entity the target entity to update
     */
    @Mapping(target = "id", ignore = true) // Never update ID
    @Mapping(target = "usagePoints", ignore = true) // Preserve existing collection
    @Mapping(target = "customer", ignore = true) // Preserve existing relationship
    @Mapping(target = "created", ignore = true) // Preserve audit field
    @Mapping(target = "updated", ignore = true) // Will be updated by persistence layer
    @Mapping(target = "published", ignore = true) // Preserve audit field
    @Mapping(target = "selfLink", ignore = true) // Preserve existing link
    @Mapping(target = "upLink", ignore = true) // Preserve existing link
    @Mapping(target = "relatedLinks", ignore = true) // Preserve existing links
    @Mapping(target = "description", ignore = true) // Generated dynamically by entity
    void updateEntity(TimeConfigurationDto dto, @MappingTarget TimeConfigurationEntity entity);
}
