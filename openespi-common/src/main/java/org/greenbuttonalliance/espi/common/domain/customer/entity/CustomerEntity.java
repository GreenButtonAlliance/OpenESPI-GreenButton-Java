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
import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;
import org.hibernate.proxy.HibernateProxy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for Customer without JAXB concerns.
 * 
 * Organisation receiving services from service supplier.
 * Contains customer-specific information including kind, special needs,
 * VIP status, PUC number, status, priority, locale, and customer name.
 * 
 * This entity represents Personally Identifiable Information (PII) and
 * cannot be part of normal ESPI usage information. These resources are
 * available via the same messaging patterns as other ESPI data but are
 * defined in a separate namespace.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity extends IdentifiedObject {

    /**
     * Organisation having this role.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "organisationName", column = @Column(name = "customer_organisation_name")),
        @AttributeOverride(name = "streetAddress.streetDetail", column = @Column(name = "customer_street_detail")),
        @AttributeOverride(name = "streetAddress.townDetail", column = @Column(name = "customer_town_detail")),
        @AttributeOverride(name = "streetAddress.stateOrProvince", column = @Column(name = "customer_state_or_province")),
        @AttributeOverride(name = "streetAddress.postalCode", column = @Column(name = "customer_postal_code")),
        @AttributeOverride(name = "streetAddress.country", column = @Column(name = "customer_country")),
        @AttributeOverride(name = "postalAddress.streetDetail", column = @Column(name = "customer_postal_street_detail")),
        @AttributeOverride(name = "postalAddress.townDetail", column = @Column(name = "customer_postal_town_detail")),
        @AttributeOverride(name = "postalAddress.stateOrProvince", column = @Column(name = "customer_postal_state_or_province")),
        @AttributeOverride(name = "postalAddress.postalCode", column = @Column(name = "customer_postal_postal_code")),
        @AttributeOverride(name = "postalAddress.country", column = @Column(name = "customer_postal_country")),
        @AttributeOverride(name = "electronicAddress.email1", column = @Column(name = "customer_email1")),
        @AttributeOverride(name = "electronicAddress.email2", column = @Column(name = "customer_email2")),
        @AttributeOverride(name = "electronicAddress.web", column = @Column(name = "customer_web")),
        @AttributeOverride(name = "electronicAddress.radio", column = @Column(name = "customer_radio"))
    })
    private Organisation organisation;

    /**
     * Kind of customer (enum value).
     */
    @Column(name = "kind")
    @Enumerated(EnumType.STRING)
    private CustomerKind kind;

    /**
     * True if customer organisation has special service needs such as life support, hospitals, etc.
     */
    @Column(name = "special_need", length = 256)
    private String specialNeed;

    /**
     * (use 'priority' instead) True if this is an important customer. 
     * Importance is for matters different than those in 'specialNeed' attribute.
     */
    @Column(name = "vip")
    private Boolean vip;

    /**
     * (if applicable) Public utilities commission (PUC) identification number.
     */
    @Column(name = "puc_number", length = 256)
    private String pucNumber;

    /**
     * Status of this customer.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "status_value")),
        @AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time")),
        @AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
    })
    private Status status;

    /**
     * Priority of the customer.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "priority_value")),
        @AttributeOverride(name = "rank", column = @Column(name = "priority_rank")),
        @AttributeOverride(name = "type", column = @Column(name = "priority_type"))
    })
    private Priority priority;

    /**
     * Locale designating language to use in communications with this customer.
     */
    @Column(name = "locale", length = 256)
    private String locale;

    /**
     * [extension] Customer name
     */
    @Column(name = "customer_name", length = 256)
    private String customerName;
    
    /**
     * Customer accounts owned by this customer.
     * One customer can have multiple customer accounts.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomerAccountEntity> customerAccounts;
    
    /**
     * Time configuration for this customer.
     * Each customer has one time configuration.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_configuration_id")
    private TimeConfigurationEntity timeConfiguration;
    
    /**
     * Billing statements for this customer.
     * One customer can have multiple statements.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StatementEntity> statements;

    /**
     * Phone numbers for this customer's organisation.
     * Managed via separate PhoneNumberEntity to avoid column conflicts.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_uuid", referencedColumnName = "id")
    @Where(clause = "parent_entity_type = 'CustomerEntity'")
    private List<PhoneNumberEntity> phoneNumbers;

    /**
     * Embeddable class for Status
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class Status {
        @Column(name = "value", length = 256)
        private String value;
        
        @Column(name = "date_time")
        private OffsetDateTime dateTime;
        
        @Column(name = "reason", length = 256)
        private String reason;
    }

    /**
     * Embeddable class for Priority
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class Priority {
        @Column(name = "value")
        private Integer value;
        
        @Column(name = "rank")
        private Integer rank;
        
        @Column(name = "type", length = 256)
        private String type;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        CustomerEntity that = (CustomerEntity) o;
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
                "organisation = " + getOrganisation() + ", " +
                "kind = " + getKind() + ", " +
                "specialNeed = " + getSpecialNeed() + ", " +
                "vip = " + getVip() + ", " +
                "pucNumber = " + getPucNumber() + ", " +
                "status = " + getStatus() + ", " +
                "priority = " + getPriority() + ", " +
                "locale = " + getLocale() + ", " +
                "customerName = " + getCustomerName() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}