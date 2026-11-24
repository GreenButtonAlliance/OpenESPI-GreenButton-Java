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

import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.util.StringUtils;

/**
 * The customer's consent for the replayed {@code /oauth2/authorize} comes from the delegation it names, not from
 * a saved consent (#305).
 *
 * <p>Before #305, {@code /oauth2/authorize/continue} saved the consent for (client, customer), and it was removed
 * after the authorization response. Two authorizations at once in one browser then shared it: one's removal sent
 * the other's replay back to the DC, and a replay the browser never followed left it in place for the next
 * authorization (#276). Now nothing is saved: on the replay of a delegation (the request whose context
 * {@link org.greenbuttonalliance.espi.authserver.web.delegate.DelegatedCustomerAuthenticationFilter} took,
 * {@link GrantContextSessionStore#DELEGATED_CONTEXT_ATTRIBUTE}), for that delegation's client and customer, the
 * consent is the scope the customer approved on the DC Authorization Screen. Every other lookup goes to the stored consents, which only
 * a principal other than a delegated customer can reach: a customer is signed in only on such a replay
 * ({@link org.greenbuttonalliance.espi.authserver.web.delegate.DelegatedCustomerAuthenticationFilter}).</p>
 */
public class DelegatedConsentService implements OAuth2AuthorizationConsentService {

	private final OAuth2AuthorizationConsentService delegate;

	public DelegatedConsentService(OAuth2AuthorizationConsentService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void save(OAuth2AuthorizationConsent authorizationConsent) {
		delegate.save(authorizationConsent);
	}

	@Override
	public void remove(OAuth2AuthorizationConsent authorizationConsent) {
		delegate.remove(authorizationConsent);
	}

	@Override
	public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
		GrantContext ctx = GrantContextSessionStore.delegated(GrantContextSessionStore.currentRequest());
		if (ctx != null && ctx.registeredClientId().equals(registeredClientId)
				&& ctx.principal().equals(principalName)) {
			OAuth2AuthorizationConsent.Builder consent = OAuth2AuthorizationConsent.withId(registeredClientId, principalName);
			// Space-delimited per RFC 6749 section 3.3: an ESPI scope's ';' separates its parameters, not scopes (#210).
			for (String scope : StringUtils.tokenizeToStringArray(ctx.approvedScope(), " ")) {
				consent.scope(scope);
			}
			return consent.build();
		}
		return delegate.findById(registeredClientId, principalName);
	}
}
