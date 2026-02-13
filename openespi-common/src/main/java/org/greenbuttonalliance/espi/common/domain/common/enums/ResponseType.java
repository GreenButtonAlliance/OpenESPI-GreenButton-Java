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
 * Enumeration for ResponseType values.
 *
 * OAuth 2.0 ESPI supported response types.
 * Per ESPI 4.0 espi.xsd lines 1684-1695.
 */
@XmlType(name = "ResponseType", namespace = "http://naesb.org/espi")
@XmlEnum
public enum ResponseType {

    /**
     * Indicates a request for an authorization code (RFC 6749 Section 4.1.1).
     * XSD value: "code" (line 1689)
     */
    @XmlEnumValue("code")
    CODE("code");

    private final String value;

    ResponseType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ResponseType fromValue(String value) {
        for (ResponseType type : ResponseType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ResponseType value: " + value);
    }
}