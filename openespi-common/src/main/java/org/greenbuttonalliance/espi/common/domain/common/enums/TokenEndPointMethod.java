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
 * Enumeration for TokenEndPointMethod values.
 *
 * Token endpoint method in OAuth 2.0.
 * Per ESPI 4.0 espi.xsd lines 1650-1661.
 */
@XmlType(name = "TokenEndPointMethod", namespace = "http://naesb.org/espi")
@XmlEnum
public enum TokenEndPointMethod {

    /**
     * Indicates the client uses HTTP Basic authentication (RFC 6749 Section 2.3.1).
     * XSD value: "client_secret_basic" (line 1655)
     */
    @XmlEnumValue("client_secret_basic")
    CLIENT_SECRET_BASIC("client_secret_basic");

    private final String value;

    TokenEndPointMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TokenEndPointMethod fromValue(String value) {
        for (TokenEndPointMethod type : TokenEndPointMethod.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid TokenEndPointMethod value: " + value);
    }
}