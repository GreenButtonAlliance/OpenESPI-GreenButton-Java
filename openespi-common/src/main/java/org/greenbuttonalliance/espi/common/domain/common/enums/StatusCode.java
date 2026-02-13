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
 * Enumeration for StatusCode values.
 *
 * Indicates the status code of the associated transaction.
 * Per ESPI 4.0 espi.xsd lines 4696-4790.
 */
@XmlType(name = "StatusCode", namespace = "http://naesb.org/espi")
@XmlEnum
public enum StatusCode {

    /**
     * Ok.
     * XSD value: 200 (line 4703)
     */
    @XmlEnumValue("200")
    OK(200),

    /**
     * Created.
     * XSD value: 201 (line 4709)
     */
    @XmlEnumValue("201")
    CREATED(201),

    /**
     * Accepted.
     * XSD value: 202 (line 4715)
     */
    @XmlEnumValue("202")
    ACCEPTED(202),

    /**
     * No Content.
     * XSD value: 204 (line 4721)
     */
    @XmlEnumValue("204")
    NO_CONTENT(204),

    /**
     * Moved Permanently.
     * XSD value: 301 (line 4727)
     */
    @XmlEnumValue("301")
    MOVED_PERMANENTLY(301),

    /**
     * Redirect.
     * XSD value: 302 (line 4733)
     */
    @XmlEnumValue("302")
    REDIRECT(302),

    /**
     * Not Modified.
     * XSD value: 304 (line 4739)
     */
    @XmlEnumValue("304")
    NOT_MODIFIED(304),

    /**
     * Bad Request.
     * XSD value: 400 (line 4745)
     */
    @XmlEnumValue("400")
    BAD_REQUEST(400),

    /**
     * Unauthorized.
     * XSD value: 401 (line 4751)
     */
    @XmlEnumValue("401")
    UNAUTHORIZED(401),

    /**
     * Forbidden.
     * XSD value: 403 (line 4757)
     */
    @XmlEnumValue("403")
    FORBIDDEN(403),

    /**
     * Not Found.
     * XSD value: 404 (line 4763)
     */
    @XmlEnumValue("404")
    NOT_FOUND(404),

    /**
     * Method Not Allowed.
     * XSD value: 405 (line 4769)
     */
    @XmlEnumValue("405")
    METHOD_NOT_ALLOWED(405),

    /**
     * Gone.
     * XSD value: 410 (line 4775)
     */
    @XmlEnumValue("410")
    GONE(410),

    /**
     * Internal Server Error.
     * XSD value: 500 (line 4781)
     */
    @XmlEnumValue("500")
    INTERNAL_SERVER_ERROR(500);

    private final int value;

    StatusCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static StatusCode fromValue(int value) {
        for (StatusCode kind : StatusCode.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid StatusCode value: " + value);
    }
}