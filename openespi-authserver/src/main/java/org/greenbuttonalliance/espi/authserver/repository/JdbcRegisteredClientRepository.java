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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 * JDBC implementation of RegisteredClientRepository for dynamic client registration
 * 
 * Provides database-backed storage for OAuth2 client registrations with support for:
 * - OIDC Dynamic Client Registration
 * - ESPI-specific client registration
 * - Client CRUD operations
 * - Database persistence
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Repository
public class JdbcRegisteredClientRepository implements RegisteredClientRepository {

    private static final Logger logger = LoggerFactory.getLogger(JdbcRegisteredClientRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // SQL Queries
    private static final String SELECT_CLIENT_SQL = """
        SELECT id, client_id, client_id_issued_at, client_secret, client_secret_expires_at,
               client_name, client_authentication_methods, authorization_grant_types,
               redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings
        FROM oauth2_registered_client
        WHERE %s = ?
        """;

    private static final String INSERT_CLIENT_SQL = """
        INSERT INTO oauth2_registered_client 
        (id, client_id, client_id_issued_at, client_secret, client_secret_expires_at,
         client_name, client_authentication_methods, authorization_grant_types,
         redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_CLIENT_SQL = """
        UPDATE oauth2_registered_client 
        SET client_secret = ?, client_secret_expires_at = ?, client_name = ?,
            client_authentication_methods = ?, authorization_grant_types = ?,
            redirect_uris = ?, post_logout_redirect_uris = ?, scopes = ?,
            client_settings = ?, token_settings = ?
        WHERE id = ?
        """;

    private static final String DELETE_CLIENT_SQL = """
        DELETE FROM oauth2_registered_client WHERE id = ?
        """;

    public JdbcRegisteredClientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        logger.debug("Saving registered client: {}", registeredClient.getClientId());
        
        RegisteredClient existingClient = findById(registeredClient.getId());
        if (existingClient != null) {
            updateClient(registeredClient);
        } else {
            insertClient(registeredClient);
        }
    }

    @Override
    public RegisteredClient findById(String id) {
        try {
            return jdbcTemplate.queryForObject(
                String.format(SELECT_CLIENT_SQL, "id"), 
                new RegisteredClientRowMapper(), 
                id
            );
        } catch (EmptyResultDataAccessException e) {
            logger.debug("Client not found by id: {}", id);
            return null;
        }
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        try {
            return jdbcTemplate.queryForObject(
                String.format(SELECT_CLIENT_SQL, "client_id"), 
                new RegisteredClientRowMapper(), 
                clientId
            );
        } catch (EmptyResultDataAccessException e) {
            logger.debug("Client not found by client_id: {}", clientId);
            return null;
        }
    }

    /**
     * Custom method to find all clients (not in standard interface)
     */
    public List<RegisteredClient> findAll() {
        String sql = """
            SELECT id, client_id, client_id_issued_at, client_secret, client_secret_expires_at,
                   client_name, client_authentication_methods, authorization_grant_types,
                   redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings
            FROM oauth2_registered_client
            ORDER BY client_id
            """;
        return jdbcTemplate.query(sql, new RegisteredClientRowMapper());
    }

    /**
     * Custom method to delete a client (not in standard interface)
     */
    public void deleteById(String id) {
        logger.debug("Deleting registered client: {}", id);
        jdbcTemplate.update(DELETE_CLIENT_SQL, id);
    }

    private void insertClient(RegisteredClient client) {
        jdbcTemplate.update(INSERT_CLIENT_SQL,
            client.getId(),
            client.getClientId(),
            client.getClientIdIssuedAt(),
            client.getClientSecret(),
            client.getClientSecretExpiresAt(),
            client.getClientName(),
            serializeClientAuthenticationMethods(client.getClientAuthenticationMethods()),
            serializeAuthorizationGrantTypes(client.getAuthorizationGrantTypes()),
            serializeRedirectUris(client.getRedirectUris()),
            serializeRedirectUris(client.getPostLogoutRedirectUris()),
            serializeScopes(client.getScopes()),
            serializeClientSettings(client.getClientSettings()),
            serializeTokenSettings(client.getTokenSettings())
        );
    }

    private void updateClient(RegisteredClient client) {
        jdbcTemplate.update(UPDATE_CLIENT_SQL,
            client.getClientSecret(),
            client.getClientSecretExpiresAt(),
            client.getClientName(),
            serializeClientAuthenticationMethods(client.getClientAuthenticationMethods()),
            serializeAuthorizationGrantTypes(client.getAuthorizationGrantTypes()),
            serializeRedirectUris(client.getRedirectUris()),
            serializeRedirectUris(client.getPostLogoutRedirectUris()),
            serializeScopes(client.getScopes()),
            serializeClientSettings(client.getClientSettings()),
            serializeTokenSettings(client.getTokenSettings()),
            client.getId()
        );
    }

    // Serialization methods
    private String serializeClientAuthenticationMethods(Set<ClientAuthenticationMethod> methods) {
        return methods.stream()
                .map(ClientAuthenticationMethod::getValue)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private String serializeAuthorizationGrantTypes(Set<AuthorizationGrantType> grantTypes) {
        return grantTypes.stream()
                .map(AuthorizationGrantType::getValue)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private String serializeRedirectUris(Set<String> uris) {
        return String.join(",", uris);
    }

    private String serializeScopes(Set<String> scopes) {
        return String.join(",", scopes);
    }

    private String serializeClientSettings(ClientSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings.getSettings());
        } catch (Exception e) {
            logger.error("Error serializing client settings", e);
            return "{}";
        }
    }

    private String serializeTokenSettings(TokenSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings.getSettings());
        } catch (Exception e) {
            logger.error("Error serializing token settings", e);
            return "{}";
        }
    }

    /**
     * Row mapper for RegisteredClient
     */
    private class RegisteredClientRowMapper implements RowMapper<RegisteredClient> {
        @Override
        public RegisteredClient mapRow(ResultSet rs, int rowNum) throws SQLException {
            RegisteredClient.Builder builder = RegisteredClient.withId(rs.getString("id"))
                    .clientId(rs.getString("client_id"))
                    .clientName(rs.getString("client_name"));

            // Client ID issued at
            Instant clientIdIssuedAt = rs.getTimestamp("client_id_issued_at") != null ?
                    rs.getTimestamp("client_id_issued_at").toInstant() : null;
            if (clientIdIssuedAt != null) {
                builder.clientIdIssuedAt(clientIdIssuedAt);
            }

            // Client secret
            String clientSecret = rs.getString("client_secret");
            if (StringUtils.hasText(clientSecret)) {
                builder.clientSecret(clientSecret);
            }

            // Client secret expires at
            Instant clientSecretExpiresAt = rs.getTimestamp("client_secret_expires_at") != null ?
                    rs.getTimestamp("client_secret_expires_at").toInstant() : null;
            if (clientSecretExpiresAt != null) {
                builder.clientSecretExpiresAt(clientSecretExpiresAt);
            }

            // Authentication methods
            String authMethods = rs.getString("client_authentication_methods");
            if (StringUtils.hasText(authMethods)) {
                Arrays.stream(authMethods.split(","))
                        .forEach(method -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method.trim())));
            }

            // Grant types
            String grantTypes = rs.getString("authorization_grant_types");
            if (StringUtils.hasText(grantTypes)) {
                Arrays.stream(grantTypes.split(","))
                        .forEach(grantType -> builder.authorizationGrantType(new AuthorizationGrantType(grantType.trim())));
            }

            // Redirect URIs
            String redirectUris = rs.getString("redirect_uris");
            if (StringUtils.hasText(redirectUris)) {
                Arrays.stream(redirectUris.split(","))
                        .forEach(uri -> builder.redirectUri(uri.trim()));
            }

            // Post logout redirect URIs
            String postLogoutRedirectUris = rs.getString("post_logout_redirect_uris");
            if (StringUtils.hasText(postLogoutRedirectUris)) {
                Arrays.stream(postLogoutRedirectUris.split(","))
                        .forEach(uri -> builder.postLogoutRedirectUri(uri.trim()));
            }

            // Scopes
            String scopes = rs.getString("scopes");
            if (StringUtils.hasText(scopes)) {
                Arrays.stream(scopes.split(","))
                        .forEach(scope -> builder.scope(scope.trim()));
            }

            // Client settings
            String clientSettings = rs.getString("client_settings");
            if (StringUtils.hasText(clientSettings)) {
                try {
                    Map<String, Object> settings = objectMapper.readValue(clientSettings, new TypeReference<Map<String, Object>>() {});
                    builder.clientSettings(ClientSettings.withSettings(settings).build());
                } catch (Exception e) {
                    logger.warn("Error deserializing client settings for client {}", rs.getString("client_id"), e);
                }
            }

            // Token settings
            String tokenSettings = rs.getString("token_settings");
            if (StringUtils.hasText(tokenSettings)) {
                try {
                    Map<String, Object> settings = objectMapper.readValue(tokenSettings, new TypeReference<Map<String, Object>>() {});
                    builder.tokenSettings(TokenSettings.withSettings(settings).build());
                } catch (Exception e) {
                    logger.warn("Error deserializing token settings for client {}", rs.getString("client_id"), e);
                }
            }

            return builder.build();
        }
    }
}