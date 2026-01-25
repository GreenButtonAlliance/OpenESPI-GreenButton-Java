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
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for CustomerAccountRepository.
 * Phase 18: CustomerAccount schema compliance testing.
 *
 * Tests CRUD operations, Document field persistence, Organisation embedded objects,
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
     * Creates a valid CustomerAccountEntity with all Document fields for testing.
     * Phase 18: Includes all Document base class fields per customer.xsd.
     */
    private CustomerAccountEntity createValidCustomerAccount() {
        CustomerAccountEntity account = new CustomerAccountEntity();

        // IdentifiedObject fields
        account.setDescription("Test Customer Account - " + faker.lorem().sentence(3));

        // Document fields (customer.xsd lines 819-872)
        account.setType("BILLING");
        account.setAuthorName(faker.name().fullName());
        account.setCreatedDateTime(randomOffsetDateTime());
        account.setLastModifiedDateTime(randomOffsetDateTime());
        account.setRevisionNumber("1.0");

        // Document.electronicAddress
        Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
        electronicAddress.setEmail1(faker.internet().emailAddress());
        electronicAddress.setEmail2(faker.internet().emailAddress());
        electronicAddress.setWeb(faker.internet().url());
        account.setElectronicAddress(electronicAddress);

        account.setSubject("Billing Account");
        account.setTitle("Customer Account " + faker.number().digits(6));

        // Document.docStatus
        Status docStatus = new Status();
        docStatus.setValue("ACTIVE");
        docStatus.setDateTime(randomOffsetDateTime());
        docStatus.setReason("Account in good standing");
        account.setDocStatus(docStatus);

        // CustomerAccount specific fields (customer.xsd lines 118-158)
        account.setBillingCycle("MONTHLY");
        account.setBudgetBill("STANDARD");
        account.setLastBillAmount(faker.number().numberBetween(5000L, 50000L));

        // contactInfo Organisation embedded object
        Organisation contactInfo = new Organisation();
        contactInfo.setOrganisationName(faker.company().name());
        Organisation.StreetAddress streetAddress = new Organisation.StreetAddress();
        streetAddress.setStreetDetail(faker.address().streetAddress());
        streetAddress.setTownDetail(faker.address().city());
        streetAddress.setStateOrProvince(faker.address().stateAbbr());
        streetAddress.setPostalCode(faker.address().zipCode());
        streetAddress.setCountry("USA");
        contactInfo.setStreetAddress(streetAddress);
        account.setContactInfo(contactInfo);

        account.setAccountId("ACCT-" + faker.number().digits(8));

        // Extension fields
        account.setIsPrePay(false);

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

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getId()).isEqualTo(saved.getId());
            assertThat(retrieved.get().getAccountId()).isEqualTo(saved.getAccountId());
            assertThat(retrieved.get().getBillingCycle()).isEqualTo(saved.getBillingCycle());
        }

        @Test
        @DisplayName("Should save customer account with customer relationship")
        void shouldSaveCustomerAccountWithCustomerRelationship() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomer()).isNotNull();
            assertThat(retrieved.get().getCustomer().getId()).isEqualTo(saved.getCustomer().getId());
        }

        @Test
        @DisplayName("Should update customer account successfully")
        void shouldUpdateCustomerAccountSuccessfully() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            CustomerAccountEntity saved = persistAndFlush(account);
            UUID savedId = saved.getId();

            // Act
            saved.setBillingCycle("QUARTERLY");
            saved.setLastBillAmount(25000L);
            customerAccountRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedId);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getBillingCycle()).isEqualTo("QUARTERLY");
            assertThat(retrieved.get().getLastBillAmount()).isEqualTo(25000L);
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
            long initialCount = customerAccountRepository.count();
            CustomerAccountEntity account1 = createCompleteTestSetup();
            CustomerAccountEntity account2 = createCompleteTestSetup();
            persistAndFlush(account1);
            persistAndFlush(account2);

            // Act
            List<CustomerAccountEntity> allAccounts = customerAccountRepository.findAll();

            // Assert
            assertThat(allAccounts).hasSizeGreaterThanOrEqualTo(2);
            long finalCount = customerAccountRepository.count();
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }

        @Test
        @DisplayName("Should check if customer account exists")
        void shouldCheckIfCustomerAccountExists() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            CustomerAccountEntity saved = persistAndFlush(account);

            // Act
            boolean exists = customerAccountRepository.existsById(saved.getId());
            boolean notExists = customerAccountRepository.existsById(UUID.randomUUID());

            // Assert
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
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
    @DisplayName("Document Field Persistence")
    class DocumentFieldPersistenceTest {

        @Test
        @DisplayName("Should persist all Document base fields correctly")
        void shouldPersistAllDocumentBaseFieldsCorrectly() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            OffsetDateTime createdTime = OffsetDateTime.now().minusDays(5).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            OffsetDateTime modifiedTime = OffsetDateTime.now().minusDays(1).truncatedTo(java.time.temporal.ChronoUnit.MICROS);

            account.setType("PAYMENT");
            account.setAuthorName("System Administrator");
            account.setCreatedDateTime(createdTime);
            account.setLastModifiedDateTime(modifiedTime);
            account.setRevisionNumber("2.5");
            account.setSubject("Payment Account Management");
            account.setTitle("Payment Account #98765");

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity found = retrieved.get();
            assertThat(found.getType()).isEqualTo("PAYMENT");
            assertThat(found.getAuthorName()).isEqualTo("System Administrator");
            assertThat(found.getCreatedDateTime()).isEqualTo(createdTime);
            assertThat(found.getLastModifiedDateTime()).isEqualTo(modifiedTime);
            assertThat(found.getRevisionNumber()).isEqualTo("2.5");
            assertThat(found.getSubject()).isEqualTo("Payment Account Management");
            assertThat(found.getTitle()).isEqualTo("Payment Account #98765");
        }

        @Test
        @DisplayName("Should persist Document electronicAddress embedded object")
        void shouldPersistDocumentElectronicAddressEmbeddedObject() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setEmail1("primary@example.com");
            electronicAddress.setEmail2("secondary@example.com");
            electronicAddress.setWeb("https://www.example.com");
            electronicAddress.setRadio("FM-101.5");
            account.setElectronicAddress(electronicAddress);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity found = retrieved.get();
            assertThat(found.getElectronicAddress()).isNotNull();
            assertThat(found.getElectronicAddress().getEmail1()).isEqualTo("primary@example.com");
            assertThat(found.getElectronicAddress().getEmail2()).isEqualTo("secondary@example.com");
            assertThat(found.getElectronicAddress().getWeb()).isEqualTo("https://www.example.com");
            assertThat(found.getElectronicAddress().getRadio()).isEqualTo("FM-101.5");
        }

        @Test
        @DisplayName("Should persist Document docStatus embedded object")
        void shouldPersistDocumentDocStatusEmbeddedObject() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            Status docStatus = new Status();
            docStatus.setValue("SUSPENDED");
            OffsetDateTime statusTime = OffsetDateTime.now().minusDays(2).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            docStatus.setDateTime(statusTime);
            docStatus.setReason("Payment overdue");
            account.setDocStatus(docStatus);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity found = retrieved.get();
            assertThat(found.getDocStatus()).isNotNull();
            assertThat(found.getDocStatus().getValue()).isEqualTo("SUSPENDED");
            assertThat(found.getDocStatus().getDateTime()).isEqualTo(statusTime);
            assertThat(found.getDocStatus().getReason()).isEqualTo("Payment overdue");
        }
    }

    @Nested
    @DisplayName("CustomerAccount Field Persistence")
    class CustomerAccountFieldPersistenceTest {

        @Test
        @DisplayName("Should persist all CustomerAccount specific fields correctly")
        void shouldPersistAllCustomerAccountSpecificFieldsCorrectly() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setBillingCycle("QUARTERLY");
            account.setBudgetBill("PREMIUM");
            account.setLastBillAmount(35000L);
            account.setAccountId("ACCT-SPECIAL-12345");
            account.setIsPrePay(true);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity found = retrieved.get();
            assertThat(found.getBillingCycle()).isEqualTo("QUARTERLY");
            assertThat(found.getBudgetBill()).isEqualTo("PREMIUM");
            assertThat(found.getLastBillAmount()).isEqualTo(35000L);
            assertThat(found.getAccountId()).isEqualTo("ACCT-SPECIAL-12345");
            assertThat(found.getIsPrePay()).isTrue();
        }

        @Test
        @DisplayName("Should persist contactInfo Organisation embedded object")
        void shouldPersistContactInfoOrganisationEmbeddedObject() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();

            Organisation contactInfo = new Organisation();
            contactInfo.setOrganisationName("ACME Corporation");

            Organisation.StreetAddress streetAddress = new Organisation.StreetAddress();
            streetAddress.setStreetDetail("123 Main Street");
            streetAddress.setTownDetail("Springfield");
            streetAddress.setStateOrProvince("IL");
            streetAddress.setPostalCode("62701");
            streetAddress.setCountry("USA");
            contactInfo.setStreetAddress(streetAddress);

            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setEmail1("contact@acme.com");
            electronicAddress.setWeb("https://www.acme.com");
            contactInfo.setElectronicAddress(electronicAddress);

            account.setContactInfo(contactInfo);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity found = retrieved.get();
            assertThat(found.getContactInfo()).isNotNull();
            assertThat(found.getContactInfo().getOrganisationName()).isEqualTo("ACME Corporation");
            assertThat(found.getContactInfo().getStreetAddress()).isNotNull();
            assertThat(found.getContactInfo().getStreetAddress().getStreetDetail()).isEqualTo("123 Main Street");
            assertThat(found.getContactInfo().getStreetAddress().getTownDetail()).isEqualTo("Springfield");
            assertThat(found.getContactInfo().getStreetAddress().getStateOrProvince()).isEqualTo("IL");
            assertThat(found.getContactInfo().getStreetAddress().getPostalCode()).isEqualTo("62701");
            assertThat(found.getContactInfo().getStreetAddress().getCountry()).isEqualTo("USA");
            assertThat(found.getContactInfo().getElectronicAddress()).isNotNull();
            assertThat(found.getContactInfo().getElectronicAddress().getEmail1()).isEqualTo("contact@acme.com");
            assertThat(found.getContactInfo().getElectronicAddress().getWeb()).isEqualTo("https://www.acme.com");
        }

        @Test
        @DisplayName("Should handle null optional fields correctly")
        void shouldHandleNullOptionalFieldsCorrectly() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setBudgetBill(null);
            account.setLastBillAmount(null);
            account.setElectronicAddress(null);
            account.setDocStatus(null);
            account.setContactInfo(null);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity found = retrieved.get();
            assertThat(found.getBudgetBill()).isNull();
            assertThat(found.getLastBillAmount()).isNull();
            assertThat(found.getElectronicAddress()).isNull();
            assertThat(found.getDocStatus()).isNull();
            assertThat(found.getContactInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("Customer Relationship Testing")
    class CustomerRelationshipTest {

        @Test
        @DisplayName("Should maintain Customer relationship")
        void shouldMaintainCustomerRelationship() {
            // Arrange
            CustomerEntity customer = createValidCustomer();
            customer.setCustomerName("Test Corporation");
            CustomerEntity savedCustomer = persistAndFlush(customer);

            CustomerAccountEntity account = createValidCustomerAccount();
            account.setCustomer(savedCustomer);
            CustomerAccountEntity savedAccount = persistAndFlush(account);

            // Act
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomer()).isNotNull();
            assertThat(retrieved.get().getCustomer().getId()).isEqualTo(savedCustomer.getId());
            assertThat(retrieved.get().getCustomer().getCustomerName()).isEqualTo("Test Corporation");
        }

        @Test
        @DisplayName("Should handle lazy loading of Customer")
        void shouldHandleLazyLoadingOfCustomer() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            CustomerAccountEntity saved = persistAndFlush(account);
            flushAndClear();

            // Act
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomer()).isNotNull();
            // Access lazy-loaded property
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
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
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
            account.setDescription("Test Description for Account");
            java.time.LocalDateTime publishedTime = java.time.LocalDateTime.now().minusDays(10).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            account.setPublished(publishedTime);

            // Act
            CustomerAccountEntity saved = persistAndFlush(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity found = retrieved.get();
            assertThat(found.getId()).isNotNull();
            assertThat(found.getDescription()).isEqualTo("Test Description for Account");
            assertThat(found.getPublished()).isEqualTo(publishedTime);
            assertThat(found.getCreated()).isNotNull();
            assertThat(found.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should update timestamps on modification")
        void shouldUpdateTimestampsOnModification() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            CustomerAccountEntity saved = persistAndFlush(account);
            java.time.LocalDateTime originalUpdated = saved.getUpdated();
            UUID savedId = saved.getId();

            // Act
            saved.setBillingCycle("ANNUAL");
            customerAccountRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedId);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUpdated()).isAfter(originalUpdated);
        }

        @Test
        @DisplayName("Should generate unique IDs for different entities")
        void shouldGenerateUniqueIdsForDifferentEntities() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            CustomerAccountEntity account2 = createCompleteTestSetup();

            // Act
            CustomerAccountEntity saved1 = persistAndFlush(account1);
            CustomerAccountEntity saved2 = persistAndFlush(account2);

            // Assert
            assertThat(saved1.getId()).isNotNull();
            assertThat(saved2.getId()).isNotNull();
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            CustomerAccountEntity account1 = createCompleteTestSetup();
            CustomerAccountEntity saved1 = persistAndFlush(account1);

            CustomerAccountEntity account2 = createCompleteTestSetup();
            CustomerAccountEntity saved2 = persistAndFlush(account2);

            // Act & Assert
            assertThat(saved1)
                .isEqualTo(saved1)
                .isNotEqualTo(saved2)
                .hasSameHashCodeAs(saved1);
        }

        @Test
        @DisplayName("Should generate meaningful toString representation")
        void shouldGenerateMeaningfulToStringRepresentation() {
            // Arrange
            CustomerAccountEntity account = createCompleteTestSetup();
            account.setAccountId("TEST-ACCOUNT-123");
            CustomerAccountEntity saved = persistAndFlush(account);

            // Act
            String toString = saved.toString();

            // Assert
            assertThat(toString)
                .contains("CustomerAccountEntity")
                .contains("id")
                .contains("accountId");
        }
    }
}
