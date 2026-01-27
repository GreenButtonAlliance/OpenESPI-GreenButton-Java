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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for PhoneNumber to resolve embedded mapping conflicts.
 *
 * Separate entity table for phone numbers to eliminate column duplication
 * issues when multiple entities embed Organisation with PhoneNumber fields.
 *
 * Note: PhoneNumber does NOT extend IdentifiedObject per ESPI 4.0 specification.
 * It is not a top-level resource with selfLink/upLink/relatedLinks.
 */
@Entity
@Table(name = "phone_numbers")
@Getter
@Setter
@NoArgsConstructor
public class PhoneNumberEntity {

    /**
     * Primary key identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "char(36)", updatable = false, nullable = false)
    private UUID id;

    /**
     * Country code (per customer.xsd TelephoneNumber).
     */
    @Column(name = "country_code", length = 256)
    private String countryCode;

    /**
     * Area or region code (per customer.xsd TelephoneNumber).
     */
    @Column(name = "area_code", length = 256)
    private String areaCode;

    /**
     * City code (per customer.xsd TelephoneNumber).
     */
    @Column(name = "city_code", length = 256)
    private String cityCode;

    /**
     * Main (local) part of this telephone number (per customer.xsd TelephoneNumber).
     */
    @Column(name = "local_number", length = 256)
    private String localNumber;

    /**
     * Extension for this telephone number (per customer.xsd TelephoneNumber "ext" element).
     */
    @Column(name = "extension", length = 256)
    private String extension;

    /**
     * Dial out code, for instance to call outside an enterprise (per customer.xsd TelephoneNumber).
     */
    @Column(name = "dial_out", length = 256)
    private String dialOut;

    /**
     * Prefix used when calling an international number (per customer.xsd TelephoneNumber).
     */
    @Column(name = "international_prefix", length = 256)
    private String internationalPrefix;

    /**
     * Phone number according to ITU E.164 (per customer.xsd TelephoneNumber).
     */
    @Column(name = "itu_phone", length = 256)
    private String ituPhone;

    /**
     * Type of phone number (PRIMARY, SECONDARY, etc.).
     */
    @Column(name = "phone_type", length = 20)
    @Enumerated(EnumType.STRING)
    private PhoneType phoneType;

    /**
     * Reference to the parent entity UUID that owns this phone number.
     * This is a generic reference that can point to any entity type.
     */
    @Column(name = "parent_entity_uuid", length = 36)
    private String parentEntityUuid;

    /**
     * Type of the parent entity (CustomerEntity, ServiceSupplierEntity, etc.).
     */
    @Column(name = "parent_entity_type", length = 100)
    private String parentEntityType;

    /**
     * Enum for phone number types.
     */
    public enum PhoneType {
        PRIMARY,
        SECONDARY,
        LOCATION_PRIMARY,
        LOCATION_SECONDARY
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PhoneNumberEntity that = (PhoneNumberEntity) o;
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
                "areaCode = " + getAreaCode() + ", " +
                "cityCode = " + getCityCode() + ", " +
                "localNumber = " + getLocalNumber() + ", " +
                "extension = " + getExtension() + ", " +
                "phoneType = " + getPhoneType() + ", " +
                "parentEntityUuid = " + getParentEntityUuid() + ", " +
                "parentEntityType = " + getParentEntityType() + ")";
    }
}