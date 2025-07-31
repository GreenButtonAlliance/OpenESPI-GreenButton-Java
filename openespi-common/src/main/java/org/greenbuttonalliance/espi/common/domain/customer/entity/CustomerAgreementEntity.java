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
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"futureStatus"})
public class CustomerAgreementEntity extends IdentifiedObject {

    // Document fields (previously inherited from Document superclass)
    
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
     * Type of this document.
     */
    @Column(name = "type", length = 256)
    private String type;

    // Agreement fields (previously inherited from Agreement superclass)
    
    /**
     * Date this agreement was consummated among associated persons and/or organisations.
     */
    @Column(name = "sign_date")
    private OffsetDateTime signDate;

    /**
     * Date and time interval this agreement is valid (from going into effect to termination).
     */
    @Embedded
    private DateTimeInterval validityInterval;

    // CustomerAgreement specific fields

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
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "status_value")),
        @AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time")),
        @AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
    })
    private List<CustomerEntity.Status> futureStatus;

    /**
     * [extension] Customer agreement identifier
     */
    @Column(name = "agreement_id", length = 256)
    private String agreementId;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        CustomerAgreementEntity that = (CustomerAgreementEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + getId() + ", " +
                "createdDateTime = " + getCreatedDateTime() + ", " +
                "lastModifiedDateTime = " + getLastModifiedDateTime() + ", " +
                "revisionNumber = " + getRevisionNumber() + ", " +
                "subject = " + getSubject() + ", " +
                "title = " + getTitle() + ", " +
                "type = " + getType() + ", " +
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