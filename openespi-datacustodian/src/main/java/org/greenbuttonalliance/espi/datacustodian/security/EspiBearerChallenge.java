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

package org.greenbuttonalliance.espi.datacustodian.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Set;

/**
 * The {@code WWW-Authenticate} challenge of every 401 and 403 on the ESPI resource API (#316): RFC 6750 §3, adopted
 * by REQ.21.6.1.2 and checked by OpenESPI-GreenButtonCMDTest. It carries the {@code error} code and nothing else.
 * <ul>
 *   <li>No token: 401, {@code Bearer} with no error code (RFC 6750 §3.1).</li>
 *   <li>A token that is missing its value, inactive or otherwise not acceptable: {@code Bearer error="invalid_token"}
 *       (401), or {@code error="invalid_request"} (400) for a malformed request, such as two tokens. A Spring
 *       {@code BearerTokenError} keeps its own code and status.</li>
 *   <li>A valid token that may not read the resource: 403, {@code Bearer error="insufficient_scope"}. This is the
 *       same whether a URL rule refused it (a Function Block or token type) or the Data Custodian's row-level
 *       check did (another subscription or retail customer, #286).</li>
 * </ul>
 * There is no {@code error_description}, {@code error_uri}, {@code scope} or {@code resource_metadata}, all optional
 * in RFC 6750 and OAuth 2.1. A more specific reason would let a client probing the API learn which subscriptions,
 * retail customers or entries exist. Certified Data Custodians answer the same way.
 * <p>
 * The reason is logged on the server instead, for operators: the method, the request path (not the query string),
 * the token's {@code client_id} (known once introspection accepted the token, so on a 403), and the exception's message. Never the token, the {@code Authorization}
 * header or the exception's cause. A 403 or a rejected token is logged at INFO; a request without a token at DEBUG,
 * since every anonymous request gets one.
 */
public class EspiBearerChallenge implements AuthenticationEntryPoint, AccessDeniedHandler {

	static final String NO_TOKEN = "Bearer";

	private static final Set<String> RFC_6750_CODES = Set.of(BearerTokenErrorCodes.INVALID_REQUEST,
			BearerTokenErrorCodes.INVALID_TOKEN, BearerTokenErrorCodes.INSUFFICIENT_SCOPE);

	private static final Logger log = LoggerFactory.getLogger(EspiBearerChallenge.class);

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		if (authException instanceof OAuth2AuthenticationException oauth2) {
			// A BearerTokenError keeps its own RFC 6750 code and status, so the two always agree; any other
			// OAuth 2.0 error is an unacceptable token.
			HttpStatus status = HttpStatus.UNAUTHORIZED;
			String code = BearerTokenErrorCodes.INVALID_TOKEN;
			if (oauth2.getError() instanceof BearerTokenError bearer && RFC_6750_CODES.contains(bearer.getErrorCode())) {
				status = bearer.getHttpStatus();
				code = bearer.getErrorCode();
			}
			logRejection(request, code, authException.getMessage());
			challenge(response, status, "Bearer error=\"" + code + "\"");
		} else {
			log.debug("{} {}: no bearer token ({})", request.getMethod(), request.getRequestURI(),
					authException.getMessage());
			challenge(response, HttpStatus.UNAUTHORIZED, NO_TOKEN);
		}
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) {
		logRejection(request, BearerTokenErrorCodes.INSUFFICIENT_SCOPE, accessDeniedException.getMessage());
		challenge(response, HttpStatus.FORBIDDEN,
				"Bearer error=\"" + BearerTokenErrorCodes.INSUFFICIENT_SCOPE + "\"");
	}

	/** One format for every rejected token, whichever path refused it. */
	private static void logRejection(HttpServletRequest request, String code, String reason) {
		log.info("{} {}: {} for client_id {} ({})", request.getMethod(), request.getRequestURI(), code, clientId(),
				reason);
	}

	/**
	 * The {@code client_id} introspection attribute of the request's token, or "unknown" when there is none, as for a
	 * token that failed introspection. Never the token itself.
	 */
	private static Object clientId() {
		if (SecurityContextHolder.getContext().getAuthentication() instanceof BearerTokenAuthentication token) {
			Object clientId = token.getTokenAttributes().get("client_id");
			return clientId != null ? clientId : "unknown";
		}
		return "unknown";
	}

	private static void challenge(HttpServletResponse response, HttpStatus status, String value) {
		response.addHeader(HttpHeaders.WWW_AUTHENTICATE, value);
		response.setStatus(status.value());
	}
}
