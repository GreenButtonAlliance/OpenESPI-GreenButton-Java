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

package org.greenbuttonalliance.espi.authserver.service;

import org.greenbuttonalliance.espi.authserver.config.AuthorizationPurgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Deletes fully-expired rows from {@code oauth2_authorization}, writing one
 * {@value #PURGE_EVENT_TYPE} row to {@code oauth2_audit_log} per deleted authorization (#192).
 *
 * <p>Spring Authorization Server's {@code JdbcOAuth2AuthorizationService} never deletes rows, so
 * without this job the table (each row carrying serialized token data) grows forever.</p>
 *
 * <h2>Eligibility</h2>
 * <p>A row is purged only when <strong>every</strong> non-null token expiry on it (authorization
 * code, access token, OIDC ID token, refresh token, user code, device code) is earlier than
 * {@code now - gracePeriod}, and the row has at least one non-null expiry. Deleting on access-token
 * expiry alone would break long-lived ESPI authorizations that refresh. As a safety net, a row that
 * holds a token value without an expiry is never purged (such a token would never expire).
 * Rows with no expiry at all, e.g. abandoned authorization requests holding only {@code state}, are
 * deliberately left alone.</p>
 *
 * <h2>Audit before delete, safe across instances</h2>
 * <p>Each chunk runs in one transaction: select up to {@code batchSize} candidate rows, then for each
 * one {@code DELETE ... WHERE id = ? AND <same eligibility>}; the audit row is inserted only when that
 * delete removed exactly one row. If two AS instances race on the same row, only the one whose
 * delete wins writes the audit record, so a purge is never audited twice. Candidates are processed
 * in {@code id} order so concurrent instances lock rows in the same order.</p>
 *
 * <h2>Audit retention</h2>
 * <p>After the purge pass, {@value #PURGE_EVENT_TYPE} audit rows older than
 * {@code auditRetention} are deleted. Other audit event types are never touched.</p>
 *
 * <p>Never touches {@code oauth2_authorization_consent} or {@code oauth2_registered_client} (the
 * latter is only read, to resolve the public {@code client_id} for the audit record). All SQL is
 * portable across H2, MySQL 8 and PostgreSQL; time thresholds are computed in Java and bound as
 * parameters. The one vendor difference is how a JSON string is bound into
 * {@code additional_data} (JSON on MySQL/H2, JSONB on PostgreSQL).</p>
 */
public class AuthorizationPurgeService {

	/** {@code oauth2_audit_log.event_type} written for each purged authorization. */
	public static final String PURGE_EVENT_TYPE = "AUTHORIZATION_PURGED";

	/**
	 * Token column prefixes of {@code oauth2_authorization}; each has {@code _value},
	 * {@code _issued_at} and {@code _expires_at} columns in every vendor schema.
	 */
	static final List<String> TOKEN_COLUMN_PREFIXES = List.of(
			"authorization_code", "access_token", "oidc_id_token", "refresh_token", "user_code", "device_code");

	/** Width of {@code oauth2_audit_log.grant_type} (narrower than the authorization's column). */
	private static final int AUDIT_GRANT_TYPE_LENGTH = 50;

	private static final Logger logger = LoggerFactory.getLogger(AuthorizationPurgeService.class);

	private static final String SELECT_CANDIDATES_SQL = "SELECT a.id, a.registered_client_id, c.client_id,"
			+ " a.principal_name, a.authorization_grant_type, a.authorized_scopes, "
			+ TOKEN_COLUMN_PREFIXES.stream()
					.map(p -> "a." + p + "_issued_at, a." + p + "_expires_at")
					.collect(Collectors.joining(", "))
			+ " FROM oauth2_authorization a"
			+ " LEFT JOIN oauth2_registered_client c ON c.id = a.registered_client_id"
			+ " WHERE " + eligibility("a.")
			+ " ORDER BY a.id"
			+ " LIMIT ?";

	private static final String DELETE_AUTHORIZATION_SQL = "DELETE FROM oauth2_authorization WHERE id = ? AND "
			+ eligibility("");

	private static final String DELETE_EXPIRED_AUDIT_SQL =
			"DELETE FROM oauth2_audit_log WHERE event_type = ? AND event_timestamp < ?";

	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;
	private final AuthorizationPurgeProperties properties;
	private final Clock clock;
	private final ObjectMapper objectMapper = new ObjectMapper();

	/** Resolved lazily from the database product name on first use. */
	private volatile String insertAuditSql;

	public AuthorizationPurgeService(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate,
			AuthorizationPurgeProperties properties, Clock clock) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
		this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
		this.properties = Objects.requireNonNull(properties, "properties");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	/**
	 * Run one purge pass followed by one audit-retention pass.
	 *
	 * @return counts of deleted authorization rows and deleted (expired) purge-audit rows
	 */
	public PurgeResult purge() {
		long startNanos = System.nanoTime();
		Instant now = clock.instant();
		Instant cutoff = now.minus(properties.gracePeriod());
		Instant auditCutoff = now.minus(properties.auditRetention());

		int purged = 0;
		while (true) {
			ChunkResult chunk = Objects.requireNonNull(
					transactionTemplate.execute(status -> purgeChunk(now, cutoff)));
			purged += chunk.deleted();
			// Stop on a short chunk (nothing left) or on a chunk where every delete lost to another
			// instance (it is working through the same rows; the next scheduled run continues).
			if (chunk.selected() < properties.batchSize() || chunk.deleted() == 0) {
				break;
			}
		}

		Integer auditExpired = transactionTemplate.execute(status -> jdbcTemplate.update(
				DELETE_EXPIRED_AUDIT_SQL, PURGE_EVENT_TYPE, Timestamp.from(auditCutoff)));
		PurgeResult result = new PurgeResult(purged, auditExpired == null ? 0 : auditExpired);

		if (result.authorizationsPurged() > 0 || result.auditRecordsExpired() > 0) {
			logger.info("Authorization purge: deleted {} fully-expired oauth2_authorization row(s) (latest expiry before {}),"
							+ " deleted {} {} audit row(s) older than {}, in {} ms",
					result.authorizationsPurged(), cutoff, result.auditRecordsExpired(), PURGE_EVENT_TYPE,
					auditCutoff, (System.nanoTime() - startNanos) / 1_000_000);
		}
		return result;
	}

	private ChunkResult purgeChunk(Instant now, Instant cutoff) {
		Object[] selectArgs = new Object[TOKEN_COLUMN_PREFIXES.size() + 1];
		fillCutoffs(selectArgs, 0, cutoff);
		selectArgs[selectArgs.length - 1] = properties.batchSize();
		List<Candidate> candidates = jdbcTemplate.query(SELECT_CANDIDATES_SQL, this::mapCandidate, selectArgs);

		Object[] deleteArgs = new Object[TOKEN_COLUMN_PREFIXES.size() + 1];
		fillCutoffs(deleteArgs, 1, cutoff);
		String auditSql = insertAuditSql();
		Timestamp eventTimestamp = Timestamp.from(now);

		int deleted = 0;
		for (Candidate candidate : candidates) {
			deleteArgs[0] = candidate.id();
			if (jdbcTemplate.update(DELETE_AUTHORIZATION_SQL, deleteArgs) != 1) {
				// Refreshed since the select, or already purged (and audited) by another instance.
				continue;
			}
			jdbcTemplate.update(auditSql,
					PURGE_EVENT_TYPE,
					candidate.clientId() != null ? candidate.clientId() : candidate.registeredClientId(),
					candidate.principalName(),
					true,
					truncate(candidate.grantType(), AUDIT_GRANT_TYPE_LENGTH),
					candidate.authorizedScopes(),
					eventTimestamp,
					additionalData(candidate));
			deleted++;
		}
		return new ChunkResult(candidates.size(), deleted);
	}

	private Candidate mapCandidate(ResultSet rs, int rowNum) throws SQLException {
		List<Instant> issued = new ArrayList<>();
		List<Instant> expires = new ArrayList<>();
		for (String prefix : TOKEN_COLUMN_PREFIXES) {
			addIfPresent(issued, rs.getTimestamp(prefix + "_issued_at"));
			addIfPresent(expires, rs.getTimestamp(prefix + "_expires_at"));
		}
		return new Candidate(
				rs.getString("id"),
				rs.getString("registered_client_id"),
				rs.getString("client_id"),
				rs.getString("principal_name"),
				rs.getString("authorization_grant_type"),
				rs.getString("authorized_scopes"),
				issued.stream().min(Comparator.naturalOrder()).orElse(null),
				issued.stream().max(Comparator.naturalOrder()).orElse(null),
				expires.stream().max(Comparator.naturalOrder()).orElse(null));
	}

	private String additionalData(Candidate candidate) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("authorization_id", candidate.id());
		data.put("registered_client_id", candidate.registeredClientId());
		data.put("first_issued_at", isoOrNull(candidate.firstIssuedAt()));
		data.put("last_issued_at", isoOrNull(candidate.lastIssuedAt()));
		data.put("latest_expires_at", isoOrNull(candidate.latestExpiresAt()));
		data.put("grace_period", properties.gracePeriod().toString());
		return objectMapper.writeValueAsString(data);
	}

	/**
	 * The audit INSERT. {@code additional_data} is JSON on MySQL and H2 and JSONB on PostgreSQL, and
	 * each needs a string parameter bound differently: MySQL parses a plain string, PostgreSQL rejects
	 * a varchar into JSONB without a cast, and H2 would store a plain string as a JSON <em>string
	 * value</em> unless told the text is already JSON ({@code FORMAT JSON}).
	 */
	private String insertAuditSql() {
		String sql = insertAuditSql;
		if (sql == null) {
			String jsonParameter = jsonParameterFor(databaseProductName());
			sql = "INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, grant_type,"
					+ " scopes_granted, event_timestamp, additional_data) VALUES (?, ?, ?, ?, ?, ?, ?, "
					+ jsonParameter + ")";
			insertAuditSql = sql;
		}
		return sql;
	}

	static String jsonParameterFor(String databaseProductName) {
		String product = databaseProductName == null ? "" : databaseProductName.toLowerCase(Locale.ROOT);
		if (product.contains("postgres")) {
			return "CAST(? AS JSONB)";
		}
		if (product.equals("h2")) {
			return "? FORMAT JSON";
		}
		return "?";
	}

	private String databaseProductName() {
		try {
			return JdbcUtils.extractDatabaseMetaData(
					Objects.requireNonNull(jdbcTemplate.getDataSource(), "dataSource"),
					DatabaseMetaData::getDatabaseProductName);
		} catch (Exception e) {
			logger.warn("Could not determine database product for the authorization purge audit insert; "
					+ "binding additional_data as a plain parameter", e);
			return "";
		}
	}

	/**
	 * The eligibility predicate, with one {@code ?} cutoff parameter per token type, in
	 * {@link #TOKEN_COLUMN_PREFIXES} order.
	 */
	static String eligibility(String alias) {
		String anyExpiry = TOKEN_COLUMN_PREFIXES.stream()
				.map(p -> alias + p + "_expires_at IS NOT NULL")
				.collect(Collectors.joining(" OR ", "(", ")"));
		String allExpired = TOKEN_COLUMN_PREFIXES.stream()
				.map(p -> "(" + alias + p + "_expires_at IS NULL OR " + alias + p + "_expires_at < ?)")
				.collect(Collectors.joining(" AND "));
		String noUnboundedToken = TOKEN_COLUMN_PREFIXES.stream()
				.map(p -> "(" + alias + p + "_value IS NULL OR " + alias + p + "_expires_at IS NOT NULL)")
				.collect(Collectors.joining(" AND "));
		return anyExpiry + " AND " + allExpired + " AND " + noUnboundedToken;
	}

	private static void fillCutoffs(Object[] args, int offset, Instant cutoff) {
		Timestamp cutoffTimestamp = Timestamp.from(cutoff);
		for (int i = 0; i < TOKEN_COLUMN_PREFIXES.size(); i++) {
			args[offset + i] = cutoffTimestamp;
		}
	}

	private static void addIfPresent(List<Instant> target, Timestamp timestamp) {
		if (timestamp != null) {
			target.add(timestamp.toInstant());
		}
	}

	private static String isoOrNull(Instant instant) {
		return instant == null ? null : instant.toString();
	}

	private static String truncate(String value, int maxLength) {
		return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	/**
	 * Outcome of one {@link #purge()} run.
	 *
	 * @param authorizationsPurged {@code oauth2_authorization} rows deleted (each one audited)
	 * @param auditRecordsExpired  {@value #PURGE_EVENT_TYPE} audit rows deleted by the retention pass
	 */
	public record PurgeResult(int authorizationsPurged, int auditRecordsExpired) {
	}

	private record ChunkResult(int selected, int deleted) {
	}

	private record Candidate(String id, String registeredClientId, String clientId, String principalName,
			String grantType, String authorizedScopes, Instant firstIssuedAt, Instant lastIssuedAt,
			Instant latestExpiresAt) {
	}
}
