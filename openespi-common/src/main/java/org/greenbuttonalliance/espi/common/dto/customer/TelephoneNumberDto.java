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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * TelephoneNumber DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Telephone number.
 * Field order matches customer.xsd:1428-1478.
 */
@XmlRootElement(name = "TelephoneNumber", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TelephoneNumber", namespace = "http://naesb.org/espi/customer", propOrder = {
    "countryCode", "areaCode", "cityCode", "localNumber", "ext", "dialOut", "internationalPrefix", "ituPhone"
})
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TelephoneNumberDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Country code.
     * Maps to customer.xsd countryCode element (lines 1431-1436).
     */
    @XmlElement(name = "countryCode", namespace = "http://naesb.org/espi/customer")
    private String countryCode;

    /**
     * Area or city code.
     * Maps to customer.xsd areaCode element (lines 1437-1442).
     */
    @XmlElement(name = "areaCode", namespace = "http://naesb.org/espi/customer")
    private String areaCode;

    /**
     * City code.
     * Maps to customer.xsd cityCode element (lines 1443-1448).
     */
    @XmlElement(name = "cityCode", namespace = "http://naesb.org/espi/customer")
    private String cityCode;

    /**
     * Local number.
     * Maps to customer.xsd localNumber element (lines 1449-1454).
     */
    @XmlElement(name = "localNumber", namespace = "http://naesb.org/espi/customer")
    private String localNumber;

    /**
     * Extension.
     * Maps to customer.xsd ext element (lines 1455-1460).
     */
    @XmlElement(name = "ext", namespace = "http://naesb.org/espi/customer")
    private String ext;

    /**
     * Dial out code, e.g., '0'.
     * Maps to customer.xsd dialOut element (lines 1461-1466).
     */
    @XmlElement(name = "dialOut", namespace = "http://naesb.org/espi/customer")
    private String dialOut;

    /**
     * International prefix, e.g., '00', '+'.
     * Maps to customer.xsd internationalPrefix element (lines 1467-1472).
     */
    @XmlElement(name = "internationalPrefix", namespace = "http://naesb.org/espi/customer")
    private String internationalPrefix;

    /**
     * ITU-T phone number per E.164.
     * Maps to customer.xsd ituPhone element (lines 1473-1477).
     */
    @XmlElement(name = "ituPhone", namespace = "http://naesb.org/espi/customer")
    private String ituPhone;
}