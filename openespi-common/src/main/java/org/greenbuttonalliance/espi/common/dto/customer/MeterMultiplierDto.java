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
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.customer.enums.MeterMultiplierKind;

/**
 * MeterMultiplier DTO for ESPI 4.0 XSD compliance.
 * <p>
 * Represents a multiplier applied at the meter level.
 * <p>
 * XSD Definition: customer.xsd lines 1056-1076
 * Extends: Object (NOT IdentifiedObject - no mRID or description)
 * <p>
 * Field sequence MUST match customer.xsd definition exactly.
 */
@XmlRootElement(name = "MeterMultiplier", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MeterMultiplier", namespace = "http://naesb.org/espi/customer", propOrder = {
    "kind", "value"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeterMultiplierDto {

    /**
     * Kind of multiplier (e.g., kH, transformerRatio, ctRatio, ptRatio).
     * XSD: MeterMultiplierKind, minOccurs="0"
     */
    @XmlElement(name = "kind", namespace = "http://naesb.org/espi/customer")
    private MeterMultiplierKind kind;

    /**
     * Numeric value of the multiplier.
     * XSD: xs:float, minOccurs="0"
     */
    @XmlElement(name = "value", namespace = "http://naesb.org/espi/customer")
    private Float value;
}
