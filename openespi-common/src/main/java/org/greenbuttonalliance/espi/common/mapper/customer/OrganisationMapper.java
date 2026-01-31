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
import org.greenbuttonalliance.espi.common.dto.customer.OrganisationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Organisation entity and DTO.
 *
 * Handles embedded StreetAddress, TelephoneNumber, and ElectronicAddress mappings.
 */
@Mapper(componentModel = "spring", uses = {
    StreetAddressMapper.class,
    TelephoneNumberMapper.class,
    ElectronicAddressMapper.class
})
public interface OrganisationMapper {

    /**
     * Converts an Organisation entity to a DTO.
     * Maps all embedded objects (addresses, phones, electronic address).
     *
     * @param entity the organisation entity
     * @return the organisation DTO
     */
    @Mapping(target = "streetAddress", source = "streetAddress")
    @Mapping(target = "postalAddress", source = "postalAddress")
    @Mapping(target = "phone1", source = "phone1")
    @Mapping(target = "phone2", source = "phone2")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "organisationName", source = "organisationName")
    OrganisationDto toDto(Organisation entity);

    /**
     * Converts an Organisation DTO to an entity.
     * Maps all embedded objects from DTO to entity.
     *
     * @param dto the organisation DTO
     * @return the organisation entity
     */
    @Mapping(target = "streetAddress", source = "streetAddress")
    @Mapping(target = "postalAddress", source = "postalAddress")
    @Mapping(target = "phone1", source = "phone1")
    @Mapping(target = "phone2", source = "phone2")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "organisationName", source = "organisationName")
    Organisation toEntity(OrganisationDto dto);
}
