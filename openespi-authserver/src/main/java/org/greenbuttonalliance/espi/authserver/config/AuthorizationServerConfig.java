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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.greenbuttonalliance.espi.authserver.service.EspiTokenCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * OAuth2 Authorization Server Configuration for OpenESPI
 * <p>
 * Configures Spring Authorization Server 1.3+ for ESPI Green Button Alliance protocol:
 * - OAuth2 authorization flows (authorization_code, client_credentials, refresh_token)
 * - JWT token settings with ESPI-compliant scopes
 * - Client registration for DataCustodian and ThirdParty applications
 * - JWK source for JWT signing and validation
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    @Value("${espi.security.require-https:false}")
    private boolean requireHttps;

    @Value("${spring.security.oauth2.authorizationserver.issuer:http://localhost:9999}")
    private String issuerUri;

    @Value("${oauth2.client.defaults.redirect-uri-base:http://localhost}")
    private String defaultRedirectUriBase;

    @Value("${espi.authorization-server.introspection-endpoint:http://localhost:8080/oauth2/introspect}")
    private String introspectionUri;

    @Value("${espi.authorization-server.client-id:datacustodian}")
    private String clientId;

    @Value("${espi.authorization-server.client-secret:datacustodian-secret}")
    private String clientSecret;


    /**
     * OAuth2 Authorization Server Security Filter Chain
     * <p>
     * Configures the authorization server endpoints and security:
     * - /oauth2/authorize (authorization endpoint)
     * - /oauth2/token (token endpoint)
     * - /oauth2/jwks (JWK Set endpoint)
     * - /.well-known/oauth-authorization-server (discovery endpoint)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        // Canonical Spring Security 7.x Authorization Server setup.
        // The Spring Authorization Server 1.x static
        // OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http) was
        // removed in Spring Security 7.x; the replacement pattern manually
        // installs the configurer via http.with(...) and scopes the chain via
        // http.securityMatcher(configurer.getEndpointsMatcher()), so THIS chain
        // only claims the auth-server endpoints (/oauth2/authorize, /oauth2/token,
        // /oauth2/jwks, /oauth2/introspect, /oauth2/revoke, /connect/register,
        // /userinfo, etc.). Everything else falls through to
        // defaultSecurityFilterChain @Order(2).
        //
        // (No resource-server filter is configured on this chain — see below.)
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        // OIDC intentionally NOT enabled. ESPI uses opaque access tokens only and the
        // resource server carries no JWK/JWT (issue #134). OIDC is DEFERRED, not removed
        // forever — it returns when multi-utility Third-Party registration is built
        // (see #122). Re-add via authorizationServerConfigurer.oidc(...) at that time.
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        // Delegate both customer authentication AND consent to the Data Custodian. Spring AS's
        // loginPage / consentPage hooks accept a static URL; the AS-side AuthorizeDelegateController
        // at /authorize/delegate receives the redirect, builds a signed outbound handoff carrying
        // (client_id, scope, return_url=/oauth2/authorize/continue), and re-redirects the user to
        // DC's /oauth/authorize-screen?handoff=<signed>. DC handles its own customer login
        // (PR C2a) and Authorization Screen (PR C2b), then redirects back to AS's
        // /oauth2/authorize/continue with a signed return handoff (PR C3.3).
        //
        // Why a single delegate endpoint for both login AND consent: DC's authorize-screen sits
        // behind DC's customer-login filter chain. When the user-agent hits the screen
        // unauthenticated, DC's filter chain redirects to its own /login (preserving the screen
        // URL in return_to) and back. So delegating only the consent path covers both — the
        // login step is handled transparently inside DC.
        authorizationServerConfigurer.authorizationEndpoint(authorize -> authorize
                .consentPage("/authorize/delegate"));

        // Augment the /oauth2/token JSON response so ESPI 1.1/4.0 third parties see DC's canonical
        // resource URIs (resourceURI, authorizationURI, customerResourceURI) alongside the standard
        // access_token / token_type / expires_in / scope fields. The URIs are produced by
        // GrantBackchannelTokenCustomizer at access-token mint time (C4.3) and persisted on the
        // OAuth2Authorization's access-token claims. EspiTokenResponseSuccessHandler looks them up
        // by the token value at response time and writes them as additionalParameters. Falls back
        // to default behavior when no ESPI URI claims are present (e.g. admin client_credentials).
        OAuth2AuthorizationService authorizationServiceForHandler =
                http.getSharedObject(org.springframework.context.ApplicationContext.class)
                        .getBean(OAuth2AuthorizationService.class);
        authorizationServerConfigurer.tokenEndpoint(tokenEndpoint -> tokenEndpoint
                .accessTokenResponseHandler(
                        new org.greenbuttonalliance.espi.authserver.service
                                .EspiTokenResponseSuccessHandler(authorizationServiceForHandler)));

        http
                .securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .with(authorizationServerConfigurer, Customizer.withDefaults())
                // For non-consent unauthenticated cases, the same delegate endpoint is the
                // login page — Spring AS will append ?scope=...&client_id=...&state=... so the
                // delegate has the same context it needs to build the outbound handoff.
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/authorize/delegate"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );
        // No .oauth2ResourceServer(): the auth-server issues opaque tokens and does not
        // validate bearer tokens on its own endpoints. The OAuth2 protocol endpoints
        // (token/introspect/revoke) authenticate clients via client_secret_basic; the
        // admin/UI endpoints are protected by the @Order(2) session-login chain.

        return http.build();
    }

    /**
     * Default Security Filter Chain for everything NOT claimed by the
     * authorization-server endpoints matcher: login form, static resources,
     * H2 console (in dev), and any custom controllers.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/assets/**", "/webjars/**",
                                "/login", "/error",
                                "/.well-known/**",
                                "/actuator/health", "/actuator/info",
                                // PR C3 — AS↔DC delegation handoff endpoints. Both must be reachable
                                // without prior authentication: /authorize/delegate is hit BEFORE
                                // the customer authenticates anywhere (its job is to delegate auth
                                // to DC); /oauth2/authorize/continue receives the return handoff
                                // and writes the SecurityContext itself.
                                "/authorize/delegate",
                                "/oauth2/authorize/continue"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .csrf(Customizer.withDefaults())
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                                .preload(true)
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                );

        return http.build();
    }

    /**
     * Registered Client Repository
     * 
     * JDBC-backed repository for OAuth2 client registrations with support for:
     * - Dynamic client registration
     * - ESPI-specific client management
     * - Database persistence
     * - Client CRUD operations
     */
    @Bean
    @Primary
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        // Spring Authorization Server's stock JdbcRegisteredClientRepository:
        //   - constructor takes JdbcOperations only (no PasswordEncoder); the
        //     {prefix}value pattern in client_secret is resolved by Spring's
        //     DelegatingPasswordEncoder at authentication time, not on save
        //   - serializes ClientSettings/TokenSettings via the project's
        //     OAuth2AuthorizationServerJacksonModule, so typed values like
        //     OAuth2TokenFormat.REFERENCE round-trip correctly (the bug
        //     fixed by the Jackson-modules workaround in #128 is now
        //     simply absent — stock impl uses the right modules upstream)
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        // Initialize with default ESPI clients if they don't exist.
        // {noop}secret tells Spring's DelegatingPasswordEncoder to treat the
        // remainder as cleartext — actual basic-auth password is just "secret".
        // Production seeds would supply a pre-bcrypted hash via env vars instead.
        RegisteredClient datacustodianAdmin = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("data_custodian_admin")
                .clientSecret("{noop}dc-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("DataCustodian_Admin_Access")
                .clientIdIssuedAt(Instant.now())
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofMinutes(60))
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                    .build())
                .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .build())
                .build();

        // ThirdParty Client (ROLE_USER) - Environment-aware redirect URIs
        RegisteredClient thirdPartyClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("third_party")
                .clientSecret("{noop}tp-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .clientIdIssuedAt(Instant.now())
                .redirectUri(defaultRedirectUriBase + ":8080/DataCustodian/oauth/callback")
                .redirectUri(defaultRedirectUriBase + ":9090/ThirdParty/oauth/callback")
                .postLogoutRedirectUri(defaultRedirectUriBase + ":8080/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                .scope("FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13")
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofMinutes(360))
                    .refreshTokenTimeToLive(Duration.ofMinutes(3600))
                    .reuseRefreshTokens(true)
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                    .build())
                .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(true)
                    .build())
                .build();

        // ThirdParty Admin Client (ROLE_TP_ADMIN)
        RegisteredClient thirdPartyAdmin = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("third_party_admin")
                .clientSecret("{noop}tpadmin-secret")
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("ThirdParty_Admin_Access")
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofMinutes(360))
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                    .build())
                .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .build())
                .build();

        // Initialize default clients if they don't exist
        initializeDefaultClients(repository, datacustodianAdmin, thirdPartyClient, thirdPartyAdmin);
        
        return repository;
    }
    
    /**
     * Initialize default ESPI clients if they don't exist in the database.
     */
    private void initializeDefaultClients(RegisteredClientRepository repository,
                                          RegisteredClient... clients) {
        int seeded = 0;
        for (RegisteredClient client : clients) {
            if (repository.findByClientId(client.getClientId()) == null) {
                repository.save(client);
                seeded++;
            }
        }
        System.out.println("Default ESPI Clients seeded: " + seeded + " of " + clients.length);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository,
            org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore grantContextSessionStore) {
        // Wrap the standard JDBC service so the customer-selection context written into the HTTP
        // session at /oauth2/authorize/continue gets stamped onto OAuth2Authorization.attributes
        // at code-issue time. The wrapper is transparent for token-exchange (M2M, no session) and
        // idempotent for repeated saves on the same authorization.
        org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService delegate =
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        return new org.greenbuttonalliance.espi.authserver.grant.GrantContextEnrichingAuthorizationService(
                delegate, grantContextSessionStore);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * Authorization Server Settings
     * 
     * Configures OAuth2 endpoint URLs and issuer
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        // No .jwkSetEndpoint(): the auth-server has no JWK source (opaque tokens only).
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .build();
    }

    /**
     * ESPI Token Customizer.
     *
     * Holds the ESPI logic for adding resource/authorization URIs to the token.
     * Currently an OAuth2TokenCustomizer&lt;JwtEncodingContext&gt; that only fires
     * when espi.token.format=jwt (experimental); inert for the opaque ESPI flow.
     * RETAINED intentionally: it is the sole home of the URI-augmentation logic to
     * be migrated to the opaque token-response path (the Energy/Customer/Authorization
     * URLs) — see #122 (token-response augmentation). NOT part of the #134 JWK/JWT
     * signing strip.
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> espiTokenCustomizer() {
        return new EspiTokenCustomizer();
    }

}