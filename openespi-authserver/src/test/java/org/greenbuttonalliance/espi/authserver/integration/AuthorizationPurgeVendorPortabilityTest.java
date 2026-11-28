/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
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
import org.greenbuttonalliance.espi.authserver.repository.AuditLogDao;
import org.greenbuttonalliance.espi.authserver.repository.RegisteredClientAdminDao;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.authorizationIds;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.auditTimestamps;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.insertAudit;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.insertAuthorization;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.insertConsent;
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
 *   <li><strong>PostgreSQL</strong>: the real {@code db/vendor/postgresql} migrations, whose V2
 *       creates {@code oauth2_audit_log} (it applies on a fresh database since #152).</li>
 * </ul>
 *
 * <p>The H2 half also checks V11's cascading client delete and {@code AuditLogDao}'s JSON insert, which
 * the other vendors' Testcontainers tests check for theirs (#344 review).</p>
 *
 * <p>Tagged {@code testcontainers-it} with the other Docker-backed integration tests (the PostgreSQL
 * half needs Docker; the H2 half is in-memory).</p>
 */
@Tag("testcontainers-it")
@DisplayName("Authorization purge (#192) vendor portability")
class AuthorizationPurgeVendorPortabilityTest {

	private static final Instant NOW = Instant.parse("2035-01-01T00:00:00Z");

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
			// H2's V1 seeds no clients, and since V11 (#344 review) an authorization needs one
			insertClient("h2-client", "h2_third_party");
			insertClient("h2-delete", "h2_delete_party");
		}

		private void insertClient(String id, String clientId) {
			jdbc.update("INSERT INTO oauth2_registered_client (id, client_id, client_name,"
					+ " client_authentication_methods, authorization_grant_types, scopes, client_settings,"
					+ " token_settings) VALUES (?, ?, ?, 'client_secret_basic', 'authorization_code', ?, '{}', '{}')",
					id, clientId, clientId, AuthorizationPurgeFixtures.SCOPES);
		}

		@Test
		@DisplayName("purges, audits with a JSON object, keeps live rows, and expires old purge audits")
		void purgesOnH2() {
			assertPurgeRoundTrip(jdbc, dataSource, false, "h2-client", "h2_third_party");
		}

		@Test
		@DisplayName("#344 review: deleting a client deletes its authorizations, consents and ESPI info (V11)")
		void deleteCascadesOnH2() {
			insertAuthorization(jdbc, false, "h2-delete-authz", "h2-delete", "customer-344");
			insertConsent(jdbc, "h2-delete", "customer-344");
			jdbc.update("INSERT INTO espi_application_info (client_id, client_name) VALUES (?, ?)",
					"h2_delete_party", "h2_delete_party");

			RegisteredClientAdminDao.Deleted deleted = new RegisteredClientAdminDao(jdbc).deleteById("h2-delete");

			assertThat(List.of(deleted.clients(), deleted.authorizations(),
					count("SELECT COUNT(*) FROM oauth2_registered_client WHERE id = 'h2-delete'"),
					count("SELECT COUNT(*) FROM oauth2_authorization WHERE registered_client_id = 'h2-delete'"),
					count("SELECT COUNT(*) FROM oauth2_authorization_consent WHERE registered_client_id = 'h2-delete'"),
					count("SELECT COUNT(*) FROM espi_application_info WHERE client_id = 'h2_delete_party'")))
					.containsExactly(1, 1, 0, 0, 0, 0);
		}

		@Test
		@DisplayName("#344 review: an administrator's audit event is written to the JSON column")
		void auditEventIsWrittenOnH2() {
			new AuditLogDao(jdbc).record("client_deleted", "h2-audit", "admin-1", true,
					Map.of("authorizations_deleted", 2));

			// FORMAT JSON stores a JSON object, not a JSON string holding the text
			assertThat(jdbc.queryForObject("SELECT additional_data FROM oauth2_audit_log"
					+ " WHERE client_id = 'h2-audit' AND principal_name = 'admin-1'", String.class))
					.isEqualTo("{\"authorizations_deleted\":2}");
		}

		private int count(String sql) {
			return jdbc.queryForObject(sql, Integer.class);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@DisplayName("PostgreSQL (db/vendor/postgresql migrations)")
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
			Flyway.configure().dataSource(dataSource).locations("classpath:db/vendor/postgresql").load().migrate();
			jdbc = new JdbcTemplate(dataSource);
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
