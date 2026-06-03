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

package org.greenbuttonalliance.espi.authserver.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AccessTokenResponseAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Custom {@link AuthenticationSuccessHandler} for the {@code /oauth2/token} endpoint that surfaces
 * the canonical ESPI URIs ({@code resourceURI}, {@code authorizationURI},
 * {@code customerResourceURI}) in the token-response JSON as top-level additional parameters.
 *
 * <h2>Why a custom handler</h2>
 * Spring Authorization Server's default success handler ({@link
 * OAuth2AccessTokenResponseAuthenticationSuccessHandler}) builds the {@code OAuth2AccessTokenResponse}
 * from {@link OAuth2AccessTokenAuthenticationToken#getAdditionalParameters()} only &mdash; it
 * doesn't reach back into the persisted authorization or token claims. The URIs we need to surface
 * live on the access token's claim set ({@code OAuth2Authorization.getAccessToken().getClaims()}),
 * written there by {@link GrantBackchannelTokenCustomizer} (C4.3). This handler bridges the two
 * worlds: it looks up the authorization by access-token value, reads the URI claims, and writes
 * them into the response.
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>If the access token has no ESPI URI claims (e.g. {@code client_credentials} grant for the
 *       admin client), the response matches the default handler's output verbatim.</li>
 *   <li>If the URI claims are present, they appear as top-level JSON fields alongside
 *       {@code access_token}, {@code token_type}, {@code expires_in}, and {@code scope}. ESPI
 *       1.1/4.0 third parties parse them from here.</li>
 * </ul>
 *
 * <p>This handler does NOT call the back-channel itself &mdash; that already happened during
 * token customization. The customizer is the single source of truth for back-channel state.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class EspiTokenResponseSuccessHandler implements AuthenticationSuccessHandler {

	private final OAuth2AuthorizationService authorizationService;
	private final OAuth2AccessTokenResponseHttpWriter responseWriter = new OAuth2AccessTokenResponseHttpWriter();

	private static final Set<String> ESPI_URI_CLAIMS = Set.of(
			GrantBackchannelTokenCustomizer.CLAIM_RESOURCE_URI,
			GrantBackchannelTokenCustomizer.CLAIM_AUTHORIZATION_URI,
			GrantBackchannelTokenCustomizer.CLAIM_CUSTOMER_RESOURCE_URI,
			GrantBackchannelTokenCustomizer.CLAIM_AUTHORIZATION_ID,
			GrantBackchannelTokenCustomizer.CLAIM_RESOURCE_SUBSCRIPTION_ID,
			GrantBackchannelTokenCustomizer.CLAIM_CUSTOMER_SUBSCRIPTION_ID
	);

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
										HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {
		if (!(authentication instanceof OAuth2AccessTokenAuthenticationToken tokenAuth)) {
			throw new ServletException(
					"Unexpected authentication type: " + authentication.getClass().getName());
		}

		OAuth2AccessToken accessToken = tokenAuth.getAccessToken();
		OAuth2RefreshToken refreshToken = tokenAuth.getRefreshToken();
		Map<String, Object> additional = new LinkedHashMap<>(tokenAuth.getAdditionalParameters());

		// Look up the authorization by access-token value to read the persisted claims that
		// GrantBackchannelTokenCustomizer wrote (C4.3). The claims live on
		// OAuth2Authorization.AccessToken.claims; OAuth2AccessTokenAuthenticationToken itself
		// doesn't carry them through to the response handler.
		OAuth2Authorization authorization = authorizationService.findByToken(
				accessToken.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
		if (authorization != null) {
			OAuth2Authorization.Token<OAuth2AccessToken> persistedToken =
					authorization.getToken(OAuth2AccessToken.class);
			if (persistedToken != null && persistedToken.getClaims() != null) {
				for (String claimName : ESPI_URI_CLAIMS) {
					Object value = persistedToken.getClaims().get(claimName);
					if (value != null) {
						additional.put(claimName, value);
					}
				}
			}
		}

		OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse
				.withToken(accessToken.getTokenValue())
				.tokenType(accessToken.getTokenType())
				.scopes(accessToken.getScopes());
		if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
			builder.expiresIn(java.time.Duration.between(
					accessToken.getIssuedAt(), accessToken.getExpiresAt()).getSeconds());
		}
		if (refreshToken != null) {
			builder.refreshToken(refreshToken.getTokenValue());
		}
		if (!additional.isEmpty()) {
			builder.additionalParameters(additional);
		}

		OAuth2AccessTokenResponse accessTokenResponse = builder.build();

		ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
		responseWriter.write(accessTokenResponse, httpResponse);

		log.debug("Token response augmented with espi keys: {}",
				additional.keySet().stream().filter(ESPI_URI_CLAIMS::contains).toList());
	}

	/**
	 * Thin wrapper exposing the package-default {@code OAuth2AccessTokenResponseHttpMessageConverter}.
	 * Spring Security ships the converter for client-side use (parsing token responses); we use it
	 * here on the server side to write the same wire format Spring AS's default handler produces.
	 */
	private static final class OAuth2AccessTokenResponseHttpWriter {
		private final org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter converter =
				new org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter();

		void write(OAuth2AccessTokenResponse response, ServletServerHttpResponse httpResponse)
				throws IOException {
			httpResponse.setStatusCode(HttpStatus.OK);
			httpResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
			try {
				converter.write(response, MediaType.APPLICATION_JSON, httpResponse);
			} catch (HttpMessageNotWritableException e) {
				throw new IOException("Failed to write OAuth2 access-token response", e);
			}
		}
	}
}
