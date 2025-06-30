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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for managing OAuth2 consent details and audit logging
 * 
 * Provides functionality for:
 * - Tracking detailed consent information
 * - Audit logging of consent decisions
 * - ESPI-specific consent analytics
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Service
public class ConsentService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentService.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ConsentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Record detailed consent information
     */
    public void recordConsentDetails(String clientId, String principalName, 
                                   Set<String> approvedScopes, Set<String> deniedScopes) {
        
        logger.debug("Recording consent details for user: {} client: {}", principalName, clientId);

        try {
            // Insert or update consent details
            String sql = """
                INSERT INTO oauth2_consent_details 
                (client_id, principal_name, approved_scopes, denied_scopes, consent_timestamp, espi_specific)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (client_id, principal_name) DO UPDATE SET
                    approved_scopes = EXCLUDED.approved_scopes,
                    denied_scopes = EXCLUDED.denied_scopes,
                    consent_timestamp = EXCLUDED.consent_timestamp,
                    espi_specific = EXCLUDED.espi_specific
                """;

            boolean isEspiSpecific = approvedScopes.stream().anyMatch(scope -> 
                scope.startsWith("FB=") || scope.contains("DataCustodian") || scope.contains("ThirdParty"));

            jdbcTemplate.update(sql,
                clientId,
                principalName,
                String.join(",", approvedScopes),
                deniedScopes != null ? String.join(",", deniedScopes) : "",
                Instant.now(),
                isEspiSpecific
            );

            // Log audit event
            logConsentEvent(clientId, principalName, approvedScopes, deniedScopes, "CONSENT_GRANTED");

        } catch (Exception e) {
            logger.error("Failed to record consent details", e);
            
            // Still log the audit event even if detailed storage fails
            try {
                logConsentEvent(clientId, principalName, approvedScopes, deniedScopes, "CONSENT_GRANT_ERROR");
            } catch (Exception auditError) {
                logger.error("Failed to log consent audit event", auditError);
            }
        }
    }

    /**
     * Record consent denial
     */
    public void recordConsentDenial(String clientId, String principalName, Set<String> requestedScopes) {
        logger.debug("Recording consent denial for user: {} client: {}", principalName, clientId);

        try {
            logConsentEvent(clientId, principalName, Collections.emptySet(), requestedScopes, "CONSENT_DENIED");
        } catch (Exception e) {
            logger.error("Failed to record consent denial", e);
        }
    }

    /**
     * Get consent history for a user
     */
    public List<ConsentRecord> getConsentHistory(String principalName) {
        String sql = """
            SELECT client_id, principal_name, approved_scopes, denied_scopes, 
                   consent_timestamp, espi_specific
            FROM oauth2_consent_details 
            WHERE principal_name = ?
            ORDER BY consent_timestamp DESC
            """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                ConsentRecord record = new ConsentRecord();
                record.setClientId(rs.getString("client_id"));
                record.setPrincipalName(rs.getString("principal_name"));
                record.setApprovedScopes(parseScopes(rs.getString("approved_scopes")));
                record.setDeniedScopes(parseScopes(rs.getString("denied_scopes")));
                record.setConsentTimestamp(rs.getTimestamp("consent_timestamp").toInstant());
                record.setEspiSpecific(rs.getBoolean("espi_specific"));
                return record;
            }, principalName);

        } catch (Exception e) {
            logger.error("Failed to get consent history for user: {}", principalName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get consent statistics for analytics
     */
    public ConsentStatistics getConsentStatistics() {
        try {
            String sql = """
                SELECT 
                    COUNT(*) as total_consents,
                    COUNT(CASE WHEN espi_specific = true THEN 1 END) as espi_consents,
                    COUNT(DISTINCT principal_name) as unique_users,
                    COUNT(DISTINCT client_id) as unique_clients,
                    AVG(ARRAY_LENGTH(STRING_TO_ARRAY(approved_scopes, ','), 1)) as avg_scopes_per_consent
                FROM oauth2_consent_details
                WHERE consent_timestamp >= NOW() - INTERVAL '30 days'
                """;

            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                ConsentStatistics stats = new ConsentStatistics();
                stats.setTotalConsents(rs.getLong("total_consents"));
                stats.setEspiConsents(rs.getLong("espi_consents"));
                stats.setUniqueUsers(rs.getLong("unique_users"));
                stats.setUniqueClients(rs.getLong("unique_clients"));
                stats.setAverageScopesPerConsent(rs.getDouble("avg_scopes_per_consent"));
                return stats;
            });

        } catch (Exception e) {
            logger.error("Failed to get consent statistics", e);
            return new ConsentStatistics(); // Return empty statistics
        }
    }

    /**
     * Revoke consent for a client
     */
    public void revokeConsent(String clientId, String principalName) {
        logger.info("Revoking consent for user: {} client: {}", principalName, clientId);

        try {
            // Delete consent details
            String deleteSql = "DELETE FROM oauth2_consent_details WHERE client_id = ? AND principal_name = ?";
            jdbcTemplate.update(deleteSql, clientId, principalName);

            // Log audit event
            logConsentEvent(clientId, principalName, Collections.emptySet(), Collections.emptySet(), "CONSENT_REVOKED");

        } catch (Exception e) {
            logger.error("Failed to revoke consent", e);
        }
    }

    /**
     * Log consent-related audit events
     */
    private void logConsentEvent(String clientId, String principalName, Set<String> approvedScopes, 
                                Set<String> deniedScopes, String eventType) {
        
        String sql = """
            INSERT INTO oauth2_audit_log 
            (event_type, client_id, principal_name, success, additional_data, event_timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("approved_scopes", approvedScopes);
        additionalData.put("denied_scopes", deniedScopes);
        additionalData.put("scope_count", approvedScopes.size());
        additionalData.put("espi_specific", approvedScopes.stream().anyMatch(scope -> 
            scope.startsWith("FB=") || scope.contains("DataCustodian")));

        try {
            jdbcTemplate.update(sql,
                eventType,
                clientId,
                principalName,
                "CONSENT_DENIED".equals(eventType) ? false : true,
                additionalData.toString(), // Simple string representation for now
                Instant.now()
            );

        } catch (Exception e) {
            logger.error("Failed to log consent audit event", e);
        }
    }

    /**
     * Parse comma-separated scopes string
     */
    private Set<String> parseScopes(String scopesString) {
        if (scopesString == null || scopesString.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(scopesString.split(",")));
    }

    /**
     * Consent record data class
     */
    public static class ConsentRecord {
        private String clientId;
        private String principalName;
        private Set<String> approvedScopes;
        private Set<String> deniedScopes;
        private Instant consentTimestamp;
        private boolean espiSpecific;

        // Getters and setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getPrincipalName() { return principalName; }
        public void setPrincipalName(String principalName) { this.principalName = principalName; }

        public Set<String> getApprovedScopes() { return approvedScopes; }
        public void setApprovedScopes(Set<String> approvedScopes) { this.approvedScopes = approvedScopes; }

        public Set<String> getDeniedScopes() { return deniedScopes; }
        public void setDeniedScopes(Set<String> deniedScopes) { this.deniedScopes = deniedScopes; }

        public Instant getConsentTimestamp() { return consentTimestamp; }
        public void setConsentTimestamp(Instant consentTimestamp) { this.consentTimestamp = consentTimestamp; }

        public boolean isEspiSpecific() { return espiSpecific; }
        public void setEspiSpecific(boolean espiSpecific) { this.espiSpecific = espiSpecific; }
    }

    /**
     * Consent statistics data class
     */
    public static class ConsentStatistics {
        private long totalConsents;
        private long espiConsents;
        private long uniqueUsers;
        private long uniqueClients;
        private double averageScopesPerConsent;

        // Getters and setters
        public long getTotalConsents() { return totalConsents; }
        public void setTotalConsents(long totalConsents) { this.totalConsents = totalConsents; }

        public long getEspiConsents() { return espiConsents; }
        public void setEspiConsents(long espiConsents) { this.espiConsents = espiConsents; }

        public long getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(long uniqueUsers) { this.uniqueUsers = uniqueUsers; }

        public long getUniqueClients() { return uniqueClients; }
        public void setUniqueClients(long uniqueClients) { this.uniqueClients = uniqueClients; }

        public double getAverageScopesPerConsent() { return averageScopesPerConsent; }
        public void setAverageScopesPerConsent(double averageScopesPerConsent) { this.averageScopesPerConsent = averageScopesPerConsent; }
    }
}