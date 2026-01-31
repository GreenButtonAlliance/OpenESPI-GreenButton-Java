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
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;

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

    /**
     * Primary phone number for this organisation.
     */
    @Embedded
    private TelephoneNumber phone1;

    /**
     * Secondary phone number for this organisation.
     */
    @Embedded
    private TelephoneNumber phone2;

    /**
     * Electronic address for this organisation.
     */
    @Embedded
    private ElectronicAddress electronicAddress;
}