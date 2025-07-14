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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Modern OAuth2 Client Controller using Spring Security 6.x OAuth2 client features.
 * 
 * Replaces legacy manual OAuth2 handling with Spring Security's built-in capabilities.
 * Supports multiple OAuth2 client registrations for different ESPI scope requirements.
 */
@Controller
@RequestMapping("/oauth2")
public class OAuth2ClientController {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientController.class);

    /**
     * Initiate OAuth2 authorization flow for monthly usage data access.
     * Uses Spring Security's OAuth2 client with predefined scope configuration.
     */
    @GetMapping("/authorize/monthly")
    public String authorizeMonthly() {
        logger.info("Initiating OAuth2 flow for monthly usage data access");
        return "redirect:/oauth2/authorization/datacustodian-monthly";
    }

    /**
     * Initiate OAuth2 authorization flow for daily usage data access.
     * Uses Spring Security's OAuth2 client with predefined scope configuration.
     */
    @GetMapping("/authorize/daily")
    public String authorizeDaily() {
        logger.info("Initiating OAuth2 flow for daily usage data access");
        return "redirect:/oauth2/authorization/datacustodian-daily";
    }

    /**
     * Initiate OAuth2 authorization flow for admin access.
     * Uses Spring Security's OAuth2 client with admin scope configuration.
     */
    @GetMapping("/authorize/admin")
    public String authorizeAdmin() {
        logger.info("Initiating OAuth2 flow for admin access");
        return "redirect:/oauth2/authorization/datacustodian-admin";
    }

    /**
     * Display authorized client information after successful OAuth2 flow.
     * Demonstrates how to access OAuth2 tokens and user information.
     */
    @GetMapping("/client/{registrationId}")
    public String clientInfo(@PathVariable String registrationId,
                           @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                           @AuthenticationPrincipal OAuth2User oauth2User,
                           Model model) {
        
        logger.info("Displaying client info for registration: {}", registrationId);
        
        if (authorizedClient != null) {
            model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());
            model.addAttribute("userName", oauth2User.getName());
            model.addAttribute("userAttributes", oauth2User.getAttributes());
            model.addAttribute("accessToken", authorizedClient.getAccessToken().getTokenValue());
            model.addAttribute("scopes", authorizedClient.getAccessToken().getScopes());
            
            // Token expiration info
            if (authorizedClient.getAccessToken().getExpiresAt() != null) {
                model.addAttribute("expiresAt", authorizedClient.getAccessToken().getExpiresAt().toString());
            }
            
            // Refresh token availability
            if (authorizedClient.getRefreshToken() != null) {
                model.addAttribute("hasRefreshToken", true);
            }
        }
        
        return "oauth2/client-info";
    }

    /**
     * Home page after successful OAuth2 authentication.
     */
    @GetMapping("/success")
    public String success(@AuthenticationPrincipal OAuth2User oauth2User, Model model) {
        logger.info("OAuth2 authentication successful for user: {}", oauth2User.getName());
        
        model.addAttribute("userName", oauth2User.getName());
        model.addAttribute("userAttributes", oauth2User.getAttributes());
        
        return "oauth2/success";
    }
}