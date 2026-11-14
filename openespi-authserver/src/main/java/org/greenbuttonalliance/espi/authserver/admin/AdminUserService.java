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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Business rules for the admin-user API (#205).
 *
 * <ul>
 *   <li>Passwords are hashed server-side with the application {@link PasswordEncoder} ({@code {bcrypt}})
 *       and must be at least {@value #MIN_PASSWORD_LENGTH} characters; hashes are never returned.</li>
 *   <li>Usernames must start with a letter. DC retail-customer principals are numeric ids, so an admin
 *       username can never equal a delegated customer principal (which matters because Spring Security's
 *       MFA merge keys on the principal name).</li>
 *   <li>An administrator cannot disable, delete or demote themselves, and the last enabled
 *       {@code ROLE_ADMIN} can never be disabled, deleted or demoted.</li>
 * </ul>
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    public static final int MIN_PASSWORD_LENGTH = 12;
    public static final int MAX_PASSWORD_LENGTH = 72; // bcrypt input limit
    public static final Set<String> ALLOWED_ROLES = Set.of("ROLE_ADMIN", "ROLE_DC_ADMIN");
    static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9._@-]{2,99}$");

    /** Validation failure (maps to 400). */
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    /** Rule violation such as the last-admin or self-modification guard (maps to 409). */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /** Unknown user id (maps to 404). */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    private final AdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PublicKeyCredentialUserEntityRepository userEntities;
    private final UserCredentialRepository userCredentials;

    public AdminUserService(AdminUserRepository repository,
                            PasswordEncoder passwordEncoder,
                            PublicKeyCredentialUserEntityRepository userEntities,
                            UserCredentialRepository userCredentials) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.userEntities = userEntities;
        this.userCredentials = userCredentials;
    }

    public List<AdminUser> list() {
        return repository.findAll();
    }

    @Transactional
    public AdminUser create(String username, String password, Collection<String> roles) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new InvalidRequestException(
                    "username must start with a letter and be 3-100 of [A-Za-z0-9._@-]");
        }
        validatePassword(password);
        Set<String> normalized = normalizeRoles(roles == null || roles.isEmpty() ? List.of("ROLE_ADMIN") : roles);
        if (repository.existsByUsername(username)) {
            throw new ConflictException("username already exists");
        }
        String id = repository.insert(username, passwordEncoder.encode(password), true, normalized);
        log.info("Admin user '{}' created with roles {}", username, normalized);
        return get(id);
    }

    @Transactional
    public AdminUser setEnabled(String id, boolean enabled, String actingUsername) {
        AdminUser user = get(id);
        if (!enabled) {
            guardNotSelf(user, actingUsername, "disable");
            guardNotLastAdmin(user, "disable");
        }
        repository.updateEnabled(id, enabled);
        return get(id);
    }

    @Transactional
    public void resetPassword(String id, String newPassword) {
        get(id);
        validatePassword(newPassword);
        repository.updatePasswordHash(id, passwordEncoder.encode(newPassword));
    }

    @Transactional
    public AdminUser setRoles(String id, Collection<String> roles, String actingUsername) {
        AdminUser user = get(id);
        Set<String> normalized = normalizeRoles(roles);
        if (normalized.isEmpty()) {
            throw new InvalidRequestException("at least one role is required");
        }
        if (user.roles().contains("ROLE_ADMIN") && !normalized.contains("ROLE_ADMIN")) {
            guardNotSelf(user, actingUsername, "remove ROLE_ADMIN from");
            guardNotLastAdmin(user, "remove ROLE_ADMIN from");
        }
        repository.replaceRoles(id, normalized);
        return get(id);
    }

    @Transactional
    public void delete(String id, String actingUsername) {
        AdminUser user = get(id);
        guardNotSelf(user, actingUsername, "delete");
        guardNotLastAdmin(user, "delete");
        removePasskeys(user.username());
        repository.delete(id);
        log.info("Admin user '{}' deleted", user.username());
    }

    /** Deletes all WebAuthn credentials (and the WebAuthn user entity) of the user; returns the count. */
    @Transactional
    public int resetPasskeys(String id) {
        AdminUser user = get(id);
        int removed = removePasskeys(user.username());
        log.info("Reset {} passkey(s) of admin user '{}'", removed, user.username());
        return removed;
    }

    /** Whether the user has at least one registered passkey. */
    public boolean hasPasskey(String username) {
        PublicKeyCredentialUserEntity entity = userEntities.findByUsername(username);
        return entity != null && !userCredentials.findByUserId(entity.getId()).isEmpty();
    }

    public void recordLogin(String username) {
        repository.updateLastLogin(username, java.time.Instant.now());
    }

    private int removePasskeys(String username) {
        PublicKeyCredentialUserEntity entity = userEntities.findByUsername(username);
        if (entity == null) {
            return 0;
        }
        List<CredentialRecord> credentials = userCredentials.findByUserId(entity.getId());
        credentials.forEach(credential -> userCredentials.delete(credential.getCredentialId()));
        userEntities.delete(entity.getId());
        return credentials.size();
    }

    private AdminUser get(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("no such admin user"));
    }

    private void guardNotSelf(AdminUser user, String actingUsername, String action) {
        if (user.username().equals(actingUsername)) {
            throw new ConflictException("an administrator cannot " + action + " themselves");
        }
    }

    private void guardNotLastAdmin(AdminUser user, String action) {
        if (!user.enabled() || !user.roles().contains("ROLE_ADMIN")) {
            return;
        }
        List<String> enabledAdmins = repository.lockEnabledAdminIds();
        if (enabledAdmins.size() <= 1 && enabledAdmins.contains(user.id())) {
            throw new ConflictException("cannot " + action + " the last enabled administrator");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidRequestException(
                    "password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_PASSWORD_LENGTH) {
            throw new InvalidRequestException(
                    "password must be at most " + MAX_PASSWORD_LENGTH + " bytes (bcrypt limit)");
        }
    }

    static Set<String> normalizeRoles(Collection<String> roles) {
        Set<String> normalized = new LinkedHashSet<>();
        if (roles == null) {
            return normalized;
        }
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String upper = role.trim().toUpperCase(Locale.ROOT);
            String withPrefix = upper.startsWith("ROLE_") ? upper : "ROLE_" + upper;
            if (!ALLOWED_ROLES.contains(withPrefix)) {
                throw new InvalidRequestException("unsupported role: " + role + " (allowed: ADMIN, DC_ADMIN)");
            }
            normalized.add(withPrefix);
        }
        return normalized;
    }
}
