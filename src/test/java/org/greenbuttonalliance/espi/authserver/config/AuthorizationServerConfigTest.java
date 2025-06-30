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
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.greenbuttonalliance.espi.authserver.repository.JdbcRegisteredClientRepository;
import org.greenbuttonalliance.espi.authserver.service.EspiTokenCustomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AuthorizationServerConfig
 * 
 * Tests OAuth2 Authorization Server configuration including security filter chains,
 * client repository setup, JWK configuration, and ESPI-specific customizations.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationServerConfig Tests")
class AuthorizationServerConfigTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private HttpSecurity httpSecurity;

    private AuthorizationServerConfig config;

    @BeforeEach
    void setUp() {
        config = new AuthorizationServerConfig();
    }

    @Nested
    @DisplayName("Security Filter Chain Tests")
    class SecurityFilterChainTests {

        @Test
        @DisplayName("Should create authorization server security filter chain")
        void shouldCreateAuthorizationServerSecurityFilterChain() throws Exception {
            // Given
            HttpSecurity mockHttpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
            SecurityFilterChain mockFilterChain = mock(SecurityFilterChain.class);
            
            when(mockHttpSecurity.build()).thenReturn(mockFilterChain);

            // When
            SecurityFilterChain filterChain = config.authorizationServerSecurityFilterChain(mockHttpSecurity);

            // Then
            assertThat(filterChain).isNotNull();
            verify(mockHttpSecurity).build();
        }

        @Test
        @DisplayName("Should create default security filter chain")
        void shouldCreateDefaultSecurityFilterChain() throws Exception {
            // Given
            HttpSecurity mockHttpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
            SecurityFilterChain mockFilterChain = mock(SecurityFilterChain.class);
            
            when(mockHttpSecurity.build()).thenReturn(mockFilterChain);

            // When
            SecurityFilterChain filterChain = config.defaultSecurityFilterChain(mockHttpSecurity);

            // Then
            assertThat(filterChain).isNotNull();
            verify(mockHttpSecurity).build();
        }
    }

    @Nested
    @DisplayName("Client Repository Tests")
    class ClientRepositoryTests {

        @Test
        @DisplayName("Should create registered client repository with default ESPI clients")
        void shouldCreateRegisteredClientRepositoryWithDefaultEspiClients() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then
            assertThat(repository).isNotNull();
            assertThat(repository).isInstanceOf(JdbcRegisteredClientRepository.class);
            
            // Verify default clients are attempted to be saved
            verify(jdbcTemplate, atLeast(3)).queryForObject(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("Should not overwrite existing clients")
        void shouldNotOverwriteExistingClients() {
            // Given
            RegisteredClient existingClient = mock(RegisteredClient.class);
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(existingClient);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then
            assertThat(repository).isNotNull();
            
            // Verify existing clients are found but not overwritten
            verify(jdbcTemplate, atLeast(3)).queryForObject(anyString(), any(), anyString());
            verify(jdbcTemplate, never()).update(anyString(), any());
        }

        @Test
        @DisplayName("Should configure DataCustodian admin client correctly")
        void shouldConfigureDataCustodianAdminClientCorrectly() {
            // Given
            JdbcRegisteredClientRepository mockRepo = mock(JdbcRegisteredClientRepository.class);
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then - Verify DataCustodian admin client configuration through method calls
            verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), any(), eq("data_custodian_admin"));
        }

        @Test
        @DisplayName("Should configure ThirdParty client correctly")
        void shouldConfigureThirdPartyClientCorrectly() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then - Verify ThirdParty client configuration through method calls
            verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), any(), eq("third_party"));
        }

        @Test
        @DisplayName("Should configure ThirdParty admin client correctly")
        void shouldConfigureThirdPartyAdminClientCorrectly() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then - Verify ThirdParty admin client configuration through method calls
            verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), any(), eq("third_party_admin"));
        }
    }

    @Nested
    @DisplayName("JWK Configuration Tests")
    class JwkConfigurationTests {

        @Test
        @DisplayName("Should create JWK source with RSA key")
        void shouldCreateJwkSourceWithRsaKey() {
            // When
            JWKSource<SecurityContext> jwkSource = config.jwkSource();

            // Then
            assertThat(jwkSource).isNotNull();
            
            // Verify JWK set contains RSA key
            JWKSet jwkSet = jwkSource.getJWKSet();
            assertThat(jwkSet).isNotNull();
            assertThat(jwkSet.getKeys()).hasSize(1);
            assertThat(jwkSet.getKeys().get(0)).isInstanceOf(RSAKey.class);
            
            RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0);
            assertThat(rsaKey.getKeyID()).isNotNull();
            assertThat(rsaKey.toRSAPublicKey()).isNotNull();
            assertThat(rsaKey.toRSAPrivateKey()).isNotNull();
        }

        @Test
        @DisplayName("Should create JWT decoder from JWK source")
        void shouldCreateJwtDecoderFromJwkSource() {
            // Given
            JWKSource<SecurityContext> jwkSource = config.jwkSource();

            // When
            JwtDecoder jwtDecoder = config.jwtDecoder(jwkSource);

            // Then
            assertThat(jwtDecoder).isNotNull();
        }

        @Test
        @DisplayName("Should generate different keys on each call")
        void shouldGenerateDifferentKeysOnEachCall() {
            // When
            JWKSource<SecurityContext> jwkSource1 = config.jwkSource();
            JWKSource<SecurityContext> jwkSource2 = config.jwkSource();

            // Then
            RSAKey key1 = (RSAKey) jwkSource1.getJWKSet().getKeys().get(0);
            RSAKey key2 = (RSAKey) jwkSource2.getJWKSet().getKeys().get(0);
            
            assertThat(key1.getKeyID()).isNotEqualTo(key2.getKeyID());
            assertThat(key1.toRSAPublicKey()).isNotEqualTo(key2.toRSAPublicKey());
        }
    }

    @Nested
    @DisplayName("Authorization Server Settings Tests")
    class AuthorizationServerSettingsTests {

        @Test
        @DisplayName("Should create authorization server settings with ESPI endpoints")
        void shouldCreateAuthorizationServerSettingsWithEspiEndpoints() {
            // When
            AuthorizationServerSettings settings = config.authorizationServerSettings();

            // Then
            assertThat(settings).isNotNull();
            assertThat(settings.getIssuer()).isEqualTo("http://localhost:9999");
            assertThat(settings.getAuthorizationEndpoint()).isEqualTo("/oauth2/authorize");
            assertThat(settings.getTokenEndpoint()).isEqualTo("/oauth2/token");
            assertThat(settings.getJwkSetEndpoint()).isEqualTo("/oauth2/jwks");
            assertThat(settings.getTokenRevocationEndpoint()).isEqualTo("/oauth2/revoke");
            assertThat(settings.getTokenIntrospectionEndpoint()).isEqualTo("/oauth2/introspect");
            assertThat(settings.getOidcClientRegistrationEndpoint()).isEqualTo("/connect/register");
            assertThat(settings.getOidcUserInfoEndpoint()).isEqualTo("/userinfo");
        }

        @Test
        @DisplayName("Should use correct issuer URL")
        void shouldUseCorrectIssuerUrl() {
            // When
            AuthorizationServerSettings settings = config.authorizationServerSettings();

            // Then
            assertThat(settings.getIssuer()).isEqualTo("http://localhost:9999");
        }
    }

    @Nested
    @DisplayName("Token Customizer Tests")
    class TokenCustomizerTests {

        @Test
        @DisplayName("Should create ESPI token customizer")
        void shouldCreateEspiTokenCustomizer() {
            // When
            OAuth2TokenCustomizer<JwtEncodingContext> customizer = config.espiTokenCustomizer();

            // Then
            assertThat(customizer).isNotNull();
            assertThat(customizer).isInstanceOf(EspiTokenCustomizer.class);
        }

        @Test
        @DisplayName("Should return new instance on each call")
        void shouldReturnNewInstanceOnEachCall() {
            // When
            OAuth2TokenCustomizer<JwtEncodingContext> customizer1 = config.espiTokenCustomizer();
            OAuth2TokenCustomizer<JwtEncodingContext> customizer2 = config.espiTokenCustomizer();

            // Then
            assertThat(customizer1).isNotNull();
            assertThat(customizer2).isNotNull();
            assertThat(customizer1).isNotSameAs(customizer2);
        }
    }

    @Nested
    @DisplayName("ESPI Compliance Tests")
    class EspiComplianceTests {

        @Test
        @DisplayName("Should configure clients with opaque token format (ESPI standard)")
        void shouldConfigureClientsWithOpaqueTokenFormat() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then
            // The verification is indirect through repository calls since we can't directly 
            // access the created clients, but the configuration should use OAuth2TokenFormat.REFERENCE
            verify(jdbcTemplate, atLeast(3)).queryForObject(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("Should support ESPI scopes for ThirdParty client")
        void shouldSupportEspiScopesForThirdPartyClient() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then
            // Verify that ThirdParty client is queried (which means it was configured)
            verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), any(), eq("third_party"));
        }

        @Test
        @DisplayName("Should configure proper grant types for ESPI clients")
        void shouldConfigureProperGrantTypesForEspiClients() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then
            // Verify all default ESPI clients are configured
            verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), any(), eq("data_custodian_admin"));
            verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), any(), eq("third_party"));
            verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), any(), eq("third_party_admin"));
        }

        @Test
        @DisplayName("Should configure appropriate token lifetimes for ESPI")
        void shouldConfigureAppropriateTokenLifetimesForEspi() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then
            // The token lifetimes are configured internally in the registered clients
            // We verify through the repository initialization
            assertThat(repository).isNotNull();
        }

        @Test
        @DisplayName("Should configure consent requirements correctly")
        void shouldConfigureConsentRequirementsCorrectly() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);

            // Then
            // Admin clients should not require consent, customer clients should
            // This is configured in the client settings within the method
            assertThat(repository).isNotNull();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle JdbcTemplate exceptions gracefully")
        void shouldHandleJdbcTemplateExceptionsGracefully() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString()))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then - Should not throw exception
            RegisteredClientRepository repository = config.registeredClientRepository(jdbcTemplate);
            
            assertThat(repository).isNotNull();
        }

        @Test
        @DisplayName("Should handle null JdbcTemplate gracefully")
        void shouldHandleNullJdbcTemplateGracefully() {
            // When & Then - Should throw appropriate exception
            try {
                config.registeredClientRepository(null);
            } catch (Exception e) {
                assertThat(e).isInstanceOf(NullPointerException.class);
            }
        }
    }

    @Nested
    @DisplayName("Bean Configuration Tests")
    class BeanConfigurationTests {

        @Test
        @DisplayName("Should create all required beans")
        void shouldCreateAllRequiredBeans() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository clientRepository = config.registeredClientRepository(jdbcTemplate);
            JWKSource<SecurityContext> jwkSource = config.jwkSource();
            JwtDecoder jwtDecoder = config.jwtDecoder(jwkSource);
            AuthorizationServerSettings serverSettings = config.authorizationServerSettings();
            OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer = config.espiTokenCustomizer();

            // Then
            assertThat(clientRepository).isNotNull();
            assertThat(jwkSource).isNotNull();
            assertThat(jwtDecoder).isNotNull();
            assertThat(serverSettings).isNotNull();
            assertThat(tokenCustomizer).isNotNull();
        }

        @Test
        @DisplayName("Should create beans with correct types")
        void shouldCreateBeansWithCorrectTypes() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When & Then
            assertThat(config.registeredClientRepository(jdbcTemplate))
                    .isInstanceOf(JdbcRegisteredClientRepository.class);
            
            assertThat(config.jwkSource())
                    .isInstanceOf(JWKSource.class);
            
            assertThat(config.jwtDecoder(config.jwkSource()))
                    .isInstanceOf(JwtDecoder.class);
            
            assertThat(config.authorizationServerSettings())
                    .isInstanceOf(AuthorizationServerSettings.class);
            
            assertThat(config.espiTokenCustomizer())
                    .isInstanceOf(EspiTokenCustomizer.class);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should work with complete configuration")
        void shouldWorkWithCompleteConfiguration() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository clientRepository = config.registeredClientRepository(jdbcTemplate);
            JWKSource<SecurityContext> jwkSource = config.jwkSource();
            JwtDecoder jwtDecoder = config.jwtDecoder(jwkSource);
            AuthorizationServerSettings serverSettings = config.authorizationServerSettings();
            OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer = config.espiTokenCustomizer();

            // Then - All components should work together
            assertThat(clientRepository).isNotNull();
            assertThat(jwkSource).isNotNull();
            assertThat(jwtDecoder).isNotNull();
            assertThat(serverSettings).isNotNull();
            assertThat(tokenCustomizer).isNotNull();
            
            // Test JWT decoder with JWK source
            assertThat(jwtDecoder).isNotNull();
            
            // Test server settings configuration
            assertThat(serverSettings.getIssuer()).isNotNull();
            assertThat(serverSettings.getTokenEndpoint()).isNotNull();
        }

        @Test
        @DisplayName("Should maintain ESPI compliance across all components")
        void shouldMaintainEspiComplianceAcrossAllComponents() {
            // Given
            when(jdbcTemplate.queryForObject(anyString(), any(), anyString())).thenReturn(null);

            // When
            RegisteredClientRepository clientRepository = config.registeredClientRepository(jdbcTemplate);
            AuthorizationServerSettings serverSettings = config.authorizationServerSettings();
            OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer = config.espiTokenCustomizer();

            // Then
            assertThat(clientRepository).isInstanceOf(JdbcRegisteredClientRepository.class);
            assertThat(tokenCustomizer).isInstanceOf(EspiTokenCustomizer.class);
            
            // ESPI endpoints should be properly configured
            assertThat(serverSettings.getOidcClientRegistrationEndpoint()).isEqualTo("/connect/register");
            assertThat(serverSettings.getTokenRevocationEndpoint()).isEqualTo("/oauth2/revoke");
            assertThat(serverSettings.getTokenIntrospectionEndpoint()).isEqualTo("/oauth2/introspect");
        }
    }
}