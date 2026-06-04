/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.datacustodian.config;

import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Wraps the stock {@link OpaqueTokenIntrospector} and translates the introspected token's ESPI
 * scope into ESPI Function-Block authorities the Data Custodian enforces on its resource endpoints.
 *
 * <p>The Authorization Server issues the ESPI 4.0 scope grammar
 * ({@code FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13}). Spring's stock
 * introspector would map that whole string to a single opaque authority
 * ({@code SCOPE_FB=4_5_15;...}), which matches none of the DC's endpoint rules — so a real customer
 * token would 403 on every resource (issue #157). This introspector instead parses the scope via
 * {@link EspiScope} and emits one {@code FB_<n>} authority per granted function block (e.g.
 * {@code FB_4}, {@code FB_5}, {@code FB_15}).</p>
 *
 * <p>Non-FB scopes — the admin/OIDC scopes such as {@code DataCustodian_Admin_Access},
 * {@code ThirdParty_Admin_Access}, {@code openid}, {@code profile} — are passed through with the
 * conventional {@code SCOPE_} prefix so existing admin endpoint rules keep working.</p>
 *
 * <p>This replaces the contractor's non-ESPI {@code SCOPE_FB_<n>_READ/WRITE_3rd_party} vocabulary,
 * which had no relationship to the scopes the AS actually issues.</p>
 */
public final class EspiScopeOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

	private final OpaqueTokenIntrospector delegate;

	public EspiScopeOpaqueTokenIntrospector(OpaqueTokenIntrospector delegate) {
		this.delegate = delegate;
	}

	@Override
	public OAuth2AuthenticatedPrincipal introspect(String token) {
		OAuth2AuthenticatedPrincipal principal = delegate.introspect(token);
		Set<GrantedAuthority> authorities = new LinkedHashSet<>();
		for (String scope : scopesOf(principal)) {
			EspiScope parsed = EspiScope.parse(scope);
			if (parsed.functionBlocks().isEmpty()) {
				// admin / OIDC / any non-ESPI scope — keep the stock SCOPE_ authority.
				authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
			} else {
				// ESPI FB scope — one authority per granted function block.
				for (Integer fb : parsed.functionBlocks()) {
					authorities.add(new SimpleGrantedAuthority("FB_" + fb));
				}
			}
		}
		return new DefaultOAuth2AuthenticatedPrincipal(principal.getName(), principal.getAttributes(), authorities);
	}

	/**
	 * The introspected {@code scope} claim. Spring's stock introspector stores it as a
	 * {@code List<String>} split on spaces; tolerate a raw {@code String} as well.
	 */
	private static Collection<String> scopesOf(OAuth2AuthenticatedPrincipal principal) {
		Object scope = principal.getAttribute("scope");
		if (scope instanceof Collection<?> collection) {
			List<String> out = new ArrayList<>(collection.size());
			for (Object o : collection) {
				if (o != null) {
					out.add(o.toString());
				}
			}
			return out;
		}
		if (scope instanceof String s && !s.isBlank()) {
			return Arrays.asList(s.trim().split("\\s+"));
		}
		return List.of();
	}
}
