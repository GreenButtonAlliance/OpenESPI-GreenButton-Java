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
 * Enumeration for TokenType values.
 *
 * OAuth 2.0 ESPI supported token types.
 * Per ESPI 4.0 espi.xsd lines 1696-1707.
 */
@XmlType(name = "TokenType", namespace = "http://naesb.org/espi")
@XmlEnum
public enum TokenType {

    /**
     * A security token with the property that any party in possession of the token (a "bearer")
     * can use the token in any way that any other party in possession of it can. Using a bearer
     * token does not require a bearer to prove possession of cryptographic key material
     * (proof-of-possession) (RFC6750 Section 1.2).
     * XSD value: "Bearer" (line 1701)
     */
    @XmlEnumValue("Bearer")
    BEARER("Bearer");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TokenType fromValue(String value) {
        for (TokenType type : TokenType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid TokenType value: " + value);
    }
}