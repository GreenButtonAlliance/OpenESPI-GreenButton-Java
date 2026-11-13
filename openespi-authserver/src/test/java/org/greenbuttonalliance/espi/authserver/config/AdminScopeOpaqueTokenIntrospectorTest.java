/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.authserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("#204: AdminScopeOpaqueTokenIntrospector scope-to-role mapping")
class AdminScopeOpaqueTokenIntrospectorTest {

	private static OpaqueTokenIntrospector delegateGranting(String... scopes) {
		List<GrantedAuthority> authorities = java.util.Arrays.stream(scopes)
				.<GrantedAuthority>map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
				.toList();
		return token -> new OAuth2IntrospectionAuthenticatedPrincipal(
				"client", Map.of("active", true), authorities);
	}

	@Test
	@DisplayName("authserver.admin adds ROLE_ADMIN and keeps the SCOPE_ authority")
	void adminScopeMapsToRoleAdmin() {
		assertThat(new AdminScopeOpaqueTokenIntrospector(delegateGranting("authserver.admin")).introspect("t"))
				.extracting(p -> p.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
				.containsExactlyInAnyOrder("SCOPE_authserver.admin", "ROLE_ADMIN");
	}

	@Test
	@DisplayName("ESPI admin scopes are NOT mapped to ROLE_ADMIN")
	void espiScopesAreNotAdmin() {
		assertThat(new AdminScopeOpaqueTokenIntrospector(
						delegateGranting("DataCustodian_Admin_Access", "ThirdParty_Admin_Access")).introspect("t"))
				.extracting(p -> p.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
				.containsExactlyInAnyOrder("SCOPE_DataCustodian_Admin_Access", "SCOPE_ThirdParty_Admin_Access");
	}

	@Test
	@DisplayName("adminClientDrifted compares the secret with matches(), not hash equality")
	void driftUsesPasswordMatching() {
		var encoder = org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
		var stored = org.springframework.security.oauth2.server.authorization.client.RegisteredClient
				.withId("id").clientId("espi_admin")
				.clientSecret(encoder.encode("s3cret"))
				.authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("authserver.admin")
				.tokenSettings(org.springframework.security.oauth2.server.authorization.settings.TokenSettings.builder()
						.accessTokenFormat(org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat.REFERENCE)
						.accessTokenTimeToLive(AuthorizationServerConfig.ADMIN_TOKEN_TTL)
						.build())
				.build();

		assertThat(List.of(
				AuthorizationServerConfig.adminClientDrifted(stored, encoder, "s3cret"),
				AuthorizationServerConfig.adminClientDrifted(stored, encoder, "rotated")))
				.containsExactly(false, true);
	}
}
