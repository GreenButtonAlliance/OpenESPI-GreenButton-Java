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

/**
 * MeterReading DTO record for JAXB XML marshalling/unmarshalling.
 *
 * Represents a meter reading - a set of values obtained from the meter.
 * Per ESPI 4.0 specification, MeterReading extends IdentifiedObject but
 * contains NO child elements. Relationships to ReadingType and IntervalBlock
 * are expressed via Atom links, not embedded XML elements.
 *
 * Complies with espi.xsd MeterReading complexType definition.
 *
 * @see <a href="https://www.naesb.org/ESPI_Standards.asp">NAESB ESPI 4.0</a>
 */
@XmlRootElement(name = "MeterReading", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MeterReading", namespace = "http://naesb.org/espi")
public record MeterReadingDto(
    
    @XmlTransient
    Long id,
    
    @XmlTransient
    String uuid
) {
    
    /**
     * Default constructor for JAXB.
     */
    public MeterReadingDto() {
        this(null, null);
    }
    
    /**
     * Minimal constructor for basic meter reading data.
     */
    public MeterReadingDto(String uuid) {
        this(null, uuid);
    }
}