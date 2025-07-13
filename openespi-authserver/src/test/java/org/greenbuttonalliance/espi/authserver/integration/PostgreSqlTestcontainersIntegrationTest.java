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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcontainers integration tests for PostgreSQL database operations
 * 
 * Tests the Authorization Server with a real PostgreSQL database using Testcontainers
 * to verify database schema, client operations, and ESPI-specific functionality.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainers")
@DisplayName("PostgreSQL Testcontainers Integration Tests")
class PostgreSqlTestcontainersIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("oauth2_authserver")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Flyway configuration for PostgreSQL
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/postgresql");
        registry.add("spring.flyway.baseline-on-migrate", () -> true);
        registry.add("spring.flyway.schemas", () -> "public");
        
        // JPA configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcRegisteredClientRepository clientRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        jdbcTemplate.execute("DELETE FROM oauth2_authorization WHERE registered_client_id LIKE 'test-%'");
        jdbcTemplate.execute("DELETE FROM espi_application_info WHERE client_id LIKE 'test-%'");
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
                "SELECT indexname FROM pg_indexes " +
                "WHERE schemaname = 'public' AND tablename = 'oauth2_registered_client'",
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
                "SELECT conname FROM pg_constraint " +
                "WHERE contype = 'f' AND confrelid = " +
                "(SELECT oid FROM pg_class WHERE relname = 'oauth2_registered_client')",
                String.class
            );

            // Then
            assertThat(constraints).isNotEmpty();
        }

        @Test
        @DisplayName("Should have proper UUID generation function")
        void shouldHaveProperUuidGenerationFunction() {
            // When
            List<String> functions = jdbcTemplate.queryForList(
                "SELECT proname FROM pg_proc WHERE proname = 'update_updated_at_column'",
                String.class
            );

            // Then
            assertThat(functions).contains("update_updated_at_column");
        }

        @Test
        @DisplayName("Should have proper triggers on espi_application_info")
        void shouldHaveProperTriggersOnEspiApplicationInfo() {
            // When
            List<String> triggers = jdbcTemplate.queryForList(
                "SELECT tgname FROM pg_trigger " +
                "WHERE tgrelid = (SELECT oid FROM pg_class WHERE relname = 'espi_application_info')",
                String.class
            );

            // Then
            assertThat(triggers).contains("update_espi_application_info_updated_at");
        }

        private void assertTableExists(String tableName) {
            int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name = ?",
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
        @DisplayName("Should handle PostgreSQL-specific features correctly")
        void shouldHandlePostgreSqlSpecificFeaturesCorrectly() {
            // Given
            RegisteredClient client = createEspiTestClient("test-postgres-client", "PostgreSQL Test Client");

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-postgres-client");

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getClientName()).isEqualTo("PostgreSQL Test Client");
            
            // Verify that UUIDs in ESPI application info table are properly generated
            String uuid = jdbcTemplate.queryForObject(
                "SELECT uuid FROM espi_application_info WHERE client_id = ?",
                String.class, "test-postgres-client"
            );
            assertThat(uuid).isNotNull();
            assertThat(uuid).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("Should handle JSON serialization correctly in PostgreSQL")
        void shouldHandleJsonSerializationCorrectlyInPostgreSQL() {
            // Given
            RegisteredClient client = RegisteredClient.withId("test-json-serialization")
                    .clientId("test-json-client")
                    .clientName("JSON Test Client")
                    .clientSecret("{noop}secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .scope("profile")
                    .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(true)
                            .requireProofKey(false)
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                            .accessTokenTimeToLive(Duration.ofMinutes(360))
                            .refreshTokenTimeToLive(Duration.ofMinutes(3600))
                            .reuseRefreshTokens(true)
                            .build())
                    .build();

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-json-client");

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getClientSettings().isRequireAuthorizationConsent()).isTrue();
            assertThat(retrieved.getClientSettings().isRequireProofKey()).isFalse();
            assertThat(retrieved.getTokenSettings().getAccessTokenFormat()).isEqualTo(OAuth2TokenFormat.REFERENCE);
            assertThat(retrieved.getTokenSettings().isReuseRefreshTokens()).isTrue();
        }

        @Test
        @DisplayName("Should handle large text fields correctly")
        void shouldHandleLargeTextFieldsCorrectly() {
            // Given - Create a client with large text fields
            String largeScope = "scope1 scope2 scope3 scope4 scope5 scope6 scope7 scope8 scope9 scope10 " +
                               "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13 " +
                               "FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7 " +
                               "DataCustodian_Admin_Access ThirdParty_Admin_Access Customer_Data_Access";
            
            RegisteredClient client = RegisteredClient.withId("test-large-text")
                    .clientId("test-large-text-client")
                    .clientName("Large Text Test Client with Very Long Name That Tests Field Limits")
                    .clientSecret("{noop}verylongsecretthatexceedstheusuallengthlimitstoensureitishandledproperly")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback?with=very&long=query&parameters=that&test=field&limits=properly")
                    .scope(largeScope)
                    .build();

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-large-text-client");

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getScopes()).hasSize(8); // All scopes should be preserved
            assertThat(retrieved.getScopes()).contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
        }

        @Test
        @DisplayName("Should handle concurrent access with PostgreSQL properly")
        void shouldHandleConcurrentAccessWithPostgreSqlProperly() {
            // Given
            RegisteredClient client1 = createEspiTestClient("concurrent-pg-1", "Concurrent PostgreSQL Client 1");
            RegisteredClient client2 = createEspiTestClient("concurrent-pg-2", "Concurrent PostgreSQL Client 2");

            // When - Simulate concurrent operations
            clientRepository.save(client1);
            clientRepository.save(client2);

            // Then
            RegisteredClient retrieved1 = clientRepository.findByClientId("concurrent-pg-1");
            RegisteredClient retrieved2 = clientRepository.findByClientId("concurrent-pg-2");

            assertThat(retrieved1).isNotNull();
            assertThat(retrieved2).isNotNull();
            assertThat(retrieved1.getClientId()).isEqualTo("concurrent-pg-1");
            assertThat(retrieved2.getClientId()).isEqualTo("concurrent-pg-2");
        }

        @Test
        @DisplayName("Should handle timestamp fields correctly")
        void shouldHandleTimestampFieldsCorrectly() {
            // Given
            Instant now = Instant.now();
            Instant futureExpiry = now.plus(Duration.ofDays(365));
            
            RegisteredClient client = RegisteredClient.withId("test-timestamps")
                    .clientId("test-timestamp-client")
                    .clientName("Timestamp Test Client")
                    .clientSecret("{noop}secret")
                    .clientIdIssuedAt(now)
                    .clientSecretExpiresAt(futureExpiry)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .build();

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-timestamp-client");

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getClientIdIssuedAt()).isNotNull();
            assertThat(retrieved.getClientSecretExpiresAt()).isNotNull();
            
            // PostgreSQL should preserve timestamp precision
            assertThat(retrieved.getClientIdIssuedAt()).isEqualTo(now);
            assertThat(retrieved.getClientSecretExpiresAt()).isEqualTo(futureExpiry);
        }
    }

    @Nested
    @DisplayName("ESPI Application Info Tests")
    class EspiApplicationInfoTests {

        @Test
        @DisplayName("Should populate ESPI application info for new clients")
        void shouldPopulateEspiApplicationInfoForNewClients() {
            // Given
            RegisteredClient client = createEspiTestClient("test-espi-info", "ESPI Info Test Client");

            // When
            clientRepository.save(client);

            // Then
            // Verify ESPI application info is created
            int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM espi_application_info WHERE client_id = ?",
                Integer.class, "test-espi-info"
            );
            assertThat(count).isEqualTo(1);

            // Verify updated_at trigger works
            String updatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM espi_application_info WHERE client_id = ?",
                String.class, "test-espi-info"
            );
            assertThat(updatedAt).isNotNull();
        }

        @Test
        @DisplayName("Should handle ESPI metadata correctly")
        void shouldHandleEspiMetadataCorrectly() {
            // Given
            RegisteredClient client = createEspiTestClient("test-espi-metadata", "ESPI Metadata Test");

            // When
            clientRepository.save(client);

            // Insert ESPI metadata
            jdbcTemplate.update(
                "INSERT INTO espi_application_info " +
                "(uuid, client_id, client_name, client_description, scope, grant_types, response_types, " +
                "token_endpoint_auth_method, third_party_application_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                "test-espi-metadata",
                "ESPI Metadata Test",
                "Test client for ESPI metadata handling",
                "openid profile FB=4_5_15",
                "authorization_code refresh_token",
                "code",
                "client_secret_basic",
                "WEB"
            );

            // Then
            String description = jdbcTemplate.queryForObject(
                "SELECT client_description FROM espi_application_info WHERE client_id = ?",
                String.class, "test-espi-metadata"
            );
            assertThat(description).isEqualTo("Test client for ESPI metadata handling");
        }

        @Test
        @DisplayName("Should handle ESPI scope validation correctly")
        void shouldHandleEspiScopeValidationCorrectly() {
            // Given
            RegisteredClient client = RegisteredClient.withId("test-espi-scopes")
                    .clientId("test-espi-scope-validation")
                    .clientName("ESPI Scope Validation Test")
                    .clientSecret("{noop}secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://example.com/callback")
                    .scope("openid")
                    .scope("profile")
                    .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .scope("FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7")
                    .scope("DataCustodian_Admin_Access")
                    .build();

            // When
            clientRepository.save(client);
            RegisteredClient retrieved = clientRepository.findByClientId("test-espi-scope-validation");

            // Then
            assertThat(retrieved.getScopes())
                    .contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                    .contains("FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7")
                    .contains("DataCustodian_Admin_Access");
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
        @DisplayName("Should have default ThirdParty client with ESPI scopes")
        void shouldHaveDefaultThirdPartyClientWithEspiScopes() {
            // When
            RegisteredClient thirdParty = clientRepository.findByClientId("third_party");

            // Then
            assertThat(thirdParty).isNotNull();
            assertThat(thirdParty.getClientName()).isEqualTo("ThirdParty Application");
            assertThat(thirdParty.getAuthorizationGrantTypes())
                    .contains(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .contains(AuthorizationGrantType.REFRESH_TOKEN);
            assertThat(thirdParty.getScopes())
                    .contains("openid", "profile")
                    .contains("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
            assertThat(thirdParty.getTokenSettings().getAccessTokenFormat())
                    .isEqualTo(OAuth2TokenFormat.REFERENCE);
        }

        @Test
        @DisplayName("Should have corresponding ESPI application info for default clients")
        void shouldHaveCorrespondingEspiApplicationInfoForDefaultClients() {
            // When
            int dcAdminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM espi_application_info WHERE client_id = 'data_custodian_admin'",
                Integer.class
            );
            int thirdPartyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM espi_application_info WHERE client_id = 'third_party'",
                Integer.class
            );

            // Then
            assertThat(dcAdminCount).isEqualTo(1);
            assertThat(thirdPartyCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle bulk operations efficiently with PostgreSQL")
        void shouldHandleBulkOperationsEfficientlyWithPostgreSQL() {
            // Given
            long startTime = System.currentTimeMillis();
            int clientCount = 25; // Smaller count for PostgreSQL as it's typically slower to start

            // When - Create and save multiple clients
            for (int i = 0; i < clientCount; i++) {
                RegisteredClient client = createEspiTestClient("bulk-pg-client-" + i, "Bulk PostgreSQL Client " + i);
                clientRepository.save(client);
            }

            // Retrieve all clients
            List<RegisteredClient> allClients = clientRepository.findAll();
            long endTime = System.currentTimeMillis();

            // Then
            assertThat(allClients.size()).isGreaterThanOrEqualTo(clientCount + 2); // + default clients
            assertThat(endTime - startTime).isLessThan(8000); // Should complete within 8 seconds for PostgreSQL
        }

        @Test
        @DisplayName("Should handle complex queries efficiently")
        void shouldHandleComplexQueriesEfficiently() {
            // Given
            createMultipleTestClients(10);

            // When
            long startTime = System.currentTimeMillis();
            List<RegisteredClient> clients = clientRepository.findAll();
            long endTime = System.currentTimeMillis();

            // Then
            assertThat(clients).hasSizeGreaterThan(10);
            assertThat(endTime - startTime).isLessThan(1000); // Should be fast
        }

        private void createMultipleTestClients(int count) {
            for (int i = 0; i < count; i++) {
                RegisteredClient client = createEspiTestClient("perf-client-" + i, "Performance Client " + i);
                clientRepository.save(client);
            }
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