/*
 * Copyright 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.repository;

import org.greenbuttonalliance.espi.authserver.service.AuthorizationPurgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.Objects;

/**
 * Writes the administrators' events to {@code oauth2_audit_log} (#344 review). {@code additional_data} is JSON on
 * MySQL and H2 but JSONB on PostgreSQL, which refuses a plain text parameter, so the parameter is written the way
 * {@link AuthorizationPurgeService#jsonParameterFor} gives for the database in use.
 *
 * <p>An audit failure is logged and never fails the change it records, as before.</p>
 */
@Repository
public class AuditLogDao {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogDao.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    /** Resolved from the database product name on first use. */
    private volatile String insertSql;

    public AuditLogDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    /**
     * Records one event.
     *
     * @param principalName the signed-in administrator, or {@code null} when there is none
     */
    public void record(String eventType, String clientId, String principalName, boolean success,
            Map<String, ?> additionalData) {
        try {
            jdbcTemplate.update(insertSql(), eventType, clientId, principalName, success,
                    JSON.writeValueAsString(additionalData));
        } catch (Exception e) {
            logger.error("Failed to log audit event {}", eventType, e);
        }
    }

    private String insertSql() {
        String sql = insertSql;
        if (sql == null) {
            sql = "INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, additional_data)"
                    + " VALUES (?, ?, ?, ?, " + AuthorizationPurgeService.jsonParameterFor(databaseProductName())
                    + ")";
            insertSql = sql;
        }
        return sql;
    }

    private String databaseProductName() {
        try {
            return JdbcUtils.extractDatabaseMetaData(
                    Objects.requireNonNull(jdbcTemplate.getDataSource(), "dataSource"),
                    DatabaseMetaData::getDatabaseProductName);
        } catch (Exception e) {
            logger.warn("Could not determine the database product for the audit insert; "
                    + "binding additional_data as a plain parameter", e);
            return "";
        }
    }
}
