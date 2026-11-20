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

package org.greenbuttonalliance.espi.thirdparty.web;

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.thirdparty.oauth.DataCustodianRegistrations;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * One stored Data Custodian authorization (#146) as the TP pages show it. Never carries a token
 * value except {@link #tokenPrefix}, the first characters of the access token, for the administrator's
 * client access token check.
 *
 * @param id           the authorization's id, for the Subscription page link (#89)
 * @param active       the status is ACTIVE; an expired access token still counts, since fetching refreshes it
 * @param authorizeUrl the home page's authorize button for this registration ("Authorize again"), or null
 */
public record AuthorizationView(String id, String access, String status, boolean active, String scope,
								String expires, String obtained, String tokenPrefix, String resourceUri,
								String authorizationUri, String customerResourceUri, String authorizeUrl) {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
			.withZone(ZoneId.systemDefault());
	private static final int TOKEN_PREFIX_LENGTH = 8;

	/**
	 * The home page's authorize button for a customer registration (OAuth2ClientController), or null for
	 * any other registration.
	 */
	public static String authorizeUrl(String registrationId) {
		if (DataCustodianRegistrations.MONTHLY.equals(registrationId)) {
			return "/oauth2/authorize/monthly";
		}
		if (DataCustodianRegistrations.DAILY.equals(registrationId)) {
			return "/oauth2/authorize/daily";
		}
		return null;
	}

	/**
	 * The status is ACTIVE. Unlike {@link AuthorizationEntity#isActive()}, an expired access token does not
	 * make it false: a fetch refreshes an expired access token.
	 */
	public static boolean isStatusActive(AuthorizationEntity authorization) {
		return AuthorizationEntity.STATUS_ACTIVE.equals(authorization.getStatus());
	}

	static AuthorizationView of(AuthorizationEntity authorization, ClientRegistrationRepository registrations) {
		ClientRegistration registration = authorization.getClientRegistrationId() == null ? null
				: registrations.findByRegistrationId(authorization.getClientRegistrationId());
		String access = registration == null ? authorization.getClientRegistrationId() : registration.getClientName();
		Instant expiresAt = authorization.getExpirationInstant();
		String expires = expiresAt == null ? "-" : TIME.format(expiresAt);
		String obtained = authorization.getUpdated() == null ? "-"
				: TIME.format(authorization.getUpdated().atZone(ZoneId.systemDefault()));
		String token = authorization.getAccessToken();
		String tokenPrefix = token == null ? null
				: token.substring(0, Math.min(TOKEN_PREFIX_LENGTH, token.length())) + "…";
		String id = authorization.getId() == null ? null : authorization.getId().toString();
		return new AuthorizationView(id, access, authorization.getStatus(), isStatusActive(authorization),
				authorization.getScope(), expires, obtained, tokenPrefix, authorization.getResourceURI(),
				authorization.getAuthorizationURI(), authorization.getCustomerResourceURI(),
				authorizeUrl(authorization.getClientRegistrationId()));
	}
}
