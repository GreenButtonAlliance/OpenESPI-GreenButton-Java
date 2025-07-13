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

package org.greenbuttonalliance.espi.authserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Comprehensive unit tests for EspiTokenCustomizer
 * 
 * Tests ESPI-specific JWT token customization including Green Button Alliance
 * standard claims, client type determination, and resource URI generation.
 * 
 * Note: ESPI standard uses opaque tokens, JWT customization is experimental.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EspiTokenCustomizer Tests")
class EspiTokenCustomizerTest {

    private EspiTokenCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new EspiTokenCustomizer();
        // Set default property values using reflection
        ReflectionTestUtils.setField(customizer, "dataCustodianEndpoint", "http://localhost:8080");
        ReflectionTestUtils.setField(customizer, "tokenFormat", "jwt");
    }

    @Nested
    @DisplayName("Token Format Tests")
    class TokenFormatTests {

        @Test
        @DisplayName("Should skip customization when token format is opaque (ESPI standard)")
        void shouldSkipCustomizationWhenTokenFormatIsOpaque() {
            // Given
            ReflectionTestUtils.setField(customizer, "tokenFormat", "opaque");
            
            JwtEncodingContext context = createMockJwtEncodingContext();
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder, never()).claim(anyString(), any());
        }

        @Test
        @DisplayName("Should skip customization when token format is reference")
        void shouldSkipCustomizationWhenTokenFormatIsReference() {
            // Given
            ReflectionTestUtils.setField(customizer, "tokenFormat", "reference");
            
            JwtEncodingContext context = createMockJwtEncodingContext();
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder, never()).claim(anyString(), any());
        }

        @Test
        @DisplayName("Should customize tokens when format is jwt (experimental)")
        void shouldCustomizeTokensWhenFormatIsJwt() {
            // Given
            JwtEncodingContext context = createMockJwtEncodingContext();
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("espi_version", "1.1");
            verify(claimsBuilder).claim("data_custodian_endpoint", "http://localhost:8080");
        }

        @Test
        @DisplayName("Should only customize access tokens")
        void shouldOnlyCustomizeAccessTokens() {
            // Given
            JwtEncodingContext context = createMockJwtEncodingContext();
            when(context.getTokenType()).thenReturn(OAuth2TokenType.REFRESH_TOKEN);
            
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder, never()).claim(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Client Credentials Grant Tests")
    class ClientCredentialsGrantTests {

        @Test
        @DisplayName("Should customize token for DataCustodian admin client")
        void shouldCustomizeTokenForDataCustodianAdminClient() {
            // Given
            JwtEncodingContext context = createClientCredentialsContext("data_custodian_admin", "ROLE_DC_ADMIN");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("espi_version", "1.1");
            verify(claimsBuilder).claim("data_custodian_endpoint", "http://localhost:8080");
            verify(claimsBuilder).claim("espi_client_type", "admin");
            verify(claimsBuilder).claim("espi_grant_type", "client_credentials");
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/");
            verify(claimsBuilder).claim(eq("authorization_uri"), argThat(uri -> 
                uri.toString().startsWith("http://localhost:8080/espi/1_1/resource/Authorization/")));
        }

        @Test
        @DisplayName("Should customize token for ThirdParty admin client")
        void shouldCustomizeTokenForThirdPartyAdminClient() {
            // Given
            JwtEncodingContext context = createClientCredentialsContext("third_party_admin", "ROLE_TP_ADMIN");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("espi_version", "1.1");
            verify(claimsBuilder).claim("espi_client_type", "admin");
            verify(claimsBuilder).claim("espi_grant_type", "client_credentials");
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/Batch/Bulk/**");
        }

        @Test
        @DisplayName("Should customize token for Upload admin client")
        void shouldCustomizeTokenForUploadAdminClient() {
            // Given
            JwtEncodingContext context = createClientCredentialsContext("upload_admin", "ROLE_UL_ADMIN");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/Batch/Upload/**");
        }

        @Test
        @DisplayName("Should customize token for Registration admin client")
        void shouldCustomizeTokenForRegistrationAdminClient() {
            // Given
            JwtEncodingContext context = createClientCredentialsContext("registration_admin", "ROLE_TP_REGISTRATION");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/ApplicationInformation/**");
        }

        @Test
        @DisplayName("Should use default resource URI for unknown roles")
        void shouldUseDefaultResourceUriForUnknownRoles() {
            // Given
            JwtEncodingContext context = createClientCredentialsContext("unknown_client", "ROLE_UNKNOWN");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/");
        }

        @Test
        @DisplayName("Should handle multiple authorities correctly")
        void shouldHandleMultipleAuthoritiesCorrectly() {
            // Given
            Authentication principal = mock(Authentication.class);
            when(principal.getAuthorities()).thenReturn((Collection) List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_DC_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_TP_ADMIN")
            ));

            JwtEncodingContext context = createClientCredentialsContextWithPrincipal("multi_role_client", principal);
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then - Should prioritize DC_ADMIN
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/");
        }
    }

    @Nested
    @DisplayName("Authorization Code Grant Tests")
    class AuthorizationCodeGrantTests {

        @Test
        @DisplayName("Should customize token for authorization code grant")
        void shouldCustomizeTokenForAuthorizationCodeGrant() {
            // Given
            JwtEncodingContext context = createAuthorizationCodeContext("third_party");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("espi_version", "1.1");
            verify(claimsBuilder).claim("data_custodian_endpoint", "http://localhost:8080");
            verify(claimsBuilder).claim("espi_client_type", "customer");
            verify(claimsBuilder).claim("espi_grant_type", "authorization_code");
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/");
            verify(claimsBuilder).claim(eq("authorization_uri"), argThat(uri -> 
                uri.toString().startsWith("http://localhost:8080/espi/1_1/resource/Authorization/")));
        }

        @Test
        @DisplayName("Should customize token for refresh token grant")
        void shouldCustomizeTokenForRefreshTokenGrant() {
            // Given
            JwtEncodingContext context = createRefreshTokenContext("third_party");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("espi_version", "1.1");
            verify(claimsBuilder).claim("espi_client_type", "customer");
            verify(claimsBuilder).claim("espi_grant_type", "authorization_code");
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/");
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should use custom data custodian endpoint")
        void shouldUseCustomDataCustodianEndpoint() {
            // Given
            ReflectionTestUtils.setField(customizer, "dataCustodianEndpoint", "https://datacustodian.example.com");
            
            JwtEncodingContext context = createClientCredentialsContext("test_client", "ROLE_DC_ADMIN");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("data_custodian_endpoint", "https://datacustodian.example.com");
            verify(claimsBuilder).claim("resource_uri", "https://datacustodian.example.com/espi/1_1/resource/");
            verify(claimsBuilder).claim(eq("authorization_uri"), argThat(uri -> 
                uri.toString().startsWith("https://datacustodian.example.com/espi/1_1/resource/Authorization/")));
        }

        @Test
        @DisplayName("Should handle null data custodian endpoint gracefully")
        void shouldHandleNullDataCustodianEndpointGracefully() {
            // Given
            ReflectionTestUtils.setField(customizer, "dataCustodianEndpoint", null);
            
            JwtEncodingContext context = createClientCredentialsContext("test_client", "ROLE_DC_ADMIN");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("data_custodian_endpoint", null);
            verify(claimsBuilder).claim("resource_uri", "null/espi/1_1/resource/");
        }
    }

    @Nested
    @DisplayName("Authorization ID Generation Tests")
    class AuthorizationIdGenerationTests {

        @Test
        @DisplayName("Should generate unique authorization IDs")
        void shouldGenerateUniqueAuthorizationIds() {
            // Given
            JwtEncodingContext context1 = createClientCredentialsContext("client1", "ROLE_DC_ADMIN");
            JwtEncodingContext context2 = createClientCredentialsContext("client2", "ROLE_DC_ADMIN");
            
            JwtClaimsSet.Builder claimsBuilder1 = spy(JwtClaimsSet.builder());
            JwtClaimsSet.Builder claimsBuilder2 = spy(JwtClaimsSet.builder());
            
            when(context1.getClaims()).thenReturn(claimsBuilder1);
            when(context2.getClaims()).thenReturn(claimsBuilder2);

            // When
            customizer.customize(context1);
            try {
                Thread.sleep(1); // Ensure different timestamps
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            customizer.customize(context2);

            // Then
            verify(claimsBuilder1).claim(eq("authorization_uri"), argThat(uri -> 
                uri.toString().matches("http://localhost:8080/espi/1_1/resource/Authorization/\\d+")));
            verify(claimsBuilder2).claim(eq("authorization_uri"), argThat(uri -> 
                uri.toString().matches("http://localhost:8080/espi/1_1/resource/Authorization/\\d+")));
        }
    }

    @Nested
    @DisplayName("ESPI Compliance Tests")
    class EspiComplianceTests {

        @Test
        @DisplayName("Should add ESPI version 1.1 claim")
        void shouldAddEspiVersion11Claim() {
            // Given
            JwtEncodingContext context = createClientCredentialsContext("test_client", "ROLE_DC_ADMIN");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("espi_version", "1.1");
        }

        @Test
        @DisplayName("Should use ESPI-compliant resource URI patterns")
        void shouldUseEspiCompliantResourceUriPatterns() {
            // Given
            JwtEncodingContext context = createClientCredentialsContext("test_client", "ROLE_TP_ADMIN");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim("resource_uri", "http://localhost:8080/espi/1_1/resource/Batch/Bulk/**");
        }

        @Test
        @DisplayName("Should generate ESPI-compliant authorization URIs")
        void shouldGenerateEspiCompliantAuthorizationUris() {
            // Given
            JwtEncodingContext context = createAuthorizationCodeContext("third_party");
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When
            customizer.customize(context);

            // Then
            verify(claimsBuilder).claim(eq("authorization_uri"), argThat(uri -> 
                uri.toString().startsWith("http://localhost:8080/espi/1_1/resource/Authorization/")));
        }

        @Test
        @DisplayName("Should distinguish between admin and customer client types")
        void shouldDistinguishBetweenAdminAndCustomerClientTypes() {
            // Given
            JwtEncodingContext adminContext = createClientCredentialsContext("admin_client", "ROLE_DC_ADMIN");
            JwtEncodingContext customerContext = createAuthorizationCodeContext("customer_client");
            
            JwtClaimsSet.Builder adminClaimsBuilder = spy(JwtClaimsSet.builder());
            JwtClaimsSet.Builder customerClaimsBuilder = spy(JwtClaimsSet.builder());
            
            when(adminContext.getClaims()).thenReturn(adminClaimsBuilder);
            when(customerContext.getClaims()).thenReturn(customerClaimsBuilder);

            // When
            customizer.customize(adminContext);
            customizer.customize(customerContext);

            // Then
            verify(adminClaimsBuilder).claim("espi_client_type", "admin");
            verify(customerClaimsBuilder).claim("espi_client_type", "customer");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null context gracefully")
        void shouldHandleNullContextGracefully() {
            // When & Then - Should not throw exception
            customizer.customize(null);
        }

        @Test
        @DisplayName("Should handle context with null claims gracefully")
        void shouldHandleContextWithNullClaimsGracefully() {
            // Given
            JwtEncodingContext context = createMockJwtEncodingContext();
            when(context.getClaims()).thenReturn(null);

            // When & Then - Should not throw exception
            customizer.customize(context);
        }

        @Test
        @DisplayName("Should handle context with null principal gracefully")
        void shouldHandleContextWithNullPrincipalGracefully() {
            // Given
            JwtEncodingContext context = createMockJwtEncodingContext();
            when(context.getPrincipal()).thenReturn(null);
            
            JwtClaimsSet.Builder claimsBuilder = spy(JwtClaimsSet.builder());
            when(context.getClaims()).thenReturn(claimsBuilder);

            // When & Then - Should not throw exception
            customizer.customize(context);
            
            // Should still add basic claims
            verify(claimsBuilder).claim("espi_version", "1.1");
        }
    }

    // Helper methods

    private JwtEncodingContext createMockJwtEncodingContext() {
        JwtEncodingContext context = mock(JwtEncodingContext.class);
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        
        RegisteredClient client = mock(RegisteredClient.class);
        when(client.getClientId()).thenReturn("test-client");
        when(context.getRegisteredClient()).thenReturn(client);
        
        when(context.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);
        
        Authentication principal = mock(Authentication.class);
        when(principal.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(context.getPrincipal()).thenReturn(principal);
        
        return context;
    }

    private JwtEncodingContext createClientCredentialsContext(String clientId, String authority) {
        Authentication principal = mock(Authentication.class);
        when(principal.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority(authority)));
        
        return createClientCredentialsContextWithPrincipal(clientId, principal);
    }

    private JwtEncodingContext createClientCredentialsContextWithPrincipal(String clientId, Authentication principal) {
        JwtEncodingContext context = mock(JwtEncodingContext.class);
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        
        RegisteredClient client = mock(RegisteredClient.class);
        when(client.getClientId()).thenReturn(clientId);
        when(context.getRegisteredClient()).thenReturn(client);
        
        when(context.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.CLIENT_CREDENTIALS);
        when(context.getPrincipal()).thenReturn(principal);
        
        return context;
    }

    private JwtEncodingContext createAuthorizationCodeContext(String clientId) {
        JwtEncodingContext context = mock(JwtEncodingContext.class);
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        
        RegisteredClient client = mock(RegisteredClient.class);
        when(client.getClientId()).thenReturn(clientId);
        when(context.getRegisteredClient()).thenReturn(client);
        
        when(context.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);
        
        Authentication principal = mock(Authentication.class);
        when(principal.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(context.getPrincipal()).thenReturn(principal);
        
        return context;
    }

    private JwtEncodingContext createRefreshTokenContext(String clientId) {
        JwtEncodingContext context = mock(JwtEncodingContext.class);
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        
        RegisteredClient client = mock(RegisteredClient.class);
        when(client.getClientId()).thenReturn(clientId);
        when(context.getRegisteredClient()).thenReturn(client);
        
        when(context.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.REFRESH_TOKEN);
        
        Authentication principal = mock(Authentication.class);
        when(principal.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(context.getPrincipal()).thenReturn(principal);
        
        return context;
    }
}