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

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-process {@link OpaqueTokenIntrospector} for the Authorization Server's own bearer-protected
 * endpoints (issue #196).
 *
 * <p>Resolves an opaque (REFERENCE) access token by looking it up in the server's own
 * {@link OAuth2AuthorizationService} — the same store {@code /oauth2/introspect} reads — instead of
 * making an HTTP call to {@code /oauth2/introspect} or decoding a JWT. No {@code JwtDecoder} or
 * {@code JWKSource} is involved, keeping the opaque-only constraint from #134.</p>
 *
 * <p>Attributes are built from the stored {@link OAuth2AccessToken} itself (not the persisted claims
 * map) so {@code iat}/{@code exp} are always {@link java.time.Instant}s, which
 * {@code OpaqueTokenAuthenticationProvider} requires. Scopes become {@code SCOPE_*} authorities.</p>
 */
public final class AuthorizationServiceOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final OAuth2AuthorizationService authorizationService;

    public AuthorizationServiceOpaqueTokenIntrospector(OAuth2AuthorizationService authorizationService) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        this.authorizationService = authorizationService;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        OAuth2Authorization authorization =
                authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            throw new BadOpaqueTokenException("Unknown access token");
        }
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || !accessToken.isActive()) {
            throw new BadOpaqueTokenException("Access token is not active");
        }

        OAuth2AccessToken value = accessToken.getToken();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, true);
        attributes.put(OAuth2TokenIntrospectionClaimNames.SUB, authorization.getPrincipalName());
        attributes.put(OAuth2TokenIntrospectionClaimNames.CLIENT_ID, authorization.getRegisteredClientId());
        attributes.put(OAuth2TokenIntrospectionClaimNames.SCOPE, new ArrayList<>(value.getScopes()));
        attributes.put(OAuth2TokenIntrospectionClaimNames.TOKEN_TYPE, value.getTokenType().getValue());
        if (value.getIssuedAt() != null) {
            attributes.put(OAuth2TokenIntrospectionClaimNames.IAT, value.getIssuedAt());
        }
        if (value.getExpiresAt() != null) {
            attributes.put(OAuth2TokenIntrospectionClaimNames.EXP, value.getExpiresAt());
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        value.getScopes().forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));

        return new OAuth2IntrospectionAuthenticatedPrincipal(
                authorization.getPrincipalName(), attributes, authorities);
    }
}
