/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.datacustodian.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DC <em>consumer</em> side of the AS↔DC token/introspection wire contract (#150 / #160).
 *
 * <p>Binds the Data Custodian's consumption logic — {@link EspiBatchUri} (id parsing, as the
 * resource-server / {@code ResourceValidationFilter} does) and {@link EspiScope} (FB-scope parsing,
 * as the introspector does) — to the shared, ESPI-standard fixtures in the repo-root
 * {@code contracts/} directory. If the wire format drifts from the standard on either side, the
 * producer test ({@code SubscriptionProvisioningServiceImplTest}) or this consumer test fails.</p>
 *
 * <p>Pure unit test (no Spring context, no Docker) — runs in CI as part of the DC module.</p>
 */
@DisplayName("AS↔DC token/introspection wire contract — DC consumer (#150/#160)")
class IntrospectionWireContractTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Path CONTRACTS = locateContractsDir();

	@Test
	@DisplayName("Subscription (auth-code) token response conforms to the ESPI standard")
	void subscriptionConformsToStandard() throws Exception {
		JsonNode c = readFixture("token-response-subscription.json");

		assertNoLegacyIdFields(c);
		assertFbGrammarOnlyScope(c.get("scope").asText(), 4, 5, 15);

		// Consumer parses ids out of the canonical Batch URIs (the contract).
		assertThat(EspiBatchUri.subscriptionId(c.get("resourceURI").asText())).contains("503888");
		assertThat(EspiBatchUri.authorizationId(c.get("authorizationURI").asText())).contains("503888");
		assertThat(EspiBatchUri.retailCustomerId(c.get("customerResourceURI").asText())).contains("503888");
	}

	@Test
	@DisplayName("Bulk (client-credentials) token response conforms to the ESPI standard")
	void bulkConformsToStandard() throws Exception {
		JsonNode c = readFixture("token-response-bulk.json");

		assertNoLegacyIdFields(c);
		assertFbGrammarOnlyScope(c.get("scope").asText(), 1, 3, 4, 5, 10, 11, 35);

		assertThat(EspiBatchUri.bulkId(c.get("resourceURI").asText())).contains("BULK_1");
		assertThat(EspiBatchUri.authorizationId(c.get("authorizationURI").asText())).contains("CLIENTCREDS");
		assertThat(EspiBatchUri.bulkId(c.get("customerResourceURI").asText())).contains("BULK_RC_1");
	}

	private static void assertNoLegacyIdFields(JsonNode c) {
		// ESPI 4.0 carries only the three *URI fields; the bare *_id claims were removed (#160).
		assertThat(c.has("authorization_id")).as("authorization_id must be absent").isFalse();
		assertThat(c.has("resource_subscription_id")).as("resource_subscription_id must be absent").isFalse();
		assertThat(c.has("customer_subscription_id")).as("customer_subscription_id must be absent").isFalse();
	}

	private static void assertFbGrammarOnlyScope(String scope, int... expectedFbs) {
		assertThat(scope).startsWith("FB=").doesNotContain("openid").doesNotContain("profile");
		EspiScope parsed = EspiScope.parse(scope);
		for (int fb : expectedFbs) {
			assertThat(parsed.containsFunctionBlock(fb)).as("scope must grant FB_" + fb).isTrue();
		}
	}

	private static JsonNode readFixture(String name) throws Exception {
		return MAPPER.readTree(Files.readString(CONTRACTS.resolve(name)));
	}

	/** Resolve repo-root {@code contracts/} whether tests run from the module dir or the repo root. */
	private static Path locateContractsDir() {
		for (Path candidate : new Path[] { Path.of("..", "contracts"), Path.of("contracts") }) {
			if (Files.isDirectory(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException(
				"contracts/ directory not found from " + Path.of("").toAbsolutePath());
	}
}
