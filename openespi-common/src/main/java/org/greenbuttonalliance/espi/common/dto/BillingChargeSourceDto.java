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

package org.greenbuttonalliance.espi.common.dto;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BillingChargeSource DTO class for JAXB XML marshalling/unmarshalling.
 * <p>
 * Information about the source of billing charge.
 * Per ESPI 4.0 XSD (espi.xsd:1628-1643), BillingChargeSource extends Object
 * and contains a single agencyName field.
 * <p>
 * Embedded within UsageSummary DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BillingChargeSource", namespace = "http://naesb.org/espi", propOrder = {
    "agencyName"
})
public class BillingChargeSourceDto {
    /**
     * Name of the billing source agency.
     * Maximum length 256 characters per String256 type.
     */
    @XmlElement(name = "agencyName")
    private String agencyName;

    /**
     * Checks if this billing charge source has a value.
     *
     * @return true if agency name is present
     */
    public boolean hasValue() {
        return agencyName != null && !agencyName.trim().isEmpty();
    }
}
