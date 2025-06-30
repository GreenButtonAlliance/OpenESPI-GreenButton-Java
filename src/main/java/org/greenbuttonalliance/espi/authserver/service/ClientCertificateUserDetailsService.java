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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * UserDetails service for certificate-based authentication
 * 
 * Implements Spring Security's AuthenticationUserDetailsService for
 * X.509 certificate-based authentication in ESPI 4.0 environment.
 * 
 * Features:
 * - Certificate validation integration
 * - Client role mapping
 * - ESPI-specific authorities
 * - Certificate-to-user mapping
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Service
public class ClientCertificateUserDetailsService 
    implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(ClientCertificateUserDetailsService.class);

    private final ClientCertificateService certificateService;

    public ClientCertificateUserDetailsService(ClientCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) 
            throws UsernameNotFoundException {
        
        logger.debug("Loading user details for certificate authentication");

        try {
            // Extract certificate from credentials
            X509Certificate certificate = extractCertificate(token);
            if (certificate == null) {
                throw new UsernameNotFoundException("No certificate found in authentication token");
            }

            // Validate certificate and get client ID
            ClientCertificateService.CertificateValidationResult result = 
                certificateService.validateClientCertificate(certificate);

            if (!result.isValid()) {
                logger.warn("Certificate validation failed: {}", result.getErrorMessage());
                throw new UsernameNotFoundException("Certificate validation failed: " + result.getErrorMessage());
            }

            String clientId = result.getClientId();
            logger.debug("Certificate validated for client: {}", clientId);

            // Create user details with appropriate authorities
            Collection<SimpleGrantedAuthority> authorities = getClientAuthorities(clientId, certificate);

            // Use client ID as username
            UserDetails userDetails = User.builder()
                .username(clientId)
                .password("") // No password for certificate authentication
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();

            logger.info("Certificate authentication successful for client: {}", clientId);
            return userDetails;

        } catch (Exception e) {
            logger.error("Error during certificate authentication", e);
            throw new UsernameNotFoundException("Certificate authentication failed", e);
        }
    }

    /**
     * Extract X.509 certificate from authentication token
     */
    private X509Certificate extractCertificate(PreAuthenticatedAuthenticationToken token) {
        Object credentials = token.getCredentials();
        
        if (credentials instanceof X509Certificate) {
            return (X509Certificate) credentials;
        }
        
        if (credentials instanceof X509Certificate[]) {
            X509Certificate[] certificates = (X509Certificate[]) credentials;
            return certificates.length > 0 ? certificates[0] : null;
        }
        
        logger.warn("No X.509 certificate found in authentication token credentials");
        return null;
    }

    /**
     * Get authorities for the authenticated client
     */
    private Collection<SimpleGrantedAuthority> getClientAuthorities(String clientId, X509Certificate certificate) {
        try {
            // Base authority for all certificate-authenticated clients
            SimpleGrantedAuthority baseAuthority = new SimpleGrantedAuthority("ROLE_CLIENT_CERTIFICATE");
            
            // Additional authorities based on certificate properties
            String subjectDn = certificate.getSubjectDN().toString();
            
            // Check if this is an admin client based on DN
            if (isAdminClient(subjectDn)) {
                return Arrays.asList(
                    baseAuthority,
                    new SimpleGrantedAuthority("ROLE_CLIENT_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_ESPI_CLIENT")
                );
            }
            
            // Check if this is a data custodian client
            if (isDataCustodianClient(subjectDn)) {
                return Arrays.asList(
                    baseAuthority,
                    new SimpleGrantedAuthority("ROLE_DATA_CUSTODIAN"),
                    new SimpleGrantedAuthority("ROLE_ESPI_CLIENT")
                );
            }
            
            // Check if this is a third party client
            if (isThirdPartyClient(subjectDn)) {
                return Arrays.asList(
                    baseAuthority,
                    new SimpleGrantedAuthority("ROLE_THIRD_PARTY"),
                    new SimpleGrantedAuthority("ROLE_ESPI_CLIENT")
                );
            }
            
            // Default client authority
            return Arrays.asList(
                baseAuthority,
                new SimpleGrantedAuthority("ROLE_ESPI_CLIENT")
            );

        } catch (Exception e) {
            logger.error("Error determining client authorities", e);
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT_CERTIFICATE"));
        }
    }

    /**
     * Check if client is an admin client based on certificate DN
     */
    private boolean isAdminClient(String subjectDn) {
        return subjectDn.toLowerCase().contains("ou=admin") || 
               subjectDn.toLowerCase().contains("cn=admin") ||
               subjectDn.toLowerCase().contains("role=admin");
    }

    /**
     * Check if client is a data custodian based on certificate DN
     */
    private boolean isDataCustodianClient(String subjectDn) {
        return subjectDn.toLowerCase().contains("ou=datacustodian") || 
               subjectDn.toLowerCase().contains("cn=datacustodian") ||
               subjectDn.toLowerCase().contains("role=datacustodian");
    }

    /**
     * Check if client is a third party based on certificate DN
     */
    private boolean isThirdPartyClient(String subjectDn) {
        return subjectDn.toLowerCase().contains("ou=thirdparty") || 
               subjectDn.toLowerCase().contains("cn=thirdparty") ||
               subjectDn.toLowerCase().contains("role=thirdparty");
    }
}