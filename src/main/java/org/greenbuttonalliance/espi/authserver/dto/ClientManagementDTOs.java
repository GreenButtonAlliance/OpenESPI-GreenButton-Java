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

package org.greenbuttonalliance.espi.authserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects for OAuth2 Client Management API
 * 
 * Contains all DTOs used in the OAuth2ClientManagementController for
 * ESPI 4.0 compliant client management operations.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
public class ClientManagementDTOs {

    // Request DTOs

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateClientRequest {
        @NotBlank(message = "Client name is required")
        @Size(max = 200, message = "Client name must not exceed 200 characters")
        @JsonProperty("client_name")
        private String clientName;

        @JsonProperty("client_name_prefix")
        private String clientNamePrefix;

        @Size(max = 1000, message = "Client description must not exceed 1000 characters")
        @JsonProperty("client_description")
        private String clientDescription;

        @JsonProperty("authentication_methods")
        private List<String> authenticationMethods;

        @JsonProperty("grant_types")
        private List<String> grantTypes;

        @JsonProperty("redirect_uris")
        private List<@Pattern(regexp = "^https?://.*", message = "Invalid redirect URI format") String> redirectUris;

        @JsonProperty("post_logout_redirect_uris")
        private List<String> postLogoutRedirectUris;

        @JsonProperty("scopes")
        private List<String> scopes;

        @JsonProperty("require_proof_key")
        private boolean requireProofKey = true;

        @JsonProperty("require_authorization_consent")
        private boolean requireAuthorizationConsent = true;

        @JsonProperty("application_type")
        @Pattern(regexp = "^(WEB|NATIVE|ADMIN)$", message = "Application type must be WEB, NATIVE, or ADMIN")
        private String applicationType;

        @JsonProperty("security_level")
        @Pattern(regexp = "^(standard|high|maximum)$", message = "Security level must be standard, high, or maximum")
        private String securityLevel;

        @JsonProperty("security_classification")
        @Pattern(regexp = "^(public|internal|confidential|restricted)$", 
                message = "Security classification must be public, internal, confidential, or restricted")
        private String securityClassification;

        @JsonProperty("certification_status")
        @Pattern(regexp = "^(self_certified|third_party_certified|gba_certified)$",
                message = "Certification status must be self_certified, third_party_certified, or gba_certified")
        private String certificationStatus;

        @JsonProperty("business_category")
        @Size(max = 100, message = "Business category must not exceed 100 characters")
        private String businessCategory;

        @JsonProperty("service_territory")
        @Size(max = 200, message = "Service territory must not exceed 200 characters")
        private String serviceTerritory;

        @JsonProperty("rate_limit_per_minute")
        @Min(value = 1, message = "Rate limit must be at least 1 request per minute")
        @Max(value = 10000, message = "Rate limit must not exceed 10000 requests per minute")
        private Integer rateLimitPerMinute;

        @JsonProperty("max_concurrent_sessions")
        @Min(value = 1, message = "Max concurrent sessions must be at least 1")
        @Max(value = 1000, message = "Max concurrent sessions must not exceed 1000")
        private Integer maxConcurrentSessions;

        @JsonProperty("created_by")
        @Size(max = 100, message = "Created by must not exceed 100 characters")
        private String createdBy;

        // Getters and setters
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }

        public String getClientNamePrefix() { return clientNamePrefix; }
        public void setClientNamePrefix(String clientNamePrefix) { this.clientNamePrefix = clientNamePrefix; }

        public String getClientDescription() { return clientDescription; }
        public void setClientDescription(String clientDescription) { this.clientDescription = clientDescription; }

        public List<String> getAuthenticationMethods() { return authenticationMethods; }
        public void setAuthenticationMethods(List<String> authenticationMethods) { this.authenticationMethods = authenticationMethods; }

        public List<String> getGrantTypes() { return grantTypes; }
        public void setGrantTypes(List<String> grantTypes) { this.grantTypes = grantTypes; }

        public List<String> getRedirectUris() { return redirectUris; }
        public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }

        public List<String> getPostLogoutRedirectUris() { return postLogoutRedirectUris; }
        public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) { this.postLogoutRedirectUris = postLogoutRedirectUris; }

        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }

        public boolean isRequireProofKey() { return requireProofKey; }
        public void setRequireProofKey(boolean requireProofKey) { this.requireProofKey = requireProofKey; }

        public boolean isRequireAuthorizationConsent() { return requireAuthorizationConsent; }
        public void setRequireAuthorizationConsent(boolean requireAuthorizationConsent) { this.requireAuthorizationConsent = requireAuthorizationConsent; }

        public String getApplicationType() { return applicationType; }
        public void setApplicationType(String applicationType) { this.applicationType = applicationType; }

        public String getSecurityLevel() { return securityLevel; }
        public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }

        public String getSecurityClassification() { return securityClassification; }
        public void setSecurityClassification(String securityClassification) { this.securityClassification = securityClassification; }

        public String getCertificationStatus() { return certificationStatus; }
        public void setCertificationStatus(String certificationStatus) { this.certificationStatus = certificationStatus; }

        public String getBusinessCategory() { return businessCategory; }
        public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }

        public String getServiceTerritory() { return serviceTerritory; }
        public void setServiceTerritory(String serviceTerritory) { this.serviceTerritory = serviceTerritory; }

        public Integer getRateLimitPerMinute() { return rateLimitPerMinute; }
        public void setRateLimitPerMinute(Integer rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

        public Integer getMaxConcurrentSessions() { return maxConcurrentSessions; }
        public void setMaxConcurrentSessions(Integer maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateClientRequest {
        @Size(max = 200, message = "Client name must not exceed 200 characters")
        @JsonProperty("client_name")
        private String clientName;

        @JsonProperty("redirect_uris")
        private List<String> redirectUris;

        @JsonProperty("scopes")
        private List<String> scopes;

        @JsonProperty("security_level")
        @Pattern(regexp = "^(standard|high|maximum)$", message = "Security level must be standard, high, or maximum")
        private String securityLevel;

        @JsonProperty("rate_limit_per_minute")
        @Min(value = 1, message = "Rate limit must be at least 1 request per minute")
        @Max(value = 10000, message = "Rate limit must not exceed 10000 requests per minute")
        private Integer rateLimitPerMinute;

        @JsonProperty("max_concurrent_sessions")
        @Min(value = 1, message = "Max concurrent sessions must be at least 1")
        @Max(value = 1000, message = "Max concurrent sessions must not exceed 1000")
        private Integer maxConcurrentSessions;

        @JsonProperty("updated_by")
        @Size(max = 100, message = "Updated by must not exceed 100 characters")
        private String updatedBy;

        // Getters and setters
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }

        public List<String> getRedirectUris() { return redirectUris; }
        public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }

        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }

        public String getSecurityLevel() { return securityLevel; }
        public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }

        public Integer getRateLimitPerMinute() { return rateLimitPerMinute; }
        public void setRateLimitPerMinute(Integer rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

        public Integer getMaxConcurrentSessions() { return maxConcurrentSessions; }
        public void setMaxConcurrentSessions(Integer maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }

        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateClientStatusRequest {
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "^(active|suspended|revoked)$", message = "Status must be active, suspended, or revoked")
        @JsonProperty("status")
        private String status;

        @Size(max = 200, message = "Reason must not exceed 200 characters")
        @JsonProperty("reason")
        private String reason;

        @JsonProperty("lock_duration_minutes")
        @Min(value = 1, message = "Lock duration must be at least 1 minute")
        @Max(value = 43200, message = "Lock duration must not exceed 30 days (43200 minutes)")
        private Integer lockDurationMinutes;

        @JsonProperty("updated_by")
        @Size(max = 100, message = "Updated by must not exceed 100 characters")
        private String updatedBy;

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public Integer getLockDurationMinutes() { return lockDurationMinutes; }
        public void setLockDurationMinutes(Integer lockDurationMinutes) { this.lockDurationMinutes = lockDurationMinutes; }

        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkOperationRequest {
        @NotNull(message = "Client IDs are required")
        @Size(min = 1, max = 100, message = "Must specify between 1 and 100 client IDs")
        @JsonProperty("client_ids")
        private List<@NotBlank String> clientIds;

        @NotBlank(message = "Operation is required")
        @Pattern(regexp = "^(suspend|activate|delete)$", message = "Operation must be suspend, activate, or delete")
        @JsonProperty("operation")
        private String operation;

        @Size(max = 200, message = "Reason must not exceed 200 characters")
        @JsonProperty("reason")
        private String reason;

        // Getters and setters
        public List<String> getClientIds() { return clientIds; }
        public void setClientIds(List<String> clientIds) { this.clientIds = clientIds; }

        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // Response DTOs

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateClientResponse {
        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("client_secret")
        private String clientSecret;

        @JsonProperty("client_name")
        private String clientName;

        @JsonProperty("created_at")
        private Instant createdAt;

        @JsonProperty("espi_version")
        private String espiVersion;

        // Getters and setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }

        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

        public String getEspiVersion() { return espiVersion; }
        public void setEspiVersion(String espiVersion) { this.espiVersion = espiVersion; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientDetails {
        @JsonProperty("id")
        private String id;

        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("client_name")
        private String clientName;

        @JsonProperty("authentication_methods")
        private List<String> authenticationMethods;

        @JsonProperty("grant_types")
        private List<String> grantTypes;

        @JsonProperty("redirect_uris")
        private List<String> redirectUris;

        @JsonProperty("scopes")
        private List<String> scopes;

        @JsonProperty("espi_version")
        private String espiVersion;

        @JsonProperty("security_level")
        private String securityLevel;

        @JsonProperty("status")
        private String status;

        @JsonProperty("rate_limit_per_minute")
        private Integer rateLimitPerMinute;

        @JsonProperty("max_concurrent_sessions")
        private Integer maxConcurrentSessions;

        @JsonProperty("created_by")
        private String createdBy;

        @JsonProperty("last_used_at")
        private Instant lastUsedAt;

        @JsonProperty("failure_count")
        private Integer failureCount;

        @JsonProperty("business_category")
        private String businessCategory;

        @JsonProperty("certification_status")
        private String certificationStatus;

        @JsonProperty("service_territory")
        private String serviceTerritory;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }

        public List<String> getAuthenticationMethods() { return authenticationMethods; }
        public void setAuthenticationMethods(List<String> authenticationMethods) { this.authenticationMethods = authenticationMethods; }

        public List<String> getGrantTypes() { return grantTypes; }
        public void setGrantTypes(List<String> grantTypes) { this.grantTypes = grantTypes; }

        public List<String> getRedirectUris() { return redirectUris; }
        public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }

        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }

        public String getEspiVersion() { return espiVersion; }
        public void setEspiVersion(String espiVersion) { this.espiVersion = espiVersion; }

        public String getSecurityLevel() { return securityLevel; }
        public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getRateLimitPerMinute() { return rateLimitPerMinute; }
        public void setRateLimitPerMinute(Integer rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

        public Integer getMaxConcurrentSessions() { return maxConcurrentSessions; }
        public void setMaxConcurrentSessions(Integer maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

        public Instant getLastUsedAt() { return lastUsedAt; }
        public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }

        public String getBusinessCategory() { return businessCategory; }
        public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }

        public String getCertificationStatus() { return certificationStatus; }
        public void setCertificationStatus(String certificationStatus) { this.certificationStatus = certificationStatus; }

        public String getServiceTerritory() { return serviceTerritory; }
        public void setServiceTerritory(String serviceTerritory) { this.serviceTerritory = serviceTerritory; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PagedClientResponse {
        @JsonProperty("clients")
        private List<ClientDetails> clients;

        @JsonProperty("page")
        private int page;

        @JsonProperty("size")
        private int size;

        @JsonProperty("total_elements")
        private long totalElements;

        @JsonProperty("total_pages")
        private int totalPages;

        // Getters and setters
        public List<ClientDetails> getClients() { return clients; }
        public void setClients(List<ClientDetails> clients) { this.clients = clients; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientMetrics {
        @JsonProperty("date")
        private LocalDate date;

        @JsonProperty("total_requests")
        private long totalRequests;

        @JsonProperty("successful_requests")
        private long successfulRequests;

        @JsonProperty("failed_requests")
        private long failedRequests;

        @JsonProperty("total_tokens_issued")
        private long totalTokensIssued;

        @JsonProperty("avg_response_time_ms")
        private double avgResponseTimeMs;

        @JsonProperty("unique_users_served")
        private int uniqueUsersServed;

        @JsonProperty("espi_data_requests")
        private long espiDataRequests;

        @JsonProperty("consent_grants")
        private int consentGrants;

        @JsonProperty("consent_withdrawals")
        private int consentWithdrawals;

        // Getters and setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

        public long getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(long successfulRequests) { this.successfulRequests = successfulRequests; }

        public long getFailedRequests() { return failedRequests; }
        public void setFailedRequests(long failedRequests) { this.failedRequests = failedRequests; }

        public long getTotalTokensIssued() { return totalTokensIssued; }
        public void setTotalTokensIssued(long totalTokensIssued) { this.totalTokensIssued = totalTokensIssued; }

        public double getAvgResponseTimeMs() { return avgResponseTimeMs; }
        public void setAvgResponseTimeMs(double avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }

        public int getUniqueUsersServed() { return uniqueUsersServed; }
        public void setUniqueUsersServed(int uniqueUsersServed) { this.uniqueUsersServed = uniqueUsersServed; }

        public long getEspiDataRequests() { return espiDataRequests; }
        public void setEspiDataRequests(long espiDataRequests) { this.espiDataRequests = espiDataRequests; }

        public int getConsentGrants() { return consentGrants; }
        public void setConsentGrants(int consentGrants) { this.consentGrants = consentGrants; }

        public int getConsentWithdrawals() { return consentWithdrawals; }
        public void setConsentWithdrawals(int consentWithdrawals) { this.consentWithdrawals = consentWithdrawals; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientMetricsSummary {
        @JsonProperty("total_requests")
        private long totalRequests;

        @JsonProperty("total_successful_requests")
        private long totalSuccessfulRequests;

        @JsonProperty("total_failed_requests")
        private long totalFailedRequests;

        @JsonProperty("total_tokens_issued")
        private long totalTokensIssued;

        @JsonProperty("success_rate")
        private double successRate;

        @JsonProperty("average_response_time")
        private double averageResponseTime;

        @JsonProperty("total_unique_users")
        private int totalUniqueUsers;

        // Getters and setters
        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

        public long getTotalSuccessfulRequests() { return totalSuccessfulRequests; }
        public void setTotalSuccessfulRequests(long totalSuccessfulRequests) { this.totalSuccessfulRequests = totalSuccessfulRequests; }

        public long getTotalFailedRequests() { return totalFailedRequests; }
        public void setTotalFailedRequests(long totalFailedRequests) { this.totalFailedRequests = totalFailedRequests; }

        public long getTotalTokensIssued() { return totalTokensIssued; }
        public void setTotalTokensIssued(long totalTokensIssued) { this.totalTokensIssued = totalTokensIssued; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }

        public int getTotalUniqueUsers() { return totalUniqueUsers; }
        public void setTotalUniqueUsers(int totalUniqueUsers) { this.totalUniqueUsers = totalUniqueUsers; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientMetricsResponse {
        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("period_days")
        private int periodDays;

        @JsonProperty("summary")
        private ClientMetricsSummary summary;

        @JsonProperty("daily_metrics")
        private List<ClientMetrics> dailyMetrics;

        // Getters and setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public int getPeriodDays() { return periodDays; }
        public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }

        public ClientMetricsSummary getSummary() { return summary; }
        public void setSummary(ClientMetricsSummary summary) { this.summary = summary; }

        public List<ClientMetrics> getDailyMetrics() { return dailyMetrics; }
        public void setDailyMetrics(List<ClientMetrics> dailyMetrics) { this.dailyMetrics = dailyMetrics; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkOperationResult {
        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("success")
        private boolean success;

        @JsonProperty("message")
        private String message;

        public BulkOperationResult() {}

        public BulkOperationResult(String clientId, boolean success, String message) {
            this.clientId = clientId;
            this.success = success;
            this.message = message;
        }

        // Getters and setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkOperationResponse {
        @JsonProperty("operation")
        private String operation;

        @JsonProperty("total_clients")
        private int totalClients;

        @JsonProperty("success_count")
        private int successCount;

        @JsonProperty("failure_count")
        private int failureCount;

        @JsonProperty("results")
        private List<BulkOperationResult> results;

        // Getters and setters
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }

        public int getTotalClients() { return totalClients; }
        public void setTotalClients(int totalClients) { this.totalClients = totalClients; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

        public List<BulkOperationResult> getResults() { return results; }
        public void setResults(List<BulkOperationResult> results) { this.results = results; }
    }

    // Common response DTOs

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SuccessResponse {
        @JsonProperty("status")
        private String status = "success";

        @JsonProperty("message")
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
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