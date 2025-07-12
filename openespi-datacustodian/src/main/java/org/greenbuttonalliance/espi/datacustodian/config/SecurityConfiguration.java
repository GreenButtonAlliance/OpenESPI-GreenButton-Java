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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the OpenESPI Data Custodian Resource Server.
 * 
 * This configuration implements OAuth2 Resource Server security using opaque tokens
 * from the separate OpenESPI AuthorizationEntity Server. It replaces the legacy Spring
 * Security configuration replacing legacy Spring XML with modern Spring Security 6.5.
 * 
 * Key Features:
 * - OAuth2 Resource Server with opaque token introspection
 * - ESPI-specific authorization rules  
 * - CORS configuration for web clients
 * - Method-level security for service layers
 * 
 * Note: ESPI standard uses opaque OAuth2 tokens. JWT support will be added
 * in future enhancement for dynamic client registration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfiguration {

    @Value("${espi.authorization-server.introspection-endpoint:http://localhost:8080/oauth2/introspect}")
    private String introspectionUri;
    
    @Value("${espi.authorization-server.client-id:datacustodian}")
    private String clientId;
    
    @Value("${espi.authorization-server.client-secret:datacustodian-secret}")
    private String clientSecret;

    /**
     * Main security filter chain for ESPI Resource Server endpoints.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF for API endpoints
            .csrf(AbstractHttpConfigurer::disable)
            
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management (stateless for OAuth2)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/h2-console/**"
                ).permitAll()
                
                // ESPI root resource endpoints (require authentication)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/ApplicationInformation/**")
                    .hasAnyAuthority("SCOPE_DataCustodian_Admin_Access", "SCOPE_ThirdParty_Admin_Access")
                
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/Authorization/**")
                    .hasAnyAuthority("SCOPE_DataCustodian_Admin_Access")
                
                // ESPI Usage Point endpoints
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/UsagePoint/**")
                    .hasAnyAuthority(
                        "SCOPE_FB_15_READ_3rd_party", 
                        "SCOPE_FB_16_READ_3rd_party", 
                        "SCOPE_FB_36_READ_3rd_party",
                        "SCOPE_DataCustodian_Admin_Access"
                    )
                
                .requestMatchers(HttpMethod.POST, "/espi/1_1/resource/UsagePoint/**")
                    .hasAnyAuthority(
                        "SCOPE_FB_15_WRITE_3rd_party", 
                        "SCOPE_FB_16_WRITE_3rd_party", 
                        "SCOPE_FB_36_WRITE_3rd_party",
                        "SCOPE_DataCustodian_Admin_Access"
                    )
                
                // ESPI SubscriptionEntity-based endpoints
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/Subscription/*/UsagePoint/**")
                    .hasAnyAuthority(
                        "SCOPE_FB_15_READ_3rd_party", 
                        "SCOPE_FB_16_READ_3rd_party", 
                        "SCOPE_FB_36_READ_3rd_party"
                    )
                
                .requestMatchers(HttpMethod.POST, "/espi/1_1/resource/Subscription/*/UsagePoint/**")
                    .hasAnyAuthority(
                        "SCOPE_FB_15_WRITE_3rd_party", 
                        "SCOPE_FB_16_WRITE_3rd_party", 
                        "SCOPE_FB_36_WRITE_3rd_party"
                    )
                
                // ESPI Meter Reading endpoints
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/**/MeterReading/**")
                    .hasAnyAuthority(
                        "SCOPE_FB_15_READ_3rd_party", 
                        "SCOPE_FB_16_READ_3rd_party", 
                        "SCOPE_FB_36_READ_3rd_party",
                        "SCOPE_DataCustodian_Admin_Access"
                    )
                
                .requestMatchers(HttpMethod.POST, "/espi/1_1/resource/**/MeterReading/**")
                    .hasAnyAuthority(
                        "SCOPE_FB_15_WRITE_3rd_party", 
                        "SCOPE_FB_16_WRITE_3rd_party", 
                        "SCOPE_FB_36_WRITE_3rd_party",
                        "SCOPE_DataCustodian_Admin_Access"
                    )
                
                // ESPI Interval Reading endpoints
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/**/IntervalReading/**")
                    .hasAnyAuthority(
                        "SCOPE_FB_15_READ_3rd_party", 
                        "SCOPE_FB_16_READ_3rd_party", 
                        "SCOPE_FB_36_READ_3rd_party",
                        "SCOPE_DataCustodian_Admin_Access"
                    )
                
                // ESPI Batch endpoints
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/Batch/**")
                    .hasAnyAuthority(
                        "SCOPE_FB_15_READ_3rd_party", 
                        "SCOPE_FB_16_READ_3rd_party", 
                        "SCOPE_FB_36_READ_3rd_party",
                        "SCOPE_DataCustodian_Admin_Access"
                    )
                
                // Admin endpoints
                .requestMatchers("/admin/**")
                    .hasAuthority("SCOPE_DataCustodian_Admin_Access")
                
                // All other ESPI endpoints require authentication
                .requestMatchers("/espi/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Configure OAuth2 Resource Server with opaque token introspection
            // ESPI standard requires opaque tokens, not JWT tokens
            .oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaque -> opaque
                    .introspectionUri(introspectionUri)
                    .introspectionClientCredentials(clientId, clientSecret)
                )
            )
            
            // Security headers configuration
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin()) // Allow H2 console in development
                .contentTypeOptions(contentTypeOptions -> contentTypeOptions.and())
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true))
            )
            
            .build();
    }

    /**
     * OAuth2 Resource Server configuration notes:
     * 
     * The DataCustodian acts as an OAuth2 Resource Server that validates opaque tokens
     * issued by the separate OpenESPI AuthorizationEntity Server. This configuration:
     * 
     * 1. Uses opaque token introspection (ESPI standard requirement)
     * 2. Connects to the AuthorizationEntity Server's introspection endpoint
     * 3. Uses client credentials for introspection authentication
     * 
     * Future enhancement: Add JWT support for dynamic client registration scenarios
     */

    /**
     * CORS configuration for web clients.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "https://localhost:*",
            "https://*.greenbuttonalliance.org"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}