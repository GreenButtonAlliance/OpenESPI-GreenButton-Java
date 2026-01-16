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
 * Maps only TimeConfiguration fields. IdentifiedObject fields are NOT part of the usage.xsd
 * definition and are handled by AtomFeedDto/AtomEntryDto.
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
    @Mapping(target = "id", ignore = true) // IdentifiedObject field handled by Atom layer
    TimeConfigurationDto toDto(TimeConfigurationEntity entity);

    /**
     * Converts a TimeConfigurationDto to a TimeConfigurationEntity.
     * Maps timezone offset, DST rules, and DST offset.
     * Byte arrays are properly cloned to prevent external modification.
     *
     * @param dto the time configuration DTO
     * @return the time configuration entity
     */
    @Mapping(target = "id", ignore = true) // IdentifiedObject field handled by Atom layer
    @Mapping(target = "usagePoints", ignore = true) // Collection managed separately
    @Mapping(target = "customer", ignore = true) // Relationship managed separately
    TimeConfigurationEntity toEntity(TimeConfigurationDto dto);

}
