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

/**
 * ReadingQuality DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents quality indicators for readings, providing information about
 * the accuracy, validation status, and reliability of meter readings.
 *
 * Note: ReadingQuality does NOT extend IdentifiedObject per ESPI 4.0 specification.
 * It is not a top-level resource with selfLink/upLink/relatedLinks.
 */
@XmlRootElement(name = "ReadingQuality", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReadingQuality", namespace = "http://naesb.org/espi", propOrder = {
    "quality"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingQualityDto {

    @XmlElement(name = "quality", namespace = "http://naesb.org/espi")
    private String quality;
}