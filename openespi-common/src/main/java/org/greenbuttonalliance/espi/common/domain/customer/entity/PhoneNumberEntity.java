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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;

import jakarta.persistence.*;

/**
 * JPA entity for PhoneNumber to resolve embedded mapping conflicts.
 * 
 * Separate entity table for phone numbers to eliminate column duplication
 * issues when multiple entities embed Organisation with PhoneNumber fields.
 */
@Entity
@Table(name = "phone_numbers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"uuid"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
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
}