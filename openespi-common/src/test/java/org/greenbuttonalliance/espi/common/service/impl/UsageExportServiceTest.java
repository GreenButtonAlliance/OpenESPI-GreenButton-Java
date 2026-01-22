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

package org.greenbuttonalliance.espi.common.service.impl;

import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for UsageExportService namespace handling.
 * <p>
 * Verifies that usage domain exports:
 * - Declare ONLY Atom and ESPI namespaces (NO customer namespace)
 * - Use Atom as default namespace (no prefix on entry, id, title)
 * - Use espi: prefix for usage domain elements
 */
@DisplayName("UsageExportService Namespace Tests")
class UsageExportServiceTest {

    private UsageExportService usageExportService;

    @BeforeEach
    void setUp() {
        usageExportService = new UsageExportService();
        // Manually initialize since we're not using Spring context
        usageExportService.init();
    }

    @Test
    @DisplayName("Should declare ONLY espi namespace (NOT customer namespace)")
    void shouldDeclareEspiNamespaceOnly() {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440010",
            new byte[]{0x01},
            null, (short) 1,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null
        );
        UsageAtomEntryDto entry = new UsageAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440011",
            "Usage Test",
            usagePoint
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        usageExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Debug output
        System.out.println("\n========== Usage Domain XML Output ==========");
        System.out.println(xml);
        System.out.println("=============================================\n");

        // Assert - ESPI namespace PRESENT
        assertThat(xml)
            .as("XML should declare espi namespace")
            .contains("xmlns:espi=\"http://naesb.org/espi\"");

        // Assert - Customer namespace ABSENT
        assertThat(xml)
            .as("XML should NOT declare customer namespace")
            .doesNotContain("xmlns:cust")
            .doesNotContain("http://naesb.org/espi/customer");

        // Assert - Atom namespace is declared with atom prefix
        assertThat(xml)
            .as("XML should declare Atom namespace with atom prefix")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        // Assert - Usage content with espi prefix
        assertThat(xml)
            .as("UsagePoint should use espi prefix")
            .contains("<espi:UsagePoint");
    }

    @Test
    @DisplayName("Should use atom prefix for Atom elements")
    void shouldUseAtomPrefixForAtomElements() {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440012",
            new byte[]{0x02},
            null, (short) 1,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null
        );
        UsageAtomEntryDto entry = new UsageAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440013",
            "Atom Prefix Test",
            usagePoint
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        usageExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Assert - Atom elements WITH atom: prefix
        assertThat(xml)
            .as("entry element should have atom prefix")
            .contains("<atom:entry");

        assertThat(xml)
            .as("id element should have atom prefix")
            .contains("<atom:id>urn:uuid:550e8400-e29b-51d4-a716-446655440013</atom:id>");

        assertThat(xml)
            .as("title element should have atom prefix")
            .contains("<atom:title>Atom Prefix Test</atom:title>");

        // Assert - NO ns3/ns5 generic prefixes on Atom elements
        assertThat(xml)
            .as("Should NOT have ns3 or ns5 generic prefixes")
            .doesNotContain("ns3:id")
            .doesNotContain("ns3:title")
            .doesNotContain("ns5:id")
            .doesNotContain("ns5:title")
            .doesNotContain("ns3:entry")
            .doesNotContain("ns5:entry");
    }

    @Test
    @DisplayName("Should use espi prefix for UsagePoint elements")
    void shouldUseEspiPrefixForUsagePoint() {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440014",
            new byte[]{0x03},
            null, (short) 1,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null
        );
        UsageAtomEntryDto entry = new UsageAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440015",
            "ESPI Prefix Test",
            usagePoint
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        usageExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Assert
        assertThat(xml).contains("<espi:UsagePoint>");
        assertThat(xml).contains("<espi:roleFlags>");
        assertThat(xml).contains("<espi:status>");
        assertThat(xml).contains("</espi:UsagePoint>");
    }

    @Test
    @DisplayName("Should produce valid ESPI XML structure")
    void shouldProduceValidEspiXmlStructure() {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:debug-usage",
            new byte[]{0x01, 0x02},  // roleFlags
            null,  // serviceCategory
            (short) 1,  // status
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null
        );

        UsageAtomEntryDto entry = new UsageAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440000",
            "Residential Electric Service - Usage Domain",
            usagePoint
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        usageExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Debug output
        System.out.println("\n========== Complete Usage Domain XML ==========");
        System.out.println(xml);
        System.out.println("===============================================\n");

        // Assert comprehensive structure
        assertThat(xml).contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");
        assertThat(xml).contains("xmlns:espi=\"http://naesb.org/espi\"");
        assertThat(xml).doesNotContain("xmlns:cust");  // No customer namespace pollution
        assertThat(xml).doesNotContain("ns3:");  // No generic prefixes
        assertThat(xml).doesNotContain("ns5:");  // No generic prefixes
        assertThat(xml).contains("<atom:entry");  // Atom elements use atom prefix
        assertThat(xml).contains("<espi:UsagePoint>");  // Usage elements use espi prefix
        assertThat(xml).contains("urn:uuid:550e8400-e29b-51d4-a716-446655440000");
        assertThat(xml).contains("Residential Electric Service - Usage Domain");
    }
}