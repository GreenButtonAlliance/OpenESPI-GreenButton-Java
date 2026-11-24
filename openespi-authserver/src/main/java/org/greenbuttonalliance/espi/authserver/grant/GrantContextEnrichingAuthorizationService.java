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

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wraps the standard {@code JdbcOAuth2AuthorizationService} to stamp ESPI grant context onto each
 * new {@code OAuth2Authorization}'s attribute map at code-issue time.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Customer returns from DC's Authorization Screen; {@code AuthorizeContinueController}
 *       writes a {@link GrantContext} into the HTTP session via
 *       {@link GrantContextSessionStore} and redirects to {@code /oauth2/authorize}.</li>
 *   <li>Spring AS's {@code OAuth2AuthorizationCodeRequestAuthenticationProvider} runs inside that
 *       request, builds an {@link OAuth2Authorization} carrying the freshly-minted authorization
 *       code, and calls {@link #save(OAuth2Authorization)}.</li>
 *   <li>This wrapper sees the save of the authorization carrying the code, takes the {@link GrantContext}
 *       the replay of its delegation took from the session
 *       ({@link GrantContextSessionStore#DELEGATED_CONTEXT_ATTRIBUTE}, #305), checks it is this client's and
 *       customer's (or fails the request), and rebuilds the authorization with the grant context as attributes
 *       before delegating to the underlying JDBC service.</li>
 * </ol>
 *
 * <h2>Why attributes?</h2>
 * {@code OAuth2Authorization.attributes} is Spring AS's designed extension point for
 * "auxiliary state attached to the authorization aggregate." The {@code JdbcOAuth2Authorization
 * Service} persists the map to the JSON {@code attributes} column automatically, so the values
 * survive JVM restart and are accessible at {@code /oauth2/token} time without any additional
 * infrastructure.
 *
 * <h2>Serialization</h2>
 * All values stored as {@code String}. The JDBC store uses Jackson with a curated allow-list of
 * types; sticking to {@code String} avoids needing custom mixins. Collections become
 * {@code ;}-delimited; consumers split at read time.
 *
 * <h2>Idempotency</h2>
 * Spring AS calls {@code save()} repeatedly across the authorization lifecycle (code issue, token
 * exchange, token use). Only the replay request carries the context, and an authorization already stamped is
 * left alone; later saves (token exchange, token use) see no context and pass through.
 */
@Slf4j
public class GrantContextEnrichingAuthorizationService implements OAuth2AuthorizationService {

	public static final String ATTR_RETAIL_CUSTOMER_ID = "espi.retail_customer_id";
	public static final String ATTR_GRANTED_SCOPE = "espi.granted_scope";
	public static final String ATTR_SELECTED_USAGE_POINT_IDS = "espi.selected_usage_point_ids";
	public static final String ATTR_CORRELATION_ID = "espi.correlation_id";

	private final OAuth2AuthorizationService delegate;

	public GrantContextEnrichingAuthorizationService(OAuth2AuthorizationService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void save(OAuth2Authorization authorization) {
		delegate.save(maybeEnrich(authorization));
	}

	@Override
	public void remove(OAuth2Authorization authorization) {
		delegate.remove(authorization);
	}

	@Override
	public OAuth2Authorization findById(String id) {
		return delegate.findById(id);
	}

	@Override
	public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
		return delegate.findByToken(token, tokenType);
	}

	private OAuth2Authorization maybeEnrich(OAuth2Authorization authorization) {
		// Idempotency: don't re-enrich an authorization we've already stamped.
		if (authorization.getAttribute(ATTR_GRANTED_SCOPE) != null) {
			return authorization;
		}

		// Only the authorization that carries the code: Spring AS also saves a state-only one before a
		// consent-page redirect, which must not take the context.
		if (authorization.getToken(OAuth2AuthorizationCode.class) == null) {
			return authorization;
		}

		// #305: the context the replay of its delegation took (DelegatedCustomerAuthenticationFilter), on this
		// request only. A customer is signed in only on such a replay, so a customer's code always has one.
		GrantContext ctx = GrantContextSessionStore.delegated(GrantContextSessionStore.currentRequest());
		if (ctx == null) {
			return authorization;
		}
		if (!ctx.registeredClientId().equals(authorization.getRegisteredClientId())
				|| !ctx.principal().equals(authorization.getPrincipalName())) {
			// Never mint a code without its delegation's grant context: the provider turns this into an error.
			throw new IllegalStateException("grant context of correlation_id=" + ctx.correlationId()
					+ " is another client's or customer's");
		}

		log.info("Stamping grant context on OAuth2Authorization id={}, correlation_id={}",
				authorization.getId(), ctx.correlationId());

		OAuth2Authorization.Builder b = OAuth2Authorization.from(authorization)
				.attribute(ATTR_RETAIL_CUSTOMER_ID, String.valueOf(ctx.retailCustomerId()))
				.attribute(ATTR_GRANTED_SCOPE, ctx.approvedScope())
				.attribute(ATTR_SELECTED_USAGE_POINT_IDS, serializeUuids(ctx.selectedUsagePointIds()))
				.attribute(ATTR_CORRELATION_ID, ctx.correlationId());
		return b.build();
	}

	static String serializeUuids(List<UUID> ids) {
		if (ids == null || ids.isEmpty()) {
			return "";
		}
		return ids.stream().map(UUID::toString).collect(Collectors.joining(";"));
	}

	/**
	 * Parse the {@code ;}-delimited form back into a {@link UUID} list.
	 * Inverse of {@link #serializeUuids}. Returned for use by C4.3 consumers.
	 */
	public static List<UUID> parseUuids(String serialized) {
		if (serialized == null || serialized.isBlank()) {
			return List.of();
		}
		return java.util.Arrays.stream(serialized.split(";"))
				.filter(s -> !s.isBlank())
				.map(UUID::fromString)
				.toList();
	}

}
