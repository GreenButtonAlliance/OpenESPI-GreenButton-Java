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

import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SubscriptionDto builder.
 * Verifies Atom Feed construction for Subscription output.
 */
@DisplayName("SubscriptionDto Builder Tests")
class SubscriptionDtoTest {

    private static final String TEST_SUBSCRIPTION_ID = "550e8400-e29b-51d4-a716-446655440000";
    private static final String EXPECTED_SELF_HREF = "https://greenbuttondata.org/espi/1_1/resource/Subscription/" + TEST_SUBSCRIPTION_ID;
    private static final String CERTIFICATION_URL = "https://cert.greenbuttonalliance.org/certificate/test-cert-id";

    @Test
    @DisplayName("Should create SubscriptionDto with Energy schema type")
    void shouldCreateSubscriptionDtoWithEnergySchemaType() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        );

        assertThat(dto.getSubscriptionId()).isEqualTo(TEST_SUBSCRIPTION_ID);
        assertThat(dto.getSchemaType()).isEqualTo(SubscriptionDto.SchemaType.ENERGY);
        assertThat(dto.getLinks()).hasSize(1);
        assertThat(dto.getLinks().get(0).rel()).isEqualTo("self");
        assertThat(dto.getLinks().get(0).href()).isEqualTo(EXPECTED_SELF_HREF);
        assertThat(dto.getLinks().get(0).type()).isEqualTo("espi-entry/Subscription");
    }

    @Test
    @DisplayName("Should create SubscriptionDto with Customer schema type")
    void shouldCreateSubscriptionDtoWithCustomerSchemaType() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.CUSTOMER,
            now,
            now
        );

        assertThat(dto.getSchemaType()).isEqualTo(SubscriptionDto.SchemaType.CUSTOMER);
    }

    @Test
    @DisplayName("Should add certification link when provided")
    void shouldAddCertificationLinkWhenProvided() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        ).withCertificationLink(CERTIFICATION_URL);

        assertThat(dto.getLinks()).hasSize(2);

        LinkDto relatedLink = dto.getLinks().stream()
            .filter(link -> "related".equals(link.rel()))
            .findFirst()
            .orElse(null);

        assertThat(relatedLink).isNotNull();
        assertThat(relatedLink.href()).isEqualTo(CERTIFICATION_URL);
        assertThat(relatedLink.type()).isEqualTo("text/html");
    }

    @Test
    @DisplayName("Should not add certification link when null or empty")
    void shouldNotAddCertificationLinkWhenNullOrEmpty() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SubscriptionDto dtoWithNull = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        ).withCertificationLink(null);

        assertThat(dtoWithNull.getLinks()).hasSize(1);

        SubscriptionDto dtoWithEmpty = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        ).withCertificationLink("   ");

        assertThat(dtoWithEmpty.getLinks()).hasSize(1);
    }

    @Test
    @DisplayName("Should add entries using fluent API")
    void shouldAddEntriesUsingFluentApi() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        AtomEntryDto usagePointEntry = new AtomEntryDto(
            "urn:uuid:test-usage-point",
            "Test Usage Point",
            new UsagePointDto()
        );

        AtomEntryDto meterReadingEntry = new AtomEntryDto(
            "urn:uuid:test-meter-reading",
            "Test Meter Reading",
            new MeterReadingDto()
        );

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        )
            .withEntry(usagePointEntry)
            .withEntry(meterReadingEntry);

        assertThat(dto.getEntries()).hasSize(2);
        assertThat(dto.getEntryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should add multiple entries at once")
    void shouldAddMultipleEntriesAtOnce() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<AtomEntryDto> entries = List.of(
            new AtomEntryDto("urn:uuid:entry1", "Entry 1", new UsagePointDto()),
            new AtomEntryDto("urn:uuid:entry2", "Entry 2", new MeterReadingDto())
        );

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        ).withEntries(entries);

        assertThat(dto.getEntries()).hasSize(2);
    }

    @Test
    @DisplayName("Should convert to AtomFeedDto with Energy title")
    void shouldConvertToAtomFeedDtoWithEnergyTitle() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        );

        AtomFeedDto feed = dto.toAtomFeed();

        assertThat(feed.id()).isEqualTo("urn:uuid:" + TEST_SUBSCRIPTION_ID);
        assertThat(feed.title()).isEqualTo("Green Button Energy Feed");
        assertThat(feed.published()).isEqualTo(now);
        assertThat(feed.updated()).isEqualTo(now);
        assertThat(feed.links()).hasSize(1);
    }

    @Test
    @DisplayName("Should convert to AtomFeedDto with Customer title")
    void shouldConvertToAtomFeedDtoWithCustomerTitle() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.CUSTOMER,
            now,
            now
        );

        AtomFeedDto feed = dto.toAtomFeed();

        assertThat(feed.title()).isEqualTo("Green Button Customer Feed");
    }

    @Test
    @DisplayName("Should include all entries in AtomFeedDto")
    void shouldIncludeAllEntriesInAtomFeedDto() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        AtomEntryDto entry1 = new AtomEntryDto("urn:uuid:entry1", "Entry 1", new UsagePointDto());
        AtomEntryDto entry2 = new AtomEntryDto("urn:uuid:entry2", "Entry 2", new MeterReadingDto());

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        )
            .withEntry(entry1)
            .withEntry(entry2)
            .withCertificationLink(CERTIFICATION_URL);

        AtomFeedDto feed = dto.toAtomFeed();

        assertThat(feed.entries()).hasSize(2);
        assertThat(feed.links()).hasSize(2); // self + related
    }

    @Test
    @DisplayName("Should handle null entry gracefully")
    void shouldHandleNullEntryGracefully() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        ).withEntry(null);

        assertThat(dto.getEntries()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null entries list gracefully")
    void shouldHandleNullEntriesListGracefully() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SubscriptionDto dto = new SubscriptionDto(
            TEST_SUBSCRIPTION_ID,
            SubscriptionDto.SchemaType.ENERGY,
            now,
            now
        ).withEntries(null);

        assertThat(dto.getEntries()).isEmpty();
    }
}
