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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.greenbuttonalliance.espi.authserver.service.DataCustodianIntegrationService;
import org.greenbuttonalliance.espi.authserver.service.UserInfoService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OIDC UserInfo Endpoint with ESPI-specific claims
 * 
 * Implements the OpenID Connect UserInfo endpoint as specified in:
 * - OpenID Connect Core 1.0 (Section 5.3)
 * - NAESB ESPI 4.0 specification
 * 
 * Provides user information including:
 * - Standard OIDC claims (sub, name, email, etc.)
 * - ESPI-specific claims (customer_id, usage_points, etc.)
 * - Green Button Alliance extensions
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@RestController
@RequestMapping("/userinfo")
public class UserInfoController {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoController.class);

    private final OAuth2AuthorizationService authorizationService;
    private final UserInfoService userInfoService;
    private final DataCustodianIntegrationService dataCustodianService;

    @Autowired
    public UserInfoController(
            OAuth2AuthorizationService authorizationService,
            UserInfoService userInfoService,
            DataCustodianIntegrationService dataCustodianService) {
        this.authorizationService = authorizationService;
        this.userInfoService = userInfoService;
        this.dataCustodianService = dataCustodianService;
    }

    /**
     * OIDC UserInfo endpoint
     * 
     * GET /userinfo
     * POST /userinfo
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
                   produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> userInfo(Authentication authentication) {
        logger.debug("UserInfo request from: {}", authentication.getName());

        try {
            if (!(authentication instanceof JwtAuthenticationToken)) {
                logger.warn("Invalid authentication type for UserInfo request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("invalid_token", "Invalid access token"));
            }

            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            String accessToken = jwtAuth.getToken().getTokenValue();
            
            // Find the authorization for this access token
            OAuth2Authorization authorization = authorizationService.findByToken(
                accessToken, OAuth2TokenType.ACCESS_TOKEN);
            
            if (authorization == null) {
                logger.warn("No authorization found for access token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("invalid_token", "Access token not found"));
            }

            // Check if token has required scopes
            Set<String> authorizedScopes = authorization.getAuthorizedScopes();
            if (!authorizedScopes.contains("openid")) {
                logger.warn("Access token does not include openid scope");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("insufficient_scope", "Token missing required openid scope"));
            }

            // Build UserInfo response
            UserInfoResponse userInfo = buildUserInfoResponse(authorization, authorizedScopes);
            
            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            logger.error("Error processing UserInfo request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", "Internal server error"));
        }
    }

    /**
     * Build UserInfo response with OIDC and ESPI claims
     */
    private UserInfoResponse buildUserInfoResponse(OAuth2Authorization authorization, Set<String> scopes) {
        String principalName = authorization.getPrincipalName();
        String clientId = authorization.getRegisteredClientId();
        
        UserInfoResponse userInfo = new UserInfoResponse();
        
        // Required OIDC claims
        userInfo.setSub(principalName); // Subject identifier
        
        // Get user information from UserInfoService
        UserDetails userDetails = userInfoService.getUserDetails(principalName);
        
        if (userDetails != null) {
            // Standard OIDC claims (if profile scope is present)
            if (scopes.contains("profile")) {
                userInfo.setName(userDetails.getFullName());
                userInfo.setGivenName(userDetails.getFirstName());
                userInfo.setFamilyName(userDetails.getLastName());
                userInfo.setPreferredUsername(userDetails.getUsername());
                userInfo.setUpdatedAt(userDetails.getUpdatedAt());
                
                if (userDetails.getLocale() != null) {
                    userInfo.setLocale(userDetails.getLocale());
                }
                if (userDetails.getZoneInfo() != null) {
                    userInfo.setZoneinfo(userDetails.getZoneInfo());
                }
            }
            
            // Email claims (if email scope is present)
            if (scopes.contains("email")) {
                userInfo.setEmail(userDetails.getEmail());
                userInfo.setEmailVerified(userDetails.isEmailVerified());
            }
            
            // ESPI-specific claims
            addEspiClaims(userInfo, userDetails, authorization, scopes);
        }
        
        // Add authorization-specific claims
        addAuthorizationClaims(userInfo, authorization);
        
        return userInfo;
    }

    /**
     * Add ESPI-specific claims to UserInfo response
     */
    private void addEspiClaims(UserInfoResponse userInfo, UserDetails userDetails, 
                              OAuth2Authorization authorization, Set<String> scopes) {
        
        // ESPI customer information
        if (userDetails.getCustomerId() != null) {
            userInfo.setCustomerId(userDetails.getCustomerId());
            userInfo.setCustomerType(userDetails.getCustomerType());
            userInfo.setAccountNumber(userDetails.getAccountNumber());
            userInfo.setServiceTerritory(userDetails.getServiceTerritory());
        }
        
        // Get ESPI scopes
        Set<String> espiScopes = scopes.stream()
            .filter(scope -> scope.startsWith("FB="))
            .collect(Collectors.toSet());
        
        if (!espiScopes.isEmpty()) {
            userInfo.setEspiScopes(new ArrayList<>(espiScopes));
            
            // Get DataCustodian integration mapping
            DataCustodianIntegrationService.IntegrationMapping mapping = 
                dataCustodianService.getIntegrationMapping(authorization.getId());
            
            if (mapping != null) {
                userInfo.setDataCustodianGrantId(mapping.getGrantId());
                userInfo.setAuthorizedUsagePoints(mapping.getUsagePointIds());
                
                // Get usage point details if authorized
                if (mapping.getUsagePointIds() != null && !mapping.getUsagePointIds().isEmpty()) {
                    try {
                        List<DataCustodianIntegrationService.UsagePointInfo> usagePoints = 
                            dataCustodianService.getUsagePoints(mapping.getCustomerId());
                        
                        List<UsagePointInfo> authorizedUsagePoints = usagePoints.stream()
                            .filter(up -> mapping.getUsagePointIds().contains(up.getUsagePointId()))
                            .map(this::mapToUsagePointInfo)
                            .collect(Collectors.toList());
                        
                        userInfo.setUsagePointDetails(authorizedUsagePoints);
                    } catch (Exception e) {
                        logger.warn("Failed to get usage point details for customer: {}", 
                                   mapping.getCustomerId(), e);
                    }
                }
            }
        }
        
        // Green Button Alliance extensions
        userInfo.setGbaVersion("2024.1");
        userInfo.setEspiVersion("4.0");
        userInfo.setDataRights(getDataRights(scopes));
    }

    /**
     * Add authorization-specific claims
     */
    private void addAuthorizationClaims(UserInfoResponse userInfo, OAuth2Authorization authorization) {
        userInfo.setAud(authorization.getRegisteredClientId()); // Audience
        userInfo.setIss("https://authorization.greenbuttonalliance.org"); // Issuer
        userInfo.setIat(authorization.getAccessToken().getToken().getIssuedAt());
        userInfo.setExp(authorization.getAccessToken().getToken().getExpiresAt());
        
        // Authorization-specific information
        userInfo.setAuthTime(authorization.getAccessToken().getToken().getIssuedAt());
        userInfo.setScope(String.join(" ", authorization.getAuthorizedScopes()));
    }

    /**
     * Map DataCustodian UsagePointInfo to UserInfo UsagePointInfo
     */
    private UsagePointInfo mapToUsagePointInfo(DataCustodianIntegrationService.UsagePointInfo dcInfo) {
        UsagePointInfo info = new UsagePointInfo();
        info.setUsagePointId(dcInfo.getUsagePointId());
        info.setUsagePointUuid(dcInfo.getUsagePointUUID());
        info.setServiceCategory(dcInfo.getServiceCategory());
        info.setServiceKind(dcInfo.getServiceKind());
        info.setMeterNumber(dcInfo.getMeterNumber());
        info.setServiceAddress(dcInfo.getServiceAddress());
        info.setStatus(dcInfo.getStatus());
        return info;
    }

    /**
     * Determine data rights based on scopes
     */
    private List<String> getDataRights(Set<String> scopes) {
        List<String> rights = new ArrayList<>();
        
        for (String scope : scopes) {
            if (scope.startsWith("FB=")) {
                rights.add("ENERGY_USAGE_DATA");
                
                if (scope.contains("IntervalDuration=900")) {
                    rights.add("15_MINUTE_INTERVALS");
                } else if (scope.contains("IntervalDuration=3600")) {
                    rights.add("HOURLY_INTERVALS");
                }
                
                if (scope.contains("BlockDuration=monthly")) {
                    rights.add("MONTHLY_BILLING_DATA");
                } else if (scope.contains("BlockDuration=daily")) {
                    rights.add("DAILY_USAGE_DATA");
                }
            }
        }
        
        if (scopes.contains("DataCustodian_Admin_Access")) {
            rights.add("ADMIN_ACCESS");
        }
        
        if (scopes.contains("Upload_Admin_Access")) {
            rights.add("UPLOAD_ACCESS");
        }
        
        return rights;
    }

    // DTO Classes

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfoResponse {
        // Standard OIDC claims
        @JsonProperty("sub")
        private String sub;

        @JsonProperty("name")
        private String name;

        @JsonProperty("given_name")
        private String givenName;

        @JsonProperty("family_name")
        private String familyName;

        @JsonProperty("preferred_username")
        private String preferredUsername;

        @JsonProperty("email")
        private String email;

        @JsonProperty("email_verified")
        private Boolean emailVerified;

        @JsonProperty("locale")
        private String locale;

        @JsonProperty("zoneinfo")
        private String zoneinfo;

        @JsonProperty("updated_at")
        private Instant updatedAt;

        // Authorization claims
        @JsonProperty("aud")
        private String aud;

        @JsonProperty("iss")
        private String iss;

        @JsonProperty("iat")
        private Instant iat;

        @JsonProperty("exp")
        private Instant exp;

        @JsonProperty("auth_time")
        private Instant authTime;

        @JsonProperty("scope")
        private String scope;

        // ESPI-specific claims
        @JsonProperty("customer_id")
        private String customerId;

        @JsonProperty("customer_type")
        private String customerType;

        @JsonProperty("account_number")
        private String accountNumber;

        @JsonProperty("service_territory")
        private String serviceTerritory;

        @JsonProperty("espi_scopes")
        private List<String> espiScopes;

        @JsonProperty("datacustodian_grant_id")
        private String dataCustodianGrantId;

        @JsonProperty("authorized_usage_points")
        private List<String> authorizedUsagePoints;

        @JsonProperty("usage_point_details")
        private List<UsagePointInfo> usagePointDetails;

        // Green Button Alliance extensions
        @JsonProperty("gba_version")
        private String gbaVersion;

        @JsonProperty("espi_version")
        private String espiVersion;

        @JsonProperty("data_rights")
        private List<String> dataRights;

        // Getters and setters
        public String getSub() { return sub; }
        public void setSub(String sub) { this.sub = sub; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getGivenName() { return givenName; }
        public void setGivenName(String givenName) { this.givenName = givenName; }

        public String getFamilyName() { return familyName; }
        public void setFamilyName(String familyName) { this.familyName = familyName; }

        public String getPreferredUsername() { return preferredUsername; }
        public void setPreferredUsername(String preferredUsername) { this.preferredUsername = preferredUsername; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Boolean getEmailVerified() { return emailVerified; }
        public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }

        public String getZoneinfo() { return zoneinfo; }
        public void setZoneinfo(String zoneinfo) { this.zoneinfo = zoneinfo; }

        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

        public String getAud() { return aud; }
        public void setAud(String aud) { this.aud = aud; }

        public String getIss() { return iss; }
        public void setIss(String iss) { this.iss = iss; }

        public Instant getIat() { return iat; }
        public void setIat(Instant iat) { this.iat = iat; }

        public Instant getExp() { return exp; }
        public void setExp(Instant exp) { this.exp = exp; }

        public Instant getAuthTime() { return authTime; }
        public void setAuthTime(Instant authTime) { this.authTime = authTime; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getCustomerType() { return customerType; }
        public void setCustomerType(String customerType) { this.customerType = customerType; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getServiceTerritory() { return serviceTerritory; }
        public void setServiceTerritory(String serviceTerritory) { this.serviceTerritory = serviceTerritory; }

        public List<String> getEspiScopes() { return espiScopes; }
        public void setEspiScopes(List<String> espiScopes) { this.espiScopes = espiScopes; }

        public String getDataCustodianGrantId() { return dataCustodianGrantId; }
        public void setDataCustodianGrantId(String dataCustodianGrantId) { this.dataCustodianGrantId = dataCustodianGrantId; }

        public List<String> getAuthorizedUsagePoints() { return authorizedUsagePoints; }
        public void setAuthorizedUsagePoints(List<String> authorizedUsagePoints) { this.authorizedUsagePoints = authorizedUsagePoints; }

        public List<UsagePointInfo> getUsagePointDetails() { return usagePointDetails; }
        public void setUsagePointDetails(List<UsagePointInfo> usagePointDetails) { this.usagePointDetails = usagePointDetails; }

        public String getGbaVersion() { return gbaVersion; }
        public void setGbaVersion(String gbaVersion) { this.gbaVersion = gbaVersion; }

        public String getEspiVersion() { return espiVersion; }
        public void setEspiVersion(String espiVersion) { this.espiVersion = espiVersion; }

        public List<String> getDataRights() { return dataRights; }
        public void setDataRights(List<String> dataRights) { this.dataRights = dataRights; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UsagePointInfo {
        @JsonProperty("usage_point_id")
        private String usagePointId;

        @JsonProperty("usage_point_uuid")
        private String usagePointUuid;

        @JsonProperty("service_category")
        private String serviceCategory;

        @JsonProperty("service_kind")
        private String serviceKind;

        @JsonProperty("meter_number")
        private String meterNumber;

        @JsonProperty("service_address")
        private String serviceAddress;

        @JsonProperty("status")
        private String status;

        // Getters and setters
        public String getUsagePointId() { return usagePointId; }
        public void setUsagePointId(String usagePointId) { this.usagePointId = usagePointId; }

        public String getUsagePointUuid() { return usagePointUuid; }
        public void setUsagePointUuid(String usagePointUuid) { this.usagePointUuid = usagePointUuid; }

        public String getServiceCategory() { return serviceCategory; }
        public void setServiceCategory(String serviceCategory) { this.serviceCategory = serviceCategory; }

        public String getServiceKind() { return serviceKind; }
        public void setServiceKind(String serviceKind) { this.serviceKind = serviceKind; }

        public String getMeterNumber() { return meterNumber; }
        public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }

        public String getServiceAddress() { return serviceAddress; }
        public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

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

    // UserDetails interface for service layer
    public interface UserDetails {
        String getUsername();
        String getFullName();
        String getFirstName();
        String getLastName();
        String getEmail();
        boolean isEmailVerified();
        String getLocale();
        String getZoneInfo();
        Instant getUpdatedAt();
        String getCustomerId();
        String getCustomerType();
        String getAccountNumber();
        String getServiceTerritory();
    }
}