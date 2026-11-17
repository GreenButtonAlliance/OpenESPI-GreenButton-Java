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

import org.greenbuttonalliance.espi.thirdparty.oauth.DataCustodianRegistrations;
import org.greenbuttonalliance.espi.thirdparty.oauth.EspiAuthorizationOAuth2AuthorizedClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * A TP customer authorizing the Third Party at a Data Custodian (#146), with Spring Security's
 * {@code oauth2Client}: the home page's cards start the authorization-code flow, and the customer
 * comes back here when it has finished.
 */
@Controller
public class OAuth2ClientController {

	private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientController.class);

	private final EspiAuthorizationOAuth2AuthorizedClientService authorizedClientService;

	public OAuth2ClientController(EspiAuthorizationOAuth2AuthorizedClientService authorizedClientService) {
		this.authorizedClientService = authorizedClientService;
	}

	/** Monthly usage data, 1-hour intervals (the home page's "Monthly Data" card). */
	@GetMapping("/oauth2/authorize/monthly")
	public String authorizeMonthly() {
		return "redirect:/oauth2/authorization/" + DataCustodianRegistrations.MONTHLY;
	}

	/** Daily usage data, 15-minute intervals (the home page's "Daily Data" card). */
	@GetMapping("/oauth2/authorize/daily")
	public String authorizeDaily() {
		return "redirect:/oauth2/authorization/" + DataCustodianRegistrations.DAILY;
	}

	/**
	 * Where the customer lands after the Data Custodian: {@code OAuth2AuthorizationCodeGrantFilter}
	 * has handled the callback (exchanged the code and stored the token) and redirects here again,
	 * without parameters on success and with {@code ?error=...} when consent was refused or the
	 * exchange failed. A callback the filter did not recognise (no authorization in progress in this
	 * session, or a forged one) also arrives here, still carrying {@code code} and {@code state}.
	 * Success is reported only if a token is actually stored for this customer.
	 */
	@GetMapping("/login/oauth2/code/{registrationId}")
	public String authorizationFinished(@PathVariable String registrationId,
										@RequestParam Map<String, String> parameters,
										Authentication authentication) {
		boolean callbackParameters = parameters.containsKey(OAuth2ParameterNames.ERROR)
				|| parameters.containsKey(OAuth2ParameterNames.CODE) || parameters.containsKey(OAuth2ParameterNames.STATE);
		if (!callbackParameters
				&& authorizedClientService.loadAuthorizedClient(registrationId, authentication.getName()) != null) {
			return "redirect:/?authorized";
		}
		logger.info("Data Custodian authorization for {} did not complete: error={}", registrationId,
				parameters.getOrDefault(OAuth2ParameterNames.ERROR, "(none, no token stored)"));
		return "redirect:/?authorization_error";
	}
}
