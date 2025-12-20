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
import org.greenbuttonalliance.espi.common.domain.customer.entity.StatementEntity;
import org.greenbuttonalliance.espi.common.domain.usage.*;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.customer.StatementRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.*;
import org.greenbuttonalliance.espi.common.test.BaseTestContainersTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Complex relationship integration tests for JPA entities using MySQL TestContainer.
 *
 * Tests basic relationship operations and entity hierarchies using
 * available repositories and methods with a real MySQL database.
 */
@DisplayName("Complex Relationship Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class ComplexRelationshipMySQLIntegrationTest extends BaseTestContainersTest {

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

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private IntervalBlockRepository intervalBlockRepository;

    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    @Nested
    @DisplayName("Basic Relationship Operations")
    class BasicRelationshipTest {

        @Test
        @DisplayName("Should handle Customer → Statement relationships")
        void shouldHandleCustomerStatementRelationships() {
            // Arrange
            CustomerEntity customer = TestDataBuilders.createValidCustomer();
            customer.setCustomerName("MySQL Relationship Test Customer");
            CustomerEntity savedCustomer = customerRepository.save(customer);

            StatementEntity statement = TestDataBuilders.createValidStatement();
            statement.setCustomer(savedCustomer);
            statement.setDescription("MySQL Relationship Test Statement");
            StatementEntity savedStatement = statementRepository.save(statement);

            flushAndClear();

            // Act - Retrieve customer and statement
            Optional<CustomerEntity> retrievedCustomer = customerRepository.findById(savedCustomer.getId());
            Optional<StatementEntity> retrievedStatement = statementRepository.findById(savedStatement.getId());

            // Assert
            assertThat(retrievedCustomer).isPresent();
            assertThat(retrievedStatement).isPresent();
            assertThat(retrievedStatement.get().getCustomer()).isNotNull();
            assertThat(retrievedStatement.get().getCustomer().getId()).isEqualTo(savedCustomer.getId());
        }

        @Test
        @DisplayName("Should handle UsagePoint → MeterReading → IntervalBlock hierarchy")
        void shouldHandleUsagePointHierarchy() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("MySQL Hierarchy Test Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setUsagePoint(savedUsagePoint);
            meterReading.setDescription("MySQL Hierarchy Test Meter Reading");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);

            IntervalBlockEntity intervalBlock = TestDataBuilders.createValidIntervalBlock();
            intervalBlock.setMeterReading(savedMeterReading);
            intervalBlock.setDescription("MySQL Hierarchy Test Interval Block");
            IntervalBlockEntity savedIntervalBlock = intervalBlockRepository.save(intervalBlock);

            flushAndClear();

            // Act - Retrieve the hierarchy
            Optional<UsagePointEntity> retrievedUsagePoint = usagePointRepository.findById(savedUsagePoint.getId());
            Optional<MeterReadingEntity> retrievedMeterReading = meterReadingRepository.findById(savedMeterReading.getId());
            Optional<IntervalBlockEntity> retrievedIntervalBlock = intervalBlockRepository.findById(savedIntervalBlock.getId());

            // Assert
            assertThat(retrievedUsagePoint).isPresent();
            assertThat(retrievedMeterReading).isPresent();
            assertThat(retrievedIntervalBlock).isPresent();

            assertThat(retrievedMeterReading.get().getUsagePoint()).isNotNull();
            assertThat(retrievedMeterReading.get().getUsagePoint().getId()).isEqualTo(savedUsagePoint.getId());

            assertThat(retrievedIntervalBlock.get().getMeterReading()).isNotNull();
            assertThat(retrievedIntervalBlock.get().getMeterReading().getId()).isEqualTo(savedMeterReading.getId());
        }

        @Test
        @DisplayName("Should handle RetailCustomer → UsagePoint relationships")
        void shouldHandleRetailCustomerUsagePointRelationships() {
            // Arrange
            RetailCustomerEntity retailCustomer = TestDataBuilders.createValidRetailCustomer();
            retailCustomer.setUsername("mysql.test@example.com");
            RetailCustomerEntity savedRetailCustomer = retailCustomerRepository.save(retailCustomer);

            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setRetailCustomer(savedRetailCustomer);
            usagePoint.setDescription("MySQL Relationship Test Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            flushAndClear();

            // Act - Retrieve retail customer and usage point
            Optional<RetailCustomerEntity> retrievedCustomer = retailCustomerRepository.findById(savedRetailCustomer.getId());
            Optional<UsagePointEntity> retrievedUsagePoint = usagePointRepository.findById(savedUsagePoint.getId());

            // Assert
            assertThat(retrievedCustomer).isPresent();
            assertThat(retrievedUsagePoint).isPresent();
            assertThat(retrievedUsagePoint.get().getRetailCustomer()).isNotNull();
            assertThat(retrievedUsagePoint.get().getRetailCustomer().getUsername()).isEqualTo("mysql.test@example.com");
        }
    }

    @Nested
    @DisplayName("Transaction Boundary Scenarios")
    class TransactionBoundaryTest {

        @Test
        @DisplayName("Should maintain data consistency across transaction boundaries")
        @Transactional
        void shouldMaintainDataConsistency() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("MySQL Transaction Test");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setUsagePoint(savedUsagePoint);
            meterReading.setDescription("MySQL Transaction Test Reading");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);

            // Act - Modify within same transaction
            savedUsagePoint.setDescription("MySQL Modified in Transaction");
            savedMeterReading.setDescription("MySQL Modified Reading in Transaction");

            usagePointRepository.save(savedUsagePoint);
            meterReadingRepository.save(savedMeterReading);

            // Assert - Changes should be visible within transaction
            UsagePointEntity retrievedUsagePoint = usagePointRepository.findById(savedUsagePoint.getId()).orElse(null);
            assertThat(retrievedUsagePoint).isNotNull();
            assertThat(retrievedUsagePoint.getDescription()).isEqualTo("MySQL Modified in Transaction");

            MeterReadingEntity retrievedMeterReading = meterReadingRepository.findById(savedMeterReading.getId()).orElse(null);
            assertThat(retrievedMeterReading).isNotNull();
            assertThat(retrievedMeterReading.getDescription()).isEqualTo("MySQL Modified Reading in Transaction");
        }
    }

    @Nested
    @DisplayName("Bulk Operation Integrity")
    class BulkOperationTest {

        @Test
        @DisplayName("Should handle bulk save operations correctly")
        void shouldHandleBulkSaveOperations() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("MySQL Bulk Test Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            List<MeterReadingEntity> meterReadings = TestDataBuilders.createValidEntities(5,
                () -> {
                    MeterReadingEntity reading = TestDataBuilders.createValidMeterReading();
                    reading.setUsagePoint(savedUsagePoint);
                    return reading;
                });

            // Act - Bulk save
            List<MeterReadingEntity> savedReadings = meterReadingRepository.saveAll(meterReadings);
            flushAndClear();

            // Assert
            assertThat(savedReadings).hasSize(5);
            assertThat(savedReadings).allMatch(reading -> reading.getId() != null);

            // Verify all readings were saved
            long count = meterReadingRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should handle bulk delete operations correctly")
        void shouldHandleBulkDeleteOperations() {
            // Arrange
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            List<MeterReadingEntity> meterReadings = TestDataBuilders.createValidEntities(3,
                () -> {
                    MeterReadingEntity reading = TestDataBuilders.createValidMeterReading();
                    reading.setUsagePoint(savedUsagePoint);
                    return reading;
                });

            List<MeterReadingEntity> savedReadings = meterReadingRepository.saveAll(meterReadings);
            long initialCount = meterReadingRepository.count();
            flushAndClear();

            // Act - Bulk delete
            meterReadingRepository.deleteAll(savedReadings);
            flushAndClear();

            // Assert
            long finalCount = meterReadingRepository.count();
            assertThat(finalCount).isEqualTo(initialCount - 3);
        }
    }
}