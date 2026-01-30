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

import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;
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
 * Comprehensive test suite for CustomerAgreementRepository.
 * Phase 24: CustomerAgreement schema compliance testing.
 *
 * Tests CRUD operations, Document field persistence, Agreement field persistence,
 * CustomerAgreement field persistence, Status embedded objects, and IdentifiedObject base functionality.
 */
@DisplayName("CustomerAgreement Repository Tests")
class CustomerAgreementRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CustomerAgreementRepository customerAgreementRepository;

    /**
     * Creates a valid CustomerAgreementEntity with all Document, Agreement, and CustomerAgreement fields for testing.
     * Phase 24: Includes all fields per customer.xsd (Document + Agreement + CustomerAgreement).
     */
    private CustomerAgreementEntity createValidCustomerAgreement() {
        CustomerAgreementEntity agreement = new CustomerAgreementEntity();

        // IdentifiedObject fields
        agreement.setDescription("Test Customer Agreement - " + faker.lorem().sentence(3));

        // Document fields (customer.xsd lines 819-885)
        agreement.setType("SERVICE_AGREEMENT");
        agreement.setAuthorName(faker.name().fullName());
        agreement.setCreatedDateTime(randomOffsetDateTime());
        agreement.setLastModifiedDateTime(randomOffsetDateTime());
        agreement.setRevisionNumber("1.0");

        // Document.electronicAddress (customer.xsd lines 886-936)
        ElectronicAddress electronicAddress = new ElectronicAddress();
        electronicAddress.setLan("192.168." + faker.number().numberBetween(1, 255) + "." + faker.number().numberBetween(1, 255));
        electronicAddress.setMac(faker.internet().macAddress());
        electronicAddress.setEmail1(faker.internet().emailAddress());
        electronicAddress.setEmail2(faker.internet().emailAddress());
        electronicAddress.setWeb(faker.internet().url());
        electronicAddress.setRadio("RADIO-" + faker.number().digits(6));
        electronicAddress.setUserID(faker.name().username());
        electronicAddress.setPassword(faker.internet().password());
        agreement.setElectronicAddress(electronicAddress);

        agreement.setSubject("Service Agreement");
        agreement.setTitle("Customer Agreement AGR-" + faker.number().digits(6));

        // Document.docStatus (customer.xsd lines 1254-1284)
        Status docStatus = new Status();
        docStatus.setValue("FINALIZED");
        docStatus.setDateTime(randomOffsetDateTime());
        docStatus.setRemark("Document approved");
        docStatus.setReason("Signature completed");
        agreement.setDocStatus(docStatus);

        // Document.status (customer.xsd lines 1254-1284)
        Status status = new Status();
        status.setValue("ACTIVE");
        status.setDateTime(randomOffsetDateTime());
        status.setRemark("Service active");
        status.setReason("Agreement in good standing");
        agreement.setStatus(status);

        agreement.setComment("Standard terms and conditions apply");

        // Agreement fields (customer.xsd lines 622-642)
        agreement.setSignDate(randomOffsetDateTime());
        DateTimeInterval validityInterval = new DateTimeInterval();
        validityInterval.setStart(randomOffsetDateTime().toEpochSecond());
        validityInterval.setDuration(31536000L); // 1 year in seconds
        agreement.setValidityInterval(validityInterval);

        // CustomerAgreement specific fields (customer.xsd lines 159-209)
        agreement.setLoadMgmt("STANDARD");
        agreement.setIsPrePay(false);
        agreement.setShutOffDateTime(null);
        agreement.setCurrency("USD");
        agreement.setAgreementId("AGR-" + faker.number().digits(8));

        return agreement;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve customer agreement successfully")
        void shouldSaveAndRetrieveCustomerAgreementSuccessfully() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getId()).isEqualTo(saved.getId());
            assertThat(retrieved.get().getAgreementId()).isEqualTo(saved.getAgreementId());
            assertThat(retrieved.get().getLoadMgmt()).isEqualTo(saved.getLoadMgmt());
        }

        @Test
        @DisplayName("Should update customer agreement successfully")
        void shouldUpdateCustomerAgreementSuccessfully() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            UUID savedId = saved.getId();

            // Act
            saved.setLoadMgmt("PREMIUM");
            saved.setCurrency("EUR");
            saved.setComment("Updated terms");
            customerAgreementRepository.save(saved);
            flushAndClear();

            // Assert
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedId);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getLoadMgmt()).isEqualTo("PREMIUM");
            assertThat(retrieved.get().getCurrency()).isEqualTo("EUR");
            assertThat(retrieved.get().getComment()).isEqualTo("Updated terms");
        }

        @Test
        @DisplayName("Should delete customer agreement successfully")
        void shouldDeleteCustomerAgreementSuccessfully() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            UUID savedId = saved.getId();

            // Act
            customerAgreementRepository.deleteById(savedId);
            flushAndClear();

            // Assert
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(savedId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should count customer agreements")
        void shouldCountCustomerAgreements() {
            // Arrange
            CustomerAgreementEntity agreement1 = createValidCustomerAgreement();
            CustomerAgreementEntity agreement2 = createValidCustomerAgreement();

            // Act
            long countBefore = customerAgreementRepository.count();
            persistAndFlush(agreement1);
            persistAndFlush(agreement2);
            long countAfter = customerAgreementRepository.count();

            // Assert
            assertThat(countAfter).isEqualTo(countBefore + 2);
        }

        @Test
        @DisplayName("Should find all customer agreements")
        void shouldFindAllCustomerAgreements() {
            // Arrange
            CustomerAgreementEntity agreement1 = createValidCustomerAgreement();
            CustomerAgreementEntity agreement2 = createValidCustomerAgreement();
            persistAndFlush(agreement1);
            persistAndFlush(agreement2);

            // Act
            List<CustomerAgreementEntity> allAgreements = customerAgreementRepository.findAll();

            // Assert
            assertThat(allAgreements).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should verify customer agreement exists by ID")
        void shouldVerifyCustomerAgreementExistsByID() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            CustomerAgreementEntity saved = persistAndFlush(agreement);

            // Act
            boolean exists = customerAgreementRepository.existsById(saved.getId());

            // Assert
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("Document Field Persistence")
    class DocumentFieldPersistenceTest {

        @Test
        @DisplayName("Should persist all Document fields correctly")
        void shouldPersistAllDocumentFieldsCorrectly() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert - Document fields
            assertThat(retrieved).isPresent();
            CustomerAgreementEntity result = retrieved.get();

            assertThat(result.getType()).isEqualTo(agreement.getType());
            assertThat(result.getAuthorName()).isEqualTo(agreement.getAuthorName());
            assertThat(result.getCreatedDateTime()).isEqualTo(agreement.getCreatedDateTime());
            assertThat(result.getLastModifiedDateTime()).isEqualTo(agreement.getLastModifiedDateTime());
            assertThat(result.getRevisionNumber()).isEqualTo(agreement.getRevisionNumber());
            assertThat(result.getSubject()).isEqualTo(agreement.getSubject());
            assertThat(result.getTitle()).isEqualTo(agreement.getTitle());
            assertThat(result.getComment()).isEqualTo(agreement.getComment());
        }

        @Test
        @DisplayName("Should persist ElectronicAddress with all 8 fields")
        void shouldPersistElectronicAddressWithAllFields() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert - ElectronicAddress fields (customer.xsd lines 886-936)
            assertThat(retrieved).isPresent();
            ElectronicAddress result = retrieved.get().getElectronicAddress();

            assertThat(result).isNotNull();
            assertThat(result.getLan()).isEqualTo(agreement.getElectronicAddress().getLan());
            assertThat(result.getMac()).isEqualTo(agreement.getElectronicAddress().getMac());
            assertThat(result.getEmail1()).isEqualTo(agreement.getElectronicAddress().getEmail1());
            assertThat(result.getEmail2()).isEqualTo(agreement.getElectronicAddress().getEmail2());
            assertThat(result.getWeb()).isEqualTo(agreement.getElectronicAddress().getWeb());
            assertThat(result.getRadio()).isEqualTo(agreement.getElectronicAddress().getRadio());
            assertThat(result.getUserID()).isEqualTo(agreement.getElectronicAddress().getUserID());
            assertThat(result.getPassword()).isEqualTo(agreement.getElectronicAddress().getPassword());
        }

        @Test
        @DisplayName("Should persist docStatus with all 4 fields including remark")
        void shouldPersistDocStatusWithAllFields() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert - docStatus fields (customer.xsd lines 1254-1284)
            assertThat(retrieved).isPresent();
            Status result = retrieved.get().getDocStatus();

            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo(agreement.getDocStatus().getValue());
            assertThat(result.getDateTime()).isEqualTo(agreement.getDocStatus().getDateTime());
            assertThat(result.getRemark()).isEqualTo(agreement.getDocStatus().getRemark());
            assertThat(result.getReason()).isEqualTo(agreement.getDocStatus().getReason());
        }

        @Test
        @DisplayName("Should persist status with all 4 fields including remark")
        void shouldPersistStatusWithAllFields() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert - status fields (customer.xsd lines 1254-1284)
            assertThat(retrieved).isPresent();
            Status result = retrieved.get().getStatus();

            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo(agreement.getStatus().getValue());
            assertThat(result.getDateTime()).isEqualTo(agreement.getStatus().getDateTime());
            assertThat(result.getRemark()).isEqualTo(agreement.getStatus().getRemark());
            assertThat(result.getReason()).isEqualTo(agreement.getStatus().getReason());
        }
    }

    @Nested
    @DisplayName("Agreement Field Persistence")
    class AgreementFieldPersistenceTest {

        @Test
        @DisplayName("Should persist signDate correctly")
        void shouldPersistSignDateCorrectly() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            OffsetDateTime signDate = randomOffsetDateTime();
            agreement.setSignDate(signDate);

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getSignDate()).isEqualTo(signDate);
        }

        @Test
        @DisplayName("Should persist validityInterval correctly")
        void shouldPersistValidityIntervalCorrectly() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            DateTimeInterval validityInterval = new DateTimeInterval();
            OffsetDateTime start = randomOffsetDateTime();
            validityInterval.setStart(start.toEpochSecond());
            validityInterval.setDuration(86400L); // 1 day
            agreement.setValidityInterval(validityInterval);

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getValidityInterval()).isNotNull();
            assertThat(retrieved.get().getValidityInterval().getStart()).isEqualTo(validityInterval.getStart());
            assertThat(retrieved.get().getValidityInterval().getDuration()).isEqualTo(validityInterval.getDuration());
        }
    }

    @Nested
    @DisplayName("CustomerAgreement Field Persistence")
    class CustomerAgreementFieldPersistenceTest {

        @Test
        @DisplayName("Should persist loadMgmt correctly")
        void shouldPersistLoadMgmtCorrectly() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            agreement.setLoadMgmt("PREMIUM");

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getLoadMgmt()).isEqualTo("PREMIUM");
        }

        @Test
        @DisplayName("Should persist isPrePay correctly")
        void shouldPersistIsPrePayCorrectly() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            agreement.setIsPrePay(true);

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getIsPrePay()).isTrue();
        }

        @Test
        @DisplayName("Should persist shutOffDateTime correctly")
        void shouldPersistShutOffDateTimeCorrectly() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            OffsetDateTime shutOffDateTime = randomOffsetDateTime();
            agreement.setShutOffDateTime(shutOffDateTime);

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getShutOffDateTime()).isEqualTo(shutOffDateTime);
        }

        @Test
        @DisplayName("Should persist currency correctly")
        void shouldPersistCurrencyCorrectly() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            agreement.setCurrency("EUR");

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Should persist agreementId correctly")
        void shouldPersistAgreementIdCorrectly() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            String agreementId = "AGR-TEST-" + faker.number().digits(10);
            agreement.setAgreementId(agreementId);

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAgreementId()).isEqualTo(agreementId);
        }
    }

    @Nested
    @DisplayName("IdentifiedObject Base Class Tests")
    class BaseClassTest {

        @Test
        @DisplayName("Should persist IdentifiedObject description field")
        void shouldPersistIdentifiedObjectDescriptionField() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            String description = "Custom Description - " + faker.lorem().sentence(5);
            agreement.setDescription(description);

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo(description);
        }

        @Test
        @DisplayName("Should auto-generate UUID on persist")
        void shouldAutoGenerateUuidOnPersist() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getId()).isInstanceOf(UUID.class);
        }

        @Test
        @DisplayName("Should persist created and updated timestamps")
        void shouldPersistCreatedAndUpdatedTimestamps() {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();

            // Act
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getCreated()).isNotNull();
            assertThat(retrieved.get().getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should update timestamp on modification")
        void shouldUpdateTimestampOnModification() throws InterruptedException {
            // Arrange
            CustomerAgreementEntity agreement = createValidCustomerAgreement();
            CustomerAgreementEntity saved = persistAndFlush(agreement);
            java.time.LocalDateTime initialUpdated = saved.getUpdated();

            // Wait to ensure timestamp difference
            Thread.sleep(100);

            // Act
            saved.setComment("Modified comment");
            customerAgreementRepository.save(saved);
            flushAndClear();
            Optional<CustomerAgreementEntity> retrieved = customerAgreementRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getUpdated()).isAfter(initialUpdated);
        }
    }
}
