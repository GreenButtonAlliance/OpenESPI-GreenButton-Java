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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * IntervalBlock DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents a time sequence of readings of the same ReadingType.
 * Contains a date/time interval and a collection of interval readings.
 * Supports Atom protocol XML wrapping.
 *
 * Field order strictly matches espi.xsd IntervalBlock element sequence.
 *
 * @see <a href="https://www.naesb.org/ESPI_Standards.asp">NAESB ESPI 4.0</a>
 */
@XmlRootElement(name = "IntervalBlock", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IntervalBlock", namespace = "http://naesb.org/espi", propOrder = {
    "interval", "intervalReadings"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntervalBlockDto {

    @XmlTransient
    private Long id;

    @XmlTransient
    private String uuid;

    @XmlElement(name = "interval", namespace = "http://naesb.org/espi")
    private DateTimeIntervalDto interval;

    @XmlElement(name = "IntervalReading", namespace = "http://naesb.org/espi")
    private List<IntervalReadingDto> intervalReadings;

    /**
     * Convenience constructor for creating interval block with uuid, interval, and readings.
     *
     * @param uuid the resource identifier
     * @param interval the time interval
     * @param intervalReadings the list of readings
     */
    public IntervalBlockDto(String uuid, DateTimeIntervalDto interval, List<IntervalReadingDto> intervalReadings) {
        this(null, uuid, interval, intervalReadings);
    }

    /**
     * Generates the default self href for an interval block.
     *
     * @return default self href
     */
    public String generateSelfHref() {
        return uuid != null ? "/espi/1_1/resource/IntervalBlock/" + uuid : null;
    }

    /**
     * Generates the default up href for an interval block.
     *
     * @return default up href
     */
    public String generateUpHref() {
        return "/espi/1_1/resource/IntervalBlock";
    }

    /**
     * Gets the total number of interval readings.
     *
     * @return interval reading count
     */
    public int getIntervalReadingCount() {
        return intervalReadings != null ? intervalReadings.size() : 0;
    }
}