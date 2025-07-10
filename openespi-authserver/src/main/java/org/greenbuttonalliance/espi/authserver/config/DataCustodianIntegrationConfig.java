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
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration for DataCustodian integration
 * 
 * Configures:
 * - HTTP client settings for DataCustodian communication
 * - Connection timeouts and retry policies
 * - Authentication credentials
 * - Health check intervals
 * - Synchronization settings
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Configuration
@ConfigurationProperties(prefix = "espi.datacustodian")
@Validated
public class DataCustodianIntegrationConfig {

    @NotBlank
    private String baseUrl = "http://localhost:8080/DataCustodian";

    @NotBlank
    private String adminClientId = "data_custodian_admin";

    private String adminClientSecret = "";

    @NotNull
    @Min(1000)
    @Max(60000)
    private Integer connectionTimeout = 5000;

    @NotNull
    @Min(1000)
    @Max(300000)
    private Integer readTimeout = 10000;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer maxRetries = 3;

    @NotNull
    @Min(1000)
    @Max(60000)
    private Integer retryDelay = 2000;

    @NotNull
    @Min(30)
    @Max(3600)
    private Integer healthCheckInterval = 300; // 5 minutes

    @NotNull
    @Min(1)
    @Max(24)
    private Integer healthLogRetentionDays = 24;

    @NotNull
    @Min(1)
    @Max(24)
    private Integer apiLogRetentionDays = 7;

    private boolean enableHealthChecks = true;
    private boolean enableApiLogging = true;
    private boolean enableRetries = true;
    private boolean validateSslCertificates = true;

    /**
     * Configure RestTemplate for DataCustodian communication
     */
    @Bean("dataCustodianRestTemplate")
    public RestTemplate dataCustodianRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure HTTP client factory with timeouts
        ClientHttpRequestFactory factory = clientHttpRequestFactory();
        restTemplate.setRequestFactory(factory);
        
        return restTemplate;
    }

    /**
     * Configure HTTP client factory with custom timeouts
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    // Getters and setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Integer retryDelay) {
        this.retryDelay = retryDelay;
    }

    public Integer getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Integer healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public Integer getHealthLogRetentionDays() {
        return healthLogRetentionDays;
    }

    public void setHealthLogRetentionDays(Integer healthLogRetentionDays) {
        this.healthLogRetentionDays = healthLogRetentionDays;
    }

    public Integer getApiLogRetentionDays() {
        return apiLogRetentionDays;
    }

    public void setApiLogRetentionDays(Integer apiLogRetentionDays) {
        this.apiLogRetentionDays = apiLogRetentionDays;
    }

    public boolean isEnableHealthChecks() {
        return enableHealthChecks;
    }

    public void setEnableHealthChecks(boolean enableHealthChecks) {
        this.enableHealthChecks = enableHealthChecks;
    }

    public boolean isEnableApiLogging() {
        return enableApiLogging;
    }

    public void setEnableApiLogging(boolean enableApiLogging) {
        this.enableApiLogging = enableApiLogging;
    }

    public boolean isEnableRetries() {
        return enableRetries;
    }

    public void setEnableRetries(boolean enableRetries) {
        this.enableRetries = enableRetries;
    }

    public boolean isValidateSslCertificates() {
        return validateSslCertificates;
    }

    public void setValidateSslCertificates(boolean validateSslCertificates) {
        this.validateSslCertificates = validateSslCertificates;
    }
}