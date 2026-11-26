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

package org.greenbuttonalliance.espi.authserver.grant;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The Return-handoff fields the AS needs to remember between
 * {@code /oauth2/authorize/continue} (where the customer's selections arrive from DC) and the
 * back-channel call at {@code /oauth2/token} time. Saved into the HTTP session by
 * {@link org.greenbuttonalliance.espi.authserver.web.delegate.AuthorizeContinueController} and
 * stamped onto the corresponding {@code OAuth2Authorization.attributes} by
 * {@link GrantContextEnrichingAuthorizationService} when Spring AS persists the authorization at
 * code-issue time.
 *
 * <p>Implements {@link Serializable} because {@link jakarta.servlet.http.HttpSession} requires it
 * for replication/persistence.</p>
 *
 * <p>Since #305 it also stands in for the customer's AS sign-in and consent: it is kept per delegation
 * (keyed by {@code correlationId}) with an expiry, and only the replayed {@code /oauth2/authorize} that carries
 * its id is signed in
 * ({@link org.greenbuttonalliance.espi.authserver.web.delegate.DelegatedCustomerAuthenticationFilter}) and consented
 * ({@link DelegatedConsentService}) from it. The filter takes it from the session, once, and only for a request
 * whose parameters are those of the replay {@code /continue} built ({@link #replay()}), with no
 * {@code request_uri}.</p>
 *
 * @param correlationId         the AS-generated id of the delegation (#331); ties the outbound, return, and
 *                              back-channel calls together and keys this context
 * @param clientId              the Third Party's {@code client_id}
 * @param registeredClientId    the Third Party's {@code RegisteredClient} id (the consent key)
 * @param principal             the customer identity the DC returned (the authorization's principal name)
 * @param replay                the request parameters of the replayed {@code /oauth2/authorize} besides
 *                              {@code client_id} and {@code scope} (the approved scope)
 * @param retailCustomerId      DC primary key of the authenticated customer; numeric per DC's
 *                              {@code SubscriptionProvisionRequest} contract. The AS parses the
 *                              return handoff's opaque {@code principal} into this Long at
 *                              {@code /continue} time; non-numeric principals are rejected.
 * @param approvedScope         the customer's effective scope (post Authorization-Screen
 *                              narrowing); {@code ;}-delimited ESPI tokens
 * @param selectedUsagePointIds usage points the customer chose to share with the TP; may be empty
 *                              for grants that include only Customer/PII scope
 * @param expiresAt             after this the context is ignored and discarded
 */
public record GrantContext(
		String correlationId,
		String clientId,
		String registeredClientId,
		String principal,
		Replay replay,
		Long retailCustomerId,
		String approvedScope,
		List<UUID> selectedUsagePointIds,
		Instant expiresAt
) implements Serializable {

	boolean isExpired(Instant now) {
		return !now.isBefore(expiresAt);
	}

	/**
	 * The Third Party's {@code redirect_uri}, {@code state} and PKCE challenge, as replayed (#305 review): a request
	 * naming the delegation with any other value is not its replay. Blank and missing values are the same.
	 * {@code redirectUri} is null when the Third Party left it out (#335): the replay leaves it out too.
	 */
	public record Replay(String redirectUri, String state, String codeChallenge, String codeChallengeMethod)
			implements Serializable {
	}
}
