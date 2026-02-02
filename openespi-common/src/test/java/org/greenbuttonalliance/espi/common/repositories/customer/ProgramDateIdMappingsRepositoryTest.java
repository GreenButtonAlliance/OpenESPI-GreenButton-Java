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

package org.greenbuttonalliance.espi.common.repositories.customer;

import org.greenbuttonalliance.espi.common.domain.customer.common.ProgramDateIdMapping;
import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;
import org.greenbuttonalliance.espi.common.domain.customer.enums.ProgramDateKind;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for ProgramDateIdMappingsRepository.
 *
 * Tests all CRUD operations and validation constraints for ProgramDateIdMappings entities.
 * Per ESPI 4.0 API specification, only ID-based queries are supported.
 */
@DisplayName("ProgramDateIdMappings Repository Tests")
class ProgramDateIdMappingsRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ProgramDateIdMappingsRepository programDateIdMappingsRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve program date ID mappings successfully")
        void shouldSaveAndRetrieveProgramDateIdMappingsSuccessfully() {
            // Arrange
            ProgramDateIdMappingsEntity entity = createValidProgramDateIdMappings();
            entity.setDescription("Test DR Program Enrollment");

            // Act
            ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = programDateIdMappingsRepository.findById(saved.getId());

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(mapping -> assertThat(mapping)
                            .extracting(
                                    ProgramDateIdMappingsEntity::getDescription,
                                    m -> m.getProgramDateIdMapping().getProgramDateType(),
                                    m -> m.getProgramDateIdMapping().getCode()
                            )
                            .containsExactly("Test DR Program Enrollment",
                                    ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE,
                                    "DR-001"));
        }

        @Test
        @DisplayName("Should find all program date ID mappings")
        void shouldFindAllProgramDateIdMappings() {
            // Arrange
            ProgramDateIdMappingsEntity mapping1 = createValidProgramDateIdMappings();
            mapping1.setDescription("Mapping 1");
            ProgramDateIdMappingsEntity mapping2 = createValidProgramDateIdMappings();
            mapping2.setDescription("Mapping 2");
            ProgramDateIdMappingsEntity mapping3 = createValidProgramDateIdMappings();
            mapping3.setDescription("Mapping 3");

            programDateIdMappingsRepository.saveAll(List.of(mapping1, mapping2, mapping3));
            flushAndClear();

            // Act
            List<ProgramDateIdMappingsEntity> allMappings = programDateIdMappingsRepository.findAll();

            // Assert
            assertThat(allMappings)
                    .hasSizeGreaterThanOrEqualTo(3)
                    .extracting(ProgramDateIdMappingsEntity::getDescription)
                    .contains("Mapping 1", "Mapping 2", "Mapping 3");
        }

        @Test
        @DisplayName("Should delete program date ID mappings successfully")
        void shouldDeleteProgramDateIdMappingsSuccessfully() {
            // Arrange
            ProgramDateIdMappingsEntity entity = createValidProgramDateIdMappings();
            entity.setDescription("To Be Deleted");
            ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
            UUID id = saved.getId();
            flushAndClear();

            // Act
            programDateIdMappingsRepository.deleteById(id);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = programDateIdMappingsRepository.findById(id);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if program date ID mappings exists")
        void shouldCheckIfProgramDateIdMappingsExists() {
            // Arrange
            ProgramDateIdMappingsEntity entity = createValidProgramDateIdMappings();
            entity.setDescription("Exists Check");
            ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
            flushAndClear();

            // Act & Assert
            assertThat(programDateIdMappingsRepository.existsById(saved.getId())).isTrue();
            assertThat(programDateIdMappingsRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count program date ID mappings")
        void shouldCountProgramDateIdMappings() {
            // Arrange
            long initialCount = programDateIdMappingsRepository.count();
            programDateIdMappingsRepository.saveAll(List.of(
                    createValidProgramDateIdMappings(),
                    createValidProgramDateIdMappings(),
                    createValidProgramDateIdMappings()
            ));
            flushAndClear();

            // Act & Assert
            assertThat(programDateIdMappingsRepository.count()).isEqualTo(initialCount + 3);
        }
    }

    @Nested
    @DisplayName("Embedded Object Persistence")
    class EmbeddedObjectPersistenceTest {

        @Test
        @DisplayName("Should persist all ProgramDateIdMapping fields correctly")
        void shouldPersistAllProgramDateIdMappingFields() {
            // Arrange
            ProgramDateIdMapping mapping = new ProgramDateIdMapping(
                    ProgramDateKind.CUST_DR_PROGRAM_DE_ENROLLMENT_DATE,
                    "CODE-123",
                    "Program De-enrollment",
                    "Optional note about de-enrollment"
            );

            ProgramDateIdMappingsEntity entity = createValidProgramDateIdMappings();
            entity.setProgramDateIdMapping(mapping);
            entity.setDescription("Full Field Test");

            // Act
            ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = programDateIdMappingsRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(e -> assertThat(e.getProgramDateIdMapping())
                            .isNotNull()
                            .extracting(
                                    ProgramDateIdMapping::getProgramDateType,
                                    ProgramDateIdMapping::getCode,
                                    ProgramDateIdMapping::getName,
                                    ProgramDateIdMapping::getNote
                            )
                            .containsExactly(
                                    ProgramDateKind.CUST_DR_PROGRAM_DE_ENROLLMENT_DATE,
                                    "CODE-123",
                                    "Program De-enrollment",
                                    "Optional note about de-enrollment"
                            ));
        }

        @Test
        @DisplayName("Should persist all ProgramDateKind enum values")
        void shouldPersistAllProgramDateKindEnumValues() {
            // Test all 4 enum values to ensure they persist correctly
            ProgramDateKind[] allKinds = ProgramDateKind.values();

            for (ProgramDateKind kind : allKinds) {
                // Arrange
                ProgramDateIdMapping mapping = new ProgramDateIdMapping(
                        kind,
                        "CODE-" + kind.ordinal(),
                        "Test " + kind.name(),
                        "Note for " + kind.name()
                );
                ProgramDateIdMappingsEntity entity = createValidProgramDateIdMappings();
                entity.setProgramDateIdMapping(mapping);

                // Act
                ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
                flushAndClear();
                Optional<ProgramDateIdMappingsEntity> retrieved = programDateIdMappingsRepository.findById(saved.getId());

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
            entity.setDescription("No embedded mapping");
            entity.setProgramDateIdMapping(null);

            // Act
            ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = programDateIdMappingsRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(e -> assertThat(e.getProgramDateIdMapping()).isNull());
        }

        @Test
        @DisplayName("Should handle embedded object with null fields")
        void shouldHandleEmbeddedObjectWithNullFields() {
            // Arrange
            ProgramDateIdMapping mapping = new ProgramDateIdMapping();
            mapping.setProgramDateType(ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE);
            // Leave code, name, note as null

            ProgramDateIdMappingsEntity entity = createValidProgramDateIdMappings();
            entity.setProgramDateIdMapping(mapping);

            // Act
            ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = programDateIdMappingsRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(e -> assertThat(e.getProgramDateIdMapping())
                            .isNotNull()
                            .satisfies(m -> {
                                assertThat(m.getProgramDateType())
                                        .isEqualTo(ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE);
                                assertThat(m.getCode()).isNull();
                                assertThat(m.getName()).isNull();
                                assertThat(m.getNote()).isNull();
                            }));
        }
    }

    @Nested
    @DisplayName("IdentifiedObject Fields")
    class IdentifiedObjectFieldsTest {

        @Test
        @DisplayName("Should persist IdentifiedObject metadata fields")
        void shouldPersistIdentifiedObjectMetadataFields() {
            // Arrange
            ProgramDateIdMappingsEntity entity = createValidProgramDateIdMappings();
            entity.setDescription("Metadata Test");

            // Act
            ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
            flushAndClear();
            Optional<ProgramDateIdMappingsEntity> retrieved = programDateIdMappingsRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved)
                    .isPresent()
                    .hasValueSatisfying(e -> assertThat(e)
                            .extracting(
                                    ProgramDateIdMappingsEntity::getId,
                                    ProgramDateIdMappingsEntity::getDescription,
                                    ProgramDateIdMappingsEntity::getCreated,
                                    ProgramDateIdMappingsEntity::getUpdated
                            )
                            .doesNotContainNull());
        }

        @Test
        @DisplayName("Should update timestamps on modification")
        void shouldUpdateTimestampsOnModification() throws InterruptedException {
            // Arrange
            ProgramDateIdMappingsEntity entity = createValidProgramDateIdMappings();
            ProgramDateIdMappingsEntity saved = programDateIdMappingsRepository.save(entity);
            flushAndClear();

            // Act
            Thread.sleep(10); // Ensure timestamp difference
            saved.setDescription("Updated Description");
            ProgramDateIdMappingsEntity updated = programDateIdMappingsRepository.save(saved);
            flushAndClear();

            // Assert
            assertThat(updated.getUpdated()).isAfter(updated.getCreated());
        }
    }

    /**
     * Helper method to create a valid ProgramDateIdMappingsEntity for testing.
     */
    private ProgramDateIdMappingsEntity createValidProgramDateIdMappings() {
        ProgramDateIdMapping mapping = new ProgramDateIdMapping(
                ProgramDateKind.CUST_DR_PROGRAM_ENROLLMENT_DATE,
                "DR-001",
                "Demand Response Enrollment",
                "Test program enrollment"
        );

        ProgramDateIdMappingsEntity entity = new ProgramDateIdMappingsEntity();
        entity.setProgramDateIdMapping(mapping);
        return entity;
    }
}
