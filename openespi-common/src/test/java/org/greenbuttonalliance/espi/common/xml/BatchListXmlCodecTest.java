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

package org.greenbuttonalliance.espi.common.xml;

import org.greenbuttonalliance.espi.common.dto.usage.BatchListDto;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link BatchListXmlCodec} — the single canonical ESPI {@code BatchList}
 * wire codec (#158). Also binds the codec to the shared repo-root fixture
 * {@code contracts/notification-batchlist.xml} (the producer side of the DC→TP contract).
 */
@DisplayName("BatchListXmlCodec (canonical ESPI BatchList XML codec) #158")
class BatchListXmlCodecTest {

	private static final String BASE = "https://utilityapi.com/DataCustodian/espi/1_1/resource";

	@Test
	@DisplayName("marshals to the ESPI BatchList root element and namespace (prefix-agnostic)")
	void marshalProducesEspiBatchList() throws Exception {
		String uri = EspiBatchUri.batchSubscription(BASE, "503888");
		String xml = BatchListXmlCodec.marshal(new BatchListDto(List.of(uri)));

		// The XML prefix (espi: vs default) is insignificant; the contract is the qualified name.
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		Document doc = factory.newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("BatchList");
		assertThat(doc.getDocumentElement().getNamespaceURI()).isEqualTo("http://naesb.org/espi");
		assertThat(xml).contains(uri);
	}

	@Test
	@DisplayName("marshal then unmarshal round-trips the resource list")
	void roundTrips() {
		List<String> resources = List.of(
				EspiBatchUri.batchSubscription(BASE, "503888"),
				EspiBatchUri.batchBulk(BASE, "BULK_1"));

		BatchListDto parsed = BatchListXmlCodec.unmarshal(
				BatchListXmlCodec.marshal(new BatchListDto(resources)));

		assertThat(parsed.getResources()).containsExactlyElementsOf(resources);
	}

	@Test
	@DisplayName("parses the shared contract fixture and its canonical Batch/Subscription URI")
	void parsesContractFixture() throws Exception {
		String xml = Files.readString(locateContractsDir().resolve("notification-batchlist.xml"));

		BatchListDto parsed = BatchListXmlCodec.unmarshal(xml);

		assertThat(parsed.getResources())
				.singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
				.contains("/Batch/Subscription/503888")
				.contains("published-min=")
				.satisfies(uri ->
						assertThat(EspiBatchUri.subscriptionId(uri)).contains("503888"));
	}

	@Test
	@DisplayName("rejects null inputs")
	void rejectsNull() {
		assertThatThrownBy(() -> BatchListXmlCodec.marshal(null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> BatchListXmlCodec.unmarshal(null))
				.isInstanceOf(IllegalArgumentException.class);
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
