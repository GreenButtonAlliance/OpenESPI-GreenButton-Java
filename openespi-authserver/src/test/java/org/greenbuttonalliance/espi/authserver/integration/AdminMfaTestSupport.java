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

package org.greenbuttonalliance.espi.authserver.integration;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Simulated admin sessions for #205 tests. A real WebAuthn ceremony cannot run in MockMvc, so the
 * session's {@code FactorGrantedAuthority} values (what DaoAuthenticationProvider and
 * WebAuthnAuthenticationProvider add on success) are set directly.
 */
final class AdminMfaTestSupport {

	private AdminMfaTestSupport() {
	}

	/** Admin session with the role and BOTH the password and passkey factors. */
	static RequestPostProcessor adminWithBothFactors(String username, String role) {
		return adminWithFactors(username, role,
				FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.WEBAUTHN_AUTHORITY);
	}

	/** Admin session with the role and only the password factor (no passkey yet). */
	static RequestPostProcessor adminWithPasswordOnly(String username, String role) {
		return adminWithFactors(username, role, FactorGrantedAuthority.PASSWORD_AUTHORITY);
	}

	static RequestPostProcessor adminWithFactors(String username, String role, String... factors) {
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(role));
		for (String factor : factors) {
			authorities.add(FactorGrantedAuthority.fromAuthority(factor));
		}
		return authentication(UsernamePasswordAuthenticationToken.authenticated(username, null, authorities));
	}
}
