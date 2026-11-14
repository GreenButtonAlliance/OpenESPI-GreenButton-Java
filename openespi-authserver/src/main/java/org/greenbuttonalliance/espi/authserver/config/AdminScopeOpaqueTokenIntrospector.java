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

package org.greenbuttonalliance.espi.authserver.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorates an {@link OpaqueTokenIntrospector} for the admin-API bearer chain (issue #204): a token
 * carrying the dedicated {@value #ADMIN_SCOPE} scope additionally receives {@value #ADMIN_ROLE}.
 *
 * <p>Only {@value #ADMIN_SCOPE} is mapped. ESPI scopes such as {@code DataCustodian_Admin_Access} or
 * {@code ThirdParty_Admin_Access} are deliberately NOT treated as Authorization Server
 * administration rights. The delegate's {@code SCOPE_*} authorities are kept unchanged.</p>
 *
 * <p>Kept separate from {@link AuthorizationServiceOpaqueTokenIntrospector} so that class stays
 * identical across branches.</p>
 */
public final class AdminScopeOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    /** The scope that grants Authorization Server administration. */
    public static final String ADMIN_SCOPE = "authserver.admin";

    /** The authority granted for {@link #ADMIN_SCOPE}. */
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    private static final String ADMIN_SCOPE_AUTHORITY = "SCOPE_" + ADMIN_SCOPE;

    private final OpaqueTokenIntrospector delegate;

    public AdminScopeOpaqueTokenIntrospector(OpaqueTokenIntrospector delegate) {
        Assert.notNull(delegate, "delegate cannot be null");
        this.delegate = delegate;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        OAuth2AuthenticatedPrincipal principal = delegate.introspect(token);
        boolean hasAdminScope = principal.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_SCOPE_AUTHORITY.equals(authority.getAuthority()));
        if (!hasAdminScope) {
            return principal;
        }
        List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(ADMIN_ROLE));
        return new OAuth2IntrospectionAuthenticatedPrincipal(
                principal.getName(), principal.getAttributes(), authorities);
    }
}
