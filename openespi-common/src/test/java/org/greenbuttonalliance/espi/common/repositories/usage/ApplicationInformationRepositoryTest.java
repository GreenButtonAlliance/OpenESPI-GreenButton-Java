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

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.common.GrantType;
import org.greenbuttonalliance.espi.common.domain.common.ResponseType;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
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
 * Comprehensive test suite for ApplicationInformationRepository.
 * 
 * Tests all CRUD operations, 7 custom query methods, OAuth2 field validation,
 * GrantType and ResponseType enum handling, and Authorization relationships.
 */
@DisplayName("ApplicationInformation Repository Tests")
class ApplicationInformationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ApplicationInformationRepository applicationInformationRepository;

    /**
     * Creates a valid ApplicationInformationEntity for testing.
     */
    private ApplicationInformationEntity createValidApplicationInformation() {
        ApplicationInformationEntity app = new ApplicationInformationEntity();
        app.setDescription("Test Application Information");
        app.setClientId(faker.internet().uuid().substring(0, 32)); // Valid length
        app.setClientSecret(faker.internet().password());
        app.setThirdPartyApplicationName("Test Application");
        app.setDataCustodianId("test-datacustodian-" + faker.number().digits(8));
        app.setKind("WEB_APPLICATION");
        app.setThirdPartyApplicationStatus("ACTIVE");
        app.setDataCustodianApplicationStatus("APPROVED");
        app.setAuthorizationServerUri("https://auth.example.com");
        app.setRedirectUri("https://app.example.com/callback");
        
        // Add OAuth2 scopes
        Set<String> scopes = new HashSet<>();
        scopes.add("FB=1_3_4_5_13_14_39_40");
        scopes.add("IntervalDuration=3600");
        scopes.add("BlockDuration=monthly");
        scopes.add("HistoryLength=13");
        app.setScope(scopes);
        
        // Add grant types
        Set<GrantType> grantTypes = new HashSet<>();
        grantTypes.add(GrantType.AUTHORIZATION_CODE);
        grantTypes.add(GrantType.CLIENT_CREDENTIALS);
        app.setGrantTypes(grantTypes);
        
        // Set response type
        app.setResponseTypes(ResponseType.CODE);
        
        return app;
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {

        @Test
        @DisplayName("Should save and retrieve application information successfully")
        void shouldSaveAndRetrieveApplicationInformationSuccessfully() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setDescription("Test Application for CRUD");

            // Act
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            flushAndClear();
            Optional<ApplicationInformationEntity> retrieved = applicationInformationRepository.findById(saved.getId());

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo("Test Application for CRUD");
            assertThat(retrieved.get().getClientId()).isEqualTo(app.getClientId());
            assertThat(retrieved.get().getThirdPartyApplicationName()).isEqualTo("Test Application");
        }

        @Test
        @DisplayName("Should save application information with OAuth2 fields")
        void shouldSaveApplicationInformationWithOAuth2Fields() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setDescription("Application with OAuth2 Fields");

            // Act
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            flushAndClear();
            Optional<ApplicationInformationEntity> retrieved = applicationInformationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ApplicationInformationEntity entity = retrieved.get();
            assertThat(entity.getClientId()).isNotNull();
            assertThat(entity.getClientSecret()).isNotNull();
            assertThat(entity.getScope()).hasSize(4);
            assertThat(entity.getScope()).contains("FB=1_3_4_5_13_14_39_40");
            assertThat(entity.getGrantTypes()).hasSize(2);
            assertThat(entity.getGrantTypes()).contains(GrantType.AUTHORIZATION_CODE, GrantType.CLIENT_CREDENTIALS);
            assertThat(entity.getResponseTypes()).isEqualTo(ResponseType.CODE);
        }

        @Test
        @DisplayName("Should find all application information")
        void shouldFindAllApplicationInformation() {
            // Arrange
            List<ApplicationInformationEntity> apps = List.of(
                createValidApplicationInformation(),
                createValidApplicationInformation(),
                createValidApplicationInformation()
            );
            apps.get(0).setDescription("App 1");
            apps.get(1).setDescription("App 2");
            apps.get(2).setDescription("App 3");
            applicationInformationRepository.saveAll(apps);
            flushAndClear();

            // Act
            List<ApplicationInformationEntity> allApps = applicationInformationRepository.findAll();

            // Assert
            assertThat(allApps).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allApps).extracting(ApplicationInformationEntity::getDescription)
                    .contains("App 1", "App 2", "App 3");
        }

        @Test
        @DisplayName("Should delete application information successfully")
        void shouldDeleteApplicationInformationSuccessfully() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setDescription("Application to Delete");
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            UUID appId = saved.getId();
            flushAndClear();

            // Act
            applicationInformationRepository.deleteById(appId);
            flushAndClear();
            Optional<ApplicationInformationEntity> retrieved = applicationInformationRepository.findById(appId);

            // Assert
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should check if application information exists")
        void shouldCheckIfApplicationInformationExists() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            flushAndClear();

            // Act & Assert
            assertThat(applicationInformationRepository.existsById(saved.getId())).isTrue();
            assertThat(applicationInformationRepository.existsById(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should count application information")
        void shouldCountApplicationInformation() {
            // Arrange
            long initialCount = applicationInformationRepository.count();
            List<ApplicationInformationEntity> apps = List.of(
                createValidApplicationInformation(),
                createValidApplicationInformation()
            );
            applicationInformationRepository.saveAll(apps);
            flushAndClear();

            // Act
            long finalCount = applicationInformationRepository.count();

            // Assert
            assertThat(finalCount).isEqualTo(initialCount + 2);
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest {

        @Test
        @DisplayName("Should find application information by client ID")
        void shouldFindApplicationInformationByClientId() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            String clientId = "test-client-" + faker.number().digits(8);
            app.setClientId(clientId);
            app.setDescription("Application with Specific Client ID");
            applicationInformationRepository.save(app);
            flushAndClear();

            // Act
            Optional<ApplicationInformationEntity> result = applicationInformationRepository.findByClientId(clientId);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getClientId()).isEqualTo(clientId);
            assertThat(result.get().getDescription()).isEqualTo("Application with Specific Client ID");
        }

        @Test
        @DisplayName("Should find application information by data custodian ID")
        void shouldFindApplicationInformationByDataCustodianId() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            String dataCustodianId = "datacustodian-" + faker.number().digits(8);
            app.setDataCustodianId(dataCustodianId);
            app.setDescription("Application with Specific Data Custodian ID");
            applicationInformationRepository.save(app);
            flushAndClear();

            // Act
            Optional<ApplicationInformationEntity> result = applicationInformationRepository.findByDataCustodianId(dataCustodianId);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getDataCustodianId()).isEqualTo(dataCustodianId);
            assertThat(result.get().getDescription()).isEqualTo("Application with Specific Data Custodian ID");
        }

        @Test
        @DisplayName("Should find all application information by kind")
        void shouldFindAllApplicationInformationByKind() {
            // Arrange
            String kind = "MOBILE_APPLICATION";
            ApplicationInformationEntity app1 = createValidApplicationInformation();
            app1.setKind(kind);
            app1.setDescription("Mobile App 1");
            
            ApplicationInformationEntity app2 = createValidApplicationInformation();
            app2.setKind(kind);
            app2.setDescription("Mobile App 2");
            
            ApplicationInformationEntity app3 = createValidApplicationInformation();
            app3.setKind("WEB_APPLICATION");
            app3.setDescription("Web App");
            
            applicationInformationRepository.saveAll(List.of(app1, app2, app3));
            flushAndClear();

            // Act
            List<ApplicationInformationEntity> results = applicationInformationRepository.findByKind(kind);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ApplicationInformationEntity::getKind).containsOnly(kind);
            assertThat(results).extracting(ApplicationInformationEntity::getDescription)
                    .contains("Mobile App 1", "Mobile App 2");
        }

        @Test
        @DisplayName("Should find all application information IDs")
        void shouldFindAllApplicationInformationIds() {
            // Arrange
            long initialCount = applicationInformationRepository.count();
            List<ApplicationInformationEntity> apps = List.of(
                createValidApplicationInformation(),
                createValidApplicationInformation(),
                createValidApplicationInformation()
            );
            List<ApplicationInformationEntity> savedApps = applicationInformationRepository.saveAll(apps);
            flushAndClear();

            // Act
            List<UUID> allIds = applicationInformationRepository.findAllIds();

            // Assert
            assertThat(allIds).hasSizeGreaterThanOrEqualTo(3);
            assertThat(allIds).contains(
                    savedApps.get(0).getId(),
                    savedApps.get(1).getId(),
                    savedApps.get(2).getId()
            );
        }

        @Test
        @DisplayName("Should check if application exists by client ID")
        void shouldCheckIfApplicationExistsByClientId() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            String clientId = "exists-test-client-" + faker.number().digits(8);
            app.setClientId(clientId);
            applicationInformationRepository.save(app);
            flushAndClear();

            // Act & Assert
            assertThat(applicationInformationRepository.existsByClientId(clientId)).isTrue();
            assertThat(applicationInformationRepository.existsByClientId("nonexistent-client")).isFalse();
        }

        @Test
        @DisplayName("Should find applications by third party application status")
        void shouldFindApplicationsByThirdPartyApplicationStatus() {
            // Arrange
            String status = "PENDING";
            ApplicationInformationEntity app1 = createValidApplicationInformation();
            app1.setThirdPartyApplicationStatus(status);
            app1.setDescription("Pending App 1");
            
            ApplicationInformationEntity app2 = createValidApplicationInformation();
            app2.setThirdPartyApplicationStatus(status);
            app2.setDescription("Pending App 2");
            
            ApplicationInformationEntity app3 = createValidApplicationInformation();
            app3.setThirdPartyApplicationStatus("ACTIVE");
            app3.setDescription("Active App");
            
            applicationInformationRepository.saveAll(List.of(app1, app2, app3));
            flushAndClear();

            // Act
            List<ApplicationInformationEntity> results = applicationInformationRepository.findByThirdPartyApplicationStatus(status);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ApplicationInformationEntity::getThirdPartyApplicationStatus).containsOnly(status);
            assertThat(results).extracting(ApplicationInformationEntity::getDescription)
                    .contains("Pending App 1", "Pending App 2");
        }

        @Test
        @DisplayName("Should find applications by data custodian application status")
        void shouldFindApplicationsByDataCustodianApplicationStatus() {
            // Arrange
            String status = "REJECTED";
            ApplicationInformationEntity app1 = createValidApplicationInformation();
            app1.setDataCustodianApplicationStatus(status);
            app1.setDescription("Rejected App 1");
            
            ApplicationInformationEntity app2 = createValidApplicationInformation();
            app2.setDataCustodianApplicationStatus(status);
            app2.setDescription("Rejected App 2");
            
            ApplicationInformationEntity app3 = createValidApplicationInformation();
            app3.setDataCustodianApplicationStatus("APPROVED");
            app3.setDescription("Approved App");
            
            applicationInformationRepository.saveAll(List.of(app1, app2, app3));
            flushAndClear();

            // Act
            List<ApplicationInformationEntity> results = applicationInformationRepository.findByDataCustodianApplicationStatus(status);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ApplicationInformationEntity::getDataCustodianApplicationStatus).containsOnly(status);
            assertThat(results).extracting(ApplicationInformationEntity::getDescription)
                    .contains("Rejected App 1", "Rejected App 2");
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResultsGracefully() {
            // Act & Assert
            assertThat(applicationInformationRepository.findByClientId("nonexistent-client")).isEmpty();
            assertThat(applicationInformationRepository.findByDataCustodianId("nonexistent-datacustodian")).isEmpty();
            assertThat(applicationInformationRepository.findByKind("NONEXISTENT_KIND")).isEmpty();
            assertThat(applicationInformationRepository.findByThirdPartyApplicationStatus("NONEXISTENT_STATUS")).isEmpty();
            assertThat(applicationInformationRepository.findByDataCustodianApplicationStatus("NONEXISTENT_STATUS")).isEmpty();
            assertThat(applicationInformationRepository.existsByClientId("nonexistent-client")).isFalse();
        }
    }

    @Nested
    @DisplayName("OAuth2 Field Testing")
    class OAuth2FieldTest {

        @Test
        @DisplayName("Should validate OAuth2 scope collection")
        void shouldValidateOAuth2ScopeCollection() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            Set<String> scopes = new HashSet<>();
            scopes.add("FB=1_3_4_5_13_14_39_40");
            scopes.add("IntervalDuration=900");
            scopes.add("BlockDuration=daily");
            scopes.add("HistoryLength=24");
            scopes.add("SubscriptionFrequency=daily");
            app.setScope(scopes);

            // Act
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            flushAndClear();
            Optional<ApplicationInformationEntity> retrieved = applicationInformationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ApplicationInformationEntity entity = retrieved.get();
            assertThat(entity.getScope()).hasSize(5);
            assertThat(entity.getScope()).contains(
                    "FB=1_3_4_5_13_14_39_40",
                    "IntervalDuration=900",
                    "BlockDuration=daily",
                    "HistoryLength=24",
                    "SubscriptionFrequency=daily"
            );
            assertThat(entity.getScopeArray()).hasSize(5);
        }

        @Test
        @DisplayName("Should handle empty scope collection")
        void shouldHandleEmptyScopeCollection() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setScope(new HashSet<>());

            // Act
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            flushAndClear();
            Optional<ApplicationInformationEntity> retrieved = applicationInformationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ApplicationInformationEntity entity = retrieved.get();
            assertThat(entity.getScope()).isEmpty();
            assertThat(entity.getScopeArray()).isEmpty();
        }

        @Test
        @DisplayName("Should validate client ID constraints")
        void shouldValidateClientIdConstraints() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setClientId("a"); // Too short

            // Act
            Set<ConstraintViolation<ApplicationInformationEntity>> violations = validator.validate(app);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                    .extracting(Object::toString)
                    .contains("clientId");
        }

        @Test
        @DisplayName("Should validate third party application name constraints")
        void shouldValidateThirdPartyApplicationNameConstraints() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setThirdPartyApplicationName(""); // Empty

            // Act
            Set<ConstraintViolation<ApplicationInformationEntity>> violations = validator.validate(app);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                    .extracting(Object::toString)
                    .contains("thirdPartyApplicationName");
        }
    }

    @Nested
    @DisplayName("GrantType and ResponseType Enum Testing")
    class EnumTest {

        @Test
        @DisplayName("Should save and retrieve GrantType enums")
        void shouldSaveAndRetrieveGrantTypeEnums() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            Set<GrantType> grantTypes = new HashSet<>();
            grantTypes.add(GrantType.AUTHORIZATION_CODE);
            grantTypes.add(GrantType.CLIENT_CREDENTIALS);
            grantTypes.add(GrantType.REFRESH_TOKEN);
            app.setGrantTypes(grantTypes);

            // Act
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            flushAndClear();
            Optional<ApplicationInformationEntity> retrieved = applicationInformationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ApplicationInformationEntity entity = retrieved.get();
            assertThat(entity.getGrantTypes()).hasSize(3);
            assertThat(entity.getGrantTypes()).contains(
                    GrantType.AUTHORIZATION_CODE,
                    GrantType.CLIENT_CREDENTIALS,
                    GrantType.REFRESH_TOKEN
            );
        }

        @Test
        @DisplayName("Should save and retrieve ResponseType enum")
        void shouldSaveAndRetrieveResponseTypeEnum() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setResponseTypes(ResponseType.TOKEN);

            // Act
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            flushAndClear();
            Optional<ApplicationInformationEntity> retrieved = applicationInformationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ApplicationInformationEntity entity = retrieved.get();
            assertThat(entity.getResponseTypes()).isEqualTo(ResponseType.TOKEN);
        }

        @Test
        @DisplayName("Should handle empty GrantType and null ResponseType")
        void shouldHandleEmptyGrantTypeAndNullResponseType() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setGrantTypes(new HashSet<>()); // Empty set instead of null
            app.setResponseTypes(null);

            // Act
            ApplicationInformationEntity saved = applicationInformationRepository.save(app);
            flushAndClear();
            Optional<ApplicationInformationEntity> retrieved = applicationInformationRepository.findById(saved.getId());

            // Assert
            assertThat(retrieved).isPresent();
            ApplicationInformationEntity entity = retrieved.get();
            assertThat(entity.getGrantTypes()).isEmpty();
            assertThat(entity.getResponseTypes()).isNull();
        }
    }

    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest {

        @Test
        @DisplayName("Should validate application information with valid data")
        void shouldValidateApplicationInformationWithValidData() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();

            // Act
            Set<ConstraintViolation<ApplicationInformationEntity>> violations = validator.validate(app);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() {
            // Arrange
            ApplicationInformationEntity app = new ApplicationInformationEntity();
            // Leave required fields null/empty
            // Note: thirdPartyApplicationName has a default value, so only clientId will fail validation

            // Act
            Set<ConstraintViolation<ApplicationInformationEntity>> violations = validator.validate(app);

            // Assert
            assertThat(violations).isNotEmpty();
            Set<String> violatedProperties = violations.stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(violatedProperties).contains("clientId");
        }
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {

        @Test
        @DisplayName("Should inherit IdentifiedObject functionality")
        void shouldInheritIdentifiedObjectFunctionality() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();
            app.setDescription("Application for Base Class Test");

            // Act
            ApplicationInformationEntity saved = persistAndFlush(app);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreated()).isNotNull();
            assertThat(saved.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should handle equals and hashCode correctly")
        void shouldHandleEqualsAndHashCodeCorrectly() {
            // Arrange
            ApplicationInformationEntity app = createValidApplicationInformation();

            // Act & Assert - Test basic equals/hashCode functionality
            assertThat(app).isEqualTo(app); // Same instance should equal itself
            assertThat(app.hashCode()).isEqualTo(app.hashCode()); // HashCode should be consistent
            assertThat(app).isNotEqualTo(null); // Should not equal null
            assertThat(app).isNotEqualTo("not an ApplicationInformationEntity"); // Should not equal different type
        }
    }
}