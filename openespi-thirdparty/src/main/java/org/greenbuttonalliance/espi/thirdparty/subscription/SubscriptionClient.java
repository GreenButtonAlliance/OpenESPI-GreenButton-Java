package org.greenbuttonalliance.espi.thirdparty.subscription;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.format.DateTimeFormatter;

/** GETs a {@code Batch/Subscription} feed from the Data Custodian with a customer's access token (#89). */
@Component
public class SubscriptionClient {

	private static final MediaType ATOM = MediaType.APPLICATION_ATOM_XML;

	private final RestClient restClient;

	public SubscriptionClient(RestClient.Builder builder) {
		this.restClient = builder.clone().build();
	}

	/** Any HTTP status is a Response; its body is empty when the server sent none. */
	public record Response(int status, byte[] body) {
	}

	/**
	 * @throws DataCustodianUnreachableException on a connection failure; its message never holds the token
	 */
	public Response get(String resourceUri, String accessToken, FetchRange range) {
		URI uri = UriComponentsBuilder.fromUriString(resourceUri)
				.queryParam("published-min", DateTimeFormatter.ISO_INSTANT.format(range.publishedMin()))
				.queryParam("published-max", DateTimeFormatter.ISO_INSTANT.format(range.publishedMax()))
				.build().encode().toUri();
		try {
			return restClient.get().uri(uri)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.accept(ATOM)
					.exchange((request, response) -> {
						byte[] body = response.getBody().readAllBytes();
						return new Response(response.getStatusCode().value(), body);
					});
		} catch (ResourceAccessException e) {
			throw new DataCustodianUnreachableException("The Data Custodian could not be reached.", e);
		}
	}
}
