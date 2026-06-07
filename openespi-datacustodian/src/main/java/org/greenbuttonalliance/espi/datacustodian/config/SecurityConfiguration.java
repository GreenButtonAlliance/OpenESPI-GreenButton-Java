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

import org.greenbuttonalliance.espi.common.scope.FunctionBlock;
import org.greenbuttonalliance.espi.common.scope.FunctionBlockCategory;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
     * Exclude the portal's static web assets (CSS/JS/images/webjars/favicon) from the security
     * filter chains entirely. These are public, non-sensitive files; routing them through the
     * stateless OAuth2 resource-server chain otherwise 401s them and the admin/customer portal
     * renders unstyled (#173). {@link PathRequest#toStaticResources()} covers Spring Boot's
     * standard static locations.
     */
    @Bean
    public WebSecurityCustomizer staticResourceSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                PathRequest.toStaticResources().atCommonLocations());
    }

    /**
     * Main security filter chain for ESPI Resource Server endpoints.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OpaqueTokenIntrospector introspector) throws Exception {
        // ESPI 4.0 Function-Block authorization managers. Authorities are minted by
        // EspiScopeOpaqueTokenIntrospector: FB_<n> per granted function block, plus SCOPE_<scope>
        // for admin/OIDC scopes. (Replaces the contractor's non-ESPI SCOPE_FB_n_READ/WRITE_3rd_party.)
        AuthorizationManager<RequestAuthorizationContext> admin =
                AuthorityAuthorizationManager.hasAuthority("SCOPE_DataCustodian_Admin_Access");
        AuthorizationManager<RequestAuthorizationContext> adminOrTpAdmin =
                AuthorityAuthorizationManager.hasAnyAuthority(
                        "SCOPE_DataCustodian_Admin_Access", "SCOPE_ThirdParty_Admin_Access");

        // Usage data (UsagePoint + its interval data: MeterReading, ReadingType, IntervalBlock,
        // LocalTimeParameters, Batch). ESPI: base FB_4 (Interval Metering) AND at least one
        // commodity FB (electricity/gas/water/temperature) — or DataCustodian admin.
        AuthorizationManager<RequestAuthorizationContext> fb4 =
                AuthorityAuthorizationManager.hasAuthority("FB_4");
        AuthorizationManager<RequestAuthorizationContext> commodity =
                AuthorityAuthorizationManager.hasAnyAuthority(commodityAuthorities());
        AuthorizationManager<RequestAuthorizationContext> usageData =
                AuthorizationManagers.anyOf(admin, AuthorizationManagers.allOf(fb4, commodity));

        // Usage summaries (data-shape FBs) and power-quality summary (FB_17).
        AuthorizationManager<RequestAuthorizationContext> usageSummary =
                AuthorizationManagers.anyOf(admin,
                        AuthorityAuthorizationManager.hasAnyAuthority("FB_15", "FB_16", "FB_27", "FB_28"));
        AuthorizationManager<RequestAuthorizationContext> powerQuality =
                AuthorizationManagers.anyOf(admin, AuthorityAuthorizationManager.hasAuthority("FB_17"));

        // Customer / PII resources — least-privilege per the ESPI Customer-PII catalog: the base
        // Connect-My-Data customer FB (FB_53) AND the resource-specific FB, or admin. FB→resource
        // mapping is authoritative from the DMD-validator conformance XSLTs (FB_NN.xsl). Note: FB_55
        // (Address) gates a field embedded in Customer, not an endpoint — deferred to response-shaping.
        AuthorizationManager<RequestAuthorizationContext> customerBase = piiGate(admin, "FB_54");   // Customer
        AuthorizationManager<RequestAuthorizationContext> customerAccount = piiGate(admin, "FB_56"); // billing
        AuthorizationManager<RequestAuthorizationContext> customerAgreement = piiGate(admin, "FB_57");
        AuthorizationManager<RequestAuthorizationContext> serviceLocation = piiGate(admin, "FB_58");
        AuthorizationManager<RequestAuthorizationContext> serviceSupplier = piiGate(admin, "FB_59");
        AuthorizationManager<RequestAuthorizationContext> meter = piiGate(admin, "FB_60");
        AuthorizationManager<RequestAuthorizationContext> endDevice = piiGate(admin, "FB_61");
        AuthorizationManager<RequestAuthorizationContext> programMapping = piiGate(admin, "FB_62");

        return http
            // Disable CSRF for API endpoints
            .csrf(AbstractHttpConfigurer::disable)

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure session management (stateless for OAuth2)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Configure authorization rules. ESPI customer FB scopes are READ-ONLY; every write is
            // admin-gated (see the blanket write rule below).
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(
                    "/error",
                    "/actuator/health",
                    "/actuator/info",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/h2-console/**"
                ).permitAll()

                // --- Admin-only resources (OAuth2 metadata, not energy/PII) ---
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/ApplicationInformation/**")
                    .access(adminOrTpAdmin)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/Authorization/**")
                    .access(adminOrTpAdmin)

                // --- Usage summaries (GET) : data-shape FBs, or admin. The summary/PQ leaf rules
                //     (including Subscription-/RetailCustomer-scoped XPath forms) MUST precede the
                //     broad usage-data rule, so a summary reached via /Subscription/.../UsageSummary is
                //     gated by its own FB rather than FB_4+commodity. ---
                .requestMatchers(HttpMethod.GET,
                    "/espi/1_1/resource/UsageSummary/**",
                    "/espi/1_1/resource/ElectricPowerUsageSummary/**",
                    "/espi/1_1/resource/Subscription/*/UsagePoint/*/UsageSummary/**",
                    "/espi/1_1/resource/Subscription/*/UsagePoint/*/ElectricPowerUsageSummary/**",
                    "/espi/1_1/resource/RetailCustomer/*/UsagePoint/*/UsageSummary/**",
                    "/espi/1_1/resource/RetailCustomer/*/UsagePoint/*/ElectricPowerUsageSummary/**")
                    .access(usageSummary)

                // --- Power-quality summary (GET) : FB_17, or admin ---
                .requestMatchers(HttpMethod.GET,
                    "/espi/1_1/resource/ElectricPowerQualitySummary/**",
                    "/espi/1_1/resource/Subscription/*/UsagePoint/*/ElectricPowerQualitySummary/**",
                    "/espi/1_1/resource/RetailCustomer/*/UsagePoint/*/ElectricPowerQualitySummary/**")
                    .access(powerQuality)

                // --- Usage data (GET, read-only) : FB_4 + commodity, or admin ---
                .requestMatchers(HttpMethod.GET,
                    "/espi/1_1/resource/UsagePoint/**",
                    "/espi/1_1/resource/MeterReading/**",
                    "/espi/1_1/resource/ReadingType/**",
                    "/espi/1_1/resource/IntervalBlock/**",
                    "/espi/1_1/resource/IntervalReading/**",
                    "/espi/1_1/resource/LocalTimeParameter/**",
                    "/espi/1_1/resource/LocalTimeParameters/**",
                    "/espi/1_1/resource/Batch/**",
                    "/espi/1_1/resource/Subscription/**",
                    "/espi/1_1/resource/RetailCustomer/*/UsagePoint/**")
                    .access(usageData)

                // --- Customer / PII resources (GET) : FB_53 base + resource-specific FB, or admin ---
                // ROOT forms are gated 1:1 per the conformance-XSLT FB mapping.
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/CustomerAccount/**").access(customerAccount)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/CustomerAgreement/**").access(customerAgreement)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/ServiceSupplier/**").access(serviceSupplier)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/ServiceLocation/**").access(serviceLocation)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/Meter/**").access(meter)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/EndDevice/**").access(endDevice)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/ProgramDateIdMappings/**").access(programMapping)
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/Customer/**").access(customerBase)
                // XPath customer subtree: customer-base floor (FB_53+FB_54). Per-leaf XPath gating and
                // FB_55 (Address, a Customer field) are response-shaping refinements — Phase 2.
                .requestMatchers(HttpMethod.GET, "/espi/1_1/resource/RetailCustomer/*/Customer/**").access(customerBase)

                // --- All resource writes are admin-only (ESPI third-party access is read-only) ---
                .requestMatchers(HttpMethod.POST, "/espi/1_1/resource/**").access(admin)
                .requestMatchers(HttpMethod.PUT, "/espi/1_1/resource/**").access(admin)
                .requestMatchers(HttpMethod.DELETE, "/espi/1_1/resource/**").access(admin)

                // Admin endpoints
                .requestMatchers("/admin/**").access(admin)

                // All other ESPI endpoints require authentication
                .requestMatchers("/espi/**").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Configure OAuth2 Resource Server with opaque token introspection.
            // ESPI standard requires opaque tokens, not JWT. The custom introspector
            // (EspiScopeOpaqueTokenIntrospector) maps the ESPI FB scope to FB_<n> authorities.
            .oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaque -> opaque.introspector(introspector))
            )
            
            // Security headers configuration
            .headers(headers -> headers
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // Allow H2 console in development
                    .contentTypeOptions(contentTypeOptions -> {})
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
     * NOTE: JWT/JWK support is intentionally absent. ESPI 4.0 mandates opaque
     * access tokens; the GBA Resource Server must contain no JWK/JWT functions
     * (see issue #134 / #122).
     */

    /**
     * Configures the Opaque Token Introspector to validate tokens against the Authorization Server,
     * wrapped so the ESPI FB scope is translated into FB_&lt;n&gt; authorities the resource rules enforce
     * (see {@link EspiScopeOpaqueTokenIntrospector}).
     */
    @Bean
    public OpaqueTokenIntrospector introspector() {
        OpaqueTokenIntrospector delegate = SpringOpaqueTokenIntrospector.withIntrospectionUri(introspectionUri)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        return new EspiScopeOpaqueTokenIntrospector(delegate);
    }

    /**
     * The {@code FB_<n>} authorities for every commodity Function Block (electricity/gas/water/
     * temperature), derived from the single ESPI FB catalog in {@link FunctionBlock} so this never
     * drifts from the spec.
     */
    private static String[] commodityAuthorities() {
        return Stream.of(FunctionBlock.values())
                .filter(fb -> fb.getCategory() == FunctionBlockCategory.COMMODITY)
                .map(fb -> "FB_" + fb.getId())
                .toArray(String[]::new);
    }

    /**
     * Customer-PII endpoint gate: {@code admin OR (FB_53 base AND the resource-specific FB)}.
     * FB_53 (Connect My Data, Retail Customer) is the prerequisite base for any customer/PII access;
     * the resource FB (54/56/57/58/59/60/61/62) enforces least privilege per the ESPI Customer-PII
     * catalog (mapping derived from the DMD-validator conformance XSLTs).
     */
    private static AuthorizationManager<RequestAuthorizationContext> piiGate(
            AuthorizationManager<RequestAuthorizationContext> admin, String resourceFb) {
        AuthorizationManager<RequestAuthorizationContext> base = AuthorityAuthorizationManager.hasAuthority("FB_53");
        AuthorizationManager<RequestAuthorizationContext> specific = AuthorityAuthorizationManager.hasAuthority(resourceFb);
        return AuthorizationManagers.anyOf(admin, AuthorizationManagers.allOf(base, specific));
    }


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