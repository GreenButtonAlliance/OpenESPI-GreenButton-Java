/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.authserver.integration;

import org.greenbuttonalliance.espi.authserver.service.AuthorizationPurgeService;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Row builders and readers shared by the authorization purge integration tests (#192).
 *
 * <p>Writes {@code oauth2_authorization} and {@code oauth2_audit_log} rows with plain SQL so each
 * test controls every token expiry exactly. Token values are written as bytes (BLOB on MySQL/H2)
 * or text (TEXT on PostgreSQL), depending on {@code textTokenValues}.</p>
 */
final class AuthorizationPurgeFixtures {

	static final String GRANT_TYPE = "authorization_code";
	static final String SCOPES = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";

	private static final ObjectMapper JSON = new ObjectMapper();

	private AuthorizationPurgeFixtures() {
	}

	/** One token on an authorization row; a {@code null} expiry writes a value with no expiry. */
	record Token(String prefix, Instant issuedAt, Instant expiresAt) {
	}

	static Token token(String prefix, Instant issuedAt, Instant expiresAt) {
		return new Token(prefix, issuedAt, expiresAt);
	}

	/** A {@code AUTHORIZATION_PURGED} audit row as read back from the database. */
	record PurgeAudit(String clientId, String principalName, boolean success, String grantType,
			String scopesGranted, JsonNode additionalData) {

		String authorizationId() {
			return data("authorization_id");
		}

		/** A top-level text field of {@code additional_data}. */
		String data(String field) {
			return additionalData.path(field).asString();
		}
	}

	static void insertAuthorization(JdbcTemplate jdbc, boolean textTokenValues, String id,
			String registeredClientId, String principalName, Token... tokens) {
		List<String> columns = new ArrayList<>(List.of(
				"id", "registered_client_id", "principal_name", "authorization_grant_type", "authorized_scopes", "state"));
		List<Object> values = new ArrayList<>(List.of(
				id, registeredClientId, principalName, GRANT_TYPE, SCOPES, "state-" + id));
		for (Token token : tokens) {
			String tokenValue = token.prefix() + "-" + id;
			columns.add(token.prefix() + "_value");
			values.add(textTokenValues ? tokenValue : tokenValue.getBytes(StandardCharsets.UTF_8));
			columns.add(token.prefix() + "_issued_at");
			values.add(timestamp(token.issuedAt()));
			columns.add(token.prefix() + "_expires_at");
			values.add(timestamp(token.expiresAt()));
		}
		String sql = "INSERT INTO oauth2_authorization (" + String.join(", ", columns) + ") VALUES ("
				+ String.join(", ", columns.stream().map(c -> "?").toList()) + ")";
		jdbc.update(sql, values.toArray());
	}

	static void insertConsent(JdbcTemplate jdbc, String registeredClientId, String principalName) {
		jdbc.update("INSERT INTO oauth2_authorization_consent (registered_client_id, principal_name, authorities)"
				+ " VALUES (?, ?, ?)", registeredClientId, principalName, "SCOPE_" + SCOPES);
	}

	/** Audit row without {@code additional_data}, so no vendor-specific JSON binding is needed. */
	static void insertAudit(JdbcTemplate jdbc, String eventType, String clientId, Instant eventTimestamp) {
		jdbc.update("INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, event_timestamp)"
				+ " VALUES (?, ?, ?, ?, ?)", eventType, clientId, "retention-test", true, Timestamp.from(eventTimestamp));
	}

	static List<String> authorizationIds(JdbcTemplate jdbc) {
		return jdbc.queryForList("SELECT id FROM oauth2_authorization ORDER BY id", String.class);
	}

	static List<PurgeAudit> purgeAudits(JdbcTemplate jdbc) {
		return jdbc.query("SELECT client_id, principal_name, success, grant_type, scopes_granted, additional_data"
						+ " FROM oauth2_audit_log WHERE event_type = ? ORDER BY id",
				(rs, rowNum) -> new PurgeAudit(
						rs.getString("client_id"),
						rs.getString("principal_name"),
						rs.getBoolean("success"),
						rs.getString("grant_type"),
						rs.getString("scopes_granted"),
						JSON.readTree(rs.getString("additional_data"))),
				AuthorizationPurgeService.PURGE_EVENT_TYPE);
	}

	static List<Instant> auditTimestamps(JdbcTemplate jdbc, String eventType) {
		return jdbc.query("SELECT event_timestamp FROM oauth2_audit_log WHERE event_type = ? ORDER BY event_timestamp",
				(rs, rowNum) -> rs.getTimestamp("event_timestamp").toInstant(), eventType);
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}
}
