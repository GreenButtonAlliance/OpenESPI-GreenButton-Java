/*
 *
 *        Copyright (c) 2026 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.authserver.grant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * #251: each grant yields only its own kind of scope, even for a client registered for both kinds.
 */
@DisplayName("ESPI per-grant scope policy (#251)")
class EspiGrantScopePolicyTest {

	private static final String CUSTOMER_SCOPE = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";

	/** A client registered for both grants and both kinds of scope: the case Spring alone would allow. */
	private static final RegisteredClient CLIENT = RegisteredClient.withId("id-1")
			.clientId("tp-1")
			.clientSecret("{noop}x")
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.redirectUri("https://tp.example/cb")
			.scope(CUSTOMER_SCOPE)
			.scope("ThirdParty_Admin_Access")
			.scope(EspiGrantScopePolicy.REGISTRATION_RENEWAL_SCOPE)
			.build();

	private static OAuth2ClientCredentialsAuthenticationContext clientCredentials(Set<String> scopes) {
		OAuth2ClientAuthenticationToken principal =
				new OAuth2ClientAuthenticationToken(CLIENT, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "x");
		return OAuth2ClientCredentialsAuthenticationContext
				.with(new OAuth2ClientCredentialsAuthenticationToken(principal, scopes, Map.of()))
				.registeredClient(CLIENT)
				.build();
	}

	private static OAuth2AuthorizationCodeRequestAuthenticationContext authorizationRequest(Set<String> scopes) {
		OAuth2AuthorizationCodeRequestAuthenticationToken request = new OAuth2AuthorizationCodeRequestAuthenticationToken(
				"https://as.example/oauth2/authorize", CLIENT.getClientId(),
				new TestingAuthenticationToken("customer", null), "https://tp.example/cb", "state-1", scopes, Map.of());
		return OAuth2AuthorizationCodeRequestAuthenticationContext.with(request).registeredClient(CLIENT).build();
	}

	@Test
	@DisplayName("client_credentials: a customer FB scope is invalid_scope")
	void clientCredentialsRejectsCustomerScope() {
		assertThatThrownBy(() -> EspiGrantScopePolicy.clientCredentialsValidator()
				.accept(clientCredentials(Set.of(CUSTOMER_SCOPE))))
				.isInstanceOfSatisfying(OAuth2AuthenticationException.class,
						e -> assertThat(e.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_SCOPE));
	}

	@ParameterizedTest
	@ValueSource(strings = {"ThirdParty_Admin_Access", EspiGrantScopePolicy.REGISTRATION_RENEWAL_SCOPE})
	@DisplayName("client_credentials: the client access token and renewal scopes are allowed")
	void clientCredentialsAllowsAdministrativeScopes(String scope) {
		assertThatCode(() -> EspiGrantScopePolicy.clientCredentialsValidator().accept(clientCredentials(Set.of(scope))))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("client_credentials: Spring's own check still runs (an unregistered scope is invalid_scope)")
	void clientCredentialsKeepsDefaultCheck() {
		assertThatThrownBy(() -> EspiGrantScopePolicy.clientCredentialsValidator()
				.accept(clientCredentials(Set.of("DataCustodian_Admin_Access"))))
				.isInstanceOfSatisfying(OAuth2AuthenticationException.class,
						e -> assertThat(e.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_SCOPE));
	}

	@ParameterizedTest
	@ValueSource(strings = {"ThirdParty_Admin_Access", EspiGrantScopePolicy.REGISTRATION_RENEWAL_SCOPE})
	@DisplayName("authorization_code: an administrative scope is invalid_scope, redirected to the client")
	void authorizationCodeRejectsAdministrativeScope(String scope) {
		assertThatThrownBy(() -> EspiGrantScopePolicy.authorizationCodeValidator()
				.accept(authorizationRequest(Set.of(CUSTOMER_SCOPE, scope))))
				.isInstanceOfSatisfying(OAuth2AuthorizationCodeRequestAuthenticationException.class, e -> assertThat(e)
						.satisfies(ex -> assertThat(ex.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_SCOPE))
						.satisfies(ex -> assertThat(ex.getAuthorizationCodeRequestAuthentication().getRedirectUri())
								.isEqualTo("https://tp.example/cb"))
						.satisfies(ex -> assertThat(ex.getAuthorizationCodeRequestAuthentication().getState())
								.isEqualTo("state-1")));
	}

	@Test
	@DisplayName("authorization_code: a customer FB scope is allowed")
	void authorizationCodeAllowsCustomerScope() {
		assertThatCode(() -> EspiGrantScopePolicy.authorizationCodeValidator()
				.accept(authorizationRequest(Set.of(CUSTOMER_SCOPE))))
				.doesNotThrowAnyException();
	}

	/** Spellings the Data Custodian's tolerant parser still reads as customer Function Blocks. */
	@ParameterizedTest
	@ValueSource(strings = {"IntervalDuration=3600;FB=4_5_15", " FB=4_5_15", "fb=4_5_15", "FB =4", "Foo;FB=1_3_4"})
	@DisplayName("client_credentials: any spelling of a customer scope is invalid_scope (allowlist, fails closed)")
	void clientCredentialsRejectsAnySpellingOfCustomerScope(String scope) {
		assertThat(EspiGrantScopePolicy.isClientCredentialsScope(scope)).isFalse();
		assertThat(EspiGrantScopePolicy.isAuthorizationCodeScope(scope)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"ThirdParty_Admin_Access", "DataCustodian_Admin_Access", "Upload_Admin_Access",
			"FB=36_40", "fb=36_40", "FB=36", "FB=40;IntervalDuration=900", "FB=4_36", "authserver.admin",
			"client.create", "FB=abc", "FB=", "FB_1", "anything_else"})
	@DisplayName("authorization_code: administrative, reserved-FB, unparseable and unknown scopes are not allowed")
	void authorizationCodeAllowlist(String scope) {
		assertThat(EspiGrantScopePolicy.isAuthorizationCodeScope(scope)).isFalse();
	}

	@Test
	@DisplayName("scope classification: each grant's allowlist")
	void scopeClassification() {
		assertThat(Set.of(CUSTOMER_SCOPE, "FB=1_3_4_5_13_14_39", "openid", "profile"))
				.allMatch(EspiGrantScopePolicy::isAuthorizationCodeScope)
				.noneMatch(EspiGrantScopePolicy::isClientCredentialsScope);
		assertThat(EspiGrantScopePolicy.ADMINISTRATIVE_SCOPES)
				.allMatch(EspiGrantScopePolicy::isClientCredentialsScope)
				.noneMatch(EspiGrantScopePolicy::isAuthorizationCodeScope);
		assertThat(Set.of(" ThirdParty_Admin_Access", "thirdparty_admin_access", "FB=36_40;", "openid"))
				.noneMatch(EspiGrantScopePolicy::isClientCredentialsScope);
	}
}
