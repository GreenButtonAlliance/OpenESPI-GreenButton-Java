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
 * Organisation DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Organisation that might have roles as utility, customer, supplier, manufacturer, etc.
 * Field order matches customer.xsd:1096-1125.
 */
@XmlRootElement(name = "Organisation", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Organisation", namespace = "http://naesb.org/espi/customer", propOrder = {
    "streetAddress", "postalAddress", "phone1", "phone2", "electronicAddress", "organisationName"
})
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Street address for this organisation.
     * Maps to customer.xsd streetAddress element (lines 1097-1105).
     */
    @XmlElement(name = "streetAddress", namespace = "http://naesb.org/espi/customer")
    private CustomerDto.StreetAddressDto streetAddress;

    /**
     * Postal address for this organisation.
     * Maps to customer.xsd postalAddress element (lines 1106-1114).
     */
    @XmlElement(name = "postalAddress", namespace = "http://naesb.org/espi/customer")
    private CustomerDto.StreetAddressDto postalAddress;

    /**
     * Primary phone number for this organisation.
     * Maps to customer.xsd phone1 element (lines 1115-1116).
     */
    @XmlElement(name = "phone1", namespace = "http://naesb.org/espi/customer")
    private TelephoneNumberDto phone1;

    /**
     * Secondary phone number for this organisation.
     * Maps to customer.xsd phone2 element (lines 1117-1118).
     */
    @XmlElement(name = "phone2", namespace = "http://naesb.org/espi/customer")
    private TelephoneNumberDto phone2;

    /**
     * Electronic address for this organisation.
     * Maps to customer.xsd electronicAddress element (lines 1119-1120).
     */
    @XmlElement(name = "electronicAddress", namespace = "http://naesb.org/espi/customer")
    private ElectronicAddressDto electronicAddress;

    /**
     * Organisation name.
     * Maps to customer.xsd organisationName element (lines 1121-1124).
     */
    @XmlElement(name = "organisationName", namespace = "http://naesb.org/espi/customer")
    private String organisationName;
}
