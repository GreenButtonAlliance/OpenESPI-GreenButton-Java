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
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a single sandbox custodian admin on application startup if no admin exists.
 *
 * <p><strong>DEV / SANDBOX ONLY.</strong> Gated by {@link Profile} to never fire in production.
 * The credentials (username {@code admin}, password {@code admin}) are public knowledge by virtue
 * of being in this source file — production deployments use their own sync from the utility CRM
 * to populate {@code retail_customers} and never activate the dev profiles.</p>
 *
 * <p>This is an out-of-Flyway seed because:</p>
 * <ul>
 *   <li>Flyway has no conditional-on-profile mechanism — a seed migration would fire in production too.</li>
 *   <li>The bcrypt cost makes the password expensive to embed as a literal in SQL; using the
 *       runtime encoder keeps the cleartext readable.</li>
 *   <li>Idempotent: if any {@code ROLE_CUSTODIAN} customer already exists, this runner does nothing.</li>
 * </ul>
 *
 * <p>This runner exists only because PR C2a brings DC's customer login online for the first time.
 * Once a real production deployment runs, it ships without the dev profile and the seed never fires.</p>
 */
@Slf4j
@Component
@Profile({"dev-mysql", "dev-postgresql", "local", "test", "testcontainers"})
@RequiredArgsConstructor
public class DevSandboxAdminSeedRunner implements CommandLineRunner {

	private static final String DEFAULT_ADMIN_USERNAME = "admin";
	private static final String DEFAULT_ADMIN_PASSWORD = "admin";

	private final RetailCustomerRepository repository;
	private final PasswordEncoder customerPasswordEncoder;

	@Override
	public void run(String... args) {
		if (repository.findByUsername(DEFAULT_ADMIN_USERNAME).isPresent()) {
			log.debug("Dev sandbox admin '{}' already present; skipping seed.", DEFAULT_ADMIN_USERNAME);
			return;
		}

		RetailCustomerEntity admin = new RetailCustomerEntity(
				DEFAULT_ADMIN_USERNAME, "Sandbox", "Admin",
				"admin@example.local", RetailCustomerEntity.ROLE_CUSTODIAN);
		admin.setPassword(customerPasswordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
		admin.setEnabled(Boolean.TRUE);
		admin.setAccountLocked(Boolean.FALSE);

		repository.save(admin);

		log.warn("Seeded DEV sandbox admin: username='{}', password='{}' — DO NOT USE IN PRODUCTION",
				DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
	}
}
