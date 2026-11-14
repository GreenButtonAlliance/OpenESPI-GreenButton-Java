/*
 * Copyright (c) 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.authserver.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.OAuth2ClientRegistrationEndpointFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPIKE for #251: can a registration access token be added to Spring Security's plain RFC 7591
 * registration response, without bringing back OIDC client registration?
 *
 * <p>Answers spike questions 1 and 2 against Spring's REAL {@link OAuth2ClientRegistrationEndpointFilter}
 * and its JSON message converter: a decorator around the registration {@link AuthenticationProvider}
 * returns a new {@link OAuth2ClientRegistrationAuthenticationToken} whose {@link OAuth2ClientRegistration}
 * carries {@code registration_access_token} and {@code registration_client_uri} (RFC 7592 §3), and the
 * filter writes them unchanged next to the standard members.</p>
 */
@DisplayName("SPIKE #251: registration access token on the RFC 7591 response")
class RegistrationAccessTokenSpikeTest {

    private static final String REGISTRATION_CLIENT_URI =
            "https://dc.example/DataCustodian/espi/1_1/resource/ApplicationInformation/2b2c3f8e-0b8a-5d0e-9b1c-4e6f7a8b9c0d";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * The prototype decorator: run Spring's provider, then add the RFC 7592 members to its result.
     * In the real implementation {@code mintToken} mints an opaque token bound to the new client through
     * the OAuth2TokenGenerator / OAuth2AuthorizationService, and {@code clientUri} comes from the DC
     * back-channel that creates the ApplicationInformation record.
     */
    static AuthenticationProvider withRegistrationAccessToken(AuthenticationProvider delegate,
                                                              Function<OAuth2ClientRegistration, String> mintToken,
                                                              Function<OAuth2ClientRegistration, String> clientUri) {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) {
                OAuth2ClientRegistrationAuthenticationToken registered =
                        (OAuth2ClientRegistrationAuthenticationToken) delegate.authenticate(authentication);
                OAuth2ClientRegistration response = registered.getClientRegistration();
                OAuth2ClientRegistration withToken = OAuth2ClientRegistration.withClaims(response.getClaims())
                        .claim("registration_access_token", mintToken.apply(response))
                        .claim("registration_client_uri", clientUri.apply(response))
                        .build();
                return new OAuth2ClientRegistrationAuthenticationToken(
                        (Authentication) registered.getPrincipal(), withToken);
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return delegate.supports(authentication);
            }
        };
    }

    /** Stands in for Spring's OAuth2ClientRegistrationAuthenticationProvider (which needs a repository). */
    private static final AuthenticationProvider SPRING_PROVIDER_STANDIN = new AuthenticationProvider() {
        @Override
        public Authentication authenticate(Authentication authentication) {
            OAuth2ClientRegistrationAuthenticationToken request = (OAuth2ClientRegistrationAuthenticationToken) authentication;
            OAuth2ClientRegistration registered = OAuth2ClientRegistration.withClaims(request.getClientRegistration().getClaims())
                    .clientId("tp-registered-client")
                    .clientSecret("generated-client-secret")
                    .build();
            return new OAuth2ClientRegistrationAuthenticationToken((Authentication) request.getPrincipal(), registered);
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return OAuth2ClientRegistrationAuthenticationToken.class.isAssignableFrom(authentication);
        }
    };

    @Test
    @DisplayName("Q1+Q2: the real endpoint filter writes registration_access_token and registration_client_uri")
    void endpointWritesRegistrationAccessToken() throws Exception {
        // The initial access token (client.create) has already been authenticated by the resource server chain.
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("espi_registrar", null, "SCOPE_client.create"));

        AuthenticationProvider decorated = withRegistrationAccessToken(SPRING_PROVIDER_STANDIN,
                registration -> "rat-opaque-" + registration.getClientId(), registration -> REGISTRATION_CLIENT_URI);
        OAuth2ClientRegistrationEndpointFilter filter =
                new OAuth2ClientRegistrationEndpointFilter(new ProviderManager(decorated));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/register");
        request.setServletPath("/oauth2/register");
        request.setContentType("application/json");
        request.setContent("""
                {"client_name":"Spike TP","redirect_uris":["https://tp.example/cb"],
                 "grant_types":["authorization_code"],"response_types":["code"],
                 "token_endpoint_auth_method":"client_secret_basic",
                 "scope":"FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"}
                """.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(201);
        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        System.out.println("[spike #251] registration response: " + body.toPrettyString());
        assertThat(body.path("client_id").asString()).isEqualTo("tp-registered-client");
        assertThat(body.path("client_secret").asString()).isEqualTo("generated-client-secret");
        assertThat(body.path("client_name").asString()).isEqualTo("Spike TP");
        assertThat(body.path("registration_access_token").asString()).isEqualTo("rat-opaque-tp-registered-client");
        assertThat(body.path("registration_client_uri").asString()).isEqualTo(REGISTRATION_CLIENT_URI);
    }

    @Test
    @DisplayName("Q2: the decorator keeps every member Spring's provider produced")
    void decoratorPreservesSpringMembers() {
        OAuth2ClientRegistrationAuthenticationToken request = new OAuth2ClientRegistrationAuthenticationToken(
                new TestingAuthenticationToken("espi_registrar", null),
                OAuth2ClientRegistration.builder().clientName("Spike TP").redirectUri("https://tp.example/cb")
                        .grantType("authorization_code").scope("FB=4_5_15").build());

        OAuth2ClientRegistrationAuthenticationToken result = (OAuth2ClientRegistrationAuthenticationToken)
                withRegistrationAccessToken(SPRING_PROVIDER_STANDIN, r -> "rat", r -> REGISTRATION_CLIENT_URI)
                        .authenticate(request);

        assertThat(result.getClientRegistration().getClaims())
                .containsEntry("client_id", "tp-registered-client")
                .containsEntry("client_secret", "generated-client-secret")
                .containsEntry("client_name", "Spike TP")
                .containsEntry("registration_access_token", "rat")
                .containsEntry("registration_client_uri", REGISTRATION_CLIENT_URI);
    }
}
