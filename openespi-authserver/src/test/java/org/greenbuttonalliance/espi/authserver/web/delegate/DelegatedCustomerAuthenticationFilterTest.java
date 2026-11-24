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

import org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore;
import org.greenbuttonalliance.espi.authserver.grant.GrantContexts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DelegatedCustomerAuthenticationFilter (#305)")
class DelegatedCustomerAuthenticationFilterTest {

	private final GrantContextSessionStore grantContexts = new GrantContextSessionStore();
	private final DelegatedCustomerAuthenticationFilter filter = new DelegatedCustomerAuthenticationFilter(grantContexts);
	private final MockHttpSession session = new MockHttpSession();

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	private MockHttpServletRequest authorize(String method, String delegation, String clientId) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, "/oauth2/authorize");
		request.setSession(session);
		if (delegation != null) {
			request.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, delegation);
		}
		request.setParameter("client_id", clientId);
		return request;
	}

	/** Who the rest of the chain sees as signed in. */
	private Authentication signedInDuring(MockHttpServletRequest request) throws Exception {
		AtomicReference<Authentication> seen = new AtomicReference<>();
		filter.doFilter(request, new MockHttpServletResponse(),
				(req, res) -> seen.set(SecurityContextHolder.getContext().getAuthentication()));
		return seen.get();
	}

	@Test
	@DisplayName("the replay of a live delegation signs its customer in for that request only, never in the session")
	void replaySignsTheCustomerInForThatRequestOnly() throws Exception {
		grantContexts.put(session, GrantContexts.of("cid", "FB_1", List.of()));

		Authentication during = signedInDuring(authorize("GET", "cid", "tp"));

		assertThat(during).isNotNull()
				.satisfies(a -> assertThat(a.getName()).isEqualTo("42"))
				.satisfies(a -> assertThat(a.isAuthenticated()).isTrue())
				.satisfies(a -> assertThat(a.getAuthorities()).extracting(GrantedAuthority::getAuthority)
						.containsExactly("ROLE_CUSTOMER"));
		assertThat(SecurityContextHolder.getContext().getAuthentication()).as("after the request").isNull();
		assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();
		assertThat(grantContexts.peek(session, "cid")).as("the filter only reads the context").isNotNull();
	}

	@Test
	@DisplayName("no sign-in without a delegation, with an unknown one, for another client, or on another method")
	void noSignInOtherwise() throws Exception {
		grantContexts.put(session, GrantContexts.of("cid", "FB_1", List.of()));

		assertThat(signedInDuring(authorize("GET", null, "tp"))).isNull();
		assertThat(signedInDuring(authorize("GET", "other", "tp"))).isNull();
		assertThat(signedInDuring(authorize("GET", "cid", "another-tp"))).isNull();
		assertThat(signedInDuring(authorize("POST", "cid", "tp"))).isNull();
	}

	@Test
	@DisplayName("a delegation of another session signs no one in")
	void anotherSessionsDelegationSignsNoOneIn() throws Exception {
		grantContexts.put(new MockHttpSession(), GrantContexts.of("cid", "FB_1", List.of()));

		assertThat(signedInDuring(authorize("GET", "cid", "tp"))).isNull();
	}
}
