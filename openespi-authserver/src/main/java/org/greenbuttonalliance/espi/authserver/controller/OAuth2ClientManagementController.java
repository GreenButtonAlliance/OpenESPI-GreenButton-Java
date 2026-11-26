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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth2 Client Management Controller for ESPI 4.0
 * 
 * The administrators' API for creating, reading, updating and changing the status of registered clients, with the
 * ESPI data the AS keeps about them ({@code espi_application_info} and the compliance columns). Kept and trimmed
 * down by owner decision (#344): {@code OAuthAdminController} lists, reads and deletes clients, and Third Parties
 * register themselves through RFC 7591 {@code /oauth2/register}.
 *
 * Endpoints:
 * - GET /api/v1/oauth2/clients: List clients with pagination and filtering
 * - POST /api/v1/oauth2/clients: Create new client
 * - GET /api/v1/oauth2/clients/{clientId}: Get client details
 * - PUT /api/v1/oauth2/clients/{clientId}: Update client
 * - PUT /api/v1/oauth2/clients/{clientId}/status: Update client status. The status is stored but not yet
 *   enforced at the token, authorization or introspection endpoints (#345).
 *
 * The update and status requests are validated after the client is found, so an unknown client is 404 whatever
 * the body holds, as long as it is JSON of the right shape (#344).
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
    private final SecureRandom secureRandom = new SecureRandom();

    // ESPI 4.0 compliant scopes. HistoryLength is in seconds (REQ.21.6.1.2.1):
    // 34128000 = 395 days (about 13 months), 604800 = 7 days.
    private static final Set<String> ESPI_SCOPES = Set.of(
        "openid", "profile", "email",
        "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000",
        "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000",
        "FB=4_5_15;IntervalDuration=3600;BlockDuration=daily;HistoryLength=604800",
        "FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=604800",
        "FB=34_35",   // Third Party client access token (client_credentials)
        "FB=36_40",   // registration access token renewal
        "DataCustodian_Admin_Access",
        "ThirdParty_Admin_Access"
    );

    /**
     * The client authentication methods a client created through this API can use at the token endpoint (#332).
     * {@code tls_client_auth} needs the client's certificate subject DN in its settings, and
     * {@code client_secret_jwt} / {@code private_key_jwt} a JWK Set, none of which this API sets; any other
     * value is not a method at all.
     */
    static final Set<String> SUPPORTED_AUTHENTICATION_METHODS = Set.of(
        ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(),
        ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue(),
        ClientAuthenticationMethod.NONE.getValue());

    /**
     * The columns the client list may be sorted by (#344), by the name a request gives. The ORDER BY is built only
     * from these values, never from request text.
     */
    static final Map<String, String> SORT_COLUMNS = Map.of(
        "client_id", "c.client_id",
        "client_name", "c.client_name",
        "client_id_issued_at", "c.client_id_issued_at",
        "last_used_at", "c.last_used_at");

    /** The largest page of the client list. */
    static final int MAX_PAGE_SIZE = 100;

    private final Validator validator;

    @Autowired
    public OAuth2ClientManagementController(
            RegisteredClientRepository registeredClientRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            Validator validator) {
        this.registeredClientRepository = registeredClientRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    /**
     * List OAuth2 clients with pagination, filtering, and sorting
     * 
     * GET /api/v1/oauth2/clients
     */
    @GetMapping
    public ResponseEntity<?> listClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "client_id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String securityLevel,
            @RequestParam(required = false) String espiVersion,
            @RequestParam(required = false) String search) {

        logger.debug("Listing clients - page: {}, size: {}, status: {}, search: {}",
                    page, size, printable(status), printable(search));

        // #344: the sort column and direction come only from fixed values; the page is bounded
        String sortColumn = SORT_COLUMNS.get(sortBy);
        if (sortColumn == null) {
            return badRequest("sortBy must be one of: " + String.join(", ", new TreeSet<>(SORT_COLUMNS.keySet())));
        }
        if (!"asc".equalsIgnoreCase(sortDirection) && !"desc".equalsIgnoreCase(sortDirection)) {
            return badRequest("sortDirection must be asc or desc");
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            return badRequest("page must be at least 0 and size between 1 and " + MAX_PAGE_SIZE);
        }
        String direction = "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";

        try {
            // #344: one FROM ... WHERE for the page and its count, so the count no longer depends on a regex
            StringBuilder from = new StringBuilder("""
                 FROM oauth2_registered_client c
                LEFT JOIN espi_application_info e ON c.client_id = e.client_id
                WHERE 1=1""");

            List<Object> params = new ArrayList<>();

            // Add filters
            if (StringUtils.hasText(status)) {
                from.append(" AND c.client_status = ?");
                params.add(status);
            }

            if (StringUtils.hasText(securityLevel)) {
                from.append(" AND c.security_level = ?");
                params.add(securityLevel);
            }

            if (StringUtils.hasText(espiVersion)) {
                from.append(" AND c.espi_version = ?");
                params.add(espiVersion);
            }

            if (StringUtils.hasText(search)) {
                from.append(" AND (c.client_name LIKE ? OR c.client_id LIKE ?)");
                params.add("%" + search + "%");
                params.add("%" + search + "%");
            }

            String sql = """
                SELECT c.id, c.client_id, c.client_name, c.client_authentication_methods,
                       c.authorization_grant_types, c.redirect_uris, c.scopes,
                       c.espi_version, c.security_level, c.client_status, c.rate_limit_per_minute,
                       c.max_concurrent_sessions, c.created_by, c.last_used_at, c.failure_count,
                       e.business_category, e.certification_status, e.service_territory
                """ + from + " ORDER BY " + sortColumn + " " + direction + " LIMIT ? OFFSET ?";
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(size);
            pageParams.add((long) page * size);

            // Execute query
            List<ClientDetails> clients = jdbcTemplate.query(sql,
                pageParams.toArray(),
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
            Long totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*)" + from, Long.class, params.toArray());

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
     * Whether a new client must send PKCE. A confidential client may (#274: accepted, not required),
     * but a public client ({@code none} authentication) always must: RFC 9700 section 2.1.1, and Spring
     * Authorization Server authenticates a public client at the token endpoint only by its code_verifier.
     * A client whose methods include {@code none} next to a secret method counts as public: PKCE is a
     * per-client setting, and such a client may authenticate as public, so it must always send PKCE.
     */
    static boolean requiresProofKey(CreateClientRequest request) {
        List<String> methods = request.getAuthenticationMethods();
        boolean publicClient = methods != null
                && methods.contains(ClientAuthenticationMethod.NONE.getValue());
        return publicClient || request.isRequireProofKey();
    }

    /**
     * Whether a new client is public only: its sole authentication method is {@code none} (#332). It gets no
     * secret, since it could never use one. A client with {@code none} next to a secret method still gets one.
     */
    static boolean isPublicOnly(CreateClientRequest request) {
        List<String> methods = request.getAuthenticationMethods();
        return methods != null && !methods.isEmpty()
                && methods.stream().allMatch(ClientAuthenticationMethod.NONE.getValue()::equals);
    }

    /** A 400 {@code invalid_request} with a message that quotes no request value. */
    private static ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", message));
    }

    /**
     * Apply the request's bean validation (#344): called after the client is found, so an unknown client is 404
     * rather than 400. The messages are the constraints' own, sorted, and quote no request value.
     */
    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
    }

    /**
     * Refuse a null or blank scope (#344): Spring's {@code RegisteredClient.Builder} accepts null, and the
     * repository would save it as the literal scope "null".
     */
    private static void refuseBlankScopes(List<String> scopes) {
        if (scopes != null && scopes.stream().anyMatch(scope -> !StringUtils.hasText(scope))) {
            throw new IllegalArgumentException("scopes must not contain a null or blank value");
        }
    }

    /** Refuse a request naming fields it does not have (#332), rather than silently dropping them. */
    private static void refuseUnknownFields(RefusesUnknownFields request) {
        if (!request.getUnknownFields().isEmpty()) {
            throw new IllegalArgumentException("Unknown field(s): " + request.getUnknownFields().stream()
                    .map(OAuth2ClientManagementController::printable)
                    .collect(Collectors.joining(", ")));
        }
    }

    /** A request-supplied name as it may appear in an error message and the log: printable ASCII, at most 64 chars. */
    private static String printable(String name) {
        return printable(name, 64);
    }

    private static String printable(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        String safe = text.replaceAll("[^\\x20-\\x7E]", "?");
        return safe.length() > maxLength ? safe.substring(0, maxLength) + "..." : safe;
    }

    /** The longest validation message that is logged and returned. */
    static final int MAX_MESSAGE_LENGTH = 500;

    /** How much of its start a longer validation message keeps; the rest of the room goes to its end. */
    private static final int MESSAGE_HEAD_LENGTH = 200;

    /**
     * A validation failure's message as it may appear in the log and the 400 body (#332 review): printable ASCII,
     * at most {@value #MAX_MESSAGE_LENGTH} characters, or "Invalid request" when the exception has none. It may quote
     * request values: this controller's own messages already pass them through {@link #printable(String)}, but
     * Spring's {@code RegisteredClient.Builder} quotes a refused scope or redirect URI as it was sent, before the
     * reason. A longer message keeps its start and its end, with "..." between, so the reason survives whatever the
     * quoted value contains.
     */
    static String safeMessage(IllegalArgumentException e) {
        if (!StringUtils.hasText(e.getMessage())) {
            return "Invalid request";
        }
        String safe = e.getMessage().replaceAll("[^\\x20-\\x7E]", "?");
        if (safe.length() <= MAX_MESSAGE_LENGTH) {
            return safe;
        }
        int tailLength = MAX_MESSAGE_LENGTH - MESSAGE_HEAD_LENGTH - "...".length();
        return safe.substring(0, MESSAGE_HEAD_LENGTH) + "..." + safe.substring(safe.length() - tailLength);
    }

    /** The longest client_id: ESPI's ApplicationInformation.client_id is a String64 (#332 review). */
    static final int CLIENT_ID_MAX_LENGTH = 64;

    /** The length of what {@link #clientIdSuffix} adds to the prefix: "_", 13 digits of milliseconds, "_", 8 hex digits. */
    static final int CLIENT_ID_SUFFIX_LENGTH = 23;

    /** The longest client_name_prefix, so the client_id stays within {@value #CLIENT_ID_MAX_LENGTH}. */
    static final int CLIENT_ID_PREFIX_MAX_LENGTH = CLIENT_ID_MAX_LENGTH - CLIENT_ID_SUFFIX_LENGTH;

    /**
     * What a client_name_prefix may contain (#332 review): it becomes part of the client_id, which is saved, returned
     * and logged, so only letters, digits, '_' and '-', at most {@value #CLIENT_ID_PREFIX_MAX_LENGTH} of them.
     */
    static final java.util.regex.Pattern CLIENT_ID_PREFIX =
            java.util.regex.Pattern.compile("[A-Za-z0-9_-]{1," + CLIENT_ID_PREFIX_MAX_LENGTH + "}");

    /** The prefix of a client_id when the request gives none. */
    private static final String DEFAULT_CLIENT_ID_PREFIX = "espi_client";

    /**
     * Create new OAuth2 client with ESPI 4.0 compliance
     * 
     * POST /api/v1/oauth2/clients
     */
    @PostMapping
    public ResponseEntity<?> createClient(@Valid @RequestBody CreateClientRequest request) {
        logger.info("Creating new client: {}", printable(request.getClientName()));

        try {
            // Validate ESPI compliance
            validateEspiCompliance(request);

            // Generate client credentials: no secret for a public client (#332)
            String clientId = generateClientId(request.getClientNamePrefix());
            String clientSecret = isPublicOnly(request) ? null : generateClientSecret();

            // Build RegisteredClient
            RegisteredClient.Builder clientBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientName(request.getClientName())
                    .clientIdIssuedAt(Instant.now());
            if (clientSecret != null) {
                clientBuilder.clientSecret(passwordEncoder.encode(clientSecret));
            }

            // Configure authentication methods
            if (request.getAuthenticationMethods() != null && !request.getAuthenticationMethods().isEmpty()) {
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
                           .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000");
            }

            // Configure client settings
            ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder()
                    .requireProofKey(requiresProofKey(request))
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

            logger.info("Successfully created client: {} with client_id: {}", printable(request.getClientName()), clientId);

            // Build response
            CreateClientResponse response = new CreateClientResponse();
            response.setClientId(clientId);
            response.setClientSecret(clientSecret);
            response.setClientName(request.getClientName());
            response.setCreatedAt(Instant.now());
            response.setEspiVersion("4.0");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            String message = safeMessage(e);
            logger.warn("Client creation validation failed: {}", message);
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", message));
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
            @RequestBody UpdateClientRequest request) {

        logger.info("Updating client: {}", clientId);

        try {
            RegisteredClient existingClient = registeredClientRepository.findByClientId(clientId);
            if (existingClient == null) {
                return ResponseEntity.notFound().build();
            }

            // #332: refuse what this update would not apply, rather than answering 200 for it
            refuseUnknownFields(request);
            validate(request);
            refuseBlankScopes(request.getScopes());
            if (request.getClientName() != null && !StringUtils.hasText(request.getClientName())) {
                throw new IllegalArgumentException("client_name must not be blank");
            }
            // #332 review: the same redirect URI check as create, so an update cannot set one create would refuse
            if (request.getRedirectUris() != null) {
                for (String uri : request.getRedirectUris()) {
                    if (!isValidRedirectUri(uri)) {
                        throw new IllegalArgumentException("Invalid redirect_uri: " + printable(uri));
                    }
                }
            }

            // Build updated client
            RegisteredClient.Builder clientBuilder = RegisteredClient.from(existingClient);

            if (request.getClientName() != null) {
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

            // Update ESPI 4.0 compliance data and who updated the client (#332 review: also when updated_by is
            // the only one given). Not written otherwise: the H2 schema has none of these columns (#145).
            if (request.getSecurityLevel() != null || request.getRateLimitPerMinute() != null ||
                request.getMaxConcurrentSessions() != null || request.getUpdatedBy() != null) {
                updateEspi40ComplianceData(clientId, request);
            }

            logger.info("Successfully updated client: {}", clientId);
            return ResponseEntity.ok(new SuccessResponse("Client updated successfully"));

        } catch (IllegalArgumentException e) {
            String message = safeMessage(e);
            logger.warn("Client update validation failed: {}", message);
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", message));
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
            @RequestBody UpdateClientStatusRequest request) {

        logger.info("Updating client status: {} to {}", clientId, printable(request.getStatus()));

        try {
            RegisteredClient client = registeredClientRepository.findByClientId(clientId);
            if (client == null) {
                return ResponseEntity.notFound().build();
            }
            refuseUnknownFields(request);
            validate(request);

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

            // Log audit event (#344: reason is optional, and Map.of refuses a null value)
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("new_status", request.getStatus());
            if (request.getReason() != null) {
                auditData.put("reason", request.getReason());
            }
            logAuditEvent("client_status_update", clientId, request.getUpdatedBy(), true, auditData);

            return ResponseEntity.ok(new SuccessResponse("Client status updated successfully"));

        } catch (IllegalArgumentException e) {
            String message = safeMessage(e);
            logger.warn("Client status validation failed: {}", message);
            return badRequest(message);
        } catch (Exception e) {
            logger.error("Error updating client status", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("server_error", "Failed to update client status"));
        }
    }

    // Helper methods

    private void validateEspiCompliance(CreateClientRequest request) {
        refuseUnknownFields(request);

        // #332 review (rounds 3-5): the prefix becomes part of the client_id. A prefix that is given, even empty or
        // blank, must match; null means absent (as for every optional field here) and gets the default.
        String prefix = request.getClientNamePrefix();
        if (prefix != null && !CLIENT_ID_PREFIX.matcher(prefix).matches()) {
            throw new IllegalArgumentException("client_name_prefix must be 1-" + CLIENT_ID_PREFIX_MAX_LENGTH
                    + " letters, digits, '_' or '-': \"" + printable(prefix) + "\"");
        }

        // #332: only the authentication methods a client created here can use
        if (request.getAuthenticationMethods() != null) {
            for (String method : request.getAuthenticationMethods()) {
                if (method == null || !SUPPORTED_AUTHENTICATION_METHODS.contains(method)) {
                    throw new IllegalArgumentException("Unsupported authentication_methods value: "
                            + (method == null ? "null" : printable(method)) + " (supported: "
                            + String.join(", ", new TreeSet<>(SUPPORTED_AUTHENTICATION_METHODS)) + ")");
                }
            }
        }

        // A public client has no credentials, so it cannot use client_credentials
        if (isPublicOnly(request) && request.getGrantTypes() != null
                && request.getGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())) {
            throw new IllegalArgumentException(
                    "A public client (authentication method none only) cannot use the client_credentials grant");
        }

        // Validate scopes are ESPI-compliant: a null or blank one is refused (#344), any other non-ESPI one logged
        refuseBlankScopes(request.getScopes());
        if (request.getScopes() != null) {
            for (String scope : request.getScopes()) {
                if (!ESPI_SCOPES.contains(scope)) {
                    logger.warn("Non-ESPI scope requested: {}", printable(scope));
                }
            }
        }

        // Validate grant types (#332 review: request values reach the log and the response only as printable())
        if (request.getGrantTypes() != null) {
            Set<String> allowedGrantTypes = Set.of("authorization_code", "client_credentials", "refresh_token");
            for (String grantType : request.getGrantTypes()) {
                if (grantType == null || !allowedGrantTypes.contains(grantType)) {
                    throw new IllegalArgumentException("Unsupported grant_type for ESPI: " + printable(grantType));
                }
            }
        }

        // Validate redirect URIs: http or https. Plain http is still accepted for any host, in every profile;
        // refusing it outside local/dev (except loopback) is part of #298.
        if (request.getRedirectUris() != null) {
            for (String uri : request.getRedirectUris()) {
                if (!isValidRedirectUri(uri)) {
                    throw new IllegalArgumentException("Invalid redirect_uri: " + printable(uri));
                }
            }
        }
    }

    /**
     * The method recorded in {@code espi_application_info.token_endpoint_auth_method} (#332 review): the client's
     * secret method when it has one (the first in name order if several), otherwise {@code none}. The methods are
     * an unordered set, so taking whichever comes first could record {@code none} for a client that has a secret.
     */
    static String recordedAuthenticationMethod(RegisteredClient client) {
        return client.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue)
                .filter(method -> !ClientAuthenticationMethod.NONE.getValue().equals(method))
                .sorted()
                .findFirst()
                .orElse(ClientAuthenticationMethod.NONE.getValue());
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
        return clientId(prefix, System.currentTimeMillis(), secureRandom.nextInt());
    }

    /**
     * A client_id: the prefix ({@value #DEFAULT_CLIENT_ID_PREFIX} when null) and {@link #clientIdSuffix}. The limits
     * are enforced here as well as in the request validation (#332 review), so no caller can build a client_id with
     * an unsafe prefix or longer than {@value #CLIENT_ID_MAX_LENGTH}.
     */
    static String clientId(String prefix, long millis, int random) {
        String basePrefix = prefix != null ? prefix : DEFAULT_CLIENT_ID_PREFIX;
        if (!CLIENT_ID_PREFIX.matcher(basePrefix).matches()) {
            throw new IllegalStateException("client_id prefix not validated");
        }
        String clientId = basePrefix + clientIdSuffix(millis, random);
        if (clientId.length() > CLIENT_ID_MAX_LENGTH) {
            throw new IllegalStateException("client_id longer than " + CLIENT_ID_MAX_LENGTH + " characters");
        }
        return clientId;
    }

    /** "_", the milliseconds as 13 digits, "_", the random value as 8 hex digits: {@value #CLIENT_ID_SUFFIX_LENGTH} characters. */
    static String clientIdSuffix(long millis, int random) {
        // Locale.ROOT: a default locale could otherwise format the digits in another script
        return String.format(java.util.Locale.ROOT, "_%013d_%08x", millis, random);
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
            recordedAuthenticationMethod(client),
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

        // Records who made the change (updated_by, or "system"); called only when one of these fields is given
        setParts.add("updated_by = ?");
        params.add(request.getUpdatedBy() != null ? request.getUpdatedBy() : "system");
        params.add(clientId);

        String sql = "UPDATE oauth2_registered_client SET " +
                    String.join(", ", setParts) + " WHERE client_id = ?";

        jdbcTemplate.update(sql, params.toArray());
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

    private void logAuditEvent(String eventType, String clientId, String principalName, 
                              boolean success, Map<String, Object> additionalData) {
        try {
            String sql = """
                INSERT INTO oauth2_audit_log 
                (event_type, client_id, principal_name, success, additional_data)
                VALUES (?, ?, ?, ?, ?)
                """;
            
            String additionalDataJson = new ObjectMapper()
                .writeValueAsString(additionalData);
            
            jdbcTemplate.update(sql, eventType, clientId, principalName, success, additionalDataJson);
        } catch (Exception e) {
            logger.error("Failed to log audit event", e);
        }
    }

    // DTO Classes follow in the next message due to length limits
}