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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * JDBC access to {@code admin_users} / {@code admin_user_roles} (#205, Flyway V9_0_0).
 *
 * <p>Plain JDBC, like {@code RegisteredClientAdminDao}: these tables are not JPA entities, so
 * {@code ddl-auto: validate} is unaffected.</p>
 */
@Repository
public class AdminUserRepository {

    /** Row used to authenticate: includes the hash, never exposed beyond this package. */
    record Credentials(String id, String username, String passwordHash, boolean enabled, Set<String> roles) {
    }

    private static final String USER_COLUMNS =
            "id, username, enabled, created_at, updated_at, last_login_at";

    private final JdbcTemplate jdbcTemplate;

    public AdminUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_users", Long.class);
        return count == null ? 0 : count;
    }

    public List<AdminUser> findAll() {
        Map<String, Set<String>> roles = allRoles();
        return jdbcTemplate.query("SELECT " + USER_COLUMNS + " FROM admin_users ORDER BY username",
                (rs, n) -> new AdminUser(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getBoolean("enabled"),
                        roles.getOrDefault(rs.getString("id"), Set.of()),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")),
                        toInstant(rs.getTimestamp("last_login_at"))));
    }

    public Optional<AdminUser> findById(String id) {
        return jdbcTemplate.query("SELECT " + USER_COLUMNS + " FROM admin_users WHERE id = ?",
                        (rs, n) -> new AdminUser(
                                rs.getString("id"),
                                rs.getString("username"),
                                rs.getBoolean("enabled"),
                                rolesOf(rs.getString("id")),
                                toInstant(rs.getTimestamp("created_at")),
                                toInstant(rs.getTimestamp("updated_at")),
                                toInstant(rs.getTimestamp("last_login_at"))),
                        id)
                .stream().findFirst();
    }

    Optional<Credentials> findCredentialsByUsername(String username) {
        return jdbcTemplate.query(
                        "SELECT id, username, password_hash, enabled FROM admin_users WHERE username = ?",
                        (rs, n) -> new Credentials(
                                rs.getString("id"),
                                rs.getString("username"),
                                rs.getString("password_hash"),
                                rs.getBoolean("enabled"),
                                rolesOf(rs.getString("id"))),
                        username)
                .stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_users WHERE username = ?", Long.class, username);
        return count != null && count > 0;
    }

    /** Inserts a user with the given (already hashed) password and roles; returns the new id. */
    public String insert(String username, String passwordHash, boolean enabled, Collection<String> roles) {
        String id = UUID.randomUUID().toString();
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
                "INSERT INTO admin_users (id, username, password_hash, enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, username, passwordHash, enabled, now, now);
        replaceRoles(id, roles);
        return id;
    }

    public void updateEnabled(String id, boolean enabled) {
        jdbcTemplate.update("UPDATE admin_users SET enabled = ?, updated_at = ? WHERE id = ?",
                enabled, Timestamp.from(Instant.now()), id);
    }

    public void updatePasswordHash(String id, String passwordHash) {
        jdbcTemplate.update("UPDATE admin_users SET password_hash = ?, updated_at = ? WHERE id = ?",
                passwordHash, Timestamp.from(Instant.now()), id);
    }

    public void updateLastLogin(String username, Instant at) {
        jdbcTemplate.update("UPDATE admin_users SET last_login_at = ? WHERE username = ?",
                Timestamp.from(at), username);
    }

    public void replaceRoles(String id, Collection<String> roles) {
        jdbcTemplate.update("DELETE FROM admin_user_roles WHERE user_id = ?", id);
        for (String role : new TreeSet<>(roles)) {
            jdbcTemplate.update("INSERT INTO admin_user_roles (user_id, role) VALUES (?, ?)", id, role);
        }
        jdbcTemplate.update("UPDATE admin_users SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now()), id);
    }

    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM admin_user_roles WHERE user_id = ?", id);
        jdbcTemplate.update("DELETE FROM admin_users WHERE id = ?", id);
    }

    /**
     * Locks the rows of every enabled {@code ROLE_ADMIN} user ({@code SELECT ... FOR UPDATE}) and
     * returns their ids, so concurrent "disable/delete/demote" requests serialize on the last-admin
     * check instead of both passing it. Must run inside a transaction.
     */
    public List<String> lockEnabledAdminIds() {
        return jdbcTemplate.queryForList(
                "SELECT u.id FROM admin_users u JOIN admin_user_roles r ON r.user_id = u.id "
                        + "WHERE u.enabled = TRUE AND r.role = 'ROLE_ADMIN' ORDER BY u.id FOR UPDATE",
                String.class);
    }

    private Set<String> rolesOf(String id) {
        return new TreeSet<>(jdbcTemplate.queryForList(
                "SELECT role FROM admin_user_roles WHERE user_id = ?", String.class, id));
    }

    private Map<String, Set<String>> allRoles() {
        Map<String, Set<String>> roles = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT user_id, role FROM admin_user_roles", rs -> {
            roles.computeIfAbsent(rs.getString("user_id"), k -> new TreeSet<>()).add(rs.getString("role"));
        });
        return roles;
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
