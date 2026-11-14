/*
 * Copyright (c) 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.common.service;

import org.greenbuttonalliance.espi.common.domain.common.LinkType;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.service.impl.CustomerExportService;
import org.greenbuttonalliance.espi.common.service.impl.UsageExportService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * #247: collections of ESPI resources are exported as an Atom feed, using the real exporters
 * (the controller tests mock them, which is how the feed endpoints' 500s went unnoticed).
 */
@DisplayName("Atom feed export (#247)")
class AtomFeedExportTest {

    private static final String FEED = "https://dc.example/DataCustodian/espi/1_1/resource/UsagePoint";

    private static UsageExportService usage;
    private static CustomerExportService customer;

    @BeforeAll
    static void initExporters() {
        usage = new UsageExportService();
        usage.init();
        customer = new CustomerExportService();
        customer.init();
    }

    @Test
    @DisplayName("a list of usage points becomes a feed with one entry per resource")
    void usagePointsBecomeAFeed() {
        UUID first = UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f5fb");
        UUID second = UUID.fromString("5d6f1b42-0a6e-5f0e-9c3e-2a1d7b8c9e01");
        UsagePointEntity stored = usagePoint(first, "Front of house meter");
        stored.setSelfLink(new LinkType(FEED + "/" + first, "self"));
        stored.setPublished(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        UsagePointEntity derived = usagePoint(second, null);

        String xml = export(usage, List.of(stored, derived));
        System.out.println(xml);

        String feedId = usage.buildFeed(FEED, "t", List.<UsagePointEntity>of(), u -> u).getId();
        assertThat(xml)
                .contains("<atom:feed", "xmlns:atom=\"http://www.w3.org/2005/Atom\"", "xmlns:espi=\"http://naesb.org/espi\"")
                .contains("<atom:id>" + feedId + "</atom:id>", "<atom:title>UsagePoint Feed</atom:title>")
                .contains("rel=\"self\" href=\"" + FEED + "\" type=\"espi-feed/UsagePoint\"")
                .contains("<atom:id>urn:uuid:" + first + "</atom:id>", "<atom:title>Front of house meter</atom:title>")
                .contains("href=\"" + FEED + "/" + first + "\" type=\"espi-entry/UsagePoint\"")
                .contains("<atom:id>urn:uuid:" + second + "</atom:id>", "<atom:title>UsagePoint</atom:title>")
                .contains("href=\"" + FEED + "/" + second + "\"")
                .contains("rel=\"up\" href=\"" + FEED + "\" type=\"espi-feed/UsagePoint\"")
                .contains("<atom:published>2026-01-02T03:04:05Z</atom:published>")
                .contains("<espi:UsagePoint")
                .doesNotContain("xsi:type", "<atom:published/>", "<atom:updated/>");
        assertThat(feedId).matches("urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-5[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(count(xml, "<atom:entry")).isEqualTo(2);
    }

    @Test
    @DisplayName("an empty list is still a valid feed with no entries")
    void emptyListIsAnEmptyFeed() {
        String xml = export(usage, List.of());

        assertThat(xml)
                .contains("<atom:feed", "rel=\"self\" href=\"" + FEED + "\"")
                .containsPattern("<atom:updated>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z</atom:updated>")
                .doesNotContain("<atom:entry");
    }

    @Test
    @DisplayName("customer resources use the customer entry type and namespace")
    void customerFeedUsesCustomerEntries() {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(UUID.fromString("7c0e7a6e-3b1d-5a3c-8f43-6d1f2a9b4c11"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        customer.exportFeed("https://dc.example/DataCustodian/espi/1_1/resource/Customer", "Customer",
                List.of(entity), e -> new CustomerDto(), out);
        String xml = out.toString(StandardCharsets.UTF_8);
        System.out.println(xml);

        assertThat(xml)
                .contains("<atom:feed", "xmlns:cust=\"http://naesb.org/espi/customer\"", "<cust:Customer")
                .contains("href=\"https://dc.example/DataCustodian/espi/1_1/resource/Customer/" + entity.getId()
                        + "\" type=\"espi-entry/Customer\"")
                .doesNotContain("xsi:type", "xmlns:espi=", "href=\"self\"");
    }

    @Test
    @DisplayName("every atom:link is written as rel=, href=, type= in that order (CMD certification)")
    void linkAttributesAreRelHrefType() {
        UsagePointEntity entity = usagePoint(UUID.fromString("48c2a019-5598-5e16-b0f9-49e4ff27f5fb"), null);
        String xml = export(usage, List.of(entity)) + export(usage, List.of());

        List<String> links = java.util.regex.Pattern.compile("<atom:link\\b[^>]*/>").matcher(xml)
                .results().map(java.util.regex.MatchResult::group).toList();

        // 2 feeds' self links + 1 entry's self and up links
        assertThat(links).hasSize(4)
                .allSatisfy(link -> assertThat(link)
                        .matches("<atom:link rel=\"[^\"]+\" href=\"[^\"]+\"( type=\"[^\"]+\")?/>"));
        assertThat(links).filteredOn(link -> link.contains("type=")).hasSize(3);
    }

    @Test
    @DisplayName("exportDto rejects a bare collection with a pointer to exportFeed")
    void exportDtoRejectsCollections() {
        assertThatThrownBy(() -> usage.exportDto(List.of(new UsagePointDto()), new ByteArrayOutputStream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exportFeed");
    }

    private static UsagePointEntity usagePoint(UUID id, String description) {
        UsagePointEntity entity = new UsagePointEntity();
        entity.setId(id);
        entity.setDescription(description);
        return entity;
    }

    private static String export(UsageExportService exporter, List<UsagePointEntity> entities) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.exportFeed(FEED, "UsagePoint Feed", entities, e -> new UsagePointDto(), out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private static int count(String haystack, String needle) {
        return haystack.split(needle, -1).length - 1;
    }
}
