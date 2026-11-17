/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.thirdparty.oauth;

import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the ESPI URIs of a token response until the token is stored (#146).
 *
 * <p>REQ.21 6.1.2 puts {@code resourceURI}, {@code authorizationURI} and (with FB_51)
 * {@code customerResourceURI} in the token response, next to the access token. Spring Security parses
 * them into {@link OAuth2AccessTokenResponse#getAdditionalParameters()}, but the
 * {@code OAuth2AuthorizedClient} it then stores holds only the tokens, so they would be lost. The
 * {@link #capturing capturing} token-response client keeps them in memory, keyed by the access token,
 * and {@link EspiAuthorizationOAuth2AuthorizedClientService} takes them when it stores that token.</p>
 *
 * <p>The hand-over does not depend on a servlet request, so a token obtained or refreshed on another
 * thread (a {@code WebClient} call, a scheduled import) keeps its URIs too. Spring stores the token
 * right after the response in the same call, so an entry normally lives for milliseconds; one that is
 * never taken (the store failed) is dropped after {@link #RETENTION}.</p>
 */
public final class EspiTokenResponseParameters {

	/** REQ.21 6.1.2 token-response parameter names (as the Authorization Server writes them). */
	public static final String RESOURCE_URI = "resourceURI";
	public static final String AUTHORIZATION_URI = "authorizationURI";
	public static final String CUSTOMER_RESOURCE_URI = "customerResourceURI";

	/** How long the URIs of a token response wait to be taken before they are dropped. */
	static final Duration RETENTION = Duration.ofMinutes(5);

	private record Pending(EspiUris uris, Instant received) {
	}

	/** Token responses whose token has not been stored yet, by access token value. */
	private static final Map<String, Pending> PENDING = new ConcurrentHashMap<>();

	/** The ESPI URIs of one token response; any of them may be {@code null}. */
	public record EspiUris(String resourceUri, String authorizationUri, String customerResourceUri) {

		static EspiUris from(Map<String, Object> additionalParameters) {
			return new EspiUris(text(additionalParameters, RESOURCE_URI), text(additionalParameters, AUTHORIZATION_URI),
					text(additionalParameters, CUSTOMER_RESOURCE_URI));
		}

		private static String text(Map<String, Object> parameters, String name) {
			Object value = parameters.get(name);
			return value == null || value.toString().isBlank() ? null : value.toString();
		}
	}

	private EspiTokenResponseParameters() {
	}

	/** Wraps a Spring token-response client so the ESPI URIs of each response are kept for storage. */
	public static <T extends AbstractOAuth2AuthorizationGrantRequest> OAuth2AccessTokenResponseClient<T> capturing(
			OAuth2AccessTokenResponseClient<T> delegate) {
		return grantRequest -> {
			OAuth2AccessTokenResponse response = delegate.getTokenResponse(grantRequest);
			if (response != null) {
				Instant now = Instant.now();
				PENDING.values().removeIf(pending -> pending.received().isBefore(now.minus(RETENTION)));
				PENDING.put(response.getAccessToken().getTokenValue(),
						new Pending(EspiUris.from(response.getAdditionalParameters()), now));
			}
			return response;
		};
	}

	/** The ESPI URIs that came with this access token, removed once taken. */
	static Optional<EspiUris> take(String accessTokenValue) {
		return Optional.ofNullable(PENDING.remove(accessTokenValue)).map(Pending::uris);
	}
}
