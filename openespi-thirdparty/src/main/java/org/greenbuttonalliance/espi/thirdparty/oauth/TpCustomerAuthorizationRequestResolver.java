/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.thirdparty.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Starts a Data Custodian authorization ({@code /oauth2/authorization/{registrationId}}) only for a
 * signed-in TP customer ({@code ROLE_USER}) and only for an authorization_code registration (#146).
 *
 * <p>Spring's {@code OAuth2AuthorizationRequestRedirectFilter} runs before the authorization rules,
 * so they cannot stop it. Returning {@code null} here lets the request through to them instead: an
 * anonymous visitor is sent to the login page (and back here afterwards), a TP administrator gets
 * 403, and a client_credentials registration ({@code datacustodian-admin}) is not a redirect at all,
 * where Spring's resolver would answer 500.</p>
 */
public class TpCustomerAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

	public static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

	private static final String REGISTRATION_ID = "registrationId";

	private final ClientRegistrationRepository clientRegistrationRepository;
	private final DefaultOAuth2AuthorizationRequestResolver delegate;
	private final RequestMatcher authorizationRequestMatcher = PathPatternRequestMatcher.withDefaults()
			.matcher(AUTHORIZATION_BASE_URI + "/{" + REGISTRATION_ID + "}");

	public TpCustomerAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, AUTHORIZATION_BASE_URI);
		// No PKCE for now. Spring Security 7 adds it by default (a ClientRegistration's requireProofKey
		// defaults to true for authorization_code), but the AS replays /oauth2/authorize after the Data
		// Custodian's consent without code_challenge, so the code exchange then fails with invalid_grant;
		// the AS registers third_party with requireProofKey(false) ("ESPI does not use PKCE", #148).
		// Restore PKCE once the AS carries code_challenge through the delegation (#274).
		this.delegate.setAuthorizationRequestCustomizer(builder -> builder
				.attributes(attributes -> attributes.remove(PkceParameterNames.CODE_VERIFIER))
				.additionalParameters(parameters -> {
					parameters.remove(PkceParameterNames.CODE_CHALLENGE);
					parameters.remove(PkceParameterNames.CODE_CHALLENGE_METHOD);
				}));
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
		RequestMatcher.MatchResult match = authorizationRequestMatcher.matcher(request);
		return match.isMatch() ? resolve(request, match.getVariables().get(REGISTRATION_ID)) : null;
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
		if (clientRegistrationId == null || !isTpCustomer()) {
			return null;
		}
		ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
		if (registration == null
				|| !AuthorizationGrantType.AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType())) {
			return null;
		}
		return delegate.resolve(request, clientRegistrationId);
	}

	private static boolean isTpCustomer() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream()
				.anyMatch(authority -> RetailCustomerEntity.ROLE_USER.equals(authority.getAuthority()));
	}
}
