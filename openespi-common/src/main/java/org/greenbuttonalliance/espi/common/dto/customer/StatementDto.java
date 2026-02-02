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

import java.util.List;

/**
 * Statement DTO for JAXB XML marshalling/unmarshalling.
 *
 * [extension] Billing statement for provided services.
 *
 * Corresponds to customer.xsd Statement definition (lines 373-393).
 * Statement extends IdentifiedObject, but DTO excludes IdentifiedObject fields
 * (id, description, published, updated, links) as they're handled by Atom Entry wrapper.
 *
 * ESPI 4.0 XSD Sequence (customer.xsd lines 379-392):
 * 1. issueDateTime (TimeType) - optional
 * 2. statementRef (StatementRef collection) - optional, unbounded
 */
@XmlRootElement(name = "Statement", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Statement", namespace = "http://naesb.org/espi/customer", propOrder = {
    "issueDateTime",
    "statementRef"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatementDto {

    /**
     * [extension] Date and time at which a billing statement was issued.
     * Stored as Unix epoch timestamp (seconds since 1970-01-01T00:00:00Z).
     */
    @XmlElement(name = "issueDateTime", namespace = "http://naesb.org/espi/customer")
    private Long issueDateTime;

    /**
     * [extension] Contains document reference metadata needed to access a document
     * representation of a billing statement.
     */
    @XmlElement(name = "statementRef", namespace = "http://naesb.org/espi/customer")
    private List<StatementRefDto> statementRef;
}
