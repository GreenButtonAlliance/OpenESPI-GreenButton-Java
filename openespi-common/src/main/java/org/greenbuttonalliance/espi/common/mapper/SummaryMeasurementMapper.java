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

package org.greenbuttonalliance.espi.common.mapper;

import org.greenbuttonalliance.espi.common.domain.common.SummaryMeasurement;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between SummaryMeasurement and SummaryMeasurementDto.
 *
 * Handles the conversion between the embedded value object used in entities and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring")
public interface SummaryMeasurementMapper {

    /**
     * Converts a SummaryMeasurement to a SummaryMeasurementDto.
     *
     * @param measurement the summary measurement value object
     * @return the summary measurement DTO
     */
    SummaryMeasurementDto toDto(SummaryMeasurement measurement);

    /**
     * Converts a SummaryMeasurementDto to a SummaryMeasurement.
     *
     * @param dto the summary measurement DTO
     * @return the summary measurement value object
     */
    SummaryMeasurement toEntity(SummaryMeasurementDto dto);
}
