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

package org.greenbuttonalliance.espi.datacustodian.web.internal.backchannel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.greenbuttonalliance.espi.datacustodian.web.internal.backchannel.dto.SubscriptionProvisionRequest;
import org.greenbuttonalliance.espi.datacustodian.web.internal.backchannel.dto.SubscriptionProvisionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DC <em>provider</em> side of the AS→DC back-channel wire contract (Contract 1 of #150).
 *
 * <p>The Data Custodian <em>consumes</em> the {@code POST /internal/backchannel/v1/subscriptions}
 * request and <em>produces</em> the 201 response. This test binds DC's DTOs
 * ({@link SubscriptionProvisionRequest} / {@link SubscriptionProvisionResponse}) to the shared,
 * repo-root {@code contracts/} fixtures, and binds the response's canonical URIs to the single
 * source of truth ({@link EspiBatchUri}) and the scope to {@link EspiScope}. If the wire format
 * drifts on either side, this provider test or the AS consumer test
 * ({@code DataCustodianBackchannelClientTest}) fails.</p>
 *
 * <p>The companion introspection / token-response wire contract (Contract 2 of #150) is pinned
 * separately by {@code IntrospectionWireContractTest} (#160).</p>
 *
 * <p>Pure unit test (no Spring context, no Docker) — runs in CI as part of the DC module.</p>
 */
@DisplayName("AS↔DC back-channel wire contract — DC provider (#150)")
class BackchannelWireContractTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Path CONTRACTS = locateContractsDir();
	private static final String RESOURCE_BASE =
			"https://utilityapi.com/DataCustodian/espi/1_1/resource";

	private static final UUID AUTH_ID = UUID.fromString("11111111-1111-5111-8111-111111111111");
	private static final UUID RES_SUB_ID = UUID.fromString("22222222-2222-5222-8222-222222222222");
	private static final UUID CUST_SUB_ID = UUID.fromString("33333333-3333-5333-8333-333333333333");

	@Test
	@DisplayName("DC consumes the request fixture into SubscriptionProvisionRequest")
	void consumesRequest() throws Exception {
		SubscriptionProvisionRequest request =
				MAPPER.readValue(readFixture("backchannel-subscription-request.json"),
						SubscriptionProvisionRequest.class);

		assertThat(request.correlationId()).isEqualTo("corr-7f3a1c20");
		assertThat(request.clientId()).isEqualTo("third_party");
		assertThat(request.retailCustomerId()).isEqualTo(42L);
		assertThat(request.selectedUsagePointIds())
				.containsExactly(UUID.fromString("00000000-0000-5000-8000-000000000001"));

		// granted_scope must be the ESPI FB grammar (parseable by the shared parser), not legacy.
		assertThat(request.grantedScope()).startsWith("FB=");
		EspiScope scope = EspiScope.parse(request.grantedScope());
		assertThat(scope.containsFunctionBlock(4)).isTrue();
		assertThat(scope.containsFunctionBlock(5)).isTrue();
		assertThat(scope.containsFunctionBlock(15)).isTrue();
	}

	@Test
	@DisplayName("the response fixture's canonical URIs round-trip through EspiBatchUri")
	void responseFixtureUrisAreCanonical() throws Exception {
		SubscriptionProvisionResponse response =
				MAPPER.readValue(readFixture("backchannel-subscription-response.json"),
						SubscriptionProvisionResponse.class);

		assertThat(response.authorizationId()).isEqualTo(AUTH_ID);
		assertThat(response.resourceSubscriptionId()).isEqualTo(RES_SUB_ID);
		assertThat(response.customerSubscriptionId()).isEqualTo(CUST_SUB_ID);

		// resource_uri carries the resource_subscription_id; authorization_uri the authorization_id;
		// customer_resource_uri the retail_customer_id (Long) — per the DC producer (#160).
		assertThat(EspiBatchUri.subscriptionId(response.resourceUri())).contains(RES_SUB_ID.toString());
		assertThat(EspiBatchUri.authorizationId(response.authorizationUri())).contains(AUTH_ID.toString());
		assertThat(EspiBatchUri.retailCustomerId(response.customerResourceUri())).contains("42");
	}

	@Test
	@DisplayName("DC produces a response whose serialized shape equals the fixture")
	void producesResponseMatchingFixture() throws Exception {
		// Build the response exactly as the DC producer does (URIs via EspiBatchUri), then assert it
		// serializes to the shared fixture — so the producer cannot drift from the contract.
		SubscriptionProvisionResponse produced = new SubscriptionProvisionResponse(
				AUTH_ID,
				RES_SUB_ID,
				CUST_SUB_ID,
				EspiBatchUri.batchSubscription(RESOURCE_BASE, RES_SUB_ID),
				EspiBatchUri.authorization(RESOURCE_BASE, AUTH_ID),
				EspiBatchUri.batchRetailCustomer(RESOURCE_BASE, "42"));

		JsonNode producedTree = MAPPER.valueToTree(produced);
		JsonNode fixtureTree = MAPPER.readTree(readFixture("backchannel-subscription-response.json"));

		assertThat(producedTree).isEqualTo(fixtureTree);
	}

	private static String readFixture(String name) throws Exception {
		return Files.readString(CONTRACTS.resolve(name));
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
