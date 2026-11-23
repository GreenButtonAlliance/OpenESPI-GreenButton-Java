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

package org.greenbuttonalliance.espi.authserver.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * The response to a client that is not authenticated at the token, introspection and revocation endpoints (#225):
 * RFC 6749 §5.2 {@code invalid_client}.
 * <ul>
 *   <li>No client credentials ({@link AuthenticationEntryPoint}): 401 {@code invalid_client}. Before #225 such a
 *       request with no {@code Accept} header or a wildcard one, as curl and most HTTP client libraries send, was
 *       redirected to the customer login delegate.</li>
 *   <li>Credentials that fail ({@link AuthenticationFailureHandler}): the same response as Spring Authorization
 *       Server's own (401 for {@code invalid_client}, otherwise 400, with the error code only), plus the challenge.</li>
 * </ul>
 * Every 401 carries {@code WWW-Authenticate: Basic realm="oauth2"}: RFC 6749 §5.2 requires one matching the scheme a
 * client used in the {@code Authorization} header, and RFC 9110 §15.5.2 one on every 401. {@code client_secret_basic}
 * is the scheme ESPI clients are registered with. The body is the error code alone, so a caller learns nothing about
 * which clients exist.
 */
public class OAuth2ClientChallenge implements AuthenticationEntryPoint, AuthenticationFailureHandler {

	static final String CHALLENGE = "Basic realm=\"oauth2\"";

	private final HttpMessageConverter<OAuth2Error> errorConverter = new OAuth2ErrorHttpMessageConverter();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		write(response, OAuth2ErrorCodes.INVALID_CLIENT);
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		SecurityContextHolder.clearContext();
		write(response, exception instanceof OAuth2AuthenticationException oauth2
				? oauth2.getError().getErrorCode()
				: OAuth2ErrorCodes.INVALID_CLIENT);
	}

	private void write(HttpServletResponse response, String errorCode) throws IOException {
		ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
		if (OAuth2ErrorCodes.INVALID_CLIENT.equals(errorCode)) {
			httpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
			httpResponse.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, CHALLENGE);
		} else {
			httpResponse.setStatusCode(HttpStatus.BAD_REQUEST);
		}
		errorConverter.write(new OAuth2Error(errorCode), null, httpResponse);
	}
}
