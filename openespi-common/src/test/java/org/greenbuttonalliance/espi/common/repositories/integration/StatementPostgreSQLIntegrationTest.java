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

import org.greenbuttonalliance.espi.common.domain.customer.entity.*;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.*;
import org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository;
import org.greenbuttonalliance.espi.common.test.BaseTestContainersTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Statement entity integration tests using PostgreSQL TestContainer.
 *
 * Tests Phase 19 ESPI 4.0 compliance:
 * - StatementRef as @ElementCollection (extends Object, not IdentifiedObject)
 * - ID-based relationship queries (Customer, CustomerAccount, CustomerAgreement, UsageSummary)
 * - Full CRUD operations with real PostgreSQL database
 */
@DisplayName("Statement Integration Tests - PostgreSQL")
@ActiveProfiles({"test", "test-postgresql"})
class StatementPostgreSQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final org.testcontainers.containers.PostgreSQLContainer<?> postgres = postgresqlContainer;

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configurePostgreSQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private CustomerAgreementRepository customerAgreementRepository;

    @Autowired
    private UsageSummaryRepository usageSummaryRepository;

    @Test
    @DisplayName("Should persist statement with StatementRef @ElementCollection")
    void shouldPersistStatementWithElementCollection() {
        // Arrange
        StatementEntity statement = new StatementEntity();
        statement.setDescription("PostgreSQL Statement Test");
        statement.setIssueDateTime(OffsetDateTime.now());

        List<StatementRefEntity> refs = new ArrayList<>();
        StatementRefEntity ref1 = new StatementRefEntity();
        ref1.setFileName("bill.pdf");
        ref1.setMediaType("application/pdf");
        ref1.setStatementURL("https://example.com/bills/001.pdf");
        refs.add(ref1);

        statement.setStatementRefs(refs);

        // Act
        StatementEntity saved = statementRepository.save(statement);
        entityManager.flush();
        entityManager.clear();
        Optional<StatementEntity> retrieved = statementRepository.findById(saved.getId());

        // Assert - StatementRef stored in collection table without id column
        assertThat(retrieved)
                .isPresent()
                .hasValueSatisfying(stmt -> {
                    assertThat(stmt.getStatementRefs()).hasSize(1);
                    assertThat(stmt.getStatementRefs().get(0))
                            .extracting("fileName", "mediaType", "statementURL")
                            .containsExactly("bill.pdf", "application/pdf", "https://example.com/bills/001.pdf");
                });
    }

    @Test
    @DisplayName("Should support relationship queries with Customer")
    void shouldSupportCustomerRelationship() {
        // Arrange
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerName("Test Customer");
        CustomerEntity savedCustomer = customerRepository.save(customer);

        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement for Customer");
        statement.setIssueDateTime(OffsetDateTime.now());
        statement.setCustomer(savedCustomer);
        statementRepository.save(statement);

        entityManager.flush();
        entityManager.clear();

        // Act
        List<StatementEntity> found = statementRepository.findByCustomerId(savedCustomer.getId());

        // Assert
        assertThat(found).hasSize(1)
                .first()
                .extracting(s -> s.getCustomer().getId())
                .isEqualTo(savedCustomer.getId());
    }

    @Test
    @DisplayName("Should support all relationship queries")
    void shouldSupportAllRelationshipQueries() {
        // Arrange
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setDescription("Test Account");
        CustomerAccountEntity savedAccount = customerAccountRepository.save(account);

        CustomerAgreementEntity agreement = new CustomerAgreementEntity();
        agreement.setDescription("Test Agreement");
        CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);

        UsageSummaryEntity summary = new UsageSummaryEntity();
        summary.setDescription("Test Summary");
        UsageSummaryEntity savedSummary = usageSummaryRepository.save(summary);

        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement with All Relationships");
        statement.setCustomerAccount(savedAccount);
        statement.setCustomerAgreement(savedAgreement);
        statement.setUsageSummary(savedSummary);
        statementRepository.save(statement);

        entityManager.flush();
        entityManager.clear();

        // Act & Assert - All relationship queries work
        assertThat(statementRepository.findByCustomerAccountId(savedAccount.getId())).hasSize(1);
        assertThat(statementRepository.findByCustomerAgreementId(savedAgreement.getId())).hasSize(1);
        assertThat(statementRepository.findByUsageSummaryId(savedSummary.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Should handle cascading delete of @ElementCollection")
    void shouldCascadeDeleteElementCollection() {
        // Arrange
        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement to Delete");
        statement.setIssueDateTime(OffsetDateTime.now());

        List<StatementRefEntity> refs = new ArrayList<>();
        StatementRefEntity ref = new StatementRefEntity();
        ref.setFileName("test.pdf");
        ref.setMediaType("application/pdf");
        ref.setStatementURL("https://example.com/test.pdf");
        refs.add(ref);
        statement.setStatementRefs(refs);

        StatementEntity saved = statementRepository.save(statement);
        entityManager.flush();
        entityManager.clear();

        // Act - Delete statement should cascade to collection
        statementRepository.deleteById(saved.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(statementRepository.findById(saved.getId())).isEmpty();
    }
}
