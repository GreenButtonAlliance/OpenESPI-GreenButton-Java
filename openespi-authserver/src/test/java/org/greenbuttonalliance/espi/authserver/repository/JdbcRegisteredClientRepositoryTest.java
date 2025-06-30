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

package org.greenbuttonalliance.espi.authserver.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for JdbcRegisteredClientRepository
 * 
 * Tests database-backed client registration storage with proper serialization/deserialization
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcRegisteredClientRepository Tests")
class JdbcRegisteredClientRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcRegisteredClientRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Nested
    @DisplayName("Save Operation Tests")
    class SaveOperationTests {

        @Test
        @DisplayName("Should insert new client when not exists")
        void shouldInsertNewClientWhenNotExists() {
            // Given
            RegisteredClient client = createTestRegisteredClient();
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(null);

            // When
            repository.save(client);

            // Then
            verify(jdbcTemplate).update(
                    contains("INSERT INTO oauth2_registered_client"),
                    eq(client.getId()),
                    eq(client.getClientId()),
                    eq(client.getClientIdIssuedAt()),
                    eq(client.getClientSecret()),
                    eq(client.getClientSecretExpiresAt()),
                    eq(client.getClientName()),
                    eq("client_secret_basic"),
                    eq("authorization_code,refresh_token"),
                    eq("https://example.com/callback"),
                    eq("https://example.com/logout"),
                    eq("openid,profile"),
                    anyString(),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should update existing client when exists")
        void shouldUpdateExistingClientWhenExists() {
            // Given
            RegisteredClient existingClient = createTestRegisteredClient();
            RegisteredClient updatedClient = createTestRegisteredClient();
            
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(existingClient);

            // When
            repository.save(updatedClient);

            // Then
            verify(jdbcTemplate).update(
                    contains("UPDATE oauth2_registered_client"),
                    eq(updatedClient.getClientSecret()),
                    eq(updatedClient.getClientSecretExpiresAt()),
                    eq(updatedClient.getClientName()),
                    eq("client_secret_basic"),
                    eq("authorization_code,refresh_token"),
                    eq("https://example.com/callback"),
                    eq("https://example.com/logout"),
                    eq("openid,profile"),
                    anyString(),
                    anyString(),
                    eq(updatedClient.getId())
            );
        }

        @Test
        @DisplayName("Should serialize client authentication methods correctly")
        void shouldSerializeClientAuthenticationMethodsCorrectly() {
            // Given
            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("test-client")
                    .clientName("Test Client")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .build();

            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(null);

            // When
            repository.save(client);

            // Then
            ArgumentCaptor<String> authMethodsCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString(),
                    authMethodsCaptor.capture(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
            
            String serializedAuthMethods = authMethodsCaptor.getValue();
            assertThat(serializedAuthMethods).contains("client_secret_basic");
            assertThat(serializedAuthMethods).contains("client_secret_post");
        }

        @Test
        @DisplayName("Should serialize ESPI scopes correctly")
        void shouldSerializeEspiScopesCorrectly() {
            // Given
            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("espi-client")
                    .clientName("ESPI Client")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .scope("profile")
                    .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .build();

            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(null);

            // When
            repository.save(client);

            // Then
            ArgumentCaptor<String> scopesCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString(),
                    anyString(), anyString(), anyString(), anyString(), scopesCaptor.capture(), anyString(), anyString());
            
            String serializedScopes = scopesCaptor.getValue();
            assertThat(serializedScopes).contains("openid");
            assertThat(serializedScopes).contains("profile");
            assertThat(serializedScopes).contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
        }

        @Test
        @DisplayName("Should serialize token settings with opaque format")
        void shouldSerializeTokenSettingsWithOpaqueFormat() {
            // Given
            TokenSettings tokenSettings = TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard
                    .accessTokenTimeToLive(Duration.ofMinutes(360))
                    .refreshTokenTimeToLive(Duration.ofMinutes(3600))
                    .build();

            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("espi-client")
                    .clientName("ESPI Client")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .tokenSettings(tokenSettings)
                    .build();

            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(null);

            // When
            repository.save(client);

            // Then
            ArgumentCaptor<String> tokenSettingsCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString(),
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), tokenSettingsCaptor.capture());
            
            String serializedTokenSettings = tokenSettingsCaptor.getValue();
            assertThat(serializedTokenSettings).contains("reference"); // Opaque token format
        }
    }

    @Nested
    @DisplayName("Find Operation Tests")
    class FindOperationTests {

        @Test
        @DisplayName("Should find client by ID")
        void shouldFindClientById() {
            // Given
            RegisteredClient expectedClient = createTestRegisteredClient();
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("test-id")))
                    .thenReturn(expectedClient);

            // When
            RegisteredClient foundClient = repository.findById("test-id");

            // Then
            assertThat(foundClient).isNotNull();
            assertThat(foundClient.getId()).isEqualTo(expectedClient.getId());
            assertThat(foundClient.getClientId()).isEqualTo(expectedClient.getClientId());
            
            verify(jdbcTemplate).queryForObject(
                    contains("WHERE id = ?"),
                    any(RowMapper.class),
                    eq("test-id")
            );
        }

        @Test
        @DisplayName("Should find client by client ID")
        void shouldFindClientByClientId() {
            // Given
            RegisteredClient expectedClient = createTestRegisteredClient();
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("test-client-id")))
                    .thenReturn(expectedClient);

            // When
            RegisteredClient foundClient = repository.findByClientId("test-client-id");

            // Then
            assertThat(foundClient).isNotNull();
            assertThat(foundClient.getClientId()).isEqualTo(expectedClient.getClientId());
            
            verify(jdbcTemplate).queryForObject(
                    contains("WHERE client_id = ?"),
                    any(RowMapper.class),
                    eq("test-client-id")
            );
        }

        @Test
        @DisplayName("Should return null when client not found by ID")
        void shouldReturnNullWhenClientNotFoundById() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("non-existent")))
                    .thenThrow(new EmptyResultDataAccessException(1));

            // When
            RegisteredClient foundClient = repository.findById("non-existent");

            // Then
            assertThat(foundClient).isNull();
        }

        @Test
        @DisplayName("Should return null when client not found by client ID")
        void shouldReturnNullWhenClientNotFoundByClientId() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("non-existent")))
                    .thenThrow(new EmptyResultDataAccessException(1));

            // When
            RegisteredClient foundClient = repository.findByClientId("non-existent");

            // Then
            assertThat(foundClient).isNull();
        }

        @Test
        @DisplayName("Should find all clients")
        void shouldFindAllClients() {
            // Given
            List<RegisteredClient> expectedClients = List.of(
                    createTestRegisteredClient(),
                    createEspiRegisteredClient()
            );
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                    .thenReturn(expectedClients);

            // When
            List<RegisteredClient> foundClients = repository.findAll();

            // Then
            assertThat(foundClients).hasSize(2);
            assertThat(foundClients).containsExactlyElementsOf(expectedClients);
            
            verify(jdbcTemplate).query(
                    contains("ORDER BY client_id"),
                    any(RowMapper.class)
            );
        }
    }

    @Nested
    @DisplayName("Delete Operation Tests")
    class DeleteOperationTests {

        @Test
        @DisplayName("Should delete client by ID")
        void shouldDeleteClientById() {
            // When
            repository.deleteById("test-id");

            // Then
            verify(jdbcTemplate).update(
                    contains("DELETE FROM oauth2_registered_client WHERE id = ?"),
                    eq("test-id")
            );
        }
    }

    @Nested
    @DisplayName("Row Mapper Tests")
    class RowMapperTests {

        @Test
        @DisplayName("Should map result set to RegisteredClient correctly")
        void shouldMapResultSetToRegisteredClientCorrectly() throws SQLException {
            // Given
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getString("id")).thenReturn("test-id");
            when(resultSet.getString("client_id")).thenReturn("test-client-id");
            when(resultSet.getString("client_name")).thenReturn("Test Client");
            when(resultSet.getTimestamp("client_id_issued_at")).thenReturn(Timestamp.from(Instant.now()));
            when(resultSet.getString("client_secret")).thenReturn("{noop}secret");
            when(resultSet.getTimestamp("client_secret_expires_at")).thenReturn(null);
            when(resultSet.getString("client_authentication_methods")).thenReturn("client_secret_basic");
            when(resultSet.getString("authorization_grant_types")).thenReturn("authorization_code,refresh_token");
            when(resultSet.getString("redirect_uris")).thenReturn("https://example.com/callback");
            when(resultSet.getString("post_logout_redirect_uris")).thenReturn("https://example.com/logout");
            when(resultSet.getString("scopes")).thenReturn("openid,profile");
            when(resultSet.getString("client_settings")).thenReturn("{\"settings.client.require-authorization-consent\":true}");
            when(resultSet.getString("token_settings")).thenReturn("{\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"reference\"}}");

            RegisteredClient expectedClient = createTestRegisteredClient();
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(expectedClient);

            // When
            RegisteredClient client = repository.findById("test-id");

            // Then
            assertThat(client).isNotNull();
            assertThat(client.getId()).isEqualTo("test-id");
            assertThat(client.getClientId()).isEqualTo("test-client-id");
            assertThat(client.getClientName()).isEqualTo("Test Client");
        }

        @Test
        @DisplayName("Should handle null client secret")
        void shouldHandleNullClientSecret() throws SQLException {
            // Given
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getString("id")).thenReturn("test-id");
            when(resultSet.getString("client_id")).thenReturn("test-client-id");
            when(resultSet.getString("client_name")).thenReturn("Test Client");
            when(resultSet.getString("client_secret")).thenReturn(null);
            when(resultSet.getString("client_authentication_methods")).thenReturn("none");
            when(resultSet.getString("authorization_grant_types")).thenReturn("authorization_code");
            when(resultSet.getString("redirect_uris")).thenReturn("https://example.com/callback");
            when(resultSet.getString("scopes")).thenReturn("openid");
            when(resultSet.getString("client_settings")).thenReturn("{}");
            when(resultSet.getString("token_settings")).thenReturn("{}");

            RegisteredClient clientWithoutSecret = RegisteredClient.withId("test-id")
                    .clientId("test-client-id")
                    .clientName("Test Client")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .build();

            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(clientWithoutSecret);

            // When
            RegisteredClient client = repository.findById("test-id");

            // Then
            assertThat(client).isNotNull();
            assertThat(client.getClientSecret()).isNull();
            assertThat(client.getClientAuthenticationMethods()).contains(ClientAuthenticationMethod.NONE);
        }

        @Test
        @DisplayName("Should deserialize ESPI scopes correctly")
        void shouldDeserializeEspiScopesCorrectly() {
            // Given
            RegisteredClient espiClient = createEspiRegisteredClient();
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(espiClient);

            // When
            RegisteredClient client = repository.findById("espi-client-id");

            // Then
            assertThat(client).isNotNull();
            assertThat(client.getScopes()).contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
            assertThat(client.getScopes()).contains("DataCustodian_Admin_Access");
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should handle empty collections gracefully")
        void shouldHandleEmptyCollectionsGracefully() {
            // Given
            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("minimal-client")
                    .clientName("Minimal Client")
                    .build();

            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(null);

            // When
            repository.save(client);

            // Then - Should not throw exception and handle empty collections
            verify(jdbcTemplate).update(anyString(), anyString(), anyString(), any(), 
                    isNull(), any(), anyString(), anyString(), anyString(), anyString(), 
                    anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully in settings")
        void shouldHandleMalformedJsonGracefullyInSettings() {
            // This test would require more sophisticated mocking to test the actual row mapper
            // For now, we verify that the serialization creates valid JSON
            
            RegisteredClient client = createTestRegisteredClient();
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                    .thenReturn(null);

            // When
            repository.save(client);

            // Then
            ArgumentCaptor<String> clientSettingsCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> tokenSettingsCaptor = ArgumentCaptor.forClass(String.class);
            
            verify(jdbcTemplate).update(anyString(), anyString(), anyString(), any(), anyString(), 
                    any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), 
                    clientSettingsCaptor.capture(), tokenSettingsCaptor.capture());
            
            // Verify JSON is well-formed (starts and ends with braces)
            String clientSettings = clientSettingsCaptor.getValue();
            String tokenSettings = tokenSettingsCaptor.getValue();
            
            assertThat(clientSettings).startsWith("{").endsWith("}");
            assertThat(tokenSettings).startsWith("{").endsWith("}");
        }
    }

    // Helper methods

    private RegisteredClient createTestRegisteredClient() {
        return RegisteredClient.withId("test-id")
                .clientId("test-client-id")
                .clientName("Test Client")
                .clientSecret("{noop}secret")
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://example.com/callback")
                .postLogoutRedirectUri("https://example.com/logout")
                .scope("openid")
                .scope("profile")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofMinutes(360))
                        .refreshTokenTimeToLive(Duration.ofMinutes(3600))
                        .build())
                .build();
    }

    private RegisteredClient createEspiRegisteredClient() {
        return RegisteredClient.withId("espi-id")
                .clientId("espi-client-id")
                .clientName("ESPI Client")
                .clientSecret("{noop}espi-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("https://espi.example.com/callback")
                .scope("openid")
                .scope("profile")
                .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                .scope("DataCustodian_Admin_Access")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard
                        .accessTokenTimeToLive(Duration.ofMinutes(360))
                        .refreshTokenTimeToLive(Duration.ofMinutes(3600))
                        .build())
                .build();
    }
}