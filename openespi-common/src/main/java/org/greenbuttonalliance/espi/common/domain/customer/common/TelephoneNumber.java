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

package org.greenbuttonalliance.espi.common.domain.customer.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Embeddable TelephoneNumber type.
 * <p>
 * Telephone number representation.
 * Per customer.xsd TelephoneNumber type (lines 1428-1478).
 * <p>
 * Extends Object (NOT IdentifiedObject) per ESPI 4.0 specification.
 * Shared across multiple ESPI resources: Organisation, ServiceLocation, and others.
 * <p>
 * 8 fields per ESPI 4.0 specification:
 * countryCode, areaCode, cityCode, localNumber, ext, dialOut, internationalPrefix, ituPhone
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelephoneNumber implements Serializable {

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
