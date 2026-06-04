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

package org.greenbuttonalliance.espi.common.uri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EspiBatchUri} — the single canonical ESPI Batch-URI builder/parser (#160).
 */
@DisplayName("EspiBatchUri (canonical ESPI Batch URI builder/parser) #160")
class EspiBatchUriTest {

	private static final String BASE = "https://utilityapi.com/DataCustodian/espi/1_1/resource";

	@Test
	@DisplayName("builders produce the ESPI 4.0 standard Batch forms")
	void buildersProduceStandardForms() {
		assertThat(EspiBatchUri.batchSubscription(BASE, "503888"))
				.isEqualTo(BASE + "/Batch/Subscription/503888");
		assertThat(EspiBatchUri.batchBulk(BASE, "BULK_1"))
				.isEqualTo(BASE + "/Batch/Bulk/BULK_1");
		assertThat(EspiBatchUri.batchRetailCustomer(BASE, "503888"))
				.isEqualTo(BASE + "/Batch/RetailCustomer/503888");
		assertThat(EspiBatchUri.authorization(BASE, "503888"))
				.isEqualTo(BASE + "/Authorization/503888");
	}

	@Test
	@DisplayName("a trailing slash on the base is normalized away")
	void normalizesTrailingSlash() {
		assertThat(EspiBatchUri.batchSubscription(BASE + "/", "1"))
				.isEqualTo(BASE + "/Batch/Subscription/1");
	}

	@Test
	@DisplayName("build then parse round-trips the id (UUID and alphanumeric)")
	void roundTrips() {
		UUID id = UUID.randomUUID();
		assertThat(EspiBatchUri.subscriptionId(EspiBatchUri.batchSubscription(BASE, id)))
				.contains(id.toString());
		assertThat(EspiBatchUri.bulkId(EspiBatchUri.batchBulk(BASE, "1dfa07c5740a_118328")))
				.contains("1dfa07c5740a_118328");
		assertThat(EspiBatchUri.retailCustomerId(EspiBatchUri.batchRetailCustomer(BASE, "503888")))
				.contains("503888");
		assertThat(EspiBatchUri.authorizationId(EspiBatchUri.authorization(BASE, "CLIENTCREDS")))
				.contains("CLIENTCREDS");
	}

	@Test
	@DisplayName("parser extracts the id from deeper paths and query strings")
	void parsesDeepPathsAndQueryStrings() {
		assertThat(EspiBatchUri.subscriptionId(BASE + "/Batch/Subscription/503888/UsagePoint/7"))
				.contains("503888");
		assertThat(EspiBatchUri.bulkId(BASE + "/Batch/Bulk/1?published-min=2012-04-01T04:00:00Z"))
				.contains("1");
	}

	@Test
	@DisplayName("parser returns empty for a non-matching or null URI")
	void parserEmptyWhenAbsent() {
		// the legacy (wrong) non-Batch form must NOT parse as a Batch subscription
		assertThat(EspiBatchUri.subscriptionId(BASE + "/Subscription/503888")).isEmpty();
		assertThat(EspiBatchUri.subscriptionId(null)).isEmpty();
		assertThat(EspiBatchUri.bulkId(BASE + "/Batch/Subscription/1")).isEmpty();
	}

	@Test
	@DisplayName("builders reject a blank base or id")
	void buildersRejectBlankInputs() {
		assertThatThrownBy(() -> EspiBatchUri.batchSubscription(" ", "1"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EspiBatchUri.batchSubscription(BASE, ""))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EspiBatchUri.authorization(BASE, null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
