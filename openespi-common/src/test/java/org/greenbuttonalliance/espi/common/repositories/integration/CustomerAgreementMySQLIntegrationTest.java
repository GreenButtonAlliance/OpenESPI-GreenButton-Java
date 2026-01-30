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

import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAgreementRepository;
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
 * CustomerAgreement entity integration tests using MySQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real MySQL database.
 */
@DisplayName("CustomerAgreement Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class CustomerAgreementMySQLIntegrationTest extends BaseTestContainersTest {

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
    private CustomerAgreementRepository customerAgreementRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer agreement with all fields")
        void shouldSaveAndRetrieveCustomerAgreementWithAllFields() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setType("Standard Service Agreement");
            agreement.setAuthorName("MySQL Agreement Author");
            agreement.setCreatedDateTime(OffsetDateTime.now().minusDays(30));
            agreement.setLastModifiedDateTime(OffsetDateTime.now());
            agreement.setRevisionNumber("Rev-1.2");
            agreement.setSubject("MySQL Agreement Subject");
            agreement.setTitle("MySQL Service Agreement");
            agreement.setComment("MySQL test agreement");
            agreement.setSignDate(OffsetDateTime.now().minusDays(15));
            agreement.setLoadMgmt("LOAD-MGMT-001");
            agreement.setIsPrePay(true);
            agreement.setShutOffDateTime(OffsetDateTime.now().plusDays(365));
            agreement.setCurrency("USD");
            agreement.setAgreementId("MYSQL-AGR-12345");

            // Electronic address for document
            ElectronicAddress electronicAddress = new ElectronicAddress();
            electronicAddress.setEmail1("agreement@mysql.test");
            electronicAddress.setWeb("https://mysql-agreement.test");
            agreement.setElectronicAddress(electronicAddress);

            // Document status
            Status docStatus = new Status();
            docStatus.setValue("final");
            docStatus.setDateTime(OffsetDateTime.now());
            docStatus.setReason("MySQL document finalization");
            agreement.setDocStatus(docStatus);

            // Agreement status
            Status status = new Status();
            status.setValue("active");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("MySQL agreement activation");
            agreement.setStatus(status);

            // Validity interval
            DateTimeInterval validityInterval = new DateTimeInterval();
            validityInterval.setStart(OffsetDateTime.now().minusDays(10).toEpochSecond());
            validityInterval.setDuration(31536000L); // 1 year in seconds
            agreement.setValidityInterval(validityInterval);

            // Act
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAgreementEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo("Standard Service Agreement");
            assertThat(result.getAuthorName()).isEqualTo("MySQL Agreement Author");
            assertThat(result.getComment()).isEqualTo("MySQL test agreement");
            assertThat(result.getLoadMgmt()).isEqualTo("LOAD-MGMT-001");
            assertThat(result.getIsPrePay()).isTrue();
            assertThat(result.getCurrency()).isEqualTo("USD");
            assertThat(result.getAgreementId()).isEqualTo("MYSQL-AGR-12345");

            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("agreement@mysql.test");

            assertThat(result.getDocStatus()).isNotNull();
            assertThat(result.getDocStatus().getValue()).isEqualTo("final");

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("active");

            assertThat(result.getValidityInterval()).isNotNull();
            assertThat(result.getValidityInterval().getDuration()).isEqualTo(31536000L);
        }

        @Test
        @DisplayName("Should update customer agreement fields")
        void shouldUpdateCustomerAgreementFields() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("ORIGINAL-AGR-ID");
            agreement.setIsPrePay(false);
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();

            // Act
            savedAgreement.setAgreementId("UPDATED-AGR-ID");
            savedAgreement.setIsPrePay(true);
            savedAgreement.setCurrency("EUR");
            CustomerAgreementEntity updatedAgreement = customerAgreementRepository.save(savedAgreement);
            flushAndClear();

            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(updatedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAgreementId()).isEqualTo("UPDATED-AGR-ID");
            assertThat(retrieved.get().getIsPrePay()).isTrue();
            assertThat(retrieved.get().getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Should delete customer agreement")
        void shouldDeleteCustomerAgreement() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("DELETE-ME-AGR");
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();

            // Act
            customerAgreementRepository.deleteById(savedAgreement.getId());
            flushAndClear();

            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedAgreement.getId());

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
            List<CustomerAgreementEntity> agreements = TestDataBuilders.createValidEntities(5,
                TestDataBuilders::createValidCustomerAgreement);

            for (int i = 0; i < agreements.size(); i++) {
                agreements.get(i).setAgreementId("MYSQL-BULK-AGR-" + i);
            }

            // Act
            List<CustomerAgreementEntity> savedAgreements = customerAgreementRepository.saveAll(agreements);
            flushAndClear();

            // Assert
            assertThat(savedAgreements).hasSize(5);
            assertThat(savedAgreements).allMatch(agreement -> agreement.getId() != null);

            long count = customerAgreementRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should handle bulk delete operations")
        void shouldHandleBulkDeleteOperations() {
            // Arrange
            List<CustomerAgreementEntity> agreements = TestDataBuilders.createValidEntities(3,
                TestDataBuilders::createValidCustomerAgreement);

            List<CustomerAgreementEntity> savedAgreements = customerAgreementRepository.saveAll(agreements);
            long initialCount = customerAgreementRepository.count();
            flushAndClear();

            // Act
            customerAgreementRepository.deleteAll(savedAgreements);
            flushAndClear();

            // Assert
            long finalCount = customerAgreementRepository.count();
            assertThat(finalCount).isEqualTo(initialCount - 3);
        }
    }

    @Nested
    @DisplayName("Embedded Objects Persistence")
    class EmbeddedObjectsTest {

        @Test
        @DisplayName("Should persist DateTimeInterval with all fields")
        void shouldPersistDateTimeIntervalWithAllFields() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("MYSQL-INTERVAL-TEST");

            DateTimeInterval validityInterval = new DateTimeInterval();
            validityInterval.setStart(OffsetDateTime.now().minusYears(1).toEpochSecond());
            validityInterval.setDuration(63072000L); // 2 years in seconds
            agreement.setValidityInterval(validityInterval);

            // Act
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            DateTimeInterval retrievedInterval = retrieved.get().getValidityInterval();
            assertThat(retrievedInterval).isNotNull();
            assertThat(retrievedInterval.getDuration()).isEqualTo(63072000L);
            assertThat(retrievedInterval.getStart()).isNotNull();
        }

        @Test
        @DisplayName("Should persist both document and agreement status")
        void shouldPersistBothStatuses() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("MYSQL-STATUS-TEST");

            Status docStatus = new Status();
            docStatus.setValue("approved");
            docStatus.setDateTime(OffsetDateTime.now());
            docStatus.setRemark("MySQL doc remark");
            docStatus.setReason("MySQL doc reason");
            agreement.setDocStatus(docStatus);

            Status agreementStatus = new Status();
            agreementStatus.setValue("pending");
            agreementStatus.setDateTime(OffsetDateTime.now());
            agreementStatus.setRemark("MySQL agreement remark");
            agreementStatus.setReason("MySQL agreement reason");
            agreement.setStatus(agreementStatus);

            // Act
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Status retrievedDocStatus = retrieved.get().getDocStatus();
            assertThat(retrievedDocStatus).isNotNull();
            assertThat(retrievedDocStatus.getValue()).isEqualTo("approved");
            assertThat(retrievedDocStatus.getRemark()).isEqualTo("MySQL doc remark");

            Status retrievedAgreementStatus = retrieved.get().getStatus();
            assertThat(retrievedAgreementStatus).isNotNull();
            assertThat(retrievedAgreementStatus.getValue()).isEqualTo("pending");
            assertThat(retrievedAgreementStatus.getRemark()).isEqualTo("MySQL agreement remark");
        }

        @Test
        @DisplayName("Should persist future status collection")
        void shouldPersistFutureStatusCollection() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("MYSQL-FUTURE-STATUS-TEST");

            List<Status> futureStatuses = new ArrayList<>();

            Status futureStatus1 = new Status();
            futureStatus1.setValue("scheduled_renewal");
            futureStatus1.setDateTime(OffsetDateTime.now().plusMonths(6));
            futureStatus1.setReason("Scheduled renewal");
            futureStatuses.add(futureStatus1);

            Status futureStatus2 = new Status();
            futureStatus2.setValue("scheduled_termination");
            futureStatus2.setDateTime(OffsetDateTime.now().plusYears(1));
            futureStatus2.setReason("End of contract period");
            futureStatuses.add(futureStatus2);

            agreement.setFutureStatus(futureStatuses);

            // Act
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            List<Status> retrievedFutureStatuses = retrieved.get().getFutureStatus();
            assertThat(retrievedFutureStatuses).isNotNull();
            assertThat(retrievedFutureStatuses).hasSize(2);
            assertThat(retrievedFutureStatuses.get(0).getValue()).isEqualTo("scheduled_renewal");
            assertThat(retrievedFutureStatuses.get(1).getValue()).isEqualTo("scheduled_termination");
        }
    }
}