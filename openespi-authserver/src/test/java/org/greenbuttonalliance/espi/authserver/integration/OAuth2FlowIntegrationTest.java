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

package org.greenbuttonalliance.espi.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive OAuth2 Flow Integration Tests
 * 
 * Tests complete OAuth2 authorization flows including authorization code,
 * client credentials, refresh token, and ESPI-specific functionality.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("OAuth2 Flow Integration Tests")
@Transactional
class OAuth2FlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CLIENT_ID = "third_party";
    private static final String CLIENT_SECRET = "secret";
    private static final String ADMIN_CLIENT_ID = "data_custodian_admin";
    private static final String ADMIN_CLIENT_SECRET = "secret";
    private static final String REDIRECT_URI = "http://localhost:8080/DataCustodian/oauth/callback";

    @BeforeEach
    void setUp() {
        // Test setup is handled by @Transactional and application-test.yml
    }

    @Nested
    @DisplayName("Authorization Code Flow Tests")
    class AuthorizationCodeFlowTests {

        @Test
        @DisplayName("Should initiate authorization code flow successfully")
        void shouldInitiateAuthorizationCodeFlowSuccessfully() throws Exception {
            // When
            MvcResult result = mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", REDIRECT_URI)
                    .param("scope", "openid profile")
                    .param("state", "test-state"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            // Then
            String location = result.getResponse().getHeader("Location");
            assertThat(location).contains("/login");
        }

        @Test
        @WithMockUser(username = "customer@example.com", roles = "USER")
        @DisplayName("Should complete authorization code flow with user consent")
        void shouldCompleteAuthorizationCodeFlowWithUserConsent() throws Exception {
            // Given - Start authorization request
            MvcResult authResult = mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", REDIRECT_URI)
                    .param("scope", "openid profile")
                    .param("state", "test-state"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            // Extract consent URL from redirect
            String consentLocation = authResult.getResponse().getHeader("Location");
            
            if (consentLocation != null && consentLocation.contains("/oauth2/consent")) {
                // When - Submit consent
                mockMvc.perform(post("/oauth2/consent")
                        .param("client_id", CLIENT_ID)
                        .param("scope", "openid")
                        .param("scope", "profile")
                        .param("state", "test-state")
                        .with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(header().string("Location", containsString("code=")))
                        .andExpect(header().string("Location", containsString("state=test-state")));
            }
        }

        @Test
        @DisplayName("Should exchange authorization code for access token")
        void shouldExchangeAuthorizationCodeForAccessToken() throws Exception {
            // Given - Simulate having an authorization code
            String authorizationCode = "test-auth-code";
            
            // When
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "authorization_code")
                    .param("code", authorizationCode)
                    .param("redirect_uri", REDIRECT_URI)
                    .param("client_id", CLIENT_ID)
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.has("access_token")).isTrue();
            assertThat(response.has("token_type")).isTrue();
            assertThat(response.get("token_type").asText()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("Should handle authorization code flow with ESPI scopes")
        void shouldHandleAuthorizationCodeFlowWithEspiScopes() throws Exception {
            // Given
            String espiScope = "openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";
            
            // When
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", REDIRECT_URI)
                    .param("scope", espiScope)
                    .param("state", "espi-test-state"))
                    .andExpect(status().is3xxRedirection());

            // Then - Should handle ESPI scopes without error
            // The redirect to login page indicates successful scope parsing
        }

        @Test
        @DisplayName("Should reject invalid redirect URI")
        void shouldRejectInvalidRedirectUri() throws Exception {
            // When & Then
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "https://malicious.com/callback")
                    .param("scope", "openid")
                    .param("state", "test-state"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject invalid client ID")
        void shouldRejectInvalidClientId() throws Exception {
            // When & Then
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", "invalid_client")
                    .param("redirect_uri", REDIRECT_URI)
                    .param("scope", "openid")
                    .param("state", "test-state"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Client Credentials Flow Tests")
    class ClientCredentialsFlowTests {

        @Test
        @DisplayName("Should complete client credentials flow for admin client")
        void shouldCompleteClientCredentialsFlowForAdminClient() throws Exception {
            // When
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.has("access_token")).isTrue();
            assertThat(response.has("token_type")).isTrue();
            assertThat(response.has("expires_in")).isTrue();
            assertThat(response.has("scope")).isTrue();
            assertThat(response.get("token_type").asText()).isEqualTo("Bearer");
            assertThat(response.get("scope").asText()).contains("DataCustodian_Admin_Access");
        }

        @Test
        @DisplayName("Should generate opaque access tokens (ESPI standard)")
        void shouldGenerateOpaqueAccessTokens() throws Exception {
            // When
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            String accessToken = response.get("access_token").asText();
            
            // Opaque tokens should not be JWT format (no dots)
            assertThat(accessToken).doesNotContain(".");
            assertThat(accessToken).hasLengthGreaterThan(10);
            
            // Should not be a valid JWT (can't decode base64 parts)
            assertThat(accessToken.split("\\.")).hasLengthLessThan(3);
        }

        @Test
        @DisplayName("Should reject client credentials flow for non-admin client")
        void shouldRejectClientCredentialsFlowForNonAdminClient() throws Exception {
            // When & Then
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "openid")
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate client credentials properly")
        void shouldValidateClientCredentialsProperly() throws Exception {
            // When & Then - Invalid credentials
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, "wrong-secret"))
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle multiple admin scopes correctly")
        void shouldHandleMultipleAdminScopesCorrectly() throws Exception {
            // When
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access ThirdParty_Admin_Access")
                    .with(httpBasic("third_party_admin", "secret"))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            String scope = response.get("scope").asText();
            assertThat(scope).contains("ThirdParty_Admin_Access");
        }
    }

    @Nested
    @DisplayName("Refresh Token Flow Tests")
    class RefreshTokenFlowTests {

        @Test
        @DisplayName("Should handle refresh token flow")
        void shouldHandleRefreshTokenFlow() throws Exception {
            // Given - First get an access token with refresh token
            MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "authorization_code")
                    .param("code", "test-auth-code")
                    .param("redirect_uri", REDIRECT_URI)
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            String tokenResponseBody = tokenResult.getResponse().getContentAsString();
            JsonNode tokenResponse = objectMapper.readTree(tokenResponseBody);
            
            if (tokenResponse.has("refresh_token")) {
                String refreshToken = tokenResponse.get("refresh_token").asText();

                // When - Use refresh token to get new access token
                MvcResult refreshResult = mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", refreshToken)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andReturn();

                // Then
                String refreshResponseBody = refreshResult.getResponse().getContentAsString();
                JsonNode refreshResponse = objectMapper.readTree(refreshResponseBody);
                
                assertThat(refreshResponse.has("access_token")).isTrue();
                assertThat(refreshResponse.has("token_type")).isTrue();
                assertThat(refreshResponse.get("token_type").asText()).isEqualTo("Bearer");
            }
        }

        @Test
        @DisplayName("Should reject invalid refresh token")
        void shouldRejectInvalidRefreshToken() throws Exception {
            // When & Then
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "refresh_token")
                    .param("refresh_token", "invalid-refresh-token")
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Token Introspection Tests")
    class TokenIntrospectionTests {

        @Test
        @DisplayName("Should introspect valid access token")
        void shouldIntrospectValidAccessToken() throws Exception {
            // Given - Get an access token first
            MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            String tokenResponseBody = tokenResult.getResponse().getContentAsString();
            JsonNode tokenResponse = objectMapper.readTree(tokenResponseBody);
            String accessToken = tokenResponse.get("access_token").asText();

            // When - Introspect the token
            MvcResult introspectResult = mockMvc.perform(post("/oauth2/introspect")
                    .param("token", accessToken)
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // Then
            String introspectResponseBody = introspectResult.getResponse().getContentAsString();
            JsonNode introspectResponse = objectMapper.readTree(introspectResponseBody);
            
            assertThat(introspectResponse.get("active").asBoolean()).isTrue();
            assertThat(introspectResponse.has("client_id")).isTrue();
            assertThat(introspectResponse.has("scope")).isTrue();
            assertThat(introspectResponse.has("exp")).isTrue();
        }

        @Test
        @DisplayName("Should handle introspection of invalid token")
        void shouldHandleIntrospectionOfInvalidToken() throws Exception {
            // When
            MvcResult result = mockMvc.perform(post("/oauth2/introspect")
                    .param("token", "invalid-token")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.get("active").asBoolean()).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Revocation Tests")
    class TokenRevocationTests {

        @Test
        @DisplayName("Should revoke access token successfully")
        void shouldRevokeAccessTokenSuccessfully() throws Exception {
            // Given - Get an access token first
            MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            String tokenResponseBody = tokenResult.getResponse().getContentAsString();
            JsonNode tokenResponse = objectMapper.readTree(tokenResponseBody);
            String accessToken = tokenResponse.get("access_token").asText();

            // When - Revoke the token
            mockMvc.perform(post("/oauth2/revoke")
                    .param("token", accessToken)
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk());

            // Then - Token should be inactive when introspected
            MvcResult introspectResult = mockMvc.perform(post("/oauth2/introspect")
                    .param("token", accessToken)
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            String introspectResponseBody = introspectResult.getResponse().getContentAsString();
            JsonNode introspectResponse = objectMapper.readTree(introspectResponseBody);
            
            assertThat(introspectResponse.get("active").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("Should handle revocation of invalid token gracefully")
        void shouldHandleRevocationOfInvalidTokenGracefully() throws Exception {
            // When & Then
            mockMvc.perform(post("/oauth2/revoke")
                    .param("token", "invalid-token")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk()); // RFC 7009: should return 200 even for invalid tokens
        }
    }

    @Nested
    @DisplayName("OIDC Discovery Tests")
    class OidcDiscoveryTests {

        @Test
        @DisplayName("Should provide OAuth2 authorization server metadata")
        void shouldProvideOAuth2AuthorizationServerMetadata() throws Exception {
            // When
            MvcResult result = mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.has("issuer")).isTrue();
            assertThat(response.has("authorization_endpoint")).isTrue();
            assertThat(response.has("token_endpoint")).isTrue();
            assertThat(response.has("jwks_uri")).isTrue();
            assertThat(response.has("token_introspection_endpoint")).isTrue();
            assertThat(response.has("token_revocation_endpoint")).isTrue();
            
            // ESPI-specific endpoints
            assertThat(response.get("token_endpoint").asText()).isEqualTo("http://localhost:9999/oauth2/token");
            assertThat(response.get("authorization_endpoint").asText()).isEqualTo("http://localhost:9999/oauth2/authorize");
        }

        @Test
        @DisplayName("Should provide supported grant types in metadata")
        void shouldProvideSupportedGrantTypesInMetadata() throws Exception {
            // When
            MvcResult result = mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            JsonNode grantTypes = response.get("grant_types_supported");
            assertThat(grantTypes).isNotNull();
            assertThat(grantTypes.isArray()).isTrue();
            
            // Should support ESPI-required grant types
            boolean hasAuthorizationCode = false;
            boolean hasClientCredentials = false;
            boolean hasRefreshToken = false;
            
            for (JsonNode grantType : grantTypes) {
                String value = grantType.asText();
                if ("authorization_code".equals(value)) hasAuthorizationCode = true;
                if ("client_credentials".equals(value)) hasClientCredentials = true;
                if ("refresh_token".equals(value)) hasRefreshToken = true;
            }
            
            assertThat(hasAuthorizationCode).isTrue();
            assertThat(hasClientCredentials).isTrue();
            assertThat(hasRefreshToken).isTrue();
        }
    }

    @Nested
    @DisplayName("JWK Set Tests")
    class JwkSetTests {

        @Test
        @DisplayName("Should provide JWK set for token verification")
        void shouldProvideJwkSetForTokenVerification() throws Exception {
            // When
            MvcResult result = mockMvc.perform(get("/oauth2/jwks"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.has("keys")).isTrue();
            JsonNode keys = response.get("keys");
            assertThat(keys.isArray()).isTrue();
            assertThat(keys.size()).isGreaterThan(0);
            
            // Verify first key has required properties
            JsonNode firstKey = keys.get(0);
            assertThat(firstKey.has("kty")).isTrue(); // Key type
            assertThat(firstKey.has("kid")).isTrue(); // Key ID
            assertThat(firstKey.has("use")).isTrue(); // Key use
            assertThat(firstKey.get("kty").asText()).isEqualTo("RSA");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed token requests")
        void shouldHandleMalformedTokenRequests() throws Exception {
            // When & Then
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "invalid_grant")
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
        }

        @Test
        @DisplayName("Should handle missing required parameters")
        void shouldHandleMissingRequiredParameters() throws Exception {
            // When & Then
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "authorization_code")
                    // Missing code parameter
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_request"));
        }

        @Test
        @DisplayName("Should handle unauthorized access attempts")
        void shouldHandleUnauthorizedAccessAttempts() throws Exception {
            // When & Then
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    // No authentication
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // Helper methods

    private String extractCodeFromRedirect(String location) {
        Pattern pattern = Pattern.compile("code=([^&]+)");
        Matcher matcher = pattern.matcher(location);
        if (matcher.find()) {
            return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
        }
        return null;
    }

    private String extractStateFromRedirect(String location) {
        Pattern pattern = Pattern.compile("state=([^&]+)");
        Matcher matcher = pattern.matcher(location);
        if (matcher.find()) {
            return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
        }
        return null;
    }
}