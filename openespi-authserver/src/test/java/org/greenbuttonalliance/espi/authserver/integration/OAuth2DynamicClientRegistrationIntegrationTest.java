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

package org.greenbuttonalliance.espi.authserver.integration;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.JWKSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spike #196: RFC 7591 OAuth 2.0 Dynamic Client Registration ({@code POST /oauth2/register}) running
 * while the Authorization Server issues ONLY opaque (REFERENCE) access tokens — no OIDC, no JWK, no JWT.
 */
@DisplayName("RFC 7591 dynamic client registration with opaque tokens only (#196)")
class OAuth2DynamicClientRegistrationIntegrationTest extends AbstractAuthserverIntegrationTest {

    private static final String REGISTER = "/oauth2/register";
    /** A JWS/JWT compact serialization: three base64url segments separated by dots. */
    private static final String JWT_PATTERN = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*$";
    private static final String ESPI_SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("register -> hashed secret, opaque client_credentials token, introspection active; initial token single-use")
    void registersClientAndNewClientGetsOpaqueToken() throws Exception {
        String initialToken = clientCredentialsToken("espi_registrar", "registrar-secret", "client.create");
        assertThat(initialToken).isNotBlank().doesNotMatch(JWT_PATTERN);

        String clientName = "DCR Spike TP " + UUID.randomUUID();
        String body = mockMvc.perform(post(REGISTER)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(clientName, ESPI_SCOPE)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> registration = JsonPath.read(body, "$");
        String clientId = (String) registration.get("client_id");
        String clientSecret = (String) registration.get("client_secret");
        assertThat(registration)
                .containsKeys("client_id", "client_secret", "client_id_issued_at")
                .containsEntry("client_name", clientName)
                // Spring's RFC 7591 implementation does not issue a registration_access_token (OIDC-only).
                .doesNotContainKey("registration_access_token")
                .doesNotContainKey("registration_client_uri");
        assertThat(clientSecret).isNotBlank();

        // Persisted with a hashed secret and opaque access-token format.
        assertThat(registeredClientRepository.findByClientId(clientId))
                .isNotNull()
                .satisfies(saved -> assertThat(saved.getClientSecret()).startsWith("{bcrypt}").isNotEqualTo(clientSecret))
                .satisfies(saved -> assertThat(saved.getTokenSettings().getAccessTokenFormat())
                        .isEqualTo(OAuth2TokenFormat.REFERENCE))
                .satisfies(saved -> assertThat(saved.getScopes()).containsExactly(ESPI_SCOPE));

        // The new client mints an opaque access token via client_credentials ...
        String accessToken = clientCredentialsToken(clientId, clientSecret, ESPI_SCOPE);
        assertThat(accessToken).isNotBlank().doesNotMatch(JWT_PATTERN);

        // ... which /oauth2/introspect reports active.
        mockMvc.perform(post("/oauth2/introspect")
                        .with(httpBasic("datacustodian", "datacustodian-secret"))
                        .param("token", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.client_id").value(clientId))
                .andExpect(jsonPath("$.scope").value(ESPI_SCOPE));

        // The initial access token was invalidated by the registration (single use).
        mockMvc.perform(post(REGISTER)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("reuse " + clientName, ESPI_SCOPE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("no RFC 7592 read: GET /oauth2/register is not served even with a valid client.create token")
    void registrationReadIsNotSupported() throws Exception {
        String initialToken = clientCredentialsToken("espi_registrar", "registrar-secret", "client.create");

        mockMvc.perform(get(REGISTER).header(HttpHeaders.AUTHORIZATION, "Bearer " + initialToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("registration without a bearer token is rejected with 401")
    void registrationWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("no token", ESPI_SCOPE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("registration with a token lacking client.create is rejected with 403 insufficient_scope")
    void registrationWithoutClientCreateScopeIsRejected() throws Exception {
        String adminToken = clientCredentialsToken("data_custodian_admin", "dc-secret", "DataCustodian_Admin_Access");

        mockMvc.perform(post(REGISTER)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("wrong scope", ESPI_SCOPE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("insufficient_scope"));
    }

    @Test
    @DisplayName("registration with a bogus bearer token is rejected with 401")
    void registrationWithUnknownTokenIsRejected() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("bogus token", ESPI_SCOPE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a registrant cannot self-assign client.create or *_Admin_Access scopes")
    void privilegedScopesAreRejected() throws Exception {
        String initialToken = clientCredentialsToken("espi_registrar", "registrar-secret", "client.create");

        mockMvc.perform(post(REGISTER)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("escalation", "client.create")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_client_metadata"));
    }

    @Test
    @DisplayName("production context carries no JWKSource, JwtDecoder or JwtEncoder bean")
    void noJwtInfrastructureInContext() {
        assertThat(applicationContext.getBeanNamesForType(JWKSource.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(JwtDecoder.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(JwtEncoder.class)).isEmpty();
    }

    private String clientCredentialsToken(String clientId, String clientSecret, String scope) throws Exception {
        String body = mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(clientId, clientSecret))
                        .param("grant_type", "client_credentials")
                        .param("scope", scope))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.access_token");
    }

    private static String registrationJson(String clientName, String scope) {
        return """
                {
                  "client_name": "%s",
                  "grant_types": ["client_credentials"],
                  "token_endpoint_auth_method": "client_secret_basic",
                  "scope": "%s"
                }
                """.formatted(clientName, scope);
    }
}
