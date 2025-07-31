/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.common.migration;

import org.greenbuttonalliance.espi.common.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the OpenESPI Data Custodian Spring Boot application with H2 In-Memory Database.
 * 
 * This test verifies that the application context loads successfully with H2 in-memory database
 * and that Flyway migrations execute correctly with the new vendor-specific migration structure.
 * H2 uses BINARY columns for byte array data instead of BLOB/BYTEA.
 */
@SpringBootTest(classes = { TestApplication.class })
@ActiveProfiles("test")
@DisplayName("H2 In-Memory Database Integration Tests")
class DataCustodianApplicationH2Test {

    @Autowired
    private DataSource dataSource;

    /**
     * Test that the Spring Boot application context loads successfully with H2 in-memory database.
     * This verifies that all configuration classes, beans, and dependencies
     * are properly configured and can be instantiated with H2 database.
     */
    @Test
    @DisplayName("Application context loads with H2 database")
    void contextLoads() {
        // Verify that DataSource is available
        assertNotNull(dataSource, "DataSource should be autowired");
        
        // This test passes if the application context loads without errors
        // It validates the entire Spring Boot configuration including:
        // - H2 in-memory database configuration
        // - JPA configuration with H2 dialect
        // - Flyway migration configuration
        // - Service layer beans
        // - Repository layer beans
    }

    /**
     * Test that database migrations execute successfully with the new vendor-specific structure.
     * This verifies that both base migrations and H2-specific migrations created the expected tables.
     */
    @Test
    @DisplayName("Database migrations execute successfully")
    void flywayMigrationsExecute() throws SQLException {
        // Connect to the H2 database and verify that expected tables exist
        try (Connection connection = dataSource.getConnection()) {
            
            // Verify base migration tables exist (from V1__Create_Base_Tables.sql)
            assertTrue(tableExists(connection, "APPLICATION_INFORMATION"), 
                "APPLICATION_INFORMATION table should exist from base migration");
            assertTrue(tableExists(connection, "RETAIL_CUSTOMERS"), 
                "RETAIL_CUSTOMERS table should exist from base migration");
            assertTrue(tableExists(connection, "READING_TYPES"), 
                "READING_TYPES table should exist from base migration");
            assertTrue(tableExists(connection, "SUBSCRIPTIONS"), 
                "SUBSCRIPTIONS table should exist from base migration");
            assertTrue(tableExists(connection, "BATCH_LISTS"), 
                "BATCH_LISTS table should exist from base migration");
            
            // Verify H2-specific migration tables exist (from V2__H2_Specific_Tables.sql)
            assertTrue(tableExists(connection, "TIME_CONFIGURATIONS"), 
                "TIME_CONFIGURATIONS table should exist from H2-specific migration");
            assertTrue(tableExists(connection, "USAGE_POINTS"), 
                "USAGE_POINTS table should exist from H2-specific migration");
            assertTrue(tableExists(connection, "METER_READINGS"), 
                "METER_READINGS table should exist from H2-specific migration");
            assertTrue(tableExists(connection, "INTERVAL_BLOCKS"), 
                "INTERVAL_BLOCKS table should exist from H2-specific migration");
            
            // Verify that BINARY columns exist in H2-specific tables
            assertTrue(columnExists(connection, "TIME_CONFIGURATIONS", "DST_END_RULE"), 
                "DST_END_RULE BINARY column should exist in TIME_CONFIGURATIONS");
            assertTrue(columnExists(connection, "TIME_CONFIGURATIONS", "DST_START_RULE"), 
                "DST_START_RULE BINARY column should exist in TIME_CONFIGURATIONS");
            assertTrue(columnExists(connection, "USAGE_POINTS", "ROLE_FLAGS"), 
                "ROLE_FLAGS BINARY column should exist in USAGE_POINTS");
        }
    }

    /**
     * Test that binary column types work correctly with H2 database.
     * This verifies that byte array data can be stored and retrieved from BINARY columns.
     */
    @Test
    @DisplayName("Binary column types work correctly")
    void binaryColumnTypesWork() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            
            // Test inserting and retrieving byte array data in TIME_CONFIGURATIONS table
            byte[] testData = "Test binary data for H2".getBytes();
            
            // Insert test data
            String insertSql = "INSERT INTO TIME_CONFIGURATIONS (ID, DST_END_RULE, DST_OFFSET, TZ_OFFSET, created, updated ) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setObject(1, java.util.UUID.randomUUID());
                insertStmt.setBytes(2, testData);
                insertStmt.setLong(3, 3600000L); // 1 hour in milliseconds
                insertStmt.setLong(4, -18000000L); // -5 hours in milliseconds
                insertStmt.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
                insertStmt.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
                int rowsInserted = insertStmt.executeUpdate();
                assertEquals(1, rowsInserted, "Should insert exactly one row");
            }
            
            // Retrieve and verify the data
            String selectSql = "SELECT DST_END_RULE FROM TIME_CONFIGURATIONS WHERE DST_END_RULE IS NOT NULL";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql);
                 ResultSet rs = selectStmt.executeQuery()) {
                
                assertTrue(rs.next(), "Should find the inserted row");
                byte[] retrievedData = rs.getBytes("DST_END_RULE");
                assertNotNull(retrievedData, "Retrieved binary data should not be null");
                assertArrayEquals(testData, retrievedData, "Retrieved data should match inserted data");
            }
        }
    }
    
    /**
     * Helper method to check if a table exists in the database.
     */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toUpperCase(), null)) {
            return rs.next();
        }
    }
    
    /**
     * Helper method to check if a column exists in a table.
     */
    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return rs.next();
        }
    }
}