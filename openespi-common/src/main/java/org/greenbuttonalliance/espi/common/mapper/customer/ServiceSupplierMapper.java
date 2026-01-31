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

import org.greenbuttonalliance.espi.common.domain.customer.entity.ServiceSupplierEntity;
import org.greenbuttonalliance.espi.common.dto.customer.ServiceSupplierDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between ServiceSupplierEntity and ServiceSupplierDto.
 *
 * Maps only ServiceSupplier fields. IdentifiedObject fields are NOT part of the customer.xsd
 * definition and are handled by AtomFeedDto/AtomEntryDto.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    OrganisationMapper.class
})
public interface ServiceSupplierMapper {

    /**
     * Converts a ServiceSupplierEntity to a ServiceSupplierDto.
     * Maps service supplier information including embedded organisation.
     *
     * @param entity the service supplier entity
     * @return the service supplier DTO
     */
    @Mapping(target = "organisation", source = "organisation")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "issuerIdentificationNumber", source = "issuerIdentificationNumber")
    @Mapping(target = "effectiveDate", source = "effectiveDate")
    ServiceSupplierDto toDto(ServiceSupplierEntity entity);

    /**
     * Converts a ServiceSupplierDto to a ServiceSupplierEntity.
     * Maps service supplier information from DTO to entity.
     *
     * @param dto the service supplier DTO
     * @return the service supplier entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "organisation", source = "organisation")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "issuerIdentificationNumber", source = "issuerIdentificationNumber")
    @Mapping(target = "effectiveDate", source = "effectiveDate")
    ServiceSupplierEntity toEntity(ServiceSupplierDto dto);

    /**
     * Updates an existing ServiceSupplierEntity with data from a ServiceSupplierDto.
     * Does not update IdentifiedObject fields (id, selfLink, upLink, published, updated, created, description).
     *
     * @param dto the service supplier DTO with updated data
     * @param entity the existing service supplier entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "organisation", source = "organisation")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "issuerIdentificationNumber", source = "issuerIdentificationNumber")
    @Mapping(target = "effectiveDate", source = "effectiveDate")
    void updateEntityFromDto(ServiceSupplierDto dto, @MappingTarget ServiceSupplierEntity entity);
}