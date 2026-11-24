/*
 * Copyright 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.grant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DelegatedConsentService (#305)")
class DelegatedConsentServiceTest {

	private static final String SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";
	private static final String SECOND = "FB=1_3_4_5_13_14_39;IntervalDuration=900;BlockDuration=daily;HistoryLength=604800";

	private final InMemoryOAuth2AuthorizationConsentService stored = new InMemoryOAuth2AuthorizationConsentService();
	private final GrantContextSessionStore grantContexts = new GrantContextSessionStore();
	private final DelegatedConsentService consents = new DelegatedConsentService(stored, grantContexts);
	private final MockHttpSession session = new MockHttpSession();
	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		request = new MockHttpServletRequest("GET", "/oauth2/authorize");
		request.setSession(session);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	private void delegation(String correlationId, String scope) {
		grantContexts.put(session, GrantContexts.of(correlationId, "tp", "reg-tp", "42", scope, List.of(),
				Instant.now().plusSeconds(300)));
	}

	@Test
	@DisplayName("the replay of a delegation: the consent is the approved scope, each ESPI scope string whole (#210)")
	void replayGetsTheApprovedScope() {
		delegation("cid", SCOPE + "  " + SECOND);
		request.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, "cid");

		assertThat(consents.findById("reg-tp", "42"))
				.extracting(OAuth2AuthorizationConsent::getRegisteredClientId, OAuth2AuthorizationConsent::getPrincipalName,
						OAuth2AuthorizationConsent::getScopes)
				.containsExactly("reg-tp", "42", java.util.Set.of(SCOPE, SECOND));
	}

	@Test
	@DisplayName("a request naming no delegation gets no delegated consent, and nothing is ever stored")
	void noDelegationNoConsent() {
		delegation("cid", SCOPE);

		assertThat(consents.findById("reg-tp", "42")).isNull();
		assertThat(stored.findById("reg-tp", "42")).isNull();
	}

	@Test
	@DisplayName("another client's or customer's lookup on the replay is not given the delegation's consent")
	void otherClientOrCustomerGetsNoConsent() {
		delegation("cid", SCOPE);
		request.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, "cid");

		assertThat(consents.findById("reg-other", "42")).isNull();
		assertThat(consents.findById("reg-tp", "77")).isNull();
	}

	@Test
	@DisplayName("an expired delegation gives no consent")
	void expiredDelegationGivesNoConsent() {
		grantContexts.put(session, GrantContexts.of("cid", "tp", "reg-tp", "42", SCOPE, List.of(),
				Instant.now().minusSeconds(1)));
		request.setParameter(GrantContextSessionStore.DELEGATION_PARAMETER, "cid");

		assertThat(consents.findById("reg-tp", "42")).isNull();
	}

	@Test
	@DisplayName("outside a delegation, stored consents (a non-customer's) are used as before")
	void storedConsentsOutsideADelegation() {
		OAuth2AuthorizationConsent admin = OAuth2AuthorizationConsent.withId("reg-tp", "admin").scope("x").build();
		consents.save(admin);

		assertThat(consents.findById("reg-tp", "admin")).isEqualTo(admin);
	}
}
