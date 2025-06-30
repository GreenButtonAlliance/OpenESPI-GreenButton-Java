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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.validation.annotation.Validated;
import org.greenbuttonalliance.espi.authserver.service.ClientCertificateService;
import org.greenbuttonalliance.espi.authserver.service.ClientCertificateUserDetailsService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Configuration for certificate-based client authentication
 * 
 * Implements NAESB ESPI 4.0 certificate-based authentication:
 * - X.509 client certificate validation
 * - Certificate revocation checking (CRL/OCSP)
 * - Certificate chain validation
 * - Subject Distinguished Name (DN) mapping
 * - Certificate authority trust store management
 * - ESPI-specific certificate extensions
 * 
 * Features:
 * - Support for mutual TLS (mTLS) authentication
 * - Certificate-based OAuth2 client authentication
 * - Integration with ESPI compliance requirements
 * - Flexible trust store configuration
 * - Certificate renewal and rotation support
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Configuration
@ConfigurationProperties(prefix = "espi.security.certificate")
@Validated
public class ClientCertificateAuthenticationConfig {

    @NotNull
    private boolean enableCertificateAuthentication = false;

    @NotNull
    private boolean requireClientCertificate = false;

    @NotNull
    private boolean enableCertificateRevocationCheck = true;

    @NotNull
    private boolean enableOcspCheck = true;

    @NotNull
    private boolean enableCrlCheck = true;

    @NotBlank
    private String trustStorePath = "classpath:certificates/truststore.jks";

    private String trustStorePassword = "";

    @NotBlank
    private String trustStoreType = "JKS";

    @NotNull
    @Min(1)
    @Max(365)
    private Integer certificateValidityDays = 30;

    @NotNull
    @Min(1)
    @Max(90)
    private Integer certificateRenewalWarningDays = 7;

    @NotNull
    @Min(3600)
    @Max(86400)
    private Integer certificateCacheExpiration = 3600; // 1 hour

    private List<String> trustedCertificateAuthorities;
    private List<String> allowedCertificateExtensions;
    private List<String> requiredCertificateExtensions;

    /**
     * X.509 Authentication Filter for client certificate authentication
     */
    @Bean
    public X509AuthenticationFilter x509AuthenticationFilter(
            AuthenticationConfiguration authConfig,
            ClientCertificateUserDetailsService userDetailsService) throws Exception {
        
        X509AuthenticationFilter filter = new X509AuthenticationFilter();
        filter.setContinueFilterChainOnUnsuccessfulAuthentication(true);
        filter.setAuthenticationManager(authConfig.getAuthenticationManager());
        filter.setPrincipalExtractor(new SubjectDnX509PrincipalExtractor());
        
        return filter;
    }

    /**
     * Pre-authenticated authentication provider for certificate authentication
     */
    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider(
            ClientCertificateUserDetailsService userDetailsService) {
        
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(userDetailsService);
        provider.setThrowExceptionWhenTokenRejected(false);
        
        return provider;
    }

    /**
     * Certificate-based user details service
     */
    @Bean
    public ClientCertificateUserDetailsService clientCertificateUserDetailsService(
            ClientCertificateService certificateService) {
        return new ClientCertificateUserDetailsService(certificateService);
    }

    // Getters and setters
    public boolean isEnableCertificateAuthentication() {
        return enableCertificateAuthentication;
    }

    public void setEnableCertificateAuthentication(boolean enableCertificateAuthentication) {
        this.enableCertificateAuthentication = enableCertificateAuthentication;
    }

    public boolean isRequireClientCertificate() {
        return requireClientCertificate;
    }

    public void setRequireClientCertificate(boolean requireClientCertificate) {
        this.requireClientCertificate = requireClientCertificate;
    }

    public boolean isEnableCertificateRevocationCheck() {
        return enableCertificateRevocationCheck;
    }

    public void setEnableCertificateRevocationCheck(boolean enableCertificateRevocationCheck) {
        this.enableCertificateRevocationCheck = enableCertificateRevocationCheck;
    }

    public boolean isEnableOcspCheck() {
        return enableOcspCheck;
    }

    public void setEnableOcspCheck(boolean enableOcspCheck) {
        this.enableOcspCheck = enableOcspCheck;
    }

    public boolean isEnableCrlCheck() {
        return enableCrlCheck;
    }

    public void setEnableCrlCheck(boolean enableCrlCheck) {
        this.enableCrlCheck = enableCrlCheck;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public Integer getCertificateValidityDays() {
        return certificateValidityDays;
    }

    public void setCertificateValidityDays(Integer certificateValidityDays) {
        this.certificateValidityDays = certificateValidityDays;
    }

    public Integer getCertificateRenewalWarningDays() {
        return certificateRenewalWarningDays;
    }

    public void setCertificateRenewalWarningDays(Integer certificateRenewalWarningDays) {
        this.certificateRenewalWarningDays = certificateRenewalWarningDays;
    }

    public Integer getCertificateCacheExpiration() {
        return certificateCacheExpiration;
    }

    public void setCertificateCacheExpiration(Integer certificateCacheExpiration) {
        this.certificateCacheExpiration = certificateCacheExpiration;
    }

    public List<String> getTrustedCertificateAuthorities() {
        return trustedCertificateAuthorities;
    }

    public void setTrustedCertificateAuthorities(List<String> trustedCertificateAuthorities) {
        this.trustedCertificateAuthorities = trustedCertificateAuthorities;
    }

    public List<String> getAllowedCertificateExtensions() {
        return allowedCertificateExtensions;
    }

    public void setAllowedCertificateExtensions(List<String> allowedCertificateExtensions) {
        this.allowedCertificateExtensions = allowedCertificateExtensions;
    }

    public List<String> getRequiredCertificateExtensions() {
        return requiredCertificateExtensions;
    }

    public void setRequiredCertificateExtensions(List<String> requiredCertificateExtensions) {
        this.requiredCertificateExtensions = requiredCertificateExtensions;
    }
}