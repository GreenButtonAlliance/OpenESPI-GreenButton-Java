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
 * Embeddable class for Organisation information.
 * 
 * Organisation that might have roles as utility, customer, supplier, manufacturer, etc.
 * This is an embeddable component, not a standalone entity.
 */
@Embeddable
@Data
@NoArgsConstructor
@ToString
public class Organisation {

    /**
     * Organisation name (replaces deprecated 'name' field)
     */
    @Column(name = "organisation_name", length = 256)
    private String organisationName;

    /**
     * Street address for this organisation.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "streetDetail", column = @Column(name = "street_detail")),
        @AttributeOverride(name = "townDetail", column = @Column(name = "town_detail")),
        @AttributeOverride(name = "stateOrProvince", column = @Column(name = "state_or_province")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "country"))
    })
    private StreetAddress streetAddress;

    /**
     * Postal address for this organisation.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "streetDetail", column = @Column(name = "postal_street_detail")),
        @AttributeOverride(name = "townDetail", column = @Column(name = "postal_town_detail")),
        @AttributeOverride(name = "stateOrProvince", column = @Column(name = "postal_state_or_province")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "postal_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "postal_country"))
    })
    private StreetAddress postalAddress;

    /**
     * Primary phone number for this organisation.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "areaCode", column = @Column(name = "phone1_area_code")),
        @AttributeOverride(name = "cityCode", column = @Column(name = "phone1_city_code")),
        @AttributeOverride(name = "localNumber", column = @Column(name = "phone1_local_number")),
        @AttributeOverride(name = "extension", column = @Column(name = "phone1_extension"))
    })
    private PhoneNumber phone1;

    /**
     * Secondary phone number for this organisation.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "areaCode", column = @Column(name = "phone2_area_code")),
        @AttributeOverride(name = "cityCode", column = @Column(name = "phone2_city_code")),
        @AttributeOverride(name = "localNumber", column = @Column(name = "phone2_local_number")),
        @AttributeOverride(name = "extension", column = @Column(name = "phone2_extension"))
    })
    private PhoneNumber phone2;

    /**
     * Electronic address for this organisation.
     */
    @Embedded
    private ElectronicAddress electronicAddress;

    /**
     * Embeddable class for StreetAddress
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class StreetAddress {
        private String streetDetail;
        private String townDetail;
        private String stateOrProvince;
        private String postalCode;
        private String country;
    }

    /**
     * Embeddable class for PhoneNumber
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class PhoneNumber {
        private String areaCode;
        private String cityCode;
        private String localNumber;
        private String extension;
    }

    /**
     * Embeddable class for ElectronicAddress
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class ElectronicAddress {
        @Column(name = "email1", length = 256)
        private String email1;
        
        @Column(name = "email2", length = 256)
        private String email2;
        
        @Column(name = "web", length = 256)
        private String web;
        
        @Column(name = "radio", length = 256)
        private String radio;
    }
}