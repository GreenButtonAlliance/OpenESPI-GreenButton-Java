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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

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

    private final String insertSql;

    /**
     * Reads the database product once, here, so the AS does not start if it cannot: a lookup that failed later
     * and fell back to a plain parameter would make PostgreSQL refuse every audit row until restart.
     *
     * @throws IllegalStateException if the database product cannot be read
     */
    @Autowired
    public AuditLogDao(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, AuthorizationPurgeService.jsonParameterFor(jdbcTemplate));
    }

    /** For a known {@code additional_data} parameter, as {@link AuthorizationPurgeService#jsonParameterFor} gives. */
    public AuditLogDao(JdbcTemplate jdbcTemplate, String jsonParameter) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.insertSql = "INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success,"
                + " additional_data) VALUES (?, ?, ?, ?, " + Objects.requireNonNull(jsonParameter, "jsonParameter")
                + ")";
    }

    /**
     * Records one event.
     *
     * @param principalName the signed-in administrator, or {@code null} when there is none
     */
    public void record(String eventType, String clientId, String principalName, boolean success,
            Map<String, ?> additionalData) {
        try {
            jdbcTemplate.update(insertSql, eventType, clientId, principalName, success,
                    JSON.writeValueAsString(additionalData));
        } catch (Exception e) {
            logger.error("Failed to log audit event {}", eventType, e);
        }
    }
}
