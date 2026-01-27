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

import lombok.*;
import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;

import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for CustomerAgreement without JAXB concerns.
 *
 * Agreement between the customer and the service supplier to pay for service at a specific service location.
 * It records certain billing information about the type of service provided at the service location and is
 * used during charge creation to determine the type of service.
 *
 * This is an actual ESPI resource entity that extends IdentifiedObject directly.
 */
@Entity
@Table(name = "customer_agreements")
@AttributeOverride(name = "upLink.rel", column = @Column(name = "customer_agreement_up_link_rel"))
@AttributeOverride(name = "upLink.href", column = @Column(name = "customer_agreement_up_link_href"))
@AttributeOverride(name = "upLink.type", column = @Column(name = "customer_agreement_up_link_type"))
@AttributeOverride(name = "selfLink.rel", column = @Column(name = "customer_agreement_self_link_rel"))
@AttributeOverride(name = "selfLink.href", column = @Column(name = "customer_agreement_self_link_href"))
@AttributeOverride(name = "selfLink.type", column = @Column(name = "customer_agreement_self_link_type"))
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"futureStatus"})
public class CustomerAgreementEntity extends IdentifiedObject {

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
    @AttributeOverride(name = "lan", column = @Column(name = "doc_lan"))
    @AttributeOverride(name = "mac", column = @Column(name = "doc_mac"))
    @AttributeOverride(name = "email1", column = @Column(name = "doc_email1"))
    @AttributeOverride(name = "email2", column = @Column(name = "doc_email2"))
    @AttributeOverride(name = "web", column = @Column(name = "doc_web"))
    @AttributeOverride(name = "radio", column = @Column(name = "doc_radio"))
    @AttributeOverride(name = "userID", column = @Column(name = "doc_user_id"))
    @AttributeOverride(name = "password", column = @Column(name = "doc_password"))
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
    @AttributeOverride(name = "value", column = @Column(name = "doc_status_value"))
    @AttributeOverride(name = "dateTime", column = @Column(name = "doc_status_date_time"))
    @AttributeOverride(name = "remark", column = @Column(name = "doc_status_remark"))
    @AttributeOverride(name = "reason", column = @Column(name = "doc_status_reason"))
    private Status docStatus;

    /**
     * Status of subject matter (e.g., Agreement, Work) this document represents.
     * For status of the document itself, use 'docStatus' attribute.
     */
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "status_value"))
    @AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time"))
    @AttributeOverride(name = "remark", column = @Column(name = "status_remark"))
    @AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
    private Status status;

    /**
     * Free text comment.
     */
    @Column(name = "comment", length = 256)
    private String comment;

    // Agreement fields (previously inherited from Agreement superclass)
    // Field order matches customer.xsd Agreement type definition (lines 622-660)

    /**
     * Date this agreement was consummated among associated persons and/or organisations.
     */
    @Column(name = "sign_date")
    private OffsetDateTime signDate;

    /**
     * Date and time interval this agreement is valid (from going into effect to termination).
     */
    @Embedded
    @AttributeOverride(name = "start", column = @Column(name = "validity_start"))
    @AttributeOverride(name = "duration", column = @Column(name = "validity_duration"))
    private DateTimeInterval validityInterval;

    // CustomerAgreement specific fields
    // Field order matches customer.xsd CustomerAgreement type definition (lines 159-260)

    /**
     * Load management code.
     */
    @Column(name = "load_mgmt", length = 256)
    private String loadMgmt;

    /**
     * If true, the customer is a pre-pay customer for the specified service.
     */
    @Column(name = "is_pre_pay")
    private Boolean isPrePay;

    /**
     * Final date and time the service will be billed to the previous customer.
     */
    @Column(name = "shut_off_date_time")
    private OffsetDateTime shutOffDateTime;

    /**
     * Demand Response program characteristics covered by Customer Agreement
     * TODO: Create DemandResponseProgramEntity and enable this relationship
     */
    // @OneToMany(mappedBy = "customerAgreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<DemandResponseProgramEntity> demandResponsePrograms;

    /**
     * All pricing structures applicable to this customer agreement.
     * TODO: Create PricingStructureEntity and enable this relationship
     */
    // @OneToMany(mappedBy = "customerAgreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<PricingStructureEntity> pricingStructures;

    /**
     * Currency for all monetary amounts for this agreement.
     */
    @Column(name = "currency", length = 3)
    private String currency; // ISO 4217 currency code

    /**
     * [extension] Known future changes to CustomerAgreement's Status of Service.
     */
    @ElementCollection
    @CollectionTable(name = "customer_agreement_future_status", joinColumns = @JoinColumn(name = "customer_agreement_id"))
    @AttributeOverride(name = "value", column = @Column(name = "status_value"))
    @AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time"))
    @AttributeOverride(name = "remark", column = @Column(name = "status_remark"))
    @AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
    private List<Status> futureStatus;

    /**
     * [extension] Customer agreement identifier
     */
    @Column(name = "agreement_id", length = 256)
    private String agreementId;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        CustomerAgreementEntity that = (CustomerAgreementEntity) o;
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
                "status = " + getStatus() + ", " +
                "comment = " + getComment() + ", " +
                "signDate = " + getSignDate() + ", " +
                "validityInterval = " + getValidityInterval() + ", " +
                "loadMgmt = " + getLoadMgmt() + ", " +
                "isPrePay = " + getIsPrePay() + ", " +
                "shutOffDateTime = " + getShutOffDateTime() + ", " +
                "currency = " + getCurrency() + ", " +
                "futureStatus = " + getFutureStatus() + ", " +
                "agreementId = " + getAgreementId() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}
