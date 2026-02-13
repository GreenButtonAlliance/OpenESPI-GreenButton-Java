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

import jakarta.validation.ConstraintViolation;
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
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Comprehensive test suite for CustomerRepository.
 *
 * Tests all CRUD operations, relationships, and validation constraints for Customer entities.
 * Per ESPI 4.0 API specification, only default JpaRepository methods are supported (findById, findAll, save, delete).
 * Removed tests for: findByCustomerName, findByKind, findByPucNumber, findVipCustomers,
 * findCustomersWithSpecialNeeds, findByLocale, findByPriorityRange, findByOrganisationName
 */
@DisplayName("Customer Repository Tests")
class CustomerRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer successfully")
        void shouldSaveAndRetrieveCustomerSuccessfully() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setDescription("Test Customer for CRUD");
            customer.setCustomerName("ACME Corporation");

            // Act
            CustomerEntity saved = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Customer for CRUD");
            assertThat(retrieved.get().getCustomerName()).isEqualTo("ACME Corporation");
            assertThat(retrieved.get().getKind()).isEqualTo(CustomerKind.RESIDENTIAL);
        }

        @Test
        @DisplayName("Should find all customers")
        void shouldFindAllCustomers() {
            // Arrange
            List<CustomerEntity> customers = TestDataBuilders.createValidEntities(3, TestDataBuilders::createValidCustomer);
            customers.forEach(c -> c.setCustomerName("Bulk Customer " + customers.indexOf(c)));
            customerRepository.saveAll(customers);
            flushAndClear();

            // Act
            List<CustomerEntity> allCustomers = customerRepository.findAll();

            // Assert
            assertThat(allCustomers).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allCustomers).extracting(CustomerEntity::getCustomerName)
                    .contains("Bulk Customer 0", "Bulk Customer 1", "Bulk Customer 2");
        }

        @Test
        @DisplayName("Should delete customer successfully")
        void shouldDeleteCustomerSuccessfully() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer to Delete");
            CustomerEntity saved = customerRepository.save(customer);
            UUID customerId = saved.getId();
            flushAndClear();

            // Act
            customerRepository.deleteById(customerId);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(customerId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if customer exists")
        void shouldCheckIfCustomerExists() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            CustomerEntity saved = customerRepository.save(customer);
            flushAndClear();

            // Act & Assert
            assertThat(customerRepository.existsById(saved.getId())).isTrue();
            assertThat(customerRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count customers")
        void shouldCountCustomers() {
            // Arrange
            long initialCount = customerRepository.count();
            List<CustomerEntity> customers = TestDataBuilders.createValidEntities(5, TestDataBuilders::createValidCustomer);
            customerRepository.saveAll(customers);
            flushAndClear();

            // Act
            long finalCount = customerRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 5);
        }
    }

    @Nested
    @DisplayName("JPA Relationships")
    class RelationshipsTest {

        @Test
        @DisplayName("Should handle customer accounts relationship")
        void shouldHandleCustomerAccountsRelationship() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer with Accounts");

            // Act
            CustomerEntity savedCustomer = customerRepository.save(customer);
            flushAndClear();

            Optional<CustomerEntity> retrieved = customerRepository.findById(savedCustomer.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCustomerAccounts()).isNotNull();
            // Note: In a real implementation, you'd test actual CustomerAccount relationships
        }

        @Test
        @DisplayName("Should handle null relationships gracefully")
        void shouldHandleNullRelationshipsGracefully() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer without Relationships");
            customer.setCustomerAccounts(null);

            // Act & Assert
            assertThatCode(() -> {
                CustomerEntity saved = customerRepository.save(customer);
                System.out.println("[DEBUG_LOG] After save - customerAccounts: " + saved.getCustomerAccounts());
                flushAndClear();
                Optional<CustomerEntity> retrieved = customerRepository.findById(saved.getId());
                System.out.println("[DEBUG_LOG] After retrieve - customerAccounts: " + retrieved.get().getCustomerAccounts());
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get().getCustomerAccounts()).isNotNull();
                assertThat(retrieved.get().getCustomerAccounts()).isEmpty();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Embedded Objects Persistence")
    class EmbeddedObjectsTest {

        @Test
        @DisplayName("Should persist and retrieve Organisation embedded object")
        void shouldPersistAndRetrieveOrganisation() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer with Organisation");

            // Set Organisation embedded object
            Organisation org = new Organisation();
            org.setOrganisationName("ACME Energy Services");

            StreetAddress streetAddress = new StreetAddress();
            streetAddress.setStreetDetail("123 Main Street");
            streetAddress.setTownDetail("San Francisco");
            streetAddress.setStateOrProvince("CA");
            streetAddress.setPostalCode("94102");
            streetAddress.setCountry("USA");
            org.setStreetAddress(streetAddress);

            StreetAddress postalAddress = new StreetAddress();
            postalAddress.setStreetDetail("PO Box 789");
            postalAddress.setTownDetail("San Francisco");
            postalAddress.setStateOrProvince("CA");
            postalAddress.setPostalCode("94103");
            postalAddress.setCountry("USA");
            org.setPostalAddress(postalAddress);

            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("contact@acme.com");
            electronicAddress.setEmail2("support@acme.com");
            electronicAddress.setWeb("https://www.acme.com");
            org.setElectronicAddress(electronicAddress);

            customer.setOrganisation(org);

            // Act
            CustomerEntity saved = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Organisation retrievedOrg = retrieved.get().getOrganisation();
            assertThat(retrievedOrg).isNotNull();
            assertThat(retrievedOrg.getOrganisationName()).isEqualTo("ACME Energy Services");
            assertThat(retrievedOrg.getStreetAddress()).isNotNull();
            assertThat(retrievedOrg.getStreetAddress().getStreetDetail()).isEqualTo("123 Main Street");
            assertThat(retrievedOrg.getStreetAddress().getTownDetail()).isEqualTo("San Francisco");
            assertThat(retrievedOrg.getPostalAddress()).isNotNull();
            assertThat(retrievedOrg.getPostalAddress().getStreetDetail()).isEqualTo("PO Box 789");
            assertThat(retrievedOrg.getElectronicAddress()).isNotNull();
            assertThat(retrievedOrg.getElectronicAddress().getEmail1()).isEqualTo("contact@acme.com");
        }

        @Test
        @DisplayName("Should persist and retrieve Status embedded object")
        void shouldPersistAndRetrieveStatus() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer with Status");

            CustomerEntity.Status status = new CustomerEntity.Status();
            status.setValue("active");
            status.setDateTime(java.time.OffsetDateTime.now());
            status.setReason("Account activated");
            customer.setStatus(status);

            // Act
            CustomerEntity saved = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity.Status retrievedStatus = retrieved.get().getStatus();
            assertThat(retrievedStatus).isNotNull();
            assertThat(retrievedStatus.getValue()).isEqualTo("active");
            assertThat(retrievedStatus.getDateTime()).isNotNull();
            assertThat(retrievedStatus.getReason()).isEqualTo("Account activated");
        }

        @Test
        @DisplayName("Should persist and retrieve Priority embedded object")
        void shouldPersistAndRetrievePriority() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer with Priority");

            CustomerEntity.Priority priority = new CustomerEntity.Priority();
            priority.setValue(1);
            priority.setRank(10);
            priority.setType("high-priority");
            customer.setPriority(priority);

            // Act
            CustomerEntity saved = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity.Priority retrievedPriority = retrieved.get().getPriority();
            assertThat(retrievedPriority).isNotNull();
            assertThat(retrievedPriority.getValue()).isEqualTo(1);
            assertThat(retrievedPriority.getRank()).isEqualTo(10);
            assertThat(retrievedPriority.getType()).isEqualTo("high-priority");
        }

        @Test
        @DisplayName("Should persist and retrieve all embedded objects together")
        void shouldPersistAndRetrieveAllEmbeddedObjects() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer with All Embedded Objects");
            customer.setKind(CustomerKind.COMMERCIAL_INDUSTRIAL);
            customer.setSpecialNeed("Wheelchair access");
            customer.setVip(true);
            customer.setPucNumber("PUC-12345");
            customer.setLocale("en-US");

            // Organisation
            Organisation org = new Organisation();
            org.setOrganisationName("Complete Corp");
            customer.setOrganisation(org);

            // Status
            CustomerEntity.Status status = new CustomerEntity.Status();
            status.setValue("active");
            customer.setStatus(status);

            // Priority
            CustomerEntity.Priority priority = new CustomerEntity.Priority();
            priority.setValue(5);
            customer.setPriority(priority);

            // Act
            CustomerEntity saved = customerRepository.save(customer);
            flushAndClear();
            Optional<CustomerEntity> retrieved = customerRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerEntity result = retrieved.get();
            assertThat(result.getCustomerName()).isEqualTo("Customer with All Embedded Objects");
            assertThat(result.getKind()).isEqualTo(CustomerKind.COMMERCIAL_INDUSTRIAL);
            assertThat(result.getSpecialNeed()).isEqualTo("Wheelchair access");
            assertThat(result.getVip()).isTrue();
            assertThat(result.getPucNumber()).isEqualTo("PUC-12345");
            assertThat(result.getLocale()).isEqualTo("en-US");
            assertThat(result.getOrganisation()).isNotNull();
            assertThat(result.getOrganisation().getOrganisationName()).isEqualTo("Complete Corp");
            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("active");
            assertThat(result.getPriority()).isNotNull();
            assertThat(result.getPriority().getValue()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate customer with valid data")
        void shouldValidateCustomerWithValidData() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Valid Customer Name");

            // Act
            Set<ConstraintViolation<CustomerEntity>> violations = validator.validate(customer);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle null customer name gracefully")
        void shouldHandleNullCustomerNameGracefully() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName(null);

            // Act
            Set<ConstraintViolation<CustomerEntity>> violations = validator.validate(customer);

            // Assert - Customer name is typically optional
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should validate customer kind enum")
        void shouldValidateCustomerKindEnum() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setKind(CustomerKind.COMMERCIAL_INDUSTRIAL);

            // Act
            Set<ConstraintViolation<CustomerEntity>> violations = validator.validate(customer);

            // Assert
            assertThat(violations).isEmpty();
            assertThat(customer.getKind()).isEqualTo(CustomerKind.COMMERCIAL_INDUSTRIAL);
        }

        @Test
        @DisplayName("Should validate VIP flag")
        void shouldValidateVipFlag() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setVip(true);

            // Act
            Set<ConstraintViolation<CustomerEntity>> violations = validator.validate(customer);

            // Assert
            assertThat(violations).isEmpty();
            assertThat(customer.getVip()).isTrue();
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange & Act
            CustomerEntity customer = TestDataBuilders.createValidCustomer();

            // Assert
            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getId()).isInstanceOf(UUID.class);
            // CustomerEntity extends IdentifiedObject and inherits UUID functionality
        }

        @Test
        @DisplayName("Should set timestamps on persist")
        void shouldSetTimestampsOnPersist() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Customer for Timestamp Test");

            // Act
            CustomerEntity saved = customerRepository.save(customer);
            flushAndClear();

            // Assert
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should test equals and hashCode")
        void shouldTestEqualsAndHashCode() {
            // Arrange
            UUID sharedId = UUID.randomUUID();
            
            CustomerEntity customer1 = TestDataBuilders.createValidCustomer();
            customer1.setId(sharedId);
            customer1.setCustomerName("Customer 1");
            
            CustomerEntity customer2 = TestDataBuilders.createValidCustomer();
            customer2.setId(sharedId);
            customer2.setCustomerName("Customer 2");

            // Act & Assert
            assertThat(customer1).isEqualTo(customer2);
            assertThat(customer1.hashCode()).isEqualTo(customer2.hashCode());
        }

        @Test
        @DisplayName("Should generate meaningful toString representation")
        void shouldGenerateMeaningfulToStringRepresentation() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("Test Customer");

            // Act
            String toString = customer.toString();

            // Assert
            assertThat(toString).isNotNull();
            assertThat(toString).contains("CustomerEntity");
            assertThat(toString).contains(customer.getId().toString());
        }
    }
}