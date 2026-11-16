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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDevAdminSeedRunnerTest {

	@Mock
	private RetailCustomerRepository repository;

	private final PasswordEncoder encoder = new BCryptPasswordEncoder();

	@Test
	void runs_on_the_local_profile_only() {
		assertThat(LocalDevAdminSeedRunner.class.getAnnotation(Profile.class).value()).containsExactly("local");
	}

	@Test
	void seeds_an_enabled_bcrypt_custodian_admin() {
		when(repository.findByUsername("admin")).thenReturn(Optional.empty());

		new LocalDevAdminSeedRunner(repository, encoder).run();

		ArgumentCaptor<RetailCustomerEntity> saved = ArgumentCaptor.forClass(RetailCustomerEntity.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue())
				.extracting(RetailCustomerEntity::getUsername, RetailCustomerEntity::getRole,
						RetailCustomerEntity::getEnabled, RetailCustomerEntity::getAccountLocked)
				.containsExactly("admin", RetailCustomerEntity.ROLE_CUSTODIAN, true, false);
		assertThat(encoder.matches("admin", saved.getValue().getPassword())).isTrue();
	}

	@Test
	void does_nothing_when_the_admin_exists() {
		when(repository.findByUsername("admin")).thenReturn(Optional.of(new RetailCustomerEntity()));

		new LocalDevAdminSeedRunner(repository, encoder).run();

		verify(repository, never()).save(any());
	}
}
