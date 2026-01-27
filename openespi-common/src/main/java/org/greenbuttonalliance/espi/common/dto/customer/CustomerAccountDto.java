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
import org.greenbuttonalliance.espi.common.domain.customer.enums.NotificationMethodKind;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * CustomerAccount DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Assignment of a group of products and services purchased by the customer through a
 * customer agreement, used as a mechanism for customer billing and payment.
 * It contains common information from the various types of customer agreements to
 * create billings (invoices) for a customer and receive payment.
 *
 * Extends Document type. Field order matches customer.xsd CustomerAccount definition (lines 118-158)
 * and Document base type (lines 819-872).
 */
@XmlRootElement(name = "CustomerAccount", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomerAccount", namespace = "http://naesb.org/espi/customer", propOrder = {
    "type", "authorName", "createdDateTime", "lastModifiedDateTime", "revisionNumber",
    "electronicAddress", "subject", "title", "docStatus",
    "billingCycle", "budgetBill", "lastBillAmount", "notifications", "contactInfo", "accountId"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAccountDto {

    @XmlTransient
    private String uuid;

    // ========== Document fields (customer.xsd lines 819-872) ==========

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
    private CustomerDto.ElectronicAddressDto electronicAddress;

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

    // ========== CustomerAccount fields (customer.xsd lines 118-158) ==========

    /**
     * Cycle day on which the associated customer account will normally be billed,
     * used to determine when to produce the billing.
     */
    @XmlElement(name = "billingCycle", namespace = "http://naesb.org/espi/customer")
    private String billingCycle;

    /**
     * Budget bill code.
     */
    @XmlElement(name = "budgetBill", namespace = "http://naesb.org/espi/customer")
    private String budgetBill;

    /**
     * The last amount that will be billed to the customer prior to shut off of the account.
     */
    @XmlElement(name = "lastBillAmount", namespace = "http://naesb.org/espi/customer")
    private Long lastBillAmount;

    /**
     * Set of customer account notifications.
     */
    @XmlElement(name = "AccountNotification", namespace = "http://naesb.org/espi/customer")
    @XmlElementWrapper(name = "notifications", namespace = "http://naesb.org/espi/customer")
    private List<AccountNotificationDto> notifications;

    /**
     * [extension] Customer contact information used to identify individual
     * responsible for billing and payment of CustomerAccount.
     */
    @XmlElement(name = "contactInfo", namespace = "http://naesb.org/espi/customer")
    private CustomerDto.OrganisationDto contactInfo;

    /**
     * [extension] Customer account identifier.
     */
    @XmlElement(name = "accountId", namespace = "http://naesb.org/espi/customer")
    private String accountId;

    /**
     * Nested DTO for Status information.
     * Matches customer.xsd Status type (lines 1149-1173).
     * XML type name is "DocStatus" to avoid conflict with CustomerDto.StatusDto.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "DocStatus", namespace = "http://naesb.org/espi/customer", propOrder = {
        "value", "dateTime", "remark", "reason"
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDto {
        /**
         * Status value.
         */
        @XmlElement(name = "value", namespace = "http://naesb.org/espi/customer")
        private String value;

        /**
         * Date and time status was last changed.
         */
        @XmlElement(name = "dateTime", namespace = "http://naesb.org/espi/customer")
        private OffsetDateTime dateTime;

        /**
         * Remark.
         */
        @XmlElement(name = "remark", namespace = "http://naesb.org/espi/customer")
        private String remark;

        /**
         * Reason for status change.
         */
        @XmlElement(name = "reason", namespace = "http://naesb.org/espi/customer")
        private String reason;
    }

    /**
     * Nested DTO for AccountNotification information.
     * [extension] Customer action notification (e.g., delinquency, move in, move out).
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "AccountNotification", namespace = "http://naesb.org/espi/customer", propOrder = {
        "methodKind", "time", "note", "customerNotificationKind"
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountNotificationDto {
        /**
         * Method by which the customer was notified.
         */
        @XmlElement(name = "methodKind", namespace = "http://naesb.org/espi/customer")
        private NotificationMethodKind methodKind;

        /**
         * Time/date of notification.
         */
        @XmlElement(name = "time", namespace = "http://naesb.org/espi/customer")
        private OffsetDateTime time;

        /**
         * Annotation of the reason for the notification.
         */
        @XmlElement(name = "note", namespace = "http://naesb.org/espi/customer")
        private String note;

        /**
         * Type of customer notification (delinquency, move in, move out ...).
         */
        @XmlElement(name = "customerNotificationKind", namespace = "http://naesb.org/espi/customer")
        private String customerNotificationKind;
    }
}
