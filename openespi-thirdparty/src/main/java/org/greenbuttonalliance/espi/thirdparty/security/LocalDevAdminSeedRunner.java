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
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the Third Party's development administrator ({@code admin} / {@code admin},
 * {@code ROLE_CUSTODIAN}) on the {@code local} profile only (#249).
 *
 * <p><strong>DEV ONLY.</strong> The credentials are public by being in this file. Other profiles
 * (MySQL, PostgreSQL) need working TP Flyway first (#267) and never seed an account. Idempotent:
 * does nothing when an {@code admin} account already exists. Mirrors the DC's
 * {@code DevSandboxAdminSeedRunner}.</p>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDevAdminSeedRunner implements CommandLineRunner {

	static final String ADMIN_USERNAME = "admin";
	static final String ADMIN_PASSWORD = "admin";

	private final RetailCustomerRepository repository;
	private final PasswordEncoder customerPasswordEncoder;

	@Override
	public void run(String... args) {
		if (repository.findByUsername(ADMIN_USERNAME).isPresent()) {
			log.debug("Third Party dev admin '{}' already present; skipping seed.", ADMIN_USERNAME);
			return;
		}

		RetailCustomerEntity admin = new RetailCustomerEntity(
				ADMIN_USERNAME, "Third Party", "Admin", "admin@example.local", RetailCustomerEntity.ROLE_CUSTODIAN);
		admin.setPassword(customerPasswordEncoder.encode(ADMIN_PASSWORD));
		admin.setEnabled(Boolean.TRUE);
		admin.setAccountLocked(Boolean.FALSE);
		repository.save(admin);

		log.warn("Seeded Third Party DEV admin: username='{}', password='{}' - DO NOT USE IN PRODUCTION",
				ADMIN_USERNAME, ADMIN_PASSWORD);
	}
}
