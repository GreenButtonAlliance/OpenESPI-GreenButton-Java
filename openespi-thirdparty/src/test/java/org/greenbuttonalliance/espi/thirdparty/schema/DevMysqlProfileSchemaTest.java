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
import org.testcontainers.mysql.MySQLContainer;

/**
 * The {@code dev-mysql} profile on a real MySQL server (Testcontainers). Only the connection is
 * overridden; the profile's own Flyway locations and {@code validate} are what is tested. Skipped
 * where Docker is not available; CI runs it.
 */
@SpringBootTest
@ActiveProfiles("dev-mysql")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Third Party schema: dev-mysql profile (MySQL)")
class DevMysqlProfileSchemaTest extends AbstractThirdPartySchemaTest {

	@Container
	static MySQLContainer mysql = new MySQLContainer("mysql:9.5.0")
			.withDatabaseName("thirdparty")
			.withUsername("espi")
			.withPassword("espi123");

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}
}
