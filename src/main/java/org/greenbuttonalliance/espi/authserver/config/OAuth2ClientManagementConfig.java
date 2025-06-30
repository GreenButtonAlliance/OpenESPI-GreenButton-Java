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

package org.greenbuttonalliance.espi.authserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration for OAuth2 Client Management features
 * 
 * Enables and configures:
 * - Scheduled tasks for metrics calculation
 * - Password encoding for client secrets
 * - Client management security settings
 * - Rate limiting and session management defaults
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "espi.oauth2.client-management")
@Validated
public class OAuth2ClientManagementConfig {

    @NotNull
    @Min(1)
    @Max(100)
    private Integer defaultRateLimitPerMinute = 100;

    @NotNull
    @Min(1)
    @Max(1000)
    private Integer defaultMaxConcurrentSessions = 5;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer maxFailuresBeforeLock = 10;

    @NotNull
    @Min(1)
    @Max(43200) // 30 days in minutes
    private Integer defaultLockDurationMinutes = 60;

    @NotNull
    @Min(1)
    @Max(3650) // 10 years
    private Integer metricsRetentionDays = 365;

    private boolean enableAutomaticMetricsCalculation = true;
    private boolean enableAutomaticCleanup = true;
    private boolean enableSecurityMonitoring = true;

    /**
     * Password encoder for client secrets
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strong password encoding
    }

    // Getters and setters
    public Integer getDefaultRateLimitPerMinute() {
        return defaultRateLimitPerMinute;
    }

    public void setDefaultRateLimitPerMinute(Integer defaultRateLimitPerMinute) {
        this.defaultRateLimitPerMinute = defaultRateLimitPerMinute;
    }

    public Integer getDefaultMaxConcurrentSessions() {
        return defaultMaxConcurrentSessions;
    }

    public void setDefaultMaxConcurrentSessions(Integer defaultMaxConcurrentSessions) {
        this.defaultMaxConcurrentSessions = defaultMaxConcurrentSessions;
    }

    public Integer getMaxFailuresBeforeLock() {
        return maxFailuresBeforeLock;
    }

    public void setMaxFailuresBeforeLock(Integer maxFailuresBeforeLock) {
        this.maxFailuresBeforeLock = maxFailuresBeforeLock;
    }

    public Integer getDefaultLockDurationMinutes() {
        return defaultLockDurationMinutes;
    }

    public void setDefaultLockDurationMinutes(Integer defaultLockDurationMinutes) {
        this.defaultLockDurationMinutes = defaultLockDurationMinutes;
    }

    public Integer getMetricsRetentionDays() {
        return metricsRetentionDays;
    }

    public void setMetricsRetentionDays(Integer metricsRetentionDays) {
        this.metricsRetentionDays = metricsRetentionDays;
    }

    public boolean isEnableAutomaticMetricsCalculation() {
        return enableAutomaticMetricsCalculation;
    }

    public void setEnableAutomaticMetricsCalculation(boolean enableAutomaticMetricsCalculation) {
        this.enableAutomaticMetricsCalculation = enableAutomaticMetricsCalculation;
    }

    public boolean isEnableAutomaticCleanup() {
        return enableAutomaticCleanup;
    }

    public void setEnableAutomaticCleanup(boolean enableAutomaticCleanup) {
        this.enableAutomaticCleanup = enableAutomaticCleanup;
    }

    public boolean isEnableSecurityMonitoring() {
        return enableSecurityMonitoring;
    }

    public void setEnableSecurityMonitoring(boolean enableSecurityMonitoring) {
        this.enableSecurityMonitoring = enableSecurityMonitoring;
    }
}