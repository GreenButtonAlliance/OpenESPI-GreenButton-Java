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
import org.springframework.boot.persistence.autoconfigure.EntityScan;
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
    // Scan the entire common package — matches DC's pattern. Previous narrow
    // scan ('.service.impl' + '.utils' only) was missing the MapStruct mappers
    // ('.mapper') AND non-impl services like EspiIdGeneratorService in '.service'.
    // Broadening to '.common' covers all current and future common-module beans
    // without playing whack-a-mole. Pre-existing TP bring-up gap, surfaced by
    // manual end-to-end testing of #122 PR C4.
    "org.greenbuttonalliance.espi.common"
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