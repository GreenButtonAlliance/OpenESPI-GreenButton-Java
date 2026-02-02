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

package org.greenbuttonalliance.espi.common.dto.customer;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * ProgramDateIdMappings DTO for ESPI 4.0 customer.xsd schema compliance.
 * Per customer.xsd lines 269-283.
 *
 * ProgramDateIdMappings is an ESPI Resource that inherits from IdentifiedObject.
 *
 * [extension] Collection of all customer Energy Efficiency programs.
 * Contains a single embedded ProgramDateIdMapping object with customer energy efficiency
 * program date mapping information.
 *
 * This DTO contains ONLY the 1 XSD element from customer.xsd:
 * - programDateIdMapping: Single customer energy efficiency program date mapping
 *
 * Atom protocol fields (id, published, updated, links) are handled by CustomerAtomEntryDto wrapper.
 */
@XmlRootElement(name = "ProgramDateIdMappings", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProgramDateIdMappings", namespace = "http://naesb.org/espi/customer", propOrder = {
    "programDateIdMapping"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDateIdMappingsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Single customer energy efficiency program date mapping.
     * Maps to customer.xsd ProgramDateIdMapping element (lines 270-282).
     */
    @XmlElement(name = "ProgramDateIdMapping", namespace = "http://naesb.org/espi/customer")
    private ProgramDateIdMappingDto programDateIdMapping;
}