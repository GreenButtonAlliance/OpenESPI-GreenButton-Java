/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.web.delegate;

import org.greenbuttonalliance.espi.handoff.HandoffNonceService;
import org.greenbuttonalliance.espi.handoff.InvalidHandoffException;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Covers the three terminal paths through {@code /oauth2/authorize/continue}:
 *
 * <ol>
 *   <li><strong>Allow</strong> &mdash; valid return handoff with {@code consent="allow"}
 *       and a non-blank {@code approved_scope}. Should pre-populate the consent store, save the
 *       SecurityContext, and 302 to {@code /oauth2/authorize} with the original TP params.</li>
 *   <li><strong>Deny</strong> &mdash; valid return handoff with {@code consent="deny"}. Should
 *       301/302 to the third party's registered {@code redirect_uri} with
 *       {@code error=access_denied} per RFC 6749 &sect;4.1.2.1, including the TP's original
 *       {@code state}.</li>
 *   <li><strong>Invalid handoff</strong> &mdash; codec verification failure (expiry, signature,
 *       direction, or wrong nonce). Should render the uniform 400 error page with no information
 *       leak; downstream collaborators (nonce service, consent store) must not be touched.</li>
 * </ol>
 *
 * <p>Uses real codec + real {@link DelegationStateService} for end-to-end fidelity; nonce service,
 * consent service, and registered-client repo are mocked.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizeContinueController")
class AuthorizeContinueControllerTest {

	private static final String SIGNING_KEY = "test-signing-key-must-be-at-least-32-chars-long";

	@Mock
	private HandoffNonceService nonceService;
	@Mock
	private OAuth2AuthorizationConsentService consentService;
	@Mock
	private RegisteredClientRepository registeredClientRepository;

	private SignedHandoffCodec codec;
	private DelegationStateService delegationStateService;
	private org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore grantContextSessionStore;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		codec = new SignedHandoffCodec(SIGNING_KEY);
		delegationStateService = new DelegationStateService();
		grantContextSessionStore = new org.greenbuttonalliance.espi.authserver.grant.GrantContextSessionStore();

		AuthorizeContinueController controller = new AuthorizeContinueController(
				codec, nonceService, delegationStateService, consentService, registeredClientRepository,
				grantContextSessionStore);

		// No custom view resolver: Spring's standalone setup handles "redirect:" directives natively
		// (via RedirectView), and view().name(...) assertions match against the controller's return
		// value before resolution — so the absence of a Thymeleaf engine doesn't matter.
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Nested
	@DisplayName("Allow path")
	class AllowPath {

		@Test
		@DisplayName("valid allow handoff: pre-populates consent and redirects to /oauth2/authorize")
		void allowRedirectsToAuthorizeWithConsentSeeded() throws Exception {
			String cid = "corr-allow-1";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1");

			RegisteredClient client = RegisteredClient.withId("client-internal-id-1")
					.clientId("tp-1")
					.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.redirectUri("https://tp.example/cb")
					.scope("FB_1").scope("FB_4_5")
					.build();
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client);

			String token = codec.encode(allowReturn(cid, "FB_1;FB_4_5"));

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl(
							"/oauth2/authorize?response_type=code&client_id=tp-1&redirect_uri=https://tp.example/cb&scope=FB_1;FB_4_5&state=tp-state-1"));

			verify(nonceService).consume(any(), any());

			ArgumentCaptor<OAuth2AuthorizationConsent> cap =
					ArgumentCaptor.forClass(OAuth2AuthorizationConsent.class);
			verify(consentService).save(cap.capture());
			OAuth2AuthorizationConsent saved = cap.getValue();
			assertThat(saved.getRegisteredClientId()).isEqualTo("client-internal-id-1");
			assertThat(saved.getPrincipalName()).isEqualTo("42");
			assertThat(saved.getScopes()).containsExactlyInAnyOrder("FB_1", "FB_4_5");

			// Delegation state is single-use
			assertThat(delegationStateService.consume(cid)).isNull();
		}

		@Test
		@DisplayName("registered client missing at /continue: 400 with no consent write")
		void clientGoneAtContinueIs400() throws Exception {
			String cid = "corr-allow-2";
			delegationStateService.save(cid, "tp-vanished", "https://tp.example/cb", "tp-state");
			when(registeredClientRepository.findByClientId("tp-vanished")).thenReturn(null);

			String token = codec.encode(allowReturn(cid, "FB_1"));

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			verifyNoInteractions(consentService);
		}
	}

	@Nested
	@DisplayName("Deny path")
	class DenyPath {

		@Test
		@DisplayName("valid deny handoff: 302 to TP redirect_uri with error=access_denied + state")
		void denyRedirectsToTpWithAccessDenied() throws Exception {
			String cid = "corr-deny-1";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1");
			String token = codec.encode(denyReturn(cid));

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(
							"https://tp.example/cb?error=access_denied&error_description=*&state=tp-state-1"));

			verify(nonceService).consume(any(), any());
			verifyNoInteractions(consentService);
		}

		@Test
		@DisplayName("deny with no tpState: redirects without state param")
		void denyWithoutStateOmitsStateParam() throws Exception {
			String cid = "corr-deny-2";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", null);
			String token = codec.encode(denyReturn(cid));

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl(
							"https://tp.example/cb?error=access_denied&error_description=The%20customer%20did%20not%20authorize%20the%20request."));
		}

		@Test
		@DisplayName("deny with no registered redirect_uri: 400 (cannot deliver access_denied)")
		void denyWithoutRedirectUriIs400() throws Exception {
			String cid = "corr-deny-3";
			delegationStateService.save(cid, "tp-no-uri", null, "tp-state");
			String token = codec.encode(denyReturn(cid));

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));
		}

		@Test
		@DisplayName("allow with blank approved_scope falls through to deny semantics")
		void allowWithBlankApprovedScopeIsDeny() throws Exception {
			String cid = "corr-deny-4";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1");
			String token = codec.encode(allowReturn(cid, ""));

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(
							"https://tp.example/cb?error=access_denied&error_description=*&state=tp-state-1"));

			verifyNoInteractions(consentService);
		}
	}

	@Nested
	@DisplayName("Invalid handoff path")
	class InvalidPath {

		@Test
		@DisplayName("malformed token: 400 error/400 view, nothing else touched")
		void malformedTokenIs400() throws Exception {
			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", "not.a.valid.token"))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			verifyNoInteractions(nonceService, consentService, registeredClientRepository);
		}

		@Test
		@DisplayName("expired token: 400")
		void expiredTokenIs400() throws Exception {
			// Manually craft an expired payload
			Instant past = Instant.now().minusSeconds(3600);
			SignedHandoff.Return expired = SignedHandoff.Return.of(
					"corr-expired", past.minusSeconds(600), past, "nonce-x",
					"42", List.of(), "https://dc.example/cust/1",
					SignedHandoff.Return.CONSENT_ALLOW, "FB_1");
			String token = codec.encode(expired);

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			verifyNoInteractions(nonceService, consentService);
		}

		@Test
		@DisplayName("nonce replay: nonceService throws InvalidHandoffException → 400")
		void nonceReplayIs400() throws Exception {
			String cid = "corr-replay";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state");
			String token = codec.encode(allowReturn(cid, "FB_1"));

			org.mockito.Mockito.doThrow(new InvalidHandoffException("nonce replay"))
					.when(nonceService).consume(any(), any());

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			verifyNoInteractions(consentService);
		}

		@Test
		@DisplayName("missing delegation state: 400")
		void missingStateIs400() throws Exception {
			// Don't save any state — codec decode succeeds, but state lookup fails
			String token = codec.encode(allowReturn("corr-unknown", "FB_1"));

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));
		}

		@Test
		@DisplayName("missing handoff param: 400")
		void missingHandoffParamIs400() throws Exception {
			mockMvc.perform(get("/oauth2/authorize/continue"))
					.andExpect(status().isBadRequest());
		}
	}

	// --- Helpers ---

	private static SignedHandoff.Return allowReturn(String cid, String approvedScope) {
		Instant now = Instant.now();
		return SignedHandoff.Return.of(
				cid, now, now.plusSeconds(120),
				UUID.randomUUID().toString().replace("-", ""),
				"42",
				List.of(),
				"https://dc.example/RetailCustomer/1",
				SignedHandoff.Return.CONSENT_ALLOW,
				approvedScope);
	}

	private static SignedHandoff.Return denyReturn(String cid) {
		Instant now = Instant.now();
		return SignedHandoff.Return.of(
				cid, now, now.plusSeconds(120),
				UUID.randomUUID().toString().replace("-", ""),
				"42",
				List.of(),
				"https://dc.example/RetailCustomer/1",
				SignedHandoff.Return.CONSENT_DENY,
				null);
	}
}
