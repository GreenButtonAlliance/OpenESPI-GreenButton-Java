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

import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.dto.customer.MeterDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between MeterEntity and MeterDto.
 * <p>
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 * <p>
 * Maps customer.xsd Meter fields (lines 243-268) including:
 * - Asset fields (12) - lines 650-709
 * - EndDevice fields (4) - lines 219-238
 * - Meter fields (3) - lines 250-264
 * <p>
 * Note: meterMultipliers collection is deferred (commented out in entity).
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class,
    ElectronicAddressMapper.class,
    LifecycleDateMapper.class,
    AcceptanceTestMapper.class,
    StatusMapper.class
})
public interface MeterMapper {

    /**
     * Converts a MeterEntity to a MeterDto.
     * Maps all Asset fields (12), EndDevice fields (4), and Meter fields (3) per customer.xsd.
     *
     * @param entity the meter entity
     * @return the meter DTO
     */
    // Asset fields (12) - mapped from embedded asset
    @Mapping(target = "type", source = "asset.type")
    @Mapping(target = "utcNumber", source = "asset.utcNumber")
    @Mapping(target = "serialNumber", source = "asset.serialNumber")
    @Mapping(target = "lotNumber", source = "asset.lotNumber")
    @Mapping(target = "purchasePrice", source = "asset.purchasePrice")
    @Mapping(target = "critical", source = "asset.critical")
    @Mapping(target = "electronicAddress", source = "asset.electronicAddress")
    @Mapping(target = "lifecycle", source = "asset.lifecycle")
    @Mapping(target = "acceptanceTest", source = "asset.acceptanceTest")
    @Mapping(target = "initialCondition", source = "asset.initialCondition")
    @Mapping(target = "initialLossOfLife", source = "asset.initialLossOfLife")
    @Mapping(target = "status", source = "asset.status")
    // EndDevice fields (4) - mapped from embedded endDeviceFields
    @Mapping(target = "isVirtual", source = "endDeviceFields.isVirtual")
    @Mapping(target = "isPan", source = "endDeviceFields.isPan")
    @Mapping(target = "installCode", source = "endDeviceFields.installCode")
    @Mapping(target = "amrSystem", source = "endDeviceFields.amrSystem")
    // Meter fields (3) - customer.xsd lines 250-264
    @Mapping(target = "formNumber", source = "formNumber")
    @Mapping(target = "meterMultipliers", ignore = true) // TODO: Implement when MeterMultiplierEntity exists
    @Mapping(target = "intervalLength", source = "intervalLength")
    MeterDto toDto(MeterEntity entity);

    /**
     * Converts a MeterDto to a MeterEntity.
     * Maps all Asset fields (12), EndDevice fields (4), and Meter fields (3) per customer.xsd.
     *
     * @param dto the meter DTO
     * @return the meter entity
     */
    // Asset fields (12) - mapped to embedded asset
    @Mapping(target = "asset.type", source = "type")
    @Mapping(target = "asset.utcNumber", source = "utcNumber")
    @Mapping(target = "asset.serialNumber", source = "serialNumber")
    @Mapping(target = "asset.lotNumber", source = "lotNumber")
    @Mapping(target = "asset.purchasePrice", source = "purchasePrice")
    @Mapping(target = "asset.critical", source = "critical")
    @Mapping(target = "asset.electronicAddress", source = "electronicAddress")
    @Mapping(target = "asset.lifecycle", source = "lifecycle")
    @Mapping(target = "asset.acceptanceTest", source = "acceptanceTest")
    @Mapping(target = "asset.initialCondition", source = "initialCondition")
    @Mapping(target = "asset.initialLossOfLife", source = "initialLossOfLife")
    @Mapping(target = "asset.status", source = "status")
    // EndDevice fields (4) - mapped to embedded endDeviceFields
    @Mapping(target = "endDeviceFields.isVirtual", source = "isVirtual")
    @Mapping(target = "endDeviceFields.isPan", source = "isPan")
    @Mapping(target = "endDeviceFields.installCode", source = "installCode")
    @Mapping(target = "endDeviceFields.amrSystem", source = "amrSystem")
    // Meter fields (3) - customer.xsd lines 250-264
    @Mapping(target = "formNumber", source = "formNumber")
    // meterMultipliers not mapped - collection commented out in entity (TODO)
    @Mapping(target = "intervalLength", source = "intervalLength")
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
    MeterEntity toEntity(MeterDto dto);
}
