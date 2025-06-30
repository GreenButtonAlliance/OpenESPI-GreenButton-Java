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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service for managing OAuth2 client metrics and analytics
 * 
 * Provides functionality for:
 * - Calculating daily client metrics
 * - Aggregating usage statistics
 * - Performance monitoring
 * - ESPI-specific analytics
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Service
public class ClientMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(ClientMetricsService.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ClientMetricsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Calculate and store daily metrics for all clients
     * Runs automatically every day at 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void calculateDailyMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        logger.info("Calculating daily metrics for {}", yesterday);

        try {
            calculateDailyMetrics(yesterday);
            logger.info("Successfully calculated daily metrics for {}", yesterday);
        } catch (Exception e) {
            logger.error("Failed to calculate daily metrics for {}", yesterday, e);
        }
    }

    /**
     * Calculate metrics for a specific date
     */
    public void calculateDailyMetrics(LocalDate date) {
        String sql = """
            INSERT INTO oauth2_client_metrics (
                client_id, metric_date, total_requests, successful_requests, failed_requests,
                total_tokens_issued, avg_response_time_ms, unique_users_served,
                espi_data_requests, consent_grants, consent_withdrawals
            )
            SELECT 
                au.client_id,
                ? as metric_date,
                COUNT(*) as total_requests,
                SUM(CASE WHEN au.success = true THEN 1 ELSE 0 END) as successful_requests,
                SUM(CASE WHEN au.success = false THEN 1 ELSE 0 END) as failed_requests,
                COALESCE((
                    SELECT COUNT(*) 
                    FROM oauth2_authorization oa 
                    WHERE oa.registered_client_id = (
                        SELECT id FROM oauth2_registered_client orc WHERE orc.client_id = au.client_id
                    ) 
                    AND DATE(oa.access_token_issued_at) = ?
                ), 0) as total_tokens_issued,
                COALESCE((
                    SELECT AVG(tu.response_time_ms) 
                    FROM oauth2_token_usage tu 
                    WHERE tu.client_id = au.client_id 
                    AND DATE(tu.usage_timestamp) = ?
                ), 0) as avg_response_time_ms,
                COUNT(DISTINCT au.principal_name) as unique_users_served,
                COALESCE((
                    SELECT COUNT(*) 
                    FROM oauth2_token_usage tu 
                    WHERE tu.client_id = au.client_id 
                    AND DATE(tu.usage_timestamp) = ?
                    AND tu.espi_resource_type IS NOT NULL
                ), 0) as espi_data_requests,
                COALESCE((
                    SELECT COUNT(*) 
                    FROM oauth2_consent_details cd 
                    WHERE cd.registered_client_id = (
                        SELECT id FROM oauth2_registered_client orc WHERE orc.client_id = au.client_id
                    )
                    AND DATE(cd.consent_timestamp) = ?
                ), 0) as consent_grants,
                COALESCE((
                    SELECT COUNT(*) 
                    FROM oauth2_consent_details cd 
                    WHERE cd.registered_client_id = (
                        SELECT id FROM oauth2_registered_client orc WHERE orc.client_id = au.client_id
                    )
                    AND DATE(cd.withdrawal_timestamp) = ?
                ), 0) as consent_withdrawals
            FROM oauth2_audit_log au
            WHERE DATE(au.event_timestamp) = ?
            GROUP BY au.client_id
            ON CONFLICT (client_id, metric_date) 
            DO UPDATE SET
                total_requests = EXCLUDED.total_requests,
                successful_requests = EXCLUDED.successful_requests,
                failed_requests = EXCLUDED.failed_requests,
                total_tokens_issued = EXCLUDED.total_tokens_issued,
                avg_response_time_ms = EXCLUDED.avg_response_time_ms,
                unique_users_served = EXCLUDED.unique_users_served,
                espi_data_requests = EXCLUDED.espi_data_requests,
                consent_grants = EXCLUDED.consent_grants,
                consent_withdrawals = EXCLUDED.consent_withdrawals
            """;

        try {
            int rowsAffected = jdbcTemplate.update(sql, 
                date, date, date, date, date, date, date);
            logger.debug("Calculated metrics for {} clients on {}", rowsAffected, date);
        } catch (Exception e) {
            logger.error("Error calculating metrics for date: {}", date, e);
            throw e;
        }
    }

    /**
     * Get client performance summary
     */
    public Map<String, Object> getClientPerformanceSummary(String clientId, int days) {
        String sql = """
            SELECT 
                SUM(total_requests) as total_requests,
                SUM(successful_requests) as successful_requests,
                SUM(failed_requests) as failed_requests,
                AVG(avg_response_time_ms) as avg_response_time,
                MAX(unique_users_served) as peak_users,
                SUM(total_tokens_issued) as total_tokens_issued,
                SUM(espi_data_requests) as total_espi_requests,
                CASE 
                    WHEN SUM(total_requests) > 0 THEN 
                        (SUM(successful_requests)::decimal / SUM(total_requests) * 100)
                    ELSE 0 
                END as success_rate_percent
            FROM oauth2_client_metrics
            WHERE client_id = ? 
            AND metric_date >= CURRENT_DATE - INTERVAL ? DAY
            """;

        try {
            return jdbcTemplate.queryForMap(sql, clientId, days);
        } catch (Exception e) {
            logger.error("Error getting performance summary for client: {}", clientId, e);
            throw e;
        }
    }

    /**
     * Get top performing clients by various metrics
     */
    public List<Map<String, Object>> getTopPerformingClients(String metric, int limit, int days) {
        String orderByClause;
        switch (metric.toLowerCase()) {
            case "requests":
                orderByClause = "SUM(total_requests) DESC";
                break;
            case "users":
                orderByClause = "MAX(unique_users_served) DESC";
                break;
            case "success_rate":
                orderByClause = "(SUM(successful_requests)::decimal / NULLIF(SUM(total_requests), 0)) DESC";
                break;
            case "tokens":
                orderByClause = "SUM(total_tokens_issued) DESC";
                break;
            default:
                orderByClause = "SUM(total_requests) DESC";
        }

        String sql = """
            SELECT 
                c.client_id,
                c.client_name,
                SUM(m.total_requests) as total_requests,
                SUM(m.successful_requests) as successful_requests,
                SUM(m.failed_requests) as failed_requests,
                MAX(m.unique_users_served) as peak_users,
                SUM(m.total_tokens_issued) as total_tokens_issued,
                AVG(m.avg_response_time_ms) as avg_response_time,
                CASE 
                    WHEN SUM(m.total_requests) > 0 THEN 
                        (SUM(m.successful_requests)::decimal / SUM(m.total_requests) * 100)
                    ELSE 0 
                END as success_rate_percent
            FROM oauth2_registered_client c
            JOIN oauth2_client_metrics m ON c.client_id = m.client_id
            WHERE m.metric_date >= CURRENT_DATE - INTERVAL ? DAY
            GROUP BY c.client_id, c.client_name
            ORDER BY """ + orderByClause + """
            LIMIT ?
            """;

        try {
            return jdbcTemplate.queryForList(sql, days, limit);
        } catch (Exception e) {
            logger.error("Error getting top performing clients", e);
            throw e;
        }
    }

    /**
     * Get security violations for monitoring
     */
    public List<Map<String, Object>> getSecurityViolations(int hours) {
        String sql = """
            SELECT 
                client_id,
                COUNT(*) as violation_count,
                MAX(event_timestamp) as latest_violation,
                array_agg(DISTINCT error_code) as error_codes
            FROM oauth2_audit_log
            WHERE success = false 
            AND event_timestamp >= CURRENT_TIMESTAMP - INTERVAL ? HOUR
            AND error_code IS NOT NULL
            GROUP BY client_id
            HAVING COUNT(*) > 5
            ORDER BY violation_count DESC
            """;

        try {
            return jdbcTemplate.queryForList(sql, hours);
        } catch (Exception e) {
            logger.error("Error getting security violations", e);
            throw e;
        }
    }

    /**
     * Update client last used timestamp
     */
    public void updateClientLastUsed(String clientId) {
        String sql = """
            UPDATE oauth2_registered_client 
            SET last_used_at = CURRENT_TIMESTAMP 
            WHERE client_id = ?
            """;

        try {
            jdbcTemplate.update(sql, clientId);
        } catch (Exception e) {
            logger.error("Error updating last used timestamp for client: {}", clientId, e);
        }
    }

    /**
     * Increment client failure count
     */
    public void incrementClientFailureCount(String clientId) {
        String sql = """
            UPDATE oauth2_registered_client 
            SET failure_count = failure_count + 1 
            WHERE client_id = ?
            """;

        try {
            jdbcTemplate.update(sql, clientId);
        } catch (Exception e) {
            logger.error("Error incrementing failure count for client: {}", clientId, e);
        }
    }

    /**
     * Reset client failure count
     */
    public void resetClientFailureCount(String clientId) {
        String sql = """
            UPDATE oauth2_registered_client 
            SET failure_count = 0, locked_until = NULL 
            WHERE client_id = ?
            """;

        try {
            jdbcTemplate.update(sql, clientId);
        } catch (Exception e) {
            logger.error("Error resetting failure count for client: {}", clientId, e);
        }
    }

    /**
     * Check if client should be locked due to excessive failures
     */
    public boolean shouldLockClient(String clientId, int maxFailures) {
        String sql = """
            SELECT failure_count 
            FROM oauth2_registered_client 
            WHERE client_id = ?
            """;

        try {
            Integer failureCount = jdbcTemplate.queryForObject(sql, Integer.class, clientId);
            return failureCount != null && failureCount >= maxFailures;
        } catch (Exception e) {
            logger.error("Error checking if client should be locked: {}", clientId, e);
            return false;
        }
    }

    /**
     * Lock client for specified duration
     */
    public void lockClient(String clientId, int lockDurationMinutes, String reason) {
        String sql = """
            UPDATE oauth2_registered_client 
            SET client_status = 'suspended', 
                locked_until = CURRENT_TIMESTAMP + INTERVAL ? MINUTE
            WHERE client_id = ?
            """;

        try {
            jdbcTemplate.update(sql, lockDurationMinutes, clientId);
            logger.warn("Locked client {} for {} minutes. Reason: {}", clientId, lockDurationMinutes, reason);
        } catch (Exception e) {
            logger.error("Error locking client: {}", clientId, e);
        }
    }

    /**
     * Get usage trends for reporting
     */
    public List<Map<String, Object>> getUsageTrends(int days) {
        String sql = """
            SELECT 
                metric_date,
                COUNT(DISTINCT client_id) as active_clients,
                SUM(total_requests) as total_requests,
                SUM(successful_requests) as successful_requests,
                SUM(failed_requests) as failed_requests,
                SUM(total_tokens_issued) as total_tokens_issued,
                SUM(unique_users_served) as total_unique_users,
                AVG(avg_response_time_ms) as avg_response_time
            FROM oauth2_client_metrics
            WHERE metric_date >= CURRENT_DATE - INTERVAL ? DAY
            GROUP BY metric_date
            ORDER BY metric_date DESC
            """;

        try {
            return jdbcTemplate.queryForList(sql, days);
        } catch (Exception e) {
            logger.error("Error getting usage trends", e);
            throw e;
        }
    }

    /**
     * Clean up old metrics data
     */
    @Scheduled(cron = "0 0 2 1 * *") // Run monthly at 2 AM on the 1st
    public void cleanupOldMetrics() {
        int retentionDays = 365; // Keep 1 year of data
        String sql = """
            DELETE FROM oauth2_client_metrics 
            WHERE metric_date < CURRENT_DATE - INTERVAL ? DAY
            """;

        try {
            int deletedRows = jdbcTemplate.update(sql, retentionDays);
            logger.info("Cleaned up {} old metric records older than {} days", deletedRows, retentionDays);
        } catch (Exception e) {
            logger.error("Error cleaning up old metrics", e);
        }
    }
}