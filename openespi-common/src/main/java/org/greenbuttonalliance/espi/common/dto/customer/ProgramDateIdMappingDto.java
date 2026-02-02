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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.customer.enums.ProgramDateKind;

import java.io.Serializable;

/**
 * ProgramDateIdMapping DTO for embedded complex type.
 * Per ESPI 4.0 customer.xsd lines 1223-1251.
 *
 * Single customer energy efficiency program date mapping with 4 fields:
 * - programDateType: Type of customer energy efficiency program date
 * - code: Code value (may be alphanumeric)
 * - name: Name associated with code
 * - note: Optional description of code
 *
 * This is an embedded component (extends Object in XSD, not IdentifiedObject).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProgramDateIdMapping", namespace = "http://naesb.org/espi/customer", propOrder = {
    "programDateType", "code", "name", "note"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDateIdMappingDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Type of customer energy efficiency program date.
     * Maps to customer.xsd programDateType element (lines 1224-1238).
     */
    @XmlElement(name = "programDateType", namespace = "http://naesb.org/espi/customer")
    private ProgramDateKind programDateType;

    /**
     * Code value (may be alphanumeric).
     * Maps to customer.xsd code element (lines 1239-1242).
     */
    @XmlElement(name = "code", namespace = "http://naesb.org/espi/customer")
    private String code;

    /**
     * Name associated with code.
     * Maps to customer.xsd name element (lines 1243-1246).
     */
    @XmlElement(name = "name", namespace = "http://naesb.org/espi/customer")
    private String name;

    /**
     * Optional description of code.
     * Maps to customer.xsd note element (lines 1247-1250).
     */
    @XmlElement(name = "note", namespace = "http://naesb.org/espi/customer")
    private String note;
}
