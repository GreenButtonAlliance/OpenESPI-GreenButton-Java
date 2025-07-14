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
import org.greenbuttonalliance.espi.thirdparty.repository.UsagePointRESTRepository;
import org.greenbuttonalliance.espi.thirdparty.service.WebClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.NoResultException;
import jakarta.xml.bind.JAXBException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Modern authorization controller using WebClient and Spring Boot 3.5 patterns.
 * 
 * Handles OAuth2 authorization code flow for Green Button third party applications.
 */
@Controller
public class ModernAuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(ModernAuthorizationController.class);

    @Autowired
    private AuthorizationService authorizationService;
    
    @Autowired
    private RetailCustomerService retailCustomerService;

    @Autowired
    private UsagePointRESTRepository usagePointRESTRepository;

    @Autowired
    private WebClientService webClientService;

    /**
     * Handles OAuth2 authorization callback from Data Custodian.
     * Modern replacement for legacy RestTemplate usage.
     */
    @GetMapping("/oauth/callback")
    public String authorizationCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            ModelMap model,
            Principal principal,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            @RequestParam(value = "error_uri", required = false) String errorUri) {

        try {
            // Validate OAuth state parameter
            AuthorizationEntity authorization = authorizationService.findByState(state);
            if (authorization == null) {
                logger.warn("Invalid OAuth state parameter received: {}", state);
                return "redirect:/home?error=invalid_state";
            }

            ApplicationInformationEntity applicationInfo = authorization.getApplicationInformation();

            // Handle OAuth2 error responses
            if (error != null) {
                logger.warn("OAuth2 authorization error: {} - {}", error, errorDescription);
                handleAuthorizationError(authorization, error, errorDescription, errorUri);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Authorization failed: " + errorDescription);
            }

            // Handle successful authorization with code
            if (code != null) {
                processAuthorizationCode(authorization, applicationInfo, code, principal);
                return "redirect:/RetailCustomer/" + getCurrentCustomerId(principal) + "/AuthorizationList";
            } else {
                logger.error("No authorization code received in callback");
                return "redirect:/home?error=no_code";
            }

        } catch (NoResultException | EmptyResultDataAccessException e) {
            logger.warn("Invalid authorization state received: {}", state);
            return "redirect:/home?error=invalid_request";
        } catch (Exception e) {
            logger.error("Error processing authorization callback", e);
            return "redirect:/home?error=server_error";
        }
    }

    /**
     * Shows authorization list for the current retail customer.
     */
    @GetMapping("/authorizations")
    public String authorizationList(ModelMap model, Principal principal) {
        try {
            Long customerId = getCurrentCustomerId(principal);
            var authorizations = authorizationService.findAllByRetailCustomerId(customerId);
            model.put("authorizationList", authorizations);
            return "/RetailCustomer/AuthorizationList/index";
        } catch (Exception e) {
            logger.error("Error loading authorization list", e);
            return "redirect:/home?error=server_error";
        }
    }

    /**
     * Processes the authorization code by exchanging it for an access token.
     * Uses modern WebClient instead of legacy RestTemplate.
     */
    private void processAuthorizationCode(AuthorizationEntity authorization, 
                                        ApplicationInformationEntity applicationInfo, 
                                        String code, 
                                        Principal principal) {
        try {
            // Update authorization record with code
            authorization.setCode(code);
            authorization.setGrantType(GrantType.AUTHORIZATION_CODE);
            authorization.setUpdated(LocalDateTime.now());
            authorizationService.merge(authorization);

            // Create authenticated WebClient for token exchange
            WebClient authenticatedClient = webClientService.createAuthenticatedClient(
                applicationInfo.getClientId(), 
                applicationInfo.getClientSecret()
            );

            // Build token endpoint URL
            String tokenUrl = String.format("%s?redirect_uri=%s&code=%s&grant_type=authorization_code",
                applicationInfo.getAuthorizationServerTokenEndpoint(),
                applicationInfo.getRedirectUri(),
                code);

            logger.debug("Requesting access token from: {}", 
                applicationInfo.getAuthorizationServerTokenEndpoint());

            // Exchange authorization code for access token using WebClient
            Map<String, Object> tokenResponse = webClientService.getForObject(
                authenticatedClient, tokenUrl, Map.class);

            if (tokenResponse != null && tokenResponse.containsKey("access_token")) {
                updateAuthorizationWithToken(authorization, tokenResponse);
                importInitialData(principal);
            } else {
                logger.error("Token exchange failed - no access token in response");
                markAuthorizationAsFailed(authorization, "no_access_token", "Token exchange failed");
            }

        } catch (WebClientResponseException e) {
            logger.error("HTTP error during token exchange: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            markAuthorizationAsFailed(authorization, "token_exchange_error", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token exchange failed");
        } catch (Exception e) {
            logger.error("Error processing authorization code", e);
            markAuthorizationAsFailed(authorization, "processing_error", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Processing failed");
        }
    }

    /**
     * Updates authorization entity with token response data.
     */
    private void updateAuthorizationWithToken(AuthorizationEntity authorization, Map<String, Object> tokenResponse) {
        authorization.setAccessToken((String) tokenResponse.get("access_token"));
        authorization.setTokenType(TokenType.fromValue((String) tokenResponse.get("token_type")));
        authorization.setExpiresIn(getLongValue(tokenResponse, "expires_in"));
        authorization.setRefreshToken((String) tokenResponse.get("refresh_token"));
        authorization.setScope((String) tokenResponse.get("scope"));
        authorization.setAuthorizationURI((String) tokenResponse.get("authorization_uri"));
        authorization.setResourceURI((String) tokenResponse.get("resource_uri"));
        authorization.setUpdated(LocalDateTime.now());
        authorization.setStatus("1"); // Active
        authorization.setState(null); // Clear state for security

        authorizationService.merge(authorization);
        logger.info("Successfully updated authorization with access token");
    }

    /**
     * Handles authorization errors by updating the authorization record.
     */
    private void handleAuthorizationError(AuthorizationEntity authorization, 
                                        String error, 
                                        String errorDescription, 
                                        String errorUri) {
        authorization.setError(error != null ? OAuthError.fromValue(error) : null);
        authorization.setErrorDescription(errorDescription);
        authorization.setErrorUri(errorUri);
        authorization.setUpdated(LocalDateTime.now());
        authorization.setStatus("2"); // Denied
        authorization.setState(null); // Clear state for security

        authorizationService.merge(authorization);
    }

    /**
     * Marks authorization as failed with error details.
     */
    private void markAuthorizationAsFailed(AuthorizationEntity authorization, String error, String description) {
        authorization.setError(error != null ? OAuthError.fromValue(error) : null);
        authorization.setErrorDescription(description);
        authorization.setUpdated(LocalDateTime.now());
        authorization.setStatus("2"); // Failed
        authorization.setState(null);

        authorizationService.merge(authorization);
    }

    /**
     * Attempts initial data import from the authorized resource.
     */
    private void importInitialData(Principal principal) {
        try {
            Long customerId = getCurrentCustomerId(principal);
            usagePointRESTRepository.findAllByRetailCustomerId(customerId);
            logger.debug("Successfully imported initial usage point data");
        } catch (JAXBException e) {
            logger.info("No initial data available for import - will import later on demand");
        } catch (Exception e) {
            logger.warn("Error during initial data import: {}", e.getMessage());
        }
    }

    /**
     * Gets the current customer ID from the authenticated principal.
     */
    private Long getCurrentCustomerId(Principal principal) {
        if (principal instanceof Authentication auth) {
            var customer = retailCustomerService.findByUsername(auth.getName());
            if (customer != null) {
                return (long) customer.getId().hashCode();
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer not found");
    }

    /**
     * Safely extracts Long value from token response map.
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid number format for {}: {}", key, value);
            }
        }
        return null;
    }
}