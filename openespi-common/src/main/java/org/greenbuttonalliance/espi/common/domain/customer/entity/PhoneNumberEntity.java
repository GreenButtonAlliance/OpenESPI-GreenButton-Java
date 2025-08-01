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

import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/**
 * JPA entity for PhoneNumber to resolve embedded mapping conflicts.
 * 
 * Separate entity table for phone numbers to eliminate column duplication
 * issues when multiple entities embed Organisation with PhoneNumber fields.
 */
@Entity
@Table(name = "phone_numbers")
@Getter
@Setter
@NoArgsConstructor
public class PhoneNumberEntity extends IdentifiedObject {

    /**
     * Area code for phone number.
     */
    @Column(name = "area_code", length = 10)
    private String areaCode;

    /**
     * City code for phone number.
     */
    @Column(name = "city_code", length = 10)
    private String cityCode;

    /**
     * Local number for phone number.
     */
    @Column(name = "local_number", length = 20)
    private String localNumber;

    /**
     * Extension for phone number.
     */
    @Column(name = "extension", length = 10)
    private String extension;

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
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PhoneNumberEntity that = (PhoneNumberEntity) o;
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
                "areaCode = " + getAreaCode() + ", " +
                "cityCode = " + getCityCode() + ", " +
                "localNumber = " + getLocalNumber() + ", " +
                "extension = " + getExtension() + ", " +
                "phoneType = " + getPhoneType() + ", " +
                "parentEntityUuid = " + getParentEntityUuid() + ", " +
                "parentEntityType = " + getParentEntityType() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}