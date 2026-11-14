/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.authserver;

import org.greenbuttonalliance.espi.authserver.grant.GrantContextEnrichingAuthorizationService;
import org.greenbuttonalliance.espi.authserver.integration.AbstractAuthserverIntegrationTest;
import org.greenbuttonalliance.espi.authserver.service.EspiTokenCustomizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Application context test for the OpenESPI Authorization Server, booted with the real configuration
 * against the shared MySQL Testcontainer (see {@link AbstractAuthserverIntegrationTest}).
 *
 * <p>Replaces the former H2 {@code local}-profile {@code contextLoads} and the mock-{@code HttpSecurity}
 * {@code AuthorizationServerConfigTest} (#129): instead of calling {@code @Bean} methods against mocks,
 * it asserts what the started context actually contains.</p>
 */
@DisplayName("Authorization Server application context")
class AuthorizationServerApplicationTests extends AbstractAuthserverIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AuthorizationServerSettings authorizationServerSettings;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private OAuth2TokenCustomizer<JwtEncodingContext> espiTokenCustomizer;

    @Test
    @DisplayName("registers exactly the five application security filter chains")
    void registersTheApplicationSecurityFilterChains() {
        // Pinned on purpose: a new chain changes which requests each existing chain sees.
        assertThat(context.getBeansOfType(SecurityFilterChain.class))
                .containsOnlyKeys(
                        "actuatorSecurityFilterChain",   // #240: health public, the rest authserver.admin
                        "clientRegistrationSecurityFilterChain",
                        "authorizationServerSecurityFilterChain",
                        "adminApiBearerSecurityFilterChain",
                        "defaultSecurityFilterChain");
    }

    @Test
    @DisplayName("is opaque-token only: no JwtDecoder or JwtEncoder bean (#134)")
    void hasNoJwtInfrastructure() {
        assertThat(context.getBeanNamesForType(JwtDecoder.class)).isEmpty();
        assertThat(context.getBeanNamesForType(JwtEncoder.class)).isEmpty();
    }

    @Test
    @DisplayName("exposes the configured issuer and OAuth2 protocol endpoints")
    void authorizationServerSettings() {
        assertThat(authorizationServerSettings)
                .extracting(
                        AuthorizationServerSettings::getIssuer,
                        AuthorizationServerSettings::getAuthorizationEndpoint,
                        AuthorizationServerSettings::getTokenEndpoint,
                        AuthorizationServerSettings::getTokenRevocationEndpoint,
                        AuthorizationServerSettings::getTokenIntrospectionEndpoint)
                .containsExactly(
                        "http://localhost:9999",
                        "/oauth2/authorize",
                        "/oauth2/token",
                        "/oauth2/revoke",
                        "/oauth2/introspect");
    }

    @Test
    @DisplayName("wires the JDBC client repository, the grant-context authorization service and the ESPI customizer")
    void persistenceAndCustomizerBeans() {
        assertThat(registeredClientRepository).isInstanceOf(JdbcRegisteredClientRepository.class);
        assertThat(authorizationService).isInstanceOf(GrantContextEnrichingAuthorizationService.class);
        assertThat(espiTokenCustomizer).isInstanceOf(EspiTokenCustomizer.class);
    }

    @Test
    @DisplayName("seeds data_custodian_admin: client_credentials, opaque, 60 min, no consent")
    void seedsDataCustodianAdminClient() {
        assertThat(registeredClientRepository.findByClientId("data_custodian_admin"))
                .isNotNull()
                .satisfies(client -> assertThat(client)
                        .extracting(
                                RegisteredClient::getAuthorizationGrantTypes,
                                RegisteredClient::getScopes,
                                c -> c.getTokenSettings().getAccessTokenFormat(),
                                c -> c.getTokenSettings().getAccessTokenTimeToLive(),
                                c -> c.getClientSettings().isRequireAuthorizationConsent())
                        .containsExactly(
                                Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                                Set.of("DataCustodian_Admin_Access"),
                                OAuth2TokenFormat.REFERENCE,
                                Duration.ofMinutes(60),
                                false));
    }

    @Test
    @DisplayName("seeds third_party: authorization_code + refresh_token, ESPI scopes, opaque, 6 h, consent, no PKCE")
    void seedsThirdPartyClient() {
        assertThat(registeredClientRepository.findByClientId("third_party"))
                .isNotNull()
                .satisfies(client -> assertThat(client)
                        .extracting(
                                RegisteredClient::getAuthorizationGrantTypes,
                                c -> c.getTokenSettings().getAccessTokenFormat(),
                                c -> c.getTokenSettings().getAccessTokenTimeToLive(),
                                c -> c.getClientSettings().isRequireAuthorizationConsent(),
                                c -> c.getClientSettings().isRequireProofKey())
                        .containsExactly(
                                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE,
                                        AuthorizationGrantType.REFRESH_TOKEN),
                                OAuth2TokenFormat.REFERENCE,
                                Duration.ofHours(6),
                                true,
                                false))
                .extracting(RegisteredClient::getScopes)
                .satisfies(scopes -> assertThat(scopes).contains(
                        "openid", "profile",
                        "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
                        "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13"));
    }

    @Test
    @DisplayName("seeds third_party_admin and the datacustodian introspection client with opaque tokens")
    void seedsRemainingDefaultClients() {
        assertThat(registeredClientRepository.findByClientId("third_party_admin"))
                .isNotNull()
                .extracting(
                        RegisteredClient::getAuthorizationGrantTypes,
                        RegisteredClient::getScopes,
                        c -> c.getTokenSettings().getAccessTokenFormat())
                .containsExactly(
                        Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                        Set.of("ThirdParty_Admin_Access"),
                        OAuth2TokenFormat.REFERENCE);
        assertThat(registeredClientRepository.findByClientId("datacustodian"))
                .isNotNull()
                .extracting(RegisteredClient::getAuthorizationGrantTypes)
                .isEqualTo(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS));
    }
}
