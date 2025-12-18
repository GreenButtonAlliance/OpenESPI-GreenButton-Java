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

import org.greenbuttonalliance.espi.common.domain.usage.*;
import org.greenbuttonalliance.espi.common.domain.common.GrantType;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintViolation;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for SubscriptionRepository.
 * 
 * Tests subscription lifecycle management, all custom query methods,
 * relationship testing, and validation constraints.
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
     */
    private SubscriptionEntity createValidSubscription() {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setDescription("Test Subscription");
        subscription.setHashedId("hashed-" + faker.internet().uuid());
        subscription.setLastUpdate(Calendar.getInstance());
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
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Subscription");
            assertThat(retrieved.get().getRetailCustomer().getId()).isEqualTo(savedCustomer.getId());
            assertThat(retrieved.get().getApplicationInformation().getId()).isEqualTo(savedApp.getId());
        }

        @Test
        @DisplayName("Should save subscription with lifecycle fields")
        void shouldSaveSubscriptionWithLifecycleFields() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("life" + UUID.randomUUID().toString().substring(0, 8));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);
            subscription.setDescription("Subscription with Lifecycle Fields");
            
            Calendar lastUpdate = Calendar.getInstance();
            lastUpdate.add(Calendar.HOUR, -1); // 1 hour ago
            subscription.setLastUpdate(lastUpdate);

            // Act
            SubscriptionEntity saved = subscriptionRepository.save(subscription);
            flushAndClear();
            Optional<SubscriptionEntity> retrieved = subscriptionRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            SubscriptionEntity entity = retrieved.get();
            assertThat(entity.getHashedId()).isNotNull();
            assertThat(entity.getLastUpdate()).isNotNull();
            assertThat(entity.getLastUpdate().getTimeInMillis()).isLessThan(System.currentTimeMillis());
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
            
            for (int i = 0; i < subscriptions.size(); i++) {
                subscriptions.get(i).setDescription("Subscription " + (i + 1));
                subscriptions.get(i).setRetailCustomer(savedCustomer);
                subscriptions.get(i).setApplicationInformation(savedApp);
            }
            
            subscriptionRepository.saveAll(subscriptions);
            flushAndClear();

            // Act
            List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findAll();

            // Assert
            assertThat(allSubscriptions).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allSubscriptions).extracting(SubscriptionEntity::getDescription)
                    .contains("Subscription 1", "Subscription 2", "Subscription 3");
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
            subscription.setDescription("Subscription to Delete");
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
            subscription.setDescription("Subscription with Specific Hashed ID");
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);
            
            subscriptionRepository.save(subscription);
            flushAndClear();

            // Act
            Optional<SubscriptionEntity> result = subscriptionRepository.findByHashedId(hashedId);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getHashedId()).isEqualTo(hashedId);
            assertThat(result.get().getDescription()).isEqualTo("Subscription with Specific Hashed ID");
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
            subscription.setDescription("Subscription with Authorization");
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);
            subscription.setAuthorization(savedAuth);
            
            subscriptionRepository.save(subscription);
            flushAndClear();

            // Act
            Optional<SubscriptionEntity> result = subscriptionRepository.findByAuthorizationId(savedAuth.getId());

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getAuthorization().getId()).isEqualTo(savedAuth.getId());
            assertThat(result.get().getDescription()).isEqualTo("Subscription with Authorization");
        }

        @Test
        @DisplayName("Should find all subscription IDs")
        void shouldFindAllSubscriptionIds() {
            // Arrange
            long initialCount = subscriptionRepository.count();
            
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("ids" + faker.number().digits(7));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            List<SubscriptionEntity> subscriptions = List.of(
                createValidSubscription(),
                createValidSubscription(),
                createValidSubscription()
            );
            
            subscriptions.forEach(sub -> {
                sub.setRetailCustomer(savedCustomer);
                sub.setApplicationInformation(savedApp);
            });
            
            List<SubscriptionEntity> savedSubscriptions = subscriptionRepository.saveAll(subscriptions);
            flushAndClear();

            // Act
            List<UUID> allIds = subscriptionRepository.findAllIds();

            // Assert
            assertThat(allIds).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allIds).contains(
                    savedSubscriptions.get(0).getId(),
                    savedSubscriptions.get(1).getId(),
                    savedSubscriptions.get(2).getId()
            );
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
            sub1.setDescription("Customer Subscription 1");
            sub1.setRetailCustomer(savedCustomer);
            sub1.setApplicationInformation(savedApp);
            
            SubscriptionEntity sub2 = createValidSubscription();
            sub2.setDescription("Customer Subscription 2");
            sub2.setRetailCustomer(savedCustomer);
            sub2.setApplicationInformation(savedApp);

            subscriptionRepository.saveAll(List.of(sub1, sub2));
            flushAndClear();

            // Act
            List<SubscriptionEntity> results = subscriptionRepository.findByRetailCustomerId(savedCustomer.getId());

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(SubscriptionEntity::getDescription)
                    .contains("Customer Subscription 1", "Customer Subscription 2");
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
            sub1.setDescription("App Subscription 1");
            sub1.setRetailCustomer(savedCustomer);
            sub1.setApplicationInformation(savedApp);
            
            SubscriptionEntity sub2 = createValidSubscription();
            sub2.setDescription("App Subscription 2");
            sub2.setRetailCustomer(savedCustomer);
            sub2.setApplicationInformation(savedApp);

            subscriptionRepository.saveAll(List.of(sub1, sub2));
            flushAndClear();

            // Act
            List<SubscriptionEntity> results = subscriptionRepository.findByApplicationInformationId(savedApp.getId());

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(SubscriptionEntity::getDescription)
                    .contains("App Subscription 1", "App Subscription 2");
        }

        @Test
        @DisplayName("Should find active subscriptions")
        void shouldFindActiveSubscriptions() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("active" + faker.number().digits(5));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            // Create active authorization
            AuthorizationEntity activeAuth = new AuthorizationEntity();
            activeAuth.setDescription("Active Authorization");
            activeAuth.setAccessToken("active-token-" + faker.internet().uuid());
            activeAuth.setStatus(AuthorizationEntity.STATUS_ACTIVE);
            AuthorizationEntity savedActiveAuth = authorizationRepository.save(activeAuth);

            // Create inactive authorization
            AuthorizationEntity inactiveAuth = new AuthorizationEntity();
            inactiveAuth.setDescription("Inactive Authorization");
            inactiveAuth.setAccessToken("inactive-token-" + faker.internet().uuid());
            inactiveAuth.setStatus(AuthorizationEntity.STATUS_REVOKED);
            AuthorizationEntity savedInactiveAuth = authorizationRepository.save(inactiveAuth);

            // Create subscriptions
            SubscriptionEntity activeSub = createValidSubscription();
            activeSub.setDescription("Active Subscription");
            activeSub.setRetailCustomer(savedCustomer);
            activeSub.setApplicationInformation(savedApp);
            activeSub.setAuthorization(savedActiveAuth);
            
            SubscriptionEntity inactiveSub = createValidSubscription();
            inactiveSub.setDescription("Inactive Subscription");
            inactiveSub.setRetailCustomer(savedCustomer);
            inactiveSub.setApplicationInformation(savedApp);
            inactiveSub.setAuthorization(savedInactiveAuth);

            subscriptionRepository.saveAll(List.of(activeSub, inactiveSub));
            flushAndClear();

            // Act
            List<SubscriptionEntity> results = subscriptionRepository.findActiveSubscriptions();

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDescription()).isEqualTo("Active Subscription");
            assertThat(results.get(0).getAuthorization().getStatus()).isEqualTo(AuthorizationEntity.STATUS_ACTIVE);
        }

        @Test
        @DisplayName("Should find subscriptions by usage point ID")
        void shouldFindSubscriptionsByUsagePointId() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("usage" + faker.number().digits(6));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Test Usage Point for Subscription");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setDescription("Subscription with Usage Point");
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);
            subscription.getUsagePoints().add(savedUsagePoint);
            
            subscriptionRepository.save(subscription);
            flushAndClear();

            // Act
            List<SubscriptionEntity> results = subscriptionRepository.findByUsagePointId(savedUsagePoint.getId());

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDescription()).isEqualTo("Subscription with Usage Point");
            assertThat(results.get(0).getUsagePoints()).hasSize(1);
            assertThat(results.get(0).getUsagePoints().get(0).getId()).isEqualTo(savedUsagePoint.getId());
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Act & Assert
            assertThat(subscriptionRepository.findByHashedId("nonexistent-hash")).isEmpty();
            assertThat(subscriptionRepository.findByAuthorizationId(UUID.randomUUID())).isEmpty();
            assertThat(subscriptionRepository.findByRetailCustomerId(UUID.randomUUID())).isEmpty();
            assertThat(subscriptionRepository.findByApplicationInformationId(UUID.randomUUID())).isEmpty();
            assertThat(subscriptionRepository.findByUsagePointId(UUID.randomUUID())).isEmpty();
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
            subscription.setDescription("Subscription with All Relationships");
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
            subscription.setDescription("Subscription with Minimal Relationships");
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
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("base" + faker.number().digits(7));
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            SubscriptionEntity subscription = createValidSubscription();
            subscription.setDescription("Subscription for Base Class Test");
            subscription.setRetailCustomer(savedCustomer);
            subscription.setApplicationInformation(savedApp);

            // Act
            SubscriptionEntity saved = persistAndFlush(subscription);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
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
    }
}