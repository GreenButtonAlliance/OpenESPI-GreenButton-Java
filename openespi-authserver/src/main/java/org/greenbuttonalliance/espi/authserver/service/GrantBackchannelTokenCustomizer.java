/*
 * Copyright 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.authserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelRequest;
import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelResponse;
import org.greenbuttonalliance.espi.authserver.backchannel.DataCustodianBackchannelClient;
import org.greenbuttonalliance.espi.authserver.backchannel.DataCustodianBackchannelException;
import org.greenbuttonalliance.espi.authserver.grant.GrantContextEnrichingAuthorizationService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Opaque-token customizer that calls DC's back-channel at access-token mint time and stamps the
 * returned canonical URIs onto the token's claims set.
 *
 * <h2>When this fires</h2>
 * Spring Authorization Server invokes the customizer once per access token minted at
 * {@code /oauth2/token}. Only the {@code authorization_code} grant provisions at the Data Custodian;
 * a {@code refresh_token} grant continues the same ESPI Authorization, so its new access token
 * inherits the URI claims of the token it replaces (no second back-channel call, #202); any other
 * grant ({@code client_credentials}) is left untouched. The {@link
 * OAuth2TokenClaimsContext} provides read access to the {@link OAuth2Authorization} that was
 * persisted at code-issue time &mdash; including the grant-context attributes
 * {@link GrantContextEnrichingAuthorizationService} wrote there (C4.1).
 *
 * <h2>What it does</h2>
 * <ol>
 *   <li>Reads the grant-context attributes (retail_customer_id, granted_scope, selected UPs,
 *       customer_resource_uri, correlation_id).</li>
 *   <li>If no grant-context attributes are present, returns silently &mdash; this is a
 *       {@code client_credentials} or admin-flavored grant that didn't go through the customer
 *       Authorization Screen, and shouldn't trigger back-channel provisioning.</li>
 *   <li>Calls {@link DataCustodianBackchannelClient#provision} synchronously.</li>
 *   <li>Writes the response URIs as claims on the {@link OAuth2TokenClaimsContext}. These flow
 *       into the persisted access token's claim set, so {@code /oauth2/introspect} returns them
 *       automatically.</li>
 * </ol>
 *
 * <h2>Claim names</h2>
 * Exactly the ESPI 4.0 (REQ.21) token-response wire format the third party expects:
 * {@code resourceURI}, {@code authorizationURI}, {@code customerResourceURI}. Consumers parse any
 * needed ids out of those canonical URIs (see {@code EspiBatchUri}); the previously-emitted bare
 * {@code *_id} claims were non-standard and were removed in #160.
 *
 * <h2>Failure handling</h2>
 * Any {@link DataCustodianBackchannelException} is rethrown as an {@link OAuth2AuthenticationException}
 * with error code {@code server_error}. Spring AS's token endpoint converts that into a standard
 * OAuth2 error response, so the TP receives a clean failure and no token is issued. No half-issued
 * grants &mdash; the back-channel result is the gate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrantBackchannelTokenCustomizer implements OAuth2TokenCustomizer<OAuth2TokenClaimsContext> {

	public static final String CLAIM_RESOURCE_URI = "resourceURI";
	public static final String CLAIM_AUTHORIZATION_URI = "authorizationURI";
	public static final String CLAIM_CUSTOMER_RESOURCE_URI = "customerResourceURI";

	/** The ESPI 4.0 (REQ.21 6.1.2) token-response URI claims, in wire order. */
	public static final List<String> ESPI_URI_CLAIMS = List.of(
			CLAIM_RESOURCE_URI, CLAIM_AUTHORIZATION_URI, CLAIM_CUSTOMER_RESOURCE_URI);

	private final DataCustodianBackchannelClient backchannelClient;

	@Override
	public void customize(OAuth2TokenClaimsContext context) {
		if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
			return;
		}
		OAuth2Authorization authorization = context.getAuthorization();
		if (authorization == null) {
			return;
		}

		AuthorizationGrantType grantType = context.getAuthorizationGrantType();
		if (AuthorizationGrantType.REFRESH_TOKEN.equals(grantType)) {
			// #202: a refresh continues the SAME ESPI Authorization. Never re-provision (that would
			// create another Authorization/Subscription at the DC); carry the URIs over from the access
			// token being replaced so the refresh response and introspection stay consistent.
			copyClaimsFromPreviousAccessToken(context, authorization);
			return;
		}
		if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(grantType)) {
			// client_credentials etc.: no customer consent, nothing to provision here (see #202 gap 3).
			return;
		}

		String grantedScope = authorization.getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE);
		if (grantedScope == null || grantedScope.isBlank()) {
			log.debug("No ESPI grant context on authorization id={}; skipping back-channel call",
					authorization.getId());
			return;
		}

		String retailCustomerIdStr = authorization.getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_RETAIL_CUSTOMER_ID);
		String correlationId = authorization.getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_CORRELATION_ID);
		String selectedUpsSerialized = authorization.getAttribute(
				GrantContextEnrichingAuthorizationService.ATTR_SELECTED_USAGE_POINT_IDS);

		String clientId = context.getRegisteredClient().getClientId();
		List<UUID> selectedUps = GrantContextEnrichingAuthorizationService
				.parseUuids(selectedUpsSerialized);

		BackchannelRequest request = new BackchannelRequest(
				correlationId,
				clientId,
				grantedScope,
				Long.valueOf(retailCustomerIdStr),
				selectedUps);

		BackchannelResponse response;
		try {
			response = backchannelClient.provision(request);
		} catch (DataCustodianBackchannelException e) {
			log.error("DC back-channel failed at token mint for client={}, correlation_id={}: {}",
					clientId, correlationId, e.getMessage());
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
							"Subscription provisioning failed at the Data Custodian", null), e);
		}

		writeClaims(context, response);
	}

	private static void copyClaimsFromPreviousAccessToken(OAuth2TokenClaimsContext context,
														  OAuth2Authorization authorization) {
		OAuth2Authorization.Token<OAuth2AccessToken> previous = authorization.getAccessToken();
		Map<String, Object> previousClaims = previous != null ? previous.getClaims() : null;
		if (previousClaims == null) {
			log.debug("Refresh on authorization id={} has no prior access-token claims to carry over",
					authorization.getId());
			return;
		}
		var claims = context.getClaims();
		for (String name : ESPI_URI_CLAIMS) {
			Object value = previousClaims.get(name);
			if (value != null) {
				claims.claim(name, value);
			}
		}
	}

	private static void writeClaims(OAuth2TokenClaimsContext context, BackchannelResponse response) {
		// ESPI 4.0 (REQ.21) token-response / introspection augmentation carries ONLY the three canonical
		// URIs — resourceURI, authorizationURI, customerResourceURI. The bare *_id fields were
		// non-standard duplicates of the ids already embedded in those URIs (and customer_subscription_id
		// was mislabeled); consumers parse ids from the URIs via EspiBatchUri instead. Removed per #160.
		var claims = context.getClaims();
		if (response.resourceUri() != null) {
			claims.claim(CLAIM_RESOURCE_URI, response.resourceUri());
		}
		if (response.authorizationUri() != null) {
			claims.claim(CLAIM_AUTHORIZATION_URI, response.authorizationUri());
		}
		if (response.customerResourceUri() != null) {
			claims.claim(CLAIM_CUSTOMER_RESOURCE_URI, response.customerResourceUri());
		}
	}
}
