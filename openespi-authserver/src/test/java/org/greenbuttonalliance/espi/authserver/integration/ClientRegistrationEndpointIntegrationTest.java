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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Client Registration Endpoint Integration Tests
 * 
 * Tests OIDC Dynamic Client Registration (RFC 7591) with ESPI-specific
 * validation and functionality including Green Button Alliance compliance.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Client Registration Endpoint Integration Tests")
@Transactional
class ClientRegistrationEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean up any test clients
        jdbcTemplate.execute("DELETE FROM oauth2_registered_client WHERE client_id LIKE 'test-%'");
    }

    @Nested
    @DisplayName("Client Registration Tests")
    class ClientRegistrationTests {

        @Test
        @DisplayName("Should register new ESPI client successfully")
        void shouldRegisterNewEspiClientSuccessfully() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.has("client_id")).isTrue();
            assertThat(response.has("client_secret")).isTrue();
            assertThat(response.has("client_id_issued_at")).isTrue();
            assertThat(response.get("client_name").asText()).isEqualTo("Test ESPI Client");
            assertThat(response.get("client_id").asText()).startsWith("espi_client_");
            
            // Verify ESPI scopes are preserved
            JsonNode scopes = response.get("scope");
            assertThat(scopes.asText()).contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
            
            // Verify grant types
            JsonNode grantTypes = response.get("grant_types");
            assertThat(grantTypes).isNotNull();
            assertThat(grantTypes.isArray()).isTrue();
        }

        @Test
        @DisplayName("Should register client with DataCustodian admin access")
        void shouldRegisterClientWithDataCustodianAdminAccess() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createAdminClientRegistrationRequest();

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.get("scope").asText()).contains("DataCustodian_Admin_Access");
            assertThat(response.get("grant_types").toString()).contains("client_credentials");
        }

        @Test
        @DisplayName("Should handle client registration with multiple redirect URIs")
        void shouldHandleClientRegistrationWithMultipleRedirectUris() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("redirect_uris", List.of(
                "https://app.example.com/callback",
                "https://app.example.com/callback2",
                "https://mobile.example.com/callback"
            ));

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            JsonNode redirectUris = response.get("redirect_uris");
            assertThat(redirectUris.isArray()).isTrue();
            assertThat(redirectUris.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should set client secret expiration when provided")
        void shouldSetClientSecretExpirationWhenProvided() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            long expirationTime = System.currentTimeMillis() / 1000 + 86400; // 24 hours from now
            registrationRequest.put("client_secret_expires_at", expirationTime);

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.has("client_secret_expires_at")).isTrue();
            assertThat(response.get("client_secret_expires_at").asLong()).isEqualTo(expirationTime);
        }

        @Test
        @DisplayName("Should configure opaque tokens for ESPI compliance")
        void shouldConfigureOpaqueTokensForEspiCompliance() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            String clientId = response.get("client_id").asText();
            
            // Verify in database that token format is set to opaque (reference)
            String tokenSettings = jdbcTemplate.queryForObject(
                "SELECT token_settings FROM oauth2_registered_client WHERE client_id = ?",
                String.class, clientId
            );
            
            assertThat(tokenSettings).contains("\"value\":\"reference\"");
        }
    }

    @Nested
    @DisplayName("Client Retrieval Tests")
    class ClientRetrievalTests {

        @Test
        @DisplayName("Should retrieve registered client information")
        void shouldRetrieveRegisteredClientInformation() throws Exception {
            // Given - Register a client first
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            
            MvcResult registrationResult = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String registrationResponseBody = registrationResult.getResponse().getContentAsString();
            JsonNode registrationResponse = objectMapper.readTree(registrationResponseBody);
            String clientId = registrationResponse.get("client_id").asText();

            // When - Retrieve the client
            MvcResult result = mockMvc.perform(get("/connect/register/" + clientId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            assertThat(response.get("client_id").asText()).isEqualTo(clientId);
            assertThat(response.get("client_name").asText()).isEqualTo("Test ESPI Client");
            assertThat(response.has("client_secret")).isFalse(); // Should not return secret
            assertThat(response.has("redirect_uris")).isTrue();
            assertThat(response.has("grant_types")).isTrue();
            assertThat(response.has("scope")).isTrue();
        }

        @Test
        @DisplayName("Should return 404 for non-existent client")
        void shouldReturn404ForNonExistentClient() throws Exception {
            // When & Then
            mockMvc.perform(get("/connect/register/non-existent-client"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should handle retrieval of ESPI client with complex scopes")
        void shouldHandleRetrievalOfEspiClientWithComplexScopes() throws Exception {
            // Given - Register client with complex ESPI scopes
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("scope", "openid profile " +
                "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13 " +
                "FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7 " +
                "DataCustodian_Admin_Access");

            MvcResult registrationResult = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String registrationResponseBody = registrationResult.getResponse().getContentAsString();
            JsonNode registrationResponse = objectMapper.readTree(registrationResponseBody);
            String clientId = registrationResponse.get("client_id").asText();

            // When - Retrieve the client
            MvcResult result = mockMvc.perform(get("/connect/register/" + clientId))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            String scope = response.get("scope").asText();
            assertThat(scope).contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
            assertThat(scope).contains("FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7");
            assertThat(scope).contains("DataCustodian_Admin_Access");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject registration without client name")
        void shouldRejectRegistrationWithoutClientName() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.remove("client_name");

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("client_name is required")));
        }

        @Test
        @DisplayName("Should reject registration without redirect URIs")
        void shouldRejectRegistrationWithoutRedirectUris() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.remove("redirect_uris");

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("redirect_uris is required")));
        }

        @Test
        @DisplayName("Should reject invalid redirect URI")
        void shouldRejectInvalidRedirectUri() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("redirect_uris", List.of("invalid-uri", "not-a-url"));

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("Invalid redirect_uri")));
        }

        @Test
        @DisplayName("Should reject unsupported grant type for ESPI")
        void shouldRejectUnsupportedGrantTypeForEspi() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("grant_types", List.of("implicit")); // Not supported in ESPI

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("Unsupported grant_type for ESPI")));
        }

        @Test
        @DisplayName("Should reject unsupported authentication method")
        void shouldRejectUnsupportedAuthenticationMethod() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("token_endpoint_auth_method", "client_secret_jwt"); // Not commonly supported

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("Unsupported token_endpoint_auth_method")));
        }

        @Test
        @DisplayName("Should validate HTTPS redirect URIs for production")
        void shouldValidateHttpsRedirectUrisForProduction() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("redirect_uris", List.of("http://insecure.example.com/callback"));

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("HTTPS required for redirect URIs")));
        }

        @Test
        @DisplayName("Should allow localhost HTTP URIs for development")
        void shouldAllowLocalhostHttpUrisForDevelopment() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("redirect_uris", List.of("http://localhost:8080/callback"));

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("ESPI Compliance Tests")
    class EspiComplianceTests {

        @Test
        @DisplayName("Should require authorization consent for customer clients")
        void shouldRequireAuthorizationConsentForCustomerClients() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("grant_types", List.of("authorization_code", "refresh_token"));

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            String clientId = response.get("client_id").asText();
            
            // Verify in database that consent is required
            String clientSettings = jdbcTemplate.queryForObject(
                "SELECT client_settings FROM oauth2_registered_client WHERE client_id = ?",
                String.class, clientId
            );
            
            assertThat(clientSettings).contains("\"settings.client.require-authorization-consent\":true");
        }

        @Test
        @DisplayName("Should not require consent for admin clients")
        void shouldNotRequireConsentForAdminClients() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createAdminClientRegistrationRequest();

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            String clientId = response.get("client_id").asText();
            
            // Verify in database that consent is not required for admin clients
            String clientSettings = jdbcTemplate.queryForObject(
                "SELECT client_settings FROM oauth2_registered_client WHERE client_id = ?",
                String.class, clientId
            );
            
            assertThat(clientSettings).contains("\"settings.client.require-authorization-consent\":false");
        }

        @Test
        @DisplayName("Should set appropriate token lifetimes for ESPI")
        void shouldSetAppropriateTokenLifetimesForEspi() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            String clientId = response.get("client_id").asText();
            
            // Verify token lifetimes in database
            String tokenSettings = jdbcTemplate.queryForObject(
                "SELECT token_settings FROM oauth2_registered_client WHERE client_id = ?",
                String.class, clientId
            );
            
            // ESPI-appropriate lifetimes: 6 hours access, 60 hours refresh
            assertThat(tokenSettings).contains("21600.000000000"); // 6 hours in seconds
            assertThat(tokenSettings).contains("216000.000000000"); // 60 hours in seconds
        }

        @Test
        @DisplayName("Should validate ESPI scope format")
        void shouldValidateEspiScopeFormat() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("scope", "openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.scope").value(containsString("FB=4_5_15")));
        }

        @Test
        @DisplayName("Should handle Green Button scope variations")
        void shouldHandleGreenButtonScopeVariations() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            registrationRequest.put("scope", 
                "openid profile " +
                "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13 " +
                "FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7 " +
                "FB=4_5_17;IntervalDuration=86400;BlockDuration=yearly;HistoryLength=1"
            );

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            
            String scope = response.get("scope").asText();
            assertThat(scope).contains("FB=4_5_15");
            assertThat(scope).contains("FB=4_5_16");
            assertThat(scope).contains("FB=4_5_17");
        }
    }

    @Nested
    @DisplayName("Database Integration Tests")
    class DatabaseIntegrationTests {

        @Test
        @DisplayName("Should persist registered client to database")
        void shouldPersistRegisteredClientToDatabase() throws Exception {
            // Given
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();

            // When
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            String clientId = response.get("client_id").asText();
            
            // Verify client exists in database
            int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_registered_client WHERE client_id = ?",
                Integer.class, clientId
            );
            assertThat(count).isEqualTo(1);
            
            // Verify client details
            Map<String, Object> clientData = jdbcTemplate.queryForMap(
                "SELECT client_name, scopes, authorization_grant_types FROM oauth2_registered_client WHERE client_id = ?",
                clientId
            );
            
            assertThat(clientData.get("client_name")).isEqualTo("Test ESPI Client");
            assertThat(clientData.get("scopes").toString()).contains("openid");
            assertThat(clientData.get("authorization_grant_types").toString()).contains("authorization_code");
        }

        @Test
        @DisplayName("Should handle concurrent client registrations")
        void shouldHandleConcurrentClientRegistrations() throws Exception {
            // Given
            Map<String, Object> request1 = createValidEspiRegistrationRequest();
            request1.put("client_name", "Concurrent Client 1");
            
            Map<String, Object> request2 = createValidEspiRegistrationRequest();
            request2.put("client_name", "Concurrent Client 2");

            // When - Simulate concurrent registrations
            MvcResult result1 = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated())
                    .andReturn();

            MvcResult result2 = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String response1Body = result1.getResponse().getContentAsString();
            String response2Body = result2.getResponse().getContentAsString();
            
            JsonNode response1 = objectMapper.readTree(response1Body);
            JsonNode response2 = objectMapper.readTree(response2Body);
            
            String clientId1 = response1.get("client_id").asText();
            String clientId2 = response2.get("client_id").asText();
            
            assertThat(clientId1).isNotEqualTo(clientId2);
            assertThat(response1.get("client_name").asText()).isEqualTo("Concurrent Client 1");
            assertThat(response2.get("client_name").asText()).isEqualTo("Concurrent Client 2");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() throws Exception {
            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle empty request body")
        void shouldHandleEmptyRequestBody() throws Exception {
            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"));
        }

        @Test
        @DisplayName("Should handle database constraint violations")
        void shouldHandleDatabaseConstraintViolations() throws Exception {
            // Given - Register a client first
            Map<String, Object> registrationRequest = createValidEspiRegistrationRequest();
            
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated());

            // When - Try to register with same details (should generate new client_id)
            MvcResult result = mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then - Should succeed with different client_id
            String responseBody = result.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseBody);
            assertThat(response.has("client_id")).isTrue();
        }
    }

    // Helper methods

    private Map<String, Object> createValidEspiRegistrationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("client_name", "Test ESPI Client");
        request.put("redirect_uris", List.of("https://example.com/callback"));
        request.put("grant_types", List.of("authorization_code", "refresh_token"));
        request.put("scope", "openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
        request.put("token_endpoint_auth_method", "client_secret_basic");
        return request;
    }

    private Map<String, Object> createAdminClientRegistrationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("client_name", "Test Admin Client");
        request.put("grant_types", List.of("client_credentials"));
        request.put("scope", "DataCustodian_Admin_Access");
        request.put("token_endpoint_auth_method", "client_secret_basic");
        return request;
    }
}