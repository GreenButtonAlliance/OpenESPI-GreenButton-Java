/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.service;

import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelRequest;
import org.greenbuttonalliance.espi.authserver.backchannel.BackchannelResponse;
import org.greenbuttonalliance.espi.authserver.backchannel.DataCustodianBackchannelClient;
import org.greenbuttonalliance.espi.authserver.backchannel.DataCustodianBackchannelException;
import org.greenbuttonalliance.espi.authserver.grant.GrantContextEnrichingAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsSet;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level coverage of {@link GrantBackchannelTokenCustomizer}: skips correctly when there's no
 * ESPI grant context (e.g. {@code client_credentials} flow), forwards request fields verbatim to
 * the back-channel client, writes the response URIs as claims, and translates back-channel
 * failures into OAuth2 {@code server_error}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GrantBackchannelTokenCustomizer")
class GrantBackchannelTokenCustomizerTest {

	private static final UUID UP_1 = UUID.fromString("00000000-0000-5000-8000-000000000001");
	private static final UUID UP_2 = UUID.fromString("00000000-0000-5000-8000-000000000002");
	private static final UUID AUTH_ID = UUID.fromString("11111111-1111-5111-8111-111111111111");
	private static final UUID RES_SUB_ID = UUID.fromString("22222222-2222-5222-8222-222222222222");
	private static final UUID CUST_SUB_ID = UUID.fromString("33333333-3333-5333-8333-333333333333");

	@Mock
	private DataCustodianBackchannelClient client;

	private GrantBackchannelTokenCustomizer customizer;

	@BeforeEach
	void setUp() {
		customizer = new GrantBackchannelTokenCustomizer(client);
	}

	@Test
	@DisplayName("with grant context: forwards to back-channel and writes URI claims")
	void withGrantContextWritesClaims() {
		OAuth2Authorization auth = authorizationWithGrantContext(
				"corr-9", 42L, "FB_1;FB_4_5",
				UP_1 + ";" + UP_2,
				"https://dc.example/RetailCustomer/42");

		when(client.provision(any())).thenReturn(new BackchannelResponse(
				AUTH_ID, RES_SUB_ID, CUST_SUB_ID,
				"https://dc.example/Subscription/" + RES_SUB_ID,
				"https://dc.example/Authorization/" + AUTH_ID,
				"https://dc.example/RetailCustomer/42/Customer/" + CUST_SUB_ID));

		OAuth2TokenClaimsContext ctx = buildContext(auth);
		customizer.customize(ctx);

		ArgumentCaptor<BackchannelRequest> cap = ArgumentCaptor.forClass(BackchannelRequest.class);
		verify(client).provision(cap.capture());
		BackchannelRequest sent = cap.getValue();
		assertThat(sent)
				.extracting(BackchannelRequest::correlationId,
						BackchannelRequest::clientId,
						BackchannelRequest::grantedScope,
						BackchannelRequest::retailCustomerId,
						BackchannelRequest::customerResourceUri)
				.containsExactly("corr-9", "tp-1", "FB_1;FB_4_5", 42L,
						"https://dc.example/RetailCustomer/42");
		assertThat(sent.selectedUsagePointIds()).containsExactly(UP_1, UP_2);

		Map<String, Object> claims = ctx.getClaims().build().getClaims();
		assertThat(claims)
				.containsEntry(GrantBackchannelTokenCustomizer.CLAIM_RESOURCE_URI,
						"https://dc.example/Subscription/" + RES_SUB_ID)
				.containsEntry(GrantBackchannelTokenCustomizer.CLAIM_AUTHORIZATION_URI,
						"https://dc.example/Authorization/" + AUTH_ID)
				.containsEntry(GrantBackchannelTokenCustomizer.CLAIM_CUSTOMER_RESOURCE_URI,
						"https://dc.example/RetailCustomer/42/Customer/" + CUST_SUB_ID);
		// *_id claims removed in #160 — consumers parse ids from the canonical URIs (EspiBatchUri).
	}

	@Test
	@DisplayName("no grant context (e.g. client_credentials): no back-channel call, no claims added")
	void noGrantContextSkipsBackchannel() {
		OAuth2Authorization auth = blankAuthorization();

		OAuth2TokenClaimsContext ctx = buildContext(auth);
		customizer.customize(ctx);

		verify(client, never()).provision(any());
		assertThat(ctx.getClaims().build().getClaims())
				.doesNotContainKey(GrantBackchannelTokenCustomizer.CLAIM_RESOURCE_URI);
	}

	@Test
	@DisplayName("back-channel failure surfaces as OAuth2AuthenticationException(server_error)")
	void backchannelFailureSurfacesAsOAuth2Error() {
		OAuth2Authorization auth = authorizationWithGrantContext(
				"corr-fail", 7L, "FB_1", "", null);
		when(client.provision(any())).thenThrow(new DataCustodianBackchannelException("DC 503"));

		OAuth2TokenClaimsContext ctx = buildContext(auth);

		assertThatThrownBy(() -> customizer.customize(ctx))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.satisfies(e -> {
					OAuth2AuthenticationException oauth = (OAuth2AuthenticationException) e;
					assertThat(oauth.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.SERVER_ERROR);
				});
	}

	@Test
	@DisplayName("response with only resource URI (PII-less grant): customer claim omitted")
	void piiLessGrantOmitsCustomerClaim() {
		OAuth2Authorization auth = authorizationWithGrantContext(
				"corr-pii-less", 8L, "FB_1", UP_1.toString(), null);
		when(client.provision(any())).thenReturn(new BackchannelResponse(
				AUTH_ID, RES_SUB_ID, null,
				"https://dc.example/Subscription/" + RES_SUB_ID,
				"https://dc.example/Authorization/" + AUTH_ID,
				null));

		OAuth2TokenClaimsContext ctx = buildContext(auth);
		customizer.customize(ctx);

		Map<String, Object> claims = ctx.getClaims().build().getClaims();
		assertThat(claims)
				.containsEntry(GrantBackchannelTokenCustomizer.CLAIM_RESOURCE_URI,
						"https://dc.example/Subscription/" + RES_SUB_ID)
				.doesNotContainKey(GrantBackchannelTokenCustomizer.CLAIM_CUSTOMER_RESOURCE_URI);
	}

	@Test
	@DisplayName("refresh-token customization: skipped")
	void refreshTokenSkipped() {
		OAuth2Authorization auth = authorizationWithGrantContext(
				"corr-ref", 1L, "FB_1", "", null);

		OAuth2TokenClaimsContext ctx = OAuth2TokenClaimsContext
				.with(OAuth2TokenClaimsSet.builder())
				.registeredClient(registeredClient())
				.authorization(auth)
				.tokenType(OAuth2TokenType.REFRESH_TOKEN)
				.build();

		customizer.customize(ctx);
		verify(client, never()).provision(any());
	}

	// --- helpers ---

	private static RegisteredClient registeredClient() {
		return RegisteredClient.withId("internal-id-1")
				.clientId("tp-1")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("https://tp.example/cb")
				.scope("FB_1")
				.build();
	}

	private static OAuth2Authorization blankAuthorization() {
		return OAuth2Authorization.withRegisteredClient(registeredClient())
				.id("auth-blank")
				.principalName("42")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.build();
	}

	private static OAuth2Authorization authorizationWithGrantContext(String cid, long retailCustomerId,
																	 String scope, String selectedUps,
																	 String customerResourceUri) {
		OAuth2Authorization.Builder b = OAuth2Authorization.withRegisteredClient(registeredClient())
				.id("auth-" + cid)
				.principalName(String.valueOf(retailCustomerId))
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.attribute(GrantContextEnrichingAuthorizationService.ATTR_CORRELATION_ID, cid)
				.attribute(GrantContextEnrichingAuthorizationService.ATTR_RETAIL_CUSTOMER_ID,
						String.valueOf(retailCustomerId))
				.attribute(GrantContextEnrichingAuthorizationService.ATTR_GRANTED_SCOPE, scope)
				.attribute(GrantContextEnrichingAuthorizationService.ATTR_SELECTED_USAGE_POINT_IDS,
						selectedUps == null ? "" : selectedUps);
		if (customerResourceUri != null) {
			b.attribute(GrantContextEnrichingAuthorizationService.ATTR_CUSTOMER_RESOURCE_URI,
					customerResourceUri);
		}
		return b.build();
	}

	private static OAuth2TokenClaimsContext buildContext(OAuth2Authorization auth) {
		// OAuth2TokenClaimsContext requires a fresh claims-set builder; subject + iat are placeholder.
		OAuth2TokenClaimsSet.Builder claimsBuilder = OAuth2TokenClaimsSet.builder()
				.subject(auth.getPrincipalName())
				.issuedAt(Instant.now());
		return OAuth2TokenClaimsContext.with(claimsBuilder)
				.registeredClient(registeredClient())
				.authorization(auth)
				.tokenType(OAuth2TokenType.ACCESS_TOKEN)
				.build();
	}
}
