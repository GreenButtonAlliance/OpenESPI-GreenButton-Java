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

package org.greenbuttonalliance.espi.authserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for ClientRegistrationController
 * 
 * Tests OIDC Dynamic Client Registration (RFC 7591) with ESPI-specific validations
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientRegistrationController Tests")
class ClientRegistrationControllerTest {

    @Mock
    private RegisteredClientRepository clientRepository;

    private ClientRegistrationController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        controller = new ClientRegistrationController(clientRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Client Registration Tests")
    class ClientRegistrationTests {

        @Test
        @DisplayName("Should register new client with valid request")
        void shouldRegisterNewClientWithValidRequest() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            when(clientRepository.findByClientId(anyString())).thenReturn(null);

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.client_id").exists())
                    .andExpect(jsonPath("$.client_secret").exists())
                    .andExpect(jsonPath("$.client_name").value("Test ESPI Client"))
                    .andExpect(jsonPath("$.redirect_uris[0]").value("https://example.com/callback"))
                    .andExpect(jsonPath("$.grant_types").isArray())
                    .andExpect(jsonPath("$.scope").exists())
                    .andExpect(jsonPath("$.token_endpoint_auth_method").value("client_secret_basic"));

            // Verify client was saved
            ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
            verify(clientRepository).save(clientCaptor.capture());
            
            RegisteredClient savedClient = clientCaptor.getValue();
            assertThat(savedClient.getClientName()).isEqualTo("Test ESPI Client");
            assertThat(savedClient.getRedirectUris()).contains("https://example.com/callback");
            assertThat(savedClient.getAuthorizationGrantTypes()).contains(AuthorizationGrantType.AUTHORIZATION_CODE);
            assertThat(savedClient.getClientAuthenticationMethods()).contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        }

        @Test
        @DisplayName("Should register client with ESPI-specific scopes")
        void shouldRegisterClientWithEspiScopes() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setScope("openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.scope").value(containsString("FB=4_5_15")));

            // Verify ESPI scopes are saved
            ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
            verify(clientRepository).save(clientCaptor.capture());
            
            RegisteredClient savedClient = clientCaptor.getValue();
            assertThat(savedClient.getScopes()).contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
        }

        @Test
        @DisplayName("Should register client credentials flow for admin clients")
        void shouldRegisterClientCredentialsFlow() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setGrantTypes(List.of("client_credentials"));
            request.setScope("DataCustodian_Admin_Access");

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.grant_types[0]").value("client_credentials"))
                    .andExpect(jsonPath("$.scope").value("DataCustodian_Admin_Access"));

            // Verify admin client configuration
            ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
            verify(clientRepository).save(clientCaptor.capture());
            
            RegisteredClient savedClient = clientCaptor.getValue();
            assertThat(savedClient.getAuthorizationGrantTypes()).contains(AuthorizationGrantType.CLIENT_CREDENTIALS);
            assertThat(savedClient.getScopes()).contains("DataCustodian_Admin_Access");
        }

        @Test
        @DisplayName("Should generate unique client ID with ESPI prefix")
        void shouldGenerateUniqueClientIdWithEspiPrefix() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.client_id").value(startsWith("espi_client_")));
        }

        @Test
        @DisplayName("Should set client secret expiration when provided")
        void shouldSetClientSecretExpirationWhenProvided() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            long expirationTime = Instant.now().plusSeconds(86400).getEpochSecond(); // 24 hours
            request.setClientSecretExpiresAt(expirationTime);

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.client_secret_expires_at").value(expirationTime));

            // Verify expiration is set
            ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
            verify(clientRepository).save(clientCaptor.capture());
            
            RegisteredClient savedClient = clientCaptor.getValue();
            assertThat(savedClient.getClientSecretExpiresAt()).isNotNull();
            assertThat(savedClient.getClientSecretExpiresAt().getEpochSecond()).isEqualTo(expirationTime);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject registration without client name")
        void shouldRejectRegistrationWithoutClientName() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setClientName(null);

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("client_name is required")));

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject registration without redirect URIs")
        void shouldRejectRegistrationWithoutRedirectUris() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setRedirectUris(null);

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("redirect_uris is required")));

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject invalid redirect URI")
        void shouldRejectInvalidRedirectUri() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setRedirectUris(List.of("invalid-uri"));

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("Invalid redirect_uri")));

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject unsupported grant type for ESPI")
        void shouldRejectUnsupportedGrantType() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setGrantTypes(List.of("implicit")); // Not supported in ESPI

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("Unsupported grant_type for ESPI")));

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject unsupported authentication method")
        void shouldRejectUnsupportedAuthMethod() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setTokenEndpointAuthMethod("client_secret_jwt"); // Not commonly supported

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
                    .andExpect(jsonPath("$.error_description").value(containsString("Unsupported token_endpoint_auth_method")));

            verify(clientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Client Retrieval Tests")
    class ClientRetrievalTests {

        @Test
        @DisplayName("Should retrieve existing client information")
        void shouldRetrieveExistingClientInformation() throws Exception {
            // Given
            RegisteredClient existingClient = createRegisteredClient();
            when(clientRepository.findByClientId("test-client-id")).thenReturn(existingClient);

            // When & Then
            mockMvc.perform(get("/connect/register/test-client-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.client_id").value("test-client-id"))
                    .andExpect(jsonPath("$.client_name").value("Test Client"))
                    .andExpect(jsonPath("$.client_secret").doesNotExist()) // Should not return secret
                    .andExpect(jsonPath("$.redirect_uris").isArray())
                    .andExpect(jsonPath("$.grant_types").isArray());
        }

        @Test
        @DisplayName("Should return 404 for non-existent client")
        void shouldReturn404ForNonExistentClient() throws Exception {
            // Given
            when(clientRepository.findByClientId("non-existent")).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/connect/register/non-existent"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("ESPI Compliance Tests")
    class EspiComplianceTests {

        @Test
        @DisplayName("Should configure opaque tokens for ESPI compliance")
        void shouldConfigureOpaqueTokensForEspiCompliance() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Verify opaque token format (ESPI standard)
            ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
            verify(clientRepository).save(clientCaptor.capture());
            
            RegisteredClient savedClient = clientCaptor.getValue();
            assertThat(savedClient.getTokenSettings().getAccessTokenFormat().getValue()).isEqualTo("reference");
        }

        @Test
        @DisplayName("Should set appropriate token lifetimes for ESPI")
        void shouldSetAppropriateTokenLifetimesForEspi() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Verify ESPI-appropriate token lifetimes
            ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
            verify(clientRepository).save(clientCaptor.capture());
            
            RegisteredClient savedClient = clientCaptor.getValue();
            assertThat(savedClient.getTokenSettings().getAccessTokenTimeToLive().toMinutes()).isEqualTo(360); // 6 hours
            assertThat(savedClient.getTokenSettings().getRefreshTokenTimeToLive().toMinutes()).isEqualTo(3600); // 60 hours
        }

        @Test
        @DisplayName("Should require consent for customer data access")
        void shouldRequireConsentForCustomerDataAccess() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setGrantTypes(List.of("authorization_code", "refresh_token"));

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Verify consent requirement for authorization_code flow
            ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
            verify(clientRepository).save(clientCaptor.capture());
            
            RegisteredClient savedClient = clientCaptor.getValue();
            assertThat(savedClient.getClientSettings().isRequireAuthorizationConsent()).isTrue();
        }

        @Test
        @DisplayName("Should not require consent for admin clients")
        void shouldNotRequireConsentForAdminClients() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            request.setGrantTypes(List.of("client_credentials"));
            request.setScope("DataCustodian_Admin_Access");

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Verify no consent requirement for client_credentials flow
            ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
            verify(clientRepository).save(clientCaptor.capture());
            
            RegisteredClient savedClient = clientCaptor.getValue();
            assertThat(savedClient.getClientSettings().isRequireAuthorizationConsent()).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() throws Exception {
            // Given
            ClientRegistrationController.ClientRegistrationRequest request = createValidRegistrationRequest();
            when(clientRepository.findByClientId(anyString())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("server_error"))
                    .andExpect(jsonPath("$.error_description").value("Internal server error"));
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() throws Exception {
            // When & Then
            mockMvc.perform(post("/connect/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }
    }

    // Helper methods

    private ClientRegistrationController.ClientRegistrationRequest createValidRegistrationRequest() {
        ClientRegistrationController.ClientRegistrationRequest request = 
            new ClientRegistrationController.ClientRegistrationRequest();
        request.setClientName("Test ESPI Client");
        request.setRedirectUris(List.of("https://example.com/callback"));
        request.setGrantTypes(List.of("authorization_code", "refresh_token"));
        request.setScope("openid profile");
        request.setTokenEndpointAuthMethod("client_secret_basic");
        return request;
    }

    private RegisteredClient createRegisteredClient() {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client-id")
                .clientName("Test Client")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://example.com/callback")
                .scope("openid")
                .scope("profile")
                .build();
    }
}