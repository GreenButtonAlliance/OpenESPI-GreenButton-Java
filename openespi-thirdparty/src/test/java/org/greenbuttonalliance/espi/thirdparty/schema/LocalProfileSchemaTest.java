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

import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Third Party started with <em>no</em> profile chosen, as {@code mvn spring-boot:run} does: it
 * must default to {@code local} (in-memory H2 in MySQL mode). Runs on its own in-memory database so
 * it never meets other test contexts. No {@code @ActiveProfiles} on purpose.
 */
@SpringBootTest(properties =
		"spring.datasource.url=jdbc:h2:mem:tp-local-schema-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
@DisplayName("Third Party schema: default profile is local (H2)")
class LocalProfileSchemaTest extends AbstractThirdPartySchemaTest {

	@Autowired
	private RetailCustomerRepository retailCustomerRepository;

	@Autowired
	private Environment environment;

	@Test
	@DisplayName("with no profile chosen, the Third Party runs on local (not dev-mysql from openespi-common)")
	void default_profile_is_local() {
		assertThat(environment.getActiveProfiles()).containsExactly("local");
	}

	@Test
	@DisplayName("the default local profile listens on loopback only, so its DEV ONLY accounts never reach the network")
	void local_profile_binds_loopback_only() {
		assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
	}

	@Test
	@DisplayName("the local profile's development accounts are seeded into the Flyway-created schema")
	void dev_accounts_are_seeded() {
		assertThat(retailCustomerRepository.findByUsername("admin")).isPresent();
		assertThat(retailCustomerRepository.findByUsername("customer")).isPresent();
	}
}
