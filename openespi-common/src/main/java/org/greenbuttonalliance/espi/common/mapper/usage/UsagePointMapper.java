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

import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between UsagePointEntity and UsagePointDto.
 * <p>
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 * <p>
 * Phase 16d: Updated to map all ESPI 4.0 XSD UsagePoint fields including enums and extension fields.
 * IdentifiedObject fields are NOT part of the espi.xsd UsagePoint definition and are handled by AtomFeedDto/AtomEntryDto.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class,
    MeterReadingMapper.class,
    UsageSummaryMapper.class,
    ElectricPowerQualitySummaryMapper.class,
    ServiceDeliveryPointMapper.class,
    PnodeRefMapper.class,
    AggregatedNodeRefMapper.class
})
public interface UsagePointMapper {

    /**
     * Converts a UsagePointEntity to a UsagePointDto.
     * Maps only ESPI 4.0 XSD UsagePoint fields (enums, booleans, strings, SummaryMeasurements).
     *
     * @param entity the usage point entity
     * @return the usage point DTO
     */
    @Mapping(target = "roleFlags", source = "roleFlags")
    @Mapping(target = "serviceCategory", source = "serviceCategory")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "serviceDeliveryPoint", source = "serviceDeliveryPoint")
    @Mapping(target = "amiBillingReady", source = "amiBillingReady")
    @Mapping(target = "checkBilling", source = "checkBilling")
    @Mapping(target = "connectionState", source = "connectionState")
    @Mapping(target = "estimatedLoad", source = "estimatedLoad")
    @Mapping(target = "grounded", source = "grounded")
    @Mapping(target = "isSdp", source = "isSdp")
    @Mapping(target = "isVirtual", source = "isVirtual")
    @Mapping(target = "minimalUsageExpected", source = "minimalUsageExpected")
    @Mapping(target = "nominalServiceVoltage", source = "nominalServiceVoltage")
    @Mapping(target = "outageRegion", source = "outageRegion")
    @Mapping(target = "phaseCode", source = "phaseCode")
    @Mapping(target = "ratedCurrent", source = "ratedCurrent")
    @Mapping(target = "ratedPower", source = "ratedPower")
    @Mapping(target = "readCycle", source = "readCycle")
    @Mapping(target = "readRoute", source = "readRoute")
    @Mapping(target = "serviceDeliveryRemark", source = "serviceDeliveryRemark")
    @Mapping(target = "servicePriority", source = "servicePriority")
    @Mapping(target = "pnodeRefs", ignore = true) // TODO: Add mapper implementation
    @Mapping(target = "aggregatedNodeRefs", ignore = true) // TODO: Add mapper implementation
    @Mapping(target = "meterReadings", ignore = true) // Circular dependency - handle separately
    @Mapping(target = "usageSummaries", ignore = true) // Circular dependency - handle separately
    @Mapping(target = "electricPowerQualitySummaries", ignore = true) // Circular dependency - handle separately
    UsagePointDto toDto(UsagePointEntity entity);

    /**
     * Converts a UsagePointDto to a UsagePointEntity.
     * Maps only ESPI 4.0 XSD UsagePoint fields (enums, booleans, strings, SummaryMeasurements).
     *
     * @param dto the usage point DTO
     * @return the usage point entity
     */
    @Mapping(target = "roleFlags", source = "roleFlags")
    @Mapping(target = "serviceCategory", source = "serviceCategory")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "serviceDeliveryPoint", source = "serviceDeliveryPoint")
    @Mapping(target = "amiBillingReady", source = "amiBillingReady")
    @Mapping(target = "checkBilling", source = "checkBilling")
    @Mapping(target = "connectionState", source = "connectionState")
    @Mapping(target = "estimatedLoad", source = "estimatedLoad")
    @Mapping(target = "grounded", source = "grounded")
    @Mapping(target = "isSdp", source = "isSdp")
    @Mapping(target = "isVirtual", source = "isVirtual")
    @Mapping(target = "minimalUsageExpected", source = "minimalUsageExpected")
    @Mapping(target = "nominalServiceVoltage", source = "nominalServiceVoltage")
    @Mapping(target = "outageRegion", source = "outageRegion")
    @Mapping(target = "phaseCode", source = "phaseCode")
    @Mapping(target = "ratedCurrent", source = "ratedCurrent")
    @Mapping(target = "ratedPower", source = "ratedPower")
    @Mapping(target = "readCycle", source = "readCycle")
    @Mapping(target = "readRoute", source = "readRoute")
    @Mapping(target = "serviceDeliveryRemark", source = "serviceDeliveryRemark")
    @Mapping(target = "servicePriority", source = "servicePriority")
    @Mapping(target = "pnodeRefs", ignore = true) // TODO: Add mapper implementation
    @Mapping(target = "aggregatedNodeRefs", ignore = true) // TODO: Add mapper implementation
    @Mapping(target = "meterReadings", ignore = true) // Circular dependency - handle separately
    @Mapping(target = "usageSummaries", ignore = true) // Circular dependency - handle separately
    @Mapping(target = "electricPowerQualitySummaries", ignore = true) // Circular dependency - handle separately
    @Mapping(target = "retailCustomer", ignore = true) // Relationship - handled separately
    @Mapping(target = "localTimeParameters", ignore = true) // Relationship - handled separately
    @Mapping(target = "subscriptions", ignore = true) // Relationship - handled separately
    @Mapping(target = "subscription", ignore = true) // Relationship - handled separately
    UsagePointEntity toEntity(UsagePointDto dto);

}
