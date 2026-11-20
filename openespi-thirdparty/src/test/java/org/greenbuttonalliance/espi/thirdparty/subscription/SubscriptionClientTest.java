package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class SubscriptionClientTest {

	private static final String URI = "https://dc.example/espi/1_1/resource/Batch/Subscription/abc";
	private static final FetchRange RANGE = new FetchRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-03T00:00:00Z"));

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final SubscriptionClient client = new SubscriptionClient(builder);

	@Test
	void sendsRangeBearerAndAccept() {
		server.expect(requestTo(URI + "?published-min=2026-01-01T00:00:00Z&published-max=2026-01-03T00:00:00Z"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer tok"))
				.andExpect(header("Accept", "application/atom+xml"))
				.andRespond(MockRestResponseCreators.withSuccess("<feed/>", MediaType.APPLICATION_ATOM_XML));

		SubscriptionClient.Response response = client.get(URI, "tok", RANGE);

		server.verify();
		assertThat(response).extracting(SubscriptionClient.Response::status, r -> new String(r.body(), StandardCharsets.UTF_8))
				.containsExactly(200, "<feed/>");
	}

	@Test
	void returnsNon200WithoutThrowing() {
		server.expect(requestTo(org.hamcrest.Matchers.startsWith(URI)))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN).body("denied"));

		SubscriptionClient.Response response = client.get(URI, "tok", RANGE);

		assertThat(response).extracting(SubscriptionClient.Response::status, r -> new String(r.body(), StandardCharsets.UTF_8))
				.containsExactly(403, "denied");
	}

	@Test
	void connectionFailureIsUnreachable() {
		server.expect(requestTo(org.hamcrest.Matchers.startsWith(URI)))
				.andRespond(request -> {
					throw new IOException("connection refused");
				});

		assertThatThrownBy(() -> client.get(URI, "secret-token", RANGE))
				.isInstanceOf(DataCustodianUnreachableException.class)
				.hasMessageNotContaining("secret-token");
	}
}
