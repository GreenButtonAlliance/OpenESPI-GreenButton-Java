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

package org.greenbuttonalliance.espi.authserver.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Admin-only operations on the {@code oauth2_registered_client} table that are
 * not part of the {@link org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository}
 * interface — currently {@code findAll()} (for listing) and {@code deleteById()}
 * (for revocation). Used by {@code OAuthAdminController}.
 *
 * <p>Kept narrow on purpose: the standard {@code RegisteredClientRepository}
 * (Spring's stock {@code JdbcRegisteredClientRepository}) handles save / find,
 * and any operation that touches OAuth2 client state should go through that
 * abstraction. This DAO only exposes the small set of read-all / delete-by-id
 * operations the admin UI needs.
 */
@Repository
public class RegisteredClientAdminDao {

    private static final String CLIENT_ID_COLUMN = "client_id";
    private static final String ID_COLUMN = "id";

    private static final String FIND_ALL_CLIENT_IDS_SQL =
            "SELECT " + CLIENT_ID_COLUMN + " FROM oauth2_registered_client ORDER BY " + CLIENT_ID_COLUMN;

    private static final String DELETE_BY_ID_SQL =
            "DELETE FROM oauth2_registered_client WHERE " + ID_COLUMN + " = ?";

    private final JdbcTemplate jdbcTemplate;

    public RegisteredClientAdminDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns all registered client_id values, alphabetically ordered.
     * Callers should resolve each through {@code RegisteredClientRepository.findByClientId(...)}
     * to get the full {@code RegisteredClient} — that path goes through Spring's
     * tested deserialization and avoids the row-mapping bugs we hit with the
     * earlier custom repo.
     */
    public List<String> findAllClientIds() {
        return jdbcTemplate.queryForList(FIND_ALL_CLIENT_IDS_SQL, String.class);
    }

    /**
     * Deletes a registered client by its primary-key {@code id} (not its
     * OAuth2 {@code client_id}). Returns the number of rows deleted (0 or 1).
     */
    public int deleteById(String id) {
        return jdbcTemplate.update(DELETE_BY_ID_SQL, id);
    }
}
