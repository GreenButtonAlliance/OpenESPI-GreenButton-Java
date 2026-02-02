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

import org.greenbuttonalliance.espi.common.domain.customer.entity.*;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for StatementRepository - Phase 19 ESPI 4.0 Compliance.
 *
 * Tests CRUD operations and ID-based relationship queries following
 * ESPI standard patterns. Non-ID queries removed per Issue #28.
 */
@DisplayName("Statement Repository Tests - Phase 19")
class StatementRepositoryTest extends BaseRepositoryTest {

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
    @DisplayName("Should save and retrieve statement successfully")
    void shouldSaveAndRetrieveStatement() {
        // Arrange
        StatementEntity statement = new StatementEntity();
        statement.setDescription("Test Statement");
        statement.setIssueDateTime(OffsetDateTime.now());

        // Act
        StatementEntity saved = statementRepository.save(statement);
        flushAndClear();
        Optional<StatementEntity> retrieved = statementRepository.findById(saved.getId());

        // Assert
        assertThat(retrieved)
                .isPresent()
                .hasValueSatisfying(stmt -> assertThat(stmt)
                        .extracting("description", "issueDateTime")
                        .containsExactly("Test Statement", saved.getIssueDateTime()));
    }

    @Test
    @DisplayName("Should save statement with StatementRef collection")
    void shouldSaveStatementWithRefs() {
        // Arrange
        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement with Refs");
        statement.setIssueDateTime(OffsetDateTime.now());

        // StatementRef is @Embeddable, stored as @ElementCollection
        List<StatementRefEntity> refs = new ArrayList<>();
        StatementRefEntity ref1 = new StatementRefEntity();
        ref1.setFileName("bill.pdf");
        ref1.setMediaType("application/pdf");
        ref1.setStatementURL("https://example.com/bills/bill.pdf");
        refs.add(ref1);

        StatementRefEntity ref2 = new StatementRefEntity();
        ref2.setFileName("invoice.html");
        ref2.setMediaType("text/html");
        ref2.setStatementURL("https://example.com/bills/invoice.html");
        refs.add(ref2);

        statement.setStatementRefs(refs);

        // Act
        StatementEntity saved = statementRepository.save(statement);
        flushAndClear();
        Optional<StatementEntity> retrieved = statementRepository.findById(saved.getId());

        // Assert
        assertThat(retrieved)
                .isPresent()
                .hasValueSatisfying(stmt -> {
                    assertThat(stmt.getStatementRefs()).hasSize(2);
                    assertThat(stmt.getStatementRefs().get(0))
                            .extracting("fileName", "mediaType")
                            .containsExactly("bill.pdf", "application/pdf");
                });
    }

    @Test
    @DisplayName("Should find statements by customer ID")
    void shouldFindByCustomerId() {
        // Arrange
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerName("Test Customer");
        CustomerEntity savedCustomer = customerRepository.save(customer);

        StatementEntity statement1 = createStatementWithCustomer(savedCustomer);
        StatementEntity statement2 = createStatementWithCustomer(savedCustomer);
        statementRepository.save(statement1);
        statementRepository.save(statement2);
        flushAndClear();

        // Act
        List<StatementEntity> found = statementRepository.findByCustomerId(savedCustomer.getId());

        // Assert
        assertThat(found).hasSize(2)
                .allSatisfy(stmt -> assertThat(stmt.getCustomer().getId())
                        .isEqualTo(savedCustomer.getId()));
    }

    @Test
    @DisplayName("Should find statements by customer account ID")
    void shouldFindByCustomerAccountId() {
        // Arrange
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setDescription("Test Account");
        CustomerAccountEntity savedAccount = customerAccountRepository.save(account);

        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement for Account");
        statement.setCustomerAccount(savedAccount);
        statementRepository.save(statement);
        flushAndClear();

        // Act
        List<StatementEntity> found = statementRepository.findByCustomerAccountId(savedAccount.getId());

        // Assert
        assertThat(found).hasSize(1)
                .first()
                .extracting(s -> s.getCustomerAccount().getId())
                .isEqualTo(savedAccount.getId());
    }

    @Test
    @DisplayName("Should find statements by customer agreement ID")
    void shouldFindByCustomerAgreementId() {
        // Arrange
        CustomerAgreementEntity agreement = new CustomerAgreementEntity();
        agreement.setDescription("Test Agreement");
        CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);

        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement for Agreement");
        statement.setCustomerAgreement(savedAgreement);
        statementRepository.save(statement);
        flushAndClear();

        // Act
        List<StatementEntity> found = statementRepository.findByCustomerAgreementId(savedAgreement.getId());

        // Assert
        assertThat(found).hasSize(1)
                .first()
                .extracting(s -> s.getCustomerAgreement().getId())
                .isEqualTo(savedAgreement.getId());
    }

    @Test
    @DisplayName("Should find statements by usage summary ID")
    void shouldFindByUsageSummaryId() {
        // Arrange
        UsageSummaryEntity summary = new UsageSummaryEntity();
        summary.setDescription("Test Summary");
        UsageSummaryEntity savedSummary = usageSummaryRepository.save(summary);

        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement for Summary");
        statement.setUsageSummary(savedSummary);
        statementRepository.save(statement);
        flushAndClear();

        // Act
        List<StatementEntity> found = statementRepository.findByUsageSummaryId(savedSummary.getId());

        // Assert
        assertThat(found).hasSize(1)
                .first()
                .extracting(s -> s.getUsageSummary().getId())
                .isEqualTo(savedSummary.getId());
    }

    @Test
    @DisplayName("Should delete statement successfully")
    void shouldDeleteStatement() {
        // Arrange
        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement to Delete");
        StatementEntity saved = statementRepository.save(statement);
        flushAndClear();

        // Act
        statementRepository.deleteById(saved.getId());
        flushAndClear();
        Optional<StatementEntity> retrieved = statementRepository.findById(saved.getId());

        // Assert
        assertThat(retrieved).isEmpty();
    }

    private StatementEntity createStatementWithCustomer(CustomerEntity customer) {
        StatementEntity statement = new StatementEntity();
        statement.setDescription("Statement for Customer");
        statement.setIssueDateTime(OffsetDateTime.now());
        statement.setCustomer(customer);
        return statement;
    }
}
