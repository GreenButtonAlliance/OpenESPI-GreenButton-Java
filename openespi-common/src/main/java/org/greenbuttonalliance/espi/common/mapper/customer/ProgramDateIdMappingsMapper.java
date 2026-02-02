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

package org.greenbuttonalliance.espi.common.mapper.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;
import org.greenbuttonalliance.espi.common.dto.customer.ProgramDateIdMappingsDto;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between ProgramDateIdMappingsEntity and ProgramDateIdMappingsDto.
 *
 * Maps only ProgramDateIdMappings fields. IdentifiedObject fields are NOT part of the customer.xsd
 * definition and are handled by AtomFeedDto/AtomEntryDto.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    ProgramDateIdMappingMapper.class
})
public interface ProgramDateIdMappingsMapper {

    /**
     * Converts a ProgramDateIdMappingsEntity to a ProgramDateIdMappingsDto.
     * Maps the embedded program date ID mapping information.
     *
     * @param entity the program date ID mappings entity
     * @return the program date ID mappings DTO
     */
    ProgramDateIdMappingsDto toDto(ProgramDateIdMappingsEntity entity);

    /**
     * Converts a ProgramDateIdMappingsDto to a ProgramDateIdMappingsEntity.
     * Maps program date ID mapping information from DTO to entity.
     *
     * @param dto the program date ID mappings DTO
     * @return the program date ID mappings entity
     */
    ProgramDateIdMappingsEntity toEntity(ProgramDateIdMappingsDto dto);

    /**
     * Updates an existing ProgramDateIdMappingsEntity with data from a ProgramDateIdMappingsDto.
     *
     * @param dto the program date ID mappings DTO with updated data
     * @param entity the existing program date ID mappings entity to update
     */
    void updateEntityFromDto(ProgramDateIdMappingsDto dto, @MappingTarget ProgramDateIdMappingsEntity entity);
}
