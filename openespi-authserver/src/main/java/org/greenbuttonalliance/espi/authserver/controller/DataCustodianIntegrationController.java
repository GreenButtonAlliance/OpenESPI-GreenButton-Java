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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.greenbuttonalliance.espi.authserver.service.DataCustodianIntegrationService;
import org.greenbuttonalliance.espi.authserver.service.DataCustodianIntegrationService.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for DataCustodian integration endpoints
 * 
 * Provides API endpoints for:
 * - User verification and authentication
 * - Customer and usage point management
 * - Authorization grant management
 * - Health status monitoring
 * - Integration status and diagnostics
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@RestController
@RequestMapping("/api/v1/datacustodian")
@PreAuthorize("hasRole('ADMIN') or hasRole('DC_ADMIN')")
@Validated
public class DataCustodianIntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(DataCustodianIntegrationController.class);

    private final DataCustodianIntegrationService integrationService;

    @Autowired
    public DataCustodianIntegrationController(DataCustodianIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /**
     * Verify user credentials
     * 
     * POST /api/v1/datacustodian/verify-user
     */
    @PostMapping("/verify-user")
    public ResponseEntity<?> verifyUser(@Valid @RequestBody UserVerificationRequest request) {
        logger.debug("Verifying user: {}", request.getUsername());

        try {
            UserVerificationResult result = integrationService.verifyUser(
                request.getUsername(), request.getPassword());

            if (result.isValid()) {
                UserVerificationResponse response = new UserVerificationResponse();
                response.setValid(true);
                response.setUserId(result.getUserId());
                response.setUsername(result.getUsername());
                response.setEmail(result.getEmail());
                response.setRoles(result.getRoles());
                response.setCustomerType(result.getCustomerType());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("invalid_credentials", "Invalid username or password"));
            }

        } catch (Exception e) {
            logger.error("Error verifying user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", "Failed to verify user"));
        }
    }

    /**
     * Get retail customer information
     * 
     * GET /api/v1/datacustodian/customers/{customerId}
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<?> getCustomer(@PathVariable @NotBlank String customerId) {
        logger.debug("Getting customer: {}", customerId);

        try {
            RetailCustomerInfo customerInfo = integrationService.getRetailCustomer(customerId);
            
            if (customerInfo != null) {
                return ResponseEntity.ok(customerInfo);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error getting customer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", "Failed to retrieve customer"));
        }
    }

    /**
     * Get usage points for a customer
     * 
     * GET /api/v1/datacustodian/customers/{customerId}/usage-points
     */
    @GetMapping("/customers/{customerId}/usage-points")
    public ResponseEntity<?> getUsagePoints(@PathVariable @NotBlank String customerId) {
        logger.debug("Getting usage points for customer: {}", customerId);

        try {
            List<UsagePointInfo> usagePoints = integrationService.getUsagePoints(customerId);
            
            UsagePointsResponse response = new UsagePointsResponse();
            response.setCustomerId(customerId);
            response.setUsagePoints(usagePoints);
            response.setTotalCount(usagePoints.size());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting usage points", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", "Failed to retrieve usage points"));
        }
    }

    /**
     * Validate scope access
     * 
     * POST /api/v1/datacustodian/validate-scope
     */
    @PostMapping("/validate-scope")
    public ResponseEntity<?> validateScope(@Valid @RequestBody ScopeValidationRequest request) {
        logger.debug("Validating scope for customer: {}", request.getCustomerId());

        try {
            ScopeValidationResult result = integrationService.validateScopeAccess(
                request.getCustomerId(), request.getScope(), request.getUsagePointIds());

            ScopeValidationResponse response = new ScopeValidationResponse();
            response.setValid(result.isValid());
            response.setGrantedScope(result.getGrantedScope());
            response.setAuthorizedUsagePoints(result.getAuthorizedUsagePoints());
            response.setDeniedUsagePoints(result.getDeniedUsagePoints());
            response.setReasons(result.getReasons());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating scope", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", "Failed to validate scope"));
        }
    }

    /**
     * Create authorization grant
     * 
     * POST /api/v1/datacustodian/grants
     */
    @PostMapping("/grants")
    public ResponseEntity<?> createGrant(@Valid @RequestBody CreateGrantRequest request) {
        logger.debug("Creating grant for customer: {}, client: {}", 
                    request.getCustomerId(), request.getClientId());

        try {
            AuthorizationGrantResult result = integrationService.createAuthorizationGrant(
                request.getCustomerId(), request.getClientId(), 
                request.getScope(), request.getUsagePointIds());

            if (result != null) {
                CreateGrantResponse response = new CreateGrantResponse();
                response.setGrantId(result.getGrantId());
                response.setCustomerId(result.getCustomerId());
                response.setClientId(result.getClientId());
                response.setScope(result.getScope());
                response.setUsagePointIds(result.getUsagePointIds());
                response.setGrantedAt(result.getGrantedAt());
                response.setExpiresAt(result.getExpiresAt());
                
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("grant_creation_failed", "Failed to create authorization grant"));
            }

        } catch (Exception e) {
            logger.error("Error creating grant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", "Failed to create grant"));
        }
    }

    /**
     * Revoke authorization grant
     * 
     * DELETE /api/v1/datacustodian/grants/{grantId}
     */
    @DeleteMapping("/grants/{grantId}")
    public ResponseEntity<?> revokeGrant(
            @PathVariable @NotBlank String grantId,
            @RequestParam(required = false) String reason) {
        
        logger.debug("Revoking grant: {}", grantId);

        try {
            boolean success = integrationService.revokeAuthorizationGrant(grantId, reason);
            
            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "revoked");
                response.put("grantId", grantId);
                response.put("message", "Grant revoked successfully");
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("revocation_failed", "Failed to revoke grant"));
            }

        } catch (Exception e) {
            logger.error("Error revoking grant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", "Failed to revoke grant"));
        }
    }

    /**
     * Get DataCustodian health status
     * 
     * GET /api/v1/datacustodian/health
     */
    @GetMapping("/health")
    @PreAuthorize("permitAll()") // Allow health checks without authentication
    public ResponseEntity<?> getHealth() {
        logger.debug("Checking DataCustodian health");

        try {
            DataCustodianHealth health = integrationService.getHealthStatus();
            
            HealthResponse response = new HealthResponse();
            response.setStatus(health.getStatus());
            response.setVersion(health.getVersion());
            response.setUptime(health.getUptime());
            response.setDatabaseConnected(health.getDatabaseConnected());
            response.setActiveConnections(health.getActiveConnections());
            response.setTotalCustomers(health.getTotalCustomers());
            response.setTotalUsagePoints(health.getTotalUsagePoints());
            
            if ("UP".equals(health.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }

        } catch (Exception e) {
            logger.error("Error checking health", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("health_check_failed", "Failed to check DataCustodian health"));
        }
    }

    /**
     * Get integration mapping for an authorization
     * 
     * GET /api/v1/datacustodian/mappings/{authorizationId}
     */
    @GetMapping("/mappings/{authorizationId}")
    public ResponseEntity<?> getMapping(@PathVariable @NotBlank String authorizationId) {
        logger.debug("Getting integration mapping for authorization: {}", authorizationId);

        try {
            IntegrationMapping mapping = integrationService.getIntegrationMapping(authorizationId);
            
            if (mapping != null) {
                return ResponseEntity.ok(mapping);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error getting mapping", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", "Failed to retrieve mapping"));
        }
    }

    // DTO Classes

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserVerificationRequest {
        @NotBlank(message = "Username is required")
        @JsonProperty("username")
        private String username;

        @NotBlank(message = "Password is required")
        @JsonProperty("password")
        private String password;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserVerificationResponse {
        @JsonProperty("valid")
        private boolean valid;

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("username")
        private String username;

        @JsonProperty("email")
        private String email;

        @JsonProperty("roles")
        private List<String> roles;

        @JsonProperty("customer_type")
        private String customerType;

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }

        public String getCustomerType() { return customerType; }
        public void setCustomerType(String customerType) { this.customerType = customerType; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UsagePointsResponse {
        @JsonProperty("customer_id")
        private String customerId;

        @JsonProperty("usage_points")
        private List<UsagePointInfo> usagePoints;

        @JsonProperty("total_count")
        private int totalCount;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public List<UsagePointInfo> getUsagePoints() { return usagePoints; }
        public void setUsagePoints(List<UsagePointInfo> usagePoints) { this.usagePoints = usagePoints; }

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScopeValidationRequest {
        @NotBlank(message = "Customer ID is required")
        @JsonProperty("customer_id")
        private String customerId;

        @NotBlank(message = "Scope is required")
        @JsonProperty("scope")
        private String scope;

        @JsonProperty("usage_point_ids")
        private List<String> usagePointIds;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public List<String> getUsagePointIds() { return usagePointIds; }
        public void setUsagePointIds(List<String> usagePointIds) { this.usagePointIds = usagePointIds; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScopeValidationResponse {
        @JsonProperty("valid")
        private boolean valid;

        @JsonProperty("granted_scope")
        private String grantedScope;

        @JsonProperty("authorized_usage_points")
        private List<String> authorizedUsagePoints;

        @JsonProperty("denied_usage_points")
        private List<String> deniedUsagePoints;

        @JsonProperty("reasons")
        private List<String> reasons;

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getGrantedScope() { return grantedScope; }
        public void setGrantedScope(String grantedScope) { this.grantedScope = grantedScope; }

        public List<String> getAuthorizedUsagePoints() { return authorizedUsagePoints; }
        public void setAuthorizedUsagePoints(List<String> authorizedUsagePoints) { this.authorizedUsagePoints = authorizedUsagePoints; }

        public List<String> getDeniedUsagePoints() { return deniedUsagePoints; }
        public void setDeniedUsagePoints(List<String> deniedUsagePoints) { this.deniedUsagePoints = deniedUsagePoints; }

        public List<String> getReasons() { return reasons; }
        public void setReasons(List<String> reasons) { this.reasons = reasons; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateGrantRequest {
        @NotBlank(message = "Customer ID is required")
        @JsonProperty("customer_id")
        private String customerId;

        @NotBlank(message = "Client ID is required")
        @JsonProperty("client_id")
        private String clientId;

        @NotBlank(message = "Scope is required")
        @JsonProperty("scope")
        private String scope;

        @NotNull(message = "Usage point IDs are required")
        @JsonProperty("usage_point_ids")
        private List<String> usagePointIds;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public List<String> getUsagePointIds() { return usagePointIds; }
        public void setUsagePointIds(List<String> usagePointIds) { this.usagePointIds = usagePointIds; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateGrantResponse {
        @JsonProperty("grant_id")
        private String grantId;

        @JsonProperty("customer_id")
        private String customerId;

        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("usage_point_ids")
        private List<String> usagePointIds;

        @JsonProperty("granted_at")
        private java.time.Instant grantedAt;

        @JsonProperty("expires_at")
        private java.time.Instant expiresAt;

        // Getters and setters
        public String getGrantId() { return grantId; }
        public void setGrantId(String grantId) { this.grantId = grantId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public List<String> getUsagePointIds() { return usagePointIds; }
        public void setUsagePointIds(List<String> usagePointIds) { this.usagePointIds = usagePointIds; }

        public java.time.Instant getGrantedAt() { return grantedAt; }
        public void setGrantedAt(java.time.Instant grantedAt) { this.grantedAt = grantedAt; }

        public java.time.Instant getExpiresAt() { return expiresAt; }
        public void setExpiresAt(java.time.Instant expiresAt) { this.expiresAt = expiresAt; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HealthResponse {
        @JsonProperty("status")
        private String status;

        @JsonProperty("version")
        private String version;

        @JsonProperty("uptime")
        private Long uptime;

        @JsonProperty("database_connected")
        private Boolean databaseConnected;

        @JsonProperty("active_connections")
        private Integer activeConnections;

        @JsonProperty("total_customers")
        private Integer totalCustomers;

        @JsonProperty("total_usage_points")
        private Integer totalUsagePoints;

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public Long getUptime() { return uptime; }
        public void setUptime(Long uptime) { this.uptime = uptime; }

        public Boolean getDatabaseConnected() { return databaseConnected; }
        public void setDatabaseConnected(Boolean databaseConnected) { this.databaseConnected = databaseConnected; }

        public Integer getActiveConnections() { return activeConnections; }
        public void setActiveConnections(Integer activeConnections) { this.activeConnections = activeConnections; }

        public Integer getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(Integer totalCustomers) { this.totalCustomers = totalCustomers; }

        public Integer getTotalUsagePoints() { return totalUsagePoints; }
        public void setTotalUsagePoints(Integer totalUsagePoints) { this.totalUsagePoints = totalUsagePoints; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorResponse {
        @JsonProperty("status")
        private String status = "error";

        @JsonProperty("error")
        private String error;

        @JsonProperty("message")
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}