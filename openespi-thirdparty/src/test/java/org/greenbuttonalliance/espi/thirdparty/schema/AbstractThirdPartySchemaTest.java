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

package org.greenbuttonalliance.espi.thirdparty.schema;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Third Party's schema is owned by Flyway, using the openespi-common scripts, and Hibernate only
 * validates it (#267). Each subclass starts the application on one real Third Party profile and
 * database. The application context failing to start means the entities and the scripts disagree
 * ({@code ddl-auto: validate}).
 */
abstract class AbstractThirdPartySchemaTest {

	/**
	 * The openespi-common migrations: V1 and V3 shared, V2 vendor-specific, V5 shared. V4 is the Data
	 * Custodian's handoff-nonce table from openespi-handoff, which the Third Party does not use; the
	 * gap is expected.
	 */
	static final String[] EXPECTED_VERSIONS = {"1", "2", "3", "5"};

	@Autowired
	private Flyway flyway;

	@Autowired
	protected Environment environment;

	@Autowired
	protected RetailCustomerRepository retailCustomerRepository;

	@Test
	@DisplayName("Flyway applied the openespi-common migrations, and Hibernate only validates the schema")
	void flyway_owns_the_schema() {
		String[] applied = Arrays.stream(flyway.info().applied())
				.map(MigrationInfo::getVersion)
				.filter(v -> v != null)
				.map(v -> v.getVersion())
				.toArray(String[]::new);

		assertThat(applied).containsExactly(EXPECTED_VERSIONS);
		assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class, true)).isTrue();
	}

	@Test
	@DisplayName("TP accounts (retail_customers) can be stored and read back")
	void retail_customers_round_trip() {
		// RetailCustomerEntity allows 4-30 characters.
		String username = "schema-" + UUID.randomUUID().toString().substring(0, 8);
		RetailCustomerEntity account = new RetailCustomerEntity(username, "Schema", "Test",
				username + "@example.local", RetailCustomerEntity.ROLE_USER);
		account.setPassword("$2a$10$abcdefghijklmnopqrstuuJ0123456789abcdefghijklmnopqrstu");
		account.setEnabled(Boolean.TRUE);
		account.setAccountLocked(Boolean.FALSE);
		retailCustomerRepository.save(account);

		assertThat(retailCustomerRepository.findByUsername(username))
				.hasValueSatisfying(found -> assertThat(found)
						.extracting(RetailCustomerEntity::getRole, RetailCustomerEntity::getEnabled)
						.containsExactly(RetailCustomerEntity.ROLE_USER, true));

		retailCustomerRepository.findByUsername(username).ifPresent(retailCustomerRepository::delete);
	}
}
