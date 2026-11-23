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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Entry point of the authorization endpoint chain for a customer who is not yet authenticated at the
 * AS. It redirects to {@link AuthorizeDelegateController} ({@value #DELEGATE_PATH}) and carries the
 * authorization request's {@code client_id}, {@code scope}, {@code state} and {@code redirect_uri}
 * across, which the delegate needs to build the signed outbound handoff (#232).
 *
 * <p>A plain {@link LoginUrlAuthenticationEntryPoint} redirects to a static URL, so the delegate
 * received none of these parameters and failed with 400. By the time this entry point runs, Spring
 * Authorization Server has already validated the client, its {@code redirect_uri} and the requested
 * scope, so the values forwarded here are the validated request values.</p>
 */
public class DelegateAuthorizationEntryPoint extends LoginUrlAuthenticationEntryPoint {

	static final String DELEGATE_PATH = "/authorize/delegate";

	/** The authorization request parameters the delegate reads, in the order they are forwarded. */
	static final List<String> FORWARDED_PARAMETERS = List.of("client_id", "scope", "state", "redirect_uri",
			// #274: a Third Party's PKCE challenge (RFC 7636), carried to the replayed /oauth2/authorize. REQ.21
			// does not require PKCE; RFC 9700 2.1.1 (which updates RFC 6749) has the AS enforce it when sent.
			PkceParameterNames.CODE_CHALLENGE, PkceParameterNames.CODE_CHALLENGE_METHOD);

	public DelegateAuthorizationEntryPoint() {
		super(DELEGATE_PATH);
	}

	@Override
	protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) {
		UriComponentsBuilder url = UriComponentsBuilder.fromPath(getLoginFormUrl());
		for (String name : FORWARDED_PARAMETERS) {
			String value = request.getParameter(name);
			if (value != null) {
				// Encoded per value: a scope carries '=' and ';', and a state may carry '+' or '&'.
				url.queryParam(name, URLEncoder.encode(value, StandardCharsets.UTF_8));
			}
		}
		return url.build(true).toUriString();
	}
}
