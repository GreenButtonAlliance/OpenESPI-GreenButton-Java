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

package org.greenbuttonalliance.espi.authserver.testdata;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Test Data Builder for OAuth2 RegisteredClient and related entities
 * 
 * Provides fluent builder pattern for creating test data with ESPI-compliant
 * defaults and Green Button Alliance standard configurations.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
public class TestDataBuilder {

    /**
     * Creates a builder for ESPI-compliant customer client
     */
    public static RegisteredClientBuilder espiCustomerClient() {
        return new RegisteredClientBuilder()
                .id(UUID.randomUUID().toString())
                .clientId("espi_client_" + UUID.randomUUID().toString().substring(0, 8))
                .clientName("ESPI Customer Client")
                .clientSecret("{noop}customer_secret")
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://customer.example.com/callback")
                .postLogoutRedirectUri("https://customer.example.com/logout")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                        .accessTokenTimeToLive(Duration.ofMinutes(360)) // 6 hours
                        .refreshTokenTimeToLive(Duration.ofMinutes(3600)) // 60 hours
                        .reuseRefreshTokens(true)
                        .build());
    }

    /**
     * Creates a builder for DataCustodian admin client
     */
    public static RegisteredClientBuilder dataCustodianAdminClient() {
        return new RegisteredClientBuilder()
                .id(UUID.randomUUID().toString())
                .clientId("data_custodian_admin_" + UUID.randomUUID().toString().substring(0, 8))
                .clientName("DataCustodian Admin")
                .clientSecret("{noop}admin_secret")
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("DataCustodian_Admin_Access")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                        .accessTokenTimeToLive(Duration.ofMinutes(60)) // 1 hour for admin
                        .build());
    }

    /**
     * Creates a builder for ThirdParty admin client
     */
    public static RegisteredClientBuilder thirdPartyAdminClient() {
        return new RegisteredClientBuilder()
                .id(UUID.randomUUID().toString())
                .clientId("third_party_admin_" + UUID.randomUUID().toString().substring(0, 8))
                .clientName("ThirdParty Admin")
                .clientSecret("{noop}tp_admin_secret")
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("ThirdParty_Admin_Access")
                .scope("Batch_Upload_Access")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                        .accessTokenTimeToLive(Duration.ofMinutes(360)) // 6 hours
                        .build());
    }

    /**
     * Creates a builder for test client with minimal configuration
     */
    public static RegisteredClientBuilder minimalClient() {
        return new RegisteredClientBuilder()
                .id(UUID.randomUUID().toString())
                .clientId("minimal_client_" + UUID.randomUUID().toString().substring(0, 8))
                .clientName("Minimal Test Client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://minimal.example.com/callback")
                .scope(OidcScopes.OPENID);
    }

    /**
     * Creates a builder for invalid client (for negative testing)
     */
    public static RegisteredClientBuilder invalidClient() {
        return new RegisteredClientBuilder()
                .id(UUID.randomUUID().toString())
                .clientId("invalid_client_" + UUID.randomUUID().toString().substring(0, 8))
                .clientName("Invalid Test Client")
                .clientSecret("{noop}invalid_secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.IMPLICIT) // Not supported in ESPI
                .redirectUri("http://insecure.example.com/callback") // HTTP not HTTPS
                .scope("invalid_scope");
    }

    /**
     * Creates a builder for Green Button Connect My Data client
     */
    public static RegisteredClientBuilder greenButtonConnectClient() {
        return new RegisteredClientBuilder()
                .id(UUID.randomUUID().toString())
                .clientId("gb_connect_" + UUID.randomUUID().toString().substring(0, 8))
                .clientName("Green Button Connect My Data")
                .clientSecret("{noop}gb_connect_secret")
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://gbconnect.example.com/oauth/callback")
                .postLogoutRedirectUri("https://gbconnect.example.com/logout")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                .scope("FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7")
                .scope("FB=4_5_17;IntervalDuration=86400;BlockDuration=yearly;HistoryLength=1")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(true) // Enable PKCE for mobile clients
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                        .accessTokenTimeToLive(Duration.ofMinutes(360)) // 6 hours
                        .refreshTokenTimeToLive(Duration.ofMinutes(3600)) // 60 hours
                        .reuseRefreshTokens(false) // Better security for mobile
                        .build());
    }

    /**
     * Builder class for RegisteredClient with fluent API
     */
    public static class RegisteredClientBuilder {
        private RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString());

        public RegisteredClientBuilder id(String id) {
            this.builder = RegisteredClient.withId(id);
            return this;
        }

        public RegisteredClientBuilder clientId(String clientId) {
            this.builder.clientId(clientId);
            return this;
        }

        public RegisteredClientBuilder clientName(String clientName) {
            this.builder.clientName(clientName);
            return this;
        }

        public RegisteredClientBuilder clientSecret(String clientSecret) {
            this.builder.clientSecret(clientSecret);
            return this;
        }

        public RegisteredClientBuilder clientIdIssuedAt(Instant clientIdIssuedAt) {
            this.builder.clientIdIssuedAt(clientIdIssuedAt);
            return this;
        }

        public RegisteredClientBuilder clientSecretExpiresAt(Instant clientSecretExpiresAt) {
            this.builder.clientSecretExpiresAt(clientSecretExpiresAt);
            return this;
        }

        public RegisteredClientBuilder clientAuthenticationMethod(ClientAuthenticationMethod clientAuthenticationMethod) {
            this.builder.clientAuthenticationMethod(clientAuthenticationMethod);
            return this;
        }

        public RegisteredClientBuilder authorizationGrantType(AuthorizationGrantType authorizationGrantType) {
            this.builder.authorizationGrantType(authorizationGrantType);
            return this;
        }

        public RegisteredClientBuilder redirectUri(String redirectUri) {
            this.builder.redirectUri(redirectUri);
            return this;
        }

        public RegisteredClientBuilder postLogoutRedirectUri(String postLogoutRedirectUri) {
            this.builder.postLogoutRedirectUri(postLogoutRedirectUri);
            return this;
        }

        public RegisteredClientBuilder scope(String scope) {
            this.builder.scope(scope);
            return this;
        }

        public RegisteredClientBuilder clientSettings(ClientSettings clientSettings) {
            this.builder.clientSettings(clientSettings);
            return this;
        }

        public RegisteredClientBuilder tokenSettings(TokenSettings tokenSettings) {
            this.builder.tokenSettings(tokenSettings);
            return this;
        }

        /**
         * Add ESPI-specific scopes for different intervals and history lengths
         */
        public RegisteredClientBuilder withEspiScopes() {
            return this.scope("FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
                      .scope("FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7")
                      .scope("FB=4_5_17;IntervalDuration=86400;BlockDuration=yearly;HistoryLength=1");
        }

        /**
         * Add common OAuth2/OIDC scopes
         */
        public RegisteredClientBuilder withStandardScopes() {
            return this.scope(OidcScopes.OPENID)
                      .scope(OidcScopes.PROFILE)
                      .scope(OidcScopes.EMAIL);
        }

        /**
         * Configure for ESPI compliance (opaque tokens, appropriate lifetimes)
         */
        public RegisteredClientBuilder espiCompliant() {
            return this.tokenSettings(TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                    .accessTokenTimeToLive(Duration.ofMinutes(360)) // 6 hours
                    .refreshTokenTimeToLive(Duration.ofMinutes(3600)) // 60 hours
                    .reuseRefreshTokens(true)
                    .build());
        }

        /**
         * Configure for production security (PKCE required, consent required)
         */
        public RegisteredClientBuilder productionReady() {
            return this.clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(true)
                    .requireProofKey(true)
                    .build());
        }

        /**
         * Configure for testing (no consent, no PKCE)
         */
        public RegisteredClientBuilder testFriendly() {
            return this.clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .requireProofKey(false)
                    .build());
        }

        /**
         * Set client secret with expiration
         */
        public RegisteredClientBuilder withSecretExpiration(Duration duration) {
            return this.clientSecretExpiresAt(Instant.now().plus(duration));
        }

        /**
         * Configure as mobile client (PKCE required, shorter token lifetimes)
         */
        public RegisteredClientBuilder mobileClient() {
            return this.clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(true)
                    .requireProofKey(true)
                    .build())
                   .tokenSettings(TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                    .accessTokenTimeToLive(Duration.ofMinutes(60)) // 1 hour for mobile
                    .refreshTokenTimeToLive(Duration.ofMinutes(1440)) // 24 hours
                    .reuseRefreshTokens(false) // Better security
                    .build());
        }

        /**
         * Configure as server-to-server client (client credentials only)
         */
        public RegisteredClientBuilder serverToServerClient() {
            return this.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                   .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                   .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .requireProofKey(false)
                    .build())
                   .tokenSettings(TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                    .accessTokenTimeToLive(Duration.ofMinutes(60)) // 1 hour
                    .build());
        }

        public RegisteredClient build() {
            return this.builder.build();
        }
    }
}