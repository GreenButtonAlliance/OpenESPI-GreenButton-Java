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

import org.greenbuttonalliance.espi.authserver.controller.UserInfoController.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for providing user information for OIDC UserInfo endpoint
 * 
 * Integrates with:
 * - Local user database
 * - DataCustodian customer information
 * - ESPI-specific user attributes
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Service
public class UserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataCustodianIntegrationService dataCustodianService;
    
    // Cache for user details to reduce database calls
    private final Map<String, UserDetailsImpl> userCache = new HashMap<>();
    private static final long CACHE_EXPIRY_MS = 300000; // 5 minutes

    @Autowired
    public UserInfoService(JdbcTemplate jdbcTemplate, 
                          DataCustodianIntegrationService dataCustodianService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataCustodianService = dataCustodianService;
    }

    /**
     * Get user details for OIDC UserInfo endpoint
     */
    public UserDetails getUserDetails(String principalName) {
        logger.debug("Getting user details for: {}", principalName);

        // Check cache first
        UserDetailsImpl cached = userCache.get(principalName);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Returning cached user details for: {}", principalName);
            return cached;
        }

        try {
            UserDetailsImpl userDetails = loadUserDetails(principalName);
            
            if (userDetails != null) {
                // Cache the result
                userCache.put(principalName, userDetails);
                return userDetails;
            }

        } catch (Exception e) {
            logger.error("Error loading user details for: {}", principalName, e);
        }

        return null;
    }

    /**
     * Load user details from database and DataCustodian
     */
    private UserDetailsImpl loadUserDetails(String principalName) {
        // First, try to load from local oauth2_user_info table
        UserDetailsImpl userDetails = loadFromLocalDatabase(principalName);
        
        // If not found locally, try to get from DataCustodian
        if (userDetails == null) {
            userDetails = loadFromDataCustodian(principalName);
        }
        
        // Enhance with DataCustodian customer information if available
        if (userDetails != null && userDetails.getCustomerId() != null) {
            enhanceWithDataCustodianInfo(userDetails);
        }
        
        return userDetails;
    }

    /**
     * Load user details from local database
     */
    private UserDetailsImpl loadFromLocalDatabase(String principalName) {
        String sql = """
            SELECT username, first_name, last_name, email, email_verified,
                   locale, zone_info, customer_id, customer_type, 
                   account_number, service_territory, updated_at
            FROM oauth2_user_info 
            WHERE username = ? OR email = ?
            """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                UserDetailsImpl details = new UserDetailsImpl();
                details.setUsername(rs.getString("username"));
                details.setFirstName(rs.getString("first_name"));
                details.setLastName(rs.getString("last_name"));
                details.setEmail(rs.getString("email"));
                details.setEmailVerified(rs.getBoolean("email_verified"));
                details.setLocale(rs.getString("locale"));
                details.setZoneInfo(rs.getString("zone_info"));
                details.setCustomerId(rs.getString("customer_id"));
                details.setCustomerType(rs.getString("customer_type"));
                details.setAccountNumber(rs.getString("account_number"));
                details.setServiceTerritory(rs.getString("service_territory"));
                details.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                    rs.getTimestamp("updated_at").toInstant() : Instant.now());
                details.setCachedAt(Instant.now());
                return details;
            }, principalName, principalName);

        } catch (Exception e) {
            logger.debug("User not found in local database: {}", principalName);
            return null;
        }
    }

    /**
     * Load user details from DataCustodian
     */
    private UserDetailsImpl loadFromDataCustodian(String principalName) {
        try {
            // Try to verify user with DataCustodian (without password)
            // This is for cases where the user is authenticated via other means
            DataCustodianIntegrationService.RetailCustomerInfo customerInfo = 
                dataCustodianService.getRetailCustomer(principalName);
            
            if (customerInfo != null) {
                UserDetailsImpl details = new UserDetailsImpl();
                details.setUsername(customerInfo.getUsername());
                details.setFirstName(customerInfo.getFirstName());
                details.setLastName(customerInfo.getLastName());
                details.setEmail(customerInfo.getEmail());
                details.setEmailVerified(true); // Assume verified if from DataCustodian
                details.setCustomerId(customerInfo.getCustomerId());
                details.setCustomerType(customerInfo.getCustomerType());
                details.setAccountNumber(customerInfo.getAccountNumber());
                details.setServiceTerritory(customerInfo.getServiceTerritory());
                details.setUpdatedAt(Instant.now());
                details.setCachedAt(Instant.now());
                
                // Save to local database for future use
                saveUserDetails(details);
                
                return details;
            }

        } catch (Exception e) {
            logger.debug("User not found in DataCustodian: {}", principalName);
        }

        return null;
    }

    /**
     * Enhance user details with additional DataCustodian information
     */
    private void enhanceWithDataCustodianInfo(UserDetailsImpl userDetails) {
        try {
            if (userDetails.getCustomerId() != null) {
                DataCustodianIntegrationService.RetailCustomerInfo customerInfo = 
                    dataCustodianService.getRetailCustomer(userDetails.getCustomerId());
                
                if (customerInfo != null) {
                    // Update with latest information from DataCustodian
                    if (customerInfo.getFirstName() != null) {
                        userDetails.setFirstName(customerInfo.getFirstName());
                    }
                    if (customerInfo.getLastName() != null) {
                        userDetails.setLastName(customerInfo.getLastName());
                    }
                    if (customerInfo.getEmail() != null) {
                        userDetails.setEmail(customerInfo.getEmail());
                    }
                    if (customerInfo.getServiceTerritory() != null) {
                        userDetails.setServiceTerritory(customerInfo.getServiceTerritory());
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to enhance user details with DataCustodian info", e);
        }
    }

    /**
     * Save user details to local database
     */
    private void saveUserDetails(UserDetailsImpl userDetails) {
        String sql = """
            INSERT INTO oauth2_user_info 
            (username, first_name, last_name, email, email_verified, locale, zone_info,
             customer_id, customer_type, account_number, service_territory, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (username) DO UPDATE SET
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                email = EXCLUDED.email,
                email_verified = EXCLUDED.email_verified,
                locale = EXCLUDED.locale,
                zone_info = EXCLUDED.zone_info,
                customer_id = EXCLUDED.customer_id,
                customer_type = EXCLUDED.customer_type,
                account_number = EXCLUDED.account_number,
                service_territory = EXCLUDED.service_territory,
                updated_at = EXCLUDED.updated_at
            """;

        try {
            jdbcTemplate.update(sql,
                userDetails.getUsername(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getEmail(),
                userDetails.isEmailVerified(),
                userDetails.getLocale(),
                userDetails.getZoneInfo(),
                userDetails.getCustomerId(),
                userDetails.getCustomerType(),
                userDetails.getAccountNumber(),
                userDetails.getServiceTerritory(),
                userDetails.getUpdatedAt()
            );
            
            logger.debug("Saved user details for: {}", userDetails.getUsername());

        } catch (Exception e) {
            logger.error("Failed to save user details", e);
        }
    }

    /**
     * Update user details in cache and database
     */
    public void updateUserDetails(String principalName, UserDetailsImpl userDetails) {
        userDetails.setCachedAt(Instant.now());
        userDetails.setUpdatedAt(Instant.now());
        
        // Update cache
        userCache.put(principalName, userDetails);
        
        // Update database
        saveUserDetails(userDetails);
    }

    /**
     * Clear user from cache
     */
    public void clearUserCache(String principalName) {
        userCache.remove(principalName);
    }

    /**
     * Clear all cached users
     */
    public void clearAllCache() {
        userCache.clear();
    }

    /**
     * Implementation of UserDetails interface
     */
    public static class UserDetailsImpl implements UserDetails {
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private boolean emailVerified;
        private String locale;
        private String zoneInfo;
        private String customerId;
        private String customerType;
        private String accountNumber;
        private String serviceTerritory;
        private Instant updatedAt;
        private Instant cachedAt;

        public boolean isExpired() {
            return cachedAt != null && 
                   Instant.now().isAfter(cachedAt.plusMillis(CACHE_EXPIRY_MS));
        }

        @Override
        public String getFullName() {
            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            } else if (firstName != null) {
                return firstName;
            } else if (lastName != null) {
                return lastName;
            } else {
                return username;
            }
        }

        // Getters and setters
        @Override
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        @Override
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        @Override
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        @Override
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        @Override
        public boolean isEmailVerified() { return emailVerified; }
        public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

        @Override
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }

        @Override
        public String getZoneInfo() { return zoneInfo; }
        public void setZoneInfo(String zoneInfo) { this.zoneInfo = zoneInfo; }

        @Override
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        @Override
        public String getCustomerType() { return customerType; }
        public void setCustomerType(String customerType) { this.customerType = customerType; }

        @Override
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        @Override
        public String getServiceTerritory() { return serviceTerritory; }
        public void setServiceTerritory(String serviceTerritory) { this.serviceTerritory = serviceTerritory; }

        @Override
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

        public Instant getCachedAt() { return cachedAt; }
        public void setCachedAt(Instant cachedAt) { this.cachedAt = cachedAt; }
    }
}