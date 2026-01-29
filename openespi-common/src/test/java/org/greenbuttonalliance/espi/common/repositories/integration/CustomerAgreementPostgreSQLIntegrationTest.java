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
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
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
 * CustomerAgreement entity integration tests using PostgreSQL TestContainer.
 *
 * Tests full CRUD operations and relationship persistence with a real PostgreSQL database.
 */
@DisplayName("CustomerAgreement Integration Tests - PostgreSQL")
@ActiveProfiles({"test", "test-postgresql"})
class CustomerAgreementPostgreSQLIntegrationTest extends BaseTestContainersTest {

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
    private CustomerAgreementRepository customerAgreementRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer agreement with all fields")
        void shouldSaveAndRetrieveCustomerAgreementWithAllFields() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setType("Premium Service Agreement");
            agreement.setAuthorName("PostgreSQL Agreement Author");
            agreement.setCreatedDateTime(OffsetDateTime.now().minusDays(45));
            agreement.setLastModifiedDateTime(OffsetDateTime.now());
            agreement.setRevisionNumber("Rev-2.1");
            agreement.setSubject("PostgreSQL Agreement Subject");
            agreement.setTitle("PostgreSQL Service Agreement");
            agreement.setComment("PostgreSQL test agreement");
            agreement.setSignDate(OffsetDateTime.now().minusDays(20));
            agreement.setLoadMgmt("LOAD-MGMT-002");
            agreement.setIsPrePay(false);
            agreement.setShutOffDateTime(OffsetDateTime.now().plusDays(730));
            agreement.setCurrency("CAD");
            agreement.setAgreementId("POSTGRES-AGR-67890");

            // Electronic address for document
            Organisation.ElectronicAddress electronicAddress = new Organisation.ElectronicAddress();
            electronicAddress.setEmail1("agreement@postgres.test");
            electronicAddress.setWeb("https://postgres-agreement.test");
            agreement.setElectronicAddress(electronicAddress);

            // Document status
            Status docStatus = new Status();
            docStatus.setValue("draft");
            docStatus.setDateTime(OffsetDateTime.now());
            docStatus.setReason("PostgreSQL document drafting");
            agreement.setDocStatus(docStatus);

            // Agreement status
            Status status = new Status();
            status.setValue("pending");
            status.setDateTime(OffsetDateTime.now());
            status.setReason("PostgreSQL agreement pending approval");
            agreement.setStatus(status);

            // Validity interval
            DateTimeInterval validityInterval = new DateTimeInterval();
            validityInterval.setStart(OffsetDateTime.now().minusDays(5).toEpochSecond());
            validityInterval.setDuration(15768000L); // 6 months in seconds
            agreement.setValidityInterval(validityInterval);

            // Act
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            CustomerAgreementEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo("Premium Service Agreement");
            assertThat(result.getAuthorName()).isEqualTo("PostgreSQL Agreement Author");
            assertThat(result.getComment()).isEqualTo("PostgreSQL test agreement");
            assertThat(result.getLoadMgmt()).isEqualTo("LOAD-MGMT-002");
            assertThat(result.getIsPrePay()).isFalse();
            assertThat(result.getCurrency()).isEqualTo("CAD");
            assertThat(result.getAgreementId()).isEqualTo("POSTGRES-AGR-67890");

            assertThat(result.getElectronicAddress()).isNotNull();
            assertThat(result.getElectronicAddress().getEmail1()).isEqualTo("agreement@postgres.test");

            assertThat(result.getDocStatus()).isNotNull();
            assertThat(result.getDocStatus().getValue()).isEqualTo("draft");

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().getValue()).isEqualTo("pending");

            assertThat(result.getValidityInterval()).isNotNull();
            assertThat(result.getValidityInterval().getDuration()).isEqualTo(15768000L);
        }

        @Test
        @DisplayName("Should update customer agreement fields")
        void shouldUpdateCustomerAgreementFields() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("ORIGINAL-PG-AGR-ID");
            agreement.setIsPrePay(true);
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();

            // Act
            savedAgreement.setAgreementId("UPDATED-PG-AGR-ID");
            savedAgreement.setIsPrePay(false);
            savedAgreement.setCurrency("GBP");
            CustomerAgreementEntity updatedAgreement = customerAgreementRepository.save(savedAgreement);
            flushAndClear();

            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(updatedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAgreementId()).isEqualTo("UPDATED-PG-AGR-ID");
            assertThat(retrieved.get().getIsPrePay()).isFalse();
            assertThat(retrieved.get().getCurrency()).isEqualTo("GBP");
        }

        @Test
        @DisplayName("Should delete customer agreement")
        void shouldDeleteCustomerAgreement() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("DELETE-ME-PG-AGR");
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
                agreements.get(i).setAgreementId("POSTGRES-BULK-AGR-" + i);
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
            agreement.setAgreementId("POSTGRES-INTERVAL-TEST");

            DateTimeInterval validityInterval = new DateTimeInterval();
            validityInterval.setStart(OffsetDateTime.now().minusMonths(3).toEpochSecond());
            validityInterval.setDuration(94608000L); // 3 years in seconds
            agreement.setValidityInterval(validityInterval);

            // Act
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            DateTimeInterval retrievedInterval = retrieved.get().getValidityInterval();
            assertThat(retrievedInterval).isNotNull();
            assertThat(retrievedInterval.getDuration()).isEqualTo(94608000L);
            assertThat(retrievedInterval.getStart()).isNotNull();
        }

        @Test
        @DisplayName("Should persist both document and agreement status")
        void shouldPersistBothStatuses() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("POSTGRES-STATUS-TEST");

            Status docStatus = new Status();
            docStatus.setValue("published");
            docStatus.setDateTime(OffsetDateTime.now());
            docStatus.setRemark("PostgreSQL doc remark");
            docStatus.setReason("PostgreSQL doc reason");
            agreement.setDocStatus(docStatus);

            Status agreementStatus = new Status();
            agreementStatus.setValue("active");
            agreementStatus.setDateTime(OffsetDateTime.now());
            agreementStatus.setRemark("PostgreSQL agreement remark");
            agreementStatus.setReason("PostgreSQL agreement reason");
            agreement.setStatus(agreementStatus);

            // Act
            CustomerAgreementEntity savedAgreement = customerAgreementRepository.save(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedAgreement.getId());

            // Assert
            assertThat(retrieved).isPresent();
            Status retrievedDocStatus = retrieved.get().getDocStatus();
            assertThat(retrievedDocStatus).isNotNull();
            assertThat(retrievedDocStatus.getValue()).isEqualTo("published");
            assertThat(retrievedDocStatus.getRemark()).isEqualTo("PostgreSQL doc remark");

            Status retrievedAgreementStatus = retrieved.get().getStatus();
            assertThat(retrievedAgreementStatus).isNotNull();
            assertThat(retrievedAgreementStatus.getValue()).isEqualTo("active");
            assertThat(retrievedAgreementStatus.getRemark()).isEqualTo("PostgreSQL agreement remark");
        }

        @Test
        @DisplayName("Should persist future status collection")
        void shouldPersistFutureStatusCollection() {
            // Arrange
            CustomerAgreementEntity agreement = TestDataBuilders.createValidCustomerAgreement();
            agreement.setAgreementId("POSTGRES-FUTURE-STATUS-TEST");

            List<Status> futureStatuses = new ArrayList<>();

            Status futureStatus1 = new Status();
            futureStatus1.setValue("scheduled_update");
            futureStatus1.setDateTime(OffsetDateTime.now().plusMonths(3));
            futureStatus1.setReason("Quarterly review");
            futureStatuses.add(futureStatus1);

            Status futureStatus2 = new Status();
            futureStatus2.setValue("scheduled_expiration");
            futureStatus2.setDateTime(OffsetDateTime.now().plusMonths(18));
            futureStatus2.setReason("Contract expiration");
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
            assertThat(retrievedFutureStatuses.get(0).getValue()).isEqualTo("scheduled_update");
            assertThat(retrievedFutureStatuses.get(1).getValue()).isEqualTo("scheduled_expiration");
        }
    }
}