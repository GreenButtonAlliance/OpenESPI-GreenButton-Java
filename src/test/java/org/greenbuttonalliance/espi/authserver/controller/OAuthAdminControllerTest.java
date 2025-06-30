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
import org.greenbuttonalliance.espi.authserver.repository.JdbcRegisteredClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Comprehensive unit tests for OAuthAdminController
 * 
 * Tests administrative OAuth2 operations including token management,
 * client management, and authorization tracking with proper security.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthAdminController Tests")
class OAuthAdminControllerTest {

    @Mock
    private OAuth2AuthorizationService authorizationService;

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private JdbcRegisteredClientRepository jdbcRegisteredClientRepository;

    private OAuthAdminController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        controller = new OAuthAdminController(authorizationService, registeredClientRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Token Management Tests")
    class TokenManagementTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list all tokens with admin role")
        void shouldListAllTokensWithAdminRole() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].tokenId").value("sample-token-id"))
                    .andExpect(jsonPath("$[0].clientId").value("third_party"))
                    .andExpect(jsonPath("$[0].principalName").value("customer@example.com"))
                    .andExpect(jsonPath("$[0].scopes").isArray())
                    .andExpect(jsonPath("$[0].tokenType").value("Bearer"));
        }

        @Test
        @WithMockUser(roles = "DC_ADMIN")
        @DisplayName("Should list tokens with DC_ADMIN role")
        void shouldListTokensWithDcAdminRole() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny access to tokens with user role")
        void shouldDenyAccessToTokensWithUserRole() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter tokens by client ID")
        void shouldFilterTokensByClientId() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens")
                    .param("clientId", "third_party"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter tokens by principal name")
        void shouldFilterTokensByPrincipalName() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens")
                    .param("principalName", "customer@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should revoke token successfully")
        void shouldRevokeTokenSuccessfully() throws Exception {
            // Given
            OAuth2Authorization authorization = mock(OAuth2Authorization.class);
            when(authorizationService.findByToken("test-token", OAuth2TokenType.ACCESS_TOKEN))
                    .thenReturn(authorization);

            // When & Then
            mockMvc.perform(delete("/admin/oauth2/tokens/test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("revoked"))
                    .andExpect(jsonPath("$.tokenId").value("test-token"))
                    .andExpect(jsonPath("$.message").value("Token successfully revoked"));

            verify(authorizationService).remove(authorization);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when token not found for revocation")
        void shouldReturn404WhenTokenNotFoundForRevocation() throws Exception {
            // Given
            when(authorizationService.findByToken("non-existent", OAuth2TokenType.ACCESS_TOKEN))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(delete("/admin/oauth2/tokens/non-existent"))
                    .andExpect(status().isNotFound());

            verify(authorizationService, never()).remove(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle token revocation errors gracefully")
        void shouldHandleTokenRevocationErrorsGracefully() throws Exception {
            // Given
            when(authorizationService.findByToken("error-token", OAuth2TokenType.ACCESS_TOKEN))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            mockMvc.perform(delete("/admin/oauth2/tokens/error-token"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value(containsString("Failed to revoke token")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should revoke all client tokens successfully")
        void shouldRevokeAllClientTokensSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(delete("/admin/oauth2/clients/third_party/tokens"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("revoked"))
                    .andExpect(jsonPath("$.clientId").value("third_party"))
                    .andExpect(jsonPath("$.message").value("All tokens for client revoked successfully"));
        }
    }

    @Nested
    @DisplayName("Client Management Tests")
    class ClientManagementTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list all clients with JDBC repository")
        void shouldListAllClientsWithJdbcRepository() throws Exception {
            // Given
            controller = new OAuthAdminController(authorizationService, jdbcRegisteredClientRepository);
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

            List<RegisteredClient> mockClients = List.of(
                    createTestRegisteredClient("data_custodian_admin", "DataCustodian Admin"),
                    createEspiRegisteredClient("third_party", "ThirdParty Application")
            );
            when(jdbcRegisteredClientRepository.findAll()).thenReturn(mockClients);

            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].clientId").value("data_custodian_admin"))
                    .andExpect(jsonPath("$[0].clientName").value("DataCustodian Admin"))
                    .andExpect(jsonPath("$[0].active").value(true))
                    .andExpect(jsonPath("$[1].clientId").value("third_party"))
                    .andExpect(jsonPath("$[1].clientName").value("ThirdParty Application"));

            verify(jdbcRegisteredClientRepository).findAll();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return mock clients when JDBC repository unavailable")
        void shouldReturnMockClientsWhenJdbcRepositoryUnavailable() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].clientId").value("data_custodian_admin"))
                    .andExpect(jsonPath("$[1].clientId").value("third_party"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle client listing errors gracefully")
        void shouldHandleClientListingErrorsGracefully() throws Exception {
            // Given
            controller = new OAuthAdminController(authorizationService, jdbcRegisteredClientRepository);
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

            when(jdbcRegisteredClientRepository.findAll())
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray()); // Should return mock data
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get specific client by ID")
        void shouldGetSpecificClientById() throws Exception {
            // Given
            RegisteredClient client = createTestRegisteredClient("test-client", "Test Client");
            when(registeredClientRepository.findByClientId("test-client")).thenReturn(client);

            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients/test-client"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientId").value("test-client"))
                    .andExpect(jsonPath("$.clientName").value("Test Client"))
                    .andExpect(jsonPath("$.active").value(true));

            verify(registeredClientRepository).findByClientId("test-client");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 for non-existent client")
        void shouldReturn404ForNonExistentClient() throws Exception {
            // Given
            when(registeredClientRepository.findByClientId("non-existent")).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients/non-existent"))
                    .andExpect(status().isNotFound());

            verify(registeredClientRepository).findByClientId("non-existent");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete client successfully with JDBC repository")
        void shouldDeleteClientSuccessfullyWithJdbcRepository() throws Exception {
            // Given
            controller = new OAuthAdminController(authorizationService, jdbcRegisteredClientRepository);
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

            RegisteredClient client = createTestRegisteredClient("test-client", "Test Client");
            when(jdbcRegisteredClientRepository.findByClientId("test-client")).thenReturn(client);

            // When & Then
            mockMvc.perform(delete("/admin/oauth2/clients/test-client"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("deleted"))
                    .andExpect(jsonPath("$.clientId").value("test-client"))
                    .andExpect(jsonPath("$.message").value("Client successfully deleted"));

            verify(jdbcRegisteredClientRepository).findByClientId("test-client");
            verify(jdbcRegisteredClientRepository).deleteById(client.getId());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when deleting non-existent client")
        void shouldReturn404WhenDeletingNonExistentClient() throws Exception {
            // Given
            controller = new OAuthAdminController(authorizationService, jdbcRegisteredClientRepository);
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

            when(jdbcRegisteredClientRepository.findByClientId("non-existent")).thenReturn(null);

            // When & Then
            mockMvc.perform(delete("/admin/oauth2/clients/non-existent"))
                    .andExpect(status().isNotFound());

            verify(jdbcRegisteredClientRepository).findByClientId("non-existent");
            verify(jdbcRegisteredClientRepository, never()).deleteById(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle client deletion errors with non-JDBC repository")
        void shouldHandleClientDeletionErrorsWithNonJdbcRepository() throws Exception {
            // When & Then
            mockMvc.perform(delete("/admin/oauth2/clients/test-client"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Client deletion not supported with current repository"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle client deletion database errors gracefully")
        void shouldHandleClientDeletionDatabaseErrorsGracefully() throws Exception {
            // Given
            controller = new OAuthAdminController(authorizationService, jdbcRegisteredClientRepository);
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

            RegisteredClient client = createTestRegisteredClient("error-client", "Error Client");
            when(jdbcRegisteredClientRepository.findByClientId("error-client")).thenReturn(client);
            doThrow(new RuntimeException("Database error"))
                    .when(jdbcRegisteredClientRepository).deleteById(client.getId());

            // When & Then
            mockMvc.perform(delete("/admin/oauth2/clients/error-client"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value(containsString("Failed to delete client")));
        }
    }

    @Nested
    @DisplayName("Authorization Management Tests")
    class AuthorizationManagementTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list all authorizations")
        void shouldListAllAuthorizations() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/authorizations"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value("auth-123"))
                    .andExpect(jsonPath("$[0].clientId").value("third_party"))
                    .andExpect(jsonPath("$[0].principalName").value("customer@example.com"))
                    .andExpect(jsonPath("$[0].grantType").value("authorization_code"))
                    .andExpect(jsonPath("$[0].scopes").isArray())
                    .andExpect(jsonPath("$[0].active").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter authorizations by client ID")
        void shouldFilterAuthorizationsByClientId() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/authorizations")
                    .param("clientId", "third_party"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].clientId").value("third_party"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter authorizations by principal name")
        void shouldFilterAuthorizationsByPrincipalName() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/authorizations")
                    .param("principalName", "customer@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].principalName").value("customer@example.com"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return empty list when filtering with non-matching client ID")
        void shouldReturnEmptyListWhenFilteringWithNonMatchingClientId() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/authorizations")
                    .param("clientId", "non-existent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return empty list when filtering with non-matching principal name")
        void shouldReturnEmptyListWhenFilteringWithNonMatchingPrincipalName() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/authorizations")
                    .param("principalName", "non-existent@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithMockUser(roles = "DC_ADMIN")
        @DisplayName("Should allow DC_ADMIN to view authorizations")
        void shouldAllowDcAdminToViewAuthorizations() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/authorizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should deny unauthenticated access to admin endpoints")
        void shouldDenyUnauthenticatedAccessToAdminEndpoints() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/admin/oauth2/authorizations"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny user role access to admin endpoints")
        void shouldDenyUserRoleAccessToAdminEndpoints() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/admin/oauth2/authorizations"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should deny customer role access to admin endpoints")
        void shouldDenyCustomerRoleAccessToAdminEndpoints() throws Exception {
            // When & Then
            mockMvc.perform(delete("/admin/oauth2/tokens/test-token"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/admin/oauth2/clients/test-client"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("Should allow ROLE_ADMIN authority access")
        void shouldAllowRoleAdminAuthorityAccess() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "ROLE_DC_ADMIN")
        @DisplayName("Should allow ROLE_DC_ADMIN authority access")
        void shouldAllowRoleDcAdminAuthorityAccess() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("ESPI Compliance Tests")
    class EspiComplianceTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should properly handle ESPI scope formatting in client info")
        void shouldProperlyHandleEspiScopeFormattingInClientInfo() throws Exception {
            // Given
            RegisteredClient espiClient = createEspiRegisteredClient("espi-client", "ESPI Client");
            when(registeredClientRepository.findByClientId("espi-client")).thenReturn(espiClient);

            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients/espi-client"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scopes").isArray())
                    .andExpect(jsonPath("$.scopes[*]").value(hasItem(containsString("FB=4_5_15"))));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle DataCustodian admin client correctly")
        void shouldHandleDataCustodianAdminClientCorrectly() throws Exception {
            // Given
            RegisteredClient adminClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("data_custodian_admin")
                    .clientName("DataCustodian Admin")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scope("DataCustodian_Admin_Access")
                    .build();

            when(registeredClientRepository.findByClientId("data_custodian_admin")).thenReturn(adminClient);

            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients/data_custodian_admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientId").value("data_custodian_admin"))
                    .andExpect(jsonPath("$.grantTypes").isArray())
                    .andExpect(jsonPath("$.grantTypes[0]").value("client_credentials"))
                    .andExpect(jsonPath("$.scopes[0]").value("DataCustodian_Admin_Access"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should track ESPI token usage patterns")
        void shouldTrackEspiTokenUsagePatterns() throws Exception {
            // When & Then - Verify token info includes ESPI-specific scopes
            mockMvc.perform(get("/admin/oauth2/tokens"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].scopes").isArray())
                    .andExpect(jsonPath("$[0].scopes[*]").value(hasItem(containsString("FB=4_5_15"))));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle malformed requests gracefully")
        void shouldHandleMalformedRequestsGracefully() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/tokens")
                    .param("invalidParam", "value"))
                    .andExpect(status().isOk()); // Should ignore invalid params
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle null values in filters gracefully")
        void shouldHandleNullValuesInFiltersGracefully() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/oauth2/authorizations")
                    .param("clientId", "")
                    .param("principalName", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle special characters in path variables")
        void shouldHandleSpecialCharactersInPathVariables() throws Exception {
            // Given
            when(registeredClientRepository.findByClientId("client@test.com")).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/admin/oauth2/clients/client@test.com"))
                    .andExpect(status().isNotFound());
        }
    }

    // Helper methods

    private RegisteredClient createTestRegisteredClient(String clientId, String clientName) {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName(clientName)
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://example.com/callback")
                .scope("openid")
                .scope("profile")
                .build();
    }

    private RegisteredClient createEspiRegisteredClient(String clientId, String clientName) {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName(clientName)
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://example.com/callback")
                .scope("openid")
                .scope("profile")
                .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                .scope("DataCustodian_Admin_Access")
                .build();
    }
}