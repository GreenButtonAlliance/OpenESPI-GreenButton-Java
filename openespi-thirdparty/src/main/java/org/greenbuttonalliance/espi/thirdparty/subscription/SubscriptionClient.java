package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * GETs a {@code Batch/Subscription} feed from the Data Custodian with a customer's access token (#89).
 * The token is attached only to a URI on the configured Data Custodian, and the answer is bounded in
 * size and time. The read timeout bounds only the wait for the response headers, not a body that stalls
 * once it has started; the body cap ({@code espi.thirdparty.subscription.max-body-bytes}) bounds its size,
 * not the time it takes.
 */
@Component
public class SubscriptionClient {

	static final String NOT_ON_DATA_CUSTODIAN = "The authorization's resource URI is not on the configured Data Custodian.";

	private static final String SUBSCRIPTION_PATH = "/espi/1_1/resource/Batch/Subscription/";
	private static final Logger log = LoggerFactory.getLogger(SubscriptionClient.class);
	private static final MediaType ATOM = MediaType.APPLICATION_ATOM_XML;

	private final RestClient restClient;
	private final URI baseUrl;
	private final int maxBodyBytes;

	@Autowired
	public SubscriptionClient(RestClient.Builder builder,
							  @Value("${espi.datacustodian.base-url:http://localhost:8081/DataCustodian}") String baseUrl,
							  @Value("${espi.thirdparty.subscription.max-body-bytes:67108864}") int maxBodyBytes,
							  @Value("${espi.thirdparty.subscription.connect-timeout:10s}") Duration connectTimeout,
							  @Value("${espi.thirdparty.subscription.read-timeout:60s}") Duration readTimeout,
							  Environment environment) {
		this(builder, baseUrl, maxBodyBytes, connectTimeout, readTimeout,
				!environment.acceptsProfiles(Profiles.of("local", "dev", "dev-mysql", "dev-postgresql")), defaultTlsContext());
	}

	/** For tests: {@code builder} is used as given (a mock server may be bound to it). */
	SubscriptionClient(RestClient.Builder builder, String baseUrl, int maxBodyBytes) {
		this(builder, baseUrl, maxBodyBytes, false);
	}

	/** For tests: as above, with the https-only rule of the non-local profiles switched on or off. */
	SubscriptionClient(RestClient.Builder builder, String baseUrl, int maxBodyBytes, boolean requireHttps) {
		this(builder.clone().build(), baseUrl, maxBodyBytes, requireHttps);
	}

	/** For tests: a real connection, trusting what {@code sslContext} trusts. */
	SubscriptionClient(RestClient.Builder builder, String baseUrl, int maxBodyBytes, Duration connectTimeout,
					   Duration readTimeout, boolean requireHttps, SSLContext sslContext) {
		this(builder.clone().requestFactory(requestFactory(connectTimeout, readTimeout, sslContext)).build(),
				baseUrl, maxBodyBytes, requireHttps);
	}

	/** The one place the settings are validated; it takes a built client, which keeps its signature distinct. */
	private SubscriptionClient(RestClient restClient, String baseUrl, int maxBodyBytes, boolean requireHttps) {
		if (maxBodyBytes < 1 || maxBodyBytes > Integer.MAX_VALUE - 1) {
			throw new IllegalArgumentException("espi.thirdparty.subscription.max-body-bytes must be between 1 and "
					+ (Integer.MAX_VALUE - 1) + ": " + maxBodyBytes);
		}
		this.baseUrl = URI.create(baseUrl);
		if (requireHttps && !"https".equalsIgnoreCase(this.baseUrl.getScheme())) {
			throw new IllegalStateException("espi.datacustodian.base-url must use https outside the local and dev profiles: "
					+ baseUrl);
		}
		this.restClient = restClient;
		this.maxBodyBytes = maxBodyBytes;
	}

	private static SSLContext defaultTlsContext() {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, null, null);
			return context;
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No TLS support available", e);
		}
	}

	/** The outbound connection speaks TLS 1.3 only: a server that offers TLS 1.2 or earlier is refused. */
	private static JdkClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout,
															   SSLContext sslContext) {
		SSLParameters parameters = new SSLParameters();
		parameters.setProtocols(new String[]{"TLSv1.3"});
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
				.connectTimeout(connectTimeout).sslContext(sslContext).sslParameters(parameters).build());
		factory.setReadTimeout(readTimeout);
		return factory;
	}

	/** Any HTTP status is a Response; its body is empty when the server sent none. */
	public record Response(int status, byte[] body) {
	}

	/**
	 * @throws DataCustodianUnreachableException on a connection failure, a body over the size limit, or a
	 *                                           resource URI that is not a Batch/Subscription URI of the
	 *                                           configured Data Custodian; its message never holds the token
	 */
	public Response get(String resourceUri, String accessToken, FetchRange range) {
		if (!onDataCustodian(resourceUri)) {
			throw new DataCustodianUnreachableException(NOT_ON_DATA_CUSTODIAN, null);
		}
		URI uri = UriComponentsBuilder.fromUriString(resourceUri)
				.queryParam("published-min", DateTimeFormatter.ISO_INSTANT.format(range.publishedMin()))
				.queryParam("published-max", DateTimeFormatter.ISO_INSTANT.format(range.publishedMax()))
				.build().encode().toUri();
		try {
			return restClient.get().uri(uri)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.accept(ATOM)
					.exchange((request, response) -> new Response(response.getStatusCode().value(), readBounded(response.getBody())));
		} catch (ResourceAccessException e) {
			throw new DataCustodianUnreachableException("The Data Custodian could not be reached.", e);
		}
	}

	private byte[] readBounded(InputStream in) throws IOException {
		byte[] body = in.readNBytes(maxBodyBytes + 1);
		if (body.length > maxBodyBytes) {
			throw new DataCustodianUnreachableException(
					"The Data Custodian's response exceeded " + describeLimit(maxBodyBytes) + ".", null);
		}
		return body;
	}

	/** "64.0 MB" from 1 MiB up, "512 bytes" below it. */
	static String describeLimit(int bytes) {
		return bytes < 1024 * 1024 ? bytes + " bytes"
				: String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
	}

	/**
	 * Same scheme, host and port as the base URL, and a path that is exactly the base path +
	 * {@code /espi/1_1/resource/Batch/Subscription/{id}}: one id segment, no further segment, no query, no fragment.
	 */
	private boolean onDataCustodian(String resourceUri) {
		URI uri;
		try {
			uri = URI.create(resourceUri);
		} catch (IllegalArgumentException | NullPointerException e) {
			log.warn("Refusing a resource URI that does not parse");
			return false;
		}
		// With no dot segment (written or encoded) normalizing changes nothing, so the URI sent is the URI checked.
		if (hasDotSegment(uri) || uri.getRawQuery() != null || uri.getRawFragment() != null) {
			log.warn("Refusing a resource URI with a dot segment, a query or a fragment");
			return false;
		}
		String basePath = baseUrl.getRawPath() == null ? "" : baseUrl.getRawPath().replaceAll("/+$", "");
		String path = uri.getRawPath() == null ? "" : uri.getRawPath();
		String idPart = path.startsWith(basePath + SUBSCRIPTION_PATH) ? path.substring((basePath + SUBSCRIPTION_PATH).length()) : "";
		// Equality with the base URL's scheme is enough for https-only: the constructor has already
		// refused a non-https base URL when https is required.
		boolean ok = uri.getScheme() != null && uri.getScheme().equalsIgnoreCase(baseUrl.getScheme())
				&& uri.getHost() != null && uri.getHost().equalsIgnoreCase(baseUrl.getHost())
				&& effectivePort(uri) == effectivePort(baseUrl)
				&& !idPart.isEmpty() && idPart.indexOf('/') < 0;
		if (!ok) {
			log.warn("Refusing a resource URI whose host is {}: it is not on the configured Data Custodian", uri.getHost());
		}
		return ok;
	}

	/**
	 * True when the raw path has a {@code .} or {@code ..} segment, written plainly or percent-encoded
	 * ({@code %2e}/{@code %2E}), or when normalizing the decoded path would change it.
	 */
	private static boolean hasDotSegment(URI uri) {
		String raw = uri.getRawPath();
		if (raw == null) {
			return false;
		}
		for (String segment : raw.split("/", -1)) {
			String decoded = segment.replaceAll("(?i)%2e", ".");
			if (decoded.equals(".") || decoded.equals("..")) {
				return true;
			}
		}
		return !uri.normalize().getPath().equals(uri.getPath());
	}

	private static int effectivePort(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}
		return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
	}
}
