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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EspiScopeOpaqueTokenIntrospector} — the ESPI-scope → FB-authority
 * translation that fixes the AS↔DC vocabulary mismatch (#157).
 */
@DisplayName("ESPI FB-scope introspector (#157)")
class EspiScopeOpaqueTokenIntrospectorTest {

	/** A stub delegate that returns a principal whose {@code scope} attribute is the given value. */
	private static OpaqueTokenIntrospector delegateWithScope(Object scopeAttribute) {
		return token -> new DefaultOAuth2AuthenticatedPrincipal(
				"42", Map.of("scope", scopeAttribute, "active", true),
				AuthorityUtils.NO_AUTHORITIES);
	}

	private static List<String> authorities(OAuth2AuthenticatedPrincipal p) {
		return p.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
	}

	@Test
	@DisplayName("customer FB scope -> one FB_<n> authority per function block")
	void customerFbScopeMapsToFbAuthorities() {
		OpaqueTokenIntrospector introspector = new EspiScopeOpaqueTokenIntrospector(delegateWithScope(
				List.of("openid", "profile",
						"FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")));

		List<String> authorities = authorities(introspector.introspect("tok"));

		assertThat(authorities)
				.contains("FB_4", "FB_5", "FB_15")     // function blocks
				.contains("SCOPE_openid", "SCOPE_profile") // non-FB scopes pass through
				.doesNotContain("SCOPE_FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
	}

	@Test
	@DisplayName("admin scope -> SCOPE_ authority (unchanged)")
	void adminScopeMapsToScopeAuthority() {
		OpaqueTokenIntrospector introspector = new EspiScopeOpaqueTokenIntrospector(
				delegateWithScope(List.of("DataCustodian_Admin_Access")));

		assertThat(authorities(introspector.introspect("tok")))
				.containsExactly("SCOPE_DataCustodian_Admin_Access");
	}

	@Test
	@DisplayName("scope as a single space-delimited String is also supported")
	void scopeAsStringIsParsed() {
		OpaqueTokenIntrospector introspector = new EspiScopeOpaqueTokenIntrospector(
				delegateWithScope("profile FB=4_10;IntervalDuration=3600"));

		assertThat(authorities(introspector.introspect("tok")))
				.contains("SCOPE_profile", "FB_4", "FB_10");
	}

	@Test
	@DisplayName("attributes (e.g. ESPI URI claims) are preserved")
	void attributesArePreserved() {
		OAuth2AuthenticatedPrincipal p = new EspiScopeOpaqueTokenIntrospector(
				delegateWithScope(List.of("DataCustodian_Admin_Access"))).introspect("tok");

		assertThat(p.getName()).isEqualTo("42");
		assertThat((Boolean) p.getAttribute("active")).isTrue();
	}
}
