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

import org.greenbuttonalliance.espi.common.scope.EspiResource;
import org.greenbuttonalliance.espi.common.scope.ResourceFbPolicy;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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
                                                   OpaqueTokenIntrospector introspector,
                                                   ResourceFbPolicy resourceFbPolicy) throws Exception {
        // ESPI 4.0 Function-Block authorization managers. Authorities are minted by
        // EspiScopeOpaqueTokenIntrospector: FB_<n> per granted function block, plus SCOPE_<scope>
        // for admin/OIDC scopes. The FB→resource predicates live in the shared ResourceFbPolicy so
        // this endpoint gate and the entry-assembler's rel="related" link gate cannot drift (#119).
        AuthorizationManager<RequestAuthorizationContext> admin =
                AuthorityAuthorizationManager.hasAuthority("SCOPE_DataCustodian_Admin_Access");
        AuthorizationManager<RequestAuthorizationContext> adminOrTpAdmin =
                AuthorityAuthorizationManager.hasAnyAuthority(
                        "SCOPE_DataCustodian_Admin_Access", "SCOPE_ThirdParty_Admin_Access");

        // Energy interval data (UsagePoint + MeterReading/ReadingType/IntervalBlock/LocalTimeParameters)
        // and the Subscription/Batch ESPI REST transport mappings: energy interval-metering base
        // (FB_4 AND a commodity FB), or DataCustodian admin.
        AuthorizationManager<RequestAuthorizationContext> energyData =
                fbGate(admin, resourceFbPolicy::requiresEnergyIntervalBase);

        // Usage summaries (data-shape FBs) and power-quality summary (FB_17).
        AuthorizationManager<RequestAuthorizationContext> usageSummary =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.USAGE_SUMMARY, fbs));
        AuthorizationManager<RequestAuthorizationContext> powerQuality =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.ELECTRIC_POWER_QUALITY_SUMMARY, fbs));

        // Customer / PII resources — least-privilege per the ESPI Customer-PII catalog: the base
        // Connect-My-Data customer FB (FB_53) AND the resource-specific FB, or admin. FB→resource
        // mapping is authoritative from the DMD-validator conformance XSLTs (FB_NN.xsl). Note: FB_55
        // (Address) gates a field embedded in Customer, not an endpoint — deferred to response-shaping.
        AuthorizationManager<RequestAuthorizationContext> customerBase =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.CUSTOMER, fbs));
        AuthorizationManager<RequestAuthorizationContext> customerAccount =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.CUSTOMER_ACCOUNT, fbs));
        AuthorizationManager<RequestAuthorizationContext> customerAgreement =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.CUSTOMER_AGREEMENT, fbs));
        AuthorizationManager<RequestAuthorizationContext> serviceLocation =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.SERVICE_LOCATION, fbs));
        AuthorizationManager<RequestAuthorizationContext> serviceSupplier =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.SERVICE_SUPPLIER, fbs));
        AuthorizationManager<RequestAuthorizationContext> meter =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.METER, fbs));
        AuthorizationManager<RequestAuthorizationContext> endDevice =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.END_DEVICE, fbs));
        AuthorizationManager<RequestAuthorizationContext> programMapping =
                fbGate(admin, fbs -> resourceFbPolicy.isAuthorized(EspiResource.PROGRAM_DATE_ID_MAPPINGS, fbs));

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

                // --- Usage data (GET, read-only) : FB_4 + commodity, or admin. IntervalReading is a
                //     component of IntervalBlock (no UUID index) and is not an addressable resource, so
                //     it has no ROOT or XPath form and is intentionally not matched here. Subscription
                //     and Batch are ESPI REST API transport mappings (not resources) sharing this gate. ---
                .requestMatchers(HttpMethod.GET,
                    "/espi/1_1/resource/UsagePoint/**",
                    "/espi/1_1/resource/MeterReading/**",
                    "/espi/1_1/resource/ReadingType/**",
                    "/espi/1_1/resource/IntervalBlock/**",
                    "/espi/1_1/resource/LocalTimeParameter/**",
                    "/espi/1_1/resource/LocalTimeParameters/**",
                    "/espi/1_1/resource/Batch/**",
                    "/espi/1_1/resource/Subscription/**",
                    "/espi/1_1/resource/RetailCustomer/*/UsagePoint/**")
                    .access(energyData)

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
     * The shared FB→resource policy that both this endpoint gate and the resource entry-assembler's
     * {@code rel="related"} link gate consume, so the two cannot drift (#119 / #187).
     */
    @Bean
    public ResourceFbPolicy resourceFbPolicy() {
        return new ResourceFbPolicy();
    }

    /**
     * Adapt a {@link ResourceFbPolicy} FB predicate into an endpoint {@link AuthorizationManager}:
     * grant if the caller is DataCustodian admin, or if the predicate holds for the FB ids carried
     * by the token's {@code FB_<n>} authorities. This is the single bridge from the shared policy to
     * the filter chain, so endpoint authorization and link emission evaluate the exact same rule.
     */
    private static AuthorizationManager<RequestAuthorizationContext> fbGate(
            AuthorizationManager<RequestAuthorizationContext> admin,
            Predicate<Set<Integer>> fbPredicate) {
        AuthorizationManager<RequestAuthorizationContext> fb = (authentication, context) -> {
            Authentication auth = authentication.get();
            boolean granted = auth != null && auth.isAuthenticated()
                    && fbPredicate.test(grantedFunctionBlocks(auth));
            return new AuthorizationDecision(granted);
        };
        return AuthorizationManagers.anyOf(admin, fb);
    }

    /**
     * The numeric FB ids carried by a token's {@code FB_<n>} authorities (minted by
     * {@link EspiScopeOpaqueTokenIntrospector}). Non-FB authorities (admin/OIDC {@code SCOPE_…}) are
     * ignored here; admin is handled separately in {@link #fbGate}.
     */
    private static Set<Integer> grantedFunctionBlocks(Authentication authentication) {
        Set<Integer> fbs = new HashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith("FB_")) {
                try {
                    fbs.add(Integer.valueOf(name.substring(3)));
                } catch (NumberFormatException ignored) {
                    // Non-numeric FB authority — not an ESPI function block id; ignore.
                }
            }
        }
        return fbs;
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