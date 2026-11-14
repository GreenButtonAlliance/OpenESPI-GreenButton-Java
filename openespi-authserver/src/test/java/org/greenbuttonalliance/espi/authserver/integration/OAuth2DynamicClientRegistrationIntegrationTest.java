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
import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelResponse;
import org.greenbuttonalliance.espi.authserver.backchannel.DataCustodianBackchannelClient;
import org.greenbuttonalliance.espi.authserver.web.delegate.DelegationStateService;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RFC 7591 OAuth 2.0 Dynamic Client Registration ({@code POST /oauth2/register}, #196) while the
 * Authorization Server issues ONLY opaque (REFERENCE) access tokens — no OIDC, no JWK, no JWT.
 *
 * <p>The registrar secret comes from {@code espi.authserver.registrar.client-secret} in
 * {@code application-testcontainers.yml}. The DC back-channel is mocked so a registered client can be
 * driven through the full ESPI authorization_code flow (same harness as
 * {@link AuthCodeFlowOrchestrationIntegrationTest}).</p>
 */
@DisplayName("RFC 7591 dynamic client registration with opaque tokens only (#196)")
class OAuth2DynamicClientRegistrationIntegrationTest extends AbstractAuthserverIntegrationTest {

    private static final String REGISTER = "/oauth2/register";
    private static final String REGISTRAR_ID = "espi_registrar";
    private static final String REGISTRAR_SECRET = "registrar-test-secret";
    /** A JWS/JWT compact serialization: three base64url segments separated by dots. */
    private static final String JWT_PATTERN = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*$";
    private static final String ESPI_SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";
    private static final String REDIRECT_URI = "https://dcr-tp.example/cb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DelegationStateService delegationStateService;

    @Autowired
    private SignedHandoffCodec codec;

    @Autowired
    private OAuth2AuthorizationConsentService consentService;

    @MockitoBean
    private DataCustodianBackchannelClient backchannelClient;

    @Test
    @DisplayName("register -> hashed secret, REFERENCE format; client completes the ESPI auth-code flow with an opaque, introspectable token")
    void registeredClientCompletesAuthCodeFlowWithOpaqueToken() throws Exception {
        String initialToken = initialAccessToken();
        assertThat(initialToken).isNotBlank().doesNotMatch(JWT_PATTERN);

        String clientName = "DCR TP " + UUID.randomUUID();
        String body = register(initialToken, registrationJson(clientName, ESPI_SCOPE))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> registration = JsonPath.read(body, "$");
        String clientId = (String) registration.get("client_id");
        String clientSecret = (String) registration.get("client_secret");
        assertThat(registration)
                .containsKeys("client_id", "client_secret", "client_id_issued_at")
                .containsEntry("client_name", clientName)
                .containsEntry("token_endpoint_auth_method", "client_secret_basic")
                // Spring's RFC 7591 implementation does not issue a registration_access_token (see #203).
                .doesNotContainKeys("registration_access_token", "registration_client_uri");

        assertThat(registeredClientRepository.findByClientId(clientId))
                .isNotNull()
                .satisfies(saved -> assertThat(saved.getClientSecret()).startsWith("{bcrypt}").doesNotContain(clientSecret))
                .satisfies(saved -> assertThat(saved.getTokenSettings().getAccessTokenFormat())
                        .isEqualTo(OAuth2TokenFormat.REFERENCE))
                .satisfies(saved -> assertThat(saved.getClientSettings().isRequireProofKey()).isFalse())
                .satisfies(saved -> assertThat(saved.getScopes()).containsExactly(ESPI_SCOPE))
                .satisfies(saved -> assertThat(saved.getRedirectUris()).containsExactly(REDIRECT_URI))
                .satisfies(saved -> assertThat(saved.getAuthorizationGrantTypes()).containsExactlyInAnyOrder(
                        AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN))
                .satisfies(saved -> assertThat(saved.getClientAuthenticationMethods())
                        .containsExactly(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));

        // The registered client runs the ESPI authorization_code flow and receives an opaque token ...
        String accessToken = authorizationCodeAccessToken(clientId, clientSecret);
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
        register(initialToken, registrationJson("reuse " + clientName, ESPI_SCOPE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("registrar secret is stored PasswordEncoder-encoded, not {noop}")
    void registrarSecretIsEncoded() {
        assertThat(registeredClientRepository.findByClientId(REGISTRAR_ID))
                .isNotNull()
                .satisfies(registrar -> assertThat(registrar.getClientSecret())
                        .startsWith("{bcrypt}")
                        .doesNotContain(REGISTRAR_SECRET))
                .satisfies(registrar -> assertThat(registrar.getScopes()).containsExactly("client.create"));
    }

    @Test
    @DisplayName("discovery metadata advertises registration_endpoint")
    void metadataAdvertisesRegistrationEndpoint() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registration_endpoint").value("http://localhost:9999/oauth2/register"));
    }

    @Test
    @DisplayName("no RFC 7592 read: GET /oauth2/register is not served even with a valid client.create token")
    void registrationReadIsNotSupported() throws Exception {
        mockMvc.perform(get(REGISTER).header(HttpHeaders.AUTHORIZATION, "Bearer " + initialAccessToken()))
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
    @DisplayName("registration with a bogus bearer token is rejected with 401")
    void registrationWithUnknownTokenIsRejected() throws Exception {
        register("not-a-real-token", registrationJson("bogus token", ESPI_SCOPE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("registration with a token lacking client.create is rejected with 403 insufficient_scope")
    void registrationWithoutClientCreateScopeIsRejected() throws Exception {
        String adminToken = clientCredentialsToken("data_custodian_admin", "dc-secret", "DataCustodian_Admin_Access");

        register(adminToken, registrationJson("wrong scope", ESPI_SCOPE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("insufficient_scope"));
    }

    @ParameterizedTest(name = "rejects {0}")
    @ValueSource(strings = {
            "scope outside allow-list",
            "client.create scope",
            "admin scope",
            "missing scope",
            "client_credentials grant",
            "refresh_token without authorization_code",
            "token response_type",
            "client_secret_post auth method",
            "public client (none)",
            "missing redirect_uris"
    })
    @DisplayName("registration metadata outside ESPI policy is rejected with 400 invalid_client_metadata")
    void nonEspiMetadataIsRejected(String violation) throws Exception {
        String json = switch (violation) {
            case "scope outside allow-list" -> registrationJson("x", "openid");
            case "client.create scope" -> registrationJson("x", "client.create");
            case "admin scope" -> registrationJson("x", ESPI_SCOPE + " DataCustodian_Admin_Access");
            case "missing scope" -> """
                    {"client_name":"x","redirect_uris":["%s"]}""".formatted(REDIRECT_URI);
            case "client_credentials grant" -> registrationJson("x", ESPI_SCOPE)
                    .replace("\"authorization_code\", \"refresh_token\"", "\"client_credentials\"");
            case "refresh_token without authorization_code" -> registrationJson("x", ESPI_SCOPE)
                    .replace("\"authorization_code\", \"refresh_token\"", "\"refresh_token\"");
            case "token response_type" -> registrationJson("x", ESPI_SCOPE)
                    .replace("[\"code\"]", "[\"token\"]");
            case "client_secret_post auth method" -> registrationJson("x", ESPI_SCOPE)
                    .replace("client_secret_basic", "client_secret_post");
            case "public client (none)" -> registrationJson("x", ESPI_SCOPE)
                    .replace("client_secret_basic", "none");
            case "missing redirect_uris" -> """
                    {"client_name":"x","scope":"%s"}""".formatted(ESPI_SCOPE);
            default -> throw new IllegalArgumentException(violation);
        };

        register(initialAccessToken(), json)
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

    private org.springframework.test.web.servlet.ResultActions register(String bearer, String json) throws Exception {
        return mockMvc.perform(post(REGISTER)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String initialAccessToken() throws Exception {
        return clientCredentialsToken(REGISTRAR_ID, REGISTRAR_SECRET, "client.create");
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

    /** Drives the ESPI authorization_code flow (DC-delegated consent, back-channel mocked) for a client. */
    private String authorizationCodeAccessToken(String clientId, String clientSecret) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String correlationId = "dcr-corr-" + suffix;
        delegationStateService.save(correlationId, clientId, REDIRECT_URI, "dcr-state-" + suffix);
        when(backchannelClient.provision(any())).thenReturn(new BackchannelResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "https://dc.example/Subscription/1", "https://dc.example/Authorization/1",
                "https://dc.example/RetailCustomer/42/Customer/1"));

        Instant now = Instant.now();
        String handoff = codec.encode(SignedHandoff.Return.of(
                correlationId, now, now.plusSeconds(300),
                UUID.randomUUID().toString().replace("-", ""),
                "42",
                List.of(UUID.randomUUID()),
                SignedHandoff.Return.CONSENT_ALLOW,
                ESPI_SCOPE));

        MvcResult continueResult = mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", handoff))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/oauth2/authorize")))
                .andReturn();
        MockHttpSession session = (MockHttpSession) continueResult.getRequest().getSession(false);

        // Workaround for a separate, pre-existing issue (not #196): AuthorizeContinueController stores the
        // approved scope in the consent store split on ';' (FB=4_5_15 / IntervalDuration=3600 / ...), while
        // Spring AS parses the replayed scope parameter on spaces, so a full ESPI scope string never matches
        // the stored consent. Re-save the consent with the scope token Spring AS will compare against.
        consentService.save(OAuth2AuthorizationConsent
                .withId(registeredClientRepository.findByClientId(clientId).getId(), "42")
                .scope(ESPI_SCOPE)
                .build());

        // URI (not a String template) so MockMvc does not re-encode the already-encoded ESPI scope (%3D).
        String location = mockMvc.perform(get(URI.create(continueResult.getResponse().getHeader("Location")))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getHeader("Location");
        assertThat(location).startsWith(REDIRECT_URI).contains("code=");
        String code = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("code");

        String body = mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(clientId, clientSecret))
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.access_token");
    }

    private static String registrationJson(String clientName, String scope) {
        return """
                {
                  "client_name": "%s",
                  "redirect_uris": ["%s"],
                  "grant_types": ["authorization_code", "refresh_token"],
                  "response_types": ["code"],
                  "token_endpoint_auth_method": "client_secret_basic",
                  "scope": "%s"
                }
                """.formatted(clientName, REDIRECT_URI, scope);
    }
}
