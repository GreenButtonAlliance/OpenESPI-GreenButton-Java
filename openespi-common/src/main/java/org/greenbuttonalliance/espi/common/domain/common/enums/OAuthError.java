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
 * Enumeration for OAuthError values.
 *
 * OAuth 2.0 ESPI supported error codes.
 * Per ESPI 4.0 espi.xsd lines 1708-1788.
 */
@XmlType(name = "OAuthError", namespace = "http://naesb.org/espi")
@XmlEnum
public enum OAuthError {

    /**
     * The request is missing a required parameter, includes an unsupported parameter value
     * (other than grant type), repeats a parameter, includes multiple credentials, utilizes
     * more than one mechanism for authenticating the client, or is otherwise malformed
     * (RFC 6749 Sections 4.1.2.1 and 5.2).
     * XSD value: "invalid_request" (line 1713)
     */
    @XmlEnumValue("invalid_request")
    INVALID_REQUEST("invalid_request"),

    /**
     * Client authentication failed (e.g., unknown client, no client authentication included,
     * or unsupported authentication method). The authorization server MAY return an HTTP 401
     * (Unauthorized) status code to indicate which HTTP authentication schemes are supported.
     * If the client attempted to authenticate via the Authorization request header field, the
     * authorization server MUST respond with an HTTP 401 (Unauthorized) status code and include
     * the WWW-Authenticate response header field matching the authentication scheme used by
     * the client (RFC 6749 Section 5.2).
     * XSD value: "invalid_client" (line 1719)
     */
    @XmlEnumValue("invalid_client")
    INVALID_CLIENT("invalid_client"),

    /**
     * The provided authorization code or refresh token is invalid, expired, revoked, does not
     * match the redirection URI used in the authorization request, or was issued to another
     * client (RFC 6749 Section 5.2).
     * XSD value: "invalid_grant" (line 1724)
     */
    @XmlEnumValue("invalid_grant")
    INVALID_GRANT("invalid_grant"),

    /**
     * The authenticated client is not authorized to use this authorization grant type
     * (RFC 6749 Sections 4.1.2.1 and 5.2).
     * XSD value: "unauthorized_client" (line 1729)
     */
    @XmlEnumValue("unauthorized_client")
    UNAUTHORIZED_CLIENT("unauthorized_client"),

    /**
     * The authorization grant type is not supported by the authorization server
     * (RFC 6749 Section 5.2).
     * XSD value: "unsupported_grant_type" (line 1735)
     */
    @XmlEnumValue("unsupported_grant_type")
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),

    /**
     * The requested scope is invalid, unknown, malformed, or exceeds the scope granted by the
     * resource owner (RFC 6749 Sections 4.1.2.1 and 5.2).
     * XSD value: "invalid_scope" (line 1740)
     */
    @XmlEnumValue("invalid_scope")
    INVALID_SCOPE("invalid_scope"),

    /**
     * The value of one or more redirection URIs is invalid (RFC 7591 Section 3.2.2).
     * XSD value: "invalid_redirect_uri" (line 1745)
     */
    @XmlEnumValue("invalid_redirect_uri")
    INVALID_REDIRECT_URI("invalid_redirect_uri"),

    /**
     * The value of one of the client metadata fields is invalid and server has rejected the
     * request. Note that an authorization server MAY choose to substitute a valid value for any
     * requested parameter of a client's metadata (RFC 7591 Section 3.2.2).
     * XSD value: "invalid_client_metadata" (line 1751)
     */
    @XmlEnumValue("invalid_client_metadata")
    INVALID_CLIENT_METADATA("invalid_client_metadata"),

    /**
     * [DEPRECATED] Client authentication failed (e.g., unknown client, no client authentication
     * included, or unsupported authentication method). The authorization server MAY return an
     * HTTP 401 (Unauthorized) status code to indicate which HTTP authentication schemes are
     * supported. If the client attempted to authenticate via the Authorization request header
     * field, the authorization server MUST respond with an HTTP 401 (Unauthorized) status code
     * and include the WWW-Authenticate response header field matching the authentication scheme
     * used by the client.
     * XSD value: "invalid_client_id" (line 1757)
     */
    @XmlEnumValue("invalid_client_id")
    INVALID_CLIENT_ID("invalid_client_id"),

    /**
     * The resource owner or authorization server denied the request (RFC 6749 Section 4.1.2.1).
     * XSD value: "access_denied" (line 1763)
     */
    @XmlEnumValue("access_denied")
    ACCESS_DENIED("access_denied"),

    /**
     * The authorization server does not support obtaining an authorization code using this
     * method (RFC 6749 Section 4.1.2.1).
     * XSD value: "unsupported_response_type" (line 1769)
     */
    @XmlEnumValue("unsupported_response_type")
    UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"),

    /**
     * The authorization server encountered an unexpected condition that prevented it from
     * fulfilling the request (RFC 6749 Section 4.1.2.1).
     * XSD value: "server_error" (line 1775)
     */
    @XmlEnumValue("server_error")
    SERVER_ERROR("server_error"),

    /**
     * The authorization server is currently unable to handle the request due to a temporary
     * overloading or maintenance of the server (RFC 6749 Section 4.1.2.1).
     * XSD value: "temporarily_unavailable" (line 1781)
     */
    @XmlEnumValue("temporarily_unavailable")
    TEMPORARILY_UNAVAILABLE("temporarily_unavailable");

    private final String value;

    OAuthError(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OAuthError fromValue(String value) {
        for (OAuthError type : OAuthError.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid OAuthError value: " + value);
    }
}