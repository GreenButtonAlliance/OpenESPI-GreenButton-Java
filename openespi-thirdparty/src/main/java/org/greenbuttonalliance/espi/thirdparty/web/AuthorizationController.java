/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.common.GrantType;
import org.greenbuttonalliance.espi.common.domain.common.TokenType;
import org.greenbuttonalliance.espi.common.domain.common.OAuthError;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationEntityRepository;
import org.greenbuttonalliance.espi.thirdparty.repository.UsagePointRESTRepository;
import org.greenbuttonalliance.espi.thirdparty.service.WebClientService;
import org.greenbuttonalliance.espi.thirdparty.dto.AccessTokenDto;
import org.greenbuttonalliance.espi.thirdparty.exception.UserDeniedAuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import jakarta.persistence.NoResultException;
import jakarta.xml.bind.JAXBException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.GregorianCalendar;
import java.util.UUID;

@Controller
public class AuthorizationController {

	private static final Logger logger = LoggerFactory.getLogger(AuthorizationController.class);

	@Autowired
	private AuthorizationService authorizationService;
	
	@Autowired
	private AuthorizationEntityRepository authorizationRepository;
	
	@Autowired
	private RetailCustomerService retailCustomerService;

	@Autowired
	private UsagePointRESTRepository usagePointRESTRepository;

	@Autowired
	private WebClientService webClientService;

	@Autowired
	private ClientWebClientFactory clientWebClientFactory;
	
	@Autowired
	private RestTemplate restTemplate;

	@GetMapping("/oauth/callback")
	public String authorization(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			ModelMap model,
			Principal principal,
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "error_description", required = false) String error_description,
			@RequestParam(value = "error_uri", required = false) String error_uri) {

		try {

			// Is /oauth/authorization response valid (i.e. is the "state"
			// element correct)?
			AuthorizationEntity authorization = authorizationService
					.findByState(state);

			// Process valid /oauth/authorization response
			ApplicationInformationEntity applicationInformation = authorization
					.getApplicationInformation();

			// Verify /oauth/authorization Endpoint process completed
			// successfully
			if (code != null) {
				try {

					// Update Authorization record with returned authorization
					// code for audit purposes
					authorization.setCode(code);
					authorization.setGrantType(GrantType.AUTHORIZATION_CODE);
					authorization.setUpdated(LocalDateTime.now());
					authorizationService.merge(authorization);

					// Format /oauth/token Endpoint request
					String url = String
							.format("%s?redirect_uri=%s&code=%s&grant_type=authorization_code",
									applicationInformation
											.getAuthorizationServerTokenEndpoint(),
									applicationInformation.getRedirectUri(),
									code);

					// Build /oauth/token Endpoint request
					ClientWebClient webClient = clientWebClientFactory
							.newClientWebClient(
									applicationInformation.getClientId(),
									applicationInformation.getClientSecret());

					// Issue /oauth/token Endpoint request
					AccessTokenDto token = restTemplate.getForObject(url,
							AccessTokenDto.class);

					// Process /oauth/token Endpoint response

					if (token.getAccessToken() != null) {
						authorization.setAccessToken(token.getAccessToken());
						authorization.setTokenType(TokenType.fromValue(token.getTokenType()));
						authorization.setExpiresIn(token.getExpiresIn());
						authorization.setRefreshToken(token.getRefreshToken());
						authorization.setScope(token.getScope());
						authorization.setAuthorizationURI(token
								.getAuthorizationURI());
						authorization.setResourceURI(token.getResourceURI());
						authorization.setUpdated(LocalDateTime.now());
						authorization.setStatus("1"); // Set authorization
														// record status as
														// "Active"
						authorization.setState(null); // Clear State as a
														// security measure

						// Update authorization record with /oauth/token
						// response data
						authorizationService.merge(authorization);

						// now do the initial import of the Authorized Resource,
						// if it is
						// not ready, then we will wait till we receive a Notify
						// or the UX call for it.
						// TODO: create a Subscription to work with if needed

						RetailCustomerEntity currentCustomer = getCurrentCustomer(principal);

						try {
							usagePointRESTRepository
									.findAllByRetailCustomerId((long) currentCustomer
											.getId().hashCode());

						} catch (JAXBException e) {
							// nothing there, so log the fact and move on. It
							// will get imported later.
							System.out.printf(
									"\nThirdParty Import Exception: %s\n",
									e.toString());
							e.printStackTrace();
						}
					} else {

						System.out
								.printf("\n/oauth/token Request did not return an access token\n");
					}

				} catch (HttpClientErrorException x) {

					// TODO: Extract error, error_description and error_uri from
					// JSON response. Currently recording null for all three
					// fields.

					// Update authorization record
					System.out.printf("\nHTTPClientException: %s\n",
							x.toString());

					authorization.setError(error != null ? OAuthError.fromValue(error) : null);
					authorization.setErrorDescription(error_description);
					authorization.setErrorUri(error_uri);
					authorization.setUpdated(LocalDateTime.now());
					authorization.setStatus("2"); // Set authorization record
													// status as "Denied"
					authorization.setState(null); // Clear State as a security
													// measure
					authorizationService.merge(authorization);

					// TODO: Should the "message" differ based on the exception?
					throw new UserDeniedAuthorizationException(
							"Unable to retrieve OAuth token", x);
				}
			} else {

				System.out
						.printf("\nOAuth2 authorization_request returned an error:\n");
				System.out.printf("Error:             " + error + "\n");
				System.out.printf("Error_description: " + error_description
						+ "\n");
				System.out.printf("Error_uri:         " + error_uri + "\n");

				// Update authorization record with error response
				authorization.setError(error != null ? OAuthError.fromValue(error) : null);
				authorization.setErrorDescription(error_description);
				authorization.setErrorUri(error_uri);
				authorization.setUpdated(LocalDateTime.now());
				authorization.setStatus("2"); // Set authorization record status
												// as "Denied"
				authorization.setState(null); // Clear State as a security
												// measure
				authorizationService.merge(authorization);

				throw new UserDeniedAuthorizationException("Error: "
						+ error_description);

			}

		} catch (NoResultException | EmptyResultDataAccessException e) {

			// We received an invalid /oauth/authorization response
			// TODO: Log receipt of an invalid /oauth/authorization response
			return "/home";

		}

		return "redirect:/RetailCustomer/" + getCurrentCustomer(principal).getId()
				+ "/AuthorizationList";
	}

	@RequestMapping(value = "/authorization", method = RequestMethod.GET)
	public String index(ModelMap model, Authentication principal) {
		model.put(
				"authorizationList",
				authorizationRepository.findAllByRetailCustomerId(getCurrentCustomer(
						principal).getId()));
		return "/RetailCustomer/AuthorizationList/index";
	}

	public void setAuthorizationService(
			AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	public AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	public void setUsagePointRESTRepository(
			UsagePointRESTRepository usagePointRESTRepository) {
		this.usagePointRESTRepository = usagePointRESTRepository;
	}

	public UsagePointRESTRepository getUsagePointRESTRepository() {
		return this.usagePointRESTRepository;
	}

	public void setClientWebClientFactory(
			ClientWebClientFactory clientWebClientFactory) {
		this.clientWebClientFactory = clientWebClientFactory;
	}

	public ClientWebClientFactory getClientWebClientFactory() {
		return this.clientWebClientFactory;
	}

	private RetailCustomerEntity getCurrentCustomer(Principal principal) {
		try {
			return retailCustomerService.findByUsername(principal.getName());
		} catch (Exception e) {
			throw new RuntimeException("Unable to find current customer: " + principal.getName(), e);
		}
	}

}
