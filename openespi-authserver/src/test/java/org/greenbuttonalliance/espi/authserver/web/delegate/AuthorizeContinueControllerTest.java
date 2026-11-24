/*
 * Copyright 2025-2026 Green Button Alliance, Inc.
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
import org.greenbuttonalliance.espi.handoff.HandoffNonceService;
import org.greenbuttonalliance.espi.handoff.InvalidHandoffException;
import org.greenbuttonalliance.espi.handoff.SignedHandoff;
import org.greenbuttonalliance.espi.handoff.SignedHandoffCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
 *       and a non-blank {@code approved_scope}. Should keep the delegation's grant context in the session
 *       under its correlation id (#305), sign no one in, save no consent, and 302 to
 *       {@code /oauth2/authorize} with the original TP params and the delegation's id.</li>
 *   <li><strong>Deny</strong> &mdash; valid return handoff with {@code consent="deny"}. Should
 *       301/302 to the third party's registered {@code redirect_uri} with
 *       {@code error=access_denied} per RFC 6749 &sect;4.1.2.1, including the TP's original
 *       {@code state}.</li>
 *   <li><strong>Invalid handoff</strong> &mdash; codec verification failure (expiry, signature,
 *       direction, or wrong nonce). Should render the uniform 400 error page with no information
 *       leak; downstream collaborators (nonce service, client repository) must not be touched.</li>
 * </ol>
 *
 * <p>Uses real codec + real {@link DelegationStateService} and {@link GrantContextSessionStore} for end-to-end
 * fidelity; nonce service, authorization service and registered-client repo are mocked.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizeContinueController")
class AuthorizeContinueControllerTest {

	private static final String SIGNING_KEY = "test-signing-key-must-be-at-least-32-chars-long";
	private static final String ESPI_SCOPE =
			"FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";
	/** Each replay value is form-encoded (#305 review), so ';' and ':' and '/' are encoded too. */
	private static final String ESPI_SCOPE_IN_URL =
			"FB%3D4_5_15%3BIntervalDuration%3D3600%3BBlockDuration%3Dmonthly%3BHistoryLength%3D34128000";
	private static final String REPLAY_PREFIX =
			"/oauth2/authorize?response_type=code&client_id=tp-1&redirect_uri=https%3A%2F%2Ftp.example%2Fcb";

	@Mock
	private HandoffNonceService nonceService;
	@Mock
	private RegisteredClientRepository registeredClientRepository;
	@Mock
	private OAuth2AuthorizationService authorizationService;

	private static final String SECURITY_CONTEXT_KEY = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

	private SignedHandoffCodec codec;
	private DelegationStateService delegationStateService;
	private GrantContextSessionStore grantContextSessionStore;
	private MockMvc mockMvc;
	private MockHttpSession session;

	@BeforeEach
	void setUp() {
		codec = new SignedHandoffCodec(SIGNING_KEY);
		delegationStateService = new DelegationStateService();
		grantContextSessionStore = new GrantContextSessionStore();
		session = new MockHttpSession();

		AuthorizeContinueController controller = new AuthorizeContinueController(
				codec, nonceService, delegationStateService, registeredClientRepository,
				grantContextSessionStore, authorizationService);

		// No custom view resolver: Spring's standalone setup handles "redirect:" directives natively
		// (via RedirectView), and view().name(...) assertions match against the controller's return
		// value before resolution — so the absence of a Thymeleaf engine doesn't matter.
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Nested
	@DisplayName("Allow path")
	class AllowPath {

		@Test
		@DisplayName("valid allow handoff: keeps the delegation's grant context and redirects to /oauth2/authorize")
		void allowRedirectsToAuthorizeWithTheDelegationsContext() throws Exception {
			String cid = "corr-allow-1";
			UUID usagePoint = UUID.randomUUID();
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1", ESPI_SCOPE);
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));

			String token = codec.encode(allowReturn(cid, ESPI_SCOPE, List.of(usagePoint)));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session).param("handoff", token))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl(REPLAY_PREFIX
							+ "&scope=" + ESPI_SCOPE_IN_URL
							+ "&state=tp-state-1&espi_delegation=" + cid));

			verify(nonceService).consume(any(), any());

			assertThat(grantContextSessionStore.peek(session, cid))
					.extracting(GrantContext::clientId, GrantContext::registeredClientId, GrantContext::principal,
							GrantContext::replay, GrantContext::retailCustomerId, GrantContext::approvedScope,
							GrantContext::selectedUsagePointIds)
					.containsExactly("tp-1", "client-internal-id-1", "42",
							new GrantContext.Replay("https://tp.example/cb", "tp-state-1", null, null),
							42L, ESPI_SCOPE, List.of(usagePoint));
			// #305: the customer is signed in only on the replay, never in the session.
			assertThat(session.getAttribute(SECURITY_CONTEXT_KEY)).isNull();

			// Delegation state is single-use
			assertThat(delegationStateService.consume(cid)).isNull();
		}

		@Test
		@DisplayName("#305: two delegations in one session each keep their own grant context")
		void twoDelegationsInOneSessionKeepTheirOwnContexts() throws Exception {
			String narrowed = "FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";
			delegationStateService.save("corr-a", "tp-1", "https://tp.example/cb", "tp-a", ESPI_SCOPE);
			delegationStateService.save("corr-b", "tp-1", "https://tp.example/cb", "tp-b", ESPI_SCOPE);
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session)
							.param("handoff", codec.encode(allowReturn("corr-a", ESPI_SCOPE, List.of()))))
					.andExpect(status().is3xxRedirection());
			mockMvc.perform(get("/oauth2/authorize/continue").session(session)
							.param("handoff", codec.encode(allowReturn("corr-b", narrowed, List.of()))))
					.andExpect(status().is3xxRedirection());

			assertThat(grantContextSessionStore.peek(session, "corr-a").approvedScope()).isEqualTo(ESPI_SCOPE);
			assertThat(grantContextSessionStore.peek(session, "corr-b").approvedScope()).isEqualTo(narrowed);
		}

		@Test
		@DisplayName("PKCE (#274): the Third Party's code_challenge and method are replayed to /oauth2/authorize")
		void allowReplaysTheThirdPartysProofKey() throws Exception {
			String cid = "corr-allow-pkce";
			String challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1", ESPI_SCOPE, null,
					DelegationStateService.ProofKey.of(challenge, "S256"));
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session)
							.param("handoff", codec.encode(allowReturn(cid, ESPI_SCOPE, List.of()))))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl(REPLAY_PREFIX
							+ "&scope=" + ESPI_SCOPE_IN_URL
							+ "&state=tp-state-1&espi_delegation=" + cid
							+ "&code_challenge=" + challenge + "&code_challenge_method=S256"));

			// Only a request with this challenge is the delegation's replay (#305 review).
			assertThat(grantContextSessionStore.peek(session, cid).replay())
					.isEqualTo(new GrantContext.Replay("https://tp.example/cb", "tp-state-1", challenge, "S256"));
		}

		@Test
		@DisplayName("narrowed approved scope (#211): kept in the grant context and replayed as the effective scope")
		void narrowedApprovedScopeIsKeptAndReplayed() throws Exception {
			String cid = "corr-allow-narrow";
			String narrowed = "FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1", ESPI_SCOPE);
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session)
							.param("handoff", codec.encode(allowReturn(cid, narrowed, List.of()))))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl(REPLAY_PREFIX
							+ "&scope=FB%3D4_5%3BIntervalDuration%3D3600%3BBlockDuration%3Dmonthly%3BHistoryLength%3D34128000"
							+ "&state=tp-state-1&espi_delegation=" + cid));

			assertThat(grantContextSessionStore.peek(session, cid).approvedScope()).isEqualTo(narrowed);
		}

		@Test
		@DisplayName("#305 review: a state and redirect_uri with '+', '/' and '=' are replayed form-encoded, never as spaces")
		void reservedCharactersAreFormEncodedInTheReplay() throws Exception {
			String cid = "corr-allow-plus";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb+x", "a+b/c==", ESPI_SCOPE);
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session)
							.param("handoff", codec.encode(allowReturn(cid, ESPI_SCOPE, List.of()))))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl("/oauth2/authorize?response_type=code&client_id=tp-1"
							+ "&redirect_uri=https%3A%2F%2Ftp.example%2Fcb%2Bx"
							+ "&scope=" + ESPI_SCOPE_IN_URL
							+ "&state=a%2Bb%2Fc%3D%3D&espi_delegation=" + cid));
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"FB=1_4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000", // adds FB 1
				"FB=4_5;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000",       // changes a parameter
				"FB=4_5_15"})                                                               // drops parameters
		@DisplayName("approved scope that is not a narrowing of the request (#211): 400, nothing kept")
		void approvedScopeThatWidensTheRequestIs400(String approved) throws Exception {
			String cid = "corr-allow-widen";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1", ESPI_SCOPE);
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session)
							.param("handoff", codec.encode(allowReturn(cid, approved, List.of()))))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			assertThat(grantContextSessionStore.peek(session, cid)).isNull();
			assertThat(session.getAttribute(SECURITY_CONTEXT_KEY)).isNull();
		}

		@Test
		@DisplayName("#305 review: an allow for a delegation without redirect_uri is a 400, nothing kept")
		void allowWithoutRedirectUriIs400() throws Exception {
			String cid = "corr-allow-no-uri";
			delegationStateService.save(cid, "tp-1", null, "tp-state-1", ESPI_SCOPE);
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session)
							.param("handoff", codec.encode(allowReturn(cid, ESPI_SCOPE, List.of()))))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			assertThat(grantContextSessionStore.peek(session, cid)).isNull();
		}

		@Test
		@DisplayName("registered client missing at /continue: 400, nothing kept")
		void clientGoneAtContinueIs400() throws Exception {
			String cid = "corr-allow-2";
			delegationStateService.save(cid, "tp-vanished", "https://tp.example/cb", "tp-state", "FB_1");
			when(registeredClientRepository.findByClientId("tp-vanished")).thenReturn(null);

			mockMvc.perform(get("/oauth2/authorize/continue").session(session)
							.param("handoff", codec.encode(allowReturn(cid, "FB_1", List.of()))))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			assertThat(grantContextSessionStore.peek(session, cid)).isNull();
		}

		@Test
		@DisplayName("a delegation from Spring's consent page (#299): its pending authorization is removed")
		void pendingConsentAuthorizationIsRemoved() throws Exception {
			String cid = "corr-allow-pending";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1", ESPI_SCOPE, "pending-id");
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));
			OAuth2Authorization pending = OAuth2Authorization.withRegisteredClient(client(ESPI_SCOPE))
					.id("pending-id")
					.principalName("42")
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.build();
			when(authorizationService.findById("pending-id")).thenReturn(pending);

			mockMvc.perform(get("/oauth2/authorize/continue")
							.param("handoff", codec.encode(allowReturn(cid, ESPI_SCOPE, List.of()))))
					.andExpect(status().is3xxRedirection());

			verify(authorizationService).remove(pending);
		}

		@Test
		@DisplayName("a delegation not from the consent page touches no authorization")
		void noPendingAuthorizationNoRemoval() throws Exception {
			String cid = "corr-allow-no-pending";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1", ESPI_SCOPE);
			when(registeredClientRepository.findByClientId("tp-1")).thenReturn(client(ESPI_SCOPE));

			mockMvc.perform(get("/oauth2/authorize/continue")
							.param("handoff", codec.encode(allowReturn(cid, ESPI_SCOPE, List.of()))))
					.andExpect(status().is3xxRedirection());

			verifyNoInteractions(authorizationService);
		}
	}

	@Nested
	@DisplayName("Deny path")
	class DenyPath {

		@Test
		@DisplayName("valid deny handoff: 302 to TP redirect_uri with error=access_denied + state, nothing kept")
		void denyRedirectsToTpWithAccessDenied() throws Exception {
			String cid = "corr-deny-1";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1", ESPI_SCOPE);
			String token = codec.encode(denyReturn(cid));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session).param("handoff", token))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(
							"https://tp.example/cb?error=access_denied&error_description=*&state=tp-state-1"));

			verify(nonceService).consume(any(), any());
			assertThat(grantContextSessionStore.peek(session, cid)).isNull();
		}

		@ParameterizedTest(name = "state ''{0}'' -> {1}")
		@org.junit.jupiter.params.provider.CsvSource({"a+b/c==, a%2Bb%2Fc%3D%3D", "'{x}', %7Bx%7D", "a b, a%20b"})
		@DisplayName("#305 review: deny sends the TP's state strictly encoded: '+' is never read back as a space, '{' is no template")
		void denyEncodesTheStateStrictly(String tpState, String encoded) throws Exception {
			String cid = "corr-deny-encoded";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", tpState, ESPI_SCOPE);

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", codec.encode(denyReturn(cid))))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl("https://tp.example/cb?error=access_denied"
							+ "&error_description=The%20customer%20did%20not%20authorize%20the%20request.&state=" + encoded));
		}

		@Test
		@DisplayName("deny with no tpState: redirects without state param")
		void denyWithoutStateOmitsStateParam() throws Exception {
			String cid = "corr-deny-2";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", null, ESPI_SCOPE);
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
			delegationStateService.save(cid, "tp-no-uri", null, "tp-state", ESPI_SCOPE);
			String token = codec.encode(denyReturn(cid));

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));
		}

		@Test
		@DisplayName("allow with blank approved_scope falls through to deny semantics")
		void allowWithBlankApprovedScopeIsDeny() throws Exception {
			String cid = "corr-deny-4";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state-1", ESPI_SCOPE);
			String token = codec.encode(allowReturn(cid, "", List.of()));

			mockMvc.perform(get("/oauth2/authorize/continue").session(session).param("handoff", token))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(
							"https://tp.example/cb?error=access_denied&error_description=*&state=tp-state-1"));

			assertThat(grantContextSessionStore.peek(session, cid)).isNull();
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

			verifyNoInteractions(nonceService, registeredClientRepository);
		}

		@Test
		@DisplayName("expired token: 400")
		void expiredTokenIs400() throws Exception {
			// Manually craft an expired payload
			Instant past = Instant.now().minusSeconds(3600);
			SignedHandoff.Return expired = SignedHandoff.Return.of(
					"corr-expired", past.minusSeconds(600), past, "nonce-x",
					"42", List.of(),
					SignedHandoff.Return.CONSENT_ALLOW, "FB_1");
			String token = codec.encode(expired);

			mockMvc.perform(get("/oauth2/authorize/continue").param("handoff", token))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			verifyNoInteractions(nonceService, registeredClientRepository);
		}

		@Test
		@DisplayName("nonce replay: nonceService throws InvalidHandoffException → 400")
		void nonceReplayIs400() throws Exception {
			String cid = "corr-replay";
			delegationStateService.save(cid, "tp-1", "https://tp.example/cb", "tp-state", "FB_1");
			String token = codec.encode(allowReturn(cid, "FB_1", List.of()));

			org.mockito.Mockito.doThrow(new InvalidHandoffException("nonce replay"))
					.when(nonceService).consume(any(), any());

			mockMvc.perform(get("/oauth2/authorize/continue").session(session).param("handoff", token))
					.andExpect(status().isBadRequest())
					.andExpect(view().name("error/400"));

			assertThat(grantContextSessionStore.peek(session, cid)).isNull();
			verifyNoInteractions(registeredClientRepository);
		}

		@Test
		@DisplayName("missing delegation state: 400")
		void missingStateIs400() throws Exception {
			// Don't save any state — codec decode succeeds, but state lookup fails
			String token = codec.encode(allowReturn("corr-unknown", "FB_1", List.of()));

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

	private static RegisteredClient client(String... scopes) {
		RegisteredClient.Builder builder = RegisteredClient.withId("client-internal-id-1")
				.clientId("tp-1")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("https://tp.example/cb");
		for (String scope : scopes) {
			builder.scope(scope);
		}
		return builder.build();
	}

	private static SignedHandoff.Return allowReturn(String cid, String approvedScope, List<UUID> usagePoints) {
		Instant now = Instant.now();
		return SignedHandoff.Return.of(
				cid, now, now.plusSeconds(120),
				UUID.randomUUID().toString().replace("-", ""),
				"42",
				usagePoints,
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
				SignedHandoff.Return.CONSENT_DENY,
				null);
	}
}
