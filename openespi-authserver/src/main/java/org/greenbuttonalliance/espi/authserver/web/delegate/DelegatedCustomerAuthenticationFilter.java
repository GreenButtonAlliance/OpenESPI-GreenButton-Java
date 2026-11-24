/*
 * Copyright 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.web.delegate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.greenbuttonalliance.espi.authserver.grant.GrantContext;
import org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Signs the customer in for the replayed {@code /oauth2/authorize} of a delegation, and for that request only
 * (#305).
 *
 * <p>Before #305, {@code /oauth2/authorize/continue} saved the customer's sign-in in the AS session, where it
 * outlived the delegation whenever the browser did not follow the replay, and was shared by every authorization
 * in that browser. Now the customer the DC returned is signed in only on a {@code GET /oauth2/authorize} that
 * names a live delegation of this session ({@link GrantContextSessionStore#DELEGATION_PARAMETER}) for the same
 * {@code client_id}. The sign-in is never saved to the session: once the request ends, the session holds no
 * customer, so any other authorization is delegated to the DC again.</p>
 */
public class DelegatedCustomerAuthenticationFilter extends OncePerRequestFilter {

	static final String AUTHORIZE_PATH = "/oauth2/authorize";

	private final GrantContextSessionStore grantContexts;
	private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();

	public DelegatedCustomerAuthenticationFilter(GrantContextSessionStore grantContexts) {
		this.grantContexts = grantContexts;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !HttpMethod.GET.matches(request.getMethod())
				|| !AUTHORIZE_PATH.equals(request.getRequestURI().substring(request.getContextPath().length()))
				|| request.getParameter(GrantContextSessionStore.DELEGATION_PARAMETER) == null;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		GrantContext ctx = grantContexts.peek(request);
		if (ctx == null || !ctx.clientId().equals(request.getParameter(OAuth2ParameterNames.CLIENT_ID))) {
			chain.doFilter(request, response);
			return;
		}
		SecurityContext previous = holder.getContext();
		SecurityContext delegated = holder.createEmptyContext();
		delegated.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(ctx.principal(), null,
				List.of(new SimpleGrantedAuthority(SingleUseCustomerAuthorizationResponseHandler.CUSTOMER_AUTHORITY))));
		holder.setContext(delegated);
		try {
			chain.doFilter(request, response);
		} finally {
			holder.setContext(previous);
		}
	}
}
