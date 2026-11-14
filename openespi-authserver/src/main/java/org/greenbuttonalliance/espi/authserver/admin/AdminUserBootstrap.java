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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Creates the first Authorization Server administrator from configuration (#205).
 *
 * <p>Runs once at startup. It acts only when {@code admin_users} is empty AND both
 * {@code espi.authserver.admin.bootstrap.username} ({@code ESPI_BOOTSTRAP_ADMIN_USERNAME}) and
 * {@code espi.authserver.admin.bootstrap.password-hash} ({@code ESPI_BOOTSTRAP_ADMIN_PASSWORD_HASH})
 * are set. The hash must be a bcrypt hash, either bare ({@code $2a$/$2b$/$2y$}) or with the
 * {@code {bcrypt}} prefix. A plaintext or other value fails startup instead of being accepted. If
 * admins already exist, the settings are ignored. The bootstrapped admin gets {@code ROLE_ADMIN} and
 * must still enrol a passkey on first login.</p>
 */
@Component
public class AdminUserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserBootstrap.class);

    /** bcrypt modular-crypt format: $2a$/$2b$/$2y$, two-digit cost, 22-char salt + 31-char hash. */
    static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    private static final String BCRYPT_PREFIX = "{bcrypt}";

    private final AdminUserRepository repository;
    private final String username;
    private final String passwordHash;

    public AdminUserBootstrap(
            AdminUserRepository repository,
            @Value("${espi.authserver.admin.bootstrap.username:${ESPI_BOOTSTRAP_ADMIN_USERNAME:}}") String username,
            @Value("${espi.authserver.admin.bootstrap.password-hash:${ESPI_BOOTSTRAP_ADMIN_PASSWORD_HASH:}}") String passwordHash) {
        this.repository = repository;
        this.username = username == null ? "" : username.trim();
        this.passwordHash = passwordHash == null ? "" : passwordHash.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrap();
    }

    /** Returns true when an admin was created. */
    @Transactional
    public boolean bootstrap() {
        if (username.isEmpty() && passwordHash.isEmpty()) {
            return false;
        }
        if (repository.count() > 0) {
            log.info("Admin bootstrap skipped: admin_users is not empty.");
            return false;
        }
        if (username.isEmpty() || passwordHash.isEmpty()) {
            log.warn("Admin bootstrap skipped: both espi.authserver.admin.bootstrap.username and "
                    + "password-hash (ESPI_BOOTSTRAP_ADMIN_USERNAME / ESPI_BOOTSTRAP_ADMIN_PASSWORD_HASH) are required.");
            return false;
        }
        if (!AdminUserService.USERNAME.matcher(username).matches()) {
            throw new IllegalStateException("ESPI_BOOTSTRAP_ADMIN_USERNAME must start with a letter and "
                    + "be 3-100 of [A-Za-z0-9._@-]");
        }
        String stored = toStoredBcrypt(passwordHash);
        repository.insert(username, stored, true, List.of("ROLE_ADMIN"));
        log.info("Bootstrapped first administrator '{}' (ROLE_ADMIN). A passkey must be enrolled at first login.",
                username);
        return true;
    }

    /**
     * Normalizes a configured hash to the {@code {bcrypt}}-prefixed form the DelegatingPasswordEncoder
     * expects, rejecting anything that is not a bcrypt hash (plaintext is never accepted).
     */
    static String toStoredBcrypt(String configured) {
        String bare = configured.startsWith(BCRYPT_PREFIX) ? configured.substring(BCRYPT_PREFIX.length()) : configured;
        if (!BCRYPT.matcher(bare).matches()) {
            throw new IllegalStateException("ESPI_BOOTSTRAP_ADMIN_PASSWORD_HASH must be a bcrypt hash "
                    + "($2a$/$2b$/$2y$..., optionally prefixed with {bcrypt}); plaintext is not accepted.");
        }
        return BCRYPT_PREFIX + bare;
    }
}
