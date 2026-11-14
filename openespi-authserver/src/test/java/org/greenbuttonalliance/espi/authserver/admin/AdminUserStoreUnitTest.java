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

package org.greenbuttonalliance.espi.authserver.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@DisplayName("#205: admin user store unit tests")
class AdminUserStoreUnitTest {

	private static final String HASH = new BCryptPasswordEncoder().encode("whatever-password");

	@Test
	@DisplayName("toStoredBcrypt accepts bare and {bcrypt} hashes, rejects plaintext and other schemes")
	void bcryptValidation() {
		assertThat(List.of(
				AdminUserBootstrap.toStoredBcrypt(HASH),
				AdminUserBootstrap.toStoredBcrypt("{bcrypt}" + HASH)))
				.containsOnly("{bcrypt}" + HASH);
		for (String bad : List.of("plaintext-password", "{noop}plaintext", "{sha256}abc",
				"$2a$10$tooShort", "{bcrypt}not-a-hash")) {
			assertThatThrownBy(() -> AdminUserBootstrap.toStoredBcrypt(bad))
					.as(bad)
					.isInstanceOf(IllegalStateException.class);
		}
	}

	@Test
	@DisplayName("bootstrap: no config -> no-op; partial config -> no-op; non-empty table -> no-op")
	void bootstrapGuards() {
		AdminUserRepository repository = mock(AdminUserRepository.class);
		when(repository.count()).thenReturn(0L);

		assertThat(List.of(
				new AdminUserBootstrap(repository, "", "").bootstrap(),
				new AdminUserBootstrap(repository, "admin", "").bootstrap(),
				new AdminUserBootstrap(repository, "", HASH).bootstrap()))
				.containsOnly(false);

		when(repository.count()).thenReturn(3L);
		assertThat(new AdminUserBootstrap(repository, "admin", "plaintext").bootstrap()).isFalse();
		verify(repository, never()).insert(anyString(), anyString(), anyBoolean(), any());
	}

	@Test
	@DisplayName("bootstrap rejects a numeric (customer-principal-shaped) username")
	void bootstrapRejectsNumericUsername() {
		AdminUserRepository repository = mock(AdminUserRepository.class);
		when(repository.count()).thenReturn(0L);

		assertThatThrownBy(() -> new AdminUserBootstrap(repository, "42", HASH).bootstrap())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("normalizeRoles prefixes and upper-cases, rejects unknown roles")
	void normalizeRoles() {
		assertThat(AdminUserService.normalizeRoles(List.of("admin", "ROLE_DC_ADMIN", " dc_admin ")))
				.containsExactly("ROLE_ADMIN", "ROLE_DC_ADMIN");
		assertThatThrownBy(() -> AdminUserService.normalizeRoles(List.of("CUSTOMER")))
				.isInstanceOf(AdminUserService.InvalidRequestException.class);
	}

	@Test
	@DisplayName("AdminUserDetailsService maps admin_users rows and fails for anyone else")
	void userDetailsService() {
		AdminUserRepository repository = mock(AdminUserRepository.class);
		when(repository.findCredentialsByUsername("ops")).thenReturn(Optional.of(
				new AdminUserRepository.Credentials("id-1", "ops", "{bcrypt}" + HASH, false, Set.of("ROLE_ADMIN"))));
		when(repository.findCredentialsByUsername("42")).thenReturn(Optional.empty());
		AdminUserDetailsService service = new AdminUserDetailsService(repository);

		assertThat(service.loadUserByUsername("ops"))
				.satisfies(user -> assertThat(user)
						.extracting(u -> u.getUsername(), u -> u.getPassword(), u -> u.isEnabled())
						.containsExactly("ops", "{bcrypt}" + HASH, false))
				.satisfies(user -> assertThat(user.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactly("ROLE_ADMIN"));
		assertThatThrownBy(() -> service.loadUserByUsername("42")).isInstanceOf(UsernameNotFoundException.class);
	}
}
