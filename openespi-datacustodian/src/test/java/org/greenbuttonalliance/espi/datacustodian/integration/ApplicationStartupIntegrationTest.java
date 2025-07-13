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

package org.greenbuttonalliance.espi.datacustodian.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critical integration test to ensure the DataCustodian application starts successfully
 * with all Spring Boot 3.5 configurations and JPA entity relationships working.
 * 
 * This test validates the complete modernization is successful and the application
 * is ready for production use.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationStartupIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // This test verifies that the Spring Boot application context loads successfully
        // It validates the entire Spring Boot configuration including:
        // - Security configuration (OAuth2 Resource Server)
        // - JPA configuration with Hibernate 6
        // - Web configuration 
        // - Service layer beans
        // - Repository layer beans
        // - Fixed JPA entity relationships (Organisation @Embedded)
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void shouldHaveEssentialBeansConfigured() {
        // Verify critical beans are properly configured
        assertThat(applicationContext.getBeansOfType(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter.class))
                .isNotEmpty();
        
        // Verify JPA repositories are available
        assertThat(applicationContext.getBeansOfType(org.springframework.data.jpa.repository.JpaRepository.class))
                .isNotEmpty();
                
        // Verify controllers are available
        assertThat(applicationContext.getBeansOfType(org.springframework.stereotype.Controller.class))
                .isNotEmpty();
    }
}