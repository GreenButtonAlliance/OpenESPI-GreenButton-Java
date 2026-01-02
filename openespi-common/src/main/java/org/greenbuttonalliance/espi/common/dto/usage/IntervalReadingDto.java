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

import jakarta.xml.bind.annotation.*;

import java.util.List;

/**
 * IntervalReading DTO record for JAXB XML marshalling/unmarshalling.
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
    "cost", "currency", "value", "timePeriod", "readingQualities",
    "consumptionTier", "tou", "cpp"
})
public record IntervalReadingDto(

    @XmlElement(name = "cost")
    Long cost,

    @XmlElement(name = "currency")
    Integer currency,

    @XmlElement(name = "value")
    Long value,

    @XmlElement(name = "timePeriod")
    DateTimeIntervalDto timePeriod,

    @XmlElement(name = "ReadingQuality")
    //@XmlElementWrapper(name = "ReadingQualities")
    List<ReadingQualityDto> readingQualities,

    @XmlElement(name = "consumptionTier")
    Integer consumptionTier,

    @XmlElement(name = "tou")
    Integer tou,

    @XmlElement(name = "cpp")
    Integer cpp
) {

    /**
     * Default constructor for JAXB.
     */
    public IntervalReadingDto() {
        this(null, null, null, null, null, null, null, null);
    }

    /**
     * Minimal constructor for basic interval reading data.
     */
    public IntervalReadingDto(Long value, DateTimeIntervalDto timePeriod) {
        this(null, null, value, timePeriod, null, null, null, null);
    }

    /**
     * Constructor for interval reading with cost information.
     */
    public IntervalReadingDto(Long value, Long cost, Integer currency, DateTimeIntervalDto timePeriod) {
        this(cost, currency, value, timePeriod, null, null, null, null);
    }
}