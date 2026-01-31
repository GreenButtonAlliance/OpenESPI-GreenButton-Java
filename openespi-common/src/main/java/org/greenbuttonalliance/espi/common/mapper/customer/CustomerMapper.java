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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between CustomerEntity and CustomerDto.
 *
 * Maps only Customer fields. IdentifiedObject fields are NOT part of the customer.xsd
 * definition and are handled by AtomFeedDto/AtomEntryDto.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    OrganisationMapper.class
})
public interface CustomerMapper {

    /**
     * Converts a CustomerEntity to a CustomerDto.
     * Maps customer information including embedded objects.
     *
     * @param entity the customer entity
     * @return the customer DTO
     */
    @Mapping(target = "organisation", source = "organisation")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "specialNeed", source = "specialNeed")
    @Mapping(target = "vip", source = "vip")
    @Mapping(target = "pucNumber", source = "pucNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "locale", source = "locale")
    @Mapping(target = "customerName", source = "customerName")
    CustomerDto toDto(CustomerEntity entity);

    /**
     * Converts a CustomerDto to a CustomerEntity.
     * Maps customer information including embedded objects.
     *
     * @param dto the customer DTO
     * @return the customer entity
     */
    @Mapping(target = "organisation", source = "organisation")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "specialNeed", source = "specialNeed")
    @Mapping(target = "vip", source = "vip")
    @Mapping(target = "pucNumber", source = "pucNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "locale", source = "locale")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "customerAccounts", ignore = true)
    @Mapping(target = "timeConfiguration", ignore = true)
    @Mapping(target = "statements", ignore = true)
    CustomerEntity toEntity(CustomerDto dto);
}