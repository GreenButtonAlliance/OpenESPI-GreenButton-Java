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

import org.greenbuttonalliance.espi.authserver.controller.ClientRegistrationController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.greenbuttonalliance.espi.authserver.testdata.TestConstants.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Fixtures for OpenESPI Authorization Server
 * 
 * Provides pre-built test objects and mock configurations for consistent
 * testing across the Authorization Server test suite.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
public final class TestFixtures {

    private TestFixtures() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a valid OIDC client registration request for ESPI customer client
     */
    public static Map<String, Object> createValidEspiClientRegistrationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("client_name", TEST_CLIENT_NAME);
        request.put("redirect_uris", List.of(TEST_CALLBACK_URI));
        request.put("grant_types", List.of(AUTHORIZATION_CODE_GRANT, REFRESH_TOKEN_GRANT));
        request.put("scope", getCustomerEspiScopes());
        request.put("token_endpoint_auth_method", CLIENT_SECRET_BASIC);
        request.put("application_type", "web");
        request.put("contacts", List.of(TEST_USER_EMAIL));
        request.put("client_uri", "https://example.com");
        request.put("policy_uri", "https://example.com/privacy");
        request.put("tos_uri", "https://example.com/terms");
        return request;
    }

    /**
     * Creates a client registration request for admin client
     */
    public static Map<String, Object> createAdminClientRegistrationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("client_name", "Test Admin Client");
        request.put("grant_types", List.of(CLIENT_CREDENTIALS_GRANT));
        request.put("scope", DATACUSTODIAN_ADMIN_SCOPE);
        request.put("token_endpoint_auth_method", CLIENT_SECRET_BASIC);
        request.put("application_type", "service");
        return request;
    }

    /**
     * Creates an invalid client registration request for negative testing
     */
    public static Map<String, Object> createInvalidClientRegistrationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("client_name", ""); // Empty name
        request.put("redirect_uris", List.of(INVALID_HTTP_URI));
        request.put("grant_types", List.of(IMPLICIT_GRANT)); // Not supported
        request.put("scope", "invalid_scope");
        request.put("token_endpoint_auth_method", CLIENT_SECRET_JWT); // Not supported
        return request;
    }

    /**
     * Creates a Green Button Connect My Data client registration request
     */
    public static Map<String, Object> createGreenButtonConnectRegistrationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("client_name", "Green Button Connect My Data");
        request.put("redirect_uris", List.of("https://gbconnect.example.com/callback"));
        request.put("grant_types", List.of(AUTHORIZATION_CODE_GRANT, REFRESH_TOKEN_GRANT));
        request.put("scope", joinScopes(OPENID_SCOPE, PROFILE_SCOPE, ESPI_SCOPE_15_MIN_MONTHLY_13, ESPI_SCOPE_15_MIN_DAILY_7));
        request.put("token_endpoint_auth_method", CLIENT_SECRET_BASIC);
        request.put("application_type", "web");
        request.put("software_id", TEST_SOFTWARE_ID);
        request.put("software_version", TEST_SOFTWARE_VERSION);
        return request;
    }

    /**
     * Creates a mobile client registration request with PKCE
     */
    public static Map<String, Object> createMobileClientRegistrationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("client_name", "Mobile ESPI Client");
        request.put("redirect_uris", List.of("com.example.espi://callback"));
        request.put("grant_types", List.of(AUTHORIZATION_CODE_GRANT, REFRESH_TOKEN_GRANT));
        request.put("scope", getCustomerEspiScopes());
        request.put("token_endpoint_auth_method", NONE); // Public client
        request.put("application_type", "native");
        request.put("require_proof_key", true);
        return request;
    }

    /**
     * Creates a registered client from ClientRegistrationController request
     */
    public static ClientRegistrationController.ClientRegistrationRequest createClientRegistrationRequest() {
        ClientRegistrationController.ClientRegistrationRequest request = 
            new ClientRegistrationController.ClientRegistrationRequest();
        request.setClientName(TEST_CLIENT_NAME);
        request.setRedirectUris(List.of(TEST_CALLBACK_URI));
        request.setGrantTypes(List.of(AUTHORIZATION_CODE_GRANT, REFRESH_TOKEN_GRANT));
        request.setScope(getCustomerEspiScopes());
        request.setTokenEndpointAuthMethod(CLIENT_SECRET_BASIC);
        return request;
    }

    /**
     * Creates a mock Authentication for testing token customization
     */
    public static Authentication createMockAuthentication(String username, String... authorities) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        
        List<SimpleGrantedAuthority> grantedAuthorities = List.of(authorities)
            .stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
        
        when(authentication.getAuthorities()).thenReturn((Collection) grantedAuthorities);
        return authentication;
    }

    /**
     * Creates a mock JwtEncodingContext for token customizer testing
     */
    public static JwtEncodingContext createMockJwtEncodingContext(String clientId, String grantType, Authentication principal) {
        JwtEncodingContext context = mock(JwtEncodingContext.class);
        
        // Mock registered client
        RegisteredClient registeredClient = mock(RegisteredClient.class);
        when(registeredClient.getClientId()).thenReturn(clientId);
        when(context.getRegisteredClient()).thenReturn(registeredClient);
        
        // Mock grant type
        AuthorizationGrantType authGrantType = new AuthorizationGrantType(grantType);
        when(context.getAuthorizationGrantType()).thenReturn(authGrantType);
        
        // Mock token type
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        
        // Mock principal
        when(context.getPrincipal()).thenReturn(principal);
        
        return context;
    }

    /**
     * Creates test client credentials for OAuth2 token requests
     */
    public static class ClientCredentials {
        public final String clientId;
        public final String clientSecret;

        public ClientCredentials(String clientId, String clientSecret) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

        public static ClientCredentials datacustodianAdmin() {
            return new ClientCredentials(DEFAULT_DATACUSTODIAN_ADMIN_CLIENT_ID, DEFAULT_CLIENT_SECRET);
        }

        public static ClientCredentials thirdParty() {
            return new ClientCredentials(DEFAULT_THIRD_PARTY_CLIENT_ID, DEFAULT_CLIENT_SECRET);
        }

        public static ClientCredentials thirdPartyAdmin() {
            return new ClientCredentials(DEFAULT_THIRD_PARTY_ADMIN_CLIENT_ID, DEFAULT_CLIENT_SECRET);
        }

        public static ClientCredentials invalid() {
            return new ClientCredentials(TEST_INVALID_CLIENT_ID, "wrong-secret");
        }
    }

    /**
     * Creates OAuth2 authorization request parameters
     */
    public static class AuthorizationRequest {
        private final Map<String, String> parameters = new HashMap<>();

        public AuthorizationRequest() {
            parameters.put("response_type", CODE_RESPONSE_TYPE);
            parameters.put("client_id", DEFAULT_THIRD_PARTY_CLIENT_ID);
            parameters.put("redirect_uri", DATACUSTODIAN_CALLBACK_URI);
            parameters.put("scope", OPENID_SCOPE);
            parameters.put("state", TEST_STATE);
        }

        public AuthorizationRequest responseType(String responseType) {
            parameters.put("response_type", responseType);
            return this;
        }

        public AuthorizationRequest clientId(String clientId) {
            parameters.put("client_id", clientId);
            return this;
        }

        public AuthorizationRequest redirectUri(String redirectUri) {
            parameters.put("redirect_uri", redirectUri);
            return this;
        }

        public AuthorizationRequest scope(String scope) {
            parameters.put("scope", scope);
            return this;
        }

        public AuthorizationRequest state(String state) {
            parameters.put("state", state);
            return this;
        }

        public AuthorizationRequest nonce(String nonce) {
            parameters.put("nonce", nonce);
            return this;
        }

        public AuthorizationRequest codeChallenge(String codeChallenge) {
            parameters.put("code_challenge", codeChallenge);
            parameters.put("code_challenge_method", "S256");
            return this;
        }

        public Map<String, String> build() {
            return new HashMap<>(parameters);
        }

        public static AuthorizationRequest espiCustomer() {
            return new AuthorizationRequest()
                .scope(getCustomerEspiScopes())
                .state(TEST_STATE);
        }

        public static AuthorizationRequest withPkce() {
            return new AuthorizationRequest()
                .scope(getCustomerEspiScopes())
                .codeChallenge(TEST_CODE_CHALLENGE)
                .state(TEST_STATE);
        }

        public static AuthorizationRequest invalid() {
            return new AuthorizationRequest()
                .responseType(TOKEN_RESPONSE_TYPE) // Not supported
                .redirectUri(MALICIOUS_URI)
                .scope("invalid_scope");
        }
    }

    /**
     * Creates OAuth2 token request parameters
     */
    public static class TokenRequest {
        private final Map<String, String> parameters = new HashMap<>();

        public TokenRequest() {
            parameters.put("grant_type", CLIENT_CREDENTIALS_GRANT);
            parameters.put("scope", DATACUSTODIAN_ADMIN_SCOPE);
        }

        public TokenRequest grantType(String grantType) {
            parameters.put("grant_type", grantType);
            return this;
        }

        public TokenRequest scope(String scope) {
            parameters.put("scope", scope);
            return this;
        }

        public TokenRequest code(String code) {
            parameters.put("code", code);
            return this;
        }

        public TokenRequest redirectUri(String redirectUri) {
            parameters.put("redirect_uri", redirectUri);
            return this;
        }

        public TokenRequest refreshToken(String refreshToken) {
            parameters.put("refresh_token", refreshToken);
            return this;
        }

        public TokenRequest codeVerifier(String codeVerifier) {
            parameters.put("code_verifier", codeVerifier);
            return this;
        }

        public Map<String, String> build() {
            return new HashMap<>(parameters);
        }

        public static TokenRequest clientCredentials() {
            return new TokenRequest()
                .grantType(CLIENT_CREDENTIALS_GRANT)
                .scope(DATACUSTODIAN_ADMIN_SCOPE);
        }

        public static TokenRequest authorizationCode(String code) {
            return new TokenRequest()
                .grantType(AUTHORIZATION_CODE_GRANT)
                .code(code)
                .redirectUri(DATACUSTODIAN_CALLBACK_URI);
        }

        public static TokenRequest createRefreshTokenRequest(String refreshToken) {
            return new TokenRequest()
                .grantType(REFRESH_TOKEN_GRANT)
                .refreshToken(refreshToken);
        }

        public static TokenRequest invalid() {
            return new TokenRequest()
                .grantType(AUTHORIZATION_CODE_GRANT) // IMPLICIT removed in OAuth 2.1
                .scope("invalid_scope");
        }
    }

    /**
     * Creates test data for ESPI application information
     */
    public static Map<String, Object> createEspiApplicationInfo(String clientId) {
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("uuid", UUID.randomUUID().toString());
        appInfo.put("client_id", clientId);
        appInfo.put("client_name", TEST_CLIENT_NAME);
        appInfo.put("client_description", TEST_CLIENT_DESCRIPTION);
        appInfo.put("client_uri", "https://example.com");
        appInfo.put("logo_uri", "https://example.com/logo.png");
        appInfo.put("policy_uri", "https://example.com/privacy");
        appInfo.put("tos_uri", "https://example.com/terms");
        appInfo.put("software_id", TEST_SOFTWARE_ID);
        appInfo.put("software_version", TEST_SOFTWARE_VERSION);
        appInfo.put("scope", getCustomerEspiScopes());
        appInfo.put("grant_types", AUTHORIZATION_CODE_GRANT + "," + REFRESH_TOKEN_GRANT);
        appInfo.put("response_types", CODE_RESPONSE_TYPE);
        appInfo.put("token_endpoint_auth_method", CLIENT_SECRET_BASIC);
        appInfo.put("third_party_application_type", "WEB");
        appInfo.put("created_at", Instant.now());
        appInfo.put("updated_at", Instant.now());
        return appInfo;
    }

    /**
     * Creates test data for database operations
     */
    public static class DatabaseTestData {
        
        public static final String CLEAN_TEST_CLIENTS_SQL = 
            "DELETE FROM oauth2_registered_client WHERE client_id LIKE 'test_%'";
        
        public static final String CLEAN_TEST_AUTHORIZATIONS_SQL = 
            "DELETE FROM oauth2_authorization WHERE registered_client_id LIKE 'test_%'";
        
        public static final String CLEAN_TEST_ESPI_INFO_SQL = 
            "DELETE FROM espi_application_info WHERE client_id LIKE 'test_%'";

        public static final String COUNT_CLIENTS_SQL = 
            "SELECT COUNT(*) FROM oauth2_registered_client";
        
        public static final String FIND_CLIENT_BY_ID_SQL = 
            "SELECT * FROM oauth2_registered_client WHERE client_id = ?";
        
        public static final String FIND_ESPI_INFO_SQL = 
            "SELECT * FROM espi_application_info WHERE client_id = ?";
    }
}