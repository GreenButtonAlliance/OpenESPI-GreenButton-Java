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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for CustomerAccountRepository.
 * 
 * Tests all CRUD operations, 7 custom query methods, account management field testing,
 * Customer relationship testing, and IdentifiedObject base functionality.
 */
@DisplayName("CustomerAccount Repository Tests")
class CustomerAccountRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Creates a valid CustomerEntity for testing.
     */
    private CustomerEntity createValidCustomer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setDescription("Test Customer - " + faker.lorem().sentence(3));
        customer.setCustomerName(faker.company().name());
        customer.setPucNumber(faker.number().digits(10));
        customer.setSpecialNeed("NONE");
        return customer;
    }

    /**
     * Creates a valid CustomerAccountEntity for testing.
     */
    private CustomerAccountEntity createValidCustomerAccount() {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setDescription("Test Customer Account - " + faker.lorem().sentence(3));
        account.setTitle("Customer Account " + faker.number().digits(6));
        account.setSubject("Billing Account");
        account.setType("BILLING");
        account.setAccountId("ACCT-" + faker.number().digits(8));
        account.setBillingCycle("MONTHLY");
        account.setBudgetBill("STANDARD");
        account.setLastBillAmount(faker.number().numberBetween(5000L, 50000L)); // $50-$500
        account.setContactInfo(faker.name().fullName());
        account.setIsPrePay(false);
        account.setCreatedDateTime(randomOffsetDateTime());
        account.setLastModifiedDateTime(randomOffsetDateTime());
        account.setRevisionNumber("1.0");
        return account;
    }

    /**
     * Creates a complete test setup with Customer and CustomerAccount.
     */
    private CustomerAccountEntity createCompleteTestSetup() {
        CustomerEntity customer = createValidCustomer();
        CustomerEntity savedCustomer = persistAndFlush(customer);

        CustomerAccountEntity account = createValidCustomerAccount();
        account.setCustomer(savedCustomer);
        
        return account;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer account successfully")
        void shouldSaveAndRetrieveCustomerAccountSuccessfully() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setDescription("Test Customer Account for CRUD");
            account.setTitle("CRUD Test Account");

            // Act
            CustomerAccountEntity saved = customerAccountRepository.save(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Customer Account for CRUD");
            assertThat(retrieved.get().getTitle()).isEqualTo("CRUD Test Account");
            assertThat(retrieved.get().getCustomer()).isNotNull();
        }

        @Test
        @DisplayName("Should update customer account successfully")
        void shouldUpdateCustomerAccountSuccessfully() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            CustomerAccountEntity saved = persistAndFlush(account);

            // Act
            saved.setDescription("Updated Customer Account Description");
            saved.setLastBillAmount(25000L); // $250.00
            saved.setBillingCycle("QUARTERLY");
            CustomerAccountEntity updated = customerAccountRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Updated Customer Account Description");
            assertThat(retrieved.get().getLastBillAmount()).isEqualTo(25000L);
            assertThat(retrieved.get().getBillingCycle()).isEqualTo("QUARTERLY");
        }

        @Test
        @DisplayName("Should delete customer account successfully")
        void shouldDeleteCustomerAccountSuccessfully() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            CustomerAccountEntity saved = persistAndFlush(account);
            UUID savedId = saved.getId();

            // Act
            customerAccountRepository.deleteById(savedId);
            flushAndClear();

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should find all customer accounts")
        void shouldFindAllCustomerAccounts() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            account1.setTitle("First Account");
            CustomerAccountEntity account2 = createCompleteTestSetup();
            account2.setTitle("Second Account");
            
            persistAndFlush(account1);
            persistAndFlush(account2);

            // Act
            List<CustomerAccountEntity> allAccounts = customerAccountRepository.findAll();

            // Assert
            assertThat(allAccounts).hasSizeGreaterThanOrEqualTo(2);
            assertThat(allAccounts)
                .extracting(CustomerAccountEntity::getTitle)
                .contains("First Account", "Second Account");
        }

        @Test
        @DisplayName("Should count customer accounts correctly")
        void shouldCountCustomerAccountsCorrectly() {
            // Arrange
            long initialCount = customerAccountRepository.count();
            CustomerAccountEntity account1 = createCompleteTestSetup();
            CustomerAccountEntity account2 = createCompleteTestSetup();
            
            persistAndFlush(account1);
            persistAndFlush(account2);

            // Act
            long finalCount = customerAccountRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find customer account by account ID")
        void shouldFindCustomerAccountByAccountId() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setAccountId("UNIQUE-ACCOUNT-12345");
            CustomerAccountEntity saved = persistAndFlush(account);

            // Act
            Optional<CustomerAccountEntity> found = customerAccountRepository.findByAccountId("UNIQUE-ACCOUNT-12345");

            // Assert
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getAccountId()).isEqualTo("UNIQUE-ACCOUNT-12345");
        }

        @Test
        @DisplayName("Should find customer accounts by billing cycle")
        void shouldFindCustomerAccountsByBillingCycle() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            account1.setBillingCycle("MONTHLY");
            CustomerAccountEntity account2 = createCompleteTestSetup();
            account2.setBillingCycle("MONTHLY");
            CustomerAccountEntity account3 = createCompleteTestSetup();
            account3.setBillingCycle("QUARTERLY");
            
            persistAndFlush(account1);
            persistAndFlush(account2);
            persistAndFlush(account3);

            // Act
            List<CustomerAccountEntity> monthlyAccounts = customerAccountRepository.findByBillingCycle("MONTHLY");

            // Assert
            assertThat(monthlyAccounts).hasSize(2);
            assertThat(monthlyAccounts).extracting(CustomerAccountEntity::getBillingCycle)
                .allMatch(cycle -> cycle.equals("MONTHLY"));
        }

        @Test
        @DisplayName("Should find pre-pay accounts")
        void shouldFindPrePayAccounts() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            account1.setIsPrePay(true);
            CustomerAccountEntity account2 = createCompleteTestSetup();
            account2.setIsPrePay(true);
            CustomerAccountEntity account3 = createCompleteTestSetup();
            account3.setIsPrePay(false);
            
            persistAndFlush(account1);
            persistAndFlush(account2);
            persistAndFlush(account3);

            // Act
            List<CustomerAccountEntity> prePayAccounts = customerAccountRepository.findPrePayAccounts();

            // Assert
            assertThat(prePayAccounts).hasSize(2);
            assertThat(prePayAccounts).extracting(CustomerAccountEntity::getIsPrePay)
                .allMatch(isPrePay -> isPrePay.equals(true));
        }

        @Test
        @DisplayName("Should find budget bill accounts")
        void shouldFindBudgetBillAccounts() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            account1.setBudgetBill("BUDGET_PLAN_A");
            CustomerAccountEntity account2 = createCompleteTestSetup();
            account2.setBudgetBill("BUDGET_PLAN_B");
            CustomerAccountEntity account3 = createCompleteTestSetup();
            account3.setBudgetBill(null);
            CustomerAccountEntity account4 = createCompleteTestSetup();
            account4.setBudgetBill("");
            
            persistAndFlush(account1);
            persistAndFlush(account2);
            persistAndFlush(account3);
            persistAndFlush(account4);

            // Act
            List<CustomerAccountEntity> budgetAccounts = customerAccountRepository.findBudgetBillAccounts();

            // Assert
            assertThat(budgetAccounts).hasSize(2);
            assertThat(budgetAccounts).extracting(CustomerAccountEntity::getBudgetBill)
                .allMatch(budget -> budget != null && !budget.isEmpty());
        }

        @Test
        @DisplayName("Should find customer accounts by contact info")
        void shouldFindCustomerAccountsByContactInfo() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            account1.setContactInfo("John Smith");
            CustomerAccountEntity account2 = createCompleteTestSetup();
            account2.setContactInfo("John Smith");
            CustomerAccountEntity account3 = createCompleteTestSetup();
            account3.setContactInfo("Jane Doe");
            
            persistAndFlush(account1);
            persistAndFlush(account2);
            persistAndFlush(account3);

            // Act
            List<CustomerAccountEntity> johnSmithAccounts = customerAccountRepository.findByContactInfo("John Smith");

            // Assert
            assertThat(johnSmithAccounts).hasSize(2);
            assertThat(johnSmithAccounts).extracting(CustomerAccountEntity::getContactInfo)
                .allMatch(contact -> contact.equals("John Smith"));
        }

        @Test
        @DisplayName("Should find customer accounts by last bill amount greater than")
        void shouldFindCustomerAccountsByLastBillAmountGreaterThan() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            account1.setLastBillAmount(10000L); // $100.00
            CustomerAccountEntity account2 = createCompleteTestSetup();
            account2.setLastBillAmount(20000L); // $200.00
            CustomerAccountEntity account3 = createCompleteTestSetup();
            account3.setLastBillAmount(5000L); // $50.00
            
            persistAndFlush(account1);
            persistAndFlush(account2);
            persistAndFlush(account3);

            // Act
            List<CustomerAccountEntity> highBillAccounts = customerAccountRepository.findByLastBillAmountGreaterThan(15000L);

            // Assert
            assertThat(highBillAccounts).hasSize(1);
            assertThat(highBillAccounts.get(0).getLastBillAmount()).isEqualTo(20000L);
        }

        @Test
        @DisplayName("Should find customer accounts by title containing text")
        void shouldFindCustomerAccountsByTitleContaining() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            account1.setTitle("Residential Account Primary");
            CustomerAccountEntity account2 = createCompleteTestSetup();
            account2.setTitle("Commercial Account Secondary");
            CustomerAccountEntity account3 = createCompleteTestSetup();
            account3.setTitle("Industrial Service");
            
            persistAndFlush(account1);
            persistAndFlush(account2);
            persistAndFlush(account3);

            // Act
            List<CustomerAccountEntity> accountsWithAccount = customerAccountRepository.findByTitleContaining("Account");

            // Assert
            assertThat(accountsWithAccount).hasSize(2);
            assertThat(accountsWithAccount).extracting(CustomerAccountEntity::getTitle)
                .allMatch(title -> title.toLowerCase().contains("account"));
        }

        @Test
        @DisplayName("Should return empty results when no matches found")
        void shouldReturnEmptyResultsWhenNoMatchesFound() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setAccountId("EXISTING-ACCOUNT");
            persistAndFlush(account);

            // Act
            Optional<CustomerAccountEntity> notFound = customerAccountRepository.findByAccountId("NON-EXISTENT");
            List<CustomerAccountEntity> emptyList = customerAccountRepository.findByBillingCycle("NON-EXISTENT-CYCLE");

            // Assert
            assertThat(notFound).isEmpty();
            assertThat(emptyList).isEmpty();
        }
    }

    @Nested
    @DisplayName("Account Management Field Testing")
    class AccountManagementFieldTest {

        @Test
        @DisplayName("Should persist all document fields correctly")
        void shouldPersistAllDocumentFieldsCorrectly() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            OffsetDateTime createdTime = OffsetDateTime.now().minusDays(1);
            OffsetDateTime modifiedTime = OffsetDateTime.now();
            
            account.setCreatedDateTime(createdTime);
            account.setLastModifiedDateTime(modifiedTime);
            account.setRevisionNumber("2.1");
            account.setSubject("Billing Account Subject");
            account.setTitle("Primary Billing Account");
            account.setType("RESIDENTIAL_BILLING");

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            CustomerAccountEntity entity = retrieved.get();
            assertThat(entity.getCreatedDateTime()).isEqualTo(createdTime);
            assertThat(entity.getLastModifiedDateTime()).isEqualTo(modifiedTime);
            assertThat(entity.getRevisionNumber()).isEqualTo("2.1");
            assertThat(entity.getSubject()).isEqualTo("Billing Account Subject");
            assertThat(entity.getTitle()).isEqualTo("Primary Billing Account");
            assertThat(entity.getType()).isEqualTo("RESIDENTIAL_BILLING");
        }

        @Test
        @DisplayName("Should persist all customer account specific fields correctly")
        void shouldPersistAllCustomerAccountSpecificFieldsCorrectly() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setBillingCycle("SEMI_ANNUAL");
            account.setBudgetBill("LEVEL_PAY_PLAN");
            account.setLastBillAmount(35000L); // $350.00
            account.setContactInfo("Jane Smith - Primary Contact");
            account.setAccountId("ACCT-SPECIAL-999888");
            account.setIsPrePay(true);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            CustomerAccountEntity entity = retrieved.get();
            assertThat(entity.getBillingCycle()).isEqualTo("SEMI_ANNUAL");
            assertThat(entity.getBudgetBill()).isEqualTo("LEVEL_PAY_PLAN");
            assertThat(entity.getLastBillAmount()).isEqualTo(35000L);
            assertThat(entity.getContactInfo()).isEqualTo("Jane Smith - Primary Contact");
            assertThat(entity.getAccountId()).isEqualTo("ACCT-SPECIAL-999888");
            assertThat(entity.getIsPrePay()).isTrue();
        }

        @Test
        @DisplayName("Should handle null optional fields correctly")
        void shouldHandleNullOptionalFieldsCorrectly() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setCreatedDateTime(null);
            account.setLastModifiedDateTime(null);
            account.setRevisionNumber(null);
            account.setSubject(null);
            account.setTitle(null);
            account.setType(null);
            account.setBillingCycle(null);
            account.setBudgetBill(null);
            account.setLastBillAmount(null);
            account.setContactInfo(null);
            account.setAccountId(null);
            account.setIsPrePay(null);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            CustomerAccountEntity entity = retrieved.get();
            assertThat(entity.getCreatedDateTime()).isNull();
            assertThat(entity.getLastModifiedDateTime()).isNull();
            assertThat(entity.getRevisionNumber()).isNull();
            assertThat(entity.getSubject()).isNull();
            assertThat(entity.getTitle()).isNull();
            assertThat(entity.getType()).isNull();
            assertThat(entity.getBillingCycle()).isNull();
            assertThat(entity.getBudgetBill()).isNull();
            assertThat(entity.getLastBillAmount()).isNull();
            assertThat(entity.getContactInfo()).isNull();
            assertThat(entity.getAccountId()).isNull();
            assertThat(entity.getIsPrePay()).isNull();
        }
    }

    @Nested
    @DisplayName("Customer Relationship Testing")
    class CustomerRelationshipTest {

        @Test
        @DisplayName("Should maintain Customer relationship")
        void shouldMaintainCustomerRelationship() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomer()).isNotNull();
            assertThat(retrieved.get().getCustomer().getId()).isEqualTo(account.getCustomer().getId());
        }

        @Test
        @DisplayName("Should handle lazy loading of Customer")
        void shouldHandleLazyLoadingOfCustomer() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            CustomerAccountEntity saved = persistAndFlush(account);

            // Act - Clear persistence context to test lazy loading
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            // Access the customer to trigger lazy loading
            assertThat(retrieved.get().getCustomer().getCustomerName()).isNotNull();
        }

        @Test
        @DisplayName("Should allow null Customer")
        void shouldAllowNullCustomer() {
            // Arrange
            CustomerAccountEntity account = createValidCustomerAccount();
            account.setCustomer(null);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomer()).isNull();
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();

            // Act
            CustomerAccountEntity saved = customerAccountRepository.save(account);
            flushAndClear();

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            
            CustomerAccountEntity entity = retrieved.get();
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getCreated()).isNotNull();
            assertThat(entity.getUpdated()).isNotNull();
            assertThat(entity.getDescription()).isNotNull();
        }

        @Test
        @DisplayName("Should update timestamps on modification")
        void shouldUpdateTimestampsOnModification() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            CustomerAccountEntity saved = persistAndFlush(account);
            
            // Wait a moment to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            saved.setDescription("Updated Description");
            CustomerAccountEntity updated = customerAccountRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(updated.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUpdated()).isAfter(retrieved.get().getCreated());
        }

        @Test
        @DisplayName("Should generate unique IDs for different entities")
        void shouldGenerateUniqueIdsForDifferentEntities() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            CustomerAccountEntity account2 = createCompleteTestSetup();

            // Act
            CustomerAccountEntity saved1 = customerAccountRepository.save(account1);
            CustomerAccountEntity saved2 = customerAccountRepository.save(account2);
            flushAndClear();

            // Assert
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
            assertThat(saved1.getId()).isNotNull();
            assertThat(saved2.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            CustomerAccountEntity account2 = createCompleteTestSetup();
            
            CustomerAccountEntity saved1 = persistAndFlush(account1);
            CustomerAccountEntity saved2 = persistAndFlush(account2);

            // Act & Assert
            assertThat(saved1).isNotEqualTo(saved2);
            // Note: Hibernate proxy-aware hashCode implementation returns class hashCode for different entities
            // This is expected behavior for entities with different IDs
            
            // Same entity should be equal to itself
            assertThat(saved1).isEqualTo(saved1);
            assertThat(saved1.hashCode()).isEqualTo(saved1.hashCode());
            
            // Different entities with different IDs should not be equal
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        }

        @Test
        @DisplayName("Should generate meaningful toString representation")
        void shouldGenerateMeaningfulToStringRepresentation() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setAccountId("ACCT-12345");
            account.setTitle("Test Account");
            CustomerAccountEntity saved = persistAndFlush(account);

            // Act
            String toString = saved.toString();

            // Assert
            assertThat(toString).contains("CustomerAccountEntity");
            assertThat(toString).contains("id = " + saved.getId());
            assertThat(toString).contains("accountId = ACCT-12345");
            assertThat(toString).contains("title = Test Account");
        }
    }
}