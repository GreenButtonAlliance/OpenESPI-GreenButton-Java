/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.common.test;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.validation.Validator;

/**
 * Base class for TestContainers integration tests providing MySQL and PostgreSQL container setup.
 *
 * This abstract class provides:
 * - TestContainers setup for MySQL and PostgreSQL
 * - Dynamic datasource configuration from containers
 * - DataJpaTest configuration with real databases
 * - TestEntityManager for direct entity operations
 * - Validator for constraint testing
 *
 * Subclasses should use either {@link #mysqlContainer} or {@link #postgresqlContainer}
 * and configure the datasource properties accordingly.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = org.greenbuttonalliance.espi.common.TestApplication.class)
@ActiveProfiles("test")
public abstract class BaseTestContainersTest {

    /**
     * MySQL 8.0 container for integration testing.
     * Reusable across tests for better performance.
     */
    protected static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("openespi_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    /**
     * PostgreSQL 15 container for integration testing.
     * Reusable across tests for better performance.
     */
    protected static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("openespi_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    /**
     * TestEntityManager for direct entity operations in tests.
     */
    @Autowired
    protected TestEntityManager entityManager;

    /**
     * Bean validator for testing validation constraints.
     */
    protected Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Flushes and clears the entity manager to ensure database synchronization.
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Persists an entity and flushes to ensure it's saved to the database.
     *
     * @param entity Entity to persist
     * @param <T> Entity type
     * @return Persisted entity
     */
    protected <T> T persistAndFlush(T entity) {
        T persisted = entityManager.persistAndFlush(entity);
        entityManager.clear();
        return persisted;
    }

    /**
     * Merges a detached entity and flushes to ensure updates are persisted.
     * Use this for updating entities between operations where the context was cleared.
     *
     * @param entity Detached or managed entity with modifications
     * @param <T> Entity type
     * @return Managed, updated entity
     */
    protected <T> T mergeAndFlush(T entity) {
        T managed = entityManager.merge(entity);
        entityManager.flush();
        entityManager.clear();
        return managed;
    }
}