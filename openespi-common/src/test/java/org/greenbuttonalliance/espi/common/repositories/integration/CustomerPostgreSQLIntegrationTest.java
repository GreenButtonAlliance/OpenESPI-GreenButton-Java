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
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;
import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;
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
 * Customer entity integration tests using PostgreSQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real PostgreSQL database.
 */
@DisplayName("Customer Integration Tests - PostgreSQL")
@ActiveProfiles({"test", "test-postgresql"})
class CustomerPostgreSQLIntegrationTest extends BaseTestContainersTest {

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
    private CustomerRepository customerRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer with all fields")
        void shouldSaveAndRetrieveCustomerWithAllFields() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("PostgreSQL Integration Test Customer");
            customer.setKind(CustomerKind.RESIDENTIAL);
            customer.setSpecialNeed("Life support equipment");
            customer.setVip(false);
            customer.setPucNumber("PUC-PG-98765");
            customer.setLocale("en-GB");

            // Organisation
            Organisation org = new Organisation();
            org.setOrganisationName("PostgreSQL Test Services");

            StreetAddress streetAddress = new StreetAddress();
            streetAddress.setStreetDetail("789 PostgreSQL Boulevard");
            streetAddress.setTownDetail("Postgres City");
            streetAddress.setStateOrProvince("WA");
            streetAddress.setPostalCode("98000");
            streetAddress.setCountry("USA");
            org.setStreetAddress(streetAddress);

            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("postgres@test.com");
            electronicAddress.setWeb("https://postgres.test.com");
            org.setElectronicAddress(electronicAddress);

            customer.setOrganisation(org);

            // Status
            CustomerEntity.Status status = new CustomerEntity.Status();
            status.setValue("pending");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("PostgreSQL integration test");
            customer.setStatus(status);

            // Priority
            CustomerEntity.Priority priority = new CustomerEntity.Priority();
            priority.setValue(3);
            priority.setRank(30);
            priority.setType("normal-priority");
            customer.setPriority(priority);

            // Act
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity result = retrieved.get();

            assertThat(result.getCustomerName()).isEqualTo("PostgreSQL Integration Test Customer");
            assertThat(result.getKind()).isEqualTo(CustomerKind.RESIDENTIAL);
            assertThat(result.getSpecialNeed()).isEqualTo("Life support equipment");
            assertThat(result.getVip()).isFalse();
            assertThat(result.getPucNumber()).isEqualTo("PUC-PG-98765");
            assertThat(result.getLocale()).isEqualTo("en-GB");

            assertThat(result.getOrganisation()).isNotNull();
            assertThat(result.getOrganisation().getOrganisationName()).isEqualTo("PostgreSQL Test Services");
            assertThat(result.getOrganisation().getStreetAddress()).isNotNull();
            assertThat(result.getOrganisation().getStreetAddress().getStreetDetail()).isEqualTo("789 PostgreSQL Boulevard");

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("pending");

            assertThat(result.getPriority()).isNotNull();
            assertThat(result.getPriority().getValue()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should update customer fields")
        void shouldUpdateCustomerFields() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Original PostgreSQL Name");
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();

            // Act
            savedCustomer.setCustomerName("Updated PostgreSQL Name");
            savedCustomer.setKind(CustomerKind.COMMERCIAL);
            CustomerEntity updatedCustomer = customerRepository.save(savedCustomer);
            flushAndClear();

            Optional<CustomerEntity> retrieved = customerRepository.findById(updatedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomerName()).isEqualTo("Updated PostgreSQL Name");
            assertThat(retrieved.get().getKind()).isEqualTo(CustomerKind.COMMERCIAL);
        }

        @Test
        @DisplayName("Should delete customer")
        void shouldDeleteCustomer() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("PostgreSQL Customer to Delete");
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
                customers.get(i).setCustomerName("PostgreSQL Bulk Customer " + i);
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
            customer.setCustomerName("PostgreSQL Organisation Test");

            Organisation org = new Organisation();
            org.setOrganisationName("Complete PostgreSQL Corporation");

            StreetAddress streetAddress = new StreetAddress();
            streetAddress.setStreetDetail("321 PostgreSQL Drive");
            streetAddress.setTownDetail("Postgres Town");
            streetAddress.setStateOrProvince("OR");
            streetAddress.setPostalCode("97000");
            streetAddress.setCountry("USA");
            org.setStreetAddress(streetAddress);

            StreetAddress postalAddress = new StreetAddress();
            postalAddress.setStreetDetail("PO Box 777");
            postalAddress.setTownDetail("Postgres Town");
            postalAddress.setStateOrProvince("OR");
            postalAddress.setPostalCode("97001");
            postalAddress.setCountry("USA");
            org.setPostalAddress(postalAddress);

            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("contact@postgres.test");
            electronicAddress.setEmail2("support@postgres.test");
            electronicAddress.setWeb("https://postgres.test");
            electronicAddress.setRadio("PG-RADIO-456");
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
            assertThat(retrievedOrg.getOrganisationName()).isEqualTo("Complete PostgreSQL Corporation");
            assertThat(retrievedOrg.getStreetAddress().getStreetDetail()).isEqualTo("321 PostgreSQL Drive");
            assertThat(retrievedOrg.getPostalAddress().getStreetDetail()).isEqualTo("PO Box 777");
            assertThat(retrievedOrg.getElectronicAddress().getEmail1()).isEqualTo("contact@postgres.test");
            assertThat(retrievedOrg.getElectronicAddress().getRadio()).isEqualTo("PG-RADIO-456");
        }

        @Test
        @DisplayName("Should persist Status with all fields")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("PostgreSQL Status Test");

            CustomerEntity.Status status = new CustomerEntity.Status();
            status.setValue("active");
            OffsetDateTime testDateTime = OffsetDateTime.now();
            status.setDateTime(testDateTime);
            status.setReason("PostgreSQL test activation");
            customer.setStatus(status);

            // Act
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity.Status retrievedStatus = retrieved.get().getStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("active");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getReason()).isEqualTo("PostgreSQL test activation");
        }

        @Test
        @DisplayName("Should persist Priority with all fields")
        void shouldPersistPriorityWithAllFields() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("PostgreSQL Priority Test");

            CustomerEntity.Priority priority = new CustomerEntity.Priority();
            priority.setValue(2);
            priority.setRank(20);
            priority.setType("high-priority");
            customer.setPriority(priority);

            // Act
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity.Priority retrievedPriority = retrieved.get().getPriority();
            assertThat(retrievedPriority).isNotNull();
            assertThat(retrievedPriority.getValue()).isEqualTo(2);
            assertThat(retrievedPriority.getRank()).isEqualTo(20);
            assertThat(retrievedPriority.getType()).isEqualTo("high-priority");
        }
    }
}
