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

import org.greenbuttonalliance.espi.authserver.grant.GrantContext;
import org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore;
import org.greenbuttonalliance.espi.authserver.grant.GrantContexts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DelegatedCustomerAuthenticationFilter (#305)")
class DelegatedCustomerAuthenticationFilterTest {

	private static final String SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";
	private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

	private final GrantContextSessionStore grantContexts = new GrantContextSessionStore();
	private final DelegatedCustomerAuthenticationFilter filter = new DelegatedCustomerAuthenticationFilter(grantContexts);
	private final MockHttpSession session = new MockHttpSession();

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	/** Delegation {@code cid} of client {@code tp}, whose replay carries an S256 challenge. */
	private GrantContext delegation(MockHttpSession in) {
		GrantContext ctx = new GrantContext("cid", "tp", "reg-tp", "42",
				new GrantContext.Replay(GrantContexts.REDIRECT_URI, GrantContexts.STATE, CHALLENGE, "S256"),
				42L, SCOPE, List.of(), Instant.now().plusSeconds(300));
		grantContexts.put(in, ctx);
		return ctx;
	}

	/** The replay /continue built for delegation {@code cid}. */
	private MockHttpServletRequest replay() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
		request.setSession(session);
		request.setParameter("response_type", "code");
		request.setParameter("client_id", "tp");
		request.setParameter("redirect_uri", GrantContexts.REDIRECT_URI);
		request.setParameter("scope", SCOPE);
		request.setParameter("state", GrantContexts.STATE);
		request.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, "cid");
		request.setParameter("code_challenge", CHALLENGE);
		request.setParameter("code_challenge_method", "S256");
		return request;
	}

	/** Who the rest of the chain sees as signed in, and the context it is given. */
	private record Seen(Authentication signedIn, GrantContext delegated) {
	}

	private Seen during(MockHttpServletRequest request) throws Exception {
		AtomicReference<Seen> seen = new AtomicReference<>();
		filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> seen.set(new Seen(
				SecurityContextHolder.getContext().getAuthentication(),
				GrantContextSessionStore.delegated(request))));
		return seen.get();
	}

	@Test
	@DisplayName("the replay signs its customer in for that request only, and takes the context out of the session")
	void replaySignsTheCustomerInForThatRequestOnly() throws Exception {
		GrantContext ctx = delegation(session);

		Seen seen = during(replay());

		assertThat(seen.signedIn()).isNotNull()
				.satisfies(a -> assertThat(a.getName()).isEqualTo("42"))
				.satisfies(a -> assertThat(a.isAuthenticated()).isTrue())
				.satisfies(a -> assertThat(a.getAuthorities()).extracting(GrantedAuthority::getAuthority)
						.containsExactly("ROLE_CUSTOMER"));
		assertThat(seen.delegated()).isEqualTo(ctx);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).as("after the request").isNull();
		assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();
		assertThat(grantContexts.peek(session, "cid")).as("taken: single use").isNull();
	}

	@Test
	@DisplayName("#305 review: a second request for the same replay (a refresh) is not signed in and has no context")
	void secondRequestForTheReplayIsNotSignedIn() throws Exception {
		delegation(session);
		during(replay());

		assertThat(during(replay())).isEqualTo(new Seen(null, null));
	}

	@ParameterizedTest(name = "{0} = ''{1}''")
	@CsvSource({
			"client_id, another-tp",
			"redirect_uri, https://tp.example/other-cb",
			"scope, FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000",
			"state, another-state",
			"code_challenge, another-challenge",
			"code_challenge_method, plain"})
	@DisplayName("#305 review: a request naming the delegation with any other parameter is not its replay")
	void requestDifferingFromTheReplayIsNotSignedIn(String parameter, String value) throws Exception {
		delegation(session);
		MockHttpServletRequest request = replay();
		request.setParameter(parameter, value);

		assertThat(during(request)).isEqualTo(new Seen(null, null));
	}

	@Test
	@DisplayName("#305 review: the replay's parameters plus a request_uri are not the replay")
	void requestUriIsNotTheReplay() throws Exception {
		delegation(session);
		MockHttpServletRequest request = replay();
		request.setParameter("request_uri", "urn:ietf:params:oauth:request_uri:some-stored-request");

		assertThat(during(request)).isEqualTo(new Seen(null, null));
	}

	@Test
	@DisplayName("#305 review: a context without replay parameters (an earlier version's) matches nothing, no error")
	void contextWithoutReplayParametersMatchesNothing() throws Exception {
		grantContexts.put(session, new GrantContext("cid", "tp", "reg-tp", "42", null, 42L, SCOPE, List.of(),
				Instant.now().plusSeconds(300)));

		assertThat(during(replay())).isEqualTo(new Seen(null, null));
	}

	@Test
	@DisplayName("a request that differs from the replay uses up the delegation: the replay itself is then delegated again")
	void differingRequestUsesUpTheDelegation() throws Exception {
		delegation(session);
		MockHttpServletRequest altered = replay();
		altered.setParameter("state", "another-state");
		during(altered);

		assertThat(during(replay())).isEqualTo(new Seen(null, null));
	}

	@Test
	@DisplayName("a request without a delegation, naming an unknown one, or not a GET is passed on untouched")
	void otherRequestsArePassedOn() throws Exception {
		delegation(session);
		MockHttpServletRequest unnamed = replay();
		unnamed.removeParameter(GrantContextSessionStore.DELEGATION_PARAMETER);
		MockHttpServletRequest unknown = replay();
		unknown.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, "other");
		MockHttpServletRequest post = replay();
		post.setMethod("POST");

		assertThat(List.of(during(unnamed), during(unknown), during(post))).containsOnly(new Seen(null, null));
		assertThat(grantContexts.peek(session, "cid")).as("still there for its replay").isNotNull();
	}

	@Test
	@DisplayName("a delegation of another session signs no one in")
	void anotherSessionsDelegationSignsNoOneIn() throws Exception {
		delegation(new MockHttpSession());

		assertThat(during(replay())).isEqualTo(new Seen(null, null));
	}
}
