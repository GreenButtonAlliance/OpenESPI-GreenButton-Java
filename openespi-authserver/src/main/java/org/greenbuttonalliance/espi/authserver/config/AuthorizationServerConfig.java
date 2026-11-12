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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import org.greenbuttonalliance.espi.authserver.admin.AdminLoginSuccessHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authorization.AllRequiredFactorsAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import java.util.Set;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.converter.OAuth2ClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2ClientRegistrationEndpointFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
// GHSA-mh65-6r98-p6q8: without this, every @PreAuthorize on the admin controllers was inert.
@EnableMethodSecurity
// #205: empty authorities = enable factor merging in the authentication filters only (password then
// passkey accumulate FACTOR_* authorities on the same principal) WITHOUT a global MFA requirement.
// The requirement is applied to the admin paths of the session chain only (adminSessionAuthorization).
@EnableMultiFactorAuthentication(authorities = {})
public class AuthorizationServerConfig {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AuthorizationServerConfig.class);

    /**
     * Path prefixes of the administrative REST APIs, taken from the controllers' class-level
     * {@code @RequestMapping}s: {@code OAuthAdminController}, {@code OAuth2ClientManagementController},
     * {@code CertificateManagementController}, {@code DataCustodianIntegrationController}, plus
     * {@code /connect/**}. The hand-written {@code /connect/register} controller was replaced by the
     * RFC 7591 {@code /oauth2/register} endpoint (#196); the rule stays as defense in depth so no
     * future {@code /connect} handler (or an OIDC registration endpoint enabled by mistake) is ever
     * reachable by a customer session.
     */
    static final String[] ADMIN_API_PATHS = {
            "/admin/**",
            "/api/v1/admin/**",
            "/api/v1/oauth2/clients/**",
            "/api/v1/certificates/**",
            "/api/v1/datacustodian/**",
            "/connect/**"
    };

    /**
     * Subset of {@link #ADMIN_API_PATHS} that also accepts an {@code authserver.admin} bearer token
     * (issue #204). {@code /connect/**} is deliberately excluded: Dynamic Client Registration has its
     * own bearer model on {@code /oauth2/register} (single-use {@code client.create} initial access
     * token, #196), so {@code /connect/**} remains session-only (admin role) here.
     */
    static final String[] ADMIN_BEARER_API_PATHS = {
            "/admin/**",
            "/api/v1/admin/**",
            "/api/v1/oauth2/clients/**",
            "/api/v1/certificates/**",
            "/api/v1/datacustodian/**"
    };

    /** Roles (without the {@code ROLE_} prefix) allowed to call {@link #ADMIN_API_PATHS}. */
    static final String[] ADMIN_ROLES = {"ADMIN", "DC_ADMIN"};

    /** Client id of the seeded admin-API client (issue #204). */
    static final String ADMIN_CLIENT_ID = "espi_admin";

    /**
     * Secret of the {@value #ADMIN_CLIENT_ID} client. Empty (the default) means the client is not
     * seeded and admin-API bearer access is disabled.
     */
    @Value("${espi.authserver.admin-client.secret:${ESPI_ADMIN_CLIENT_SECRET:}}")
    private String adminClientSecret;

    /** #205 WebAuthn relying party ID: the registrable domain of the AS (no scheme or port). */
    @Value("${espi.authserver.admin.webauthn.rp-id:localhost}")
    private String webAuthnRpId;

    @Value("${espi.authserver.admin.webauthn.rp-name:OpenESPI Authorization Server}")
    private String webAuthnRpName;

    /** Exact origins (scheme://host[:port]) browsers may run the passkey ceremony from. */
    @Value("${espi.authserver.admin.webauthn.allowed-origins:http://localhost:9999}")
    private List<String> webAuthnAllowedOrigins;

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
     * Raw secret of the {@code espi_registrar} client (env {@code ESPI_REGISTRAR_CLIENT_SECRET}). Encoded with
     * the {@link PasswordEncoder} before it is stored. When blank, the registrar is not seeded and no initial
     * access token can be obtained, so dynamic registration is effectively disabled.
     */
    @Value("${espi.authserver.registrar.client-secret:}")
    private String registrarClientSecret;

    /** When true (prod / espi40 profiles), dynamically registered redirect URIs must use https. */
    @Value("${espi.security.require-https-redirect-uris:false}")
    private boolean requireHttpsRedirectUris;

    /** Scope the initial access token must carry (exactly, and only) to call the registration endpoint. */
    static final String CLIENT_CREATE_SCOPE = "client.create";

    /** Seeded client that obtains {@code client.create} initial access tokens via client_credentials. */
    static final String REGISTRAR_CLIENT_ID = "espi_registrar";

    /** RFC 7591 client registration endpoint path. */
    static final String CLIENT_REGISTRATION_ENDPOINT = "/oauth2/register";

    /**
     * ESPI Third Party (Function Block) scopes a dynamically registered client may request. Same strings
     * as the seeded {@code third_party} authorization_code client.
     */
    static final List<String> ESPI_THIRD_PARTY_SCOPES = List.of(
            "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
            "FB=4_5_15;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13");

    private static final String RFC7591_ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc7591#section-3.2.2";

    /**
     * RFC 7591 OAuth 2.0 Dynamic Client Registration — {@code POST /oauth2/register} (#196).
     *
     * <p>Uses Spring Security 7's built-in RFC 7591 classes ({@link OAuth2ClientRegistrationEndpointFilter},
     * {@link OAuth2ClientRegistrationAuthenticationProvider}) but wires them by hand on a dedicated chain
     * rather than via {@code OAuth2AuthorizationServerConfigurer.clientRegistrationEndpoint(...)}. The
     * configurer is NOT usable here: its {@code init(HttpSecurity)} unconditionally calls
     * {@code http.oauth2ResourceServer(rs -> rs.jwt(withDefaults()))} whenever the client-registration
     * configurer is present, which needs a {@code JwtDecoder} and violates the opaque-only constraint (#134).</p>
     *
     * <p>The initial access token is an ordinary opaque client_credentials token with scope
     * {@code client.create}, minted for the seeded {@code espi_registrar} client. It is authenticated by
     * {@code oauth2ResourceServer().opaqueToken()} backed by {@link AuthorizationServiceOpaqueTokenIntrospector}
     * (in-process lookup in {@link OAuth2AuthorizationService}, no HTTP, no JWT). The provider then re-resolves
     * it with {@code OAuth2AuthorizationService.findByToken(value, ACCESS_TOKEN)}, requires its scope set to be
     * exactly {@code {client.create}}, and invalidates it after one successful registration.</p>
     *
     * <p>Limitation: Spring's RFC 7591 implementation issues NO {@code registration_access_token} and has no
     * RFC 7592 client-configuration (GET/PUT/DELETE) endpoint; both exist only in the OIDC variant.</p>
     */
    @Bean
    @Order(0)
    public SecurityFilterChain clientRegistrationSecurityFilterChain(
            HttpSecurity http,
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService,
            PasswordEncoder passwordEncoder) throws Exception {

        OAuth2ClientRegistrationAuthenticationProvider registrationProvider =
                new OAuth2ClientRegistrationAuthenticationProvider(registeredClientRepository, authorizationService);
        registrationProvider.setPasswordEncoder(passwordEncoder); // stored as {bcrypt}..., returned once in clear
        registrationProvider.setRegisteredClientConverter(espiRegisteredClientConverter(requireHttpsRedirectUris));

        OAuth2ClientRegistrationEndpointFilter registrationFilter =
                new OAuth2ClientRegistrationEndpointFilter(new ProviderManager(registrationProvider));

        http
                .securityMatcher(CLIENT_REGISTRATION_ENDPOINT)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable()) // bearer-token API, no browser session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(rs -> rs.opaqueToken(opaque -> opaque
                        .introspector(new AuthorizationServiceOpaqueTokenIntrospector(authorizationService))))
                .addFilterAfter(registrationFilter, AuthorizationFilter.class);
        return http.build();
    }

    /**
     * Wraps Spring's default RFC 7591 metadata→RegisteredClient converter to apply ESPI policy. A registrant
     * may only create a confidential ESPI Third Party authorization_code client:
     * <ul>
     *   <li>{@code scope}: one or more of {@link #ESPI_THIRD_PARTY_SCOPES}, nothing else;</li>
     *   <li>{@code grant_types}: {@code authorization_code} (required) and optionally {@code refresh_token};</li>
     *   <li>{@code response_types}: {@code code} only;</li>
     *   <li>{@code token_endpoint_auth_method}: {@code client_secret_basic} (the RFC 7591 default when omitted);</li>
     *   <li>{@code redirect_uris}: at least one; https only when {@code requireHttpsRedirectUris}.</li>
     * </ul>
     * Violations are rejected with {@code invalid_client_metadata}. The resulting client gets opaque
     * (REFERENCE) access tokens and no PKCE requirement.
     */
    static Converter<OAuth2ClientRegistration, RegisteredClient> espiRegisteredClientConverter(
            boolean requireHttpsRedirectUris) {
        OAuth2ClientRegistrationRegisteredClientConverter delegate = new OAuth2ClientRegistrationRegisteredClientConverter();
        return clientRegistration -> {
            validateEspiRegistration(clientRegistration, requireHttpsRedirectUris);
            RegisteredClient client = delegate.convert(clientRegistration);
            return RegisteredClient.from(client)
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                            .accessTokenTimeToLive(Duration.ofMinutes(360))
                            .build())
                    .clientSettings(ClientSettings.withSettings(client.getClientSettings().getSettings())
                            .requireProofKey(false) // ESPI does not use PKCE (see #148)
                            .build())
                    .build();
        };
    }

    private static void validateEspiRegistration(OAuth2ClientRegistration registration,
                                                 boolean requireHttpsRedirectUris) {
        List<String> scopes = registration.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            throw invalidClientMetadata("scope is required (ESPI Third Party scopes only)");
        }
        for (String scope : scopes) {
            if (!ESPI_THIRD_PARTY_SCOPES.contains(scope)) {
                throw invalidClientMetadata("scope not allowed for dynamic registration: " + scope);
            }
        }

        List<String> grantTypes = registration.getGrantTypes();
        if (grantTypes != null && !grantTypes.isEmpty()) {
            boolean allowed = grantTypes.stream().allMatch(grant ->
                    AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(grant)
                            || AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grant));
            if (!allowed || !grantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())) {
                throw invalidClientMetadata("grant_types must be authorization_code, optionally with refresh_token");
            }
        }

        List<String> responseTypes = registration.getResponseTypes();
        if (responseTypes != null && !responseTypes.isEmpty() && !responseTypes.equals(List.of("code"))) {
            throw invalidClientMetadata("response_types must be [\"code\"]");
        }

        String authMethod = registration.getTokenEndpointAuthenticationMethod();
        if (authMethod != null && !ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(authMethod)) {
            throw invalidClientMetadata("token_endpoint_auth_method must be client_secret_basic");
        }

        if (registration.getRedirectUris() == null || registration.getRedirectUris().isEmpty()) {
            throw invalidClientMetadata("redirect_uris is required for an authorization_code client");
        }
        if (requireHttpsRedirectUris) {
            for (String redirectUri : registration.getRedirectUris()) {
                if (!redirectUri.regionMatches(true, 0, "https://", 0, "https://".length())) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_redirect_uri",
                            "redirect_uris must use https: " + redirectUri, RFC7591_ERROR_URI));
                }
            }
        }
    }

    private static OAuth2AuthenticationException invalidClientMetadata(String description) {
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_client_metadata", description, RFC7591_ERROR_URI));
    }

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
        // /oauth2/introspect, /oauth2/revoke, /.well-known/oauth-authorization-server,
        // etc.). /oauth2/register is claimed by clientRegistrationSecurityFilterChain
        // @Order(0); everything else falls through to adminApiBearerSecurityFilterChain
        // @Order(2) (bearer admin calls) or defaultSecurityFilterChain @Order(3).
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

        // Advertise the RFC 7591 endpoint (wired on its own chain, see clientRegistrationSecurityFilterChain)
        // in /.well-known/oauth-authorization-server. The stock clientRegistrationEndpoint(...) configurer
        // would do this too, but it also forces a JWT resource server, so it is not used.
        authorizationServerConfigurer.authorizationServerMetadataEndpoint(metadata -> metadata
                .authorizationServerMetadataCustomizer(builder -> builder.clientRegistrationEndpoint(
                        org.springframework.web.util.UriComponentsBuilder
                                .fromUriString(org.springframework.security.oauth2.server.authorization.context
                                        .AuthorizationServerContextHolder.getContext().getIssuer())
                                .path(CLIENT_REGISTRATION_ENDPOINT)
                                .build().toUriString())));

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
        // admin APIs accept a bearer token on the @Order(2) chain, and admin/UI endpoints are
        // otherwise protected by the @Order(3) session-login chain.

        return http.build();
    }

    /**
     * Stateless admin-API chain for machine clients (issue #204).
     *
     * <p>Claims a request only when it targets {@link #ADMIN_BEARER_API_PATHS} AND carries an
     * {@code Authorization: Bearer} header; anything else falls through to the session chain, which
     * keeps its own ADMIN/DC_ADMIN rule. The opaque token is resolved in-process against
     * {@link OAuth2AuthorizationService} (no HTTP self-call, no JWT/JWK). Unknown, revoked or expired
     * tokens yield 401 with {@code WWW-Authenticate: Bearer}; a valid token without the
     * {@code authserver.admin} scope yields 403 {@code insufficient_scope}.</p>
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminApiBearerSecurityFilterChain(
            HttpSecurity http, OAuth2AuthorizationService authorizationService) throws Exception {
        OpaqueTokenIntrospector introspector = new AdminScopeOpaqueTokenIntrospector(
                new AuthorizationServiceOpaqueTokenIntrospector(authorizationService));

        List<RequestMatcher> adminPaths = new ArrayList<>();
        for (String path : ADMIN_BEARER_API_PATHS) {
            adminPaths.add(PathPatternRequestMatcher.withDefaults().matcher(path));
        }
        RequestMatcher bearerHeader = request -> {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
        };

        http
                .securityMatcher(new AndRequestMatcher(new OrRequestMatcher(adminPaths), bearerHeader))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("ADMIN"))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .opaqueToken(opaque -> opaque.introspector(introspector)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));

        return http.build();
    }

    /**
     * Session-chain authorization for {@link #ADMIN_API_PATHS} (#205): a human admin session must hold
     * {@code ROLE_ADMIN} or {@code ROLE_DC_ADMIN} AND both the {@code FACTOR_PASSWORD} and
     * {@code FACTOR_WEBAUTHN} authorities (Spring Security 7 {@link AllRequiredFactorsAuthorizationManager}).
     *
     * <p>The role is evaluated first ({@link AuthorizationManagers#allOf} stops at the first denial).
     * A session without an admin role, such as a delegated ROLE_CUSTOMER session, therefore gets a
     * plain 403 and is never sent through the passkey step. An admin missing a factor gets a
     * {@code FactorAuthorizationDecision}. Spring's {@code DelegatingMissingAuthorityAccessDeniedHandler}
     * turns that into a redirect to the login page for the missing factor.</p>
     *
     * <p>MFA is deliberately scoped to this rule only. {@code @EnableMultiFactorAuthentication} is
     * declared with an empty {@code authorities} list, so it only turns on factor merging in the
     * authentication filters and installs no global requirement. The protocol chain, the
     * delegated-customer endpoints, the bearer admin chain and introspection are unaffected.</p>
     */
    static AuthorizationManager<RequestAuthorizationContext> adminSessionAuthorization() {
        AuthorizationManager<RequestAuthorizationContext> adminRole =
                AuthorityAuthorizationManager.hasAnyRole(ADMIN_ROLES);
        AuthorizationManager<RequestAuthorizationContext> passwordAndPasskey =
                AllRequiredFactorsAuthorizationManager.<RequestAuthorizationContext>builder()
                        .requireFactor(factor -> factor.passwordAuthority())
                        .requireFactor(factor -> factor.webauthnAuthority())
                        .build();
        return AuthorizationManagers.allOf(adminRole, passwordAndPasskey);
    }

    /**
     * Default Security Filter Chain for everything NOT claimed by the
     * authorization-server endpoints matcher: login form, static resources,
     * H2 console (in dev), and any custom controllers.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            UserDetailsService adminUserDetailsService,
            AdminLoginSuccessHandler adminLoginSuccessHandler) throws Exception {
        http
                // #205: form login (password factor) and WebAuthn (passkey factor) authenticate
                // administrators from admin_users ONLY. Retail customers never log in here; their
                // ROLE_CUSTOMER session is written by AuthorizeContinueController from the DC handoff.
                .userDetailsService(adminUserDetailsService)
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
                        // GHSA-mh65-6r98-p6q8: the admin APIs must never be reachable by an
                        // arbitrary authenticated session (e.g. the ROLE_CUSTOMER session written
                        // by AuthorizeContinueController). URL-level rule mirrors the controllers'
                        // class-level @PreAuthorize("hasRole('ADMIN') or hasRole('DC_ADMIN')"),
                        // which is now also enforced via @EnableMethodSecurity (defense in depth).
                        // #205: on THIS (session) chain the admin paths additionally require both
                        // the password and the passkey factor; see adminSessionAuthorization().
                        .requestMatchers(ADMIN_API_PATHS).access(adminSessionAuthorization())
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.successHandler(adminLoginSuccessHandler))
                .webAuthn(webAuthn -> webAuthn
                        .rpId(webAuthnRpId)
                        .rpName(webAuthnRpName)
                        .allowedOrigins(Set.copyOf(webAuthnAllowedOrigins)))
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
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate,
                                                                 PasswordEncoder passwordEncoder) {
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
                .clientName("DataCustodian Admin")
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
                .clientName("ThirdParty Application")
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
                .scopes(scopes -> scopes.addAll(ESPI_THIRD_PARTY_SCOPES))
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofMinutes(360))
                    .refreshTokenTimeToLive(Duration.ofMinutes(3600))
                    .reuseRefreshTokens(true)
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                    .build())
                .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(true)
                    // ESPI does not support PKCE. Spring Authorization Server 7.x requires
                    // code_challenge for the authorization_code grant unless requireProofKey is
                    // explicitly false, so the ESPI customer flow (which sends no code_challenge)
                    // fails with "OAuth 2.0 Parameter: code_challenge" without this. Surfaced by the
                    // Phase 2.0 end-to-end orchestration test (#148).
                    .requireProofKey(false)
                    .build())
                .build();

        // ThirdParty Admin Client (ROLE_TP_ADMIN)
        RegisteredClient thirdPartyAdmin = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("third_party_admin")
                .clientName("ThirdParty Admin")
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

        // DataCustodian resource-server introspection client. The DC resource server calls
        // POST /oauth2/introspect with these credentials (client_secret_basic) to validate opaque
        // tokens; any registered confidential client may introspect. Matches DC's default
        // espi.authorization-server.client-id / client-secret (datacustodian / datacustodian-secret),
        // so the local sandbox introspects out of the box.
        RegisteredClient dataCustodianIntrospection = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("datacustodian")
                .clientName("DataCustodian Introspection")
                .clientSecret("{noop}datacustodian-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientIdIssuedAt(Instant.now())
                .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .build())
                .build();

        // Seed default clients, and reconcile existing ones to the code definition (see below).
        initializeDefaultClients(repository, datacustodianAdmin, thirdPartyClient, thirdPartyAdmin,
                dataCustodianIntrospection);

        seedRegistrarClient(repository, passwordEncoder);

        seedAdminClient(repository, passwordEncoder);

        return repository;
    }

    /**
     * Seed (or reconcile) the {@value #ADMIN_CLIENT_ID} client used for bearer access to the admin
     * APIs (issue #204): {@code client_credentials} only, scope {@code authserver.admin}, opaque
     * tokens with a 15-minute TTL, and a secret taken from configuration and stored hashed.
     *
     * <p>Without a configured secret the client is not seeded (secure default). An existing row is
     * only rewritten when its stored hash no longer matches the configured secret (rotation) or one
     * of the managed fields drifted, so a steady-state restart does not re-hash the secret.</p>
     */
    public void seedAdminClient(RegisteredClientRepository repository, PasswordEncoder passwordEncoder) {
        if (adminClientSecret == null || adminClientSecret.isBlank()) {
            log.info("Admin API bearer access is disabled: espi.authserver.admin-client.secret "
                    + "(ESPI_ADMIN_CLIENT_SECRET) is not set, so the '{}' client is not seeded.",
                    ADMIN_CLIENT_ID);
            return;
        }
        RegisteredClient existing = repository.findByClientId(ADMIN_CLIENT_ID);
        if (existing != null && !adminClientDrifted(existing, passwordEncoder)) {
            return;
        }
        RegisteredClient.Builder desired = RegisteredClient
                .withId(existing != null ? existing.getId() : UUID.randomUUID().toString())
                .clientId(ADMIN_CLIENT_ID)
                .clientName("Authorization Server Admin API")
                .clientSecret(passwordEncoder.encode(adminClientSecret))
                .clientIdIssuedAt(existing != null && existing.getClientIdIssuedAt() != null
                        ? existing.getClientIdIssuedAt() : Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(AdminScopeOpaqueTokenIntrospector.ADMIN_SCOPE)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(ADMIN_TOKEN_TTL)
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build());
        repository.save(desired.build());
        log.info("Admin API client '{}' {}.", ADMIN_CLIENT_ID, existing == null ? "seeded" : "reconciled");
    }

    /** Access-token lifetime of the {@value #ADMIN_CLIENT_ID} client. */
    static final Duration ADMIN_TOKEN_TTL = Duration.ofMinutes(15);

    /**
     * Whether the stored {@value #ADMIN_CLIENT_ID} row differs from its code definition. The secret is
     * compared with {@link PasswordEncoder#matches}, never by hash equality (each encode salts anew).
     */
    static boolean adminClientDrifted(RegisteredClient existing, PasswordEncoder passwordEncoder,
                                      String configuredSecret) {
        boolean secretDrift = existing.getClientSecret() == null
                || !passwordEncoder.matches(configuredSecret, existing.getClientSecret());
        boolean grantDrift = !existing.getAuthorizationGrantTypes()
                .equals(java.util.Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS));
        boolean scopeDrift = !existing.getScopes()
                .equals(java.util.Set.of(AdminScopeOpaqueTokenIntrospector.ADMIN_SCOPE));
        boolean tokenDrift = !OAuth2TokenFormat.REFERENCE.equals(existing.getTokenSettings().getAccessTokenFormat())
                || !ADMIN_TOKEN_TTL.equals(existing.getTokenSettings().getAccessTokenTimeToLive());
        return secretDrift || grantDrift || scopeDrift || tokenDrift;
    }

    private boolean adminClientDrifted(RegisteredClient existing, PasswordEncoder passwordEncoder) {
        return adminClientDrifted(existing, passwordEncoder, adminClientSecret);
    }

    /**
     * Seed (or rotate) the RFC 7591 registrar client, {@code espi_registrar} (#196).
     *
     * <p>The registrar obtains the initial access token for {@code POST /oauth2/register} via
     * client_credentials with {@code scope=client.create}. That token is opaque, single-use (the
     * registration provider invalidates it) and must carry ONLY {@code client.create}.</p>
     *
     * <p>Unlike the {@code {noop}} development defaults above, the secret comes from configuration
     * ({@code espi.authserver.registrar.client-secret}, env {@code ESPI_REGISTRAR_CLIENT_SECRET}) and is
     * stored {@link PasswordEncoder}-encoded ({@code {bcrypt}...}). It is handled outside
     * {@link #initializeDefaultClients} because a salted hash differs on every startup, so the
     * field-equality drift check used there cannot apply: instead the stored hash is compared with
     * {@link PasswordEncoder#matches}, and re-encoded only when the configured secret changed (rotation).
     * When no secret is configured the registrar is not seeded, which leaves dynamic registration disabled.</p>
     */
    private void seedRegistrarClient(RegisteredClientRepository repository, PasswordEncoder passwordEncoder) {
        if (registrarClientSecret == null || registrarClientSecret.isBlank()) {
            System.out.println("ESPI registrar client not seeded: espi.authserver.registrar.client-secret "
                    + "(ESPI_REGISTRAR_CLIENT_SECRET) is not set; dynamic client registration is disabled");
            return;
        }
        RegisteredClient existing = repository.findByClientId(REGISTRAR_CLIENT_ID);
        if (existing == null) {
            repository.save(RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(REGISTRAR_CLIENT_ID)
                    .clientName("ESPI Client Registrar")
                    .clientSecret(passwordEncoder.encode(registrarClientSecret))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scope(CLIENT_CREATE_SCOPE)
                    .clientIdIssuedAt(Instant.now())
                    .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(10))
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE) // ESPI standard: opaque tokens
                        .build())
                    .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                    .build());
            System.out.println("ESPI registrar client seeded");
        } else if (!passwordEncoder.matches(registrarClientSecret, existing.getClientSecret())) {
            repository.save(RegisteredClient.from(existing)
                    .clientSecret(passwordEncoder.encode(registrarClientSecret))
                    .build());
            System.out.println("ESPI registrar client secret rotated");
        }
    }

    /**
     * Seed the default ESPI clients, and <em>reconcile</em> any that already exist so that
     * changes to a default client's code definition reach an already-seeded database.
     *
     * <p>The previous implementation only inserted when absent, so a default client created by an
     * older revision kept its stale row forever — which silently shipped two defects to existing
     * deployments: the missing {@code requireProofKey(false)} (#148, breaks the ESPI customer flow
     * with a spurious PKCE {@code code_challenge} requirement) and the placeholder UUID
     * {@code clientName}s (#149). These three clients are system-managed defaults — they are
     * authoritative in code, not via the admin API — so reconciling them on startup is safe.</p>
     *
     * <p>Reconciliation preserves the existing primary-key {@code id} (the FK target of
     * {@code oauth2_authorization}) and only writes when {@code clientName} or {@code requireProofKey}
     * has drifted, so admin-tuned fields on other clients are never touched and steady-state startups
     * issue no writes. Fix for existing deployments tracked by #154.</p>
     */
    private void initializeDefaultClients(RegisteredClientRepository repository,
                                          RegisteredClient... clients) {
        int seeded = 0;
        int reconciled = 0;
        for (RegisteredClient client : clients) {
            RegisteredClient existing = repository.findByClientId(client.getClientId());
            if (existing == null) {
                repository.save(client);
                seeded++;
            } else {
                RegisteredClient drifted = reconcile(existing, client);
                if (drifted != null) {
                    repository.save(drifted);
                    reconciled++;
                }
            }
        }
        System.out.println("Default ESPI Clients: seeded " + seeded + ", reconciled " + reconciled
                + " of " + clients.length);
    }

    /**
     * Return the client to persist when an existing default client has drifted from its code
     * definition, or {@code null} when it is already in sync.
     *
     * <p>Drift is detected on the two fields the default definitions actually manage —
     * {@code clientName} and {@code requireProofKey}. When either differs, the full {@code desired}
     * definition is adopted (re-pointed at {@code existing}'s primary-key id so the update lands on
     * the same row rather than colliding on the unique {@code client_id}).</p>
     */
    static RegisteredClient reconcile(RegisteredClient existing, RegisteredClient desired) {
        boolean nameDrift = !java.util.Objects.equals(existing.getClientName(), desired.getClientName());
        boolean proofKeyDrift = existing.getClientSettings().isRequireProofKey()
                != desired.getClientSettings().isRequireProofKey();
        if (!nameDrift && !proofKeyDrift) {
            return null;
        }
        return RegisteredClient.from(desired).id(existing.getId()).build();
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