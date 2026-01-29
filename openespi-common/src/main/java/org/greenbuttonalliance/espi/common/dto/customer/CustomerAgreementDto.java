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
import org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * CustomerAgreement DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Agreement between the customer and the service supplier to pay for service at a specific service location.
 * It records certain billing information about the type of service provided at the service location and is
 * used during charge creation to determine the type of service.
 *
 * This DTO contains ONLY the XSD-defined fields per customer.xsd CustomerAgreement type (lines 159-209).
 * Atom protocol fields (id, title, published, updated, links) are handled by CustomerAtomEntryDto wrapper.
 *
 * Inheritance chain: CustomerAgreement → Agreement → Document → IdentifiedObject
 * Field order strictly follows customer.xsd inheritance hierarchy.
 */
@XmlRootElement(name = "CustomerAgreement", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomerAgreement", namespace = "http://naesb.org/espi/customer", propOrder = {
    // Document fields (11) - lines 819-885
    "type", "authorName", "createdDateTime", "lastModifiedDateTime", "revisionNumber",
    "electronicAddress", "subject", "title", "docStatus", "status", "comment",
    // Agreement fields (2) - lines 622-642
    "signDate", "validityInterval",
    // CustomerAgreement fields (6 scalar) - lines 159-209
    "loadMgmt", "isPrePay", "shutOffDateTime", "currency", "futureStatus", "agreementId"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAgreementDto {

    // ==================== Document fields (customer.xsd lines 819-885) ====================

    /**
     * Type of this document.
     */
    @XmlElement(name = "type", namespace = "http://naesb.org/espi/customer")
    private String type;

    /**
     * Name of the author of this document.
     */
    @XmlElement(name = "authorName", namespace = "http://naesb.org/espi/customer")
    private String authorName;

    /**
     * Date and time that this document was created.
     */
    @XmlElement(name = "createdDateTime", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime createdDateTime;

    /**
     * Date and time that this document was last modified.
     */
    @XmlElement(name = "lastModifiedDateTime", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime lastModifiedDateTime;

    /**
     * Revision number for this document.
     */
    @XmlElement(name = "revisionNumber", namespace = "http://naesb.org/espi/customer")
    private String revisionNumber;

    /**
     * Electronic address for the document.
     */
    @XmlElement(name = "electronicAddress", namespace = "http://naesb.org/espi/customer")
    private ElectronicAddressDto electronicAddress;

    /**
     * Subject of this document, intended for this document to be found by a search engine.
     */
    @XmlElement(name = "subject", namespace = "http://naesb.org/espi/customer")
    private String subject;

    /**
     * Title of this document.
     */
    @XmlElement(name = "title", namespace = "http://naesb.org/espi/customer")
    private String title;

    /**
     * Status of this document.
     */
    @XmlElement(name = "docStatus", namespace = "http://naesb.org/espi/customer")
    private StatusDto docStatus;

    /**
     * Status of subject matter (e.g., Agreement, Work) this document represents.
     */
    @XmlElement(name = "status", namespace = "http://naesb.org/espi/customer")
    private StatusDto status;

    /**
     * Free text comment.
     */
    @XmlElement(name = "comment", namespace = "http://naesb.org/espi/customer")
    private String comment;

    // ==================== Agreement fields (customer.xsd lines 622-642) ====================

    /**
     * Date this agreement was consummated among associated persons and/or organisations.
     */
    @XmlElement(name = "signDate", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime signDate;

    /**
     * Date and time interval this agreement is valid (from going into effect to termination).
     */
    @XmlElement(name = "validityInterval", namespace = "http://naesb.org/espi/customer")
    private DateTimeIntervalDto validityInterval;

    // ==================== CustomerAgreement fields (customer.xsd lines 159-209) ====================

    /**
     * Load management code.
     */
    @XmlElement(name = "loadMgmt", namespace = "http://naesb.org/espi/customer")
    private String loadMgmt;

    /**
     * If true, the customer is a pre-pay customer for the specified service.
     */
    @XmlElement(name = "isPrePay", namespace = "http://naesb.org/espi/customer")
    private Boolean isPrePay;

    /**
     * Final date and time the service will be billed to the previous customer.
     */
    @XmlElement(name = "shutOffDateTime", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime shutOffDateTime;

    /**
     * Currency for all monetary amounts for this agreement.
     */
    @XmlElement(name = "currency", namespace = "http://naesb.org/espi/customer")
    private String currency;

    /**
     * Known future changes to CustomerAgreement's Status of Service.
     */
    @XmlElement(name = "futureStatus", namespace = "http://naesb.org/espi/customer")
    private List<StatusDto> futureStatus;

    /**
     * Customer agreement identifier.
     */
    @XmlElement(name = "agreementId", namespace = "http://naesb.org/espi/customer")
    private String agreementId;

    // ==================== Nested DTOs ====================

    /**
     * Status DTO for document status and future status information.
     * Matches customer.xsd Status type (lines 1254-1284).
     * XML type name is "AgreementStatus" to avoid conflict with CustomerDto.StatusDto.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "AgreementStatus", namespace = "http://naesb.org/espi/customer", propOrder = {
        "value", "dateTime", "remark", "reason"
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDto implements Serializable {

        /**
         * Status value.
         */
        @XmlElement(name = "value", namespace = "http://naesb.org/espi/customer")
        private String value;

        /**
         * Date and time for which status value applies.
         */
        @XmlElement(name = "dateTime", namespace = "http://naesb.org/espi/customer")
        private OffsetDateTime dateTime;

        /**
         * Pertinent information regarding the current value, as free form text.
         */
        @XmlElement(name = "remark", namespace = "http://naesb.org/espi/customer")
        private String remark;

        /**
         * Reason code or explanation for why an object went to the current status value.
         */
        @XmlElement(name = "reason", namespace = "http://naesb.org/espi/customer")
        private String reason;
    }
}
