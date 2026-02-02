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

/**
 * StatementRef DTO for JAXB XML marshalling/unmarshalling.
 *
 * [extension] A sequence of references to a document associated with a Statement.
 *
 * Corresponds to customer.xsd StatementRef definition (lines 285-307).
 * StatementRef extends Object (not IdentifiedObject), so it has no id/links/metadata.
 *
 * ESPI 4.0 XSD Sequence (customer.xsd lines 291-306):
 * 1. fileName (String256) - optional
 * 2. mediaType (String256) - optional
 * 3. statementURL (String2048) - optional
 */
@XmlRootElement(name = "StatementRef", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatementRef", namespace = "http://naesb.org/espi/customer", propOrder = {
    "fileName",
    "mediaType",
    "statementURL"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatementRefDto {

    /**
     * [extension] Name of document or file including filename extension if present.
     */
    @XmlElement(name = "fileName", namespace = "http://naesb.org/espi/customer")
    private String fileName;

    /**
     * [extension] Document media type as published by IANA.
     * See https://www.iana.org/assignments/media-types for more information.
     */
    @XmlElement(name = "mediaType", namespace = "http://naesb.org/espi/customer")
    private String mediaType;

    /**
     * [extension] URL used to access a representation of a statement, for example a bill image.
     * Use CDATA or URL encoding to escape characters not allowed in XML.
     */
    @XmlElement(name = "statementURL", namespace = "http://naesb.org/espi/customer")
    private String statementURL;
}
