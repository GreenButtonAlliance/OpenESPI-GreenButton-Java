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

import org.greenbuttonalliance.espi.authserver.config.AuthorizationPurgeProperties;
import org.greenbuttonalliance.espi.authserver.service.AuthorizationPurgeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * #344 review (round 3): the database product is read once, when the audit writer is created, and the AS does not
 * start if it cannot be read. Before, a failed lookup fell back to a plain parameter and was kept, so PostgreSQL
 * refused every later audit row until restart.
 */
@DisplayName("AuditLogDao (#344 review)")
class AuditLogDaoTest {

	private static JdbcTemplate jdbcTemplateFor(DataSource dataSource) {
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
		return jdbcTemplate;
	}

	private static DataSource dataSourceOf(String productName) throws SQLException {
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		when(metaData.getDatabaseProductName()).thenReturn(productName);
		Connection connection = mock(Connection.class);
		when(connection.getMetaData()).thenReturn(metaData);
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenReturn(connection);
		return dataSource;
	}

	@Test
	@DisplayName("a database product that cannot be read stops the audit writer and the purge from being created")
	void anUnreadableDatabaseProductFailsStartup() throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenThrow(new SQLException("pool exhausted"));
		JdbcTemplate jdbcTemplate = jdbcTemplateFor(dataSource);

		assertThatThrownBy(() -> new AuditLogDao(jdbcTemplate)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("database product");
		assertThatThrownBy(() -> new AuthorizationPurgeService(jdbcTemplate, mock(TransactionTemplate.class),
				new AuthorizationPurgeProperties(true, Duration.ofHours(1), Duration.ofMinutes(5), Duration.ofDays(7),
						500, Duration.ofDays(395)), Clock.systemUTC()))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("database product");
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {"PostgreSQL|CAST(? AS JSONB)", "H2|? FORMAT JSON", "MySQL|?"})
	@DisplayName("the audit INSERT binds additional_data the way the database in use needs")
	void bindsAdditionalDataForTheDatabase(String productName, String parameter) throws SQLException {
		JdbcTemplate jdbcTemplate = jdbcTemplateFor(dataSourceOf(productName));

		new AuditLogDao(jdbcTemplate).record("client_deleted", "tp", "admin-1", true, Map.of("n", 1));

		verify(jdbcTemplate).update(eq("INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success,"
				+ " additional_data) VALUES (?, ?, ?, ?, " + parameter + ")"), eq("client_deleted"), eq("tp"),
				eq("admin-1"), eq(true), eq("{\"n\":1}"));
		assertThat(AuthorizationPurgeService.jsonParameterFor(productName)).isEqualTo(parameter);
	}

	@Test
	@DisplayName("an audit row the database refuses is logged and does not fail the change it records")
	void aRefusedAuditRowDoesNotThrow() throws SQLException {
		JdbcTemplate jdbcTemplate = jdbcTemplateFor(dataSourceOf("MySQL"));
		when(jdbcTemplate.update(anyString(), any(Object[].class)))
				.thenThrow(new DataIntegrityViolationException("refused"));
		AuditLogDao auditLog = new AuditLogDao(jdbcTemplate);

		assertThatCode(() -> auditLog.record("client_deleted", "tp", null, true, Map.of()))
				.doesNotThrowAnyException();
	}
}
