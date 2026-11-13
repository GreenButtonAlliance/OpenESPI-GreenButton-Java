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

package org.greenbuttonalliance.espi.authserver.admin;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * {@link UserDetailsService} for Authorization Server administrators only (#205).
 *
 * <p>Reads {@code admin_users} exclusively. Retail customers are never looked up here: they
 * authenticate at the Data Custodian and the AS receives them through the signed return handoff
 * ({@code AuthorizeContinueController}), so a customer identifier can never log in with a password or
 * passkey on the AS. Used by both form login (password factor) and WebAuthn (passkey factor).</p>
 */
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository repository;

    public AdminUserDetailsService(AdminUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AdminUserRepository.Credentials credentials = repository.findCredentialsByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No such administrator"));
        return User.withUsername(credentials.username())
                .password(credentials.passwordHash())
                .disabled(!credentials.enabled())
                .authorities(credentials.roles().stream().map(SimpleGrantedAuthority::new).toList())
                .build();
    }
}
