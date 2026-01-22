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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Customer entity integration tests using MySQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real MySQL database.
 */
@DisplayName("Customer Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class CustomerMySQLIntegrationTest extends BaseTestContainersTest {

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
    private CustomerRepository customerRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer with all fields")
        void shouldSaveAndRetrieveCustomerWithAllFields() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("MySQL Integration Test Customer");
            customer.setKind(CustomerKind.COMMERCIAL);
            customer.setSpecialNeed("Wheelchair access");
            customer.setVip(true);
            customer.setPucNumber("PUC-MYSQL-12345");
            customer.setLocale("en-US");

            // Organisation
            Organisation org = new Organisation();
            org.setOrganisationName("MySQL Test Corporation");

            Organisation.StreetAddress streetAddress = new Organisation.StreetAddress();
            streetAddress.setStreetDetail("123 MySQL Street");
            streetAddress.setTownDetail("Database City");
            streetAddress.setStateOrProvince("CA");
            streetAddress.setPostalCode("94000");
            streetAddress.setCountry("USA");
            org.setStreetAddress(streetAddress);

            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setEmail1("mysql@test.com");
            electronicAddress.setWeb("https://mysql.test.com");
            org.setElectronicAddress(electronicAddress);

            customer.setOrganisation(org);

            // Status
            CustomerEntity.Status status = new CustomerEntity.Status();
            status.setValue("active");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("MySQL integration test");
            customer.setStatus(status);

            // Priority
            CustomerEntity.Priority priority = new CustomerEntity.Priority();
            priority.setValue(1);
            priority.setRank(10);
            priority.setType("high-priority");
            customer.setPriority(priority);

            // Act
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity result = retrieved.get();

            assertThat(result.getCustomerName()).isEqualTo("MySQL Integration Test Customer");
            assertThat(result.getKind()).isEqualTo(CustomerKind.COMMERCIAL);
            assertThat(result.getSpecialNeed()).isEqualTo("Wheelchair access");
            assertThat(result.getVip()).isTrue();
            assertThat(result.getPucNumber()).isEqualTo("PUC-MYSQL-12345");
            assertThat(result.getLocale()).isEqualTo("en-US");

            assertThat(result.getOrganisation()).isNotNull();
            assertThat(result.getOrganisation().getOrganisationName()).isEqualTo("MySQL Test Corporation");
            assertThat(result.getOrganisation().getStreetAddress()).isNotNull();
            assertThat(result.getOrganisation().getStreetAddress().getStreetDetail()).isEqualTo("123 MySQL Street");

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("active");

            assertThat(result.getPriority()).isNotNull();
            assertThat(result.getPriority().getValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should update customer fields")
        void shouldUpdateCustomerFields() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Original Name");
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();

            // Act
            savedCustomer.setCustomerName("Updated Name");
            savedCustomer.setVip(true);
            CustomerEntity updatedCustomer = customerRepository.save(savedCustomer);
            flushAndClear();

            Optional<CustomerEntity> retrieved = customerRepository.findById(updatedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomerName()).isEqualTo("Updated Name");
            assertThat(retrieved.get().getVip()).isTrue();
        }

        @Test
        @DisplayName("Should delete customer")
        void shouldDeleteCustomer() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer to Delete");
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();

            // Act
            customerRepository.deleteById(savedCustomer.getId());
            flushAndClear();

            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

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
            List<CustomerEntity> customers = TestDataBuilders.createValidEntities(5,
                TestDataBuilders::createValidCustomer);

            for (int i = 0; i < customers.size(); i++) {
                customers.get(i).setCustomerName("MySQL Bulk Customer " + i);
            }

            // Act
            List<CustomerEntity> savedCustomers = customerRepository.saveAll(customers);
            flushAndClear();

            // Assert
            assertThat(savedCustomers).hasSize(5);
            assertThat(savedCustomers).allMatch(customer -> customer.getId() != null);

            long count = customerRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should handle bulk delete operations")
        void shouldHandleBulkDeleteOperations() {
            // Arrange
            List<CustomerEntity> customers = TestDataBuilders.createValidEntities(3,
                TestDataBuilders::createValidCustomer);

            List<CustomerEntity> savedCustomers = customerRepository.saveAll(customers);
            long initialCount = customerRepository.count();
            flushAndClear();

            // Act
            customerRepository.deleteAll(savedCustomers);
            flushAndClear();

            // Assert
            long finalCount = customerRepository.count();
            assertThat(finalCount).isEqualTo(initialCount - 3);
        }
    }

    @Nested
    @DisplayName("Embedded Objects Persistence")
    class EmbeddedObjectsTest {

        @Test
        @DisplayName("Should persist and retrieve Organisation with all nested types")
        void shouldPersistOrganisationWithAllTypes() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("MySQL Organisation Test");

            Organisation org = new Organisation();
            org.setOrganisationName("Complete MySQL Corporation");

            Organisation.StreetAddress streetAddress = new Organisation.StreetAddress();
            streetAddress.setStreetDetail("456 MySQL Avenue");
            streetAddress.setTownDetail("Database Town");
            streetAddress.setStateOrProvince("NY");
            streetAddress.setPostalCode("10001");
            streetAddress.setCountry("USA");
            org.setStreetAddress(streetAddress);

            Organisation.StreetAddress postalAddress = new Organisation.StreetAddress();
            postalAddress.setStreetDetail("PO Box 999");
            postalAddress.setTownDetail("Database Town");
            postalAddress.setStateOrProvince("NY");
            postalAddress.setPostalCode("10002");
            postalAddress.setCountry("USA");
            org.setPostalAddress(postalAddress);

            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setEmail1("contact@mysql.test");
            electronicAddress.setEmail2("support@mysql.test");
            electronicAddress.setWeb("https://mysql.test");
            electronicAddress.setRadio("RADIO-123");
            org.setElectronicAddress(electronicAddress);

            customer.setOrganisation(org);

            // Act
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Organisation retrievedOrg = retrieved.get().getOrganisation();
            assertThat(retrievedOrg).isNotNull();
            assertThat(retrievedOrg.getOrganisationName()).isEqualTo("Complete MySQL Corporation");
            assertThat(retrievedOrg.getStreetAddress().getStreetDetail()).isEqualTo("456 MySQL Avenue");
            assertThat(retrievedOrg.getPostalAddress().getStreetDetail()).isEqualTo("PO Box 999");
            assertThat(retrievedOrg.getElectronicAddress().getEmail1()).isEqualTo("contact@mysql.test");
            assertThat(retrievedOrg.getElectronicAddress().getRadio()).isEqualTo("RADIO-123");
        }

        @Test
        @DisplayName("Should persist Status with all fields")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("MySQL Status Test");

            CustomerEntity.Status status = new CustomerEntity.Status();
            status.setValue("suspended");
            OffsetDateTime testDateTime = OffsetDateTime.now();
            status.setDateTime(testDateTime);
            status.setReason("MySQL test suspension");
            customer.setStatus(status);

            // Act
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity.Status retrievedStatus = retrieved.get().getStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("suspended");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getReason()).isEqualTo("MySQL test suspension");
        }

        @Test
        @DisplayName("Should persist Priority with all fields")
        void shouldPersistPriorityWithAllFields() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("MySQL Priority Test");

            CustomerEntity.Priority priority = new CustomerEntity.Priority();
            priority.setValue(5);
            priority.setRank(50);
            priority.setType("medium-priority");
            customer.setPriority(priority);

            // Act
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity.Priority retrievedPriority = retrieved.get().getPriority();
            assertThat(retrievedPriority).isNotNull();
            assertThat(retrievedPriority.getValue()).isEqualTo(5);
            assertThat(retrievedPriority.getRank()).isEqualTo(50);
            assertThat(retrievedPriority.getType()).isEqualTo("medium-priority");
        }
    }
}