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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * OIDC Dynamic Client Registration Controller
 * 
 * Implements RFC 7591 (OAuth 2.0 Dynamic Client Registration Protocol) with ESPI extensions.
 * Provides endpoints for:
 * - Dynamic client registration (/connect/register)
 * - ESPI-specific client registration validation
 * - Green Button Alliance compliance checks
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@RestController
@RequestMapping("/connect")
public class ClientRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(ClientRegistrationController.class);

    private final RegisteredClientRepository clientRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${espi.security.require-https-redirect-uris:false}")
    private boolean requireHttpsRedirectUris;

    // ESPI-specific scopes
    private static final Set<String> ESPI_SCOPES = Set.of(
        "openid", "profile",
        "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
        "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13",
        "DataCustodian_Admin_Access",
        "ThirdParty_Admin_Access"
    );

    public ClientRegistrationController(RegisteredClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * OIDC Dynamic Client Registration Endpoint
     * 
     * POST /connect/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerClient(@RequestBody ClientRegistrationRequest request) {
        logger.debug("Processing client registration request for client_name: {}", request.getClientName());

        try {
            // Validate registration request
            validateRegistrationRequest(request);

            // Generate client credentials
            String clientId = generateClientId();
            String clientSecret = generateClientSecret();

            // Build registered client
            RegisteredClient.Builder clientBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret("{noop}" + clientSecret) // TODO: Use proper password encoder
                    .clientName(request.getClientName())
                    .clientIdIssuedAt(Instant.now());

            // Set client secret expiration if provided
            if (request.getClientSecretExpiresAt() != null && request.getClientSecretExpiresAt() > 0) {
                clientBuilder.clientSecretExpiresAt(Instant.ofEpochSecond(request.getClientSecretExpiresAt()));
            }

            // Configure authentication methods
            configureAuthenticationMethods(clientBuilder, request);

            // Configure grant types
            configureGrantTypes(clientBuilder, request);

            // Configure redirect URIs
            configureRedirectUris(clientBuilder, request);

            // Configure scopes
            configureScopes(clientBuilder, request);

            // Configure client and token settings
            configureClientSettings(clientBuilder, request);
            configureTokenSettings(clientBuilder, request);

            RegisteredClient registeredClient = clientBuilder.build();

            // Save the client
            clientRepository.save(registeredClient);

            logger.info("Successfully registered client: {} with client_id: {}", request.getClientName(), clientId);

            // Return client registration response
            ClientRegistrationResponse response = buildRegistrationResponse(registeredClient, clientSecret);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Client registration validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_client_metadata", e.getMessage()));
        } catch (Exception e) {
            logger.error("Client registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("server_error", "Internal server error"));
        }
    }

    /**
     * Get registered client information
     * 
     * GET /connect/register/{client_id}
     */
    @GetMapping("/register/{clientId}")
    public ResponseEntity<?> getClient(@PathVariable String clientId) {
        logger.debug("Retrieving client information for client_id: {}", clientId);

        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        ClientRegistrationResponse response = buildRegistrationResponse(client, null); // Don't return secret
        return ResponseEntity.ok(response);
    }

    /**
     * Validate client registration request
     */
    private void validateRegistrationRequest(ClientRegistrationRequest request) {
        if (!StringUtils.hasText(request.getClientName())) {
            throw new IllegalArgumentException("client_name is required");
        }

        if (request.getRedirectUris() == null || request.getRedirectUris().isEmpty()) {
            throw new IllegalArgumentException("redirect_uris is required");
        }

        // Validate redirect URIs
        for (String uri : request.getRedirectUris()) {
            if (!isValidRedirectUri(uri)) {
                throw new IllegalArgumentException("Invalid redirect_uri: " + uri);
            }
        }

        // Validate ESPI-specific requirements
        validateEspiRequirements(request);
    }

    /**
     * Validate ESPI-specific requirements
     */
    private void validateEspiRequirements(ClientRegistrationRequest request) {
        // Ensure requested scopes are ESPI-compliant
        if (request.getScope() != null) {
            Set<String> requestedScopes = new HashSet<>(Arrays.asList(request.getScope().split(" ")));
            for (String scope : requestedScopes) {
                if (!ESPI_SCOPES.contains(scope)) {
                    logger.warn("Non-ESPI scope requested: {}", scope);
                    // Note: We don't reject here, just log the warning
                }
            }
        }

        // Validate grant types for ESPI compliance
        if (request.getGrantTypes() != null) {
            Set<String> allowedGrantTypes = Set.of("authorization_code", "client_credentials", "refresh_token");
            for (String grantType : request.getGrantTypes()) {
                if (!allowedGrantTypes.contains(grantType)) {
                    throw new IllegalArgumentException("Unsupported grant_type for ESPI: " + grantType);
                }
            }
        }
    }

    /**
     * Configure authentication methods
     */
    private void configureAuthenticationMethods(RegisteredClient.Builder builder, ClientRegistrationRequest request) {
        if (request.getTokenEndpointAuthMethod() != null) {
            switch (request.getTokenEndpointAuthMethod()) {
                case "client_secret_basic" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                case "client_secret_post" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                case "none" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
                default -> throw new IllegalArgumentException("Unsupported token_endpoint_auth_method: " + request.getTokenEndpointAuthMethod());
            }
        } else {
            // Default to client_secret_basic for ESPI
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        }
    }

    /**
     * Configure grant types
     */
    private void configureGrantTypes(RegisteredClient.Builder builder, ClientRegistrationRequest request) {
        if (request.getGrantTypes() != null && !request.getGrantTypes().isEmpty()) {
            for (String grantType : request.getGrantTypes()) {
                switch (grantType) {
                    case "authorization_code" -> builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                    case "client_credentials" -> builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                    case "refresh_token" -> builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                    default -> throw new IllegalArgumentException("Unsupported grant_type: " + grantType);
                }
            }
        } else {
            // Default to authorization_code for ESPI
            builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
            builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
        }
    }

    /**
     * Configure redirect URIs
     */
    private void configureRedirectUris(RegisteredClient.Builder builder, ClientRegistrationRequest request) {
        for (String uri : request.getRedirectUris()) {
            builder.redirectUri(uri);
        }

        if (request.getPostLogoutRedirectUris() != null) {
            for (String uri : request.getPostLogoutRedirectUris()) {
                builder.postLogoutRedirectUri(uri);
            }
        }
    }

    /**
     * Configure scopes
     */
    private void configureScopes(RegisteredClient.Builder builder, ClientRegistrationRequest request) {
        if (StringUtils.hasText(request.getScope())) {
            String[] scopes = request.getScope().split(" ");
            for (String scope : scopes) {
                builder.scope(scope.trim());
            }
        } else {
            // Default ESPI scopes
            builder.scope("openid");
            builder.scope("profile");
            builder.scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
        }
    }

    /**
     * Configure client settings
     */
    private void configureClientSettings(RegisteredClient.Builder builder, ClientRegistrationRequest request) {
        ClientSettings.Builder settingsBuilder = ClientSettings.builder();
        
        // ESPI clients typically require consent for customer data access
        boolean requireConsent = true;
        if (request.getGrantTypes() != null && request.getGrantTypes().contains("client_credentials")) {
            requireConsent = false; // Admin clients don't need consent
        }
        settingsBuilder.requireAuthorizationConsent(requireConsent);
        
        builder.clientSettings(settingsBuilder.build());
    }

    /**
     * Configure token settings
     */
    private void configureTokenSettings(RegisteredClient.Builder builder, ClientRegistrationRequest request) {
        TokenSettings.Builder settingsBuilder = TokenSettings.builder();
        
        // ESPI standard uses opaque access tokens
        settingsBuilder.accessTokenFormat(OAuth2TokenFormat.REFERENCE);
        
        // Set token lifetimes
        settingsBuilder.accessTokenTimeToLive(Duration.ofMinutes(360)); // 6 hours for ESPI
        settingsBuilder.refreshTokenTimeToLive(Duration.ofMinutes(3600)); // 60 hours
        settingsBuilder.reuseRefreshTokens(true);
        
        builder.tokenSettings(settingsBuilder.build());
    }

    /**
     * Build client registration response
     */
    private ClientRegistrationResponse buildRegistrationResponse(RegisteredClient client, String clientSecret) {
        ClientRegistrationResponse response = new ClientRegistrationResponse();
        response.setClientId(client.getClientId());
        response.setClientSecret(clientSecret); // Only set on initial registration
        response.setClientName(client.getClientName());
        response.setClientIdIssuedAt(client.getClientIdIssuedAt() != null ? 
            client.getClientIdIssuedAt().getEpochSecond() : null);
        response.setClientSecretExpiresAt(client.getClientSecretExpiresAt() != null ? 
            client.getClientSecretExpiresAt().getEpochSecond() : 0);

        // Authentication method
        if (!client.getClientAuthenticationMethods().isEmpty()) {
            response.setTokenEndpointAuthMethod(
                client.getClientAuthenticationMethods().iterator().next().getValue());
        }

        // Grant types
        response.setGrantTypes(client.getAuthorizationGrantTypes().stream()
            .map(grantType -> grantType.getValue())
            .toList());

        // Redirect URIs
        response.setRedirectUris(new ArrayList<>(client.getRedirectUris()));
        if (!client.getPostLogoutRedirectUris().isEmpty()) {
            response.setPostLogoutRedirectUris(new ArrayList<>(client.getPostLogoutRedirectUris()));
        }

        // Scopes
        response.setScope(String.join(" ", client.getScopes()));

        return response;
    }

    /**
     * Generate client ID
     */
    private String generateClientId() {
        return "espi_client_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(secureRandom.nextInt());
    }

    /**
     * Generate client secret
     */
    private String generateClientSecret() {
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }

    /**
     * Validate redirect URI with ESPI-specific rules
     */
    private boolean isValidRedirectUri(String uri) {
        // Basic URI validation
        if (!StringUtils.hasText(uri)) {
            return false;
        }
        
        // Must be absolute URI
        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
            return false;
        }
        
        // ESPI Security: Enforce HTTPS in production
        if (requireHttpsRedirectUris && uri.startsWith("http://")) {
            // Allow localhost HTTP for development/testing only
            if (!uri.contains("localhost") && !uri.contains("127.0.0.1")) {
                logger.warn("Rejecting HTTP redirect URI in production: {}", uri);
                return false;
            }
        }
        
        // Additional ESPI validation rules
        try {
            java.net.URI parsedUri = java.net.URI.create(uri);
            
            // Reject javascript: and data: schemes for security
            String scheme = parsedUri.getScheme();
            if ("javascript".equalsIgnoreCase(scheme) || "data".equalsIgnoreCase(scheme)) {
                logger.warn("Rejecting dangerous URI scheme: {}", scheme);
                return false;
            }
            
            // Reject malicious patterns
            if (uri.contains("<script>") || uri.contains("javascript:") || uri.contains("vbscript:")) {
                logger.warn("Rejecting URI with potentially malicious content: {}", uri);
                return false;
            }
            
            // ESPI compliance: Validate hostname patterns
            String host = parsedUri.getHost();
            if (host != null) {
                // Reject IP addresses in production (except localhost)
                if (requireHttpsRedirectUris && host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && 
                    !host.equals("127.0.0.1")) {
                    logger.warn("Rejecting IP address redirect URI in production: {}", host);
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.warn("Invalid URI format: {}", uri, e);
            return false;
        }
        
        return true;
    }

    /**
     * Client Registration Request DTO
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientRegistrationRequest {
        @JsonProperty("client_name")
        private String clientName;
        
        @JsonProperty("redirect_uris")
        private List<String> redirectUris;
        
        @JsonProperty("post_logout_redirect_uris")
        private List<String> postLogoutRedirectUris;
        
        @JsonProperty("token_endpoint_auth_method")
        private String tokenEndpointAuthMethod;
        
        @JsonProperty("grant_types")
        private List<String> grantTypes;
        
        @JsonProperty("response_types")
        private List<String> responseTypes;
        
        @JsonProperty("scope")
        private String scope;
        
        @JsonProperty("client_secret_expires_at")
        private Long clientSecretExpiresAt;

        // Getters and setters
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public List<String> getRedirectUris() { return redirectUris; }
        public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }
        
        public List<String> getPostLogoutRedirectUris() { return postLogoutRedirectUris; }
        public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) { this.postLogoutRedirectUris = postLogoutRedirectUris; }
        
        public String getTokenEndpointAuthMethod() { return tokenEndpointAuthMethod; }
        public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) { this.tokenEndpointAuthMethod = tokenEndpointAuthMethod; }
        
        public List<String> getGrantTypes() { return grantTypes; }
        public void setGrantTypes(List<String> grantTypes) { this.grantTypes = grantTypes; }
        
        public List<String> getResponseTypes() { return responseTypes; }
        public void setResponseTypes(List<String> responseTypes) { this.responseTypes = responseTypes; }
        
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        
        public Long getClientSecretExpiresAt() { return clientSecretExpiresAt; }
        public void setClientSecretExpiresAt(Long clientSecretExpiresAt) { this.clientSecretExpiresAt = clientSecretExpiresAt; }
    }

    /**
     * Client Registration Response DTO
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientRegistrationResponse {
        @JsonProperty("client_id")
        private String clientId;
        
        @JsonProperty("client_secret")
        private String clientSecret;
        
        @JsonProperty("client_name")
        private String clientName;
        
        @JsonProperty("client_id_issued_at")
        private Long clientIdIssuedAt;
        
        @JsonProperty("client_secret_expires_at")
        private Long clientSecretExpiresAt;
        
        @JsonProperty("redirect_uris")
        private List<String> redirectUris;
        
        @JsonProperty("post_logout_redirect_uris")
        private List<String> postLogoutRedirectUris;
        
        @JsonProperty("token_endpoint_auth_method")
        private String tokenEndpointAuthMethod;
        
        @JsonProperty("grant_types")
        private List<String> grantTypes;
        
        @JsonProperty("response_types")
        private List<String> responseTypes;
        
        @JsonProperty("scope")
        private String scope;

        // Getters and setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public Long getClientIdIssuedAt() { return clientIdIssuedAt; }
        public void setClientIdIssuedAt(Long clientIdIssuedAt) { this.clientIdIssuedAt = clientIdIssuedAt; }
        
        public Long getClientSecretExpiresAt() { return clientSecretExpiresAt; }
        public void setClientSecretExpiresAt(Long clientSecretExpiresAt) { this.clientSecretExpiresAt = clientSecretExpiresAt; }
        
        public List<String> getRedirectUris() { return redirectUris; }
        public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }
        
        public List<String> getPostLogoutRedirectUris() { return postLogoutRedirectUris; }
        public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) { this.postLogoutRedirectUris = postLogoutRedirectUris; }
        
        public String getTokenEndpointAuthMethod() { return tokenEndpointAuthMethod; }
        public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) { this.tokenEndpointAuthMethod = tokenEndpointAuthMethod; }
        
        public List<String> getGrantTypes() { return grantTypes; }
        public void setGrantTypes(List<String> grantTypes) { this.grantTypes = grantTypes; }
        
        public List<String> getResponseTypes() { return responseTypes; }
        public void setResponseTypes(List<String> responseTypes) { this.responseTypes = responseTypes; }
        
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
    }

    /**
     * Error Response DTO
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorResponse {
        @JsonProperty("error")
        private String error;
        
        @JsonProperty("error_description")
        private String errorDescription;

        public ErrorResponse(String error, String errorDescription) {
            this.error = error;
            this.errorDescription = errorDescription;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    }
}