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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for OpenESPI Authorization Server Application
 * 
 * Tests basic Spring Boot application startup and configuration
 * with Spring Authorization Server 1.3+.
 * 
 * @author Green Button Alliance
 * @version 1.0.0
 * @since Spring Boot 3.5
 */
@SpringBootTest
@ActiveProfiles("local")
class AuthorizationServerApplicationTests {

    /**
     * Test that the Spring Boot application context loads successfully
     * with all OAuth2 Authorization Server configurations.
     */
    @Test
    void contextLoads() {
        // This test verifies that:
        // - Spring Boot application starts successfully
        // - All OAuth2 Authorization Server beans are configured
        // - Database schema is created (H2 in-memory for tests)
        // - JWT key pair is generated
        // - Registered clients are loaded
        // - Security configurations are applied
    }
}