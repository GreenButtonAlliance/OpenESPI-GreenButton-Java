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
 * Seeds the Third Party's development accounts on the {@code local} profile only (#249):
 * <ul>
 *   <li>{@code admin} / {@code admin}, a TP administrator ({@code ROLE_CUSTODIAN});</li>
 *   <li>{@code customer} / {@code customer}, a TP customer ({@code ROLE_USER}), who sees the
 *       Data Access Authorization cards and can try the TP &rarr; Data Custodian authorization.</li>
 * </ul>
 *
 * <p><strong>DEV ONLY.</strong> The credentials are public by being in this file. Other profiles
 * (MySQL, PostgreSQL) need working TP Flyway first (#267) and never seed an account. Idempotent:
 * an account that already exists is left alone. Mirrors the DC's {@code DevSandboxAdminSeedRunner}.</p>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDevAccountSeedRunner implements CommandLineRunner {

	static final String ADMIN_USERNAME = "admin";
	static final String CUSTOMER_USERNAME = "customer";

	private final RetailCustomerRepository repository;
	private final PasswordEncoder customerPasswordEncoder;

	@Override
	public void run(String... args) {
		seed(ADMIN_USERNAME, "Third Party", "Admin", RetailCustomerEntity.ROLE_CUSTODIAN);
		seed(CUSTOMER_USERNAME, "Third Party", "Customer", RetailCustomerEntity.ROLE_USER);
	}

	/** Seeds one account whose password equals its username. */
	private void seed(String username, String firstName, String lastName, String role) {
		if (repository.findByUsername(username).isPresent()) {
			log.debug("Third Party dev account '{}' already present; skipping seed.", username);
			return;
		}

		RetailCustomerEntity account = new RetailCustomerEntity(
				username, firstName, lastName, username + "@example.local", role);
		account.setPassword(customerPasswordEncoder.encode(username));
		account.setEnabled(Boolean.TRUE);
		account.setAccountLocked(Boolean.FALSE);
		repository.save(account);

		log.warn("Seeded Third Party DEV account: username='{}', password='{}', role={} - DO NOT USE IN PRODUCTION",
				username, username, role);
	}
}
