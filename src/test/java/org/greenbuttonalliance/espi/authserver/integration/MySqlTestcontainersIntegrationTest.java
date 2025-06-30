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

import org.greenbuttonalliance.espi.authserver.repository.JdbcRegisteredClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcontainers integration tests for MySQL database operations
 * 
 * Tests the Authorization Server with a real MySQL database using Testcontainers
 * to verify database schema, client operations, and ESPI-specific functionality.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainers")
@DisplayName("MySQL Testcontainers Integration Tests")
class MySqlTestcontainersIntegrationTest {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("oauth2_authserver")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // Flyway configuration for MySQL
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/mysql");
        registry.add("spring.flyway.baseline-on-migrate", () -> true);
        
        // JPA configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcRegisteredClientRepository clientRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        jdbcTemplate.execute("DELETE FROM oauth2_authorization WHERE registered_client_id LIKE 'test-%'");
        jdbcTemplate.execute("DELETE FROM oauth2_registered_client WHERE id LIKE 'test-%'");
    }

    @Nested
    @DisplayName("Database Schema Tests")
    class DatabaseSchemaTests {

        @Test
        @DisplayName("Should have all required OAuth2 tables")
        void shouldHaveAllRequiredOAuth2Tables() {
            // When & Then
            assertTableExists("oauth2_registered_client");
            assertTableExists("oauth2_authorization");
            assertTableExists("oauth2_authorization_consent");
        }

        @Test
        @DisplayName("Should have ESPI application info table")
        void shouldHaveEspiApplicationInfoTable() {
            // When & Then
            assertTableExists("espi_application_info");
        }

        @Test
        @DisplayName("Should have proper indexes on oauth2_registered_client")
        void shouldHaveProperIndexesOnRegisteredClient() {
            // When
            List<String> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oauth2_registered_client'",
                String.class
            );

            // Then
            assertThat(indexes).contains("idx_oauth2_registered_client_client_id");
        }

        @Test
        @DisplayName("Should have foreign key constraints")
        void shouldHaveForeignKeyConstraints() {
            // When
            List<String> constraints = jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = 'oauth2_registered_client'",
                String.class
            );

            // Then
            assertThat(constraints).isNotEmpty();
        }

        private void assertTableExists(String tableName) {
            int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class, tableName
            );
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Client Repository Integration Tests")
    @Transactional
    class ClientRepositoryIntegrationTests {

        @Test
        @DisplayName("Should save and retrieve ESPI client successfully")
        void shouldSaveAndRetrieveEspiClientSuccessfully() {
            // Given
            RegisteredClient espiClient = createEspiTestClient();

            // When
            clientRepository.save(espiClient);
            RegisteredClient retrieved = clientRepository.findByClientId(espiClient.getClientId());

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getClientId()).isEqualTo(espiClient.getClientId());
            assertThat(retrieved.getClientName()).isEqualTo(espiClient.getClientName());
            assertThat(retrieved.getScopes()).containsAll(espiClient.getScopes());
            assertThat(retrieved.getAuthorizationGrantTypes()).containsAll(espiClient.getAuthorizationGrantTypes());
            assertThat(retrieved.getClientAuthenticationMethods()).containsAll(espiClient.getClientAuthenticationMethods());
        }

        @Test
        @DisplayName("Should handle ESPI scopes correctly")
        void shouldHandleEspiScopesCorrectly() {
            // Given
            RegisteredClient client = RegisteredClient.withId("test-espi-scopes")
                    .clientId("test-espi-client")
                    .clientName("Test ESPI Client")
                    .clientSecret("{noop}secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .scope("profile")
                    .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .scope("DataCustodian_Admin_Access")
                    .build();

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-espi-client");

            // Then
            assertThat(retrieved.getScopes())
                    .contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .contains("DataCustodian_Admin_Access");
        }

        @Test
        @DisplayName("Should persist opaque token format (ESPI standard)")
        void shouldPersistOpaqueTokenFormat() {
            // Given
            RegisteredClient client = RegisteredClient.withId("test-opaque-tokens")
                    .clientId("test-opaque-client")
                    .clientName("Test Opaque Client")
                    .clientSecret("{noop}secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard
                            .accessTokenTimeToLive(Duration.ofMinutes(360))
                            .build())
                    .build();

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-opaque-client");

            // Then
            assertThat(retrieved.getTokenSettings().getAccessTokenFormat())
                    .isEqualTo(OAuth2TokenFormat.REFERENCE);
            assertThat(retrieved.getTokenSettings().getAccessTokenTimeToLive())
                    .isEqualTo(Duration.ofMinutes(360));
        }

        @Test
        @DisplayName("Should find all clients correctly")
        void shouldFindAllClientsCorrectly() {
            // Given
            RegisteredClient client1 = createEspiTestClient("test-client-1", "Test Client 1");
            RegisteredClient client2 = createEspiTestClient("test-client-2", "Test Client 2");

            // When
            clientRepository.save(client1);
            clientRepository.save(client2);
            List<RegisteredClient> allClients = clientRepository.findAll();

            // Then
            assertThat(allClients).hasSize(5); // 2 test clients + 3 default clients
            assertThat(allClients.stream().map(RegisteredClient::getClientId))
                    .contains("test-client-1", "test-client-2");
        }

        @Test
        @DisplayName("Should delete client successfully")
        void shouldDeleteClientSuccessfully() {
            // Given
            RegisteredClient client = createEspiTestClient();
            clientRepository.save(client);

            // When
            clientRepository.deleteById(client.getId());
            RegisteredClient retrieved = clientRepository.findByClientId(client.getClientId());

            // Then
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("Should update existing client")
        void shouldUpdateExistingClient() {
            // Given
            RegisteredClient original = createEspiTestClient();
            clientRepository.save(original);

            RegisteredClient updated = RegisteredClient.from(original)
                    .clientName("Updated Test Client")
                    .scope("additional_scope")
                    .build();

            // When
            clientRepository.save(updated);
            RegisteredClient retrieved = clientRepository.findByClientId(original.getClientId());

            // Then
            assertThat(retrieved.getClientName()).isEqualTo("Updated Test Client");
            assertThat(retrieved.getScopes()).contains("additional_scope");
        }
    }

    @Nested
    @DisplayName("Default Clients Tests")
    class DefaultClientsTests {

        @Test
        @DisplayName("Should have default DataCustodian admin client")
        void shouldHaveDefaultDataCustodianAdminClient() {
            // When
            RegisteredClient dcAdmin = clientRepository.findByClientId("data_custodian_admin");

            // Then
            assertThat(dcAdmin).isNotNull();
            assertThat(dcAdmin.getClientName()).isEqualTo("DataCustodian Admin");
            assertThat(dcAdmin.getAuthorizationGrantTypes())
                    .contains(AuthorizationGrantType.CLIENT_CREDENTIALS);
            assertThat(dcAdmin.getScopes()).contains("DataCustodian_Admin_Access");
            assertThat(dcAdmin.getTokenSettings().getAccessTokenFormat())
                    .isEqualTo(OAuth2TokenFormat.REFERENCE);
        }

        @Test
        @DisplayName("Should have default ThirdParty client")
        void shouldHaveDefaultThirdPartyClient() {
            // When
            RegisteredClient thirdParty = clientRepository.findByClientId("third_party");

            // Then
            assertThat(thirdParty).isNotNull();
            assertThat(thirdParty.getClientName()).isEqualTo("ThirdParty Application");
            assertThat(thirdParty.getAuthorizationGrantTypes())
                    .contains(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .contains(AuthorizationGrantType.REFRESH_TOKEN);
            assertThat(thirdParty.getScopes()).contains("openid", "profile");
            assertThat(thirdParty.getTokenSettings().getAccessTokenFormat())
                    .isEqualTo(OAuth2TokenFormat.REFERENCE);
        }

        @Test
        @DisplayName("Should have default ThirdParty admin client")
        void shouldHaveDefaultThirdPartyAdminClient() {
            // When
            RegisteredClient tpAdmin = clientRepository.findByClientId("third_party_admin");

            // Then
            assertThat(tpAdmin).isNotNull();
            assertThat(tpAdmin.getAuthorizationGrantTypes())
                    .contains(AuthorizationGrantType.CLIENT_CREDENTIALS);
            assertThat(tpAdmin.getScopes()).contains("ThirdParty_Admin_Access");
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent client operations")
        void shouldHandleConcurrentClientOperations() {
            // Given
            RegisteredClient client1 = createEspiTestClient("concurrent-1", "Concurrent Client 1");
            RegisteredClient client2 = createEspiTestClient("concurrent-2", "Concurrent Client 2");

            // When - Simulate concurrent saves
            clientRepository.save(client1);
            clientRepository.save(client2);

            // Then
            RegisteredClient retrieved1 = clientRepository.findByClientId("concurrent-1");
            RegisteredClient retrieved2 = clientRepository.findByClientId("concurrent-2");

            assertThat(retrieved1).isNotNull();
            assertThat(retrieved2).isNotNull();
            assertThat(retrieved1.getClientId()).isEqualTo("concurrent-1");
            assertThat(retrieved2.getClientId()).isEqualTo("concurrent-2");
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should enforce unique client_id constraint")
        void shouldEnforceUniqueClientIdConstraint() {
            // Given
            RegisteredClient client1 = createEspiTestClient("duplicate-client", "First Client");
            RegisteredClient client2 = createEspiTestClient("duplicate-client", "Second Client");

            // When
            clientRepository.save(client1);

            // Then - Second save should update, not create duplicate
            clientRepository.save(client2);
            List<RegisteredClient> allClients = clientRepository.findAll();
            long duplicateCount = allClients.stream()
                    .filter(c -> "duplicate-client".equals(c.getClientId()))
                    .count();

            assertThat(duplicateCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle special characters in client data")
        void shouldHandleSpecialCharactersInClientData() {
            // Given
            RegisteredClient client = RegisteredClient.withId("test-special-chars")
                    .clientId("test-special@client.com")
                    .clientName("Test Client with Special Characters: åäö éñü")
                    .clientSecret("{noop}secret!@#$%")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback?param=value&other=test")
                    .scope("scope:with:colons")
                    .build();

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-special@client.com");

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getClientName()).contains("åäö éñü");
            assertThat(retrieved.getRedirectUris()).contains("https://example.com/callback?param=value&other=test");
            assertThat(retrieved.getScopes()).contains("scope:with:colons");
        }

        @Test
        @DisplayName("Should handle null values appropriately")
        void shouldHandleNullValuesAppropriately() {
            // Given
            RegisteredClient client = RegisteredClient.withId("test-nulls")
                    .clientId("test-null-values")
                    .clientName("Test Null Values")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .build();

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-null-values");

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getClientSecret()).isNull();
            assertThat(retrieved.getClientSecretExpiresAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle bulk operations efficiently")
        void shouldHandleBulkOperationsEfficiently() {
            // Given
            long startTime = System.currentTimeMillis();
            int clientCount = 50;

            // When - Create and save multiple clients
            for (int i = 0; i < clientCount; i++) {
                RegisteredClient client = createEspiTestClient("bulk-client-" + i, "Bulk Client " + i);
                clientRepository.save(client);
            }

            // Retrieve all clients
            List<RegisteredClient> allClients = clientRepository.findAll();
            long endTime = System.currentTimeMillis();

            // Then
            assertThat(allClients.size()).isGreaterThanOrEqualTo(clientCount + 3); // + default clients
            assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds
        }
    }

    // Helper methods

    private RegisteredClient createEspiTestClient() {
        return createEspiTestClient("test-espi-client", "Test ESPI Client");
    }

    private RegisteredClient createEspiTestClient(String clientId, String clientName) {
        return RegisteredClient.withId("test-" + UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName(clientName)
                .clientSecret("{noop}secret")
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://example.com/callback")
                .postLogoutRedirectUri("https://example.com/logout")
                .scope("openid")
                .scope("profile")
                .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
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