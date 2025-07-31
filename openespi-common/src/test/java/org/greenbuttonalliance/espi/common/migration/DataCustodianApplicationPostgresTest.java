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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the OpenESPI Data Custodian Spring Boot application with PostgreSQL Test Container.
 * 
 * This test verifies that the application context loads successfully with a real PostgreSQL database
 * running in a Docker container, and that Flyway migrations execute correctly with the new
 * vendor-specific migration structure.
 */
@SpringBootTest(classes = { TestApplication.class })
@ActiveProfiles("test-postgres")
@Testcontainers
@DisplayName("PostgreSQL Test Container Integration Tests")
class DataCustodianApplicationPostgresTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("openespi_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Configure Flyway locations for PostgreSQL vendor-specific migrations
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/vendor/postgres");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.validate-on-migrate", () -> "true");
        
        // JPA/Hibernate configuration for PostgreSQL
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "true");
    }

    /**
     * Test that the Spring Boot application context loads successfully with PostgreSQL Test Container.
     * This verifies that all configuration classes, beans, and dependencies
     * are properly configured and can be instantiated with a real PostgreSQL database.
     */
    @Test
    @DisplayName("Application context loads with PostgreSQL container")
    void contextLoads() {
        // Verify that the PostgreSQL container is running
        assertTrue(postgresContainer.isRunning(), "PostgreSQL container should be running");
        
        // Verify that the container has the expected configuration
        assertEquals("openespi_test", postgresContainer.getDatabaseName());
        assertEquals("testuser", postgresContainer.getUsername());
        assertEquals("testpass", postgresContainer.getPassword());
        
        // This test passes if the application context loads without errors
        // It validates the entire Spring Boot configuration including:
        // - PostgreSQL Test Container configuration
        // - JPA configuration with PostgreSQL dialect
        // - Flyway migration configuration
        // - Service layer beans
        // - Repository layer beans
    }

    /**
     * Test that database migrations execute successfully with the new vendor-specific structure.
     * This verifies that both base migrations and PostgreSQL-specific migrations created the expected tables.
     */
    @Test
    @DisplayName("Database migrations execute successfully")
    void databaseMigrationsExecute() throws SQLException {
        // Verify that the PostgreSQL container is running
        assertTrue(postgresContainer.isRunning(), "PostgreSQL container should be running");
        
        // Connect to the database and verify that expected tables exist
        try (Connection connection = postgresContainer.createConnection("")) {
            
            // Verify base migration tables exist (from V1__Create_Base_Tables.sql)
            assertTrue(tableExists(connection, "application_information"), 
                "application_information table should exist from base migration");
            assertTrue(tableExists(connection, "retail_customers"), 
                "retail_customers table should exist from base migration");
            assertTrue(tableExists(connection, "reading_types"), 
                "reading_types table should exist from base migration");
            assertTrue(tableExists(connection, "subscriptions"), 
                "subscriptions table should exist from base migration");
            assertTrue(tableExists(connection, "batch_lists"), 
                "batch_lists table should exist from base migration");
            
            // Verify PostgreSQL-specific migration tables exist (from V2__PostgreSQL_Specific_Tables.sql)
            assertTrue(tableExists(connection, "time_configurations"), 
                "time_configurations table should exist from PostgreSQL-specific migration");
            assertTrue(tableExists(connection, "usage_points"), 
                "usage_points table should exist from PostgreSQL-specific migration");
            assertTrue(tableExists(connection, "meter_readings"), 
                "meter_readings table should exist from PostgreSQL-specific migration");
            assertTrue(tableExists(connection, "interval_blocks"), 
                "interval_blocks table should exist from PostgreSQL-specific migration");
            
            // Verify that BYTEA columns exist in PostgreSQL-specific tables
            assertTrue(columnExists(connection, "time_configurations", "dst_end_rule"), 
                "dst_end_rule BYTEA column should exist in time_configurations");
            assertTrue(columnExists(connection, "time_configurations", "dst_start_rule"), 
                "dst_start_rule BYTEA column should exist in time_configurations");
            assertTrue(columnExists(connection, "usage_points", "role_flags"), 
                "role_flags BYTEA column should exist in usage_points");
        }
    }
    
    /**
     * Helper method to check if a table exists in the database.
     */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toLowerCase(), null)) {
            return rs.next();
        }
    }
    
    /**
     * Helper method to check if a column exists in a table.
     */
    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
            return rs.next();
        }
    }
}