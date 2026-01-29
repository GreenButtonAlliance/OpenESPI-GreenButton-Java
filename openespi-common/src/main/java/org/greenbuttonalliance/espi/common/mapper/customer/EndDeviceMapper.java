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

import org.greenbuttonalliance.espi.common.domain.customer.entity.EndDeviceEntity;
import org.greenbuttonalliance.espi.common.dto.customer.EndDeviceDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between EndDeviceEntity and EndDeviceDto.
 * <p>
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 * <p>
 * Maps customer.xsd EndDevice fields (lines 888-958) including Asset base fields (lines 651-797).
 * EndDevice extends Asset (which extends Object per ESPI standard, NOT IdentifiedObject).
 * Asset fields are embedded inline in EndDeviceEntity.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class,
    ElectronicAddressMapper.class,
    LifecycleDateMapper.class,
    AcceptanceTestMapper.class,
    StatusMapper.class
})
public interface EndDeviceMapper {

    /**
     * Converts an EndDeviceEntity to an EndDeviceDto.
     * Maps all Asset fields (12) and EndDevice fields (4) per customer.xsd.
     *
     * @param entity the end device entity
     * @return the end device DTO
     */
    // Asset fields (12)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "utcNumber", source = "utcNumber")
    @Mapping(target = "serialNumber", source = "serialNumber")
    @Mapping(target = "lotNumber", source = "lotNumber")
    @Mapping(target = "purchasePrice", source = "purchasePrice")
    @Mapping(target = "critical", source = "critical")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "lifecycle", source = "lifecycle")
    @Mapping(target = "acceptanceTest", source = "acceptanceTest")
    @Mapping(target = "initialCondition", source = "initialCondition")
    @Mapping(target = "initialLossOfLife", source = "initialLossOfLife")
    @Mapping(target = "status", source = "status")
    // EndDevice fields (4)
    @Mapping(target = "isVirtual", source = "isVirtual")
    @Mapping(target = "isPan", source = "isPan")
    @Mapping(target = "installCode", source = "installCode")
    @Mapping(target = "amrSystem", source = "amrSystem")
    EndDeviceDto toDto(EndDeviceEntity entity);

    /**
     * Converts an EndDeviceDto to an EndDeviceEntity.
     * Maps all Asset fields (12) and EndDevice fields (4) per customer.xsd.
     *
     * @param dto the end device DTO
     * @return the end device entity
     */
    // Asset fields (12)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "utcNumber", source = "utcNumber")
    @Mapping(target = "serialNumber", source = "serialNumber")
    @Mapping(target = "lotNumber", source = "lotNumber")
    @Mapping(target = "purchasePrice", source = "purchasePrice")
    @Mapping(target = "critical", source = "critical")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "lifecycle", source = "lifecycle")
    @Mapping(target = "acceptanceTest", source = "acceptanceTest")
    @Mapping(target = "initialCondition", source = "initialCondition")
    @Mapping(target = "initialLossOfLife", source = "initialLossOfLife")
    @Mapping(target = "status", source = "status")
    // EndDevice fields (4)
    @Mapping(target = "isVirtual", source = "isVirtual")
    @Mapping(target = "isPan", source = "isPan")
    @Mapping(target = "installCode", source = "installCode")
    @Mapping(target = "amrSystem", source = "amrSystem")
    // IdentifiedObject fields (inherited) - handled by Atom layer, ignore during mapping
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    // JPA entity-only fields
    @Mapping(target = "relatedLinks", ignore = true)
    @Mapping(target = "relatedLinkHrefs", ignore = true)
    EndDeviceEntity toEntity(EndDeviceDto dto);
}
