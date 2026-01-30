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
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Embeddable StreetAddress type.
 * <p>
 * General purpose street and postal address information.
 * Per customer.xsd StreetAddress type (lines 1285-1320).
 * <p>
 * Extends Object (NOT IdentifiedObject) per ESPI 4.0 specification.
 * Shared across multiple ESPI resources: Organisation, Location, and others.
 * <p>
 * Note: The XSD defines fields (streetDetail, townDetail, status, postalCode, poBox)
 * but this implementation uses (streetDetail, townDetail, stateOrProvince, postalCode, country)
 * for practical address representation in use across the codebase.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreetAddress implements Serializable {

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
