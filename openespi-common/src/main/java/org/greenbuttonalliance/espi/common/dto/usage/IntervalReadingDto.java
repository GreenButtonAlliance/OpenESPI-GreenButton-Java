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

package org.greenbuttonalliance.espi.common.dto.usage;

import org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * IntervalReading DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents specific readings of a measurement within an interval block.
 * Contains the actual energy values, costs, and reading quality information.
 *
 * Note: IntervalReading does NOT extend IdentifiedObject per ESPI 4.0 specification.
 * It is not a top-level resource with selfLink/upLink/relatedLinks.
 */
@XmlRootElement(name = "IntervalReading", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IntervalReading", namespace = "http://naesb.org/espi", propOrder = {
    "cost", "readingQualities", "timePeriod", "value", "consumptionTier", "tou", "cpp"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntervalReadingDto {

    @XmlElement(name = "cost", namespace = "http://naesb.org/espi")
    private Long cost;

    @XmlElement(name = "ReadingQuality", namespace = "http://naesb.org/espi")
    private List<ReadingQualityDto> readingQualities;

    @XmlElement(name = "timePeriod", namespace = "http://naesb.org/espi")
    private DateTimeIntervalDto timePeriod;

    @XmlElement(name = "value", namespace = "http://naesb.org/espi")
    private Long value;

    @XmlElement(name = "consumptionTier", namespace = "http://naesb.org/espi")
    private Integer consumptionTier;

    @XmlElement(name = "tou", namespace = "http://naesb.org/espi")
    private Integer tou;

    @XmlElement(name = "cpp", namespace = "http://naesb.org/espi")
    private Integer cpp;

    /**
     * Convenience constructor for basic reading with value, cost, and time period.
     *
     * @param value the reading value
     * @param cost the cost
     * @param timePeriod the time period
     */
    public IntervalReadingDto(Long value, Long cost, DateTimeIntervalDto timePeriod) {
        this(cost, null, timePeriod, value, null, null, null);
    }
}