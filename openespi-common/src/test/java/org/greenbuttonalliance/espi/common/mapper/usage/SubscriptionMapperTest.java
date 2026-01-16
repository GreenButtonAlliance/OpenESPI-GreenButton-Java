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

package org.greenbuttonalliance.espi.common.mapper.usage;

import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.dto.usage.SubscriptionDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SubscriptionMapper.
 * Verifies Entity-to-DTO conversion with certification link handling.
 */
@DisplayName("SubscriptionMapper Tests")
class SubscriptionMapperTest {

    private SubscriptionMapper subscriptionMapper;

    private static final UUID TEST_SUBSCRIPTION_ID = UUID.fromString("550e8400-e29b-51d4-a716-446655440000");
    private static final String TEST_CERTIFICATION_ID = "cert-uuid-12345";
    private static final String CERTIFICATION_BASE_URL = "https://cert.greenbuttonalliance.org/certificate/";

    @BeforeEach
    void setUp() {
        subscriptionMapper = new SubscriptionMapper();
        // Set default values (not certified)
        ReflectionTestUtils.setField(subscriptionMapper, "certified", false);
        ReflectionTestUtils.setField(subscriptionMapper, "certificationId", "");
        ReflectionTestUtils.setField(subscriptionMapper, "certificationBaseUrl", CERTIFICATION_BASE_URL);
    }

    @Test
    @DisplayName("Should return null for null entity")
    void shouldReturnNullForNullEntity() {
        SubscriptionDto dto = subscriptionMapper.toDto(null, SubscriptionDto.SchemaType.ENERGY);

        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Should create SubscriptionDto from entity")
    void shouldCreateSubscriptionDtoFromEntity() {
        SubscriptionEntity entity = new SubscriptionEntity(TEST_SUBSCRIPTION_ID);

        SubscriptionDto dto = subscriptionMapper.toDto(entity, SubscriptionDto.SchemaType.ENERGY);

        assertThat(dto).isNotNull();
        assertThat(dto.getSubscriptionId()).isEqualTo(TEST_SUBSCRIPTION_ID.toString());
        assertThat(dto.getSchemaType()).isEqualTo(SubscriptionDto.SchemaType.ENERGY);
        assertThat(dto.getLinks()).hasSize(1); // Only self link
        assertThat(dto.getLinks().get(0).rel()).isEqualTo("self");
    }

    @Test
    @DisplayName("Should include certification link when certified")
    void shouldIncludeCertificationLinkWhenCertified() {
        // Configure as certified
        ReflectionTestUtils.setField(subscriptionMapper, "certified", true);
        ReflectionTestUtils.setField(subscriptionMapper, "certificationId", TEST_CERTIFICATION_ID);

        SubscriptionEntity entity = new SubscriptionEntity(TEST_SUBSCRIPTION_ID);

        SubscriptionDto dto = subscriptionMapper.toDto(entity, SubscriptionDto.SchemaType.ENERGY);

        assertThat(dto.getLinks()).hasSize(2); // self + related
        assertThat(dto.getLinks().stream()
            .filter(link -> "related".equals(link.rel()))
            .findFirst())
            .isPresent();
    }

    @Test
    @DisplayName("Should not include certification link when not certified")
    void shouldNotIncludeCertificationLinkWhenNotCertified() {
        // Keep default (not certified)
        SubscriptionEntity entity = new SubscriptionEntity(TEST_SUBSCRIPTION_ID);

        SubscriptionDto dto = subscriptionMapper.toDto(entity, SubscriptionDto.SchemaType.ENERGY);

        assertThat(dto.getLinks()).hasSize(1); // Only self link
        assertThat(dto.getLinks().stream()
            .filter(link -> "related".equals(link.rel()))
            .findFirst())
            .isEmpty();
    }

    @Test
    @DisplayName("Should create AtomFeedDto directly with entries")
    void shouldCreateAtomFeedDtoDirectlyWithEntries() {
        SubscriptionEntity entity = new SubscriptionEntity(TEST_SUBSCRIPTION_ID);

        List<AtomEntryDto> entries = List.of(
            new AtomEntryDto("urn:uuid:entry1", "Usage Point", new UsagePointDto(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)),
            new AtomEntryDto("urn:uuid:entry2", "Meter Reading", new MeterReadingDto())
        );

        AtomFeedDto feed = subscriptionMapper.toAtomFeed(entity, SubscriptionDto.SchemaType.ENERGY, entries);

        assertThat(feed).isNotNull();
        assertThat(feed.id()).isEqualTo("urn:uuid:" + TEST_SUBSCRIPTION_ID);
        assertThat(feed.title()).isEqualTo("Green Button Energy Feed");
        assertThat(feed.entries()).hasSize(2);
    }

    @Test
    @DisplayName("Should return null AtomFeedDto for null entity")
    void shouldReturnNullAtomFeedDtoForNullEntity() {
        AtomFeedDto feed = subscriptionMapper.toAtomFeed(null, SubscriptionDto.SchemaType.ENERGY, List.of());

        assertThat(feed).isNull();
    }

    @Test
    @DisplayName("Should return correct certification URL when certified")
    void shouldReturnCorrectCertificationUrlWhenCertified() {
        ReflectionTestUtils.setField(subscriptionMapper, "certified", true);
        ReflectionTestUtils.setField(subscriptionMapper, "certificationId", TEST_CERTIFICATION_ID);

        String certUrl = subscriptionMapper.getCertificationUrl();

        assertThat(certUrl).isEqualTo(CERTIFICATION_BASE_URL + TEST_CERTIFICATION_ID);
    }

    @Test
    @DisplayName("Should return null certification URL when not certified")
    void shouldReturnNullCertificationUrlWhenNotCertified() {
        String certUrl = subscriptionMapper.getCertificationUrl();

        assertThat(certUrl).isNull();
    }

    @Test
    @DisplayName("Should report certified status correctly")
    void shouldReportCertifiedStatusCorrectly() {
        assertThat(subscriptionMapper.isCertified()).isFalse();

        ReflectionTestUtils.setField(subscriptionMapper, "certified", true);
        ReflectionTestUtils.setField(subscriptionMapper, "certificationId", TEST_CERTIFICATION_ID);

        assertThat(subscriptionMapper.isCertified()).isTrue();
    }

    @Test
    @DisplayName("Should use Customer title for Customer schema type")
    void shouldUseCustomerTitleForCustomerSchemaType() {
        SubscriptionEntity entity = new SubscriptionEntity(TEST_SUBSCRIPTION_ID);

        AtomFeedDto feed = subscriptionMapper.toAtomFeed(entity, SubscriptionDto.SchemaType.CUSTOMER, List.of());

        assertThat(feed.title()).isEqualTo("Green Button Customer Feed");
    }
}
