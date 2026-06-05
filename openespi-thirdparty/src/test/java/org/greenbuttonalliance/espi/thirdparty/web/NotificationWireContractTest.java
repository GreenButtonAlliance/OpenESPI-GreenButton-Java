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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.greenbuttonalliance.espi.common.dto.usage.BatchListDto;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.greenbuttonalliance.espi.common.xml.BatchListXmlCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer side of the DC→TP notification wire contract (#158).
 *
 * <p>Binds the Third Party's notification-parsing logic — {@link BatchListXmlCodec} (XML unmarshal,
 * as {@code NotificationController} does) and {@link EspiBatchUri} (id parsing of each resource URI)
 * — to the shared, ESPI-standard fixture in the repo-root {@code contracts/} directory. If the DC→TP
 * notification wire format drifts on either side, the producer test
 * ({@code NotificationServiceImplWireContractTest}) or this consumer test fails.</p>
 *
 * <p>Pure unit test (no Spring context, no Docker) — runs in CI as part of the TP module.</p>
 */
@DisplayName("DC→TP notification wire contract — TP consumer (#158)")
class NotificationWireContractTest {

	@Test
	@DisplayName("notification BatchList fixture conforms to the ESPI standard")
	void notificationConformsToStandard() throws Exception {
		String xml = Files.readString(locateContractsDir().resolve("notification-batchlist.xml"));

		BatchListDto batchList = BatchListXmlCodec.unmarshal(xml);

		assertThat(batchList.getResources())
				.singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
				.contains("/Batch/Subscription/503888")
				.contains("published-min=")
				.satisfies(uri ->
						assertThat(EspiBatchUri.subscriptionId(uri)).contains("503888"));
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
