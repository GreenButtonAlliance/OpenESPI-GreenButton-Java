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

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.common.*;
import org.greenbuttonalliance.espi.common.domain.common.enums.*;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for AuthorizationRepository.
 * 
 * Tests OAuth2 authorization flow, all custom query methods,
 * enum handling, and relationship testing.
 */
@DisplayName("Authorization Repository Tests")
class AuthorizationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AuthorizationRepository authorizationRepository;

    @Autowired
    private ApplicationInformationRepository applicationInformationRepository;

    @Autowired
    private RetailCustomerRepository retailCustomerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    /**
     * Creates a valid AuthorizationEntity for testing.
     */
    private AuthorizationEntity createValidAuthorization() {
        AuthorizationEntity auth = new AuthorizationEntity();
        auth.setDescription("Test Authorization");
        auth.setAccessToken("test-access-token-" + faker.internet().uuid());
        auth.setRefreshToken("test-refresh-token-" + faker.internet().uuid());
        auth.setScope("FB=1_3_4_5_13_14_39_40 IntervalDuration=3600");
        auth.setStatus(AuthorizationEntity.STATUS_ACTIVE);
        auth.setGrantType(GrantType.AUTHORIZATION_CODE);
        auth.setResponseType(ResponseType.CODE);
        auth.setTokenType(TokenType.BEARER);
        auth.setState("test-state-" + faker.internet().uuid());
        auth.setCode("test-code-" + faker.internet().uuid());
        auth.setExpiresIn(System.currentTimeMillis() + 3600000L); // 1 hour from now
        auth.setResourceURI("https://api.example.com/resource");
        auth.setAuthorizationURI("https://auth.example.com/authorize");
        auth.setThirdParty("test-third-party");
        
        // Add DateTimeInterval for authorized period
        DateTimeInterval authorizedPeriod = new DateTimeInterval();
        authorizedPeriod.setStart(System.currentTimeMillis() / 1000);
        authorizedPeriod.setDuration(3600L); // 1 hour
        auth.setAuthorizedPeriod(authorizedPeriod);
        
        return auth;
    }

    /**
     * Creates a valid ApplicationInformationEntity for testing.
     */
    private ApplicationInformationEntity createValidApplicationInformation() {
        ApplicationInformationEntity app = new ApplicationInformationEntity();
        app.setDescription("Test Application Information");
        app.setClientId("test-client-" + faker.number().digits(8));
        app.setClientSecret(faker.internet().password());
        app.setDataCustodianId("test-datacustodian-" + faker.number().digits(8));
        
        Set<GrantType> grantTypes = new HashSet<>();
        grantTypes.add(GrantType.AUTHORIZATION_CODE);
        app.setGrantTypes(grantTypes);
        
        return app;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve authorization successfully")
        void shouldSaveAndRetrieveAuthorizationSuccessfully() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAccessToken()).isEqualTo(auth.getAccessToken());
            assertThat(retrieved.get().getStatus()).isEqualTo(AuthorizationEntity.STATUS_ACTIVE);
        }

        @Test
        @DisplayName("Should save authorization with OAuth2 fields")
        void shouldSaveAuthorizationWithOAuth2Fields() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setDescription("Authorization with OAuth2 Fields");

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            AuthorizationEntity entity = retrieved.get();
            assertThat(entity.getAccessToken()).isNotNull();
            assertThat(entity.getRefreshToken()).isNotNull();
            assertThat(entity.getScope()).isEqualTo("FB=1_3_4_5_13_14_39_40 IntervalDuration=3600");
            assertThat(entity.getGrantType()).isEqualTo(GrantType.AUTHORIZATION_CODE);
            assertThat(entity.getResponseType()).isEqualTo(ResponseType.CODE);
            assertThat(entity.getTokenType()).isEqualTo(TokenType.BEARER);
            assertThat(entity.getState()).isNotNull();
            assertThat(entity.getCode()).isNotNull();
            assertThat(entity.getExpiresIn()).isGreaterThan(System.currentTimeMillis());
        }

        @Test
        @DisplayName("Should find all authorizations")
        void shouldFindAllAuthorizations() {
            // Arrange
            List<AuthorizationEntity> auths = List.of(
                createValidAuthorization(),
                createValidAuthorization(),
                createValidAuthorization()
            );
            auths.get(0).setDescription("Auth 1");
            auths.get(1).setDescription("Auth 2");
            auths.get(2).setDescription("Auth 3");
            authorizationRepository.saveAll(auths);
            flushAndClear();

            // Act
            List<AuthorizationEntity> allAuths = authorizationRepository.findAll();

            // Assert
            assertThat(allAuths).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allAuths).extracting(AuthorizationEntity::getDescription)
                    .contains("Auth 1", "Auth 2", "Auth 3");
        }

        @Test
        @DisplayName("Should delete authorization successfully")
        void shouldDeleteAuthorizationSuccessfully() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setDescription("Authorization to Delete");
            AuthorizationEntity saved = authorizationRepository.save(auth);
            UUID authId = saved.getId();
            flushAndClear();

            // Act
            authorizationRepository.deleteById(authId);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(authId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if authorization exists")
        void shouldCheckIfAuthorizationExists() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();

            // Act & Assert
            assertThat(authorizationRepository.existsById(saved.getId())).isTrue();
            assertThat(authorizationRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count authorizations")
        void shouldCountAuthorizations() {
            // Arrange
            long initialCount = authorizationRepository.count();
            List<AuthorizationEntity> auths = List.of(
                createValidAuthorization(),
                createValidAuthorization()
            );
            authorizationRepository.saveAll(auths);
            flushAndClear();

            // Act
            long finalCount = authorizationRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find all authorizations by retail customer ID")
        void shouldFindAllAuthorizationsByRetailCustomerId() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("customer@example.com");
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            AuthorizationEntity auth1 = createValidAuthorization();
            auth1.setRetailCustomer(savedCustomer);
            auth1.setDescription("Auth 1 for Customer");
            
            AuthorizationEntity auth2 = createValidAuthorization();
            auth2.setRetailCustomer(savedCustomer);
            auth2.setDescription("Auth 2 for Customer");

            authorizationRepository.saveAll(List.of(auth1, auth2));
            flushAndClear();

            // Act
            List<AuthorizationEntity> results = authorizationRepository.findAllByRetailCustomerId(savedCustomer.getId());

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(AuthorizationEntity::getDescription)
                    .contains("Auth 1 for Customer", "Auth 2 for Customer");
        }

        @Test
        @DisplayName("Should find authorization by state")
        void shouldFindAuthorizationByState() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            String state = "unique-state-" + faker.internet().uuid();
            auth.setState(state);
            auth.setDescription("Authorization with Specific State");
            authorizationRepository.save(auth);
            flushAndClear();

            // Act
            Optional<AuthorizationEntity> result = authorizationRepository.findByState(state);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getState()).isEqualTo(state);
            assertThat(result.get().getDescription()).isEqualTo("Authorization with Specific State");
        }

        @Test
        @DisplayName("Should find authorization by access token")
        void shouldFindAuthorizationByAccessToken() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            String accessToken = "unique-access-token-" + faker.internet().uuid();
            auth.setAccessToken(accessToken);
            auth.setDescription("Authorization with Specific Access Token");
            authorizationRepository.save(auth);
            flushAndClear();

            // Act
            Optional<AuthorizationEntity> result = authorizationRepository.findByAccessToken(accessToken);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getAccessToken()).isEqualTo(accessToken);
            assertThat(result.get().getDescription()).isEqualTo("Authorization with Specific Access Token");
        }

        @Test
        @DisplayName("Should find authorization by refresh token")
        void shouldFindAuthorizationByRefreshToken() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            String refreshToken = "unique-refresh-token-" + faker.internet().uuid();
            auth.setRefreshToken(refreshToken);
            auth.setDescription("Authorization with Specific Refresh Token");
            authorizationRepository.save(auth);
            flushAndClear();

            // Act
            Optional<AuthorizationEntity> result = authorizationRepository.findByRefreshToken(refreshToken);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getRefreshToken()).isEqualTo(refreshToken);
            assertThat(result.get().getDescription()).isEqualTo("Authorization with Specific Refresh Token");
        }

        @Test
        @DisplayName("Should find authorizations by resource URI")
        void shouldFindAuthorizationsByResourceUri() {
            // Arrange
            String resourceUri = "https://api.example.com/specific-resource";
            AuthorizationEntity auth1 = createValidAuthorization();
            auth1.setResourceURI(resourceUri);
            auth1.setDescription("Auth 1 with Resource URI");
            
            AuthorizationEntity auth2 = createValidAuthorization();
            auth2.setResourceURI(resourceUri);
            auth2.setDescription("Auth 2 with Resource URI");

            authorizationRepository.saveAll(List.of(auth1, auth2));
            flushAndClear();

            // Act
            List<AuthorizationEntity> results = authorizationRepository.findByResourceUri(resourceUri);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(AuthorizationEntity::getResourceURI).containsOnly(resourceUri);
            assertThat(results).extracting(AuthorizationEntity::getDescription)
                    .contains("Auth 1 with Resource URI", "Auth 2 with Resource URI");
        }

        @Test
        @DisplayName("Should find expired authorizations")
        void shouldFindExpiredAuthorizations() {
            // Arrange
            long currentTime = System.currentTimeMillis();
            
            AuthorizationEntity expiredAuth = createValidAuthorization();
            expiredAuth.setExpiresIn(currentTime - 3600000L); // 1 hour ago
            expiredAuth.setDescription("Expired Authorization");
            
            AuthorizationEntity activeAuth = createValidAuthorization();
            activeAuth.setExpiresIn(currentTime + 3600000L); // 1 hour from now
            activeAuth.setDescription("Active Authorization");

            authorizationRepository.saveAll(List.of(expiredAuth, activeAuth));
            flushAndClear();

            // Act
            List<AuthorizationEntity> results = authorizationRepository.findExpiredAuthorizations(currentTime);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDescription()).isEqualTo("Expired Authorization");
            assertThat(results.get(0).getExpiresIn()).isLessThan(currentTime);
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Act & Assert
            assertThat(authorizationRepository.findAllByRetailCustomerId(999999L)).isEmpty();
            assertThat(authorizationRepository.findByState("nonexistent-state")).isEmpty();
            assertThat(authorizationRepository.findByAccessToken("nonexistent-token")).isEmpty();
            assertThat(authorizationRepository.findByRefreshToken("nonexistent-refresh-token")).isEmpty();
            assertThat(authorizationRepository.findByResourceUri("nonexistent-uri")).isEmpty();
            assertThat(authorizationRepository.findExpiredAuthorizations(System.currentTimeMillis())).isEmpty();
        }
    }

    @Nested
    @DisplayName("OAuth2 Authorization Flow Field Testing")
    class OAuth2FieldTest {

        @Test
        @DisplayName("Should validate OAuth2 status transitions")
        void shouldValidateOAuth2StatusTransitions() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setStatus(AuthorizationEntity.STATUS_PENDING);

            // Act & Assert - Test status transitions
            AuthorizationEntity saved = authorizationRepository.save(auth);
            assertThat(saved.getStatus()).isEqualTo(AuthorizationEntity.STATUS_PENDING);

            saved.setStatus(AuthorizationEntity.STATUS_ACTIVE);
            AuthorizationEntity updated = authorizationRepository.save(saved);
            assertThat(updated.getStatus()).isEqualTo(AuthorizationEntity.STATUS_ACTIVE);

            updated.setStatus(AuthorizationEntity.STATUS_REVOKED);
            AuthorizationEntity revoked = authorizationRepository.save(updated);
            assertThat(revoked.getStatus()).isEqualTo(AuthorizationEntity.STATUS_REVOKED);
        }

        @Test
        @DisplayName("Should handle OAuth2 error information")
        void shouldHandleOAuth2ErrorInformation() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setError(OAuthError.INVALID_REQUEST);
            auth.setErrorDescription("The request is missing a required parameter");
            auth.setErrorUri("https://tools.ietf.org/html/rfc6749#section-4.1.2.1");

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            AuthorizationEntity entity = retrieved.get();
            assertThat(entity.getError()).isEqualTo(OAuthError.INVALID_REQUEST);
            assertThat(entity.getErrorDescription()).isEqualTo("The request is missing a required parameter");
            assertThat(entity.getErrorUri()).isEqualTo("https://tools.ietf.org/html/rfc6749#section-4.1.2.1");
        }

        @Test
        @DisplayName("Should validate DateTimeInterval embedded objects")
        void shouldValidateDateTimeIntervalEmbeddedObjects() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            
            // Set authorized period
            DateTimeInterval authorizedPeriod = new DateTimeInterval();
            authorizedPeriod.setStart(System.currentTimeMillis() / 1000);
            authorizedPeriod.setDuration(7200L); // 2 hours
            auth.setAuthorizedPeriod(authorizedPeriod);
            
            // Set published period
            DateTimeInterval publishedPeriod = new DateTimeInterval();
            publishedPeriod.setStart((System.currentTimeMillis() - 86400000L) / 1000); // 1 day ago
            publishedPeriod.setDuration(86400L); // 1 day
            auth.setPublishedPeriod(publishedPeriod);

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            AuthorizationEntity entity = retrieved.get();
            assertThat(entity.getAuthorizedPeriod()).isNotNull();
            assertThat(entity.getAuthorizedPeriod().getDuration()).isEqualTo(7200L);
            assertThat(entity.getPublishedPeriod()).isNotNull();
            assertThat(entity.getPublishedPeriod().getDuration()).isEqualTo(86400L);
        }
    }

    @Nested
    @DisplayName("Enum Testing")
    class EnumTest {

        @Test
        @DisplayName("Should save and retrieve GrantType enum")
        void shouldSaveAndRetrieveGrantTypeEnum() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setGrantType(GrantType.CLIENT_CREDENTIALS);

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getGrantType()).isEqualTo(GrantType.CLIENT_CREDENTIALS);
        }

        @Test
        @DisplayName("Should save and retrieve ResponseType enum")
        void shouldSaveAndRetrieveResponseTypeEnum() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setResponseType(ResponseType.CODE);

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getResponseType()).isEqualTo(ResponseType.CODE);
        }

        @Test
        @DisplayName("Should save and retrieve TokenType enum")
        void shouldSaveAndRetrieveTokenTypeEnum() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setTokenType(TokenType.BEARER);

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getTokenType()).isEqualTo(TokenType.BEARER);
        }

        @Test
        @DisplayName("Should handle null enum values")
        void shouldHandleNullEnumValues() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setGrantType(null);
            auth.setResponseType(null);
            auth.setTokenType(null);
            auth.setError(null);

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            AuthorizationEntity entity = retrieved.get();
            assertThat(entity.getGrantType()).isNull();
            assertThat(entity.getResponseType()).isNull();
            assertThat(entity.getTokenType()).isNull();
            assertThat(entity.getError()).isNull();
        }
    }

    @Nested
    @DisplayName("Relationship Testing")
    class RelationshipTest {

        @Test
        @DisplayName("Should create authorization with retail customer relationship")
        void shouldCreateAuthorizationWithRetailCustomerRelationship() {
            // Arrange
            RetailCustomerEntity customer = TestDataBuilders.createValidRetailCustomer();
            customer.setUsername("customer@example.com");
            RetailCustomerEntity savedCustomer = retailCustomerRepository.save(customer);

            AuthorizationEntity auth = createValidAuthorization();
            auth.setRetailCustomer(savedCustomer);
            auth.setDescription("Authorization with Customer");

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            AuthorizationEntity entity = retrieved.get();
            assertThat(entity.getRetailCustomer()).isNotNull();
            assertThat(entity.getRetailCustomer().getId()).isEqualTo(savedCustomer.getId());
            assertThat(entity.getRetailCustomer().getUsername()).isEqualTo("customer@example.com");
        }

        @Test
        @DisplayName("Should create authorization with application information relationship")
        void shouldCreateAuthorizationWithApplicationInformationRelationship() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setDescription("Test Application for Authorization");
            ApplicationInformationEntity savedApp = applicationInformationRepository.save(app);

            AuthorizationEntity auth = createValidAuthorization();
            auth.setApplicationInformation(savedApp);
            auth.setDescription("Authorization with Application");

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            AuthorizationEntity entity = retrieved.get();
            assertThat(entity.getApplicationInformation()).isNotNull();
            assertThat(entity.getApplicationInformation().getId()).isEqualTo(savedApp.getId());
            assertThat(entity.getApplicationInformation().getDescription()).isEqualTo("Test Application for Authorization");
        }

        @Test
        @DisplayName("Should handle authorization without relationships")
        void shouldHandleAuthorizationWithoutRelationships() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setRetailCustomer(null);
            auth.setApplicationInformation(null);
            auth.setSubscription(null);
            auth.setDescription("Authorization without Relationships");

            // Act
            AuthorizationEntity saved = authorizationRepository.save(auth);
            flushAndClear();
            Optional<AuthorizationEntity> retrieved = authorizationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            AuthorizationEntity entity = retrieved.get();
            assertThat(entity.getRetailCustomer()).isNull();
            assertThat(entity.getApplicationInformation()).isNull();
            assertThat(entity.getSubscription()).isNull();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate authorization with valid data")
        void shouldValidateAuthorizationWithValidData() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();

            // Act
            Set<ConstraintViolation<AuthorizationEntity>> violations = validator.validate(auth);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle authorization with minimal data")
        void shouldHandleAuthorizationWithMinimalData() {
            // Arrange
            AuthorizationEntity auth = new AuthorizationEntity();
            auth.setDescription("Minimal Authorization");

            // Act
            Set<ConstraintViolation<AuthorizationEntity>> violations = validator.validate(auth);

            // Assert - Should be valid with minimal data since most fields are optional
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();
            auth.setDescription("Authorization for Base Class Test");

            // Act
            AuthorizationEntity saved = persistAndFlush(auth);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            AuthorizationEntity auth = createValidAuthorization();

            // Act & Assert - Test basic equals/hashCode functionality
            assertThat(auth).isEqualTo(auth); // Same instance should equal itself
            assertThat(auth.hashCode()).isEqualTo(auth.hashCode()); // HashCode should be consistent
            assertThat(auth).isNotEqualTo(null); // Should not equal null
            assertThat(auth).isNotEqualTo("not an AuthorizationEntity"); // Should not equal different type
        }
    }
}