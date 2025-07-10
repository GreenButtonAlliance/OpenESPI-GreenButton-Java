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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.greenbuttonalliance.espi.authserver.repository.JdbcRegisteredClientRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.greenbuttonalliance.espi.authserver.service.EspiTokenCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 Authorization Server Configuration for OpenESPI
 * 
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

    /**
     * OAuth2 Authorization Server Security Filter Chain
     * 
     * Configures the authorization server endpoints and security:
     * - /oauth2/authorize (authorization endpoint)
     * - /oauth2/token (token endpoint)
     * - /oauth2/jwks (JWK Set endpoint)
     * - /.well-known/oauth-authorization-server (discovery endpoint)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0
        
        http
            // Redirect to the login page when not authenticated from the
            // authorization endpoint
            .exceptionHandling((exceptions) -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            )
            // Accept access tokens for User Info and/or Client Registration
            .oauth2ResourceServer((resourceServer) -> resourceServer
                .jwt(Customizer.withDefaults()))
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
                .and()
            );

        return http.build();
    }

    /**
     * Default Security Filter Chain for non-OAuth2 endpoints
     * 
     * Handles authentication for:
     * - Login page
     * - User consent page
     * - Static resources
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/assets/**", "/webjars/**", "/login").permitAll()
                .anyRequest().authenticated()
            )
            // Form login handles the redirect to the login page from the
            // authorization server filter chain
            .formLogin(Customizer.withDefaults())
            // HTTPS Channel Security for Production (Default Security Chain)
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
            // Secure session configuration
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
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);
        
        // Initialize with default ESPI clients if they don't exist
        // DataCustodian Admin Client (ROLE_DC_ADMIN)
        RegisteredClient datacustodianAdmin = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("data_custodian_admin")
                .clientSecret("{noop}secret") // TODO: Use proper password encoder
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("DataCustodian_Admin_Access")
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
                .clientSecret("{noop}secret") // TODO: Use proper password encoder
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
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
                .clientSecret("{noop}secret") // TODO: Use proper password encoder
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
     * Initialize default ESPI clients if they don't exist in the database
     */
    private void initializeDefaultClients(JdbcRegisteredClientRepository repository, 
                                        RegisteredClient... clients) {
        for (RegisteredClient client : clients) {
            if (repository.findByClientId(client.getClientId()) == null) {
                repository.save(client);
            }
        }
    }

    /**
     * JWK Source for JWT Token Signing
     * 
     * Generates RSA key pair for JWT signing and validation.
     * 
     * TODO: Use persistent key store for production
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT Decoder for token validation
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Authorization Server Settings
     * 
     * Configures OAuth2 endpoint URLs and issuer
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .jwkSetEndpoint("/oauth2/jwks")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .oidcClientRegistrationEndpoint("/connect/register")
                .oidcUserInfoEndpoint("/userinfo")
                .build();
    }

    /**
     * ESPI Token Customizer
     * 
     * Adds Green Button Alliance specific claims to JWT tokens
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> espiTokenCustomizer() {
        return new EspiTokenCustomizer();
    }

    /**
     * Generate RSA Key Pair for JWT signing
     */
    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }
}