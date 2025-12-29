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

import jakarta.validation.Validator;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base class for JPA repository tests providing common utilities and configuration.
 * 
 * This abstract class provides:
 * - Common @DataJpaTest configuration
 * - DataFaker instance for realistic test data generation
 * - TestEntityManager for direct entity operations
 * - Validator for constraint testing
 * - Common utility methods for test data creation
 */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = org.greenbuttonalliance.espi.common.TestApplication.class)
public abstract class BaseRepositoryTest {

    /**
     * DataFaker instance for generating realistic test data.
     */
    protected static final Faker faker = new Faker();

    /**
     * TestEntityManager for direct entity operations in tests.
     */
    @Autowired
    protected TestEntityManager entityManager;

    /**
     * Bean validator for testing validation constraints.
     * Initialized manually since @DataJpaTest doesn't include validation auto-configuration.
     */
    protected Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Generates a random UUID for testing.
     * 
     * @return Random UUID
     */
    protected UUID randomUuid() {
        return UUID.randomUUID();
    }

    /**
     * Generates a random OffsetDateTime for testing.
     * 
     * @return Random OffsetDateTime
     */
    protected OffsetDateTime randomOffsetDateTime() {
        return faker.date().past(365, java.util.concurrent.TimeUnit.DAYS).toInstant()
                .atOffset(java.time.ZoneOffset.UTC);
    }

    /**
     * Generates a random LocalDateTime for testing.
     * 
     * @return Random LocalDateTime
     */
    protected LocalDateTime randomLocalDateTime() {
        return faker.date().past(365, java.util.concurrent.TimeUnit.DAYS).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Generates a random description string for testing.
     * 
     * @return Random description
     */
    protected String randomDescription() {
        return faker.lorem().sentence(5, 10);
    }

    /**
     * Generates a random company name for testing.
     * 
     * @return Random company name
     */
    protected String randomCompanyName() {
        return faker.company().name();
    }

    /**
     * Generates a random phone number for testing.
     * 
     * @return Random phone number
     */
    protected String randomPhoneNumber() {
        return faker.phoneNumber().phoneNumber();
    }

    /**
     * Generates a random address for testing.
     * 
     * @return Random address
     */
    protected String randomAddress() {
        return faker.address().fullAddress();
    }

    /**
     * Generates a random email for testing.
     * 
     * @return Random email
     */
    protected String randomEmail() {
        return faker.internet().emailAddress();
    }

    /**
     * Generates a random numeric string of specified length.
     * 
     * @param length Length of the numeric string
     * @return Random numeric string
     */
    protected String randomNumericString(int length) {
        return faker.number().digits(length);
    }

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

    /**
     * Finds an entity by ID and ensures it's detached from the persistence context.
     * 
     * @param entityClass Entity class
     * @param id Entity ID
     * @param <T> Entity type
     * @param <ID> ID type
     * @return Found entity or null
     */
    protected <T, ID> T findAndDetach(Class<T> entityClass, ID id) {
        T entity = entityManager.find(entityClass, id);
        if (entity != null) {
            entityManager.detach(entity);
        }
        return entity;
    }
}