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

package org.greenbuttonalliance.espi.authserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OpenESPI Authorization Server Application
 * 
 * Spring Boot 3.5 OAuth2 Authorization Server for Green Button Alliance ESPI protocol.
 * 
 * This server handles:
 * - OAuth2 authorization flows (authorization_code, client_credentials)
 * - JWT token issuance and validation
 * - Client registration and management
 * - ESPI-specific token enhancement
 * - User consent management
 * 
 * Communicates with:
 * - OpenESPI-DataCustodian-java (Resource Server)
 * - OpenESPI-ThirdParty-java (OAuth2 Client)
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@SpringBootApplication
public class AuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }
}