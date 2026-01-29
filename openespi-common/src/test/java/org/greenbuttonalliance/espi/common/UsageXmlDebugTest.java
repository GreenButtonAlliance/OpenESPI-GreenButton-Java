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

package org.greenbuttonalliance.espi.common;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.utils.EspiNamespacePrefixMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Debug test for Usage Domain XML generation (ESPI namespace only).
 * <p>
 * Tests UsageAtomEntryDto with usage.xsd resources to validate:
 * - Only xmlns:espi="http://naesb.org/espi" is declared
 * - xmlns:cust (customer namespace) is NOT declared
 * - Atom namespace is default (no prefix)
 * <p>
 * Per NAESB ESPI standard, usage and customer domains are mutually exclusive.
 */
@DisplayName("Usage Domain XML Debug Test - JAXB")
class UsageXmlDebugTest {

    private JAXBContext jaxbContext;

    @BeforeEach
    void setUp() throws JAXBException {
        // Initialize Jakarta JAXB Context with USAGE DOMAIN ONLY
        // This ensures ONLY espi namespace is declared, NOT cust namespace
        // Note: Excluding AtomFeedDto to prevent JAXB from discovering CustomerAtomEntryDto
        jaxbContext = JAXBContext.newInstance(
                // Atom protocol classes
                org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto.class,  // ONLY usage, NOT customer
                org.greenbuttonalliance.espi.common.dto.atom.LinkDto.class,

                // Usage domain classes (http://naesb.org/espi)
                org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.AuthorizationDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.SubscriptionDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.LineItemDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.ServiceDeliveryPointDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.ReadingQualityDto.class,
                org.greenbuttonalliance.espi.common.dto.usage.IntervalReadingDto.class,
                org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto.class
        );
    }

    /**
     * Creates a marshaller configured for usage domain resources.
     */
    private Marshaller createMarshallerForUsageDomain() throws JAXBException {
        // Required namespaces: Atom + espi ONLY (no cust)
        Set<String> requiredNamespaces = new HashSet<>();
        requiredNamespaces.add("http://www.w3.org/2005/Atom");
        requiredNamespaces.add("http://naesb.org/espi");

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);

        // Set namespace prefix mapper
        EspiNamespacePrefixMapper prefixMapper = new EspiNamespacePrefixMapper(requiredNamespaces);
        try {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", prefixMapper);
        } catch (Exception e) {
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", prefixMapper);
        }

        return marshaller;
    }

    @Test
    @DisplayName("Should declare ONLY espi namespace (NOT customer namespace)")
    void shouldDeclareEspiNamespaceOnly() throws Exception {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            new byte[]{0x01},
            null, (short) 1,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null
        );
        UsageAtomEntryDto entry = new UsageAtomEntryDto("urn:uuid:550e8400-e29b-51d4-a716-446655440011", "Usage Test", usagePoint);

        // Act
        Marshaller marshaller = createMarshallerForUsageDomain();
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

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

        // Assert - Atom namespace declared with atom prefix
        assertThat(xml)
            .as("XML should declare Atom namespace with atom prefix")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        // Assert - Usage content with espi prefix
        assertThat(xml)
            .as("UsagePoint should use espi prefix")
            .contains("<espi:UsagePoint");
    }

    @Test
    @DisplayName("Should use espi prefix for UsagePoint elements")
    void shouldUseEspiPrefixForUsagePoint() throws Exception {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            new byte[]{0x02},
            null, (short) 1,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null
        );
        UsageAtomEntryDto entry = new UsageAtomEntryDto("urn:uuid:550e8400-e29b-51d4-a716-446655440013", "Prefix Test", usagePoint);

        // Act
        Marshaller marshaller = createMarshallerForUsageDomain();
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Assert
        assertThat(xml).contains("<espi:UsagePoint>");
        assertThat(xml).contains("<espi:roleFlags>");
        assertThat(xml).contains("<espi:status>");
        assertThat(xml).contains("</espi:UsagePoint>");
    }

    @Test
    @DisplayName("Should use Atom as default namespace (no prefix on entry/id/title)")
    void shouldUseAtomAsDefaultNamespace() throws Exception {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            new byte[]{0x03},
            null, (short) 1,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null
        );
        UsageAtomEntryDto entry = new UsageAtomEntryDto("urn:uuid:550e8400-e29b-51d4-a716-446655440015", "Atom Test", usagePoint);

        // Act
        Marshaller marshaller = createMarshallerForUsageDomain();
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Assert - Atom elements use atom prefix
        assertThat(xml)
            .as("entry element should use atom prefix")
            .contains("<atom:entry");

        assertThat(xml).contains("<atom:id>urn:uuid:550e8400-e29b-51d4-a716-446655440015</atom:id>");
        assertThat(xml).contains("<atom:title>Atom Test</atom:title>");
        assertThat(xml).doesNotContain("ns5:id");
        assertThat(xml).doesNotContain("ns5:title");
        assertThat(xml).doesNotContain("ns3:id");
        assertThat(xml).doesNotContain("ns3:title");
    }

    @Test
    @DisplayName("Debug: See complete Usage Domain XML structure")
    void debugCompleteUsageDomainXml() throws Exception {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            new byte[]{0x01, 0x02},  // roleFlags
            null,  // serviceCategory
            (short) 1,  // status
            null, null, null, null, null,  // serviceDeliveryPoint through estimatedLoad
            null, null, null, null,  // grounded through minimalUsageExpected
            null, null, null, null, null,  // nominalServiceVoltage through ratedPower
            null, null, null, null,  // readCycle through servicePriority
            null, null, null, null, null  // pnodeRefs through electricPowerQualitySummaries
        );

        UsageAtomEntryDto entry = new UsageAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440000",
            "Residential Electric Service - Usage Domain",
            usagePoint
        );

        // Act
        Marshaller marshaller = createMarshallerForUsageDomain();

        // Add listener for debugging
        marshaller.setListener(new Marshaller.Listener() {
            @Override
            public void beforeMarshal(Object source) {
                System.out.println("[JAXB] beforeMarshal: " + source.getClass().getSimpleName());
            }
        });

        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Debug output
        System.out.println("\n========== Complete Usage Domain XML ==========");
        System.out.println(xml);
        System.out.println("===============================================\n");

        // Assert comprehensive structure
        assertThat(xml).contains("xmlns:espi=\"http://naesb.org/espi\"");
        assertThat(xml).doesNotContain("xmlns:cust");
        assertThat(xml).contains("<espi:UsagePoint>");
        assertThat(xml).contains("urn:uuid:550e8400-e29b-51d4-a716-446655440000");
        assertThat(xml).contains("Residential Electric Service - Usage Domain");
    }
}
