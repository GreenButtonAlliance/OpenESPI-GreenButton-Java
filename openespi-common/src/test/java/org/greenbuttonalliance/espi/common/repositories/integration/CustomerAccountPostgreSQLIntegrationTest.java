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

import org.greenbuttonalliance.espi.common.domain.customer.entity.AccountNotification;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.enums.NotificationMethodKind;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAccountRepository;
import org.greenbuttonalliance.espi.common.test.BaseTestContainersTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * CustomerAccount entity integration tests using PostgreSQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real PostgreSQL database.
 */
@DisplayName("CustomerAccount Integration Tests - PostgreSQL")
@ActiveProfiles({"test", "test-postgresql"})
class CustomerAccountPostgreSQLIntegrationTest extends BaseTestContainersTest {

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
    private CustomerAccountRepository customerAccountRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer account with all fields")
        void shouldSaveAndRetrieveCustomerAccountWithAllFields() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setType("Residential Account");
            account.setAuthorName("PostgreSQL Test Author");
            account.setCreatedDateTime(OffsetDateTime.now().minusDays(20));
            account.setLastModifiedDateTime(OffsetDateTime.now());
            account.setRevisionNumber("Rev-2.0");
            account.setSubject("PostgreSQL Account Subject");
            account.setTitle("PostgreSQL Account Title");
            account.setBillingCycle("30");
            account.setBudgetBill("Budget Plan B");
            account.setLastBillAmount(250000L);
            account.setAccountId("POSTGRES-ACCT-67890");
            account.setIsPrePay(false);

            // Electronic address for document
            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("document@postgres.test");
            electronicAddress.setWeb("https://postgres-account.test");
            account.setElectronicAddress(electronicAddress);

            // Document status
            Status docStatus = new Status();
            docStatus.setValue("active");
            docStatus.setDateTime(OffsetDateTime.now());
            docStatus.setReason("PostgreSQL document activation");
            account.setDocStatus(docStatus);

            // Contact info
            Organisation contactInfo = new Organisation();
            contactInfo.setOrganisationName("PostgreSQL Contact Services");

            StreetAddress streetAddress = new StreetAddress();
            streetAddress.setStreetDetail("789 PostgreSQL Contact Boulevard");
            streetAddress.setTownDetail("Postgres City");
            streetAddress.setStateOrProvince("WA");
            streetAddress.setPostalCode("98001");
            streetAddress.setCountry("USA");
            contactInfo.setStreetAddress(streetAddress);

            ElectronicAddress contactElectronicAddress = new ElectronicAddress();
            contactElectronicAddress.setEmail1("contact@postgres.test");
            contactElectronicAddress.setWeb("https://contact.postgres.test");
            contactInfo.setElectronicAddress(contactElectronicAddress);

            account.setContactInfo(contactInfo);

            // Act
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo("Residential Account");
            assertThat(result.getAuthorName()).isEqualTo("PostgreSQL Test Author");
            assertThat(result.getBillingCycle()).isEqualTo("30");
            assertThat(result.getBudgetBill()).isEqualTo("Budget Plan B");
            assertThat(result.getLastBillAmount()).isEqualTo(250000L);
            assertThat(result.getAccountId()).isEqualTo("POSTGRES-ACCT-67890");
            assertThat(result.getIsPrePay()).isFalse();

            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("document@postgres.test");

            assertThat(result.getDocStatus()).isNotNull();
            assertThat(result.getDocStatus().getValue()).isEqualTo("active");

            assertThat(result.getContactInfo()).isNotNull();
            assertThat(result.getContactInfo().getOrganisationName()).isEqualTo("PostgreSQL Contact Services");
            assertThat(result.getContactInfo().getStreetAddress()).isNotNull();
            assertThat(result.getContactInfo().getStreetAddress().getStreetDetail()).isEqualTo("789 PostgreSQL Contact Boulevard");
        }

        @Test
        @DisplayName("Should update customer account fields")
        void shouldUpdateCustomerAccountFields() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("ORIGINAL-PG-ACCT-ID");
            account.setBillingCycle("5");
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();

            // Act
            savedAccount.setAccountId("UPDATED-PG-ACCT-ID");
            savedAccount.setBillingCycle("20");
            savedAccount.setIsPrePay(true);
            CustomerAccountEntity updatedAccount = customerAccountRepository.save(savedAccount);
            flushAndClear();

            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(updatedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAccountId()).isEqualTo("UPDATED-PG-ACCT-ID");
            assertThat(retrieved.get().getBillingCycle()).isEqualTo("20");
            assertThat(retrieved.get().getIsPrePay()).isTrue();
        }

        @Test
        @DisplayName("Should delete customer account")
        void shouldDeleteCustomerAccount() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("DELETE-ME-PG-ACCT");
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();

            // Act
            customerAccountRepository.deleteById(savedAccount.getId());
            flushAndClear();

            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedAccount.getId());

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
            List<CustomerAccountEntity> accounts = TestDataBuilders.createValidEntities(5,
                TestDataBuilders::createValidCustomerAccount);

            for (int i = 0; i < accounts.size(); i++) {
                accounts.get(i).setAccountId("POSTGRES-BULK-ACCT-" + i);
            }

            // Act
            List<CustomerAccountEntity> savedAccounts = customerAccountRepository.saveAll(accounts);
            flushAndClear();

            // Assert
            assertThat(savedAccounts).hasSize(5);
            assertThat(savedAccounts).allMatch(account -> account.getId() != null);

            long count = customerAccountRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should handle bulk delete operations")
        void shouldHandleBulkDeleteOperations() {
            // Arrange
            List<CustomerAccountEntity> accounts = TestDataBuilders.createValidEntities(3,
                TestDataBuilders::createValidCustomerAccount);

            List<CustomerAccountEntity> savedAccounts = customerAccountRepository.saveAll(accounts);
            long initialCount = customerAccountRepository.count();
            flushAndClear();

            // Act
            customerAccountRepository.deleteAll(savedAccounts);
            flushAndClear();

            // Assert
            long finalCount = customerAccountRepository.count();
            assertThat(finalCount).isEqualTo(initialCount - 3);
        }
    }

    @Nested
    @DisplayName("Embedded Objects Persistence")
    class EmbeddedObjectsTest {

        @Test
        @DisplayName("Should persist and retrieve Organisation contact info with all nested types")
        void shouldPersistOrganisationWithAllTypes() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("POSTGRES-ORG-TEST");

            Organisation contactInfo = new Organisation();
            contactInfo.setOrganisationName("Complete PostgreSQL Organization");

            StreetAddress streetAddress = new StreetAddress();
            streetAddress.setStreetDetail("321 PostgreSQL Contact Drive");
            streetAddress.setTownDetail("Postgres Town");
            streetAddress.setStateOrProvince("OR");
            streetAddress.setPostalCode("97001");
            streetAddress.setCountry("USA");
            contactInfo.setStreetAddress(streetAddress);

            StreetAddress postalAddress = new StreetAddress();
            postalAddress.setStreetDetail("PO Box 666");
            postalAddress.setTownDetail("Postgres Town");
            postalAddress.setStateOrProvince("OR");
            postalAddress.setPostalCode("97002");
            postalAddress.setCountry("USA");
            contactInfo.setPostalAddress(postalAddress);

            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("primary@postgres.test");
            electronicAddress.setEmail2("secondary@postgres.test");
            electronicAddress.setWeb("https://postgres.org.test");
            electronicAddress.setRadio("RADIO-PG-456");
            contactInfo.setElectronicAddress(electronicAddress);

            account.setContactInfo(contactInfo);

            // Act
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Organisation retrievedContactInfo = retrieved.get().getContactInfo();
            assertThat(retrievedContactInfo).isNotNull();
            assertThat(retrievedContactInfo.getOrganisationName()).isEqualTo("Complete PostgreSQL Organization");
            assertThat(retrievedContactInfo.getStreetAddress().getStreetDetail()).isEqualTo("321 PostgreSQL Contact Drive");
            assertThat(retrievedContactInfo.getPostalAddress().getStreetDetail()).isEqualTo("PO Box 666");
            assertThat(retrievedContactInfo.getElectronicAddress().getEmail1()).isEqualTo("primary@postgres.test");
            assertThat(retrievedContactInfo.getElectronicAddress().getRadio()).isEqualTo("RADIO-PG-456");
        }

        @Test
        @DisplayName("Should persist Status with all fields")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("POSTGRES-STATUS-TEST");

            Status docStatus = new Status();
            docStatus.setValue("archived");
            OffsetDateTime testDateTime = OffsetDateTime.now();
            docStatus.setDateTime(testDateTime);
            docStatus.setRemark("PostgreSQL test remark");
            docStatus.setReason("PostgreSQL test reason");
            account.setDocStatus(docStatus);

            // Act
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Status retrievedStatus = retrieved.get().getDocStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("archived");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getRemark()).isEqualTo("PostgreSQL test remark");
            assertThat(retrievedStatus.getReason()).isEqualTo("PostgreSQL test reason");
        }

        @Test
        @DisplayName("Should persist AccountNotification collection")
        void shouldPersistAccountNotificationCollection() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("POSTGRES-NOTIFICATION-TEST");

            List<AccountNotification> notifications = new ArrayList<>();

            AccountNotification notification1 = new AccountNotification();
            notification1.setMethodKind(NotificationMethodKind.CALL);
            notification1.setTime(OffsetDateTime.now().minusDays(3));
            notification1.setNote("PostgreSQL first notification");
            notification1.setCustomerNotificationKind("MOVE_IN");
            notifications.add(notification1);

            AccountNotification notification2 = new AccountNotification();
            notification2.setMethodKind(NotificationMethodKind.EMAIL);
            notification2.setTime(OffsetDateTime.now().minusDays(1));
            notification2.setNote("PostgreSQL second notification");
            notification2.setCustomerNotificationKind("PAYMENT_DUE");
            notifications.add(notification2);

            account.setNotifications(notifications);

            // Act
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            List<AccountNotification> retrievedNotifications = retrieved.get().getNotifications();
            assertThat(retrievedNotifications).isNotNull();
            assertThat(retrievedNotifications).hasSize(2);
            assertThat(retrievedNotifications.get(0).getMethodKind()).isEqualTo(NotificationMethodKind.CALL);
            assertThat(retrievedNotifications.get(0).getCustomerNotificationKind()).isEqualTo("MOVE_IN");
            assertThat(retrievedNotifications.get(1).getMethodKind()).isEqualTo(NotificationMethodKind.EMAIL);
        }
    }
}
