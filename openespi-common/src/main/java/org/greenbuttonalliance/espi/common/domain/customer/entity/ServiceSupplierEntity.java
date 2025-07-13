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
import org.greenbuttonalliance.espi.common.domain.customer.enums.SupplierKind;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Pure JPA/Hibernate entity for ServiceSupplier without JAXB concerns.
 * 
 * Organisation that provides services to customers.
 */
@Entity
@Table(name = "service_suppliers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"uuid"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
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
        @AttributeOverride(name = "phone1.areaCode", column = @Column(name = "supplier_org_phone1_area_code")),
        @AttributeOverride(name = "phone1.cityCode", column = @Column(name = "supplier_org_phone1_city_code")),
        @AttributeOverride(name = "phone1.localNumber", column = @Column(name = "supplier_org_phone1_local_number")),
        @AttributeOverride(name = "phone1.extension", column = @Column(name = "supplier_org_phone1_extension")),
        @AttributeOverride(name = "phone2.areaCode", column = @Column(name = "supplier_org_phone2_area_code")),
        @AttributeOverride(name = "phone2.cityCode", column = @Column(name = "supplier_org_phone2_city_code")),
        @AttributeOverride(name = "phone2.localNumber", column = @Column(name = "supplier_org_phone2_local_number")),
        @AttributeOverride(name = "phone2.extension", column = @Column(name = "supplier_org_phone2_extension")),
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
}