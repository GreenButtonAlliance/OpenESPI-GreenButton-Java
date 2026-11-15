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

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Which grant may yield which scopes (#251). Spring Authorization Server only checks that a requested
 * scope is registered for the client, whatever the grant, so a client registered for both
 * {@code client_credentials} and a customer Function Block scope could otherwise mint a customer-data
 * token with no customer consent. ESPI keeps the token kinds apart, and both rules are allowlists that
 * fail closed:
 * <ul>
 *   <li><b>client_credentials</b> (client access token, registration-token renewal, infrastructure
 *       clients) may yield only the exact {@link #ADMINISTRATIVE_SCOPES} strings.</li>
 *   <li><b>authorization_code</b> (customer consent) may yield only {@code openid} / {@code profile}
 *       and ESPI customer data scopes: scopes that, parsed the way the Data Custodian parses them
 *       ({@code EspiScope}: {@code ;}-separated, trimmed, case-insensitive {@code FB} key, {@code _}-separated
 *       integers), name at least one Function Block and only {@link #CUSTOMER_DATA_FUNCTION_BLOCKS}.</li>
 * </ul>
 * In ESPI practice {@code FB=34_35} and {@code FB=36_40} are unique Function Block combinations that mean
 * "client access token" and "registration access token"; every other {@code FB=} scope is a customer scope.
 * Neither combination can pass the customer rule, because FB 34 and 36 are not customer data Function Blocks.
 * Refresh tokens can only narrow the scopes of the grant they came from, so they need no rule here.
 */
public final class EspiGrantScopePolicy {

    /**
     * The Third Party's client access token ({@code client_credentials}): the only token the Data Custodian's
     * ESPI Authorization and Bulk APIs accept.
     */
    public static final String CLIENT_ACCESS_TOKEN_SCOPE = "FB=34_35";

    /** Scope that renews an expired registration access token (ESPI {@code FB=36_40}). */
    public static final String REGISTRATION_RENEWAL_SCOPE = "FB=36_40";

    /** The only scopes {@code client_credentials} may yield. Never granted through a customer's consent. */
    public static final Set<String> ADMINISTRATIVE_SCOPES = Set.of(
            CLIENT_ACCESS_TOKEN_SCOPE,
            REGISTRATION_RENEWAL_SCOPE,
            "ThirdParty_Admin_Access",
            "DataCustodian_Admin_Access",
            "authserver.admin",
            "client.create");

    /**
     * The customer data Function Blocks a Data Custodian can currently be certified for, per the GBA Green Button
     * Data Custodian Connect My Data (CMD) Certification Application v2.14: Energy Usage FB 1-13, 15-17, 26-29,
     * 31, 35, 37, 39, 40 and Retail Customer FB 51, 53-62, 64, 65, 67, 69-71 (FB 68 is reserved for future use).
     * FB 34 (SFTP bulk) and FB 36 (registration) are absent, so a customer can never grant {@code FB=34_35} or
     * {@code FB=36_40}, whatever the spelling or extra parameters.
     */
    public static final Set<Integer> CUSTOMER_DATA_FUNCTION_BLOCKS = Set.of(
            1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 26, 27, 28, 29, 31, 35, 37, 39, 40,
            51, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 64, 65, 67, 69, 70, 71);

    /** Non-ESPI scopes a customer authorization request may carry. */
    private static final Set<String> CUSTOMER_IDENTITY_SCOPES = Set.of("openid", "profile");

    private EspiGrantScopePolicy() {
    }

    /** True when {@code client_credentials} may yield the scope. */
    public static boolean isClientCredentialsScope(String scope) {
        return ADMINISTRATIVE_SCOPES.contains(scope);
    }

    /** True when customer authorization ({@code authorization_code}) may yield the scope. */
    public static boolean isAuthorizationCodeScope(String scope) {
        if (CUSTOMER_IDENTITY_SCOPES.contains(scope)) {
            return true;
        }
        return functionBlocks(scope)
                .filter(fbs -> !fbs.isEmpty() && CUSTOMER_DATA_FUNCTION_BLOCKS.containsAll(fbs))
                .isPresent();
    }

    /**
     * The Function Blocks the Data Custodian would read from {@code scope} (mirrors {@code EspiScope.parse}),
     * or empty when the scope cannot be parsed, which callers treat as not allowed.
     */
    public static Optional<Set<Integer>> functionBlocks(String scope) {
        if (scope == null || scope.isBlank()) {
            return Optional.empty();
        }
        Set<Integer> fbs = new TreeSet<>();
        for (String segment : scope.split(";")) {
            String term = segment.trim();
            int eq = term.indexOf('=');
            if (eq < 0 || !term.substring(0, eq).trim().toLowerCase(Locale.ROOT).equals("fb")) {
                continue;
            }
            for (String token : term.substring(eq + 1).split("_")) {
                String fb = token.trim();
                if (fb.isEmpty()) {
                    continue;
                }
                try {
                    fbs.add(Integer.parseInt(fb));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(fbs);
    }

    /** Spring's default client_credentials scope check, followed by the ESPI rule. */
    public static Consumer<OAuth2ClientCredentialsAuthenticationContext> clientCredentialsValidator() {
        return OAuth2ClientCredentialsAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR
                .andThen(EspiGrantScopePolicy::requireClientCredentialsScopes);
    }

    /**
     * True when {@code candidate} is an ESPI narrowing of {@code original} (#211): it names a non-empty subset of
     * the original's Function Blocks and carries exactly the original's other parameters ({@code IntervalDuration},
     * {@code BlockDuration}, {@code HistoryLength}, ...; keys compared case-insensitively, order ignored). The
     * Third Party may narrow its registered scope and the customer may narrow the requested scope, but neither may
     * widen it, so a candidate naming any Function Block the original does not is never a narrowing. Unparseable
     * input is never a narrowing.
     */
    public static boolean isNarrowingOf(String candidate, String original) {
        Optional<Set<Integer>> candidateFbs = functionBlocks(candidate);
        Optional<Set<Integer>> originalFbs = functionBlocks(original);
        return candidateFbs.isPresent() && originalFbs.isPresent()
                && !candidateFbs.get().isEmpty()
                && originalFbs.get().containsAll(candidateFbs.get())
                && otherParameters(candidate).equals(otherParameters(original));
    }

    /** The non-{@code FB} terms of a scope, normalized as {@code lowercased-key=trimmed-value}. */
    private static Set<String> otherParameters(String scope) {
        Set<String> terms = new TreeSet<>();
        for (String segment : scope.split(";")) {
            String term = segment.trim();
            if (term.isEmpty()) {
                continue;
            }
            int eq = term.indexOf('=');
            String key = (eq < 0 ? term : term.substring(0, eq)).trim().toLowerCase(Locale.ROOT);
            if (!key.equals("fb")) {
                terms.add(eq < 0 ? key : key + "=" + term.substring(eq + 1).trim());
            }
        }
        return terms;
    }

    /**
     * Spring's default redirect URI check, the ESPI scope check, then the ESPI per-grant rule. The scope check
     * replaces Spring's exact-match one (#211): every requested scope must be registered for the client or be an
     * {@linkplain #isNarrowingOf narrowing} of a registered scope. The registered scope may be narrowed twice and
     * widened never: by the Third Party when it starts the authorization code flow, and again by the customer on
     * the Data Custodian's Authorization Screen ({@code AuthorizeContinueController} rejects an approved scope that
     * is not a narrowing of the request, then replays the request with it). The code, the access token and the
     * Authorization therefore all carry the narrowed scope string.
     */
    public static Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> authorizationCodeValidator() {
        return OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR
                .andThen(EspiGrantScopePolicy::requireRegisteredOrNarrowing)
                .andThen(EspiGrantScopePolicy::requireAuthorizationCodeScopes);
    }

    static void requireRegisteredOrNarrowing(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
        Set<String> registered = context.getRegisteredClient().getScopes();
        OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
        boolean allowed = request.getScopes().stream().allMatch(scope ->
                registered.contains(scope) || registered.stream().anyMatch(r -> isNarrowingOf(scope, r)));
        if (!allowed) {
            throw invalidScope(context, "OAuth 2.0 Parameter: scope");
        }
    }

    static void requireClientCredentialsScopes(OAuth2ClientCredentialsAuthenticationContext context) {
        OAuth2ClientCredentialsAuthenticationToken request = context.getAuthentication();
        if (!request.getScopes().stream().allMatch(EspiGrantScopePolicy::isClientCredentialsScope)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE,
                    "client_credentials grants only administrative scopes, never ESPI customer data scopes", null));
        }
    }

    static void requireAuthorizationCodeScopes(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
        OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
        if (request.getScopes().stream().allMatch(EspiGrantScopePolicy::isAuthorizationCodeScope)) {
            return;
        }
        throw invalidScope(context,
                "OAuth 2.0 Parameter: scope (customer authorization grants only ESPI customer data scopes)");
    }

    /** Same shape as Spring's own invalid_scope error: redirected back to the client's redirect URI. */
    private static OAuth2AuthorizationCodeRequestAuthenticationException invalidScope(
            OAuth2AuthorizationCodeRequestAuthenticationContext context, String description) {
        OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
        String redirectUri = StringUtils.hasText(request.getRedirectUri())
                ? request.getRedirectUri()
                : context.getRegisteredClient().getRedirectUris().iterator().next();
        OAuth2AuthorizationCodeRequestAuthenticationToken result = new OAuth2AuthorizationCodeRequestAuthenticationToken(
                request.getAuthorizationUri(), request.getClientId(), (Authentication) request.getPrincipal(),
                redirectUri, request.getState(), request.getScopes(), request.getAdditionalParameters());
        result.setAuthenticated(true);
        return new OAuth2AuthorizationCodeRequestAuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE, description, null), result);
    }
}
