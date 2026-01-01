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

import java.time.OffsetDateTime;

/**
 * StatementRef DTO record for JAXB XML marshalling/unmarshalling.
 *
 * Represents a reference to a statement document.
 *
 * Note: StatementRef does NOT extend IdentifiedObject per ESPI 4.0 specification.
 * It is not a top-level resource with selfLink/upLink/relatedLinks.
 *
 * WARNING: DTO fields do not currently match entity fields.
 * Entity has: fileName, mediaType, statementURL
 * DTO has: referenceId, referenceType, referenceDate, referenceUrl
 * This mismatch needs to be resolved.
 */
@XmlRootElement(name = "StatementRef", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatementRef", namespace = "http://naesb.org/espi/customer", propOrder = {
    "referenceId", "referenceType", "referenceDate", "referenceUrl", "statement"
})
public record StatementRefDto(

    @XmlElement(name = "referenceId")
    String referenceId,

    @XmlElement(name = "referenceType")
    String referenceType,

    @XmlElement(name = "referenceDate")
    OffsetDateTime referenceDate,

    @XmlElement(name = "referenceUrl")
    String referenceUrl,

    @XmlElement(name = "Statement")
    StatementDto statement
) {

    /**
     * Default constructor for JAXB.
     */
    public StatementRefDto() {
        this(null, null, null, null, null);
    }

    /**
     * Minimal constructor for basic reference data.
     */
    public StatementRefDto(String referenceId, String referenceUrl) {
        this(referenceId, null, null, referenceUrl, null);
    }
}