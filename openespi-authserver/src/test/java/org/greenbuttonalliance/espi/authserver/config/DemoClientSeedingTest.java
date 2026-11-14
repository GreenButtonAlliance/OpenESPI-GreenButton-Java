/*
 *
 *        Copyright (c) 2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.authserver.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The demo OAuth clients carry secrets written in this public repository, so a production AS must not
 * seed them: they are seeded only when {@code espi.authserver.seed.demo-clients=true} (local/dev/test
 * profiles). Infrastructure clients are provisioned from configured secrets, stored hashed.
 */
@DisplayName("Demo-client seeding is opt-in; infrastructure clients come from configured secrets")
class DemoClientSeedingTest {

	private static final String[] DEMO_CLIENT_IDS =
			{"data_custodian_admin", "third_party", "third_party_admin", "datacustodian"};

	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	private EmbeddedDatabase database;

	private AuthorizationServerConfig config;

	@BeforeEach
	void setUp() {
		database = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.setName("demo-seed-" + UUID.randomUUID())
				.addScript("org/springframework/security/oauth2/server/authorization/client/oauth2-registered-client-schema.sql")
				.build();
		config = new AuthorizationServerConfig();
		ReflectionTestUtils.setField(config, "defaultRedirectUriBase", "http://localhost");
	}

	@AfterEach
	void tearDown() {
		database.shutdown();
	}

	private RegisteredClientRepository repository() {
		return config.registeredClientRepository(new JdbcTemplate(database), passwordEncoder);
	}

	@Test
	@DisplayName("by default (production) none of the demo clients is seeded")
	void demoClientsNotSeededByDefault() {
		RegisteredClientRepository repository = repository();

		assertThat(DEMO_CLIENT_IDS).allSatisfy(clientId -> assertThat(repository.findByClientId(clientId))
				.as("demo client %s must not exist on a default (production) start", clientId)
				.isNull());
	}

	@Test
	@DisplayName("with seed.demo-clients=true (local/dev/test) every demo client is seeded")
	void demoClientsSeededWhenEnabled() {
		ReflectionTestUtils.setField(config, "seedDemoClients", true);

		RegisteredClientRepository repository = repository();

		assertThat(DEMO_CLIENT_IDS).allSatisfy(clientId -> assertThat(repository.findByClientId(clientId))
				.as("demo client %s", clientId)
				.isNotNull());
	}

	@Test
	@DisplayName("a configured secret provisions the DC clients with that secret, hashed, not the demo one")
	void configuredSecretsProvisionInfrastructureClients() {
		ReflectionTestUtils.setField(config, "dataCustodianClientSecret", "introspect-from-vault");
		ReflectionTestUtils.setField(config, "dataCustodianAdminClientSecret", "admin-from-vault");

		RegisteredClientRepository repository = repository();

		assertThat(repository.findByClientId("datacustodian"))
				.isNotNull()
				.satisfies(client -> assertThat(client.getClientSecret()).doesNotStartWith("{noop}"))
				.satisfies(client -> assertThat(passwordEncoder.matches("introspect-from-vault", client.getClientSecret())).isTrue())
				.satisfies(client -> assertThat(passwordEncoder.matches("datacustodian-secret", client.getClientSecret())).isFalse());
		assertThat(repository.findByClientId("data_custodian_admin"))
				.isNotNull()
				.satisfies(client -> assertThat(passwordEncoder.matches("admin-from-vault", client.getClientSecret())).isTrue())
				.satisfies(client -> assertThat(client.getScopes()).containsExactly("DataCustodian_Admin_Access"));
		assertThat(repository.findByClientId("third_party")).isNull();
		assertThat(repository.findByClientId("third_party_admin")).isNull();
	}

	@Test
	@DisplayName("demo rows from an earlier release stop accepting their published secrets once seeding is off")
	void existingDemoRowsAreRetired() {
		ReflectionTestUtils.setField(config, "seedDemoClients", true);
		repository();

		ReflectionTestUtils.setField(config, "seedDemoClients", false);
		RegisteredClientRepository repository = repository();

		assertThat(repository.findByClientId("data_custodian_admin"))
				.satisfies(client -> assertThat(passwordEncoder.matches("dc-secret", client.getClientSecret())).isFalse());
		assertThat(repository.findByClientId("third_party"))
				.satisfies(client -> assertThat(passwordEncoder.matches("tp-secret", client.getClientSecret())).isFalse());
		assertThat(repository.findByClientId("third_party_admin"))
				.satisfies(client -> assertThat(passwordEncoder.matches("tpadmin-secret", client.getClientSecret())).isFalse());
		assertThat(repository.findByClientId("datacustodian"))
				.satisfies(client -> assertThat(passwordEncoder.matches("datacustodian-secret", client.getClientSecret())).isFalse());
	}

	@Test
	@DisplayName("a configured secret replaces a well-known demo secret left from an earlier start")
	void configuredSecretRetiresDemoSecret() {
		ReflectionTestUtils.setField(config, "seedDemoClients", true);
		RegisteredClient demo = repository().findByClientId("data_custodian_admin");

		ReflectionTestUtils.setField(config, "seedDemoClients", false);
		ReflectionTestUtils.setField(config, "dataCustodianAdminClientSecret", "admin-from-vault");
		RegisteredClient rotated = repository().findByClientId("data_custodian_admin");

		assertThat(rotated)
				.isNotNull()
				.satisfies(client -> assertThat(client.getId()).isEqualTo(demo.getId()))
				.satisfies(client -> assertThat(passwordEncoder.matches("dc-secret", client.getClientSecret())).isFalse())
				.satisfies(client -> assertThat(passwordEncoder.matches("admin-from-vault", client.getClientSecret())).isTrue());
	}
}
