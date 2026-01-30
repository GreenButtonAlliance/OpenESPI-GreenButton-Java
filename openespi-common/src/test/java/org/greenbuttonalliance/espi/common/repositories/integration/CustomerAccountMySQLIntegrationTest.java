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
 * CustomerAccount entity integration tests using MySQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real MySQL database.
 */
@DisplayName("CustomerAccount Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class CustomerAccountMySQLIntegrationTest extends BaseTestContainersTest {

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
    private CustomerAccountRepository customerAccountRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer account with all fields")
        void shouldSaveAndRetrieveCustomerAccountWithAllFields() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setType("Commercial Account");
            account.setAuthorName("MySQL Test Author");
            account.setCreatedDateTime(OffsetDateTime.now().minusDays(10));
            account.setLastModifiedDateTime(OffsetDateTime.now());
            account.setRevisionNumber("Rev-1.0");
            account.setSubject("MySQL Account Subject");
            account.setTitle("MySQL Account Title");
            account.setBillingCycle("15");
            account.setBudgetBill("Budget Plan A");
            account.setLastBillAmount(150000L);
            account.setAccountId("MYSQL-ACCT-12345");
            account.setIsPrePay(true);

            // Electronic address for document
            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("document@mysql.test");
            electronicAddress.setWeb("https://mysql-account.test");
            account.setElectronicAddress(electronicAddress);

            // Document status
            Status docStatus = new Status();
            docStatus.setValue("approved");
            docStatus.setDateTime(OffsetDateTime.now());
            docStatus.setReason("MySQL document approval");
            account.setDocStatus(docStatus);

            // Contact info
            Organisation contactInfo = new Organisation();
            contactInfo.setOrganisationName("MySQL Contact Corp");

            StreetAddress streetAddress = new StreetAddress();
            streetAddress.setStreetDetail("123 MySQL Contact Street");
            streetAddress.setTownDetail("Contact City");
            streetAddress.setStateOrProvince("CA");
            streetAddress.setPostalCode("94001");
            streetAddress.setCountry("USA");
            contactInfo.setStreetAddress(streetAddress);

            ElectronicAddress contactElectronicAddress = new ElectronicAddress();
            contactElectronicAddress.setEmail1("contact@mysql.test");
            contactElectronicAddress.setWeb("https://contact.mysql.test");
            contactInfo.setElectronicAddress(contactElectronicAddress);

            account.setContactInfo(contactInfo);

            // Act
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAccountEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo("Commercial Account");
            assertThat(result.getAuthorName()).isEqualTo("MySQL Test Author");
            assertThat(result.getBillingCycle()).isEqualTo("15");
            assertThat(result.getBudgetBill()).isEqualTo("Budget Plan A");
            assertThat(result.getLastBillAmount()).isEqualTo(150000L);
            assertThat(result.getAccountId()).isEqualTo("MYSQL-ACCT-12345");
            assertThat(result.getIsPrePay()).isTrue();

            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("document@mysql.test");

            assertThat(result.getDocStatus()).isNotNull();
            assertThat(result.getDocStatus().getValue()).isEqualTo("approved");

            assertThat(result.getContactInfo()).isNotNull();
            assertThat(result.getContactInfo().getOrganisationName()).isEqualTo("MySQL Contact Corp");
            assertThat(result.getContactInfo().getStreetAddress()).isNotNull();
            assertThat(result.getContactInfo().getStreetAddress().getStreetDetail()).isEqualTo("123 MySQL Contact Street");
        }

        @Test
        @DisplayName("Should update customer account fields")
        void shouldUpdateCustomerAccountFields() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("ORIGINAL-ACCT-ID");
            account.setBillingCycle("1");
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();

            // Act
            savedAccount.setAccountId("UPDATED-ACCT-ID");
            savedAccount.setBillingCycle("15");
            savedAccount.setIsPrePay(true);
            CustomerAccountEntity updatedAccount = customerAccountRepository.save(savedAccount);
            flushAndClear();

            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(updatedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAccountId()).isEqualTo("UPDATED-ACCT-ID");
            assertThat(retrieved.get().getBillingCycle()).isEqualTo("15");
            assertThat(retrieved.get().getIsPrePay()).isTrue();
        }

        @Test
        @DisplayName("Should delete customer account")
        void shouldDeleteCustomerAccount() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("DELETE-ME-ACCT");
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
                accounts.get(i).setAccountId("MYSQL-BULK-ACCT-" + i);
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
            account.setAccountId("MYSQL-ORG-TEST");

            Organisation contactInfo = new Organisation();
            contactInfo.setOrganisationName("Complete MySQL Organization");

            StreetAddress streetAddress = new StreetAddress();
            streetAddress.setStreetDetail("456 MySQL Contact Avenue");
            streetAddress.setTownDetail("MySQL Town");
            streetAddress.setStateOrProvince("NY");
            streetAddress.setPostalCode("10001");
            streetAddress.setCountry("USA");
            contactInfo.setStreetAddress(streetAddress);

            StreetAddress postalAddress = new StreetAddress();
            postalAddress.setStreetDetail("PO Box 888");
            postalAddress.setTownDetail("MySQL Town");
            postalAddress.setStateOrProvince("NY");
            postalAddress.setPostalCode("10002");
            postalAddress.setCountry("USA");
            contactInfo.setPostalAddress(postalAddress);

            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("primary@mysql.test");
            electronicAddress.setEmail2("secondary@mysql.test");
            electronicAddress.setWeb("https://mysql.org.test");
            electronicAddress.setRadio("RADIO-MYSQL-123");
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
            assertThat(retrievedContactInfo.getOrganisationName()).isEqualTo("Complete MySQL Organization");
            assertThat(retrievedContactInfo.getStreetAddress().getStreetDetail()).isEqualTo("456 MySQL Contact Avenue");
            assertThat(retrievedContactInfo.getPostalAddress().getStreetDetail()).isEqualTo("PO Box 888");
            assertThat(retrievedContactInfo.getElectronicAddress().getEmail1()).isEqualTo("primary@mysql.test");
            assertThat(retrievedContactInfo.getElectronicAddress().getRadio()).isEqualTo("RADIO-MYSQL-123");
        }

        @Test
        @DisplayName("Should persist Status with all fields")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("MYSQL-STATUS-TEST");

            Status docStatus = new Status();
            docStatus.setValue("pending");
            OffsetDateTime testDateTime = OffsetDateTime.now();
            docStatus.setDateTime(testDateTime);
            docStatus.setRemark("MySQL test remark");
            docStatus.setReason("MySQL test reason");
            account.setDocStatus(docStatus);

            // Act
            CustomerAccountEntity savedAccount = customerAccountRepository.save(account);
            flushAndClear();
            Optional<CustomerAccountEntity> retrieved = customerAccountRepository.findById(savedAccount.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Status retrievedStatus = retrieved.get().getDocStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("pending");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getRemark()).isEqualTo("MySQL test remark");
            assertThat(retrievedStatus.getReason()).isEqualTo("MySQL test reason");
        }

        @Test
        @DisplayName("Should persist AccountNotification collection")
        void shouldPersistAccountNotificationCollection() {
            // Arrange
            CustomerAccountEntity account = TestDataBuilders.createValidCustomerAccount();
            account.setAccountId("MYSQL-NOTIFICATION-TEST");

            List<AccountNotification> notifications = new ArrayList<>();

            AccountNotification notification1 = new AccountNotification();
            notification1.setMethodKind(NotificationMethodKind.EMAIL);
            notification1.setTime(OffsetDateTime.now().minusDays(5));
            notification1.setNote("First notification");
            notification1.setCustomerNotificationKind("DELINQUENCY");
            notifications.add(notification1);

            AccountNotification notification2 = new AccountNotification();
            notification2.setMethodKind(NotificationMethodKind.LETTER);
            notification2.setTime(OffsetDateTime.now().minusDays(1));
            notification2.setNote("Second notification");
            notification2.setCustomerNotificationKind("MOVE_OUT");
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
            assertThat(retrievedNotifications.get(0).getMethodKind()).isEqualTo(NotificationMethodKind.EMAIL);
            assertThat(retrievedNotifications.get(0).getCustomerNotificationKind()).isEqualTo("DELINQUENCY");
            assertThat(retrievedNotifications.get(1).getMethodKind()).isEqualTo(NotificationMethodKind.LETTER);
        }
    }
}
