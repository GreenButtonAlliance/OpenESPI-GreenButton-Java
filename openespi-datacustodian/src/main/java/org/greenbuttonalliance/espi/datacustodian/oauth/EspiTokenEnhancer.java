/*
 *
 *    Copyright (c) 2018-2021 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.datacustodian.oauth;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Modern Spring Boot 3.5 compatible ESPI Token Enhancer
 * 
 * TODO: Implement full OAuth2 token enhancement functionality using modern entities.
 * This is a placeholder implementation to enable core Spring Boot 3.5 migration.
 * 
 * The full implementation should:
 * - Use AuthorizationEntity, SubscriptionEntity, and other modern entities
 * - Integrate with modern UUID-based services  
 * - Use OffsetDateTime instead of GregorianCalendar
 * - Use DateTimeIntervalDto instead of legacy DateTimeInterval
 * - Use configuration properties instead of Routes constants
 * - Work with the modernized openespi-common service layer
 */

@Component
public class EspiTokenEnhancer implements TokenEnhancer {

    @Value("${espi.datacustodian.base-url:http://localhost:8081/DataCustodian}")
    private String dataCustodianBaseUrl;

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        // TODO: Implement modern ESPI token enhancement logic
        // For now, return the token unchanged to enable compilation
        
        // Log the enhancement request for debugging
        System.out.printf("EspiTokenEnhancer: Processing token enhancement for client: %s%n", 
                authentication.getOAuth2Request().getClientId());
        
        return accessToken;
    }
}
