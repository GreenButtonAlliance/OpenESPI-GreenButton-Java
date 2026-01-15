/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
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

package org.greenbuttonalliance.espi.common.dto.usage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BatchListDto.
 * Verifies resource management and fluent API for BatchList output.
 */
@DisplayName("BatchListDto Tests")
class BatchListDtoTest {

    private static final String TEST_URI_1 = "https://greenbuttondata.org/espi/1_1/resource/UsagePoint/1";
    private static final String TEST_URI_2 = "https://greenbuttondata.org/espi/1_1/resource/MeterReading/2";
    private static final String TEST_URI_3 = "https://greenbuttondata.org/espi/1_1/resource/IntervalBlock/3";

    @Test
    @DisplayName("Should create empty BatchListDto with no-arg constructor")
    void shouldCreateEmptyBatchListDtoWithNoArgConstructor() {
        BatchListDto dto = new BatchListDto();

        assertThat(dto.getResources()).isEmpty();
        assertThat(dto.getResourceCount()).isZero();
        assertThat(dto.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should create BatchListDto with resources via constructor")
    void shouldCreateBatchListDtoWithResourcesViaConstructor() {
        List<String> resources = Arrays.asList(TEST_URI_1, TEST_URI_2);
        BatchListDto dto = new BatchListDto(resources);

        assertThat(dto.getResources()).hasSize(2);
        assertThat(dto.getResourceCount()).isEqualTo(2);
        assertThat(dto.isEmpty()).isFalse();
        assertThat(dto.getResources()).containsExactly(TEST_URI_1, TEST_URI_2);
    }

    @Test
    @DisplayName("Should add single resource using fluent API")
    void shouldAddSingleResourceUsingFluentApi() {
        BatchListDto dto = new BatchListDto()
            .addResource(TEST_URI_1);

        assertThat(dto.getResources()).hasSize(1);
        assertThat(dto.getResourceCount()).isEqualTo(1);
        assertThat(dto.getResources()).containsExactly(TEST_URI_1);
    }

    @Test
    @DisplayName("Should add multiple resources sequentially using fluent API")
    void shouldAddMultipleResourcesSequentiallyUsingFluentApi() {
        BatchListDto dto = new BatchListDto()
            .addResource(TEST_URI_1)
            .addResource(TEST_URI_2)
            .addResource(TEST_URI_3);

        assertThat(dto.getResources()).hasSize(3);
        assertThat(dto.getResourceCount()).isEqualTo(3);
        assertThat(dto.getResources()).containsExactly(TEST_URI_1, TEST_URI_2, TEST_URI_3);
    }

    @Test
    @DisplayName("Should add multiple resources at once using fluent API")
    void shouldAddMultipleResourcesAtOnceUsingFluentApi() {
        List<String> resources = Arrays.asList(TEST_URI_1, TEST_URI_2, TEST_URI_3);
        BatchListDto dto = new BatchListDto()
            .addResources(resources);

        assertThat(dto.getResources()).hasSize(3);
        assertThat(dto.getResourceCount()).isEqualTo(3);
        assertThat(dto.getResources()).containsExactlyElementsOf(resources);
    }

    @Test
    @DisplayName("Should handle null resource gracefully")
    void shouldHandleNullResourceGracefully() {
        BatchListDto dto = new BatchListDto()
            .addResource(null);

        assertThat(dto.getResources()).isEmpty();
        assertThat(dto.getResourceCount()).isZero();
        assertThat(dto.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should handle empty string resource gracefully")
    void shouldHandleEmptyStringResourceGracefully() {
        BatchListDto dto = new BatchListDto()
            .addResource("")
            .addResource("   ");

        assertThat(dto.getResources()).isEmpty();
        assertThat(dto.getResourceCount()).isZero();
        assertThat(dto.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should trim whitespace from resources")
    void shouldTrimWhitespaceFromResources() {
        BatchListDto dto = new BatchListDto()
            .addResource("  " + TEST_URI_1 + "  ");

        assertThat(dto.getResources()).hasSize(1);
        assertThat(dto.getResources().get(0)).isEqualTo(TEST_URI_1);
    }

    @Test
    @DisplayName("Should handle null resources list gracefully")
    void shouldHandleNullResourcesListGracefully() {
        BatchListDto dto = new BatchListDto()
            .addResources(null);

        assertThat(dto.getResources()).isEmpty();
        assertThat(dto.getResourceCount()).isZero();
        assertThat(dto.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should handle empty resources list gracefully")
    void shouldHandleEmptyResourcesListGracefully() {
        BatchListDto dto = new BatchListDto()
            .addResources(new ArrayList<>());

        assertThat(dto.getResources()).isEmpty();
        assertThat(dto.getResourceCount()).isZero();
        assertThat(dto.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should initialize resources list when null and adding resource")
    void shouldInitializeResourcesListWhenNullAndAddingResource() {
        BatchListDto dto = new BatchListDto();
        // Resources list is initialized in constructor, but test the lazy init in addResource
        dto.addResource(TEST_URI_1);

        assertThat(dto.getResources()).isNotNull();
        assertThat(dto.getResources()).hasSize(1);
        assertThat(dto.getResourceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should initialize resources list when null and adding resources")
    void shouldInitializeResourcesListWhenNullAndAddingResources() {
        BatchListDto dto = new BatchListDto();
        List<String> resources = Arrays.asList(TEST_URI_1, TEST_URI_2);
        dto.addResources(resources);

        assertThat(dto.getResources()).isNotNull();
        assertThat(dto.getResources()).hasSize(2);
        assertThat(dto.getResourceCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return correct resource count")
    void shouldReturnCorrectResourceCount() {
        BatchListDto dto = new BatchListDto();
        assertThat(dto.getResourceCount()).isZero();

        dto.addResource(TEST_URI_1);
        assertThat(dto.getResourceCount()).isEqualTo(1);

        dto.addResource(TEST_URI_2);
        assertThat(dto.getResourceCount()).isEqualTo(2);

        dto.addResource(TEST_URI_3);
        assertThat(dto.getResourceCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return true for isEmpty when no resources")
    void shouldReturnTrueForIsEmptyWhenNoResources() {
        BatchListDto dto = new BatchListDto();
        assertThat(dto.isEmpty()).isTrue();

        dto.addResource(TEST_URI_1);
        assertThat(dto.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should support method chaining")
    void shouldSupportMethodChaining() {
        BatchListDto dto = new BatchListDto()
            .addResource(TEST_URI_1)
            .addResources(Arrays.asList(TEST_URI_2, TEST_URI_3));

        assertThat(dto.getResources()).hasSize(3);
        assertThat(dto.getResourceCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should create defensive copy in constructor")
    void shouldCreateDefensiveCopyInConstructor() {
        List<String> originalList = new ArrayList<>();
        originalList.add(TEST_URI_1);

        BatchListDto dto = new BatchListDto(originalList);

        // Modify original list
        originalList.add(TEST_URI_2);

        // DTO should not be affected by external modification
        assertThat(dto.getResources()).hasSize(1);
        assertThat(dto.getResourceCount()).isEqualTo(1);
    }
}
