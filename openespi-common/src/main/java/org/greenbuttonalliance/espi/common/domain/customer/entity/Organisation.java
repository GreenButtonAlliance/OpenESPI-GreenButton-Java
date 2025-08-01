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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.*;

/**
 * Embeddable class for Organisation information.
 * 
 * Organisation that might have roles as utility, customer, supplier, manufacturer, etc.
 * This is an embeddable component, not a standalone entity.
 */
@Embeddable
@Getter
@Setter
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
    private StreetAddress streetAddress;

    /**
     * Postal address for this organisation.
     */
    @Embedded
    private StreetAddress postalAddress;

    // PhoneNumber fields removed - phone numbers are managed separately via PhoneNumberEntity
    // to avoid JPA column mapping conflicts in embedded contexts

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
        @Column(name = "street_detail", length = 256)
        private String streetDetail;
        
        @Column(name = "town_detail", length = 256)
        private String townDetail;
        
        @Column(name = "state_or_province", length = 256)
        private String stateOrProvince;
        
        @Column(name = "postal_code", length = 256)
        private String postalCode;
        
        @Column(name = "country", length = 256)
        private String country;
    }

    // PhoneNumber embeddable class removed - using separate PhoneNumberEntity table instead

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