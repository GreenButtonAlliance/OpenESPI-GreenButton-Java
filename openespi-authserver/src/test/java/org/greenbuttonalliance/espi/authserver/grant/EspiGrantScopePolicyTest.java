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
			.scope(EspiGrantScopePolicy.CLIENT_ACCESS_TOKEN_SCOPE)
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
	@ValueSource(strings = {EspiGrantScopePolicy.CLIENT_ACCESS_TOKEN_SCOPE, EspiGrantScopePolicy.REGISTRATION_RENEWAL_SCOPE,
			"ThirdParty_Admin_Access"})
	@DisplayName("client_credentials: the client access token (FB=34_35) and renewal (FB=36_40) scopes are allowed")
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
	@ValueSource(strings = {"ThirdParty_Admin_Access", EspiGrantScopePolicy.CLIENT_ACCESS_TOKEN_SCOPE,
			EspiGrantScopePolicy.REGISTRATION_RENEWAL_SCOPE})
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

	@ParameterizedTest
	@ValueSource(strings = {"FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
			"FB=15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"})
	@DisplayName("authorization_code (#211): a narrowing of a registered scope is allowed, no prior consent needed")
	void authorizationCodeAllowsNarrowing(String narrowed) {
		assertThatCode(() -> EspiGrantScopePolicy.authorizationCodeValidator()
				.accept(authorizationRequest(Set.of(narrowed))))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {"FB=1_4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
			"FB=4_5;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13", "FB=4_5"})
	@DisplayName("authorization_code (#211): a scope that widens or changes the registered scope is invalid_scope")
	void authorizationCodeRejectsNonNarrowing(String scope) {
		assertThatThrownBy(() -> EspiGrantScopePolicy.authorizationCodeValidator()
				.accept(authorizationRequest(Set.of(scope))))
				.isInstanceOfSatisfying(OAuth2AuthorizationCodeRequestAuthenticationException.class,
						e -> assertThat(e.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_SCOPE));
	}

	@ParameterizedTest
	@ValueSource(strings = {"FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
			"FB=15;historylength=13;IntervalDuration=3600; BlockDuration=monthly", CUSTOMER_SCOPE})
	@DisplayName("isNarrowingOf (#211): a subset of the Function Blocks with the same other parameters")
	void narrowings(String candidate) {
		assertThat(EspiGrantScopePolicy.isNarrowingOf(candidate, CUSTOMER_SCOPE)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"FB=1_4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
			"FB=4_5;IntervalDuration=900;BlockDuration=monthly;HistoryLength=13",
			"FB=4_5;IntervalDuration=3600;BlockDuration=monthly", "FB=4_5_15",
			"FB=4_5;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13;Extra=1",
			"FB=;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13", "FB=x", ""})
	@DisplayName("isNarrowingOf (#211): an added Function Block, a changed, missing or extra parameter, or no FB is not")
	void notNarrowings(String candidate) {
		assertThat(EspiGrantScopePolicy.isNarrowingOf(candidate, CUSTOMER_SCOPE)).isFalse();
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
			"FB=36_40", "fb=36_40", "FB=40_36", "FB=36_40;IntervalDuration=900", "FB=34_35", "FB=35_34", " fb=34_35",
			"FB=34_35;BR=000112", "authserver.admin", "client.create", "FB=abc", "FB=", "FB_1", "anything_else"})
	@DisplayName("authorization_code: administrative, client/registration token (any spelling), unparseable and unknown scopes are not allowed")
	void authorizationCodeAllowlist(String scope) {
		assertThat(EspiGrantScopePolicy.isAuthorizationCodeScope(scope)).isFalse();
	}

	/** Customer scopes as certified Third Parties send them: bulk (35) and offline (40) among other FBs. */
	@ParameterizedTest
	@ValueSource(strings = {
			"FB=1_3_4_5_6_7_8_9_13_15_16_17_27_28_31_35_37_39_40_51_53_54_55_56_57_58_59_60_61_62_64_65_67_69_70",
			"FB=1_3_4_5_7_10_15_16_51_53_56_57_58_60;IntervalDuration=Monthly_3600_900_300;BlockDuration=Monthly_Daily",
			"FB=1_3_4_5_6_7_8_9_10_11_12_13_15_16_17_26_27_28_29_31_35_37_39_40_51_53_54_55_56_57_58_59_60_61_62_64_65_67_69_70_71",
			"FB=35", "FB=40;IntervalDuration=900", "FB=4_5_15"})
	@DisplayName("authorization_code: scopes naming only certifiable customer data Function Blocks (CMD v2.14) are allowed")
	void authorizationCodeAllowsCertifiableCustomerScopes(String scope) {
		assertThat(EspiGrantScopePolicy.isAuthorizationCodeScope(scope)).isTrue();
	}

	/** Function Blocks outside the CMD v2.14 certification list, alone or among valid ones. */
	@ParameterizedTest
	@ValueSource(strings = {"FB=1_3_4_5_13_14_39", "FB=1_3_34_35_51", "FB=1_3_36_40", "FB=36", "FB=4_36", "FB=68",
			"FB=2", "FB=18_19", "FB=1_3_4_5_66"})
	@DisplayName("authorization_code: a Function Block outside the certifiable customer data list is invalid_scope")
	void authorizationCodeRejectsNonCertifiableFunctionBlocks(String scope) {
		assertThat(EspiGrantScopePolicy.isAuthorizationCodeScope(scope)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Upload_Admin_Access", "FB=35", "FB=34_35;BR=000112", "FB=35_34"})
	@DisplayName("client_credentials: only the exact client access token string; Upload_Admin_Access is not migrated")
	void clientCredentialsRejectsOtherScopes(String scope) {
		assertThat(EspiGrantScopePolicy.isClientCredentialsScope(scope)).isFalse();
	}

	@Test
	@DisplayName("scope classification: each grant's allowlist")
	void scopeClassification() {
		assertThat(Set.of(CUSTOMER_SCOPE, "FB=1_3_4_5_13_15_39", "openid", "profile"))
				.allMatch(EspiGrantScopePolicy::isAuthorizationCodeScope)
				.noneMatch(EspiGrantScopePolicy::isClientCredentialsScope);
		assertThat(EspiGrantScopePolicy.ADMINISTRATIVE_SCOPES)
				.allMatch(EspiGrantScopePolicy::isClientCredentialsScope)
				.noneMatch(EspiGrantScopePolicy::isAuthorizationCodeScope);
		assertThat(Set.of(" ThirdParty_Admin_Access", "thirdparty_admin_access", "FB=36_40;", "openid"))
				.noneMatch(EspiGrantScopePolicy::isClientCredentialsScope);
	}
}
