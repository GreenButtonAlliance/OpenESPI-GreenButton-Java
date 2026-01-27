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

import java.io.Serializable;

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
public class Organisation implements Serializable {

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
    public static class StreetAddress implements Serializable {
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

    /**
     * Embeddable class for TelephoneNumber.
     * Per customer.xsd TelephoneNumber type (lines 1428-1478).
     * 8 fields per ESPI 4.0 specification.
     */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TelephoneNumber implements Serializable {
        @Column(name = "country_code", length = 256)
        private String countryCode;

        @Column(name = "area_code", length = 256)
        private String areaCode;

        @Column(name = "city_code", length = 256)
        private String cityCode;

        @Column(name = "local_number", length = 256)
        private String localNumber;

        @Column(name = "ext", length = 256)
        private String ext;

        @Column(name = "dial_out", length = 256)
        private String dialOut;

        @Column(name = "international_prefix", length = 256)
        private String internationalPrefix;

        @Column(name = "itu_phone", length = 256)
        private String ituPhone;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TelephoneNumber that = (TelephoneNumber) o;
            return java.util.Objects.equals(countryCode, that.countryCode) &&
                   java.util.Objects.equals(areaCode, that.areaCode) &&
                   java.util.Objects.equals(cityCode, that.cityCode) &&
                   java.util.Objects.equals(localNumber, that.localNumber) &&
                   java.util.Objects.equals(ext, that.ext) &&
                   java.util.Objects.equals(dialOut, that.dialOut) &&
                   java.util.Objects.equals(internationalPrefix, that.internationalPrefix) &&
                   java.util.Objects.equals(ituPhone, that.ituPhone);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(countryCode, areaCode, cityCode, localNumber, ext, dialOut, internationalPrefix, ituPhone);
        }

        @Override
        public String toString() {
            return "TelephoneNumber{" +
                    "countryCode='" + countryCode + '\'' +
                    ", areaCode='" + areaCode + '\'' +
                    ", cityCode='" + cityCode + '\'' +
                    ", localNumber='" + localNumber + '\'' +
                    ", ext='" + ext + '\'' +
                    ", dialOut='" + dialOut + '\'' +
                    ", internationalPrefix='" + internationalPrefix + '\'' +
                    ", ituPhone='" + ituPhone + '\'' +
                    '}';
        }
    }

    /**
     * Embeddable class for ElectronicAddress.
     * Per customer.xsd ElectronicAddress type (lines 886-936).
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class ElectronicAddress implements Serializable {
        @Column(name = "lan", length = 256)
        private String lan;

        @Column(name = "mac", length = 256)
        private String mac;

        @Column(name = "email1", length = 256)
        private String email1;

        @Column(name = "email2", length = 256)
        private String email2;

        @Column(name = "web", length = 256)
        private String web;

        @Column(name = "radio", length = 256)
        private String radio;

        @Column(name = "user_id", length = 256)
        private String userID;

        @Column(name = "password", length = 256)
        private String password;
    }
}