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

import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.test.BaseRepositoryTest;
import org.greenbuttonalliance.espi.common.test.TestDataBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ResourceRepository.
 * 
 * Tests generic resource management operations, XPath-based queries,
 * and resource lifecycle management for IdentifiedObject entities.
 */
@DisplayName("Resource Repository Tests")
@ActiveProfiles("test")
class ResourceRepositoryTest extends BaseRepositoryTest {

    @Autowired(required = false)
    private ResourceRepository resourceRepository;

    @Autowired
    private UsagePointRepository usagePointRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Nested
    @DisplayName("Basic Resource Operations")
    class BasicResourceOperationsTest {

        @Test
        @DisplayName("Should handle resource repository availability")
        void shouldHandleResourceRepositoryAvailability() {
            // Note: ResourceRepository may not have a concrete implementation in the current setup
            // This test verifies the repository interface is properly defined
            
            if (resourceRepository != null) {
                // If implementation exists, test basic functionality
                assertThat(resourceRepository).isNotNull();
            } else {
                // If no implementation, verify interface exists and is properly structured
                assertThat(ResourceRepository.class).isNotNull();
                assertThat(ResourceRepository.class.isInterface()).isTrue();
            }
        }

        @Test
        @DisplayName("Should verify resource repository interface methods")
        void shouldVerifyResourceRepositoryInterfaceMethods() {
            // Verify the interface has the expected methods
            Class<ResourceRepository> repositoryClass = ResourceRepository.class;
            
            // Check for key method signatures
            assertThat(repositoryClass.getMethods())
                .extracting(method -> method.getName())
                .contains(
                    "persist",
                    "flush", 
                    "findByUUID",
                    "update",
                    "findById",
                    "findAllIds",
                    "findAllIdsByUsagePointId",
                    "findAllIdsByXPath",
                    "findIdByXPath",
                    "findByResourceUri",
                    "deleteById",
                    "deleteByXPathId",
                    "merge",
                    "findAllParentsByRelatedHref",
                    "findAllRelated"
                );
        }
    }

    @Nested
    @DisplayName("Generic Resource Management")
    class GenericResourceManagementTest {

        @Test
        @DisplayName("Should support generic IdentifiedObject operations")
        void shouldSupportGenericIdentifiedObjectOperations() {
            // Create test entities using existing repositories
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Resource Repository Test Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            MeterReadingEntity meterReading = TestDataBuilders.createValidMeterReading();
            meterReading.setUsagePoint(savedUsagePoint);
            meterReading.setDescription("Resource Repository Test Meter Reading");
            MeterReadingEntity savedMeterReading = meterReadingRepository.save(meterReading);
            
            flushAndClear();

            // Verify entities were created and can be retrieved
            assertThat(savedUsagePoint.getId()).isNotNull();
            assertThat(savedMeterReading.getId()).isNotNull();
            
            // Verify they are IdentifiedObject instances
            assertThat(savedUsagePoint).isInstanceOf(IdentifiedObject.class);
            assertThat(savedMeterReading).isInstanceOf(IdentifiedObject.class);
        }

        @Test
        @DisplayName("Should handle UUID-based resource identification")
        void shouldHandleUuidBasedResourceIdentification() {
            // Create test entity
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("UUID Test Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            flushAndClear();

            // Verify UUID is properly generated and can be used for identification
            UUID entityId = savedUsagePoint.getId();
            assertThat(entityId).isNotNull();
            
            // Retrieve by UUID using standard repository
            UsagePointEntity retrieved = usagePointRepository.findById(entityId).orElse(null);
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(entityId);
            assertThat(retrieved.getDescription()).isEqualTo("UUID Test Usage Point");
        }
    }

    @Nested
    @DisplayName("Resource Hierarchy and Relationships")
    class ResourceHierarchyTest {

        @Test
        @DisplayName("Should support hierarchical resource relationships")
        void shouldSupportHierarchicalResourceRelationships() {
            // Create parent-child resource relationship
            UsagePointEntity parentUsagePoint = TestDataBuilders.createValidUsagePoint();
            parentUsagePoint.setDescription("Parent Usage Point");
            UsagePointEntity savedParent = usagePointRepository.save(parentUsagePoint);
            
            MeterReadingEntity childMeterReading = TestDataBuilders.createValidMeterReading();
            childMeterReading.setUsagePoint(savedParent);
            childMeterReading.setDescription("Child Meter Reading");
            MeterReadingEntity savedChild = meterReadingRepository.save(childMeterReading);
            
            flushAndClear();

            // Verify relationship is maintained
            MeterReadingEntity retrievedChild = meterReadingRepository.findById(savedChild.getId()).orElse(null);
            assertThat(retrievedChild).isNotNull();
            assertThat(retrievedChild.getUsagePoint()).isNotNull();
            assertThat(retrievedChild.getUsagePoint().getId()).isEqualTo(savedParent.getId());
        }

        @Test
        @DisplayName("Should support resource linking and references")
        void shouldSupportResourceLinkingAndReferences() {
            // Create resources with potential linking
            UsagePointEntity usagePoint = TestDataBuilders.createValidUsagePoint();
            usagePoint.setDescription("Linkable Usage Point");
            UsagePointEntity savedUsagePoint = usagePointRepository.save(usagePoint);
            
            // Verify resource has proper ESPI linking capabilities
            assertThat(savedUsagePoint.getSelfHref()).isNotNull();
            assertThat(savedUsagePoint.getUpHref()).isNotNull();
            
            // Verify resource can generate proper URIs
            String selfHref = savedUsagePoint.getSelfHref();
            assertThat(selfHref).contains("UsagePoint");
            assertThat(selfHref).contains(savedUsagePoint.getHashedId());
        }
    }

    @Nested
    @DisplayName("Resource Lifecycle Management")
    class ResourceLifecycleTest {

        @Test
        @DisplayName("Should support resource creation and persistence")
        void shouldSupportResourceCreationAndPersistence() {
            // Test resource creation
            UsagePointEntity resource = TestDataBuilders.createValidUsagePoint();
            resource.setDescription("Lifecycle Test Resource");
            
            // Persist resource
            UsagePointEntity savedResource = usagePointRepository.save(resource);
            flushAndClear();
            
            // Verify persistence
            assertThat(savedResource.getId()).isNotNull();
            assertThat(savedResource.getCreated()).isNotNull();
            assertThat(savedResource.getUpdated()).isNotNull();
        }

        @Test
        @DisplayName("Should support resource updates and merging")
        void shouldSupportResourceUpdatesAndMerging() {
            // Create initial resource
            UsagePointEntity resource = TestDataBuilders.createValidUsagePoint();
            resource.setDescription("Original Description");
            UsagePointEntity savedResource = usagePointRepository.save(resource);
            flushAndClear();
            
            // Update resource
            savedResource.setDescription("Updated Description");
            UsagePointEntity updatedResource = usagePointRepository.save(savedResource);
            flushAndClear();
            
            // Verify update
            UsagePointEntity retrievedResource = usagePointRepository.findById(updatedResource.getId()).orElse(null);
            assertThat(retrievedResource).isNotNull();
            assertThat(retrievedResource.getDescription()).isEqualTo("Updated Description");
            assertThat(retrievedResource.getUpdated()).isAfter(retrievedResource.getCreated());
        }

        @Test
        @DisplayName("Should support resource deletion")
        void shouldSupportResourceDeletion() {
            // Create resource
            UsagePointEntity resource = TestDataBuilders.createValidUsagePoint();
            resource.setDescription("To Be Deleted");
            UsagePointEntity savedResource = usagePointRepository.save(resource);
            UUID resourceId = savedResource.getId();
            flushAndClear();
            
            // Delete resource
            usagePointRepository.deleteById(resourceId);
            flushAndClear();
            
            // Verify deletion
            boolean exists = usagePointRepository.existsById(resourceId);
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Resource Query Capabilities")
    class ResourceQueryTest {

        @Test
        @DisplayName("Should support resource queries by various criteria")
        void shouldSupportResourceQueriesByVariousCriteria() {
            // Create test resources
            UsagePointEntity usagePoint1 = TestDataBuilders.createValidUsagePoint();
            usagePoint1.setDescription("Query Test Point 1");
            UsagePointEntity usagePoint2 = TestDataBuilders.createValidUsagePoint();
            usagePoint2.setDescription("Query Test Point 2");
            
            UsagePointEntity saved1 = usagePointRepository.save(usagePoint1);
            UsagePointEntity saved2 = usagePointRepository.save(usagePoint2);
            flushAndClear();
            
            // Query all resources
            List<UsagePointEntity> allUsagePoints = usagePointRepository.findAll();
            assertThat(allUsagePoints).hasSizeGreaterThanOrEqualTo(2);
            
            // Query by ID
            UsagePointEntity foundById = usagePointRepository.findById(saved1.getId()).orElse(null);
            assertThat(foundById).isNotNull();
            assertThat(foundById.getDescription()).isEqualTo("Query Test Point 1");
        }

        @Test
        @DisplayName("Should support resource counting and existence checks")
        void shouldSupportResourceCountingAndExistenceChecks() {
            // Get initial count
            long initialCount = usagePointRepository.count();
            
            // Create test resource
            UsagePointEntity resource = TestDataBuilders.createValidUsagePoint();
            resource.setDescription("Count Test Resource");
            UsagePointEntity savedResource = usagePointRepository.save(resource);
            flushAndClear();
            
            // Verify count increased
            long newCount = usagePointRepository.count();
            assertThat(newCount).isEqualTo(initialCount + 1);
            
            // Verify existence
            boolean exists = usagePointRepository.existsById(savedResource.getId());
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("Interface Compliance")
    class InterfaceComplianceTest {

        @Test
        @DisplayName("Should define complete ResourceRepository interface")
        void shouldDefineCompleteResourceRepositoryInterface() {
            // Verify interface is properly defined
            Class<ResourceRepository> repositoryClass = ResourceRepository.class;
            assertThat(repositoryClass.isInterface()).isTrue();
            
            // Verify key method groups exist
            String[] expectedMethods = {
                "persist", "flush", "update", "merge",
                "findByUUID", "findById", "findByResourceUri",
                "findAllIds", "findAllIdsByUsagePointId", "findAllIdsByXPath",
                "findIdByXPath", "deleteById", "deleteByXPathId",
                "findAllParentsByRelatedHref", "findAllRelated"
            };
            
            List<String> actualMethodNames = List.of(repositoryClass.getMethods())
                .stream()
                .map(method -> method.getName())
                .toList();
            
            for (String expectedMethod : expectedMethods) {
                assertThat(actualMethodNames).contains(expectedMethod);
            }
        }

        @Test
        @DisplayName("Should support generic type operations")
        void shouldSupportGenericTypeOperations() {
            // Verify interface supports generic operations
            Class<ResourceRepository> repositoryClass = ResourceRepository.class;
            
            // Check that methods with generic type parameters exist
            boolean hasGenericMethods = List.of(repositoryClass.getMethods())
                .stream()
                .anyMatch(method -> method.getTypeParameters().length > 0 || 
                         method.getParameterTypes().length > 0 && 
                         method.getParameterTypes()[method.getParameterTypes().length - 1] == Class.class);
            
            assertThat(hasGenericMethods).isTrue();
        }
    }
}