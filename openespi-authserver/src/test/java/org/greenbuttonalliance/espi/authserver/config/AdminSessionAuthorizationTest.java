/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.FactorAuthorizationDecision;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("#205: adminSessionAuthorization = admin role AND password AND passkey factors")
class AdminSessionAuthorizationTest {

	private static AuthorizationResult decide(String role, String... factors) {
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(role));
		for (String factor : factors) {
			authorities.add(FactorGrantedAuthority.fromAuthority(factor));
		}
		var authentication = UsernamePasswordAuthenticationToken.authenticated("someone", null, authorities);
		return AuthorizationServerConfig.adminSessionAuthorization()
				.authorize(() -> authentication, new RequestAuthorizationContext(new MockHttpServletRequest()));
	}

	@Test
	@DisplayName("both factors + ROLE_ADMIN / ROLE_DC_ADMIN -> granted")
	void bothFactorsGranted() {
		assertThat(List.of(
				decide("ROLE_ADMIN", FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.WEBAUTHN_AUTHORITY),
				decide("ROLE_DC_ADMIN", FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.WEBAUTHN_AUTHORITY)))
				.extracting(AuthorizationResult::isGranted)
				.containsOnly(true);
	}

	@Test
	@DisplayName("admin missing a factor -> FactorAuthorizationDecision (drives the passkey/password step)")
	void missingFactorIsFactorDecision() {
		assertThat(List.of(
				decide("ROLE_ADMIN", FactorGrantedAuthority.PASSWORD_AUTHORITY),
				decide("ROLE_ADMIN", FactorGrantedAuthority.WEBAUTHN_AUTHORITY),
				decide("ROLE_ADMIN")))
				.allSatisfy(result -> assertThat(result)
						.isInstanceOf(FactorAuthorizationDecision.class)
						.extracting(AuthorizationResult::isGranted)
						.isEqualTo(false));
	}

	@Test
	@DisplayName("non-admin (ROLE_CUSTOMER), even with factors -> plain denial, never a factor prompt")
	void customerIsPlainDenial() {
		assertThat(decide("ROLE_CUSTOMER", FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.WEBAUTHN_AUTHORITY))
				.isNotInstanceOf(FactorAuthorizationDecision.class)
				.extracting(AuthorizationResult::isGranted)
				.isEqualTo(false);
	}
}
