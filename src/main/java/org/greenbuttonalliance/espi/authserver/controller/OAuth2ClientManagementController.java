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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.greenbuttonalliance.espi.authserver.service.ClientMetricsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.greenbuttonalliance.espi.authserver.dto.ClientManagementDTOs.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth2 Client Management Controller for ESPI 4.0
 * 
 * Provides comprehensive CRUD operations for OAuth2 client management with ESPI 4.0 compliance:
 * - RESTful client management (GET, POST, PUT, DELETE)
 * - Bulk operations for enterprise deployments
 * - Client metrics and usage analytics
 * - NAESB ESPI 4.0 specific features
 * - Security level management and certificate handling
 * - Rate limiting and session management
 * 
 * Endpoints:
 * - GET /api/v1/oauth2/clients: List clients with pagination and filtering
 * - POST /api/v1/oauth2/clients: Create new client
 * - GET /api/v1/oauth2/clients/{clientId}: Get client details
 * - PUT /api/v1/oauth2/clients/{clientId}: Update client
 * - DELETE /api/v1/oauth2/clients/{clientId}: Delete client
 * - PUT /api/v1/oauth2/clients/{clientId}/status: Update client status
 * - GET /api/v1/oauth2/clients/{clientId}/metrics: Get client metrics
 * - POST /api/v1/oauth2/clients/bulk: Bulk operations
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@RestController
@RequestMapping("/api/v1/oauth2/clients")
@PreAuthorize("hasRole('ADMIN') or hasRole('DC_ADMIN')")
@Validated
public class OAuth2ClientManagementController {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientManagementController.class);

    private final RegisteredClientRepository registeredClientRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ClientMetricsService clientMetricsService;
    private final SecureRandom secureRandom = new SecureRandom();

    // ESPI 4.0 compliant scopes
    private static final Set<String> ESPI_SCOPES = Set.of(
        "openid", "profile", "email",
        "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
        "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13",
        "FB=4_5_15;IntervalDuration=3600;BlockDuration=daily;HistoryLength=7",
        "FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7",
        "DataCustodian_Admin_Access",
        "Upload_Admin_Access", 
        "ThirdParty_Admin_Access"
    );

    @Autowired
    public OAuth2ClientManagementController(
            RegisteredClientRepository registeredClientRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            ClientMetricsService clientMetricsService) {
        this.registeredClientRepository = registeredClientRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.clientMetricsService = clientMetricsService;
    }

    /**
     * List OAuth2 clients with pagination, filtering, and sorting
     * 
     * GET /api/v1/oauth2/clients
     */
    @GetMapping
    public ResponseEntity<PagedClientResponse> listClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "client_id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String securityLevel,
            @RequestParam(required = false) String espiVersion,
            @RequestParam(required = false) String search) {
        
        logger.debug("Listing clients - page: {}, size: {}, status: {}, search: {}", 
                    page, size, status, search);

        try {
            // Build SQL query with filters
            StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.client_id, c.client_name, c.client_authentication_methods,
                       c.authorization_grant_types, c.redirect_uris, c.scopes,
                       c.espi_version, c.security_level, c.client_status, c.rate_limit_per_minute,
                       c.max_concurrent_sessions, c.created_by, c.last_used_at, c.failure_count,
                       e.business_category, e.certification_status, e.service_territory
                FROM oauth2_registered_client c
                LEFT JOIN espi_application_info e ON c.client_id = e.client_id
                WHERE 1=1
                """);

            List<Object> params = new ArrayList<>();
            
            // Add filters
            if (StringUtils.hasText(status)) {
                sql.append(" AND c.client_status = ?");
                params.add(status);
            }
            
            if (StringUtils.hasText(securityLevel)) {
                sql.append(" AND c.security_level = ?");
                params.add(securityLevel);
            }
            
            if (StringUtils.hasText(espiVersion)) {
                sql.append(" AND c.espi_version = ?");
                params.add(espiVersion);
            }
            
            if (StringUtils.hasText(search)) {
                sql.append(" AND (c.client_name LIKE ? OR c.client_id LIKE ?)");
                params.add("%" + search + "%");
                params.add("%" + search + "%");
            }

            // Add sorting
            String direction = "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
            sql.append(" ORDER BY c.").append(sortBy).append(" ").append(direction);

            // Add pagination
            sql.append(" LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            // Execute query
            List<ClientDetails> clients = jdbcTemplate.query(sql.toString(),
                params.toArray(),
                (rs, rowNum) -> {
                    ClientDetails client = new ClientDetails();
                    client.setId(rs.getString("id"));
                    client.setClientId(rs.getString("client_id"));
                    client.setClientName(rs.getString("client_name"));
                    client.setAuthenticationMethods(Arrays.asList(
                        rs.getString("client_authentication_methods").split(",")));
                    client.setGrantTypes(Arrays.asList(
                        rs.getString("authorization_grant_types").split(",")));
                    client.setRedirectUris(StringUtils.hasText(rs.getString("redirect_uris")) ?
                        Arrays.asList(rs.getString("redirect_uris").split(",")) : new ArrayList<>());
                    client.setScopes(Arrays.asList(rs.getString("scopes").split(",")));
                    client.setEspiVersion(rs.getString("espi_version"));
                    client.setSecurityLevel(rs.getString("security_level"));
                    client.setStatus(rs.getString("client_status"));
                    client.setRateLimitPerMinute(rs.getInt("rate_limit_per_minute"));
                    client.setMaxConcurrentSessions(rs.getInt("max_concurrent_sessions"));
                    client.setCreatedBy(rs.getString("created_by"));
                    client.setLastUsedAt(rs.getTimestamp("last_used_at") != null ?
                        rs.getTimestamp("last_used_at").toInstant() : null);
                    client.setFailureCount(rs.getInt("failure_count"));
                    client.setBusinessCategory(rs.getString("business_category"));
                    client.setCertificationStatus(rs.getString("certification_status"));
                    client.setServiceTerritory(rs.getString("service_territory"));
                    return client;
                });

            // Get total count for pagination
            String countSql = sql.toString()
                .replaceFirst("SELECT.*FROM", "SELECT COUNT(*) FROM")
                .replaceFirst("ORDER BY.*", "");
            Long totalCount = jdbcTemplate.queryForObject(countSql, 
                params.subList(0, params.size() - 2).toArray(), Long.class);

            PagedClientResponse response = new PagedClientResponse();
            response.setClients(clients);
            response.setPage(page);
            response.setSize(size);
            response.setTotalElements(totalCount != null ? totalCount : 0);
            response.setTotalPages((int) Math.ceil((double) (totalCount != null ? totalCount : 0) / size));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error listing clients", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create new OAuth2 client with ESPI 4.0 compliance
     * 
     * POST /api/v1/oauth2/clients
     */
    @PostMapping
    public ResponseEntity<?> createClient(@Valid @RequestBody CreateClientRequest request) {
        logger.info("Creating new client: {}", request.getClientName());

        try {
            // Validate ESPI compliance
            validateEspiCompliance(request);

            // Generate client credentials
            String clientId = generateClientId(request.getClientNamePrefix());
            String clientSecret = generateClientSecret();

            // Build RegisteredClient
            RegisteredClient.Builder clientBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode(clientSecret))
                    .clientName(request.getClientName())
                    .clientIdIssuedAt(Instant.now());

            // Configure authentication methods
            if (request.getAuthenticationMethods() != null) {
                for (String method : request.getAuthenticationMethods()) {
                    clientBuilder.clientAuthenticationMethod(new ClientAuthenticationMethod(method));
                }
            } else {
                clientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
            }

            // Configure grant types
            if (request.getGrantTypes() != null) {
                for (String grantType : request.getGrantTypes()) {
                    clientBuilder.authorizationGrantType(new AuthorizationGrantType(grantType));
                }
            } else {
                clientBuilder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                clientBuilder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
            }

            // Configure redirect URIs
            if (request.getRedirectUris() != null) {
                for (String uri : request.getRedirectUris()) {
                    clientBuilder.redirectUri(uri);
                }
            }

            if (request.getPostLogoutRedirectUris() != null) {
                for (String uri : request.getPostLogoutRedirectUris()) {
                    clientBuilder.postLogoutRedirectUri(uri);
                }
            }

            // Configure scopes
            if (request.getScopes() != null) {
                for (String scope : request.getScopes()) {
                    clientBuilder.scope(scope);
                }
            } else {
                // Default ESPI scopes
                clientBuilder.scope("openid")
                           .scope("profile")
                           .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
            }

            // Configure client settings
            ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder()
                    .requireProofKey(request.isRequireProofKey())
                    .requireAuthorizationConsent(request.isRequireAuthorizationConsent());
            clientBuilder.clientSettings(clientSettingsBuilder.build());

            // Configure token settings
            TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                    .accessTokenTimeToLive(Duration.ofHours(6))
                    .refreshTokenTimeToLive(Duration.ofHours(60))
                    .reuseRefreshTokens(false);
            clientBuilder.tokenSettings(tokenSettingsBuilder.build());

            RegisteredClient registeredClient = clientBuilder.build();

            // Save the client
            registeredClientRepository.save(registeredClient);

            // Insert ESPI-specific data
            insertEspiApplicationInfo(registeredClient, request);

            // Insert ESPI 4.0 compliance data
            insertEspi40ComplianceData(registeredClient, request);

            logger.info("Successfully created client: {} with client_id: {}", request.getClientName(), clientId);

            // Build response
            CreateClientResponse response = new CreateClientResponse();
            response.setClientId(clientId);
            response.setClientSecret(clientSecret);
            response.setClientName(request.getClientName());
            response.setCreatedAt(Instant.now());
            response.setEspiVersion("4.0");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Client creation validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", e.getMessage()));
        } catch (Exception e) {
            logger.error("Client creation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("server_error", "Internal server error"));
        }
    }

    /**
     * Get client details by client ID
     * 
     * GET /api/v1/oauth2/clients/{clientId}
     */
    @GetMapping("/{clientId}")
    public ResponseEntity<?> getClient(@PathVariable @NotBlank String clientId) {
        logger.debug("Getting client details for: {}", clientId);

        try {
            RegisteredClient client = registeredClientRepository.findByClientId(clientId);
            if (client == null) {
                return ResponseEntity.notFound().build();
            }

            // Get ESPI application info
            String espiSql = """
                SELECT * FROM espi_application_info WHERE client_id = ?
                """;
            
            Map<String, Object> espiInfo = null;
            try {
                espiInfo = jdbcTemplate.queryForMap(espiSql, clientId);
            } catch (Exception e) {
                logger.debug("No ESPI application info found for client: {}", clientId);
            }

            // Get ESPI 4.0 compliance data
            String complianceSql = """
                SELECT espi_version, security_level, client_status, rate_limit_per_minute,
                       max_concurrent_sessions, last_used_at, failure_count
                FROM oauth2_registered_client WHERE client_id = ?
                """;
            
            Map<String, Object> complianceData = jdbcTemplate.queryForMap(complianceSql, clientId);

            // Build detailed response
            ClientDetails details = mapToClientDetails(client, espiInfo, complianceData);
            
            return ResponseEntity.ok(details);

        } catch (Exception e) {
            logger.error("Error getting client details", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("server_error", "Failed to retrieve client"));
        }
    }

    /**
     * Update client configuration
     * 
     * PUT /api/v1/oauth2/clients/{clientId}
     */
    @PutMapping("/{clientId}")
    public ResponseEntity<?> updateClient(
            @PathVariable @NotBlank String clientId,
            @Valid @RequestBody UpdateClientRequest request) {
        
        logger.info("Updating client: {}", clientId);

        try {
            RegisteredClient existingClient = registeredClientRepository.findByClientId(clientId);
            if (existingClient == null) {
                return ResponseEntity.notFound().build();
            }

            // Build updated client
            RegisteredClient.Builder clientBuilder = RegisteredClient.from(existingClient);

            if (StringUtils.hasText(request.getClientName())) {
                clientBuilder.clientName(request.getClientName());
            }

            if (request.getRedirectUris() != null) {
                clientBuilder.redirectUris(uris -> {
                    uris.clear();
                    uris.addAll(request.getRedirectUris());
                });
            }

            if (request.getScopes() != null) {
                clientBuilder.scopes(scopes -> {
                    scopes.clear();
                    scopes.addAll(request.getScopes());
                });
            }

            RegisteredClient updatedClient = clientBuilder.build();
            registeredClientRepository.save(updatedClient);

            // Update ESPI 4.0 compliance data
            if (request.getSecurityLevel() != null || request.getRateLimitPerMinute() != null ||
                request.getMaxConcurrentSessions() != null) {
                updateEspi40ComplianceData(clientId, request);
            }

            logger.info("Successfully updated client: {}", clientId);
            return ResponseEntity.ok(new SuccessResponse("Client updated successfully"));

        } catch (Exception e) {
            logger.error("Error updating client", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("server_error", "Failed to update client"));
        }
    }

    /**
     * Update client status (activate, suspend, revoke)
     * 
     * PUT /api/v1/oauth2/clients/{clientId}/status
     */
    @PutMapping("/{clientId}/status")
    public ResponseEntity<?> updateClientStatus(
            @PathVariable @NotBlank String clientId,
            @Valid @RequestBody UpdateClientStatusRequest request) {
        
        logger.info("Updating client status: {} to {}", clientId, request.getStatus());

        try {
            RegisteredClient client = registeredClientRepository.findByClientId(clientId);
            if (client == null) {
                return ResponseEntity.notFound().build();
            }

            // Update status in database
            String sql = """
                UPDATE oauth2_registered_client 
                SET client_status = ?, updated_by = ?, locked_until = ?
                WHERE client_id = ?
                """;

            Instant lockUntil = null;
            if ("suspended".equals(request.getStatus()) && request.getLockDurationMinutes() != null) {
                lockUntil = Instant.now().plus(Duration.ofMinutes(request.getLockDurationMinutes()));
            }

            jdbcTemplate.update(sql, request.getStatus(), request.getUpdatedBy(), lockUntil, clientId);

            // Log audit event
            logAuditEvent("client_status_update", clientId, request.getUpdatedBy(), true,
                         Map.of("new_status", request.getStatus(), "reason", request.getReason()));

            return ResponseEntity.ok(new SuccessResponse("Client status updated successfully"));

        } catch (Exception e) {
            logger.error("Error updating client status", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("server_error", "Failed to update client status"));
        }
    }

    /**
     * Get client metrics and usage analytics
     * 
     * GET /api/v1/oauth2/clients/{clientId}/metrics
     */
    @GetMapping("/{clientId}/metrics")
    public ResponseEntity<?> getClientMetrics(
            @PathVariable @NotBlank String clientId,
            @RequestParam(defaultValue = "30") int days) {
        
        logger.debug("Getting metrics for client: {} (last {} days)", clientId, days);

        try {
            // Get recent metrics
            String sql = """
                SELECT metric_date, total_requests, successful_requests, failed_requests,
                       total_tokens_issued, avg_response_time_ms, unique_users_served,
                       espi_data_requests, consent_grants, consent_withdrawals
                FROM oauth2_client_metrics 
                WHERE client_id = ? AND metric_date >= CURRENT_DATE - INTERVAL ? DAY
                ORDER BY metric_date DESC
                """;

            List<ClientMetrics> metrics = jdbcTemplate.query(sql, 
                new Object[]{clientId, days},
                (rs, rowNum) -> {
                    ClientMetrics metric = new ClientMetrics();
                    metric.setDate(rs.getDate("metric_date").toLocalDate());
                    metric.setTotalRequests(rs.getLong("total_requests"));
                    metric.setSuccessfulRequests(rs.getLong("successful_requests"));
                    metric.setFailedRequests(rs.getLong("failed_requests"));
                    metric.setTotalTokensIssued(rs.getLong("total_tokens_issued"));
                    metric.setAvgResponseTimeMs(rs.getDouble("avg_response_time_ms"));
                    metric.setUniqueUsersServed(rs.getInt("unique_users_served"));
                    metric.setEspiDataRequests(rs.getLong("espi_data_requests"));
                    metric.setConsentGrants(rs.getInt("consent_grants"));
                    metric.setConsentWithdrawals(rs.getInt("consent_withdrawals"));
                    return metric;
                });

            // Calculate summary statistics
            ClientMetricsSummary summary = calculateMetricsSummary(metrics);

            ClientMetricsResponse response = new ClientMetricsResponse();
            response.setClientId(clientId);
            response.setPeriodDays(days);
            response.setSummary(summary);
            response.setDailyMetrics(metrics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting client metrics", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("server_error", "Failed to retrieve client metrics"));
        }
    }

    /**
     * Bulk operations for enterprise client management
     * 
     * POST /api/v1/oauth2/clients/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkOperations(@Valid @RequestBody BulkOperationRequest request) {
        logger.info("Performing bulk operation: {} on {} clients", 
                   request.getOperation(), request.getClientIds().size());

        try {
            List<BulkOperationResult> results = new ArrayList<>();

            for (String clientId : request.getClientIds()) {
                try {
                    switch (request.getOperation()) {
                        case "suspend":
                            suspendClient(clientId, request.getReason());
                            results.add(new BulkOperationResult(clientId, true, "Client suspended"));
                            break;
                        case "activate":
                            activateClient(clientId, request.getReason());
                            results.add(new BulkOperationResult(clientId, true, "Client activated"));
                            break;
                        case "delete":
                            deleteClient(clientId);
                            results.add(new BulkOperationResult(clientId, true, "Client deleted"));
                            break;
                        default:
                            results.add(new BulkOperationResult(clientId, false, "Unknown operation"));
                    }
                } catch (Exception e) {
                    logger.error("Bulk operation failed for client: {}", clientId, e);
                    results.add(new BulkOperationResult(clientId, false, e.getMessage()));
                }
            }

            BulkOperationResponse response = new BulkOperationResponse();
            response.setOperation(request.getOperation());
            response.setTotalClients(request.getClientIds().size());
            response.setSuccessCount((int) results.stream().filter(BulkOperationResult::isSuccess).count());
            response.setFailureCount((int) results.stream().filter(r -> !r.isSuccess()).count());
            response.setResults(results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Bulk operation failed", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("server_error", "Bulk operation failed"));
        }
    }

    // Helper methods

    private void validateEspiCompliance(CreateClientRequest request) {
        // Validate scopes are ESPI-compliant
        if (request.getScopes() != null) {
            for (String scope : request.getScopes()) {
                if (!ESPI_SCOPES.contains(scope)) {
                    logger.warn("Non-ESPI scope requested: {}", scope);
                }
            }
        }

        // Validate grant types
        if (request.getGrantTypes() != null) {
            Set<String> allowedGrantTypes = Set.of("authorization_code", "client_credentials", "refresh_token");
            for (String grantType : request.getGrantTypes()) {
                if (!allowedGrantTypes.contains(grantType)) {
                    throw new IllegalArgumentException("Unsupported grant_type for ESPI: " + grantType);
                }
            }
        }

        // Validate redirect URIs for HTTPS in production
        if (request.getRedirectUris() != null) {
            for (String uri : request.getRedirectUris()) {
                if (!isValidRedirectUri(uri)) {
                    throw new IllegalArgumentException("Invalid redirect_uri: " + uri);
                }
            }
        }
    }

    private boolean isValidRedirectUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            return false;
        }
        
        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
            return false;
        }
        
        // Additional validation can be added here
        return true;
    }

    private String generateClientId(String prefix) {
        String basePrefix = StringUtils.hasText(prefix) ? prefix : "espi_client";
        return basePrefix + "_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(secureRandom.nextInt());
    }

    private String generateClientSecret() {
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }

    private void insertEspiApplicationInfo(RegisteredClient client, CreateClientRequest request) {
        String sql = """
            INSERT INTO espi_application_info 
            (uuid, client_id, client_name, client_description, scope, grant_types, 
             token_endpoint_auth_method, third_party_application_type, espi_version,
             security_classification, certification_status, business_category, service_territory)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
            UUID.randomUUID().toString(),
            client.getClientId(),
            client.getClientName(),
            request.getClientDescription(),
            String.join(",", client.getScopes()),
            client.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .collect(Collectors.joining(",")),
            client.getClientAuthenticationMethods().iterator().next().getValue(),
            request.getApplicationType() != null ? request.getApplicationType() : "WEB",
            "4.0", // ESPI version
            request.getSecurityClassification() != null ? request.getSecurityClassification() : "internal",
            request.getCertificationStatus() != null ? request.getCertificationStatus() : "self_certified",
            request.getBusinessCategory(),
            request.getServiceTerritory()
        );
    }

    private void insertEspi40ComplianceData(RegisteredClient client, CreateClientRequest request) {
        String sql = """
            UPDATE oauth2_registered_client 
            SET espi_version = ?, security_level = ?, client_status = ?,
                rate_limit_per_minute = ?, max_concurrent_sessions = ?, created_by = ?
            WHERE client_id = ?
            """;

        jdbcTemplate.update(sql,
            "4.0",
            request.getSecurityLevel() != null ? request.getSecurityLevel() : "standard",
            "active",
            request.getRateLimitPerMinute() != null ? request.getRateLimitPerMinute() : 100,
            request.getMaxConcurrentSessions() != null ? request.getMaxConcurrentSessions() : 5,
            request.getCreatedBy() != null ? request.getCreatedBy() : "system",
            client.getClientId()
        );
    }

    private void updateEspi40ComplianceData(String clientId, UpdateClientRequest request) {
        List<String> setParts = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (request.getSecurityLevel() != null) {
            setParts.add("security_level = ?");
            params.add(request.getSecurityLevel());
        }

        if (request.getRateLimitPerMinute() != null) {
            setParts.add("rate_limit_per_minute = ?");
            params.add(request.getRateLimitPerMinute());
        }

        if (request.getMaxConcurrentSessions() != null) {
            setParts.add("max_concurrent_sessions = ?");
            params.add(request.getMaxConcurrentSessions());
        }

        if (!setParts.isEmpty()) {
            setParts.add("updated_by = ?");
            params.add(request.getUpdatedBy() != null ? request.getUpdatedBy() : "system");
            params.add(clientId);

            String sql = "UPDATE oauth2_registered_client SET " + 
                        String.join(", ", setParts) + " WHERE client_id = ?";
            
            jdbcTemplate.update(sql, params.toArray());
        }
    }

    private ClientDetails mapToClientDetails(RegisteredClient client, 
                                           Map<String, Object> espiInfo,
                                           Map<String, Object> complianceData) {
        ClientDetails details = new ClientDetails();
        details.setId(client.getId());
        details.setClientId(client.getClientId());
        details.setClientName(client.getClientName());
        details.setAuthenticationMethods(client.getClientAuthenticationMethods().stream()
            .map(ClientAuthenticationMethod::getValue).collect(Collectors.toList()));
        details.setGrantTypes(client.getAuthorizationGrantTypes().stream()
            .map(AuthorizationGrantType::getValue).collect(Collectors.toList()));
        details.setRedirectUris(new ArrayList<>(client.getRedirectUris()));
        details.setScopes(new ArrayList<>(client.getScopes()));

        if (complianceData != null) {
            details.setEspiVersion((String) complianceData.get("espi_version"));
            details.setSecurityLevel((String) complianceData.get("security_level"));
            details.setStatus((String) complianceData.get("client_status"));
            details.setRateLimitPerMinute((Integer) complianceData.get("rate_limit_per_minute"));
            details.setMaxConcurrentSessions((Integer) complianceData.get("max_concurrent_sessions"));
            details.setFailureCount((Integer) complianceData.get("failure_count"));
        }

        if (espiInfo != null) {
            details.setBusinessCategory((String) espiInfo.get("business_category"));
            details.setCertificationStatus((String) espiInfo.get("certification_status"));
            details.setServiceTerritory((String) espiInfo.get("service_territory"));
        }

        return details;
    }

    private ClientMetricsSummary calculateMetricsSummary(List<ClientMetrics> metrics) {
        if (metrics.isEmpty()) {
            return new ClientMetricsSummary();
        }

        ClientMetricsSummary summary = new ClientMetricsSummary();
        summary.setTotalRequests(metrics.stream().mapToLong(ClientMetrics::getTotalRequests).sum());
        summary.setTotalSuccessfulRequests(metrics.stream().mapToLong(ClientMetrics::getSuccessfulRequests).sum());
        summary.setTotalFailedRequests(metrics.stream().mapToLong(ClientMetrics::getFailedRequests).sum());
        summary.setTotalTokensIssued(metrics.stream().mapToLong(ClientMetrics::getTotalTokensIssued).sum());
        summary.setAverageResponseTime(metrics.stream().mapToDouble(ClientMetrics::getAvgResponseTimeMs).average().orElse(0.0));
        summary.setTotalUniqueUsers(metrics.stream().mapToInt(ClientMetrics::getUniqueUsersServed).max().orElse(0));
        
        if (summary.getTotalRequests() > 0) {
            summary.setSuccessRate((double) summary.getTotalSuccessfulRequests() / summary.getTotalRequests() * 100);
        }

        return summary;
    }

    private void suspendClient(String clientId, String reason) {
        String sql = """
            UPDATE oauth2_registered_client 
            SET client_status = 'suspended', updated_by = 'bulk_operation'
            WHERE client_id = ?
            """;
        jdbcTemplate.update(sql, clientId);
        logAuditEvent("client_suspended", clientId, "bulk_operation", true, Map.of("reason", reason));
    }

    private void activateClient(String clientId, String reason) {
        String sql = """
            UPDATE oauth2_registered_client 
            SET client_status = 'active', locked_until = NULL, updated_by = 'bulk_operation'
            WHERE client_id = ?
            """;
        jdbcTemplate.update(sql, clientId);
        logAuditEvent("client_activated", clientId, "bulk_operation", true, Map.of("reason", reason));
    }

    private void deleteClient(String clientId) {
        // First check if client exists
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalArgumentException("Client not found: " + clientId);
        }

        // Delete from ESPI application info first (foreign key constraint)
        jdbcTemplate.update("DELETE FROM espi_application_info WHERE client_id = ?", clientId);
        
        // Delete the main client record
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
        
        logAuditEvent("client_deleted", clientId, "bulk_operation", true, Map.of());
    }

    private void logAuditEvent(String eventType, String clientId, String principalName, 
                              boolean success, Map<String, Object> additionalData) {
        try {
            String sql = """
                INSERT INTO oauth2_audit_log 
                (event_type, client_id, principal_name, success, additional_data)
                VALUES (?, ?, ?, ?, ?)
                """;
            
            String additionalDataJson = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(additionalData);
            
            jdbcTemplate.update(sql, eventType, clientId, principalName, success, additionalDataJson);
        } catch (Exception e) {
            logger.error("Failed to log audit event", e);
        }
    }

    // DTO Classes follow in the next message due to length limits
}