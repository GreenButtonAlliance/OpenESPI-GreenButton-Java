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

package org.greenbuttonalliance.espi.common.repositories.integration;

import org.greenbuttonalliance.espi.common.domain.customer.common.ProgramDateIdMapping;
import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;
import org.greenbuttonalliance.espi.common.domain.customer.enums.ProgramDateKind;
import org.greenbuttonalliance.espi.common.repositories.customer.ProgramDateIdMappingsRepository;
import org.greenbuttonalliance.espi.common.test.BaseTestContainersTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProgramDateIdMappings entity integration tests using MySQL TestContainer.
 *
 * Tests full CRUD operations and embedded object persistence with a real MySQL database.
 */
@DisplayName("ProgramDateIdMappings Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class ProgramDateIdMappingsMySQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final org.testcontainers.containers.MySQLContainer<?> mysql = mysqlContainer;

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void configureMySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired
    private ProgramDateIdMappingsRepository repository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve program date ID mappings with all fields")
        void shouldSaveAndRetrieveProgramDateIdMappingsWithAllFields() {
            // Arrange
            ProgramDateIdMapping mapping = new ProgramDateIdMapping(
                    ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE,
                    "MYSQL-CODE-001",
                    "MySQL DR Program Enrollment",
                    "Test note for MySQL enrollment"
            );

            ProgramDateIdMappingsEntity entity = new ProgramDateIdMappingsEntity();
            entity.setDescription("MySQL Integration Test");
            entity.setProgramDateIdMapping(mapping);

            // Act
            ProgramDateIdMappingsEntity savedEntity = repository.save(entity);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = repository.findById(savedEntity.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(result -> assertThat(result)
                            .extracting(
                                    ProgramDateIdMappingsEntity::getDescription,
                                    e -> e.getProgramDateIdMapping().getProgramDateType(),
                                    e -> e.getProgramDateIdMapping().getCode(),
                                    e -> e.getProgramDateIdMapping().getName(),
                                    e -> e.getProgramDateIdMapping().getNote()
                            )
                            .containsExactly(
                                    "MySQL Integration Test",
                                    ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE,
                                    "MYSQL-CODE-001",
                                    "MySQL DR Program Enrollment",
                                    "Test note for MySQL enrollment"
                            ));
        }

        @Test
        @DisplayName("Should update program date ID mappings fields")
        void shouldUpdateProgramDateIdMappingsFields() {
            // Arrange
            ProgramDateIdMapping originalMapping = new ProgramDateIdMapping(
                    ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE,
                    "ORIGINAL-CODE",
                    "Original Name",
                    "Original Note"
            );
            ProgramDateIdMappingsEntity entity = new ProgramDateIdMappingsEntity();
            entity.setProgramDateIdMapping(originalMapping);
            entity.setDescription("Original Description");
            ProgramDateIdMappingsEntity savedEntity = repository.save(entity);
            flushAndClear();

            // Act
            ProgramDateIdMapping updatedMapping = new ProgramDateIdMapping(
                    ProgramDateKind.CUST_DR_PROGRAM_DE_ENROLLMENT_DATE,
                    "UPDATED-CODE",
                    "Updated Name",
                    "Updated Note"
            );
            savedEntity.setProgramDateIdMapping(updatedMapping);
            savedEntity.setDescription("Updated Description");
            ProgramDateIdMappingsEntity updated = repository.save(savedEntity);
            flushAndClear();

            Optional<ProgramDateIdMappingsEntity> retrieved = repository.findById(updated.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(result -> assertThat(result)
                            .extracting(
                                    ProgramDateIdMappingsEntity::getDescription,
                                    e -> e.getProgramDateIdMapping().getProgramDateType(),
                                    e -> e.getProgramDateIdMapping().getCode()
                            )
                            .containsExactly(
                                    "Updated Description",
                                    ProgramDateKind.CUST_DR_PROGRAM_DE_ENROLLMENT_DATE,
                                    "UPDATED-CODE"
                            ));
        }

        @Test
        @DisplayName("Should delete program date ID mappings")
        void shouldDeleteProgramDateIdMappings() {
            // Arrange
            ProgramDateIdMappingsEntity entity = new ProgramDateIdMappingsEntity();
            entity.setProgramDateIdMapping(new ProgramDateIdMapping(
                    ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE,
                    "DELETE-ME",
                    "To Be Deleted",
                    null
            ));
            ProgramDateIdMappingsEntity savedEntity = repository.save(entity);
            flushAndClear();

            // Act
            repository.deleteById(savedEntity.getId());
            flushAndClear();

            Optional<ProgramDateIdMappingsEntity> retrieved = repository.findById(savedEntity.getId());

            // Assert
            assertThat(retrieved).isEmpty();
        }
    }

    @Nested
    @DisplayName("Bulk Operations")
    class BulkOperationsTest {

        @Test
        @DisplayName("Should handle bulk save operations")
        void shouldHandleBulkSaveOperations() {
            // Arrange
            List<ProgramDateIdMappingsEntity> entities = List.of(
                    createEntity(ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE, "BULK-1"),
                    createEntity(ProgramDateKind.CUST_DR_PROGRAM_DE_ENROLLMENT_DATE, "BULK-2"),
                    createEntity(ProgramDateKind.CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL, "BULK-3"),
                    createEntity(ProgramDateKind.CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL, "BULK-4")
            );

            // Act
            List<ProgramDateIdMappingsEntity> savedEntities = repository.saveAll(entities);
            flushAndClear();

            // Assert
            assertThat(savedEntities).hasSize(4).allMatch(e -> e.getId() != null);
            assertThat(repository.count()).isGreaterThanOrEqualTo(4);
        }

        @Test
        @DisplayName("Should handle bulk delete operations")
        void shouldHandleBulkDeleteOperations() {
            // Arrange
            List<ProgramDateIdMappingsEntity> entities = List.of(
                    createEntity(ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE, "DELETE-1"),
                    createEntity(ProgramDateKind.CUST_DR_PROGRAM_DE_ENROLLMENT_DATE, "DELETE-2")
            );

            List<ProgramDateIdMappingsEntity> savedEntities = repository.saveAll(entities);
            long initialCount = repository.count();
            flushAndClear();

            // Act
            repository.deleteAll(savedEntities);
            flushAndClear();

            // Assert
            assertThat(repository.count()).isEqualTo(initialCount - 2);
        }
    }

    @Nested
    @DisplayName("Embedded Object Persistence")
    class EmbeddedObjectPersistenceTest {

        @Test
        @DisplayName("Should persist all ProgramDateKind enum values")
        void shouldPersistAllProgramDateKindEnumValues() {
            for (ProgramDateKind kind : ProgramDateKind.values()) {
                // Arrange
                ProgramDateIdMapping mapping = new ProgramDateIdMapping(
                        kind,
                        "MYSQL-" + kind.ordinal(),
                        "MySQL Test " + kind.name(),
                        "Note for " + kind.name()
                );
                ProgramDateIdMappingsEntity entity = new ProgramDateIdMappingsEntity();
                entity.setProgramDateIdMapping(mapping);

                // Act
                ProgramDateIdMappingsEntity savedEntity = repository.save(entity);
                flushAndClear();
                Optional<ProgramDateIdMappingsEntity> retrieved = repository.findById(savedEntity.getId());

                // Assert
                assertThat(retrieved)
                        .as("Should persist enum value: %s", kind)
                        .isPresent()
                        .hasValueSatisfying(e ->
                                assertThat(e.getProgramDateIdMapping().getProgramDateType())
                                        .isEqualTo(kind));
            }
        }

        @Test
        @DisplayName("Should handle null embedded object")
        void shouldHandleNullEmbeddedObject() {
            // Arrange
            ProgramDateIdMappingsEntity entity = new ProgramDateIdMappingsEntity();
            entity.setDescription("Null Embedded Object Test");
            entity.setProgramDateIdMapping(null);

            // Act
            ProgramDateIdMappingsEntity savedEntity = repository.save(entity);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = repository.findById(savedEntity.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(e -> assertThat(e.getProgramDateIdMapping()).isNull());
        }

        @Test
        @DisplayName("Should handle embedded object with partial null fields")
        void shouldHandleEmbeddedObjectWithPartialNullFields() {
            // Arrange
            ProgramDateIdMapping mapping = new ProgramDateIdMapping();
            mapping.setProgramDateType(ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE);
            mapping.setCode("PARTIAL-CODE");
            // Leave name and note as null

            ProgramDateIdMappingsEntity entity = new ProgramDateIdMappingsEntity();
            entity.setProgramDateIdMapping(mapping);

            // Act
            ProgramDateIdMappingsEntity savedEntity = repository.save(entity);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = repository.findById(savedEntity.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(e -> assertThat(e.getProgramDateIdMapping())
                            .isNotNull()
                            .satisfies(m -> {
                                assertThat(m.getCode()).isEqualTo("PARTIAL-CODE");
                                assertThat(m.getName()).isNull();
                                assertThat(m.getNote()).isNull();
                            }));
        }
    }

    private ProgramDateIdMappingsEntity createEntity(ProgramDateKind kind, String code) {
        ProgramDateIdMapping mapping = new ProgramDateIdMapping(
                kind,
                code,
                "Test " + kind.name(),
                "Note for " + code
        );
        ProgramDateIdMappingsEntity entity = new ProgramDateIdMappingsEntity();
        entity.setProgramDateIdMapping(mapping);
        return entity;
    }
}
