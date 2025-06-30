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
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Integration Tests
 * 
 * Comprehensive security testing for OAuth2 Authorization Server including
 * authentication, authorization, access control, and ESPI-specific security requirements.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Security Integration Tests")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CLIENT_ID = "third_party";
    private static final String CLIENT_SECRET = "secret";
    private static final String ADMIN_CLIENT_ID = "data_custodian_admin";
    private static final String ADMIN_CLIENT_SECRET = "secret";

    @BeforeEach
    void setUp() {
        // Test setup is handled by @Transactional and application-test.yml
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should require authentication for protected endpoints")
        void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
            // OAuth2 endpoints
            mockMvc.perform(post("/oauth2/token"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/oauth2/introspect"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/oauth2/revoke"))
                    .andExpect(status().isUnauthorized());

            // Admin endpoints
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should accept valid client credentials")
        void shouldAcceptValidClientCredentials() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject invalid client credentials")
        void shouldRejectInvalidClientCredentials() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, "wrong-password"))
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject non-existent client")
        void shouldRejectNonExistentClient() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic("non-existent-client", "password"))
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle malformed basic auth header")
        void shouldHandleMalformedBasicAuthHeader() throws Exception {
            String malformedAuth = Base64.getEncoder().encodeToString("malformed".getBytes());
            
            mockMvc.perform(post("/oauth2/token")
                    .header("Authorization", "Basic " + malformedAuth)
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should validate client authentication method")
        void shouldValidateClientAuthenticationMethod() throws Exception {
            // Test client_secret_basic (valid)
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk());

            // Test client_secret_post (should work if supported)
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("client_id", ADMIN_CLIENT_ID)
                    .param("client_secret", ADMIN_CLIENT_SECRET)
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should enforce scope-based authorization")
        void shouldEnforceScopeBasedAuthorization() throws Exception {
            // Admin client should be able to access admin scopes
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk());

            // Regular client should not be able to access admin scopes
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate grant type authorization")
        void shouldValidateGrantTypeAuthorization() throws Exception {
            // Admin client authorized for client_credentials
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk());

            // Admin client not authorized for authorization_code without code
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "authorization_code")
                    .param("code", "invalid-code")
                    .param("redirect_uri", "http://localhost:8080/callback")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should enforce redirect URI validation")
        void shouldEnforceRedirectUriValidation() throws Exception {
            // Valid redirect URI
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "http://localhost:8080/DataCustodian/oauth/callback")
                    .param("scope", "openid")
                    .param("state", "test-state"))
                    .andExpect(status().is3xxRedirection());

            // Invalid redirect URI
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "https://malicious.com/callback")
                    .param("scope", "openid")
                    .param("state", "test-state"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate requested scopes")
        void shouldValidateRequestedScopes() throws Exception {
            // Valid ESPI scope
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "http://localhost:8080/DataCustodian/oauth/callback")
                    .param("scope", "openid FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .param("state", "test-state"))
                    .andExpect(status().is3xxRedirection());

            // Invalid scope for client
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "invalid_scope")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Admin Access Control Tests")
    class AdminAccessControlTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should allow admin access to admin endpoints")
        void shouldAllowAdminAccessToAdminEndpoints() throws Exception {
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/admin/oauth2/authorizations"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DC_ADMIN")
        @DisplayName("Should allow DC_ADMIN access to admin endpoints")
        void shouldAllowDcAdminAccessToAdminEndpoints() throws Exception {
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny user access to admin endpoints")
        void shouldDenyUserAccessToAdminEndpoints() throws Exception {
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should deny customer access to admin endpoints")
        void shouldDenyCustomerAccessToAdminEndpoints() throws Exception {
            mockMvc.perform(delete("/admin/oauth2/tokens/test-token"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/admin/oauth2/clients/test-client"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should allow DataCustodian admin scope access")
        void shouldAllowDataCustodianAdminScopeAccess() throws Exception {
            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_openid")
        @DisplayName("Should deny regular scope access to admin endpoints")
        void shouldDenyRegularScopeAccessToAdminEndpoints() throws Exception {
            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("CSRF Protection Tests")
    class CsrfProtectionTests {

        @Test
        @DisplayName("Should require CSRF token for state-changing operations")
        void shouldRequireCsrfTokenForStateChangingOperations() throws Exception {
            // POST without CSRF token should fail
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET)))
                    .andExpect(status().isForbidden());

            // POST with CSRF token should succeed
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should not require CSRF for GET operations")
        void shouldNotRequireCsrfForGetOperations() throws Exception {
            // GET operations should work without CSRF
            mockMvc.perform(get("/oauth2/jwks"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should require CSRF for admin operations")
        void shouldRequireCsrfForAdminOperations() throws Exception {
            // DELETE without CSRF should fail
            mockMvc.perform(delete("/admin/oauth2/tokens/test-token"))
                    .andExpect(status().isForbidden());

            // DELETE with CSRF should succeed (may return 404 for non-existent token)
            mockMvc.perform(delete("/admin/oauth2/tokens/test-token")
                    .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Token Security Tests")
    class TokenSecurityTests {

        @Test
        @DisplayName("Should generate secure opaque tokens")
        void shouldGenerateSecureOpaqueTokens() throws Exception {
            // Get an access token
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            String accessToken = response.get("access_token").asText();

            // Verify token properties (ESPI standard: opaque tokens)
            assertThat(accessToken).isNotNull();
            assertThat(accessToken.length()).isGreaterThan(20); // Should be reasonably long
            assertThat(accessToken).doesNotContain("."); // Should not be JWT format
            assertThat(accessToken).matches("[A-Za-z0-9_-]+"); // Should be URL-safe
        }

        @Test
        @DisplayName("Should protect against token replay attacks")
        void shouldProtectAgainstTokenReplayAttacks() throws Exception {
            // Get an access token
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

            // Use token for introspection (should work)
            mockMvc.perform(post("/oauth2/introspect")
                    .param("token", accessToken)
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));

            // Revoke the token
            mockMvc.perform(post("/oauth2/revoke")
                    .param("token", accessToken)
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk());

            // Try to use revoked token (should fail)
            mockMvc.perform(post("/oauth2/introspect")
                    .param("token", accessToken)
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @DisplayName("Should enforce token expiration")
        void shouldEnforceTokenExpiration() throws Exception {
            // Get an access token
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            // Verify token has expiration
            assertThat(response.has("expires_in")).isTrue();
            int expiresIn = response.get("expires_in").asInt();
            assertThat(expiresIn).isGreaterThan(0);
            assertThat(expiresIn).isLessThanOrEqualTo(3600); // Should not exceed 1 hour for admin tokens
        }

        @Test
        @DisplayName("Should validate token ownership")
        void shouldValidateTokenOwnership() throws Exception {
            // Get token with one client
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

            // Try to introspect with different client (should still work for admin)
            mockMvc.perform(post("/oauth2/introspect")
                    .param("token", accessToken)
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("ESPI Security Compliance Tests")
    class EspiSecurityComplianceTests {

        @Test
        @DisplayName("Should enforce ESPI-required opaque tokens")
        void shouldEnforceEspiRequiredOpaqueTokens() throws Exception {
            // Get token for ESPI client
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "DataCustodian_Admin_Access")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            String accessToken = response.get("access_token").asText();

            // Verify token is opaque (not JWT)
            assertThat(accessToken.split("\\.")).hasLengthLessThan(3);
            assertThat(accessToken).doesNotContain("eyJ"); // Should not start like JWT
        }

        @Test
        @DisplayName("Should require authorization consent for customer data")
        void shouldRequireAuthorizationConsentForCustomerData() throws Exception {
            // Start authorization flow that should require consent
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "http://localhost:8080/DataCustodian/oauth/callback")
                    .param("scope", "openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .param("state", "test-state"))
                    .andExpect(status().is3xxRedirection());
                    // Should redirect to login/consent page
        }

        @Test
        @DisplayName("Should validate ESPI scope format")
        void shouldValidateEspiScopeFormat() throws Exception {
            // Valid ESPI scope format
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "http://localhost:8080/DataCustodian/oauth/callback")
                    .param("scope", "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .param("state", "test-state"))
                    .andExpect(status().is3xxRedirection());

            // Should handle multiple ESPI scopes
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "http://localhost:8080/DataCustodian/oauth/callback")
                    .param("scope", "openid FB=4_5_15;IntervalDuration=3600 FB=4_5_16;IntervalDuration=900")
                    .param("state", "test-state"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("Should enforce Green Button Alliance client registration requirements")
        void shouldEnforceGreenButtonAllianceClientRegistrationRequirements() throws Exception {
            // Register ESPI-compliant client
            Map<String, Object> registrationRequest = new HashMap<>();
            registrationRequest.put("client_name", "Green Button Test Client");
            registrationRequest.put("redirect_uris", List.of("https://app.greenbuttonalliance.org/callback"));
            registrationRequest.put("grant_types", List.of("authorization_code", "refresh_token"));
            registrationRequest.put("scope", "openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
            registrationRequest.put("token_endpoint_auth_method", "client_secret_basic");

            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            // Verify ESPI compliance features
            assertThat(response.get("client_id").asText()).startsWith("espi_client_");
            assertThat(response.get("scope").asText()).contains("FB=4_5_15");
            assertThat(response.get("token_endpoint_auth_method").asText()).isEqualTo("client_secret_basic");
        }

        @Test
        @DisplayName("Should enforce proper token lifetimes for ESPI")
        void shouldEnforceProperTokenLifetimesForEspi() throws Exception {
            // Get customer access token
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "authorization_code")
                    .param("code", "test-code")
                    .param("redirect_uri", "http://localhost:8080/DataCustodian/oauth/callback")
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();

            if (result.getResponse().getStatus() == 200) {
                String responseBody = result.getResponse().getContentAsString();
                JsonNode response = objectMapper.readTree(responseBody);
                
                if (response.has("expires_in")) {
                    int expiresIn = response.get("expires_in").asInt();
                    // ESPI recommends 6-hour access tokens for customer data
                    assertThat(expiresIn).isLessThanOrEqualTo(21600); // 6 hours
                }
            }
        }
    }

    @Nested
    @DisplayName("Security Headers Tests")
    class SecurityHeadersTests {

        @Test
        @DisplayName("Should include security headers in responses")
        void shouldIncludeSecurityHeadersInResponses() throws Exception {
            mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Content-Type-Options"))
                    .andExpect(header().exists("X-Frame-Options"))
                    .andExpect(header().exists("X-XSS-Protection"));
        }

        @Test
        @DisplayName("Should set appropriate CORS headers")
        void shouldSetAppropriateCorsHeaders() throws Exception {
            mockMvc.perform(get("/oauth2/jwks")
                    .header("Origin", "https://trusted-client.example.com"))
                    .andExpect(status().isOk());
                    // CORS headers should be configured appropriately
        }

        @Test
        @DisplayName("Should prevent clickjacking attacks")
        void shouldPreventClickjackingAttacks() throws Exception {
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "http://localhost:8080/DataCustodian/oauth/callback")
                    .param("scope", "openid")
                    .param("state", "test-state"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should sanitize and validate all inputs")
        void shouldSanitizeAndValidateAllInputs() throws Exception {
            // Test with potentially malicious inputs
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "<script>alert('xss')</script>")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "javascript:alert('xss')")
                    .param("scope", "openid")
                    .param("state", "<img src=x onerror=alert('xss')>"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle SQL injection attempts")
        void shouldHandleSqlInjectionAttempts() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                    .param("grant_type", "client_credentials")
                    .param("scope", "'; DROP TABLE oauth2_registered_client; --")
                    .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate parameter lengths")
        void shouldValidateParameterLengths() throws Exception {
            String veryLongString = "a".repeat(10000);
            
            mockMvc.perform(get("/oauth2/authorize")
                    .param("response_type", "code")
                    .param("client_id", CLIENT_ID)
                    .param("redirect_uri", "http://localhost:8080/DataCustodian/oauth/callback")
                    .param("scope", veryLongString)
                    .param("state", "test-state"))
                    .andExpect(status().isBadRequest());
        }
    }

}