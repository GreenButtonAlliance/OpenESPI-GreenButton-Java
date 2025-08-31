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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.StatementEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.StatementRefEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintViolation;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for StatementRepository.
 * 
 * Tests all CRUD operations, custom query methods, relationships,
 * and validation constraints for Statement entities.
 */
@DisplayName("Statement Repository Tests")
class StatementRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve statement successfully")
        void shouldSaveAndRetrieveStatementSuccessfully() {
            // Arrange
            StatementEntity statement = TestDataBuilders.createValidStatement();
            statement.setDescription("Test Statement for CRUD");

            // Act
            StatementEntity saved = statementRepository.save(statement);
            flushAndClear();
            Optional<StatementEntity> retrieved = statementRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Statement for CRUD");
            assertThat(retrieved.get().getIssueDateTime()).isEqualTo(statement.getIssueDateTime());
        }

        @Test
        @DisplayName("Should save statement with customer relationship")
        void shouldSaveStatementWithCustomerRelationship() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Test Customer for Statement");
            CustomerEntity savedCustomer = customerRepository.save(customer);
            
            StatementEntity statement = TestDataBuilders.createValidStatementWithCustomer(savedCustomer);
            statement.setDescription("Statement with Customer");

            // Act
            StatementEntity saved = statementRepository.save(statement);
            flushAndClear();
            Optional<StatementEntity> retrieved = statementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomer()).isNotNull();
            assertThat(retrieved.get().getCustomer().getId()).isEqualTo(savedCustomer.getId());
            assertThat(retrieved.get().getCustomer().getCustomerName()).isEqualTo("Test Customer for Statement");
        }

        @Test
        @DisplayName("Should find all statements")
        void shouldFindAllStatements() {
            // Arrange
            List<StatementEntity> statements = TestDataBuilders.createValidEntities(3, TestDataBuilders::createValidStatement);
            statements.forEach(s -> s.setDescription("Bulk Statement " + statements.indexOf(s)));
            statementRepository.saveAll(statements);
            flushAndClear();

            // Act
            List<StatementEntity> allStatements = statementRepository.findAll();

            // Assert
            assertThat(allStatements).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allStatements).extracting(StatementEntity::getDescription)
                    .contains("Bulk Statement 0", "Bulk Statement 1", "Bulk Statement 2");
        }

        @Test
        @DisplayName("Should delete statement successfully")
        void shouldDeleteStatementSuccessfully() {
            // Arrange
            StatementEntity statement = TestDataBuilders.createValidStatement();
            statement.setDescription("Statement to Delete");
            StatementEntity saved = statementRepository.save(statement);
            UUID statementId = saved.getId();
            flushAndClear();

            // Act
            statementRepository.deleteById(statementId);
            flushAndClear();
            Optional<StatementEntity> retrieved = statementRepository.findById(statementId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if statement exists")
        void shouldCheckIfStatementExists() {
            // Arrange
            StatementEntity statement = TestDataBuilders.createValidStatement();
            StatementEntity saved = statementRepository.save(statement);
            flushAndClear();

            // Act & Assert
            assertThat(statementRepository.existsById(saved.getId())).isTrue();
            assertThat(statementRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count statements")
        void shouldCountStatements() {
            // Arrange
            long initialCount = statementRepository.count();
            List<StatementEntity> statements = TestDataBuilders.createValidEntities(5, TestDataBuilders::createValidStatement);
            statementRepository.saveAll(statements);
            flushAndClear();

            // Act
            long finalCount = statementRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 5);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find statements issued after specified date")
        void shouldFindStatementsIssuedAfterSpecifiedDate() {
            // Arrange
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(15);
            OffsetDateTime recentDate = OffsetDateTime.now().minusDays(5);
            OffsetDateTime oldDate = OffsetDateTime.now().minusDays(25);

            StatementEntity recentStatement = TestDataBuilders.createValidStatement();
            recentStatement.setIssueDateTime(recentDate);
            recentStatement.setDescription("Recent Statement");

            StatementEntity oldStatement = TestDataBuilders.createValidStatement();
            oldStatement.setIssueDateTime(oldDate);
            oldStatement.setDescription("Old Statement");

            statementRepository.saveAll(List.of(recentStatement, oldStatement));
            flushAndClear();

            // Act
            List<StatementEntity> results = statementRepository.findByIssueDateTimeAfter(cutoffDate);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results).extracting(StatementEntity::getDescription).contains("Recent Statement");
            assertThat(results).extracting(StatementEntity::getDescription).doesNotContain("Old Statement");
            assertThat(results).allMatch(s -> s.getIssueDateTime().isAfter(cutoffDate));
        }

        @Test
        @DisplayName("Should find statements issued before specified date")
        void shouldFindStatementsIssuedBeforeSpecifiedDate() {
            // Arrange
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(15);
            OffsetDateTime recentDate = OffsetDateTime.now().minusDays(5);
            OffsetDateTime oldDate = OffsetDateTime.now().minusDays(25);

            StatementEntity recentStatement = TestDataBuilders.createValidStatement();
            recentStatement.setIssueDateTime(recentDate);
            recentStatement.setDescription("Recent Statement");

            StatementEntity oldStatement = TestDataBuilders.createValidStatement();
            oldStatement.setIssueDateTime(oldDate);
            oldStatement.setDescription("Old Statement");

            statementRepository.saveAll(List.of(recentStatement, oldStatement));
            flushAndClear();

            // Act
            List<StatementEntity> results = statementRepository.findByIssueDateTimeBefore(cutoffDate);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results).extracting(StatementEntity::getDescription).contains("Old Statement");
            assertThat(results).extracting(StatementEntity::getDescription).doesNotContain("Recent Statement");
            assertThat(results).allMatch(s -> s.getIssueDateTime().isBefore(cutoffDate));
        }

        @Test
        @DisplayName("Should find statements issued between specified dates")
        void shouldFindStatementsIssuedBetweenSpecifiedDates() {
            // Arrange
            OffsetDateTime startDate = OffsetDateTime.now().minusDays(20);
            OffsetDateTime endDate = OffsetDateTime.now().minusDays(10);
            
            OffsetDateTime withinRangeDate = OffsetDateTime.now().minusDays(15);
            OffsetDateTime beforeRangeDate = OffsetDateTime.now().minusDays(25);
            OffsetDateTime afterRangeDate = OffsetDateTime.now().minusDays(5);

            StatementEntity withinRangeStatement = TestDataBuilders.createValidStatement();
            withinRangeStatement.setIssueDateTime(withinRangeDate);
            withinRangeStatement.setDescription("Within Range Statement");

            StatementEntity beforeRangeStatement = TestDataBuilders.createValidStatement();
            beforeRangeStatement.setIssueDateTime(beforeRangeDate);
            beforeRangeStatement.setDescription("Before Range Statement");

            StatementEntity afterRangeStatement = TestDataBuilders.createValidStatement();
            afterRangeStatement.setIssueDateTime(afterRangeDate);
            afterRangeStatement.setDescription("After Range Statement");

            statementRepository.saveAll(List.of(withinRangeStatement, beforeRangeStatement, afterRangeStatement));
            flushAndClear();

            // Act
            List<StatementEntity> results = statementRepository.findByIssueDateTimeBetween(startDate, endDate);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results).extracting(StatementEntity::getDescription).contains("Within Range Statement");
            assertThat(results).extracting(StatementEntity::getDescription)
                    .doesNotContain("Before Range Statement", "After Range Statement");
            assertThat(results).allMatch(s -> 
                    s.getIssueDateTime().isAfter(startDate) && s.getIssueDateTime().isBefore(endDate));
        }

        @Test
        @DisplayName("Should find statements with references")
        void shouldFindStatementsWithReferences() {
            // Arrange
            StatementEntity statementWithRefs = TestDataBuilders.createValidStatement();
            statementWithRefs.setDescription("Statement With References");
            statementWithRefs.setStatementRefs(new ArrayList<>());
            
            // Create mock statement references
            StatementRefEntity ref1 = new StatementRefEntity();
            ref1.setStatement(statementWithRefs);
            StatementRefEntity ref2 = new StatementRefEntity();
            ref2.setStatement(statementWithRefs);
            
            statementWithRefs.getStatementRefs().add(ref1);
            statementWithRefs.getStatementRefs().add(ref2);

            StatementEntity statementWithoutRefs = TestDataBuilders.createValidStatement();
            statementWithoutRefs.setDescription("Statement Without References");
            statementWithoutRefs.setStatementRefs(new ArrayList<>());

            statementRepository.saveAll(List.of(statementWithRefs, statementWithoutRefs));
            flushAndClear();

            // Act
            List<StatementEntity> results = statementRepository.findStatementsWithReferences();

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results).extracting(StatementEntity::getDescription).contains("Statement With References");
            assertThat(results).extracting(StatementEntity::getDescription).doesNotContain("Statement Without References");
            assertThat(results).allMatch(s -> s.getStatementRefs() != null && !s.getStatementRefs().isEmpty());
        }

        @Test
        @DisplayName("Should find statements without references")
        void shouldFindStatementsWithoutReferences() {
            // Arrange
            StatementEntity statementWithoutRefs = TestDataBuilders.createValidStatement();
            statementWithoutRefs.setDescription("Statement Without References");
            statementWithoutRefs.setStatementRefs(new ArrayList<>());

            StatementEntity statementWithRefs = TestDataBuilders.createValidStatement();
            statementWithRefs.setDescription("Statement With References");
            statementWithRefs.setStatementRefs(new ArrayList<>());
            
            StatementRefEntity ref = new StatementRefEntity();
            ref.setStatement(statementWithRefs);
            statementWithRefs.getStatementRefs().add(ref);

            statementRepository.saveAll(List.of(statementWithoutRefs, statementWithRefs));
            flushAndClear();

            // Act
            List<StatementEntity> results = statementRepository.findStatementsWithoutReferences();

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results).extracting(StatementEntity::getDescription).contains("Statement Without References");
            assertThat(results).extracting(StatementEntity::getDescription).doesNotContain("Statement With References");
            assertThat(results).allMatch(s -> s.getStatementRefs() == null || s.getStatementRefs().isEmpty());
        }

        @Test
        @DisplayName("Should find statements by description containing text")
        void shouldFindStatementsByDescriptionContainingText() {
            // Arrange
            StatementEntity statement1 = TestDataBuilders.createValidStatement();
            statement1.setDescription("Monthly Electricity Bill Statement");

            StatementEntity statement2 = TestDataBuilders.createValidStatement();
            statement2.setDescription("Annual Gas Usage Report");

            StatementEntity statement3 = TestDataBuilders.createValidStatement();
            statement3.setDescription("Quarterly Electricity Summary");

            statementRepository.saveAll(List.of(statement1, statement2, statement3));
            flushAndClear();

            // Act
            List<StatementEntity> results = statementRepository.findByDescriptionContaining("electricity");

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(StatementEntity::getDescription)
                    .contains("Monthly Electricity Bill Statement", "Quarterly Electricity Summary");
            assertThat(results).extracting(StatementEntity::getDescription)
                    .doesNotContain("Annual Gas Usage Report");
        }

        @Test
        @DisplayName("Should find recent statements")
        void shouldFindRecentStatements() {
            // Arrange
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(10);
            
            StatementEntity recentStatement1 = TestDataBuilders.createValidStatement();
            recentStatement1.setIssueDateTime(OffsetDateTime.now().minusDays(5));
            recentStatement1.setDescription("Recent Statement 1");

            StatementEntity recentStatement2 = TestDataBuilders.createValidStatement();
            recentStatement2.setIssueDateTime(OffsetDateTime.now().minusDays(3));
            recentStatement2.setDescription("Recent Statement 2");

            StatementEntity oldStatement = TestDataBuilders.createValidStatement();
            oldStatement.setIssueDateTime(OffsetDateTime.now().minusDays(15));
            oldStatement.setDescription("Old Statement");

            statementRepository.saveAll(List.of(recentStatement1, recentStatement2, oldStatement));
            flushAndClear();

            // Act
            List<StatementEntity> results = statementRepository.findRecentStatements(cutoffDate);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(StatementEntity::getDescription)
                    .contains("Recent Statement 1", "Recent Statement 2");
            assertThat(results).extracting(StatementEntity::getDescription)
                    .doesNotContain("Old Statement");
            
            // Should be ordered by issue date descending (most recent first)
            assertThat(results.get(0).getDescription()).isEqualTo("Recent Statement 2");
            assertThat(results.get(1).getDescription()).isEqualTo("Recent Statement 1");
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Arrange
            OffsetDateTime futureDate = OffsetDateTime.now().plusDays(30);

            // Act
            List<StatementEntity> results = statementRepository.findByIssueDateTimeAfter(futureDate);

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("JPA Relationships")
    class RelationshipsTest {

        @Test
        @DisplayName("Should maintain customer relationship integrity")
        void shouldMaintainCustomerRelationshipIntegrity() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Relationship Test Customer");
            CustomerEntity savedCustomer = customerRepository.save(customer);

            StatementEntity statement = TestDataBuilders.createValidStatementWithCustomer(savedCustomer);
            statement.setDescription("Statement with Customer Relationship");

            // Act
            StatementEntity savedStatement = statementRepository.save(statement);
            flushAndClear();
            
            Optional<StatementEntity> retrieved = statementRepository.findById(savedStatement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomer()).isNotNull();
            assertThat(retrieved.get().getCustomer().getId()).isEqualTo(savedCustomer.getId());
            assertThat(retrieved.get().getCustomer().getCustomerName()).isEqualTo("Relationship Test Customer");
        }

        @Test
        @DisplayName("Should handle null customer relationship")
        void shouldHandleNullCustomerRelationship() {
            // Arrange
            StatementEntity statement = TestDataBuilders.createValidStatement();
            statement.setDescription("Statement without Customer");
            statement.setCustomer(null);

            // Act & Assert
            assertThatCode(() -> {
                StatementEntity saved = statementRepository.save(statement);
                flushAndClear();
                Optional<StatementEntity> retrieved = statementRepository.findById(saved.getId());
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get().getCustomer()).isNull();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate statement with valid data")
        void shouldValidateStatementWithValidData() {
            // Arrange
            StatementEntity statement = TestDataBuilders.createValidStatement();
            statement.setDescription("Valid Statement Description");

            // Act
            Set<ConstraintViolation<StatementEntity>> violations = validator.validate(statement);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle null issue date time")
        void shouldHandleNullIssueDateTime() {
            // Arrange
            StatementEntity statement = TestDataBuilders.createValidStatement();
            statement.setIssueDateTime(null);
            statement.setDescription("Statement with null issue date");

            // Act
            Set<ConstraintViolation<StatementEntity>> violations = validator.validate(statement);

            // Assert - Issue date time is typically optional
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle null description")
        void shouldHandleNullDescription() {
            // Arrange
            StatementEntity statement = TestDataBuilders.createValidStatement();
            statement.setDescription(null);

            // Act
            Set<ConstraintViolation<StatementEntity>> violations = validator.validate(statement);

            // Assert - Description is typically optional in ESPI entities
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange & Act
            StatementEntity statement = TestDataBuilders.createValidStatement();

            // Assert
            assertThat(statement.getId()).isNotNull();
            assertThat(statement.getId()).isInstanceOf(UUID.class);
            assertThat(statement.getRelatedLinks()).isNotNull();
            assertThat(statement.getRelatedLinks()).isEmpty();
        }

        @Test
        @DisplayName("Should set timestamps on persist")
        void shouldSetTimestampsOnPersist() {
            // Arrange
            StatementEntity statement = TestDataBuilders.createValidStatement();
            statement.setDescription("Statement for Timestamp Test");

            // Act
            StatementEntity saved = statementRepository.save(statement);
            flushAndClear();

            // Assert
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }
    }
}