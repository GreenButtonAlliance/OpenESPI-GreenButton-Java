/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.datacustodian.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.beans.factory.annotation.Value;

/**
 * OAuth2 Resource Server Configuration for OpenESPI Data Custodian
 * 
 * Configures Spring Security as an OAuth2 Resource Server for ESPI Green Button Alliance protocol:
 * - OAuth2 opaque token introspection (ESPI standard)
 * - API endpoint security with proper scoping
 * - Static resource handling
 * - ESPI-compliant security headers
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Value("${espi.security.require-https:false}")
    private boolean requireHttps;

    /**
     * OAuth2 Resource Server Security Filter Chain
     * 
     * Configures the resource server endpoints and security:
     * - /espi/1_1/resource/** (protected API endpoints)
     * - /actuator/** (management endpoints)
     * - /api-docs/** (OpenAPI documentation)
     * - /swagger-ui/** (Swagger UI)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/espi/1_1/resource/**")
            .authorizeHttpRequests(authorize -> authorize
                // ESPI Resource API endpoints require OAuth2 authentication
                .requestMatchers("/espi/1_1/resource/**").authenticated()
            )
            // Configure OAuth2 Resource Server with opaque token introspection
            .oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaque -> opaque
                    // Token introspection is configured via application.yml
                    // spring.security.oauth2.resourceserver.opaquetoken.introspection-uri
                    // spring.security.oauth2.resourceserver.opaquetoken.client-id
                    // spring.security.oauth2.resourceserver.opaquetoken.client-secret
                )
            )
            // HTTPS Channel Security for Production
            .requiresChannel(channel -> {
                if (requireHttps) {
                    channel.anyRequest().requiresSecure();
                }
            })
            // Enhanced Security Headers for ESPI Compliance
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            )
            // CSRF not needed for API endpoints with OAuth2
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Default Security Filter Chain for non-API endpoints
     * 
     * Handles authentication for:
     * - Static resources (CSS, JS, images)
     * - Management endpoints
     * - API documentation
     * - Error pages
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints
                .requestMatchers(
                    "/css/**", "/js/**", "/images/**", "/favicon.ico",
                    "/error", "/actuator/health", "/actuator/info",
                    "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                ).permitAll()
                // Management endpoints require authentication
                .requestMatchers("/actuator/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Basic authentication for management endpoints
            .httpBasic(httpBasic -> {
                // HTTP Basic auth configuration if needed
            })
            // HTTPS Channel Security for Production
            .requiresChannel(channel -> {
                if (requireHttps) {
                    channel.anyRequest().requiresSecure();
                }
            })
            // Enhanced Security Headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            )
            // CSRF protection for web endpoints
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/actuator/**", "/api-docs/**")
            );

        return http.build();
    }
}