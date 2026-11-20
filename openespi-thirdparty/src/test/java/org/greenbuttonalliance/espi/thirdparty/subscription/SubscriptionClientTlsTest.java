package org.greenbuttonalliance.espi.thirdparty.subscription;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.client.RestClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The outbound connection speaks TLS 1.3 only (#298), against a real local HTTPS server. */
class SubscriptionClientTlsTest {

	private static final FetchRange RANGE = new FetchRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-03T00:00:00Z"));
	private static final char[] PASSWORD = "changeit".toCharArray();

	@TempDir
	static Path dir;

	private static SSLContext context;
	private HttpsServer server;

	@BeforeAll
	static void generateSelfSignedCertificate() throws Exception {
		Path keystore = dir.resolve("test.p12");
		String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
		Process process = new ProcessBuilder(keytool, "-genkeypair", "-alias", "test", "-keyalg", "RSA", "-keysize", "2048",
				"-validity", "2", "-dname", "CN=localhost", "-ext", "san=dns:localhost,ip:127.0.0.1",
				"-storetype", "PKCS12", "-keystore", keystore.toString(), "-storepass", new String(PASSWORD))
				.redirectErrorStream(true).start();
		process.getInputStream().readAllBytes();
		assertThat(process.waitFor()).isZero();

		KeyStore store = KeyStore.getInstance("PKCS12");
		try (InputStream in = Files.newInputStream(keystore)) {
			store.load(in, PASSWORD);
		}
		KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keys.init(store, PASSWORD);
		TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trust.init(store);
		context = SSLContext.getInstance("TLS");
		context.init(keys.getKeyManagers(), trust.getTrustManagers(), null);
	}

	private String start(String protocol) throws Exception {
		server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.setHttpsConfigurator(new HttpsConfigurator(context) {
			@Override
			public void configure(HttpsParameters params) {
				SSLParameters parameters = context.getDefaultSSLParameters();
				parameters.setProtocols(new String[]{protocol});
				params.setSSLParameters(parameters);
			}
		});
		server.createContext("/espi", exchange -> {
			byte[] body = "<feed/>".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		return "https://localhost:" + server.getAddress().getPort() + "/espi";
	}

	private SubscriptionClient client(String base) {
		return new SubscriptionClient(RestClient.builder(), base, 1024, Duration.ofSeconds(5), Duration.ofSeconds(5), true, context);
	}

	@AfterEach
	void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void succeedsAgainstATls13Server() throws Exception {
		String base = start("TLSv1.3");

		SubscriptionClient.Response response = client(base).get(base + "/1_1/resource/Batch/Subscription/abc", "tok", RANGE);

		assertThat(response).extracting(SubscriptionClient.Response::status, r -> new String(r.body(), StandardCharsets.UTF_8))
				.containsExactly(200, "<feed/>");
	}

	@Test
	void refusesATls12OnlyServer() throws Exception {
		String base = start("TLSv1.2");
		SubscriptionClient client = client(base);

		assertThatThrownBy(() -> client.get(base + "/1_1/resource/Batch/Subscription/abc", "tok", RANGE))
				.isInstanceOf(DataCustodianUnreachableException.class);
	}
}
