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
 * Embeddable ElectronicAddress type.
 * <p>
 * Electronic address information (email, LAN, MAC, web, radio, etc.).
 * Per customer.xsd ElectronicAddress type (lines 886-936).
 * <p>
 * Extends Object (NOT IdentifiedObject) per ESPI 4.0 specification.
 * Shared across multiple ESPI resources: Asset, Organisation, and others.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicAddress implements Serializable {

    /**
     * LAN address for this electronic address.
     * XSD: String256, minOccurs="0"
     */
    @Column(name = "lan", length = 256)
    private String lan;

    /**
     * MAC address for this electronic address.
     * XSD: String256, minOccurs="0"
     */
    @Column(name = "mac", length = 256)
    private String mac;

    /**
     * Primary email address.
     * XSD: String256, minOccurs="0"
     */
    @Column(name = "email1", length = 256)
    private String email1;

    /**
     * Secondary email address.
     * XSD: String256, minOccurs="0"
     */
    @Column(name = "email2", length = 256)
    private String email2;

    /**
     * Web address (URL).
     * XSD: String256, minOccurs="0"
     */
    @Column(name = "web", length = 256)
    private String web;

    /**
     * Radio address.
     * XSD: String256, minOccurs="0"
     */
    @Column(name = "radio", length = 256)
    private String radio;

    /**
     * User ID for this electronic address.
     * XSD: String256, minOccurs="0"
     */
    @Column(name = "user_id", length = 256)
    private String userID;

    /**
     * Password for this electronic address.
     * XSD: String256, minOccurs="0"
     */
    @Column(name = "password", length = 256)
    private String password;
}