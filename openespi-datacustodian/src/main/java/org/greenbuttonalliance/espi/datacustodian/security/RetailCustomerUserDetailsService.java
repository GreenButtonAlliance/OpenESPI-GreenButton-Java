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

package org.greenbuttonalliance.espi.datacustodian.security;

import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads a Spring Security {@link UserDetails} for a username by looking up the
 * {@link RetailCustomerEntity} row. Authorities are derived from the single {@code role} column
 * (e.g. {@code ROLE_CUSTODIAN}, {@code ROLE_USER}, {@code ROLE_ADMIN}); {@code enabled} and
 * {@code account_locked} flags are honored.
 *
 * <p>This is the security adapter wired into the customer-facing
 * {@code CustomerLoginSecurityConfiguration} {@code SecurityFilterChain}. It is intentionally
 * <strong>not</strong> in {@code openespi-common}: {@code UserDetailsService} is a Spring Security
 * web concern and belongs in the consuming layer, not the persistence/domain layer.</p>
 *
 * <p>Sandbox note: the codebase uses a single-entity role-discriminator model
 * ({@code retail_customers.role} carries both customer and custodian rows). The greenfield ideal
 * would split admin and customer into separate types; the codebase had already chosen the
 * single-entity path, so this UDS works against the existing schema.</p>
 */
@Service
@RequiredArgsConstructor
public class RetailCustomerUserDetailsService implements UserDetailsService {

	private final RetailCustomerRepository repository;

	@Override
	public UserDetails loadUserByUsername(String username) {
		RetailCustomerEntity entity = repository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("No customer with username: " + username));

		String role = entity.getRole() != null ? entity.getRole() : RetailCustomerEntity.ROLE_USER;
		boolean enabled = Boolean.TRUE.equals(entity.getEnabled());
		boolean accountNonLocked = !Boolean.TRUE.equals(entity.getAccountLocked());

		return User.withUsername(entity.getUsername())
				.password(entity.getPassword() != null ? entity.getPassword() : "")
				.authorities(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role)))
				.disabled(!enabled)
				.accountLocked(!accountNonLocked)
				.accountExpired(false)
				.credentialsExpired(false)
				.build();
	}
}
