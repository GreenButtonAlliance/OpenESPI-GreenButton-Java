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

/**
 * ESPI Customer DTOs for Green Button customer information exchange.
 *
 * This package contains Data Transfer Objects (DTOs) for NAESB ESPI customer-related resources
 * including Customer, CustomerAccount, ServiceLocation, and related PII data.
 * These DTOs are used for JAXB XML marshalling/unmarshalling in Green Button implementations.
 *
 * Customer data contains Personally Identifiable Information (PII) and is defined in a separate
 * namespace (http://naesb.org/espi/customer) from usage data per NAESB ESPI 4.0 specification.
 */
@jakarta.xml.bind.annotation.XmlSchema(
    namespace = "http://naesb.org/espi/customer",
    elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED,
    xmlns = {
            // Declare Atom with explicit atom prefix for CustomerAtomEntryDto
            @jakarta.xml.bind.annotation.XmlNs(prefix = "atom", namespaceURI = "http://www.w3.org/2005/Atom"),
            // Customer namespace uses cust prefix
            @jakarta.xml.bind.annotation.XmlNs(prefix = "cust", namespaceURI = "http://naesb.org/espi/customer")
    }
)
package org.greenbuttonalliance.espi.common.dto.customer;

import jakarta.xml.bind.annotation.XmlNs;
