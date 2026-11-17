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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.Optional;

/**
 * Keeps the ESPI URIs of a token response until the token is stored (#146).
 *
 * <p>REQ.21 6.1.2 puts {@code resourceURI}, {@code authorizationURI} and (with FB_51)
 * {@code customerResourceURI} in the token response, next to the access token. Spring Security parses
 * them into {@link OAuth2AccessTokenResponse#getAdditionalParameters()}, but the
 * {@code OAuth2AuthorizedClient} it then stores holds only the tokens, so they would be lost. The
 * {@link #capturing capturing} token-response client keeps them as a request attribute, keyed by the
 * access token, and {@link EspiAuthorizationOAuth2AuthorizedClientService} takes them when it stores
 * that token in the same request.</p>
 *
 * <p>Outside a request (no {@link RequestContextHolder} attributes) nothing is kept; the stored
 * Authorization then keeps the URIs it already has.</p>
 */
public final class EspiTokenResponseParameters {

	/** REQ.21 6.1.2 token-response parameter names (as the Authorization Server writes them). */
	public static final String RESOURCE_URI = "resourceURI";
	public static final String AUTHORIZATION_URI = "authorizationURI";
	public static final String CUSTOMER_RESOURCE_URI = "customerResourceURI";

	private static final String ATTRIBUTE_PREFIX = EspiTokenResponseParameters.class.getName() + ".";

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
			RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
			if (attributes != null && response != null) {
				attributes.setAttribute(ATTRIBUTE_PREFIX + response.getAccessToken().getTokenValue(),
						EspiUris.from(response.getAdditionalParameters()), RequestAttributes.SCOPE_REQUEST);
			}
			return response;
		};
	}

	/** The ESPI URIs that came with this access token in the current request, removed once taken. */
	static Optional<EspiUris> take(String accessTokenValue) {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			return Optional.empty();
		}
		String name = ATTRIBUTE_PREFIX + accessTokenValue;
		Object uris = attributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
		attributes.removeAttribute(name, RequestAttributes.SCOPE_REQUEST);
		return uris instanceof EspiUris espiUris ? Optional.of(espiUris) : Optional.empty();
	}
}
