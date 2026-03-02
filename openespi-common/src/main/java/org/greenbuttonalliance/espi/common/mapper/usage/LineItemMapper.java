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

import org.greenbuttonalliance.espi.common.domain.usage.LineItemEntity;
import org.greenbuttonalliance.espi.common.dto.usage.LineItemDto;
import org.greenbuttonalliance.espi.common.mapper.SummaryMeasurementMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between LineItemEntity and LineItemDto.
 * <p>
 * LineItem extends Object (not IdentifiedObject) in ESPI 4.0, so it does not
 * have Atom links or timestamps - only business data fields.
 */
@Mapper(componentModel = "spring", uses = {SummaryMeasurementMapper.class, DateTimeIntervalMapper.class})
public interface LineItemMapper {

    /**
     * Converts a LineItemEntity to a LineItemDto.
     *
     * @param entity the line item entity
     * @return the line item DTO
     */
    LineItemDto toDto(LineItemEntity entity);

    /**
     * Converts a LineItemDto to a LineItemEntity.
     *
     * @param dto the line item DTO
     * @return the line item entity
     */
    @Mapping(target = "dateTimeFromLocalDateTime", ignore = true)
    @Mapping(target = "dateTimeFromInstant", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usageSummary", ignore = true)
    LineItemEntity toEntity(LineItemDto dto);
}
