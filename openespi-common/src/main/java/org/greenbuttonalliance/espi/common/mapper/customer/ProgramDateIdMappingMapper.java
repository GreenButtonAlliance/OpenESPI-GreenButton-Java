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

import org.greenbuttonalliance.espi.common.domain.customer.common.ProgramDateIdMapping;
import org.greenbuttonalliance.espi.common.dto.customer.ProgramDateIdMappingDto;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between ProgramDateIdMapping and ProgramDateIdMappingDto.
 * <p>
 * Maps customer energy efficiency program date mapping fields per customer.xsd ProgramDateIdMapping type (lines 1223-1251).
 * Includes programDateType, code, name, and note fields from ESPI 4.0 customer.xsd.
 */
@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface ProgramDateIdMappingMapper {

    /**
     * Converts ProgramDateIdMapping entity to ProgramDateIdMappingDto.
     * Maps the 4 fields defined in customer.xsd: programDateType, code, name, note.
     *
     * @param entity the program date ID mapping entity
     * @return the program date ID mapping DTO
     */
    @Mapping(target = "programDateType", source = "programDateType")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "note", source = "note")
    ProgramDateIdMappingDto toDto(ProgramDateIdMapping entity);

    /**
     * Converts ProgramDateIdMappingDto to ProgramDateIdMapping entity.
     * Maps the 4 fields from customer.xsd.
     *
     * @param dto the program date ID mapping DTO
     * @return the program date ID mapping entity
     */
    @Mapping(target = "programDateType", source = "programDateType")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "note", source = "note")
    ProgramDateIdMapping toEntity(ProgramDateIdMappingDto dto);
}
