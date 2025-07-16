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
 * Embeddable class for OrganisationRole information.
 * 
 * Identifies a way in which an organisation may participate in the utility enterprise.
 * Organisation roles are not mutually exclusive; hence one organisation typically has many roles.
 * This is an embeddable component, not a standalone entity.
 */
@Embeddable
@Data
@NoArgsConstructor
@ToString
public class OrganisationRole {

    /**
     * Organisation having this role.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "organisationName", column = @Column(name = "role_organisation_name")),
        @AttributeOverride(name = "streetAddress.streetDetail", column = @Column(name = "role_street_detail")),
        @AttributeOverride(name = "streetAddress.townDetail", column = @Column(name = "role_town_detail")),
        @AttributeOverride(name = "streetAddress.stateOrProvince", column = @Column(name = "role_state_or_province")),
        @AttributeOverride(name = "streetAddress.postalCode", column = @Column(name = "role_postal_code")),
        @AttributeOverride(name = "streetAddress.country", column = @Column(name = "role_country")),
        @AttributeOverride(name = "postalAddress.streetDetail", column = @Column(name = "role_postal_street_detail")),
        @AttributeOverride(name = "postalAddress.townDetail", column = @Column(name = "role_postal_town_detail")),
        @AttributeOverride(name = "postalAddress.stateOrProvince", column = @Column(name = "role_postal_state_or_province")),
        @AttributeOverride(name = "postalAddress.postalCode", column = @Column(name = "role_postal_postal_code")),
        @AttributeOverride(name = "postalAddress.country", column = @Column(name = "role_postal_country")),
        @AttributeOverride(name = "electronicAddress.email1", column = @Column(name = "role_email1")),
        @AttributeOverride(name = "electronicAddress.email2", column = @Column(name = "role_email2")),
        @AttributeOverride(name = "electronicAddress.web", column = @Column(name = "role_web")),
        @AttributeOverride(name = "electronicAddress.radio", column = @Column(name = "role_radio"))
    })
    private Organisation organisation;
}