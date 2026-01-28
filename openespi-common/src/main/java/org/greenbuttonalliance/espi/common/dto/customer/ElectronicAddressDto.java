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

import java.io.Serializable;

/**
 * Shared DTO for ElectronicAddress information.
 * Per ESPI 4.0 customer.xsd lines 886-936.
 *
 * Electronic address information with 8 fields:
 * - lan: Local area network address
 * - mac: MAC address
 * - email1: Primary email address
 * - email2: Alternate email address
 * - web: Web address
 * - radio: Radio address
 * - userID: User ID
 * - password: Password
 *
 * Used by Customer (via Organisation) and EndDevice (Asset field).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ElectronicAddress", namespace = "http://naesb.org/espi/customer", propOrder = {
    "lan", "mac", "email1", "email2", "web", "radio", "userID", "password"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicAddressDto implements Serializable {

    @XmlElement(name = "lan", namespace = "http://naesb.org/espi/customer")
    private String lan;

    @XmlElement(name = "mac", namespace = "http://naesb.org/espi/customer")
    private String mac;

    @XmlElement(name = "email1", namespace = "http://naesb.org/espi/customer")
    private String email1;

    @XmlElement(name = "email2", namespace = "http://naesb.org/espi/customer")
    private String email2;

    @XmlElement(name = "web", namespace = "http://naesb.org/espi/customer")
    private String web;

    @XmlElement(name = "radio", namespace = "http://naesb.org/espi/customer")
    private String radio;

    @XmlElement(name = "userID", namespace = "http://naesb.org/espi/customer")
    private String userID;

    @XmlElement(name = "password", namespace = "http://naesb.org/espi/customer")
    private String password;
}
