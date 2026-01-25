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

package org.greenbuttonalliance.espi.common.domain.customer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.hibernate.proxy.HibernateProxy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for CustomerAccount without JAXB concerns.
 * 
 * Assignment of a group of products and services purchased by the customer through a 
 * customer agreement, used as a mechanism for customer billing and payment. 
 * It contains common information from the various types of customer agreements to 
 * create billings (invoices) for a customer and receive payment.
 * 
 * This is an actual ESPI resource entity that extends IdentifiedObject directly.
 */
@Entity
@Table(name = "customer_accounts")
// Resolve any potential column conflicts by ensuring unique column names
@AttributeOverride(name = "upLink.rel", column = @Column(name = "customer_account_up_link_rel"))
@AttributeOverride(name = "upLink.href", column = @Column(name = "customer_account_up_link_href"))
@AttributeOverride(name = "upLink.type", column = @Column(name = "customer_account_up_link_type"))
@AttributeOverride(name = "selfLink.rel", column = @Column(name = "customer_account_self_link_rel"))
@AttributeOverride(name = "selfLink.href", column = @Column(name = "customer_account_self_link_href"))
@AttributeOverride(name = "selfLink.type", column = @Column(name = "customer_account_self_link_type"))
@Getter
@Setter
@NoArgsConstructor
public class CustomerAccountEntity extends IdentifiedObject {

    // Document fields (previously inherited from Document superclass)
    // Field order matches customer.xsd Document type definition (lines 819-872)

    /**
     * Type of this document.
     */
    @Column(name = "document_type", length = 256)
    private String type;

    /**
     * Name of the author of this document.
     */
    @Column(name = "author_name", length = 256)
    private String authorName;

    /**
     * Date and time that this document was created.
     */
    @Column(name = "created_date_time")
    private OffsetDateTime createdDateTime;

    /**
     * Date and time that this document was last modified.
     */
    @Column(name = "last_modified_date_time")
    private OffsetDateTime lastModifiedDateTime;

    /**
     * Revision number for this document.
     */
    @Column(name = "revision_number", length = 256)
    private String revisionNumber;

    /**
     * Electronic address for the document.
     */
    @Embedded
    private Organisation.ElectronicAddress electronicAddress;

    /**
     * Subject of this document, intended for this document to be found by a search engine.
     */
    @Column(name = "subject", length = 256)
    private String subject;

    /**
     * Title of this document.
     */
    @Column(name = "title", length = 256)
    private String title;

    /**
     * Status of this document.
     */
    @Embedded
    private Status docStatus;

    // CustomerAccount specific fields

    /**
     * Cycle day on which the associated customer account will normally be billed, 
     * used to determine when to produce the billing.
     */
    @Column(name = "billing_cycle", length = 256)
    private String billingCycle;

    /**
     * Budget bill code.
     */
    @Column(name = "budget_bill", length = 256)
    private String budgetBill;

    /**
     * The last amount that will be billed to the customer prior to shut off of the account.
     */
    @Column(name = "last_bill_amount")
    private Long lastBillAmount;

    /**
     * Set of customer account notifications.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "customer_account_notifications", joinColumns = @JoinColumn(name = "customer_account_id"))
    private List<AccountNotification> notifications;

    /**
     * [extension] Customer contact information used to identify individual
     * responsible for billing and payment of CustomerAccount.
     */
    @Embedded
    @AttributeOverride(name = "organisationName", column = @Column(name = "organisation_name"))
    @AttributeOverride(name = "streetAddress.streetDetail", column = @Column(name = "street_detail"))
    @AttributeOverride(name = "streetAddress.townDetail", column = @Column(name = "town_detail"))
    @AttributeOverride(name = "streetAddress.stateOrProvince", column = @Column(name = "state_or_province"))
    @AttributeOverride(name = "streetAddress.postalCode", column = @Column(name = "postal_code"))
    @AttributeOverride(name = "streetAddress.country", column = @Column(name = "country"))
    @AttributeOverride(name = "postalAddress.streetDetail", column = @Column(name = "postal_street_detail"))
    @AttributeOverride(name = "postalAddress.townDetail", column = @Column(name = "postal_town_detail"))
    @AttributeOverride(name = "postalAddress.stateOrProvince", column = @Column(name = "postal_state_or_province"))
    @AttributeOverride(name = "postalAddress.postalCode", column = @Column(name = "postal_postal_code"))
    @AttributeOverride(name = "postalAddress.country", column = @Column(name = "postal_country"))
    @AttributeOverride(name = "electronicAddress.email1", column = @Column(name = "contact_email1"))
    @AttributeOverride(name = "electronicAddress.email2", column = @Column(name = "contact_email2"))
    @AttributeOverride(name = "electronicAddress.web", column = @Column(name = "contact_web"))
    @AttributeOverride(name = "electronicAddress.radio", column = @Column(name = "contact_radio"))
    private Organisation contactInfo;

    /**
     * [extension] Customer account identifier
     */
    @Column(name = "account_id", length = 256)
    private String accountId;
    
    /**
     * [extension] Indicates whether this customer account is a prepaid account.
     * Prepaid accounts require payment before service is provided.
     */
    @Column(name = "is_pre_pay")
    private Boolean isPrePay;
    
    /**
     * Customer that owns this account.
     * Many customer accounts can belong to one customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        CustomerAccountEntity that = (CustomerAccountEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + getId() + ", " +
                "type = " + getType() + ", " +
                "authorName = " + getAuthorName() + ", " +
                "createdDateTime = " + getCreatedDateTime() + ", " +
                "lastModifiedDateTime = " + getLastModifiedDateTime() + ", " +
                "revisionNumber = " + getRevisionNumber() + ", " +
                "electronicAddress = " + getElectronicAddress() + ", " +
                "subject = " + getSubject() + ", " +
                "title = " + getTitle() + ", " +
                "docStatus = " + getDocStatus() + ", " +
                "billingCycle = " + getBillingCycle() + ", " +
                "budgetBill = " + getBudgetBill() + ", " +
                "lastBillAmount = " + getLastBillAmount() + ", " +
                "notifications = " + getNotifications() + ", " +
                "contactInfo = " + getContactInfo() + ", " +
                "accountId = " + getAccountId() + ", " +
                "isPrePay = " + getIsPrePay() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}