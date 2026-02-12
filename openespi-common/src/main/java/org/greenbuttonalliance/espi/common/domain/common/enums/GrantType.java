/*
 * Copyright (c) 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.common.domain.common.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Enumeration for GrantType values.
 *
 * OAuth 2.0 ESPI supported grant types.
 * Per ESPI 4.0 espi.xsd lines 1662-1683.
 */
@XmlType(name = "GrantType", namespace = "http://naesb.org/espi")
@XmlEnum
public enum GrantType {

    /**
     * OAuth 2.0 Authorization Code Grant flow (RFC 6749 Section 4.1).
     * XSD value: "authorization_code" (line 1667)
     */
    @XmlEnumValue("authorization_code")
    AUTHORIZATION_CODE("authorization_code"),

    /**
     * OAuth 2.0 Client Credentials Grant flow (RFC 6749 Section 4.4).
     * XSD value: "client_credentials" (line 1672)
     */
    @XmlEnumValue("client_credentials")
    CLIENT_CREDENTIALS("client_credentials"),

    /**
     * OAuth 2.0 Refresh Token flow (RFC 6749 Section 6).
     * XSD value: "refresh_token" (line 1677)
     */
    @XmlEnumValue("refresh_token")
    REFRESH_TOKEN("refresh_token");

    private final String value;

    GrantType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GrantType fromValue(String value) {
        for (GrantType type : GrantType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid GrantType value: " + value);
    }
}