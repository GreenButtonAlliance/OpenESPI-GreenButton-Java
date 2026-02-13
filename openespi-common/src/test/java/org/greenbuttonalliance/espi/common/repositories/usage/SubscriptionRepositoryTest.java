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

package org.greenbuttonalliance.espi.common.repositories.usage;

import jakarta.validation.ConstraintViolation;
import org.greenbuttonalliance.espi.common.domain.common.enums.GrantType;
import org.greenbuttonalliance.espi.common.domain.usage.*;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for SubscriptionRepository.
 *
 * Tests subscription lifecycle management, all custom query methods,
 * relationship testing, and validation constraints.
 *
 * Note: Subscription is an application-specific entity (NOT an ESPI resource),
 * so it does not extend IdentifiedObject and has no description, created,
 * updated, or lastUpdate fields. The UUID must be set before persisting.
 */
@DisplayName("Subscription Repository Tests")
class SubscriptionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    @Autowired
    private ApplicationInformationRepository applicationInformationRepository;

    @Autowired
    private AuthorizationRepository authorizationRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    /**
     * Creates a valid SubscriptionEntity for testing.
     * Sets UUID since it's required before persisting.
     */
    private SubscriptionEntity createValidSubscription() {
        SubscriptionEntity subscription = new SubscriptionEntity(UUID.randomUUID());
        subscription.setHashedId("hashed-" + faker.internet().uuid());
        return subscription;
    }

    /**
     * Creates a valid ApplicationInformationEntity for testing.
     */
    private ApplicationInformationEntity createValidApplicationInformation() {
        ApplicationInformationEntity app = new ApplicationInformationEntity();
        app.setDescription("Test Application Information");

        // Ensure clientId meets validation constraints (2-64 chars, @NotEmpty) and is unique
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String clientId = "test-client-" + uniqueId;
        if (clientId.length() > 64) {
            clientId = clientId.substring(0, 64);
        }
        app.setClientId(clientId);

        app.setClientSecret(faker.internet().password());

        // Ensure dataCustodianId meets validation constraints (2-64 chars if present)
        String dataCustodianId = "test-datacustodian-" + faker.number().digits(6);
        if (dataCustodianId.length() > 64) {
            dataCustodianId = dataCustodianId.substring(0, 64);
        }
        app.setDataCustodianId(dataCustodianId);

        Set<GrantType> grantTypes = new HashSet<>();
        grantTypes.add(GrantType.AUTHORIZATION_CODE);
        app.setGrantTypes(grantTypes);

        return app;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve subscription successfully")
        void shouldSaveAndRetrieveSubscriptionSuccessfully() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("sub" + UUID.randomUUID().toString().substring(0, 8));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);

            // Act
            SubscriptionEntity saved = subscriptionRepository.save(subscription);
            flushAndClear();
            Optional<SubscriptionEntity> retrieved = subscriptionRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getRetailCustomer().getId()).isEqualTo(savedCustomer.getId());
            assertThat(retrieved.get().getApplicationInformation().getId()).isEqualTo(savedApp.getId());
        }

        @Test
        @DisplayName("Should save subscription with hashed ID")
        void shouldSaveSubscriptionWithHashedId() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("life" + UUID.randomUUID().toString().substring(0, 8));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);
            String expectedHashedId = "test-hashed-id-" + faker.number().digits(8);
            subscription.setHashedId(expectedHashedId);

            // Act
            SubscriptionEntity saved = subscriptionRepository.save(subscription);
            flushAndClear();
            Optional<SubscriptionEntity> retrieved = subscriptionRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            SubscriptionEntity entity = retrieved.get();
            assertThat(entity.getHashedId()).isEqualTo(expectedHashedId);
        }

        @Test
        @DisplayName("Should find all subscriptions")
        void shouldFindAllSubscriptions() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("findall" + UUID.randomUUID().toString().substring(0, 8));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            List<SubscriptionEntity> subscriptions = List.of(
                createValidSubscription(),
                createValidSubscription(),
                createValidSubscription()
            );

            for (SubscriptionEntity sub : subscriptions) {
                sub.setRetailCustomer(savedCustomer);
                sub.setApplicationInformation(savedApp);
            }

            subscriptionRepository.saveAll(subscriptions);
            flushAndClear();

            // Act
            List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findAll();

            // Assert
            assertThat(allSubscriptions).hasSizeGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Should delete subscription successfully")
        void shouldDeleteSubscriptionSuccessfully() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("delcust" + faker.number().digits(4));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);

            SubscriptionEntity saved = subscriptionRepository.save(subscription);
            UUID subscriptionId = saved.getId();
            flushAndClear();

            // Act
            subscriptionRepository.deleteById(subscriptionId);
            flushAndClear();
            Optional<SubscriptionEntity> retrieved = subscriptionRepository.findById(subscriptionId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if subscription exists")
        void shouldCheckIfSubscriptionExists() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("exists" + faker.number().digits(4));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);

            SubscriptionEntity saved = subscriptionRepository.save(subscription);
            flushAndClear();

            // Act & Assert
            assertThat(subscriptionRepository.existsById(saved.getId())).isTrue();
            assertThat(subscriptionRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count subscriptions")
        void shouldCountSubscriptions() {
            // Arrange
            long initialCount = subscriptionRepository.count();

            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("count" + UUID.randomUUID().toString().substring(0, 8));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            List<SubscriptionEntity> subscriptions = List.of(
                createValidSubscription(),
                createValidSubscription()
            );

            subscriptions.forEach(sub -> {
                sub.setRetailCustomer(savedCustomer);
                sub.setApplicationInformation(savedApp);
            });

            subscriptionRepository.saveAll(subscriptions);
            flushAndClear();

            // Act
            long finalCount = subscriptionRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find subscription by hashed ID")
        void shouldFindSubscriptionByHashedId() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("hashed" + faker.number().digits(4));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity subscription = createValidSubscription();
            String hashedId = "unique-hashed-" + faker.internet().uuid();
            subscription.setHashedId(hashedId);
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);

            subscriptionRepository.save(subscription);
            flushAndClear();

            // Act
            Optional<SubscriptionEntity> result = subscriptionRepository.findByHashedId(hashedId);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getHashedId()).isEqualTo(hashedId);
        }

        @Test
        @DisplayName("Should find subscription by authorization ID")
        void shouldFindSubscriptionByAuthorizationId() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("auth" + faker.number().digits(6));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            AuthorizationEntity auth = new AuthorizationEntity();
            auth.setDescription("Test Authorization for Subscription");
            auth.setAccessToken("test-token-" + faker.internet().uuid());
            auth.setStatus(AuthorizationEntity.STATUS_ACTIVE);
            AuthorizationEntity savedAuth = authorizationRepository.save(auth);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);
            subscription.setAuthorization(savedAuth);

            subscriptionRepository.save(subscription);
            flushAndClear();

            // Act
            Optional<SubscriptionEntity> result = subscriptionRepository.findByAuthorization_Id(savedAuth.getId());

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getAuthorization().getId()).isEqualTo(savedAuth.getId());
        }

        @Test
        @DisplayName("Should find subscriptions by retail customer ID")
        void shouldFindSubscriptionsByRetailCustomerId() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("custsubs" + faker.number().digits(4));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity sub1 = createValidSubscription();
            sub1.setRetailCustomer(savedCustomer);
            sub1.setApplicationInformation(savedApp);

            SubscriptionEntity sub2 = createValidSubscription();
            sub2.setRetailCustomer(savedCustomer);
            sub2.setApplicationInformation(savedApp);

            subscriptionRepository.saveAll(List.of(sub1, sub2));
            flushAndClear();

            // Act
            List<SubscriptionEntity> results = subscriptionRepository.findByRetailCustomer_Id(savedCustomer.getId());

            // Assert
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should find subscriptions by application information ID")
        void shouldFindSubscriptionsByApplicationInformationId() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("appsubs" + faker.number().digits(4));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setDescription("Test Application for Subscriptions");
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity sub1 = createValidSubscription();
            sub1.setRetailCustomer(savedCustomer);
            sub1.setApplicationInformation(savedApp);

            SubscriptionEntity sub2 = createValidSubscription();
            sub2.setRetailCustomer(savedCustomer);
            sub2.setApplicationInformation(savedApp);

            subscriptionRepository.saveAll(List.of(sub1, sub2));
            flushAndClear();

            // Act
            List<SubscriptionEntity> results = subscriptionRepository.findByApplicationInformation_Id(savedApp.getId());

            // Assert
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Act & Assert
            assertThat(subscriptionRepository.findByHashedId("nonexistent-hash")).isEmpty();
            assertThat(subscriptionRepository.findByAuthorization_Id(UUID.randomUUID())).isEmpty();
            assertThat(subscriptionRepository.findByRetailCustomer_Id(999999L)).isEmpty();
            assertThat(subscriptionRepository.findByApplicationInformation_Id(UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Relationship Testing")
    class RelationshipTest {

        @Test
        @DisplayName("Should create subscription with all relationships")
        void shouldCreateSubscriptionWithAllRelationships() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("fullrel" + faker.number().digits(5));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            AuthorizationEntity auth = new AuthorizationEntity();
            auth.setDescription("Full Relationship Authorization");
            auth.setAccessToken("full-token-" + faker.internet().uuid());
            auth.setStatus(AuthorizationEntity.STATUS_ACTIVE);
            AuthorizationEntity savedAuth = authorizationRepository.save(auth);

            UsagePointEntity usagePoint1 = TestDataBuilders.createValidUsagePoint();
            usagePoint1.setDescription("Usage Point 1");
            UsagePointEntity savedUsagePoint1 = usagePointRepository.save(usagePoint1);

            UsagePointEntity usagePoint2 = TestDataBuilders.createValidUsagePoint();
            usagePoint2.setDescription("Usage Point 2");
            UsagePointEntity savedUsagePoint2 = usagePointRepository.save(usagePoint2);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);
            subscription.setAuthorization(savedAuth);
            subscription.getUsagePoints().addAll(List.of(savedUsagePoint1, savedUsagePoint2));

            // Act
            SubscriptionEntity saved = subscriptionRepository.save(subscription);
            flushAndClear();
            Optional<SubscriptionEntity> retrieved = subscriptionRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            SubscriptionEntity entity = retrieved.get();
            assertThat(entity.getRetailCustomer().getId()).isEqualTo(savedCustomer.getId());
            assertThat(entity.getApplicationInformation().getId()).isEqualTo(savedApp.getId());
            assertThat(entity.getAuthorization().getId()).isEqualTo(savedAuth.getId());
            assertThat(entity.getUsagePoints()).hasSize(2);
            assertThat(entity.getUsagePoints()).extracting(UsagePointEntity::getDescription)
                    .contains("Usage Point 1", "Usage Point 2");
        }

        @Test
        @DisplayName("Should handle subscription without optional relationships")
        void shouldHandleSubscriptionWithoutOptionalRelationships() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("minrel" + faker.number().digits(6));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);
            // Leave authorization and usagePoints as null/empty

            // Act
            SubscriptionEntity saved = subscriptionRepository.save(subscription);
            flushAndClear();
            Optional<SubscriptionEntity> retrieved = subscriptionRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            SubscriptionEntity entity = retrieved.get();
            assertThat(entity.getRetailCustomer().getId()).isEqualTo(savedCustomer.getId());
            assertThat(entity.getApplicationInformation().getId()).isEqualTo(savedApp.getId());
            assertThat(entity.getAuthorization()).isNull();
            assertThat(entity.getUsagePoints()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate subscription with valid data")
        void shouldValidateSubscriptionWithValidData() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            ApplicationInformationEntity app = createValidApplicationInformation();

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(customer);
            subscription.setApplicationInformation(app);

            // Act
            Set<ConstraintViolation<SubscriptionEntity>> violations = validator.validate(subscription);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should validate required retail customer")
        void shouldValidateRequiredRetailCustomer() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(null); // Missing required field
            subscription.setApplicationInformation(app);

            // Act
            Set<ConstraintViolation<SubscriptionEntity>> violations = validator.validate(subscription);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                    .extracting(Object::toString)
                    .contains("retailCustomer");
        }

        @Test
        @DisplayName("Should validate required application information")
        void shouldValidateRequiredApplicationInformation() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(customer);
            subscription.setApplicationInformation(null); // Missing required field

            // Act
            Set<ConstraintViolation<SubscriptionEntity>> violations = validator.validate(subscription);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                    .extracting(Object::toString)
                    .contains("applicationInformation");
        }
    }

    @Nested
    @DisplayName("Entity Functionality")
    class EntityFunctionalityTest {

        @Test
        @DisplayName("Should persist with pre-set UUID")
        void shouldPersistWithPreSetUuid() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("base" + faker.number().digits(7));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            UUID presetId = UUID.randomUUID();
            SubscriptionEntity subscription = new SubscriptionEntity(presetId);
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);

            // Act
            SubscriptionEntity saved = persistAndFlush(subscription);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getId()).isEqualTo(presetId);
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            SubscriptionEntity subscription = createValidSubscription();

            // Act & Assert - Test basic equals/hashCode functionality
            assertThat(subscription).isEqualTo(subscription); // Same instance should equal itself
            assertThat(subscription.hashCode()).isEqualTo(subscription.hashCode()); // HashCode should be consistent
            assertThat(subscription).isNotEqualTo(null); // Should not equal null
            assertThat(subscription).isNotEqualTo("not a SubscriptionEntity"); // Should not equal different type
        }

        @Test
        @DisplayName("Should check active status based on authorization")
        void shouldCheckActiveStatusBasedOnAuthorization() {
            // Arrange
            SubscriptionEntity subscription = createValidSubscription();

            // Without authorization - should not be active
            assertThat(subscription.isActive()).isFalse();

            // Add active authorization
            AuthorizationEntity auth = new AuthorizationEntity();
            auth.setStatus(AuthorizationEntity.STATUS_ACTIVE);
            subscription.setAuthorization(auth);

            // With active authorization - should be active
            assertThat(subscription.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should track usage point count")
        void shouldTrackUsagePointCount() {
            // Arrange
            SubscriptionEntity subscription = createValidSubscription();

            // Initially empty
            assertThat(subscription.getUsagePointCount()).isZero();

            // Add usage points
            UsagePointEntity up1 = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity up2 = TestDataBuilders.createValidUsagePoint();
            subscription.getUsagePoints().addAll(List.of(up1, up2));

            // Should reflect count
            assertThat(subscription.getUsagePointCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should check if includes usage point")
        void shouldCheckIfIncludesUsagePoint() {
            // Arrange
            SubscriptionEntity subscription = createValidSubscription();
            UsagePointEntity up1 = TestDataBuilders.createValidUsagePoint();
            UsagePointEntity up2 = TestDataBuilders.createValidUsagePoint();

            subscription.getUsagePoints().add(up1);

            // Act & Assert
            assertThat(subscription.includesUsagePoint(up1)).isTrue();
            assertThat(subscription.includesUsagePoint(up2)).isFalse();
        }

        @Test
        @DisplayName("Should check customer ownership")
        void shouldCheckCustomerOwnership() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setId(123L);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(customer);

            // Act & Assert
            assertThat(subscription.belongsToCustomer(123L)).isTrue();
            assertThat(subscription.belongsToCustomer(456L)).isFalse();
            assertThat(subscription.belongsToCustomer(null)).isFalse();
        }

        @Test
        @DisplayName("Should extract subscription ID from URI")
        void shouldExtractSubscriptionIdFromUri() {
            // Act & Assert
            assertThat(SubscriptionEntity.getSubscriptionIdFromUri("/espi/1_1/resource/Subscription/12345"))
                    .isEqualTo("12345");
            assertThat(SubscriptionEntity.getSubscriptionIdFromUri("/espi/1_1/resource/Subscription/uuid-value"))
                    .isEqualTo("uuid-value");
            assertThat(SubscriptionEntity.getSubscriptionIdFromUri(null)).isNull();
            assertThat(SubscriptionEntity.getSubscriptionIdFromUri("")).isNull();
        }
    }
}
