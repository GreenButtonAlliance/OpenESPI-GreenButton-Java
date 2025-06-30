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

package org.greenbuttonalliance.espi.authserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.*;

/**
 * Service for integrating with OpenESPI-DataCustodian-java
 * 
 * Provides functionality for:
 * - User authentication verification
 * - Retail customer management
 * - Usage point authorization
 * - Data access scope validation
 * - ESPI resource management
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Service
public class DataCustodianIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(DataCustodianIntegrationService.class);

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${espi.datacustodian.base-url:http://localhost:8080/DataCustodian}")
    private String dataCustodianBaseUrl;

    @Value("${espi.datacustodian.admin.client-id:data_custodian_admin}")
    private String adminClientId;

    @Value("${espi.datacustodian.admin.client-secret:}")
    private String adminClientSecret;

    @Value("${espi.datacustodian.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${espi.datacustodian.read-timeout:10000}")
    private int readTimeout;

    @Autowired
    public DataCustodianIntegrationService(
            RestTemplate restTemplate,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Verify user credentials with DataCustodian
     */
    public UserVerificationResult verifyUser(String username, String password) {
        logger.debug("Verifying user credentials for: {}", username);

        try {
            String url = dataCustodianBaseUrl + "/api/v1/auth/verify";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Admin-Client-Id", adminClientId);
            headers.set("X-Admin-Client-Secret", adminClientSecret);

            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("password", password);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                UserVerificationResult result = new UserVerificationResult();
                result.setValid(true);
                result.setUserId((String) body.get("userId"));
                result.setUsername((String) body.get("username"));
                result.setEmail((String) body.get("email"));
                result.setRoles((List<String>) body.get("roles"));
                result.setCustomerType((String) body.get("customerType"));
                
                return result;
            }

        } catch (Exception e) {
            logger.error("Error verifying user credentials for: {}", username, e);
        }

        UserVerificationResult result = new UserVerificationResult();
        result.setValid(false);
        return result;
    }

    /**
     * Get retail customer information
     */
    public RetailCustomerInfo getRetailCustomer(String customerId) {
        logger.debug("Getting retail customer info for: {}", customerId);

        try {
            String url = dataCustodianBaseUrl + "/api/v1/customers/" + customerId;
            
            HttpHeaders headers = createAdminHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                RetailCustomerInfo info = new RetailCustomerInfo();
                info.setCustomerId((String) body.get("customerId"));
                info.setUsername((String) body.get("username"));
                info.setFirstName((String) body.get("firstName"));
                info.setLastName((String) body.get("lastName"));
                info.setEmail((String) body.get("email"));
                info.setCustomerType((String) body.get("customerType"));
                info.setAccountNumber((String) body.get("accountNumber"));
                info.setServiceTerritory((String) body.get("serviceTerritory"));
                
                return info;
            }

        } catch (Exception e) {
            logger.error("Error getting retail customer info for: {}", customerId, e);
        }

        return null;
    }

    /**
     * Get usage points for a customer
     */
    public List<UsagePointInfo> getUsagePoints(String customerId) {
        logger.debug("Getting usage points for customer: {}", customerId);

        try {
            String url = dataCustodianBaseUrl + "/api/v1/customers/" + customerId + "/usage-points";
            
            HttpHeaders headers = createAdminHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> usagePointData = response.getBody();
                List<UsagePointInfo> usagePoints = new ArrayList<>();
                
                for (Map<String, Object> data : usagePointData) {
                    UsagePointInfo info = new UsagePointInfo();
                    info.setUsagePointId((String) data.get("usagePointId"));
                    info.setUsagePointUUID((String) data.get("uuid"));
                    info.setServiceCategory((String) data.get("serviceCategory"));
                    info.setServiceKind((String) data.get("serviceKind"));
                    info.setCustomerId((String) data.get("customerId"));
                    info.setMeterNumber((String) data.get("meterNumber"));
                    info.setServiceAddress((String) data.get("serviceAddress"));
                    info.setStatus((String) data.get("status"));
                    
                    usagePoints.add(info);
                }
                
                return usagePoints;
            }

        } catch (Exception e) {
            logger.error("Error getting usage points for customer: {}", customerId, e);
        }

        return new ArrayList<>();
    }

    /**
     * Validate scope access for a customer and usage points
     */
    public ScopeValidationResult validateScopeAccess(String customerId, String scope, 
                                                    List<String> usagePointIds) {
        logger.debug("Validating scope access for customer: {}, scope: {}", customerId, scope);

        try {
            String url = dataCustodianBaseUrl + "/api/v1/auth/validate-scope";
            
            HttpHeaders headers = createAdminHeaders();
            
            Map<String, Object> request = new HashMap<>();
            request.put("customerId", customerId);
            request.put("scope", scope);
            request.put("usagePointIds", usagePointIds);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                ScopeValidationResult result = new ScopeValidationResult();
                result.setValid((Boolean) body.get("valid"));
                result.setGrantedScope((String) body.get("grantedScope"));
                result.setAuthorizedUsagePoints((List<String>) body.get("authorizedUsagePoints"));
                result.setDeniedUsagePoints((List<String>) body.get("deniedUsagePoints"));
                result.setReasons((List<String>) body.get("reasons"));
                
                return result;
            }

        } catch (Exception e) {
            logger.error("Error validating scope access for customer: {}", customerId, e);
        }

        ScopeValidationResult result = new ScopeValidationResult();
        result.setValid(false);
        result.setReasons(Arrays.asList("Failed to validate scope access"));
        return result;
    }

    /**
     * Create authorization grant in DataCustodian
     */
    public AuthorizationGrantResult createAuthorizationGrant(String customerId, String clientId, 
                                                           String scope, List<String> usagePointIds) {
        logger.debug("Creating authorization grant for customer: {}, client: {}", customerId, clientId);

        try {
            String url = dataCustodianBaseUrl + "/api/v1/auth/grants";
            
            HttpHeaders headers = createAdminHeaders();
            
            Map<String, Object> request = new HashMap<>();
            request.put("customerId", customerId);
            request.put("clientId", clientId);
            request.put("scope", scope);
            request.put("usagePointIds", usagePointIds);
            request.put("grantedAt", Instant.now().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                AuthorizationGrantResult result = new AuthorizationGrantResult();
                result.setGrantId((String) body.get("grantId"));
                result.setCustomerId((String) body.get("customerId"));
                result.setClientId((String) body.get("clientId"));
                result.setScope((String) body.get("scope"));
                result.setUsagePointIds((List<String>) body.get("usagePointIds"));
                result.setGrantedAt(Instant.parse((String) body.get("grantedAt")));
                result.setExpiresAt(body.get("expiresAt") != null ? 
                    Instant.parse((String) body.get("expiresAt")) : null);
                
                return result;
            }

        } catch (Exception e) {
            logger.error("Error creating authorization grant for customer: {}", customerId, e);
        }

        return null;
    }

    /**
     * Revoke authorization grant
     */
    public boolean revokeAuthorizationGrant(String grantId, String reason) {
        logger.debug("Revoking authorization grant: {}", grantId);

        try {
            String url = dataCustodianBaseUrl + "/api/v1/auth/grants/" + grantId + "/revoke";
            
            HttpHeaders headers = createAdminHeaders();
            
            Map<String, String> request = new HashMap<>();
            request.put("reason", reason);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            logger.error("Error revoking authorization grant: {}", grantId, e);
            return false;
        }
    }

    /**
     * Get DataCustodian health status
     */
    public DataCustodianHealth getHealthStatus() {
        logger.debug("Checking DataCustodian health status");

        try {
            String url = dataCustodianBaseUrl + "/api/v1/health";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                DataCustodianHealth health = new DataCustodianHealth();
                health.setStatus((String) body.get("status"));
                health.setVersion((String) body.get("version"));
                health.setUptime((Long) body.get("uptime"));
                health.setDatabaseConnected((Boolean) body.get("databaseConnected"));
                health.setActiveConnections((Integer) body.get("activeConnections"));
                health.setTotalCustomers((Integer) body.get("totalCustomers"));
                health.setTotalUsagePoints((Integer) body.get("totalUsagePoints"));
                
                return health;
            }

        } catch (Exception e) {
            logger.error("Error checking DataCustodian health status", e);
        }

        DataCustodianHealth health = new DataCustodianHealth();
        health.setStatus("DOWN");
        return health;
    }

    /**
     * Store DataCustodian integration mapping
     */
    public void storeIntegrationMapping(String authorizationId, String grantId, 
                                      String customerId, List<String> usagePointIds) {
        String sql = """
            INSERT INTO datacustodian_integration_mapping 
            (authorization_id, grant_id, customer_id, usage_point_ids, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (authorization_id) DO UPDATE SET
                grant_id = EXCLUDED.grant_id,
                customer_id = EXCLUDED.customer_id,
                usage_point_ids = EXCLUDED.usage_point_ids,
                updated_at = CURRENT_TIMESTAMP
            """;

        try {
            String usagePointIdsJson = objectMapper.writeValueAsString(usagePointIds);
            jdbcTemplate.update(sql, authorizationId, grantId, customerId, 
                              usagePointIdsJson, Instant.now());
        } catch (Exception e) {
            logger.error("Error storing integration mapping", e);
        }
    }

    /**
     * Get integration mapping
     */
    public IntegrationMapping getIntegrationMapping(String authorizationId) {
        String sql = """
            SELECT authorization_id, grant_id, customer_id, usage_point_ids, created_at, updated_at
            FROM datacustodian_integration_mapping
            WHERE authorization_id = ?
            """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                IntegrationMapping mapping = new IntegrationMapping();
                mapping.setAuthorizationId(rs.getString("authorization_id"));
                mapping.setGrantId(rs.getString("grant_id"));
                mapping.setCustomerId(rs.getString("customer_id"));
                
                try {
                    String usagePointIdsJson = rs.getString("usage_point_ids");
                    List<String> usagePointIds = objectMapper.readValue(
                        usagePointIdsJson, List.class);
                    mapping.setUsagePointIds(usagePointIds);
                } catch (Exception e) {
                    mapping.setUsagePointIds(new ArrayList<>());
                }
                
                mapping.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                mapping.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                    rs.getTimestamp("updated_at").toInstant() : null);
                
                return mapping;
            }, authorizationId);
        } catch (Exception e) {
            logger.debug("No integration mapping found for authorization: {}", authorizationId);
            return null;
        }
    }

    private HttpHeaders createAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Admin-Client-Id", adminClientId);
        headers.set("X-Admin-Client-Secret", adminClientSecret);
        return headers;
    }

    // DTO Classes

    public static class UserVerificationResult {
        private boolean valid;
        private String userId;
        private String username;
        private String email;
        private List<String> roles;
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

    public static class RetailCustomerInfo {
        private String customerId;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String customerType;
        private String accountNumber;
        private String serviceTerritory;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getCustomerType() { return customerType; }
        public void setCustomerType(String customerType) { this.customerType = customerType; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getServiceTerritory() { return serviceTerritory; }
        public void setServiceTerritory(String serviceTerritory) { this.serviceTerritory = serviceTerritory; }
    }

    public static class UsagePointInfo {
        private String usagePointId;
        private String usagePointUUID;
        private String serviceCategory;
        private String serviceKind;
        private String customerId;
        private String meterNumber;
        private String serviceAddress;
        private String status;

        // Getters and setters
        public String getUsagePointId() { return usagePointId; }
        public void setUsagePointId(String usagePointId) { this.usagePointId = usagePointId; }

        public String getUsagePointUUID() { return usagePointUUID; }
        public void setUsagePointUUID(String usagePointUUID) { this.usagePointUUID = usagePointUUID; }

        public String getServiceCategory() { return serviceCategory; }
        public void setServiceCategory(String serviceCategory) { this.serviceCategory = serviceCategory; }

        public String getServiceKind() { return serviceKind; }
        public void setServiceKind(String serviceKind) { this.serviceKind = serviceKind; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getMeterNumber() { return meterNumber; }
        public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }

        public String getServiceAddress() { return serviceAddress; }
        public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ScopeValidationResult {
        private boolean valid;
        private String grantedScope;
        private List<String> authorizedUsagePoints;
        private List<String> deniedUsagePoints;
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

    public static class AuthorizationGrantResult {
        private String grantId;
        private String customerId;
        private String clientId;
        private String scope;
        private List<String> usagePointIds;
        private Instant grantedAt;
        private Instant expiresAt;

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

        public Instant getGrantedAt() { return grantedAt; }
        public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }

        public Instant getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    }

    public static class DataCustodianHealth {
        private String status;
        private String version;
        private Long uptime;
        private Boolean databaseConnected;
        private Integer activeConnections;
        private Integer totalCustomers;
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

    public static class IntegrationMapping {
        private String authorizationId;
        private String grantId;
        private String customerId;
        private List<String> usagePointIds;
        private Instant createdAt;
        private Instant updatedAt;

        // Getters and setters
        public String getAuthorizationId() { return authorizationId; }
        public void setAuthorizationId(String authorizationId) { this.authorizationId = authorizationId; }

        public String getGrantId() { return grantId; }
        public void setGrantId(String grantId) { this.grantId = grantId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public List<String> getUsagePointIds() { return usagePointIds; }
        public void setUsagePointIds(List<String> usagePointIds) { this.usagePointIds = usagePointIds; }

        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }
}