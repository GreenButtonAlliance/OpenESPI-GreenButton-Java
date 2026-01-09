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

import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.mapper.BaseIdentifiedObjectMapper;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between MeterReadingEntity and MeterReadingDto.
 *
 * Per ESPI 4.0 specification, MeterReading has NO child elements - only relationships
 * expressed via Atom links. The DTO contains NO fields beyond id/uuid.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface MeterReadingMapper {

    /**
     * Converts a MeterReadingEntity to a MeterReadingDto.
     * Maps all related entities to their corresponding DTOs.
     * 
     * @param entity the meter reading entity
     * @return the meter reading DTO
     */
    @Mapping(target = "id", ignore = true) // DTO id field not used
    @Mapping(target = "uuid", source = "id", qualifiedByName = "uuidToString")
    MeterReadingDto toDto(MeterReadingEntity entity);

    /**
     * Converts a MeterReadingDto to a MeterReadingEntity.
     * Since MeterReading has no child elements, only audit and relationship fields are managed.
     *
     * @param dto the meter reading DTO
     * @return the meter reading entity
     */
    @Mapping(target = "id", source = "uuid", qualifiedByName = "stringToUuid")
    @Mapping(target = "created", ignore = true) // Audit field managed by persistence
    @Mapping(target = "updated", ignore = true) // Audit field managed by persistence
    @Mapping(target = "published", ignore = true) // Audit field managed by persistence
    @Mapping(target = "description", ignore = true) // Managed separately
    @Mapping(target = "selfLink", ignore = true) // Link managed separately
    @Mapping(target = "upLink", ignore = true) // Link managed separately
    @Mapping(target = "relatedLinks", ignore = true) // Links managed separately
    @Mapping(target = "usagePoint", ignore = true) // Relationship managed separately
    @Mapping(target = "readingType", ignore = true) // Relationship managed separately
    @Mapping(target = "intervalBlocks", ignore = true) // Relationship managed separately
    MeterReadingEntity toEntity(MeterReadingDto dto);
}