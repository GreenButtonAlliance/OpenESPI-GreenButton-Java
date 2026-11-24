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
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.authserver.grant.GrantContext;
import org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Signs the customer in for the replayed {@code /oauth2/authorize} of a delegation, and for that request only
 * (#305).
 *
 * <p>Before #305, {@code /oauth2/authorize/continue} saved the customer's sign-in in the AS session, where it
 * outlived the delegation whenever the browser did not follow the replay, and was shared by every authorization
 * in that browser. Now the customer the DC returned is signed in only on a {@code GET /oauth2/authorize} that
 * names a live delegation of this session ({@link GrantContextSessionStore#DELEGATION_PARAMETER}) and whose
 * {@code client_id}, {@code redirect_uri}, {@code scope} (the approved scope), {@code state} and PKCE challenge
 * are exactly the replay {@code /continue} built. The sign-in is never saved to the session.</p>
 *
 * <p>The context is taken out of the session here, once, and kept on the request
 * ({@link GrantContextSessionStore#DELEGATED_CONTEXT_ATTRIBUTE}) for the consent and the enricher. A second request
 * for the same replay (a refresh), or one that differs from it, finds no context, is not signed in, and is
 * delegated to the DC again: it can never mint a code without the delegation's grant context, and a signed-in
 * replay is always consented (its scope is the approved scope), so it never reaches Spring's consent page.</p>
 */
@Slf4j
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
		GrantContext ctx = grantContexts.consume(request);
		if (ctx == null || !isReplayOf(request, ctx)) {
			if (ctx != null) {
				log.warn("Replay of correlation_id={} refused: its parameters differ from the delegation's",
						ctx.correlationId());
			}
			chain.doFilter(request, response);
			return;
		}
		request.setAttribute(GrantContextSessionStore.DELEGATED_CONTEXT_ATTRIBUTE, ctx);
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

	/** The request's parameters are exactly those of the replay {@code /continue} built for this delegation. */
	private static boolean isReplayOf(HttpServletRequest request, GrantContext ctx) {
		GrantContext.Replay replay = ctx.replay();
		return same(request.getParameter(OAuth2ParameterNames.CLIENT_ID), ctx.clientId())
				&& same(request.getParameter(OAuth2ParameterNames.SCOPE), ctx.approvedScope())
				&& same(request.getParameter(OAuth2ParameterNames.REDIRECT_URI), replay.redirectUri())
				&& same(request.getParameter(OAuth2ParameterNames.STATE), replay.state())
				&& same(request.getParameter(PkceParameterNames.CODE_CHALLENGE), replay.codeChallenge())
				&& same(request.getParameter(PkceParameterNames.CODE_CHALLENGE_METHOD), replay.codeChallengeMethod());
	}

	private static boolean same(String actual, String expected) {
		return Objects.equals(StringUtils.hasText(actual) ? actual : null,
				StringUtils.hasText(expected) ? expected : null);
	}
}
