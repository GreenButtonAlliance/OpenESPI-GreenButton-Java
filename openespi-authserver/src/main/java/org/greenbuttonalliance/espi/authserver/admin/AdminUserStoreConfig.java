/*
 *
 *    Copyright (c) 2018-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.authserver.admin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

/**
 * Beans for the human-admin identity store (#205).
 *
 * <ul>
 *   <li>The admin {@link UserDetailsService}, the only one in the module. {@code WebAuthnConfigurer}
 *       looks it up to load the user after a passkey assertion, and the session chain wires it into
 *       form login.</li>
 *   <li>Spring Security's JDBC WebAuthn repositories over the V9_0_0 {@code user_entities} /
 *       {@code user_credentials} tables. {@code WebAuthnConfigurer} picks them up from the context.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class AdminUserStoreConfig {

    @Bean
    public UserDetailsService adminUserDetailsService(AdminUserRepository repository) {
        return new AdminUserDetailsService(repository);
    }

    @Bean
    public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcPublicKeyCredentialUserEntityRepository(jdbcTemplate);
    }

    @Bean
    public UserCredentialRepository userCredentialRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcUserCredentialRepository(jdbcTemplate);
    }

    @Bean
    public AdminLoginSuccessHandler adminLoginSuccessHandler(AdminUserService adminUserService) {
        return new AdminLoginSuccessHandler(adminUserService);
    }
}
