/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.authserver.backchannel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * AS&rarr;DC back-channel client. Calls {@code POST /internal/backchannel/v1/subscriptions} on the
 * Data Custodian with the grant context the AS gathered from the customer's Authorization Screen
 * choices; receives the canonical resource URIs the AS surfaces in the OAuth2 token response.
 *
 * <h2>Transport</h2>
 * <ul>
 *   <li>Spring {@link RestClient} (synchronous, blocking) &mdash; the back-channel call sits on the
 *       TP's token-exchange wait path, no reactive composition needed.</li>
 *   <li>HTTP Basic auth with the credential pair the DC's
 *       {@code BackchannelSecurityConfiguration} expects ({@code espi.backchannel.client-id} /
 *       {@code espi.backchannel.client-secret} must match on both sides).</li>
 *   <li>5s connect timeout, 10s read timeout. Anything slower than that means DC is unhealthy and
 *       failing the token-exchange is the correct response &mdash; better than a long-hanging TP.</li>
 *   <li>No retry. The token endpoint is itself a TP retry point; adding an inner retry doubles the
 *       worst-case latency for marginal reliability gain.</li>
 * </ul>
 *
 * <h2>Error contract</h2>
 * Any failure &mdash; network, timeout, 4xx, 5xx, parse &mdash; surfaces as
 * {@link DataCustodianBackchannelException}. The caller at token-mint translates this into an
 * OAuth2 {@code server_error} so the TP receives a standard error and no token is issued.
 */
@Slf4j
@Service
public class DataCustodianBackchannelClient {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
	private static final String SUBSCRIPTIONS_PATH = "/internal/backchannel/v1/subscriptions";

	private final RestClient restClient;

	@Autowired
	public DataCustodianBackchannelClient(
			@Value("${espi.backchannel.endpoint:http://localhost:8081/DataCustodian}") String endpoint,
			@Value("${espi.backchannel.client-id:as-backchannel}") String clientId,
			@Value("${espi.backchannel.client-secret:change-me-in-production}") String clientSecret) {
		this(buildDefaultRestClient(endpoint, clientId, clientSecret));
	}

	/** Package-private constructor for tests that need to inject a stubbed RestClient. */
	DataCustodianBackchannelClient(RestClient restClient) {
		this.restClient = restClient;
	}

	/**
	 * Call DC's back-channel and return the canonical URIs for the persisted Authorization +
	 * Subscriptions. Synchronous; blocks the caller until DC responds or the timeout fires.
	 *
	 * @throws DataCustodianBackchannelException on any transport or server failure
	 */
	public BackchannelResponse provision(BackchannelRequest request) {
		try {
			BackchannelResponse response = restClient.post()
					.uri(SUBSCRIPTIONS_PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (req, resp) -> {
						HttpStatus status = HttpStatus.resolve(resp.getStatusCode().value());
						String statusLabel = (status != null) ? status.toString()
								: String.valueOf(resp.getStatusCode().value());
						String body = readBodySafely(resp);
						throw new DataCustodianBackchannelException(
								"DC back-channel " + statusLabel + ": " + body);
					})
					.body(BackchannelResponse.class);

			if (response == null) {
				throw new DataCustodianBackchannelException(
						"DC back-channel returned empty response body");
			}
			log.info("DC back-channel ok: client={}, authorization_id={}, resource_uri={}",
					request.clientId(), response.authorizationId(), response.resourceUri());
			return response;

		} catch (ResourceAccessException e) {
			// Network failure or read timeout
			throw new DataCustodianBackchannelException(
					"DC back-channel transport failure: " + e.getMessage(), e);
		} catch (DataCustodianBackchannelException e) {
			throw e;
		} catch (RestClientException e) {
			throw new DataCustodianBackchannelException(
					"DC back-channel call failed: " + e.getMessage(), e);
		}
	}

	private static String readBodySafely(org.springframework.http.client.ClientHttpResponse resp) {
		try {
			byte[] bytes = resp.getBody().readAllBytes();
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (Exception e) {
			return "<unable to read body: " + e.getMessage() + ">";
		}
	}

	private static RestClient buildDefaultRestClient(String endpoint, String clientId, String clientSecret) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
		factory.setReadTimeout((int) READ_TIMEOUT.toMillis());

		String basicAuth = "Basic " + Base64.getEncoder()
				.encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

		return RestClient.builder()
				.baseUrl(endpoint)
				.requestFactory(factory)
				.defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
