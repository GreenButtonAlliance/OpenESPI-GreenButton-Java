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
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.customer.enums.SupplierKind;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;
import org.hibernate.proxy.HibernateProxy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for ServiceSupplier without JAXB concerns.
 * 
 * Organisation that provides services to customers.
 */
@Entity
@Table(name = "service_suppliers")
@Getter
@Setter
@NoArgsConstructor
public class ServiceSupplierEntity extends IdentifiedObject {

    /**
     * Organisation having this role.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "organisationName", column = @Column(name = "supplier_organisation_name")),
        @AttributeOverride(name = "streetAddress.streetDetail", column = @Column(name = "supplier_street_detail")),
        @AttributeOverride(name = "streetAddress.townDetail", column = @Column(name = "supplier_town_detail")),
        @AttributeOverride(name = "streetAddress.stateOrProvince", column = @Column(name = "supplier_state_or_province")),
        @AttributeOverride(name = "streetAddress.postalCode", column = @Column(name = "supplier_postal_code")),
        @AttributeOverride(name = "streetAddress.country", column = @Column(name = "supplier_country")),
        @AttributeOverride(name = "postalAddress.streetDetail", column = @Column(name = "supplier_postal_street_detail")),
        @AttributeOverride(name = "postalAddress.townDetail", column = @Column(name = "supplier_postal_town_detail")),
        @AttributeOverride(name = "postalAddress.stateOrProvince", column = @Column(name = "supplier_postal_state_or_province")),
        @AttributeOverride(name = "postalAddress.postalCode", column = @Column(name = "supplier_postal_postal_code")),
        @AttributeOverride(name = "postalAddress.country", column = @Column(name = "supplier_postal_country")),
        @AttributeOverride(name = "electronicAddress.email1", column = @Column(name = "supplier_email1")),
        @AttributeOverride(name = "electronicAddress.email2", column = @Column(name = "supplier_email2")),
        @AttributeOverride(name = "electronicAddress.web", column = @Column(name = "supplier_web")),
        @AttributeOverride(name = "electronicAddress.radio", column = @Column(name = "supplier_radio"))
    })
    private Organisation organisation;

    /**
     * Kind of supplier.
     */
    @Column(name = "kind")
    @Enumerated(EnumType.STRING)
    private SupplierKind kind;

    /**
     * Unique transaction reference prefix number issued to an entity by the International Organization for 
     * Standardization for the purpose of tagging onto electronic financial transactions, as defined in 
     * ISO/IEC 7812-1 and ISO/IEC 7812-2.
     */
    @Column(name = "issuer_identification_number", length = 256)
    private String issuerIdentificationNumber;

    /**
     * [extension] Effective Date of Service Activation
     */
    @Column(name = "effective_date")
    private OffsetDateTime effectiveDate;

    /**
     * Phone numbers for this service supplier's organisation.
     * Managed via separate PhoneNumberEntity to avoid column conflicts.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_uuid", referencedColumnName = "id")
    @Where(clause = "parent_entity_type = 'ServiceSupplierEntity'")
    private List<PhoneNumberEntity> phoneNumbers;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ServiceSupplierEntity that = (ServiceSupplierEntity) o;
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
                "issuerIdentificationNumber = " + getIssuerIdentificationNumber() + ", " +
                "effectiveDate = " + getEffectiveDate() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}