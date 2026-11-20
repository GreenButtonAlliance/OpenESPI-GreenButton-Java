package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class SubscriptionClientTest {

	private static final String URI = "https://dc.example/espi/1_1/resource/Batch/Subscription/abc";
	private static final FetchRange RANGE = new FetchRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-03T00:00:00Z"));

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final SubscriptionClient client = new SubscriptionClient(builder, "https://dc.example", 10);

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
		server.expect(requestTo(startsWith(URI)))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN).body("denied"));

		SubscriptionClient.Response response = client.get(URI, "tok", RANGE);

		assertThat(response).extracting(SubscriptionClient.Response::status, r -> new String(r.body(), StandardCharsets.UTF_8))
				.containsExactly(403, "denied");
	}

	@Test
	void connectionFailureIsUnreachable() {
		server.expect(requestTo(startsWith(URI)))
				.andRespond(request -> {
					throw new IOException("connection refused");
				});

		assertThatThrownBy(() -> client.get(URI, "secret-token", RANGE))
				.isInstanceOf(DataCustodianUnreachableException.class)
				.hasMessageNotContaining("secret-token");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"https://other.example/espi/1_1/resource/Batch/Subscription/abc",
			"https://dc.example:8443/espi/1_1/resource/Batch/Subscription/abc",
			"http://dc.example/espi/1_1/resource/Batch/Subscription/abc",
			"https://dc.example/elsewhere/1_1/resource/Batch/Subscription/abc",
			"https://dc.example/espi/../other/Batch/Subscription/abc",
			"https://dc.example/espi/1_1/resource/UsagePoint/abc",
			"https://dc.example/espi/1_1/resource/Batch/Subscription/",
			"https://dc.example/espi/1_1/resource/Batch/Subscription/abc/../../RetailCustomer/5/UsagePoint",
			"https://dc.example/espi/1_1/resource/Batch/Subscription/abc/UsagePoint",
			"https://dc.example/espi/1_1/resource/Batch/Subscription/abc?published-min=2020-01-01T00:00:00Z",
			"https://dc.example/espi/1_1/resource/Batch/Subscription/abc#frag",
			"https://dc.example/espi/1_1/%2e%2e/x/Batch/Subscription/abc",
			"https://dc.example/espi/1_1/%2E%2E/x/Batch/Subscription/abc",
			"https://dc.example/espi/1_1/resource/Batch/Subscription/abc/%2e%2E/abc"})
	void refusesAUriNotOnTheConfiguredDataCustodian(String uri) {
		assertThatThrownBy(() -> client.get(uri, "secret-token", RANGE))
				.isInstanceOf(DataCustodianUnreachableException.class)
				.hasMessage("The authorization's resource URI is not on the configured Data Custodian.");
		server.verify();
	}

	@Test
	void refusesABodyOverTheLimit() {
		server.expect(requestTo(startsWith(URI)))
				.andRespond(MockRestResponseCreators.withSuccess("12345678901", MediaType.APPLICATION_ATOM_XML));

		assertThatThrownBy(() -> client.get(URI, "tok", RANGE))
				.isInstanceOf(DataCustodianUnreachableException.class)
				.hasMessage("The Data Custodian's response exceeded 10 bytes.");
	}

	@Test
	void theLimitIsNamedInMegabytesWithOneDecimalFromOneMebibyte() {
		assertThat(SubscriptionClient.describeLimit(512 * 1024 * 2)).isEqualTo("1.0 MB");
		assertThat(SubscriptionClient.describeLimit(67_108_864)).isEqualTo("64.0 MB");
		assertThat(SubscriptionClient.describeLimit(1024 * 1024 - 1)).isEqualTo("1048575 bytes");
		assertThat(SubscriptionClient.describeLimit(1_572_864)).isEqualTo("1.5 MB");
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1, Integer.MAX_VALUE})
	void aMaxBodyBytesOutsideOneToIntMaxMinusOneFailsConstruction(int limit) {
		assertThatThrownBy(() -> new SubscriptionClient(RestClient.builder(), "https://dc.example", limit))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("espi.thirdparty.subscription.max-body-bytes");
	}

	@Test
	void returnsARedirectWithoutFollowingIt() {
		server.expect(requestTo(startsWith(URI)))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.FOUND).location(java.net.URI.create("https://evil.example/x")));

		SubscriptionClient.Response response = client.get(URI, "tok", RANGE);

		server.verify();
		assertThat(response.status()).isEqualTo(302);
	}

	@Test
	void outsideLocalAndDevAnHttpResourceUriIsRefusedWithoutARequest() {
		RestClient.Builder httpBuilder = RestClient.builder();
		MockRestServiceServer httpServer = MockRestServiceServer.bindTo(httpBuilder).build();
		SubscriptionClient strict = new SubscriptionClient(httpBuilder, "https://dc.example", 10, true);

		assertThatThrownBy(() -> strict.get("http://dc.example/espi/1_1/resource/Batch/Subscription/abc", "secret-token", RANGE))
				.isInstanceOf(DataCustodianUnreachableException.class)
				.hasMessage("The authorization's resource URI is not on the configured Data Custodian.");
		httpServer.verify();
	}

	@Test
	void outsideLocalAndDevAnHttpBaseUrlFailsConstruction() {
		assertThatThrownBy(() -> new SubscriptionClient(RestClient.builder(), "http://dc.example/espi", 10, true))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("espi.datacustodian.base-url must use https");
	}

	@Test
	void outsideLocalAndDevAnHttpsResourceUriIsAccepted() {
		RestClient.Builder httpsBuilder = RestClient.builder();
		MockRestServiceServer httpsServer = MockRestServiceServer.bindTo(httpsBuilder).build();
		SubscriptionClient strict = new SubscriptionClient(httpsBuilder, "https://dc.example", 10, true);
		httpsServer.expect(requestTo(startsWith(URI))).andRespond(MockRestResponseCreators.withSuccess("<f/>", MediaType.APPLICATION_ATOM_XML));

		assertThat(strict.get(URI, "tok", RANGE).status()).isEqualTo(200);
	}

	@Test
	void onLocalAnHttpResourceUriIsAccepted() {
		RestClient.Builder localBuilder = RestClient.builder();
		MockRestServiceServer localServer = MockRestServiceServer.bindTo(localBuilder).build();
		SubscriptionClient local = new SubscriptionClient(localBuilder, "http://localhost:8081/DataCustodian", 10, false);
		String http = "http://localhost:8081/DataCustodian/espi/1_1/resource/Batch/Subscription/abc";
		localServer.expect(requestTo(startsWith(http))).andRespond(MockRestResponseCreators.withSuccess("<f/>", MediaType.APPLICATION_ATOM_XML));

		assertThat(local.get(http, "tok", RANGE).status()).isEqualTo(200);
	}

	@Test
	void profilesDecideWhetherHttpIsAllowed() throws Exception {
		MockEnvironment dev = new MockEnvironment();
		dev.setActiveProfiles("dev-mysql");
		MockEnvironment prod = new MockEnvironment();
		prod.setActiveProfiles("prod");
		HttpServer plain = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		plain.createContext("/DataCustodian", exchange -> {
			byte[] body = "<f/>".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		plain.start();
		try {
			String base = "http://127.0.0.1:" + plain.getAddress().getPort() + "/DataCustodian";

			SubscriptionClient onDev = new SubscriptionClient(RestClient.builder(), base, 10, Duration.ofSeconds(5),
					Duration.ofSeconds(5), dev);
			SubscriptionClient.Response response = onDev.get(base + "/espi/1_1/resource/Batch/Subscription/abc", "tok", RANGE);

			assertThat(response).extracting(SubscriptionClient.Response::status, r -> new String(r.body(), StandardCharsets.UTF_8))
					.containsExactly(200, "<f/>");
			assertThatThrownBy(() -> new SubscriptionClient(RestClient.builder(), base, 10, Duration.ofSeconds(5),
					Duration.ofSeconds(5), prod))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("espi.datacustodian.base-url");
		} finally {
			plain.stop(0);
		}
	}
}
