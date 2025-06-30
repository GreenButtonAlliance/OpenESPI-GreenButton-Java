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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * ESPI Token Customizer for Spring Authorization Server
 * 
 * **IMPORTANT**: ESPI standard uses opaque access tokens, not JWT tokens.
 * This customizer is EXPERIMENTAL and only applies when JWT tokens are enabled.
 * 
 * When JWT tokens are enabled (experimental), adds ESPI-specific claims:
 * - espi_version: ESPI protocol version
 * - espi_client_type: Type of client (customer/admin)
 * - espi_grant_type: OAuth2 grant type used
 * - data_custodian_endpoint: Resource server endpoint URL
 * - resource_uri: Specific resource access URI
 * - authorization_uri: Authorization resource URI
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Service
public class EspiTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private static final Logger logger = LoggerFactory.getLogger(EspiTokenCustomizer.class);

    @Value("${espi.datacustodian.endpoint:http://localhost:8080}")
    private String dataCustodianEndpoint;

    @Value("${espi.token.format:opaque}")
    private String tokenFormat;

    @Override
    public void customize(JwtEncodingContext context) {
        // Only customize JWT tokens - ESPI standard uses opaque tokens
        if (!"jwt".equals(tokenFormat)) {
            logger.debug("Token format is '{}', skipping JWT customization (ESPI standard uses opaque tokens)", tokenFormat);
            return;
        }

        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            Authentication principal = context.getPrincipal();
            String clientId = context.getRegisteredClient().getClientId();
            String grantType = context.getAuthorizationGrantType().getValue();

            logger.debug("EXPERIMENTAL: Customizing JWT token for client: {}, grant type: {} (ESPI standard uses opaque tokens)", clientId, grantType);

            // Add ESPI standard claims
            context.getClaims().claim("espi_version", "1.1");
            context.getClaims().claim("data_custodian_endpoint", dataCustodianEndpoint);

            // Add grant type specific claims
            if ("client_credentials".equals(grantType)) {
                handleClientCredentials(context, principal, clientId);
            } else if ("authorization_code".equals(grantType) || "refresh_token".equals(grantType)) {
                handleAuthorizationCode(context, principal, clientId);
            }
        }
    }

    /**
     * Handle client_credentials grant type
     * Used for admin/bulk access scenarios
     */
    private void handleClientCredentials(JwtEncodingContext context, Authentication principal, String clientId) {
        Set<String> authorities = AuthorityUtils.authorityListToSet(principal.getAuthorities());

        logger.debug("Processing client_credentials for client: {}, authorities: {}", clientId, authorities);

        // Determine resource URI based on role
        String resourceUri = determineResourceUri(authorities, clientId);
        context.getClaims().claim("resource_uri", resourceUri);

        // Generate authorization URI for admin access
        String authorizationUri = dataCustodianEndpoint + "/espi/1_1/resource/Authorization/" + generateAuthorizationId();
        context.getClaims().claim("authorization_uri", authorizationUri);

        // Add ESPI-specific claims
        context.getClaims().claim("espi_client_type", "admin");
        context.getClaims().claim("espi_grant_type", "client_credentials");
    }

    /**
     * Handle authorization_code and refresh_token grant types
     * Used for customer consent-based access
     */
    private void handleAuthorizationCode(JwtEncodingContext context, Authentication principal, String clientId) {
        logger.debug("Processing authorization_code/refresh_token for client: {}", clientId);

        // Standard resource access for customers
        String resourceUri = dataCustodianEndpoint + "/espi/1_1/resource/";
        context.getClaims().claim("resource_uri", resourceUri);

        // Generate authorization URI
        String authorizationUri = dataCustodianEndpoint + "/espi/1_1/resource/Authorization/" + generateAuthorizationId();
        context.getClaims().claim("authorization_uri", authorizationUri);

        // Add ESPI-specific claims
        context.getClaims().claim("espi_client_type", "customer");
        context.getClaims().claim("espi_grant_type", "authorization_code");
    }

    /**
     * Determine resource URI based on client authorities
     */
    private String determineResourceUri(Set<String> authorities, String clientId) {
        String baseEndpoint = dataCustodianEndpoint + "/espi/1_1/resource";
        
        if (authorities.contains("ROLE_DC_ADMIN")) {
            return baseEndpoint + "/";
        } else if (authorities.contains("ROLE_TP_ADMIN")) {
            return baseEndpoint + "/Batch/Bulk/**";
        } else if (authorities.contains("ROLE_UL_ADMIN")) {
            return baseEndpoint + "/Batch/Upload/**";
        } else if (authorities.contains("ROLE_TP_REGISTRATION")) {
            return baseEndpoint + "/ApplicationInformation/**";
        } else {
            return baseEndpoint + "/";
        }
    }

    /**
     * Generate a simple authorization ID
     * In a real implementation, this would be coordinated with the Resource Server
     */
    private String generateAuthorizationId() {
        return String.valueOf(System.currentTimeMillis());
    }
}