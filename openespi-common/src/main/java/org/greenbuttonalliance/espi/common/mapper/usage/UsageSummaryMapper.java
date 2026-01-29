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

import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;
import org.greenbuttonalliance.espi.common.mapper.BaseIdentifiedObjectMapper;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.greenbuttonalliance.espi.common.mapper.SummaryMeasurementMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between UsageSummaryEntity and UsageSummaryDto.
 * <p>
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 * <p>
 * Maps only ESPI 4.0 XSD UsageSummary fields. IdentifiedObject fields are NOT part of
 * the espi.xsd UsageSummary definition and are handled by AtomFeedDto/AtomEntryDto.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class,
    DateTimeIntervalMapper.class,
    SummaryMeasurementMapper.class,
    LineItemMapper.class,
    TariffRiderRefMapper.class
})
public interface UsageSummaryMapper {

    /**
     * Converts a UsageSummaryEntity to a UsageSummaryDto.
     * Maps only ESPI 4.0 XSD UsageSummary fields (billing period, cost information, consumption summaries, tariff details).
     *
     * @param entity the usage summary entity
     * @return the usage summary DTO
     */
    @Mapping(target = "billingPeriod", source = "billingPeriod")
    @Mapping(target = "billLastPeriod", source = "billLastPeriod")
    @Mapping(target = "billToDate", source = "billToDate")
    @Mapping(target = "costAdditionalLastPeriod", source = "costAdditionalLastPeriod")
    @Mapping(target = "costAdditionalDetailLastPeriod", source = "costAdditionalDetailLastPeriod")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "overallConsumptionLastPeriod", source = "overallConsumptionLastPeriod")
    @Mapping(target = "currentBillingPeriodOverAllConsumption", source = "currentBillingPeriodOverAllConsumption")
    @Mapping(target = "currentDayLastYearNetConsumption", source = "currentDayLastYearNetConsumption")
    @Mapping(target = "currentDayNetConsumption", source = "currentDayNetConsumption")
    @Mapping(target = "currentDayOverallConsumption", source = "currentDayOverallConsumption")
    @Mapping(target = "peakDemand", source = "peakDemand")
    @Mapping(target = "previousDayLastYearOverallConsumption", source = "previousDayLastYearOverallConsumption")
    @Mapping(target = "previousDayNetConsumption", source = "previousDayNetConsumption")
    @Mapping(target = "previousDayOverallConsumption", source = "previousDayOverallConsumption")
    @Mapping(target = "qualityOfReading", source = "qualityOfReading")
    @Mapping(target = "ratchetDemand", source = "ratchetDemand")
    @Mapping(target = "ratchetDemandPeriod", source = "ratchetDemandPeriod")
    @Mapping(target = "statusTimeStamp", source = "statusTimeStamp")
    @Mapping(target = "commodity", source = "commodity")
    @Mapping(target = "tariffProfile", source = "tariffProfile")
    @Mapping(target = "readCycle", source = "readCycle")
    @Mapping(target = "tariffRiderRefs", source = "tariffRiderRefs", qualifiedByName = "entityListToTariffRiderRefsDto")
    @Mapping(target = "billingChargeSource", source = "billingChargeSource")
    UsageSummaryDto toDto(UsageSummaryEntity entity);

    /**
     * Converts a UsageSummaryDto to a UsageSummaryEntity.
     * Maps only ESPI 4.0 XSD UsageSummary fields (billing period, cost information, consumption summaries, tariff details).
     *
     * @param dto the usage summary DTO
     * @return the usage summary entity
     */
    @Mapping(target = "id", ignore = true) // IdentifiedObject field handled by Atom layer
    @Mapping(target = "billingPeriod", source = "billingPeriod")
    @Mapping(target = "billLastPeriod", source = "billLastPeriod")
    @Mapping(target = "billToDate", source = "billToDate")
    @Mapping(target = "costAdditionalLastPeriod", source = "costAdditionalLastPeriod")
    @Mapping(target = "costAdditionalDetailLastPeriod", source = "costAdditionalDetailLastPeriod")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "overallConsumptionLastPeriod", source = "overallConsumptionLastPeriod")
    @Mapping(target = "currentBillingPeriodOverAllConsumption", source = "currentBillingPeriodOverAllConsumption")
    @Mapping(target = "currentDayLastYearNetConsumption", source = "currentDayLastYearNetConsumption")
    @Mapping(target = "currentDayNetConsumption", source = "currentDayNetConsumption")
    @Mapping(target = "currentDayOverallConsumption", source = "currentDayOverallConsumption")
    @Mapping(target = "peakDemand", source = "peakDemand")
    @Mapping(target = "previousDayLastYearOverallConsumption", source = "previousDayLastYearOverallConsumption")
    @Mapping(target = "previousDayNetConsumption", source = "previousDayNetConsumption")
    @Mapping(target = "previousDayOverallConsumption", source = "previousDayOverallConsumption")
    @Mapping(target = "qualityOfReading", source = "qualityOfReading")
    @Mapping(target = "ratchetDemand", source = "ratchetDemand")
    @Mapping(target = "ratchetDemandPeriod", source = "ratchetDemandPeriod")
    @Mapping(target = "statusTimeStamp", source = "statusTimeStamp")
    @Mapping(target = "commodity", source = "commodity")
    @Mapping(target = "tariffProfile", source = "tariffProfile")
    @Mapping(target = "readCycle", source = "readCycle")
    @Mapping(target = "tariffRiderRefs", source = "tariffRiderRefs", qualifiedByName = "tariffRiderRefsDtoToEntityList")
    @Mapping(target = "billingChargeSource", source = "billingChargeSource")
    @Mapping(target = "usagePoint", ignore = true) // Relationship handled separately
    UsageSummaryEntity toEntity(UsageSummaryDto dto);

}