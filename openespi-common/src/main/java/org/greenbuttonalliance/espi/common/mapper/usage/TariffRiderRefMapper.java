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

import org.greenbuttonalliance.espi.common.domain.common.TariffRiderRefEntity;
import org.greenbuttonalliance.espi.common.dto.usage.TariffRiderRefDto;
import org.greenbuttonalliance.espi.common.dto.usage.TariffRiderRefsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper for converting between TariffRiderRefEntity and TariffRiderRefDto.
 * <p>
 * TariffRiderRef extends Object (not IdentifiedObject) in ESPI 4.0, so it does not
 * have Atom links or timestamps - only business data fields.
 * <p>
 * Also provides conversion methods for TariffRiderRefs wrapper DTO which contains
 * a collection of TariffRiderRef items.
 */
@Mapper(componentModel = "spring")
public interface TariffRiderRefMapper {

    /**
     * Converts a TariffRiderRefEntity to a TariffRiderRefDto.
     *
     * @param entity the tariff rider ref entity
     * @return the tariff rider ref DTO
     */
    TariffRiderRefDto toDto(TariffRiderRefEntity entity);

    /**
     * Converts a TariffRiderRefDto to a TariffRiderRefEntity.
     *
     * @param dto the tariff rider ref DTO
     * @return the tariff rider ref entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usageSummary", ignore = true)
    TariffRiderRefEntity toEntity(TariffRiderRefDto dto);

    /**
     * Converts a list of TariffRiderRefDto to a list of TariffRiderRefEntity.
     *
     * @param dtos the list of tariff rider ref DTOs
     * @return the list of tariff rider ref entities
     */
    List<TariffRiderRefEntity> toEntityList(List<TariffRiderRefDto> dtos);

    /**
     * Converts a list of TariffRiderRefEntity to a list of TariffRiderRefDto.
     *
     * @param entities the list of tariff rider ref entities
     * @return the list of tariff rider ref DTOs
     */
    List<TariffRiderRefDto> toDtoList(List<TariffRiderRefEntity> entities);

    /**
     * Converts a TariffRiderRefsDto wrapper to a list of TariffRiderRefEntity.
     * Extracts the list from the wrapper DTO.
     *
     * @param refsDto the wrapper DTO containing tariff rider refs
     * @return the list of tariff rider ref entities, or null if input is null
     */
    @Named("tariffRiderRefsDtoToEntityList")
    default List<TariffRiderRefEntity> tariffRiderRefsDtoToEntityList(TariffRiderRefsDto refsDto) {
        if (refsDto == null) {
            return null;
        }
        return toEntityList(refsDto.tariffRiderRefs());
    }

    /**
     * Converts a list of TariffRiderRefEntity to a TariffRiderRefsDto wrapper.
     * Wraps the list in the TariffRiderRefsDto container.
     *
     * @param entities the list of tariff rider ref entities
     * @return the wrapper DTO containing tariff rider refs, or null if input is null
     */
    @Named("entityListToTariffRiderRefsDto")
    default TariffRiderRefsDto entityListToTariffRiderRefsDto(List<TariffRiderRefEntity> entities) {
        if (entities == null) {
            return null;
        }
        return new TariffRiderRefsDto(toDtoList(entities));
    }
}
