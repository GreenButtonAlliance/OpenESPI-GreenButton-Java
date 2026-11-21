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

package org.greenbuttonalliance.espi.authserver.web.delegate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SingleUseCustomerAuthorizationResponseHandler (#299, #276)")
class SingleUseCustomerAuthorizationResponseHandlerTest {

	private static final String SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000";
	private static final String CONTEXT = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

	private final RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
			.clientId("tp-1")
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("https://tp.example/cb")
			.scope(SCOPE)
			.build();
	private final InMemoryOAuth2AuthorizationConsentService consents = new InMemoryOAuth2AuthorizationConsentService();
	private final SingleUseCustomerAuthorizationResponseHandler handler =
			new SingleUseCustomerAuthorizationResponseHandler(consents, new InMemoryRegisteredClientRepository(client));

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private static Authentication signedIn(String name, String authority) {
		return UsernamePasswordAuthenticationToken.authenticated(name, null, List.of(new SimpleGrantedAuthority(authority)));
	}

	private static OAuth2AuthorizationCodeRequestAuthenticationToken minted(Authentication principal, String state) {
		OAuth2AuthorizationCode code = new OAuth2AuthorizationCode("the-code", Instant.now(), Instant.now().plusSeconds(300));
		return new OAuth2AuthorizationCodeRequestAuthenticationToken("https://as.example/oauth2/authorize", "tp-1",
				principal, code, "https://tp.example/cb", state, Set.of(SCOPE));
	}

	/** A request whose session holds {@code principal}'s sign-in, as after /oauth2/authorize/continue. */
	private static MockHttpServletRequest sessionOf(Authentication principal) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		SecurityContext context = new SecurityContextImpl(principal);
		request.getSession(true).setAttribute(CONTEXT, context);
		SecurityContextHolder.setContext(context);
		return request;
	}

	@Test
	@DisplayName("a customer's code: the consent is removed, the sign-in is cleared, and the code goes to the Third Party")
	void customerCodeRemovesTheConsentAndTheSignIn() throws Exception {
		Authentication customer = signedIn("42", "ROLE_CUSTOMER");
		consents.save(OAuth2AuthorizationConsent.withId(client.getId(), "42").scope(SCOPE).build());
		MockHttpServletRequest request = sessionOf(customer);
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationSuccess(request, response, minted(customer, "tp state+&="));

		assertThat(response.getRedirectedUrl()).isEqualTo("https://tp.example/cb?code=the-code&state=tp%20state%2B%26%3D");
		assertThat(consents.findById(client.getId(), "42")).isNull();
		assertThat(request.getSession().getAttribute(CONTEXT)).isNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("a code without state is sent without state")
	void noStateNoStateParameter() throws Exception {
		Authentication customer = signedIn("42", "ROLE_CUSTOMER");
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationSuccess(sessionOf(customer), response, minted(customer, null));

		assertThat(response.getRedirectedUrl()).isEqualTo("https://tp.example/cb?code=the-code");
	}

	@Test
	@DisplayName("a non-customer (an AS administrator): the sign-in and the consent are left as they are")
	void nonCustomerSignInAndConsentAreKept() throws Exception {
		Authentication admin = signedIn("admin", "ROLE_ADMIN");
		consents.save(OAuth2AuthorizationConsent.withId(client.getId(), "admin").scope(SCOPE).build());
		MockHttpServletRequest request = sessionOf(admin);

		handler.onAuthenticationSuccess(request, new MockHttpServletResponse(), minted(admin, "s"));

		assertThat(request.getSession().getAttribute(CONTEXT))
				.isInstanceOfSatisfying(SecurityContext.class, c -> assertThat(c.getAuthentication()).isSameAs(admin));
		assertThat(consents.findById(client.getId(), "admin")).isNotNull();
	}

	private static OAuth2AuthorizationCodeRequestAuthenticationException refused(Authentication principal,
																				 String redirectUri) {
		OAuth2AuthorizationCodeRequestAuthenticationToken request = new OAuth2AuthorizationCodeRequestAuthenticationToken(
				"https://as.example/oauth2/authorize", "tp-1", principal, redirectUri, "tp state", Set.of(SCOPE),
				Map.of());
		return new OAuth2AuthorizationCodeRequestAuthenticationException(
				new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE, "scope not allowed", null), request);
	}

	@Test
	@DisplayName("a customer's replay ending in an error: the consent and the sign-in are removed, the error goes to the Third Party")
	void customerErrorRemovesTheConsentAndTheSignIn() throws Exception {
		Authentication customer = signedIn("42", "ROLE_CUSTOMER");
		consents.save(OAuth2AuthorizationConsent.withId(client.getId(), "42").scope(SCOPE).build());
		MockHttpServletRequest request = sessionOf(customer);
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationFailure(request, response, refused(customer, "https://tp.example/cb"));

		assertThat(response.getRedirectedUrl()).isEqualTo(
				"https://tp.example/cb?error=invalid_scope&error_description=scope%20not%20allowed&state=tp%20state");
		assertThat(consents.findById(client.getId(), "42")).isNull();
		assertThat(request.getSession().getAttribute(CONTEXT)).isNull();
	}

	@Test
	@DisplayName("an error without a redirect_uri is a 400, as Spring's default")
	void errorWithoutRedirectUriIs400() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
				List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

		handler.onAuthenticationFailure(new MockHttpServletRequest(), response, refused(anonymous, null));

		assertThat(response.getStatus()).isEqualTo(400);
	}
}
