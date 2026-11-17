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

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The {@code dev-postgresql} profile on a real PostgreSQL server (Testcontainers). Only the
 * connection is overridden; the profile's own Flyway locations and {@code validate} are what is
 * tested. Skipped where Docker is not available; CI runs it.
 */
@SpringBootTest
@ActiveProfiles("dev-postgresql")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Third Party schema: dev-postgresql profile (PostgreSQL)")
class DevPostgresqlProfileSchemaTest extends AbstractThirdPartySchemaTest {

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("thirdparty")
			.withUsername("espi")
			.withPassword("espi123");

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
}
