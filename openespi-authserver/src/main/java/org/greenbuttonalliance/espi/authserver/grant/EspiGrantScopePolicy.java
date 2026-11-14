/*
 *
 *        Copyright (c) 2026 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.authserver.grant;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationValidator;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Which grant may yield which scopes (#251). Spring Authorization Server only checks that a requested
 * scope is registered for the client, whatever the grant, so a client registered for both
 * {@code client_credentials} and a customer Function Block scope could otherwise mint a customer-data
 * token with no customer consent. ESPI separates the token kinds:
 * <ul>
 *   <li><b>client_credentials</b> (client access token, registration-token renewal, infrastructure
 *       clients) may never carry a customer data scope: any {@code FB=...} scope other than the
 *       {@value #REGISTRATION_RENEWAL_SCOPE} renewal scope is {@code invalid_scope}.</li>
 *   <li><b>authorization_code</b> (customer consent) may never carry an administrative scope
 *       ({@link #ADMINISTRATIVE_SCOPES}).</li>
 * </ul>
 * Refresh tokens can only narrow the scopes of the grant they came from, so they need no rule here.
 */
public final class EspiGrantScopePolicy {

    /** Scope that renews an expired registration access token (ESPI {@code FB=36_40}). */
    public static final String REGISTRATION_RENEWAL_SCOPE = "FB=36_40";

    /** Scopes only a client may hold for itself, never granted through a customer's consent. */
    public static final Set<String> ADMINISTRATIVE_SCOPES = Set.of(
            "ThirdParty_Admin_Access",
            "DataCustodian_Admin_Access",
            REGISTRATION_RENEWAL_SCOPE,
            "authserver.admin",
            "client.create");

    private EspiGrantScopePolicy() {
    }

    /** True for an ESPI customer data scope: a Function Block scope that is not the renewal scope. */
    public static boolean isCustomerDataScope(String scope) {
        return scope != null && scope.startsWith("FB=") && !REGISTRATION_RENEWAL_SCOPE.equals(scope);
    }

    /** Spring's default client_credentials scope check, followed by the ESPI rule. */
    public static Consumer<OAuth2ClientCredentialsAuthenticationContext> clientCredentialsValidator() {
        return OAuth2ClientCredentialsAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR
                .andThen(EspiGrantScopePolicy::rejectCustomerDataScopes);
    }

    /** Spring's default authorization request checks, followed by the ESPI rule. */
    public static Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> authorizationCodeValidator() {
        return new OAuth2AuthorizationCodeRequestAuthenticationValidator()
                .andThen(EspiGrantScopePolicy::rejectAdministrativeScopes);
    }

    static void rejectCustomerDataScopes(OAuth2ClientCredentialsAuthenticationContext context) {
        OAuth2ClientCredentialsAuthenticationToken request = context.getAuthentication();
        if (request.getScopes().stream().anyMatch(EspiGrantScopePolicy::isCustomerDataScope)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE,
                    "client_credentials cannot grant an ESPI customer data (Function Block) scope", null));
        }
    }

    static void rejectAdministrativeScopes(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
        OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
        if (request.getScopes().stream().noneMatch(ADMINISTRATIVE_SCOPES::contains)) {
            return;
        }
        // Same shape as Spring's own invalid_scope error: redirected back to the client's redirect URI.
        String redirectUri = StringUtils.hasText(request.getRedirectUri())
                ? request.getRedirectUri()
                : context.getRegisteredClient().getRedirectUris().iterator().next();
        OAuth2AuthorizationCodeRequestAuthenticationToken result = new OAuth2AuthorizationCodeRequestAuthenticationToken(
                request.getAuthorizationUri(), request.getClientId(), (Authentication) request.getPrincipal(),
                redirectUri, request.getState(), request.getScopes(), request.getAdditionalParameters());
        result.setAuthenticated(true);
        throw new OAuth2AuthorizationCodeRequestAuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE,
                "OAuth 2.0 Parameter: scope (administrative scopes are not granted through customer authorization)",
                null), result);
    }
}
