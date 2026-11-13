/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.greenbuttonalliance.espi.authserver.integration.AdminMfaTestSupport.adminWithBothFactors;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security integration tests for the OAuth2 protocol endpoints: client authentication, grant/scope/
 * redirect-URI validation, CSRF exemptions, opaque-token properties and response headers.
 *
 * <p>Runs against the real security configuration and the shared MySQL Testcontainer (#129). Admin-API
 * access control is covered by {@link AdminEndpointAuthorizationIntegrationTest} and
 * {@link AdminBearerTokenIntegrationTest}; RFC 7591 registration by
 * {@link OAuth2DynamicClientRegistrationIntegrationTest}.</p>
 */
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest extends AbstractAuthserverIntegrationTest {

    /** Seeded client_credentials client (AuthorizationServerConfig#registeredClientRepository). */
    private static final String ADMIN_CLIENT_ID = "data_custodian_admin";
    private static final String ADMIN_CLIENT_SECRET = "dc-secret";
    private static final String ADMIN_SCOPE = "DataCustodian_Admin_Access";

    /** Seeded client_credentials client with a different scope. */
    private static final String TP_ADMIN_CLIENT_ID = "third_party_admin";
    private static final String TP_ADMIN_CLIENT_SECRET = "tpadmin-secret";

    /** Seeded DC resource-server introspection client. */
    private static final String INTROSPECTION_CLIENT_ID = "datacustodian";
    private static final String INTROSPECTION_CLIENT_SECRET = "datacustodian-secret";

    /** Seeded authorization_code client (no client_credentials grant). */
    private static final String TP_CLIENT_ID = "third_party";
    private static final String TP_CLIENT_SECRET = "tp-secret";

    private static final String ESPI_SCOPE_HOURLY = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";
    private static final String ESPI_SCOPE_15_MIN = "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13";
    private static final String REDIRECT_URI = "https://tp.example/espi/callback";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Per-test ESPI authorization_code client with an https redirect URI (Spring AS rejects "localhost"). */
    private String authCodeClientId;

    @BeforeEach
    void seedAuthorizationCodeClient() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        authCodeClientId = "sec-it-tp-" + suffix;
        registeredClientRepository.save(RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(authCodeClientId)
                .clientName("Security IT ThirdParty")
                .clientSecret("{noop}sec-it-secret-" + suffix) // JdbcRegisteredClientRepository requires unique secrets
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT_URI)
                .scope(ESPI_SCOPE_HOURLY)
                .scope(ESPI_SCOPE_15_MIN)
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofHours(6))
                        .build())
                .build());
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        /*
         * Accept: application/json is sent explicitly. The protocol chain's login entry point matches
         * text/html, and a request with no Accept header (or a wildcard Accept) is treated as compatible
         * with text/html, so such a request is redirected to /authorize/delegate instead of getting a 401.
         */
        @Test
        @DisplayName("Should require client authentication on token, introspection and revocation endpoints")
        void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .accept(MediaType.APPLICATION_JSON)
                            .param("grant_type", "client_credentials"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/oauth2/introspect")
                            .accept(MediaType.APPLICATION_JSON)
                            .param("token", "anything"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/oauth2/revoke")
                            .accept(MediaType.APPLICATION_JSON)
                            .param("token", "anything"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should accept valid client credentials")
        void shouldAcceptValidClientCredentials() throws Exception {
            mockMvc.perform(clientCredentials(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, ADMIN_SCOPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.scope").value(ADMIN_SCOPE));
        }

        @Test
        @DisplayName("Should reject invalid client credentials")
        void shouldRejectInvalidClientCredentials() throws Exception {
            mockMvc.perform(clientCredentials(ADMIN_CLIENT_ID, "wrong-password", ADMIN_SCOPE))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("invalid_client"));
        }

        @Test
        @DisplayName("Should reject non-existent client")
        void shouldRejectNonExistentClient() throws Exception {
            mockMvc.perform(clientCredentials("non-existent-client", "password", ADMIN_SCOPE))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("invalid_client"));
        }

        @Test
        @DisplayName("Should reject a malformed Basic authorization header as invalid_request")
        void shouldHandleMalformedBasicAuthHeader() throws Exception {
            String malformedAuth = Base64.getEncoder().encodeToString("malformed".getBytes());

            mockMvc.perform(post("/oauth2/token")
                            .header("Authorization", "Basic " + malformedAuth)
                            .param("grant_type", "client_credentials")
                            .param("scope", ADMIN_SCOPE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_request"));
        }

        @Test
        @DisplayName("Should only accept the client's registered authentication method (client_secret_basic)")
        void shouldValidateClientAuthenticationMethod() throws Exception {
            mockMvc.perform(clientCredentials(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, ADMIN_SCOPE))
                    .andExpect(status().isOk());

            // data_custodian_admin is registered for client_secret_basic only
            mockMvc.perform(post("/oauth2/token")
                            .param("grant_type", "client_credentials")
                            .param("client_id", ADMIN_CLIENT_ID)
                            .param("client_secret", ADMIN_CLIENT_SECRET)
                            .param("scope", ADMIN_SCOPE))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("invalid_client"));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should enforce scope-based authorization")
        void shouldEnforceScopeBasedAuthorization() throws Exception {
            mockMvc.perform(clientCredentials(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, ADMIN_SCOPE))
                    .andExpect(status().isOk());

            // A client_credentials client cannot obtain a scope it is not registered for
            mockMvc.perform(clientCredentials(TP_ADMIN_CLIENT_ID, TP_ADMIN_CLIENT_SECRET, ADMIN_SCOPE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_scope"));
        }

        @Test
        @DisplayName("Should reject grant types the client is not registered for")
        void shouldValidateGrantTypeAuthorization() throws Exception {
            // data_custodian_admin is client_credentials only; Spring AS resolves the code first, and no
            // code can exist for a client without the authorization_code grant, so this is invalid_grant
            mockMvc.perform(post("/oauth2/token")
                            .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                            .param("grant_type", "authorization_code")
                            .param("code", "invalid-code")
                            .param("redirect_uri", REDIRECT_URI))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_grant"));

            // third_party is authorization_code/refresh_token only
            mockMvc.perform(clientCredentials(TP_CLIENT_ID, TP_CLIENT_SECRET, ESPI_SCOPE_HOURLY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("unauthorized_client"));
        }

        @Test
        @DisplayName("Should enforce redirect URI validation")
        void shouldEnforceRedirectUriValidation() throws Exception {
            // Registered redirect URI: the unauthenticated user is sent to the delegated login/consent
            mockMvc.perform(authorize(authCodeClientId, REDIRECT_URI, ESPI_SCOPE_HOURLY))
                    .andExpect(status().isFound())
                    .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                            .contains("/authorize/delegate"));

            // Unregistered redirect URI: 400, and never a redirect to the attacker's URI
            mockMvc.perform(authorize(authCodeClientId, "https://malicious.example/callback", ESPI_SCOPE_HOURLY))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().doesNotExist("Location"));
        }

        @Test
        @DisplayName("Should validate requested scopes")
        void shouldValidateRequestedScopes() throws Exception {
            // Scope the client is not registered for: invalid_scope is returned to the client's redirect URI
            mockMvc.perform(authorize(authCodeClientId, REDIRECT_URI,
                            "FB=4_5_16;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13"))
                    .andExpect(status().isFound())
                    .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                            .startsWith(REDIRECT_URI)
                            .contains("error=invalid_scope"));

            // Unknown scope at the token endpoint
            mockMvc.perform(clientCredentials(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, "invalid_scope"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_scope"));
        }
    }

    @Nested
    @DisplayName("CSRF Protection Tests")
    class CsrfProtectionTests {

        @Test
        @DisplayName("OAuth2 protocol endpoints are CSRF-exempt (client-authenticated, not session-based)")
        void protocolEndpointsAreCsrfExempt() throws Exception {
            // No csrf() post-processor: the token endpoint must still issue a token
            mockMvc.perform(clientCredentials(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, ADMIN_SCOPE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should not require authentication or CSRF for the discovery document")
        void shouldNotRequireCsrfForGetOperations() throws Exception {
            mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.issuer").value("http://localhost:9999"))
                    .andExpect(jsonPath("$.token_endpoint").value("http://localhost:9999/oauth2/token"))
                    .andExpect(jsonPath("$.introspection_endpoint").value("http://localhost:9999/oauth2/introspect"))
                    .andExpect(jsonPath("$.registration_endpoint").value("http://localhost:9999/oauth2/register"));
        }

        @Test
        @DisplayName("Should require CSRF for state-changing admin operations on the session chain")
        void shouldRequireCsrfForAdminOperations() throws Exception {
            mockMvc.perform(delete("/admin/oauth2/tokens/test-token")
                            .with(adminWithBothFactors("sec-it-admin", "ROLE_ADMIN")))
                    .andExpect(status().isForbidden());

            // With a CSRF token the request reaches the controller (unknown token -> 404)
            mockMvc.perform(delete("/admin/oauth2/tokens/test-token")
                            .with(adminWithBothFactors("sec-it-admin", "ROLE_ADMIN"))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Token Security Tests")
    class TokenSecurityTests {

        @Test
        @DisplayName("Should generate secure opaque (non-JWT) access tokens (ESPI)")
        void shouldGenerateSecureOpaqueTokens() throws Exception {
            String accessToken = obtainAccessToken(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, ADMIN_SCOPE);

            assertThat(accessToken)
                    .hasSizeGreaterThan(20)
                    .doesNotContain(".")
                    .doesNotStartWith("eyJ")
                    .matches("[A-Za-z0-9_-]+");
        }

        @Test
        @DisplayName("A revoked access token introspects as inactive")
        void shouldProtectAgainstTokenReplayAttacks() throws Exception {
            String accessToken = obtainAccessToken(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, ADMIN_SCOPE);

            mockMvc.perform(introspect(accessToken, ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));

            mockMvc.perform(post("/oauth2/revoke")
                            .with(httpBasic(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                            .param("token", accessToken))
                    .andExpect(status().isOk());

            mockMvc.perform(introspect(accessToken, ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @DisplayName("Should issue admin tokens that expire within one hour")
        void shouldEnforceTokenExpiration() throws Exception {
            JsonNode response = tokenResponse(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, ADMIN_SCOPE);

            assertThat(response.path("expires_in").asInt())
                    .isPositive()
                    .isLessThanOrEqualTo(3600);
        }

        @Test
        @DisplayName("The DataCustodian introspection client can introspect a token issued to another client")
        void shouldValidateTokenOwnership() throws Exception {
            String accessToken = obtainAccessToken(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET, ADMIN_SCOPE);

            mockMvc.perform(introspect(accessToken, INTROSPECTION_CLIENT_ID, INTROSPECTION_CLIENT_SECRET))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.client_id").value(ADMIN_CLIENT_ID))
                    .andExpect(jsonPath("$.scope").value(ADMIN_SCOPE));
        }
    }

    @Nested
    @DisplayName("ESPI Security Compliance Tests")
    class EspiSecurityComplianceTests {

        @Test
        @DisplayName("Should route an unauthenticated customer to the delegated login/consent")
        void shouldRequireAuthorizationConsentForCustomerData() throws Exception {
            mockMvc.perform(authorize(authCodeClientId, REDIRECT_URI, ESPI_SCOPE_HOURLY))
                    .andExpect(status().isFound())
                    .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                            .contains("/authorize/delegate")
                            .doesNotContain("code="));
        }

        @Test
        @DisplayName("Should accept multiple registered ESPI Function Block scopes")
        void shouldValidateEspiScopeFormat() throws Exception {
            mockMvc.perform(authorize(authCodeClientId, REDIRECT_URI, ESPI_SCOPE_HOURLY + " " + ESPI_SCOPE_15_MIN))
                    .andExpect(status().isFound())
                    .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                            .contains("/authorize/delegate")
                            .doesNotContain("error="));
        }

        @Test
        @DisplayName("The seeded third_party client issues opaque 6-hour access tokens (ESPI)")
        void shouldEnforceProperTokenLifetimesForEspi() {
            assertThat(registeredClientRepository.findByClientId(TP_CLIENT_ID))
                    .isNotNull()
                    .extracting(RegisteredClient::getTokenSettings)
                    .extracting(TokenSettings::getAccessTokenFormat, TokenSettings::getAccessTokenTimeToLive)
                    .containsExactly(OAuth2TokenFormat.REFERENCE, Duration.ofHours(6));
        }
    }

    @Nested
    @DisplayName("Security Headers Tests")
    class SecurityHeadersTests {

        @Test
        @DisplayName("Should include security headers in protocol responses")
        void shouldIncludeSecurityHeadersInResponses() throws Exception {
            mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Cache-Control", containsString("no-store")));
        }

        @Test
        @DisplayName("Should prevent clickjacking of the authorization endpoint")
        void shouldPreventClickjackingAttacks() throws Exception {
            mockMvc.perform(authorize(authCodeClientId, REDIRECT_URI, ESPI_SCOPE_HOURLY))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should reject script-injection attempts without redirecting to the injected URI")
        void shouldSanitizeAndValidateAllInputs() throws Exception {
            mockMvc.perform(get("/oauth2/authorize")
                            .accept(MediaType.TEXT_HTML)
                            .queryParam("response_type", "<script>alert('xss')</script>")
                            .queryParam("client_id", authCodeClientId)
                            .queryParam("redirect_uri", "javascript:alert('xss')")
                            .queryParam("scope", ESPI_SCOPE_HOURLY)
                            .queryParam("state", "<img src=x onerror=alert('xss')>"))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().doesNotExist("Location"));
        }

        @Test
        @DisplayName("Should treat SQL in the scope parameter as an invalid scope")
        void shouldHandleSqlInjectionAttempts() throws Exception {
            mockMvc.perform(clientCredentials(ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET,
                            "'; DROP TABLE oauth2_registered_client; --"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_scope"));

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM oauth2_registered_client WHERE client_id = ?",
                    Integer.class, ADMIN_CLIENT_ID))
                    .isOne();
        }

        @Test
        @DisplayName("Should reject an oversized scope parameter as invalid_scope")
        void shouldValidateParameterLengths() throws Exception {
            mockMvc.perform(authorize(authCodeClientId, REDIRECT_URI, "a".repeat(10000)))
                    .andExpect(status().isFound())
                    .andExpect(result -> assertThat(result.getResponse())
                            .extracting(MockHttpServletResponse::getRedirectedUrl)
                            .asString()
                            .startsWith(REDIRECT_URI)
                            .contains("error=invalid_scope"));
        }
    }

    // Helpers

    private static MockHttpServletRequestBuilder clientCredentials(String clientId, String secret, String scope) {
        return post("/oauth2/token")
                .with(httpBasic(clientId, secret))
                .param("grant_type", "client_credentials")
                .param("scope", scope);
    }

    private static MockHttpServletRequestBuilder introspect(String token, String clientId, String secret) {
        return post("/oauth2/introspect")
                .with(httpBasic(clientId, secret))
                .param("token", token);
    }

    /**
     * Browser-style authorization request (Accept: text/html, like a user agent). Uses {@code queryParam}:
     * Spring AS reads the authorization request from the query string, so {@code param(...)} on a GET
     * (which sets no query string in MockMvc) is reported as a missing {@code response_type}.
     */
    private static MockHttpServletRequestBuilder authorize(String clientId, String redirectUri, String scope) {
        return get("/oauth2/authorize")
                .accept(MediaType.TEXT_HTML)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", "test-state");
    }

    private JsonNode tokenResponse(String clientId, String secret, String scope) throws Exception {
        String body = mockMvc.perform(clientCredentials(clientId, secret, scope))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String obtainAccessToken(String clientId, String secret, String scope) throws Exception {
        return tokenResponse(clientId, secret, scope).get("access_token").asText();
    }
}
