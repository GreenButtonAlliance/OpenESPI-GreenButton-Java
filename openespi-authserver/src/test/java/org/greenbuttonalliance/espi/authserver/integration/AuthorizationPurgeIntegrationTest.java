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

import org.greenbuttonalliance.espi.authserver.config.AuthorizationPurgeConfig;
import org.greenbuttonalliance.espi.authserver.config.AuthorizationPurgeProperties;
import org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.PurgeAudit;
import org.greenbuttonalliance.espi.authserver.service.AuthorizationPurgeService;
import org.greenbuttonalliance.espi.authserver.service.AuthorizationPurgeService.PurgeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.authorizationIds;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.auditTimestamps;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.insertAudit;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.insertAuthorization;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.insertConsent;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.purgeAudits;
import static org.greenbuttonalliance.espi.authserver.integration.AuthorizationPurgeFixtures.token;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for the scheduled {@code oauth2_authorization} purge (#192) against the AS's
 * real MySQL schema (Testcontainers, inherited from {@link AbstractAuthserverIntegrationTest}).
 *
 * <p>The purge service under test is built from the context's {@link JdbcTemplate} and transaction
 * manager with a fixed {@link Clock}. {@code NOW} is set years ahead of the wall clock (but inside
 * MySQL's TIMESTAMP range), so the context's own scheduled job, which uses the system clock, can
 * never purge rows these tests create.</p>
 */
@DisplayName("Authorization purge (#192) against MySQL")
class AuthorizationPurgeIntegrationTest extends AbstractAuthserverIntegrationTest {

	private static final Instant NOW = Instant.parse("2035-01-01T00:00:00Z");
	private static final Duration GRACE = Duration.ofDays(7);
	private static final String PRINCIPAL = "customer-192";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private AuthorizationPurgeProperties contextProperties;

	@Autowired
	private AuthorizationPurgeService contextPurgeService;

	private String thirdPartyRegisteredId;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.update("DELETE FROM oauth2_authorization");
		jdbcTemplate.update("DELETE FROM oauth2_audit_log WHERE event_type IN (?, ?)",
				AuthorizationPurgeService.PURGE_EVENT_TYPE, "RETENTION_TEST_OTHER");
		jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE principal_name = ?", PRINCIPAL);
		thirdPartyRegisteredId = jdbcTemplate.queryForObject(
				"SELECT id FROM oauth2_registered_client WHERE client_id = ?", String.class, "third_party");
	}

	@Nested
	@DisplayName("Purge eligibility")
	class Eligibility {

		@Test
		@DisplayName("fully-expired row past the grace period is deleted and audited")
		void fullyExpiredRowIsDeletedAndAudited() {
			insert("expired", token("authorization_code", NOW.minus(Duration.ofDays(40)), NOW.minus(Duration.ofDays(40)).plusSeconds(300)),
					token("access_token", NOW.minus(Duration.ofDays(20)), NOW.minus(Duration.ofDays(20)).plus(Duration.ofHours(6))),
					token("refresh_token", NOW.minus(Duration.ofDays(40)), NOW.minus(Duration.ofDays(8))));

			PurgeResult result = purger(500).purge();

			assertThat(result).isEqualTo(new PurgeResult(1, 0));
			assertThat(authorizationIds(jdbcTemplate)).isEmpty();
			assertThat(purgeAudits(jdbcTemplate)).singleElement()
					.extracting(PurgeAudit::clientId, PurgeAudit::principalName, PurgeAudit::success,
							PurgeAudit::grantType, PurgeAudit::scopesGranted, PurgeAudit::authorizationId,
							audit -> audit.data("registered_client_id"),
							audit -> audit.data("first_issued_at"),
							audit -> audit.data("last_issued_at"),
							audit -> audit.data("latest_expires_at"),
							audit -> audit.data("grace_period"))
					.containsExactly("third_party", PRINCIPAL, true, AuthorizationPurgeFixtures.GRANT_TYPE,
							AuthorizationPurgeFixtures.SCOPES, "expired",
							thirdPartyRegisteredId,
							NOW.minus(Duration.ofDays(40)).toString(),
							NOW.minus(Duration.ofDays(20)).toString(),
							NOW.minus(Duration.ofDays(8)).toString(),
							"PT168H");
		}

		@Test
		@DisplayName("row whose refresh token is still valid is kept, even with a long-expired access token")
		void rowWithValidRefreshTokenIsKept() {
			insert("refreshable", token("access_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(30)).plus(Duration.ofHours(6))),
					token("refresh_token", NOW.minus(Duration.ofDays(30)), NOW.plus(Duration.ofHours(1))));

			assertThat(purger(500).purge()).isEqualTo(new PurgeResult(0, 0));
			assertThat(authorizationIds(jdbcTemplate)).containsExactly("refreshable");
			assertThat(purgeAudits(jdbcTemplate)).isEmpty();
		}

		@Test
		@DisplayName("row whose latest expiry is inside the grace period is kept; just past it is purged")
		void rowInsideGracePeriodIsKept() {
			insert("in-grace", token("access_token", NOW.minus(Duration.ofDays(3)), NOW.minus(Duration.ofDays(2))),
					token("refresh_token", NOW.minus(Duration.ofDays(3)), NOW.minus(GRACE).plusSeconds(1)));
			insert("past-grace", token("access_token", NOW.minus(Duration.ofDays(9)), NOW.minus(GRACE).minusSeconds(1)));

			assertThat(purger(500).purge()).isEqualTo(new PurgeResult(1, 0));
			assertThat(authorizationIds(jdbcTemplate)).containsExactly("in-grace");
			assertThat(purgeAudits(jdbcTemplate)).extracting(PurgeAudit::authorizationId).containsExactly("past-grace");
		}

		@Test
		@DisplayName("row with no expiry at all (abandoned authorization request) is kept")
		void rowWithNoExpiryIsKept() {
			insert("state-only");

			assertThat(purger(500).purge()).isEqualTo(new PurgeResult(0, 0));
			assertThat(authorizationIds(jdbcTemplate)).containsExactly("state-only");
		}

		@Test
		@DisplayName("row holding a token value without an expiry is kept, even if its other tokens expired")
		void rowWithUnboundedTokenIsKept() {
			insert("unbounded", token("access_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(29))),
					token("refresh_token", NOW.minus(Duration.ofDays(30)), null));

			assertThat(purger(500).purge()).isEqualTo(new PurgeResult(0, 0));
			assertThat(authorizationIds(jdbcTemplate)).containsExactly("unbounded");
		}

		@Test
		@DisplayName("OIDC and device-code expiries count toward eligibility")
		void oidcAndDeviceExpiriesAreConsidered() {
			insert("oidc-valid", token("access_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(29))),
					token("oidc_id_token", NOW.minus(Duration.ofDays(30)), NOW.plus(Duration.ofDays(1))));
			insert("device-expired", token("device_code", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(29))),
					token("user_code", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(29))));

			assertThat(purger(500).purge()).isEqualTo(new PurgeResult(1, 0));
			assertThat(authorizationIds(jdbcTemplate)).containsExactly("oidc-valid");
		}

		@Test
		@DisplayName("consent rows and registered clients are never touched")
		void consentAndRegisteredClientsUntouched() {
			insertConsent(jdbcTemplate, thirdPartyRegisteredId, PRINCIPAL);
			insert("expired", token("access_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(29))));
			Integer clientsBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth2_registered_client", Integer.class);

			assertThat(purger(500).purge()).isEqualTo(new PurgeResult(1, 0));
			assertThat(jdbcTemplate.queryForList(
					"SELECT authorities FROM oauth2_authorization_consent WHERE registered_client_id = ? AND principal_name = ?",
					String.class, thirdPartyRegisteredId, PRINCIPAL))
					.containsExactly("SCOPE_" + AuthorizationPurgeFixtures.SCOPES);
			assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth2_registered_client", Integer.class))
					.isEqualTo(clientsBefore);
		}
	}

	@Nested
	@DisplayName("Chunking and concurrency")
	class Chunking {

		@Test
		@DisplayName("works through more rows than one batch, auditing each exactly once; a re-run is a no-op")
		void processesMultipleChunks() {
			IntStream.range(0, 7).forEach(i -> insertExpired("chunk-" + i));
			insert("kept", token("refresh_token", NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1))));

			AuthorizationPurgeService purger = purger(3);

			assertThat(purger.purge()).isEqualTo(new PurgeResult(7, 0));
			assertThat(purger.purge()).isEqualTo(new PurgeResult(0, 0));
			assertThat(authorizationIds(jdbcTemplate)).containsExactly("kept");
			assertThat(purgeAudits(jdbcTemplate)).extracting(PurgeAudit::authorizationId)
					.containsExactlyInAnyOrder(IntStream.range(0, 7).mapToObj(i -> "chunk-" + i).toArray(String[]::new));
		}

		@Test
		@DisplayName("two instances purging concurrently never double-audit")
		void concurrentInstancesNeverDoubleAudit() throws Exception {
			int rows = 60;
			IntStream.range(0, rows).forEach(i -> insertExpired("race-%03d".formatted(i)));
			AuthorizationPurgeService first = purger(10);
			AuthorizationPurgeService second = purger(10);
			CountDownLatch start = new CountDownLatch(1);
			ExecutorService executor = Executors.newFixedThreadPool(2);
			try {
				Callable<PurgeResult> runFirst = () -> {
					start.await();
					return first.purge();
				};
				Callable<PurgeResult> runSecond = () -> {
					start.await();
					return second.purge();
				};
				Future<PurgeResult> a = executor.submit(runFirst);
				Future<PurgeResult> b = executor.submit(runSecond);
				start.countDown();
				int purgedByRaces = a.get().authorizationsPurged() + b.get().authorizationsPurged();
				// One instance may stop early when its whole chunk lost the race; a follow-up run finishes.
				int purgedAfter = first.purge().authorizationsPurged();

				assertThat(purgedByRaces + purgedAfter).isEqualTo(rows);
			} finally {
				executor.shutdownNow();
			}
			assertThat(authorizationIds(jdbcTemplate)).isEmpty();
			assertThat(purgeAudits(jdbcTemplate)).extracting(PurgeAudit::authorizationId)
					.hasSize(rows)
					.doesNotHaveDuplicates();
		}
	}

	@Nested
	@DisplayName("Audit retention")
	class AuditRetention {

		@Test
		@DisplayName("deletes only purge-audit rows older than the retention period")
		void deletesOnlyOldPurgeAuditRows() {
			Instant old = NOW.minus(Duration.ofDays(400));
			Instant recent = NOW.minus(Duration.ofDays(10));
			insertAudit(jdbcTemplate, AuthorizationPurgeService.PURGE_EVENT_TYPE, "third_party", old);
			insertAudit(jdbcTemplate, AuthorizationPurgeService.PURGE_EVENT_TYPE, "third_party", recent);
			insertAudit(jdbcTemplate, "RETENTION_TEST_OTHER", "third_party", NOW.minus(Duration.ofDays(1000)));

			assertThat(purger(500).purge()).isEqualTo(new PurgeResult(0, 1));
			assertThat(auditTimestamps(jdbcTemplate, AuthorizationPurgeService.PURGE_EVENT_TYPE)).containsExactly(recent);
			assertThat(auditTimestamps(jdbcTemplate, "RETENTION_TEST_OTHER")).hasSize(1);
		}
	}

	@Nested
	@DisplayName("Configuration")
	class ConfigurationTests {

		private final ApplicationContextRunner propertiesRunner = new ApplicationContextRunner()
				.withUserConfiguration(PropertiesOnly.class);

		private final ApplicationContextRunner jobRunner = new ApplicationContextRunner()
				.withUserConfiguration(AuthorizationPurgeConfig.class)
				.withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
				.withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class));

		@Test
		@DisplayName("the application context binds the documented defaults and registers the job")
		void contextUsesDefaults() {
			assertThat(contextProperties).isEqualTo(new AuthorizationPurgeProperties(true, Duration.ofHours(1),
					Duration.ofMinutes(5), Duration.ofDays(7), 500, Duration.ofDays(395)));
			assertThat(contextPurgeService).isNotNull();
		}

		@Test
		@DisplayName("custom values bind from espi.authserver.authorization-purge.*")
		void customValuesBind() {
			propertiesRunner.withPropertyValues(
							"espi.authserver.authorization-purge.interval=15m",
							"espi.authserver.authorization-purge.initial-delay=0s",
							"espi.authserver.authorization-purge.grace-period=0d",
							"espi.authserver.authorization-purge.batch-size=50",
							"espi.authserver.authorization-purge.audit-retention=2555d")
					.run(context -> assertThat(context).hasNotFailed()
							.getBean(AuthorizationPurgeProperties.class)
							.isEqualTo(new AuthorizationPurgeProperties(true, Duration.ofMinutes(15), Duration.ZERO,
									Duration.ZERO, 50, Duration.ofDays(2555))));
		}

		@Test
		@DisplayName("invalid values fail startup")
		void invalidValuesFailStartup() {
			List.of("espi.authserver.authorization-purge.batch-size=0",
							"espi.authserver.authorization-purge.grace-period=-1d",
							"espi.authserver.authorization-purge.audit-retention=-1d",
							"espi.authserver.authorization-purge.initial-delay=-1s",
							"espi.authserver.authorization-purge.interval=0s")
					.forEach(property -> propertiesRunner.withPropertyValues(property)
							.run(context -> assertThat(context).hasFailed()
									.getFailure().rootCause()
									.isInstanceOf(IllegalArgumentException.class)
									.hasMessageContaining("espi.authserver.authorization-purge.")));
		}

		@Test
		@DisplayName("enabled=false removes the job; it is on by default")
		void enabledFlagControlsJob() {
			jobRunner.run(context -> assertThat(context).hasSingleBean(AuthorizationPurgeService.class));
			jobRunner.withPropertyValues("espi.authserver.authorization-purge.enabled=false")
					.run(context -> assertThat(context).doesNotHaveBean(AuthorizationPurgeService.class)
							.doesNotHaveBean(AuthorizationPurgeConfig.class));
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AuthorizationPurgeProperties.class)
	static class PropertiesOnly {
	}

	private AuthorizationPurgeService purger(int batchSize) {
		AuthorizationPurgeProperties properties = new AuthorizationPurgeProperties(true, Duration.ofHours(1),
				Duration.ofMinutes(5), GRACE, batchSize, Duration.ofDays(395));
		return new AuthorizationPurgeService(jdbcTemplate, new TransactionTemplate(transactionManager), properties,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private void insert(String id, AuthorizationPurgeFixtures.Token... tokens) {
		insertAuthorization(jdbcTemplate, false, id, thirdPartyRegisteredId, PRINCIPAL, tokens);
	}

	private void insertExpired(String id) {
		insert(id, token("access_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(29))),
				token("refresh_token", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(10))));
	}
}
