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

import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Organisation entity and DTO.
 */
@Mapper(componentModel = "spring", uses = {
    StreetAddressMapper.class,
    ElectronicAddressMapper.class
})
public interface OrganisationMapper {

    /**
     * Converts an Organisation entity to a DTO.
     * Note: phone1 and phone2 are not included in the entity due to JPA column mapping conflicts.
     *
     * @param entity the organisation entity
     * @return the organisation DTO
     */
    @Mapping(target = "phone1", ignore = true) // Not in entity - managed separately
    @Mapping(target = "phone2", ignore = true) // Not in entity - managed separately
    CustomerDto.OrganisationDto toDto(Organisation entity);

    /**
     * Converts an Organisation DTO to an entity.
     * Note: phone1 and phone2 are not included in the entity due to JPA column mapping conflicts.
     *
     * @param dto the organisation DTO
     * @return the organisation entity
     */
    Organisation toEntity(CustomerDto.OrganisationDto dto);
}
