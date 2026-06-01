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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RetailCustomerUserDetailsService}. Verifies role-to-authority mapping for
 * both customer and custodian rows, and that the {@code enabled} / {@code account_locked} flags
 * are honored on the returned {@link UserDetails}.
 */
@ExtendWith(MockitoExtension.class)
class RetailCustomerUserDetailsServiceTest {

	@Mock private RetailCustomerRepository repository;
	@InjectMocks private RetailCustomerUserDetailsService service;

	@Test
	void customerRow_maps_to_ROLE_USER_authority() {
		when(repository.findByUsername("alice")).thenReturn(Optional.of(customer(
				"alice", "$2a$10$bcrypt", RetailCustomerEntity.ROLE_USER, true, false)));

		UserDetails details = service.loadUserByUsername("alice");

		assertThat(details)
				.extracting(UserDetails::getUsername, UserDetails::getPassword, UserDetails::isEnabled,
						UserDetails::isAccountNonLocked, UserDetails::isAccountNonExpired,
						UserDetails::isCredentialsNonExpired)
				.containsExactly("alice", "$2a$10$bcrypt", true, true, true, true);
		assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
	}

	@Test
	void custodianRow_maps_to_ROLE_CUSTODIAN_authority() {
		when(repository.findByUsername("admin")).thenReturn(Optional.of(customer(
				"admin", "$2a$10$bcrypt", RetailCustomerEntity.ROLE_CUSTODIAN, true, false)));

		UserDetails details = service.loadUserByUsername("admin");

		assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CUSTODIAN");
	}

	@Test
	void disabledRow_returns_disabled_principal() {
		when(repository.findByUsername("alice")).thenReturn(Optional.of(customer(
				"alice", "x", RetailCustomerEntity.ROLE_USER, false, false)));

		assertThat(service.loadUserByUsername("alice").isEnabled()).isFalse();
	}

	@Test
	void lockedRow_returns_account_locked_principal() {
		when(repository.findByUsername("alice")).thenReturn(Optional.of(customer(
				"alice", "x", RetailCustomerEntity.ROLE_USER, true, true)));

		assertThat(service.loadUserByUsername("alice").isAccountNonLocked()).isFalse();
	}

	@Test
	void nullRoleColumn_falls_back_to_ROLE_USER() {
		when(repository.findByUsername("alice")).thenReturn(Optional.of(customer(
				"alice", "x", null, true, false)));

		assertThat(service.loadUserByUsername("alice").getAuthorities())
				.extracting(Object::toString).containsExactly("ROLE_USER");
	}

	@Test
	void unknownUsername_throws_UsernameNotFoundException() {
		when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("ghost");
	}

	private static RetailCustomerEntity customer(String username, String password, String role,
												 boolean enabled, boolean locked) {
		RetailCustomerEntity entity = new RetailCustomerEntity();
		entity.setUsername(username);
		entity.setPassword(password);
		entity.setRole(role);
		entity.setEnabled(enabled);
		entity.setAccountLocked(locked);
		return entity;
	}
}
