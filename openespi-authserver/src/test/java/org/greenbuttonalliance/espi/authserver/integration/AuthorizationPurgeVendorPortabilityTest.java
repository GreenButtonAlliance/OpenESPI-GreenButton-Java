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

package org.greenbuttonalliance.espi.authserver.integration;

import org.flywaydb.core.Flyway;
import org.greenbuttonalliance.espi.authserver.config.AuthorizationPurgeProperties;
import org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.PurgeAudit;
import org.greenbuttonalliance.espi.authserver.service.AuthorizationPurgeService;
import org.greenbuttonalliance.espi.authserver.service.AuthorizationPurgeService.PurgeResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.authorizationIds;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.auditTimestamps;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.insertAudit;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.insertAuthorization;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.purgeAudits;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.token;

/**
 * Proves the purge SQL is portable beyond MySQL (#192): runs one purge pass, the audit insert
 * (including the vendor-specific JSON binding of {@code additional_data}) and the retention pass on
 * H2 and PostgreSQL. No Spring context: a plain {@link JdbcTemplate} over the vendor schema.
 *
 * <ul>
 *   <li><strong>H2</strong>: the real {@code db/vendor/h2} migrations, including the new V8
 *       {@code oauth2_audit_log}.</li>
 *   <li><strong>PostgreSQL</strong>: {@code db/vendor/postgresql} V1 only, plus the
 *       {@code oauth2_audit_log} DDL copied from V2, because V2 itself fails on a fresh database
 *       (#152). Replace with a full migrate once #152 lands.</li>
 * </ul>
 *
 * <p>Tagged {@code testcontainers-it} so CI runs it with the other Docker-backed integration tests
 * (the PostgreSQL half needs Docker; the H2 half is in-memory).</p>
 */
@Tag("testcontainers-it")
@DisplayName("Authorization purge (#192) vendor portability")
class AuthorizationPurgeVendorPortabilityTest {

	private static final Instant NOW = Instant.parse("2035-01-01T00:00:00Z");

	/** oauth2_audit_log as created by db/vendor/postgresql/V2_0_0 (column list and types). */
	private static final String POSTGRES_AUDIT_LOG_DDL = """
			CREATE TABLE oauth2_audit_log (
			    id BIGSERIAL PRIMARY KEY,
			    event_type VARCHAR(50) NOT NULL,
			    client_id VARCHAR(100) NOT NULL,
			    principal_name VARCHAR(200) DEFAULT NULL,
			    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
			    ip_address INET DEFAULT NULL,
			    user_agent VARCHAR(500) DEFAULT NULL,
			    session_id VARCHAR(128) DEFAULT NULL,
			    request_id VARCHAR(128) DEFAULT NULL,
			    success BOOLEAN NOT NULL DEFAULT TRUE,
			    error_code VARCHAR(50) DEFAULT NULL,
			    error_description VARCHAR(500) DEFAULT NULL,
			    scopes_requested VARCHAR(1000) DEFAULT NULL,
			    scopes_granted VARCHAR(1000) DEFAULT NULL,
			    grant_type VARCHAR(50) DEFAULT NULL,
			    espi_version VARCHAR(10) DEFAULT '4.0',
			    compliance_flags JSONB DEFAULT NULL,
			    additional_data JSONB DEFAULT NULL
			)""";

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@DisplayName("H2 (db/vendor/h2 migrations)")
	class H2 {

		private JdbcTemplate jdbc;
		private DataSource dataSource;

		@BeforeAll
		void migrate() {
			dataSource = new DriverManagerDataSource(
					"jdbc:h2:mem:purge192;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
			Flyway.configure().dataSource(dataSource).locations("classpath:db/vendor/h2").load().migrate();
			jdbc = new JdbcTemplate(dataSource);
			// H2's V1 seeds no clients; the purge audit falls back to registered_client_id when the
			// registered client cannot be resolved.
		}

		@Test
		@DisplayName("purges, audits with a JSON object, keeps live rows, and expires old purge audits")
		void purgesOnH2() {
			assertPurgeRoundTrip(jdbc, dataSource, false, "h2-client", "h2-client");
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@DisplayName("PostgreSQL (db/vendor/postgresql V1 + V2 audit table)")
	class Postgres {

		private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
				.withDatabaseName("oauth2_authserver")
				.withUsername("test_user")
				.withPassword("test_password");

		private JdbcTemplate jdbc;
		private DataSource dataSource;

		@BeforeAll
		void migrate() {
			postgres.start();
			dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
					postgres.getPassword());
			Flyway.configure().dataSource(dataSource).locations("classpath:db/vendor/postgresql")
					.target("1.0.0").load().migrate();
			jdbc = new JdbcTemplate(dataSource);
			jdbc.execute(POSTGRES_AUDIT_LOG_DDL);
		}

		@AfterAll
		void stop() {
			postgres.stop();
		}

		@Test
		@DisplayName("purges, audits into JSONB, keeps live rows, and expires old purge audits")
		void purgesOnPostgres() {
			// V1 seeds 'default-third-party' (client_id third_party); the FK from oauth2_authorization
			// to oauth2_registered_client requires a real client here.
			assertPurgeRoundTrip(jdbc, dataSource, true, "default-third-party", "third_party");
			assertThat(jdbc.queryForObject("SELECT jsonb_typeof(additional_data) FROM oauth2_audit_log"
					+ " WHERE event_type = ?", String.class, AuthorizationPurgeService.PURGE_EVENT_TYPE))
					.isEqualTo("object");
		}
	}

	private static void assertPurgeRoundTrip(JdbcTemplate jdbc, DataSource dataSource, boolean textTokenValues,
			String registeredClientId, String expectedAuditClientId) {
		insertAuthorization(jdbc, textTokenValues, "expired", registeredClientId, "customer-192",
				token("access_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(29))),
				token("refresh_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(8))));
		insertAuthorization(jdbc, textTokenValues, "refreshable", registeredClientId, "customer-192",
				token("access_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(29))),
				token("refresh_token", NOW.minus(Duration.ofDays(30)), NOW.plus(Duration.ofDays(1))));
		insertAuthorization(jdbc, textTokenValues, "state-only", registeredClientId, "customer-192");
		insertAudit(jdbc, AuthorizationPurgeService.PURGE_EVENT_TYPE, expectedAuditClientId, NOW.minus(Duration.ofDays(400)));

		AuthorizationPurgeService purger = new AuthorizationPurgeService(jdbc,
				new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
				new AuthorizationPurgeProperties(true, Duration.ofHours(1), Duration.ofMinutes(5),
						Duration.ofDays(7), 500, Duration.ofDays(395)),
				Clock.fixed(NOW, ZoneOffset.UTC));

		assertThat(purger.purge()).isEqualTo(new PurgeResult(1, 1));
		assertThat(authorizationIds(jdbc)).containsExactly("refreshable", "state-only");
		assertThat(purgeAudits(jdbc)).singleElement()
				.extracting(PurgeAudit::clientId, PurgeAudit::principalName, PurgeAudit::authorizationId,
						audit -> audit.data("latest_expires_at"))
				.containsExactly(expectedAuditClientId, "customer-192", "expired",
						NOW.minus(Duration.ofDays(8)).toString());
		assertThat(auditTimestamps(jdbc, AuthorizationPurgeService.PURGE_EVENT_TYPE)).containsExactly(NOW);
	}
}
