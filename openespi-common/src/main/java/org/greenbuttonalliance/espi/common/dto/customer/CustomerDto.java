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

import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Customer DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents a customer entity containing Personal Identifiable Information (PII)
 * separate from usage data. Supports Atom protocol XML wrapping.
 */
@XmlRootElement(name = "Customer", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Customer", namespace = "http://naesb.org/espi/customer", propOrder = {
    "organisation", "kind", "specialNeed", "vip", "pucNumber", "status", "priority", "locale", "customerName"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    @XmlElement(name = "Organisation", namespace = "http://naesb.org/espi/customer")
    private OrganisationDto organisation;

    @XmlElement(name = "kind", namespace = "http://naesb.org/espi/customer")
    private CustomerKind kind;

    @XmlElement(name = "specialNeed", namespace = "http://naesb.org/espi/customer")
    private String specialNeed;

    @XmlElement(name = "vip", namespace = "http://naesb.org/espi/customer")
    private Boolean vip;

    @XmlElement(name = "pucNumber", namespace = "http://naesb.org/espi/customer")
    private String pucNumber;

    @XmlElement(name = "status", namespace = "http://naesb.org/espi/customer")
    private StatusDto status;

    @XmlElement(name = "priority", namespace = "http://naesb.org/espi/customer")
    private PriorityDto priority;

    @XmlElement(name = "locale", namespace = "http://naesb.org/espi/customer")
    private String locale;

    @XmlElement(name = "customerName", namespace = "http://naesb.org/espi/customer")
    private String customerName;

    /**
     * Embeddable DTO for Status.
     */
    /**
     * Embeddable DTO for Priority.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "Priority", namespace = "http://naesb.org/espi/customer")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityDto {
        @XmlElement(name = "value", namespace = "http://naesb.org/espi/customer")
        private Integer value;

        @XmlElement(name = "rank", namespace = "http://naesb.org/espi/customer")
        private Integer rank;

        @XmlElement(name = "type", namespace = "http://naesb.org/espi/customer")
        private String type;
    }

    /**
     * Embeddable DTO for StreetAddress.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "StreetAddress", namespace = "http://naesb.org/espi/customer")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreetAddressDto {
        @XmlElement(name = "streetDetail", namespace = "http://naesb.org/espi/customer")
        private String streetDetail;

        @XmlElement(name = "townDetail", namespace = "http://naesb.org/espi/customer")
        private String townDetail;

        @XmlElement(name = "stateOrProvince", namespace = "http://naesb.org/espi/customer")
        private String stateOrProvince;

        @XmlElement(name = "postalCode", namespace = "http://naesb.org/espi/customer")
        private String postalCode;

        @XmlElement(name = "country", namespace = "http://naesb.org/espi/customer")
        private String country;
    }

}