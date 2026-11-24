/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.authserver.web.delegate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sends the authorization response of {@code /oauth2/authorize}, a code or an error, and ends the delegation
 * the request named (#299, #276, #305).
 *
 * <p>Since #305 the customer is signed in and has consented only on the replayed {@code /oauth2/authorize} of a
 * delegation, from its {@link org.greenbuttonalliance.espi.authserver.grant.GrantContext}
 * ({@link DelegatedCustomerAuthenticationFilter},
 * {@link org.greenbuttonalliance.espi.authserver.grant.DelegatedConsentService}); no sign-in or consent is saved.
 * When a code is minted, {@code GrantContextEnrichingAuthorizationService} has already taken the context. When
 * the replay ends in an error (e.g. {@code invalid_scope}), nothing else would, so it is removed here: the
 * delegation cannot be replayed again. Every customer authorization thus goes through
 * {@link DelegateAuthorizationEntryPoint} and the Data Custodian's Authorization Screen with the Third Party's
 * own {@code state} and {@code redirect_uri}. A replay the browser never follows leaves a context that only
 * its own replay can use, and only until it expires.</p>
 *
 * <p>The responses themselves are Spring's defaults ({@code OAuth2AuthorizationEndpointFilter}'s
 * {@code sendAuthorizationResponse} and {@code sendErrorResponse}): a redirect to the request's
 * {@code redirect_uri} with {@code code}, or with {@code error}, {@code error_description} and
 * {@code error_uri}, and, when present, {@code state}; an error without a {@code redirect_uri} is a 400.</p>
 */
@Slf4j
public class SingleUseCustomerAuthorizationResponseHandler
		implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

	static final String CUSTOMER_AUTHORITY = "ROLE_CUSTOMER";

	private final GrantContextSessionStore grantContextSessionStore;
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	public SingleUseCustomerAuthorizationResponseHandler(GrantContextSessionStore grantContextSessionStore) {
		this.grantContextSessionStore = grantContextSessionStore;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException {
		OAuth2AuthorizationCodeRequestAuthenticationToken codeRequest =
				(OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
		grantContextSessionStore.consume(request);
		UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(codeRequest.getRedirectUri())
				.queryParam(OAuth2ParameterNames.CODE, codeRequest.getAuthorizationCode().getTokenValue());
		sendRedirect(request, response, uri, codeRequest.getState());
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
										AuthenticationException exception) throws IOException {
		if (grantContextSessionStore.consume(request) != null) {
			log.debug("Delegation ended by an authorization error response: {}", exception.getMessage());
		}
		OAuth2AuthorizationCodeRequestAuthenticationToken codeRequest =
				exception instanceof OAuth2AuthorizationCodeRequestAuthenticationException codeRequestException
						? codeRequestException.getAuthorizationCodeRequestAuthentication()
						: null;
		OAuth2Error error = exception instanceof OAuth2AuthenticationException oauth2
				? oauth2.getError()
				: new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR);
		if (codeRequest == null || !StringUtils.hasText(codeRequest.getRedirectUri())) {
			response.sendError(HttpStatus.BAD_REQUEST.value(), error.toString());
			return;
		}
		UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(codeRequest.getRedirectUri())
				.queryParam(OAuth2ParameterNames.ERROR, error.getErrorCode());
		if (StringUtils.hasText(error.getDescription())) {
			uri.queryParam(OAuth2ParameterNames.ERROR_DESCRIPTION,
					UriUtils.encode(error.getDescription(), StandardCharsets.UTF_8));
		}
		if (StringUtils.hasText(error.getUri())) {
			uri.queryParam(OAuth2ParameterNames.ERROR_URI, UriUtils.encode(error.getUri(), StandardCharsets.UTF_8));
		}
		sendRedirect(request, response, uri, codeRequest.getState());
	}

	/** Adds {@code state} when present; {@code build(true)}: the components are explicitly encoded. */
	private void sendRedirect(HttpServletRequest request, HttpServletResponse response, UriComponentsBuilder uri,
							  String state) throws IOException {
		if (StringUtils.hasText(state)) {
			uri.queryParam(OAuth2ParameterNames.STATE, UriUtils.encode(state, StandardCharsets.UTF_8));
		}
		redirectStrategy.sendRedirect(request, response, uri.build(true).toUriString());
	}
}
