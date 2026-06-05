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

package org.greenbuttonalliance.espi.common.service.impl;

import com.sun.net.httpserver.HttpServer;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.dto.usage.BatchListDto;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.greenbuttonalliance.espi.common.xml.BatchListXmlCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Producer side of the DC→TP notification wire contract (#158).
 *
 * <p>Drives the real {@link NotificationServiceImpl} sender against a minimal standalone
 * TP-receiver stub (a JDK {@link HttpServer} — no WireMock, no Spring, no Docker) and asserts
 * the bytes actually put on the wire conform to the ESPI {@code BatchList} notification contract:
 * the endpoint path, {@code application/atom+xml} content type, and a body that the canonical
 * {@link BatchListXmlCodec} parses back to the expected {@code Batch/Subscription} resource URI.</p>
 */
@DisplayName("DC→TP notification wire contract — DC producer + TP-receiver stub (#158)")
class NotificationServiceImplWireContractTest {

	private static final String DC_RESOURCE_BASE =
			"https://dc.example.com/DataCustodian/espi/1_1/resource";

	private HttpServer stub;
	private final AtomicReference<String> capturedMethod = new AtomicReference<>();
	private final AtomicReference<String> capturedPath = new AtomicReference<>();
	private final AtomicReference<String> capturedContentType = new AtomicReference<>();
	private final AtomicReference<String> capturedBody = new AtomicReference<>();
	private CountDownLatch received;

	@BeforeEach
	void startStub() throws Exception {
		received = new CountDownLatch(1);
		stub = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		stub.createContext("/ThirdParty/espi/1_1/Notification", exchange -> {
			capturedMethod.set(exchange.getRequestMethod());
			capturedPath.set(exchange.getRequestURI().getPath());
			capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
			capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
			received.countDown();
		});
		stub.start();
	}

	@AfterEach
	void stopStub() {
		if (stub != null) {
			stub.stop(0);
		}
	}

	@Test
	@DisplayName("notify() POSTs a conformant ESPI BatchList to the TP notify URI")
	void notifyPostsConformantBatchList() throws Exception {
		UUID subscriptionId = UUID.fromString("11111111-2222-5333-8444-555555555555");
		String notifyUri = "http://" + stub.getAddress().getHostString()
				+ ":" + stub.getAddress().getPort() + "/ThirdParty/espi/1_1/Notification";

		ApplicationInformationEntity appInfo = new ApplicationInformationEntity();
		appInfo.setThirdPartyNotifyUri(notifyUri);
		appInfo.setDataCustodianResourceEndpoint(DC_RESOURCE_BASE);

		SubscriptionEntity subscription = new SubscriptionEntity(subscriptionId);
		subscription.setApplicationInformation(appInfo);

		DatatypeFactory dtf = DatatypeFactory.newInstance();
		XMLGregorianCalendar start = dtf.newXMLGregorianCalendar("2025-01-01T00:00:00Z");
		XMLGregorianCalendar end = dtf.newXMLGregorianCalendar("2025-02-01T00:00:00Z");

		// repositories/services are unused by the subscription notify path
		NotificationServiceImpl service =
				new NotificationServiceImpl(RestClient.builder(), null, null, null);

		service.notify(subscription, start, end);

		assertThat(received.await(5, TimeUnit.SECONDS))
				.as("TP stub must have received the notification POST").isTrue();
		assertThat(capturedMethod.get()).isEqualTo("POST");
		assertThat(capturedPath.get()).isEqualTo("/ThirdParty/espi/1_1/Notification");
		assertThat(capturedContentType.get()).startsWith("application/atom+xml");

		BatchListDto sent = BatchListXmlCodec.unmarshal(capturedBody.get());
		assertThat(sent.getResources())
				.singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
				.contains("/Batch/Subscription/" + subscriptionId)
				.contains("published-min=2025-01-01T00:00:00Z")
				.contains("published-max=2025-02-01T00:00:00Z")
				.satisfies(uri ->
						assertThat(EspiBatchUri.subscriptionId(uri)).contains(subscriptionId.toString()));
	}
}
