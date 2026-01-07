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

import org.greenbuttonalliance.espi.common.domain.usage.ServiceDeliveryPointEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ServiceDeliveryPointDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between ServiceDeliveryPointEntity and ServiceDeliveryPointDto.
 * <p>
 * ServiceDeliveryPoint extends Object (not IdentifiedObject) in ESPI 4.0 XSD (espi.xsd:1161),
 * so it does not have Atom links or timestamps - only business data fields.
 * <p>
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring")
public interface ServiceDeliveryPointMapper {

    /**
     * Converts a ServiceDeliveryPointEntity to a ServiceDeliveryPointDto.
     * Maps service delivery point attributes per ESPI 4.0 XSD.
     *
     * @param entity the service delivery point entity
     * @return the service delivery point DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true) // No mRID in entity or XSD
    @Mapping(target = "description", ignore = true) // Not in XSD for ServiceDeliveryPoint
    @Mapping(target = "tariffRiderRefs", ignore = true) // Relationship handled separately
    ServiceDeliveryPointDto toDto(ServiceDeliveryPointEntity entity);

    /**
     * Converts a ServiceDeliveryPointDto to a ServiceDeliveryPointEntity.
     * Maps service delivery point attributes per ESPI 4.0 XSD.
     *
     * @param dto the service delivery point DTO
     * @return the service delivery point entity
     */
    @Mapping(target = "id", ignore = true)
    ServiceDeliveryPointEntity toEntity(ServiceDeliveryPointDto dto);

    /**
     * Updates an existing ServiceDeliveryPointEntity with data from a ServiceDeliveryPointDto.
     * Useful for merge operations where entity values need to be updated.
     *
     * @param dto the source DTO
     * @param entity the target entity to update
     */
    @Mapping(target = "id", ignore = true)
    void updateEntity(ServiceDeliveryPointDto dto, @MappingTarget ServiceDeliveryPointEntity entity);
}