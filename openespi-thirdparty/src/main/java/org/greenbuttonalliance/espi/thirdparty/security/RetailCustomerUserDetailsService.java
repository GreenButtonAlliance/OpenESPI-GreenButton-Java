/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.thirdparty.security;

import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads the Third Party's own accounts for form login (#249).
 *
 * <p>TP accounts are {@link RetailCustomerEntity} rows in the TP's own database: customers carry
 * {@code ROLE_USER}, TP administrators {@code ROLE_CUSTODIAN}. They are the TP's customers, not the
 * Data Custodian's; a TP account's {@code Long} id means nothing at the DC and is never sent there.
 * The authority is the single {@code role} column; {@code enabled} and {@code account_locked} are
 * honored. Mirrors the DC's {@code RetailCustomerUserDetailsService}.</p>
 */
@Service
@RequiredArgsConstructor
public class RetailCustomerUserDetailsService implements UserDetailsService {

	private final RetailCustomerRepository repository;

	@Override
	public UserDetails loadUserByUsername(String username) {
		RetailCustomerEntity entity = repository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("No Third Party account with username: " + username));

		String role = entity.getRole() != null ? entity.getRole() : RetailCustomerEntity.ROLE_USER;

		return User.withUsername(entity.getUsername())
				.password(entity.getPassword() != null ? entity.getPassword() : "")
				.authorities(List.of(new SimpleGrantedAuthority(role)))
				.disabled(!Boolean.TRUE.equals(entity.getEnabled()))
				.accountLocked(Boolean.TRUE.equals(entity.getAccountLocked()))
				.accountExpired(false)
				.credentialsExpired(false)
				.build();
	}
}
