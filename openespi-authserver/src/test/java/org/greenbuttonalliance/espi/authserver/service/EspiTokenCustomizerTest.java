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

package org.greenbuttonalliance.espi.authserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EspiTokenCustomizer}.
 *
 * <p>The customizer is EXPERIMENTAL: it only fires when {@code espi.token.format=jwt}; the ESPI
 * standard (and this AS's default) is opaque tokens. The tests drive it with a real
 * {@link JwtEncodingContext} and inspect the resulting claims, rather than mocking the context
 * and verifying builder calls (#129).</p>
 */
@DisplayName("EspiTokenCustomizer Tests")
class EspiTokenCustomizerTest {

    private static final String DC_ENDPOINT = "http://localhost:8080";
    private static final String RESOURCE_BASE = DC_ENDPOINT + "/espi/1_1/resource";

    private EspiTokenCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new EspiTokenCustomizer();
        ReflectionTestUtils.setField(customizer, "dataCustodianEndpoint", DC_ENDPOINT);
        ReflectionTestUtils.setField(customizer, "tokenFormat", "jwt");
    }

    @Nested
    @DisplayName("Token Format Tests")
    class TokenFormatTests {

        @ParameterizedTest(name = "espi.token.format={0} -> no ESPI claims")
        @ValueSource(strings = {"opaque", "reference"})
        @DisplayName("Should skip customization when the token format is not jwt (ESPI standard)")
        void shouldSkipCustomizationWhenTokenFormatIsNotJwt(String format) {
            ReflectionTestUtils.setField(customizer, "tokenFormat", format);

            assertThat(customize(clientCredentials("data_custodian_admin", "ROLE_DC_ADMIN")))
                    .isEmpty();
        }

        @Test
        @DisplayName("Should add the base ESPI claims when the format is jwt (experimental)")
        void shouldCustomizeTokensWhenFormatIsJwt() {
            assertThat(customize(clientCredentials("data_custodian_admin", "ROLE_DC_ADMIN")))
                    .containsEntry("espi_version", "1.1")
                    .containsEntry("data_custodian_endpoint", DC_ENDPOINT);
        }

        @Test
        @DisplayName("Should only customize access tokens")
        void shouldOnlyCustomizeAccessTokens() {
            JwtEncodingContext.Builder refreshToken = context("third_party",
                    AuthorizationGrantType.AUTHORIZATION_CODE, customer())
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN);

            assertThat(customize(refreshToken)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Client Credentials Grant Tests")
    class ClientCredentialsGrantTests {

        @Test
        @DisplayName("Should customize token for DataCustodian admin client")
        void shouldCustomizeTokenForDataCustodianAdminClient() {
            assertThat(customize(clientCredentials("data_custodian_admin", "ROLE_DC_ADMIN")))
                    .containsEntry("espi_version", "1.1")
                    .containsEntry("data_custodian_endpoint", DC_ENDPOINT)
                    .containsEntry("espi_client_type", "admin")
                    .containsEntry("espi_grant_type", "client_credentials")
                    .containsEntry("resource_uri", RESOURCE_BASE + "/")
                    .hasEntrySatisfying("authorization_uri", uri -> assertThat(uri.toString())
                            .matches(RESOURCE_BASE.replace(".", "\\.") + "/Authorization/\\d+"));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "ROLE_DC_ADMIN,        /",
                "ROLE_TP_ADMIN,        /Batch/Bulk/**",
                "ROLE_UL_ADMIN,        /Batch/Upload/**",
                "ROLE_TP_REGISTRATION, /ApplicationInformation/**",
                "ROLE_UNKNOWN,         /"
        })
        @DisplayName("Should derive the resource URI from the client's role")
        void shouldDeriveResourceUriFromRole(String authority, String resourcePath) {
            assertThat(customize(clientCredentials("admin_client", authority)))
                    .containsEntry("resource_uri", RESOURCE_BASE + resourcePath);
        }

        @Test
        @DisplayName("Should prioritize DC_ADMIN when a client holds several admin roles")
        void shouldHandleMultipleAuthoritiesCorrectly() {
            Authentication principal = new TestingAuthenticationToken("multi_role_client", null,
                    "ROLE_USER", "ROLE_DC_ADMIN", "ROLE_TP_ADMIN");

            assertThat(customize(context("multi_role_client", AuthorizationGrantType.CLIENT_CREDENTIALS, principal)))
                    .containsEntry("resource_uri", RESOURCE_BASE + "/");
        }

        @Test
        @DisplayName("Should use the configured data custodian endpoint")
        void shouldUseCustomDataCustodianEndpoint() {
            ReflectionTestUtils.setField(customizer, "dataCustodianEndpoint", "https://datacustodian.example.com");

            assertThat(customize(clientCredentials("test_client", "ROLE_DC_ADMIN")))
                    .containsEntry("data_custodian_endpoint", "https://datacustodian.example.com")
                    .containsEntry("resource_uri", "https://datacustodian.example.com/espi/1_1/resource/")
                    .hasEntrySatisfying("authorization_uri", uri -> assertThat(uri.toString())
                            .startsWith("https://datacustodian.example.com/espi/1_1/resource/Authorization/"));
        }
    }

    @Nested
    @DisplayName("Authorization Code Grant Tests")
    class AuthorizationCodeGrantTests {

        @ParameterizedTest(name = "grant_type={0} -> customer claims")
        @ValueSource(strings = {"authorization_code", "refresh_token"})
        @DisplayName("Should add customer claims for authorization_code and refresh_token grants")
        void shouldCustomizeTokenForCustomerGrants(String grantType) {
            assertThat(customize(context("third_party", new AuthorizationGrantType(grantType), customer())))
                    .containsEntry("espi_version", "1.1")
                    .containsEntry("data_custodian_endpoint", DC_ENDPOINT)
                    .containsEntry("espi_client_type", "customer")
                    .containsEntry("espi_grant_type", "authorization_code")
                    .containsEntry("resource_uri", RESOURCE_BASE + "/")
                    .hasEntrySatisfying("authorization_uri", uri -> assertThat(uri.toString())
                            .matches(RESOURCE_BASE.replace(".", "\\.") + "/Authorization/\\d+"));
        }
    }

    // Helpers

    private Map<String, Object> customize(JwtEncodingContext.Builder contextBuilder) {
        JwtEncodingContext context = contextBuilder.build();
        customizer.customize(context);
        Map<String, Object> claims = new HashMap<>();
        context.getClaims().claims(claims::putAll);
        return claims;
    }

    private static JwtEncodingContext.Builder clientCredentials(String clientId, String authority) {
        return context(clientId, AuthorizationGrantType.CLIENT_CREDENTIALS,
                new TestingAuthenticationToken(clientId, null, authority));
    }

    private static Authentication customer() {
        return new TestingAuthenticationToken("42", null, "ROLE_CUSTOMER");
    }

    private static JwtEncodingContext.Builder context(String clientId, AuthorizationGrantType grantType,
                                                      Authentication principal) {
        RegisteredClient client = RegisteredClient.withId(clientId + "-id")
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(grantType)
                .redirectUri("https://tp.example/cb")
                .build();
        return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), JwtClaimsSet.builder())
                .registeredClient(client)
                .principal(principal)
                .authorizationGrantType(grantType)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN);
    }
}
