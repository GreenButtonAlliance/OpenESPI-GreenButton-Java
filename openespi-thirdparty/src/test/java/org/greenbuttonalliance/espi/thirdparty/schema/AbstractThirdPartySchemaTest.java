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
import org.flywaydb.core.api.MigrationVersion;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.thirdparty.subscription.FeedValidation;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionFeedFetchEntity;
import org.greenbuttonalliance.espi.thirdparty.subscription.SubscriptionFeedFetchRepository;
import org.greenbuttonalliance.espi.thirdparty.subscription.XmlValidationMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The Third Party's schema is owned by Flyway, using the openespi-common scripts, and Hibernate only
 * validates it (#267). Each subclass starts the application on one real Third Party profile and
 * database. The application context failing to start means the entities and the scripts disagree
 * ({@code ddl-auto: validate}).
 */
abstract class AbstractThirdPartySchemaTest {

	/**
	 * The openespi-common migrations: V1 and V3 shared, V2 vendor-specific, V5 and V6 (the TP's
	 * authorization client registration id, #146) shared. V4 is the Data
	 * Custodian's handoff-nonce table from openespi-handoff, which the Third Party does not use; the
	 * gap is expected. V7 is the TP-only {@code subscription_feed_fetches} table (#89), in
	 * {@code db/thirdparty/<vendor>}.
	 */
	static final String[] EXPECTED_VERSIONS = {"1", "2", "3", "5", "6", "7"};

	@Autowired
	private Flyway flyway;

	@Autowired
	protected AuthorizationRepository authorizationRepository;

	@Autowired
	protected SubscriptionFeedFetchRepository feedFetchRepository;

	@Autowired
	protected Environment environment;

	@Autowired
	protected RetailCustomerRepository retailCustomerRepository;

	@Test
	@DisplayName("Flyway applied the openespi-common migrations, and Hibernate only validates the schema")
	void flyway_owns_the_schema() {
		String[] applied = Arrays.stream(flyway.info().applied())
				.map(MigrationInfo::getVersion)
				.filter(Objects::nonNull)
				.map(MigrationVersion::getVersion)
				.toArray(String[]::new);

		assertThat(applied).containsExactly(EXPECTED_VERSIONS);
		assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
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
		RetailCustomerEntity saved = retailCustomerRepository.save(account);
		try {
			assertThat(retailCustomerRepository.findByUsername(username))
					.hasValueSatisfying(found -> assertThat(found)
							.extracting(RetailCustomerEntity::getRole, RetailCustomerEntity::getEnabled)
							.containsExactly(RetailCustomerEntity.ROLE_USER, true));
		} finally {
			retailCustomerRepository.delete(saved);
		}
	}

	@Test
	@DisplayName("saved Subscription feeds (subscription_feed_fetches) round-trip a 100 000 character body, newest first, and go with their authorization")
	void subscription_feed_fetches_round_trip() {
		String username = "feeds-" + UUID.randomUUID().toString().substring(0, 8);
		RetailCustomerEntity account = new RetailCustomerEntity(username, "Schema", "Test",
				username + "@example.local", RetailCustomerEntity.ROLE_USER);
		account.setPassword("$2a$10$abcdefghijklmnopqrstuuJ0123456789abcdefghijklmnopqrstu");
		account.setEnabled(Boolean.TRUE);
		account.setAccountLocked(Boolean.FALSE);
		RetailCustomerEntity customer = retailCustomerRepository.save(account);
		try {
			AuthorizationEntity authorization = new AuthorizationEntity(customer, null, "FB=4_5_13_14");
			UUID authorizationId = UUID.randomUUID();
			authorization.setId(authorizationId);
			authorizationRepository.save(authorization);

			Instant first = Instant.parse("2026-11-20T10:00:00Z");
			String longBody = "x".repeat(100_000);
			String uri = "https://dc.example.local/DataCustodian/espi/1_1/resource/Batch/Subscription/1";
			feedFetchRepository.save(new SubscriptionFeedFetchEntity(authorizationId, first, uri,
					Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"), 200, longBody,
					new FeedValidation(XmlValidationMode.REPORT, Boolean.TRUE, 0, List.of())));
			feedFetchRepository.save(new SubscriptionFeedFetchEntity(authorizationId, first.plusSeconds(1), uri,
					null, null, 403, "forbidden",
					new FeedValidation(XmlValidationMode.OFF, null, 0, List.of())));

			assertThat(feedFetchRepository.findAllByAuthorizationIdOrderByFetchedAtDesc(authorizationId))
					.extracting(SubscriptionFeedFetchEntity::getHttpStatus,
							f -> f.getBody().length(),
							SubscriptionFeedFetchEntity::getValidationMode,
							SubscriptionFeedFetchEntity::getSchemaValid)
					.containsExactly(
							tuple(403, "forbidden".length(), XmlValidationMode.OFF, null),
							tuple(200, 100_000, XmlValidationMode.REPORT, Boolean.TRUE));

			authorizationRepository.deleteById(authorizationId);

			assertThat(feedFetchRepository.findAllByAuthorizationIdOrderByFetchedAtDesc(authorizationId)).isEmpty();
		} finally {
			retailCustomerRepository.delete(customer);
		}
	}
}
