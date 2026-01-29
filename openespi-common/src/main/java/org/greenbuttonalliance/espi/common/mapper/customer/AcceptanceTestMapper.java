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

import org.greenbuttonalliance.espi.common.domain.customer.entity.Asset;
import org.greenbuttonalliance.espi.common.dto.customer.AcceptanceTestDto;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Asset.AcceptanceTest and AcceptanceTestDto.
 * <p>
 * Maps acceptance test fields per customer.xsd AcceptanceTest type (lines 618-657).
 * Note: customer.xsd defines 4 fields (dateTime, success, type, remark) but Asset.AcceptanceTest
 * entity only has 3 fields (dateTime, success, type). The remark field is ignored during toEntity.
 */
@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface AcceptanceTestMapper {

    /**
     * Converts Asset.AcceptanceTest entity to AcceptanceTestDto.
     * Maps the 3 fields from entity. DTO's remark field will be null.
     *
     * @param entity the acceptance test entity
     * @return the acceptance test DTO
     */
    @Mapping(target = "dateTime", source = "dateTime")
    @Mapping(target = "success", source = "success")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "remark", ignore = true)
    AcceptanceTestDto toDto(Asset.AcceptanceTest entity);

    /**
     * Converts AcceptanceTestDto to Asset.AcceptanceTest entity.
     * Maps the 3 fields that exist in entity. DTO's remark field is ignored.
     *
     * @param dto the acceptance test DTO
     * @return the acceptance test entity
     */
    @Mapping(target = "dateTime", source = "dateTime")
    @Mapping(target = "success", source = "success")
    @Mapping(target = "type", source = "type")
    Asset.AcceptanceTest toEntity(AcceptanceTestDto dto);
}
