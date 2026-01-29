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
import org.greenbuttonalliance.espi.common.dto.customer.LifecycleDateDto;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Asset.LifecycleDate and LifecycleDateDto.
 * <p>
 * Maps lifecycle date fields per customer.xsd LifecycleDate type (lines 923-947).
 * Only manufacturedDate and installationDate are included in ESPI 4.0 customer.xsd.
 */
@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface LifecycleDateMapper {

    /**
     * Converts Asset.LifecycleDate entity to LifecycleDateDto.
     * Only maps the 2 fields defined in customer.xsd: manufacturedDate and installationDate.
     *
     * @param entity the lifecycle date entity
     * @return the lifecycle date DTO
     */
    @Mapping(target = "manufacturedDate", source = "manufacturedDate")
    @Mapping(target = "installationDate", source = "installationDate")
    LifecycleDateDto toDto(Asset.LifecycleDate entity);

    /**
     * Converts LifecycleDateDto to Asset.LifecycleDate entity.
     * Only maps the 2 fields from customer.xsd. Other entity-specific fields default to null.
     *
     * @param dto the lifecycle date DTO
     * @return the lifecycle date entity
     */
    @Mapping(target = "manufacturedDate", source = "manufacturedDate")
    @Mapping(target = "installationDate", source = "installationDate")
    @Mapping(target = "purchaseDate", ignore = true)
    @Mapping(target = "receivedDate", ignore = true)
    @Mapping(target = "retirementDate", ignore = true)
    @Mapping(target = "removalDate", ignore = true)
    Asset.LifecycleDate toEntity(LifecycleDateDto dto);
}
