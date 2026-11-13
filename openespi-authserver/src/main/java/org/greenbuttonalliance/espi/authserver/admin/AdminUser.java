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

import java.time.Instant;
import java.util.Set;

/**
 * An Authorization Server administrator as exposed by the admin-user API (#205).
 *
 * <p>Deliberately carries no password hash: hashes are read only by {@link AdminUserDetailsService}
 * and never leave {@link AdminUserRepository} through this type.</p>
 */
public record AdminUser(
        String id,
        String username,
        boolean enabled,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt) {
}
