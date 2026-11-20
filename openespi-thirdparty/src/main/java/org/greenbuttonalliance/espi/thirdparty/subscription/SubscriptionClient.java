package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * GETs a {@code Batch/Subscription} feed from the Data Custodian with a customer's access token (#89).
 * The token is attached only to a URI on the configured Data Custodian, and the answer is bounded in
 * size and time.
 */
@Component
public class SubscriptionClient {

	static final String NOT_ON_DATA_CUSTODIAN = "The authorization's resource URI is not on the configured Data Custodian.";

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
							  @Value("${espi.thirdparty.subscription.read-timeout:60s}") Duration readTimeout) {
		this(builder.clone().requestFactory(requestFactory(connectTimeout, readTimeout)), baseUrl, maxBodyBytes, true);
	}

	/** For tests: {@code builder} is used as given (a mock server may be bound to it). */
	SubscriptionClient(RestClient.Builder builder, String baseUrl, int maxBodyBytes) {
		this(builder.clone(), baseUrl, maxBodyBytes, true);
	}

	private SubscriptionClient(RestClient.Builder builder, String baseUrl, int maxBodyBytes, boolean ready) {
		this.restClient = builder.build();
		this.baseUrl = URI.create(baseUrl);
		this.maxBodyBytes = maxBodyBytes;
	}

	private static JdkClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(connectTimeout).build());
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
					"The Data Custodian's response exceeded " + maxBodyBytes / (1024 * 1024) + " MB.", null);
		}
		return body;
	}

	/** Same scheme, host and port as the base URL, a path under its path, and a Batch/Subscription id. */
	private boolean onDataCustodian(String resourceUri) {
		URI uri;
		try {
			uri = URI.create(resourceUri).normalize();
		} catch (IllegalArgumentException | NullPointerException e) {
			log.warn("Refusing a resource URI that does not parse");
			return false;
		}
		String basePath = baseUrl.getPath() == null ? "" : baseUrl.getPath().replaceAll("/+$", "");
		String path = uri.getPath() == null ? "" : uri.getPath();
		boolean ok = uri.getScheme() != null && uri.getScheme().equalsIgnoreCase(baseUrl.getScheme())
				&& uri.getHost() != null && uri.getHost().equalsIgnoreCase(baseUrl.getHost())
				&& effectivePort(uri) == effectivePort(baseUrl)
				&& (path.equals(basePath) || path.startsWith(basePath + "/"))
				&& EspiBatchUri.subscriptionId(resourceUri).filter(id -> !id.isBlank()).isPresent();
		if (!ok) {
			log.warn("Refusing a resource URI whose host is {}: it is not on the configured Data Custodian", uri.getHost());
		}
		return ok;
	}

	private static int effectivePort(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}
		return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
	}
}
