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

package org.greenbuttonalliance.espi.thirdparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot application class for OpenESPI Third Party (OAuth2 Client) application.
 * 
 * This is the OAuth2 Client application that demonstrates how to:
 * - Authenticate with the authorization server
 * - Request access tokens from the data custodian
 * - Access protected resources on behalf of retail customers
 * 
 * Follows Green Button Connect My Data and Share My Data protocols.
 */
@SpringBootApplication(scanBasePackages = {
    "org.greenbuttonalliance.espi.thirdparty",
    "org.greenbuttonalliance.espi.common.service.impl",
    "org.greenbuttonalliance.espi.common.utils"
})
@EntityScan(basePackages = {
    "org.greenbuttonalliance.espi.common.domain"
})
@EnableJpaRepositories(basePackages = {
    "org.greenbuttonalliance.espi.common.repositories"
})
public class ThirdPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThirdPartyApplication.class, args);
    }
}