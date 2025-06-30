/*
 *
 *    Copyright (c) 2018-2025 Green Button Alliance, Inc.
 *
 *    Portions (c) 2013-2018 EnergyOS.org
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

package org.greenbuttonalliance.espi.authserver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.greenbuttonalliance.espi.authserver.service.ConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ESPI OAuth2 Consent Controller
 * 
 * Handles user consent for OAuth2 authorization requests in Spring Authorization Server 1.3+.
 * Provides ESPI-specific consent pages with Green Button Alliance branding and
 * scope descriptions.
 * 
 * Endpoints:
 * - GET /oauth2/consent: Display consent page
 * - POST /oauth2/consent: Process consent response
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@Controller
public class ConsentController {

    private static final Logger logger = LoggerFactory.getLogger(ConsentController.class);

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2AuthorizationConsentService authorizationConsentService;
    private final ConsentService consentService;

    @Autowired
    public ConsentController(
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService,
            OAuth2AuthorizationConsentService authorizationConsentService,
            ConsentService consentService) {
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationService = authorizationService;
        this.authorizationConsentService = authorizationConsentService;
        this.consentService = consentService;
    }

    /**
     * Display OAuth2 consent page
     * 
     * Shows ESPI-specific consent form with:
     * - Client application details
     * - Requested scopes with descriptions
     * - Green Button Alliance branding
     */
    @GetMapping("/oauth2/consent")
    public String consent(
            Principal principal,
            Model model,
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
            @RequestParam(OAuth2ParameterNames.STATE) String state,
            @RequestParam(name = OAuth2ParameterNames.USER_CODE, required = false) String userCode) {

        logger.debug("Consent request for client: {} with scopes: {}", clientId, scope);

        try {
            // Retrieve client registration
            RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
            if (registeredClient == null) {
                logger.error("Client not found: {}", clientId);
                model.addAttribute("error", "Invalid client");
                return "error";
            }

            // Get existing consent
            OAuth2AuthorizationConsent consent = authorizationConsentService
                    .findById(registeredClient.getId(), principal.getName());

            // Parse and categorize scopes
            Set<String> scopesToApprove = parseScopes(scope);
            Set<String> previouslyApprovedScopes = consent != null ? consent.getScopes() : Collections.emptySet();
            Set<String> newScopes = scopesToApprove.stream()
                    .filter(s -> !previouslyApprovedScopes.contains(s))
                    .collect(Collectors.toSet());

            // If no new scopes require consent, redirect back
            if (newScopes.isEmpty()) {
                logger.debug("No additional consent required for client: {}", clientId);
                return "redirect:/oauth2/authorize?" + buildAuthorizationParams(clientId, scope, state);
            }

            // Add model attributes for template
            model.addAttribute("clientId", clientId);
            model.addAttribute("clientName", registeredClient.getClientName());
            model.addAttribute("state", state);
            model.addAttribute("scopes", withDescription(newScopes));
            model.addAttribute("previouslyApprovedScopes", withDescription(previouslyApprovedScopes));
            model.addAttribute("principalName", principal.getName());
            model.addAttribute("userCode", userCode);
            
            // Add ESPI-specific attributes
            model.addAttribute("espiVersion", "4.0");
            model.addAttribute("greenButtonLogo", "/assets/images/green-button-logo.png");
            model.addAttribute("isEspiClient", isEspiClient(scopesToApprove));

            return "consent";

        } catch (Exception e) {
            logger.error("Error processing consent request", e);
            model.addAttribute("error", "Consent processing error");
            return "error";
        }
    }

    /**
     * Process consent form submission
     * 
     * Handles user's consent decision and redirects appropriately
     */
    @PostMapping("/oauth2/consent")
    public String processConsent(
            Principal principal,
            Model model,
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(OAuth2ParameterNames.STATE) String state,
            @RequestParam(name = "action", defaultValue = "deny") String action,
            @RequestParam Map<String, Object> parameters) {

        logger.debug("Processing consent for client: {} action: {}", clientId, action);

        try {
            RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
            if (registeredClient == null) {
                logger.error("Client not found during consent processing: {}", clientId);
                return "redirect:/oauth2/authorize?" +
                        "client_id=" + clientId +
                        "&state=" + state +
                        "&error=invalid_client";
            }

            // If user denied consent, redirect with error
            if (!"approve".equals(action)) {
                logger.info("Consent denied by user: {} for client: {}", principal.getName(), clientId);
                
                // Record the denial for audit purposes
                Set<String> requestedScopes = parameters.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("scope."))
                    .map(entry -> entry.getKey().substring(6))
                    .collect(Collectors.toSet());
                
                consentService.recordConsentDenial(clientId, principal.getName(), requestedScopes);
                
                return "redirect:/oauth2/authorize?" +
                        "client_id=" + clientId +
                        "&state=" + state +
                        "&error=access_denied" +
                        "&error_description=User+denied+access";
            }

            // Process approved scopes
            Set<String> approvedScopes = parameters.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("scope.") && "on".equals(entry.getValue()))
                    .map(entry -> entry.getKey().substring(6)) // Remove "scope." prefix
                    .collect(Collectors.toSet());

            // Save consent
            OAuth2AuthorizationConsent.Builder consentBuilder = OAuth2AuthorizationConsent
                    .withId(registeredClient.getId(), principal.getName());
            
            // Add previously approved scopes
            OAuth2AuthorizationConsent existingConsent = authorizationConsentService
                    .findById(registeredClient.getId(), principal.getName());
            if (existingConsent != null) {
                existingConsent.getScopes().forEach(consentBuilder::scope);
            }
            
            // Add new approved scopes
            approvedScopes.forEach(consentBuilder::scope);
            
            authorizationConsentService.save(consentBuilder.build());

            logger.info("Consent saved for user: {} client: {} scopes: {}", 
                      principal.getName(), clientId, approvedScopes);

            // Record detailed consent information
            Set<String> deniedScopes = parameters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("scope.") && !"on".equals(entry.getValue()))
                .map(entry -> entry.getKey().substring(6))
                .collect(Collectors.toSet());
            
            consentService.recordConsentDetails(clientId, principal.getName(), approvedScopes, deniedScopes);

            // Build approved scopes string for redirect
            String approvedScopeString = String.join(" ", approvedScopes);

            // Redirect back to authorization endpoint
            return "redirect:/oauth2/authorize?" + buildAuthorizationParams(clientId, approvedScopeString, state);

        } catch (Exception e) {
            logger.error("Error processing consent", e);
            return "redirect:/oauth2/authorize?" +
                    "client_id=" + clientId +
                    "&state=" + state +
                    "&error=server_error";
        }
    }

    /**
     * Parse scope parameter into individual scopes
     */
    private Set<String> parseScopes(String scope) {
        if (!StringUtils.hasText(scope)) {
            return Collections.emptySet();
        }
        return Set.of(scope.split("\\s+"));
    }

    /**
     * Add human-readable descriptions to scopes
     */
    private Set<ScopeWithDescription> withDescription(Set<String> scopes) {
        return scopes.stream()
                .map(this::buildScopeWithDescription)
                .collect(Collectors.toSet());
    }

    /**
     * Build scope with ESPI-specific description
     */
    private ScopeWithDescription buildScopeWithDescription(String scope) {
        String description;
        String category;

        switch (scope) {
            case "openid":
                description = "Access your basic profile information";
                category = "profile";
                break;
            case "profile":
                description = "Access your profile information (name, email)";
                category = "profile";
                break;
            case "DataCustodian_Admin_Access":
                description = "Administrative access to Data Custodian resources";
                category = "admin";
                break;
            case "ThirdParty_Admin_Access":
                description = "Administrative access for Third Party applications";
                category = "admin";
                break;
            default:
                if (scope.startsWith("FB=")) {
                    description = parseEspiScopeDescription(scope);
                    category = "energy_data";
                } else {
                    description = "Access to " + scope + " resources";
                    category = "other";
                }
                break;
        }

        return new ScopeWithDescription(scope, description, category);
    }

    /**
     * Parse ESPI scope description from Green Button scope format
     * Example: "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
     */
    private String parseEspiScopeDescription(String scope) {
        Map<String, String> params = Arrays.stream(scope.split(";"))
                .filter(part -> part.contains("="))
                .collect(Collectors.toMap(
                        part -> part.split("=")[0],
                        part -> part.split("=")[1]
                ));

        StringBuilder description = new StringBuilder("Access to your energy usage data");
        
        if (params.containsKey("IntervalDuration")) {
            int duration = Integer.parseInt(params.get("IntervalDuration"));
            description.append(" with ").append(formatDuration(duration)).append(" intervals");
        }
        
        if (params.containsKey("BlockDuration")) {
            description.append(", ").append(params.get("BlockDuration")).append(" billing periods");
        }
        
        if (params.containsKey("HistoryLength")) {
            description.append(", up to ").append(params.get("HistoryLength")).append(" months of history");
        }

        return description.toString();
    }

    /**
     * Format duration in seconds to human-readable format
     */
    private String formatDuration(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + "-hour";
        } else if (seconds >= 60) {
            return (seconds / 60) + "-minute";
        } else {
            return seconds + "-second";
        }
    }

    /**
     * Check if client is requesting ESPI-specific scopes
     */
    private boolean isEspiClient(Set<String> scopes) {
        return scopes.stream().anyMatch(scope -> 
            scope.startsWith("FB=") || 
            scope.contains("DataCustodian") || 
            scope.contains("ThirdParty"));
    }

    /**
     * Build authorization parameters for redirect
     */
    private String buildAuthorizationParams(String clientId, String scope, String state) {
        StringBuilder params = new StringBuilder();
        params.append("client_id=").append(clientId);
        if (scope != null && !scope.isEmpty()) {
            params.append("&scope=").append(scope);
        }
        if (state != null) {
            params.append("&state=").append(state);
        }
        return params.toString();
    }

    /**
     * Scope with human-readable description and category
     */
    public static class ScopeWithDescription {
        private final String scope;
        private final String description;
        private final String category;

        public ScopeWithDescription(String scope, String description, String category) {
            this.scope = scope;
            this.description = description;
            this.category = category;
        }

        public String getScope() {
            return scope;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }
    }
}