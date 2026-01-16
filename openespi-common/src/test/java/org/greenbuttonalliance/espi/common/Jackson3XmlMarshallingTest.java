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

import com.fasterxml.jackson.annotation.JsonInclude;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.util.StdDateFormat;
import tools.jackson.dataformat.xml.XmlAnnotationIntrospector;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Jackson 3 XML marshalling tests to verify JAXB annotation processing with ESPI data.
 * Tests marshal/unmarshal round-trip with realistic data structures using Jackson 3 XmlMapper.
 */
@DisplayName("Jackson 3 XML Marshalling Tests")
class Jackson3XmlMarshallingTest {

    private XmlMapper xmlMapper;

    @BeforeEach
    void setUp() {
        // Initialize Jackson 3 XmlMapper with JAXB annotation support
        AnnotationIntrospector intr = XmlAnnotationIntrospector.Pair.instance(
            new JakartaXmlBindAnnotationIntrospector(),
            new JacksonAnnotationIntrospector()
        );

        xmlMapper = XmlMapper.xmlBuilder()
            .annotationIntrospector(intr)
            .addModule(new JakartaXmlBindAnnotationModule()
                .setNonNillableInclusion(JsonInclude.Include.NON_EMPTY))
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
            .disable(XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL)
            .defaultDateFormat(new StdDateFormat())
            .build();
    }

    @Test
    @DisplayName("Should marshal UsagePointDto with realistic data")
    void shouldMarshalUsagePointWithRealisticData() throws Exception {
        // Create a UsagePointDto with realistic ESPI data
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-usage-point",
            new byte[]{0x01, 0x04}, // Electricity consumer role flags
            null, // serviceCategory
            (short) 1, // Active status
            null, null, null, null, null, // serviceDeliveryPoint, amiBillingReady, checkBilling, connectionState, estimatedLoad
            null, null, null, null, // grounded, isSdp, isVirtual, minimalUsageExpected
            null, null, null, null, null, // nominalServiceVoltage, outageRegion, phaseCode, ratedCurrent, ratedPower
            null, null, null, null, // readCycle, readRoute, serviceDeliveryRemark, servicePriority
            null, null, null, null, null // pnodeRefs, aggregatedNodeRefs, meterReadings, usageSummaries, electricPowerQualitySummaries
        );

        // Wrap in Atom entry with description as title (IdentifiedObject fields handled by Atom layer)
        AtomEntryDto entry = new AtomEntryDto("urn:uuid:test-usage-point", "Residential Electric Service", usagePoint);

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(entry);

        // Verify XML structure
        assertThat(xml).contains("entry"); // Now wrapping in Atom entry
        assertThat(xml).contains("UsagePoint");
        assertThat(xml).contains("http://naesb.org/espi");
        assertThat(xml).contains("Residential Electric Service"); // In Atom title
        assertThat(xml).containsPattern("<status[^>]*>1</status>"); // May have xmlns attribute
    }

    @Test
    @DisplayName("Should perform round-trip marshalling for UsagePointDto")
    void shouldPerformRoundTripMarshallingForUsagePoint() throws Exception {
        // Create original UsagePoint with comprehensive data
        UsagePointDto originalUsagePoint = new UsagePointDto(
            "urn:uuid:commercial-gas-point",
            new byte[]{0x02, 0x08}, // Gas consumer role flags
            null, // serviceCategory
            (short) 1, // Active status
            null, null, null, null, null, // serviceDeliveryPoint, amiBillingReady, checkBilling, connectionState, estimatedLoad
            null, null, null, null, // grounded, isSdp, isVirtual, minimalUsageExpected
            null, null, null, null, null, // nominalServiceVoltage, outageRegion, phaseCode, ratedCurrent, ratedPower
            null, null, null, null, // readCycle, readRoute, serviceDeliveryRemark, servicePriority
            null, null, null, null, null // pnodeRefs, aggregatedNodeRefs, meterReadings, usageSummaries, electricPowerQualitySummaries
        );

        // Wrap in Atom entry with description as title (IdentifiedObject fields handled by Atom layer)
        AtomEntryDto originalEntry = new AtomEntryDto("urn:uuid:commercial-gas-point", "Commercial Gas Service", originalUsagePoint);

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(originalEntry);

        // Unmarshal back from XML using Jackson 3
        AtomEntryDto roundTripEntry = xmlMapper.readValue(xml, AtomEntryDto.class);

        // Verify data integrity survived round trip
        assertThat(roundTripEntry.title()).isEqualTo(originalEntry.title()); // Description is in Atom title
        UsagePointDto roundTripUsagePoint = (UsagePointDto) roundTripEntry.content();
        assertThat(roundTripUsagePoint.status()).isEqualTo(originalUsagePoint.status());
        assertThat(roundTripUsagePoint.roleFlags()).isEqualTo(originalUsagePoint.roleFlags());
    }

    @Test
    @DisplayName("Should handle empty UsagePointDto without errors")
    void shouldHandleEmptyUsagePointWithoutErrors() throws Exception {
        // Create empty UsagePoint
        UsagePointDto empty = new UsagePointDto(
            null, // uuid
            null, // roleFlags
            null, // serviceCategory
            null, // status
            null, null, null, null, null, // serviceDeliveryPoint, amiBillingReady, checkBilling, connectionState, estimatedLoad
            null, null, null, null, // grounded, isSdp, isVirtual, minimalUsageExpected
            null, null, null, null, null, // nominalServiceVoltage, outageRegion, phaseCode, ratedCurrent, ratedPower
            null, null, null, null, // readCycle, readRoute, serviceDeliveryRemark, servicePriority
            null, null, null, null, null // pnodeRefs, aggregatedNodeRefs, meterReadings, usageSummaries, electricPowerQualitySummaries
        );

        // Wrap in Atom entry (IdentifiedObject fields handled by Atom layer)
        AtomEntryDto entry = new AtomEntryDto(null, null, empty);

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(entry);

        // Should still contain basic structure
        assertThat(xml).contains("entry");
        assertThat(xml).contains("UsagePoint");
        assertThat(xml).contains("http://naesb.org/espi");

        // Unmarshal back using Jackson 3
        AtomEntryDto roundTripEntry = xmlMapper.readValue(xml, AtomEntryDto.class);

        // Should not throw exceptions
        assertThat(roundTripEntry).isNotNull();
        assertThat(roundTripEntry.content()).isNotNull();
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() throws Exception {
        // Create UsagePoint with some null values
        UsagePointDto withNulls = new UsagePointDto(
            "urn:uuid:test-nulls",
            null, // Null role flags
            null, // serviceCategory
            (short) 1, // Non-null status
            null, null, null, null, null, // serviceDeliveryPoint, amiBillingReady, checkBilling, connectionState, estimatedLoad
            null, null, null, null, // grounded, isSdp, isVirtual, minimalUsageExpected
            null, null, null, null, null, // nominalServiceVoltage, outageRegion, phaseCode, ratedCurrent, ratedPower
            null, null, null, null, // readCycle, readRoute, serviceDeliveryRemark, servicePriority
            null, null, null, null, null // pnodeRefs, aggregatedNodeRefs, meterReadings, usageSummaries, electricPowerQualitySummaries
        );

        // Wrap in Atom entry with null description/title (IdentifiedObject fields handled by Atom layer)
        AtomEntryDto entry = new AtomEntryDto("urn:uuid:test-nulls", null, withNulls);

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(entry);

        // Unmarshal back using Jackson 3
        AtomEntryDto roundTripEntry = xmlMapper.readValue(xml, AtomEntryDto.class);

        // Verify nulls are preserved
        assertThat(roundTripEntry.title()).isNull(); // Null description is in Atom title
        UsagePointDto roundTrip = (UsagePointDto) roundTripEntry.content();
        assertThat(roundTrip.roleFlags()).isNull();
        assertThat(roundTrip.status()).isEqualTo(withNulls.status());
    }

    @Test
    @DisplayName("Should include proper XML namespaces")
    void shouldIncludeProperXmlNamespaces() throws Exception {
        // Create UsagePoint
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-namespaces",
            null, // roleFlags
            null, // serviceCategory
            null, // status
            null, null, null, null, null, // serviceDeliveryPoint, amiBillingReady, checkBilling, connectionState, estimatedLoad
            null, null, null, null, // grounded, isSdp, isVirtual, minimalUsageExpected
            null, null, null, null, null, // nominalServiceVoltage, outageRegion, phaseCode, ratedCurrent, ratedPower
            null, null, null, null, // readCycle, readRoute, serviceDeliveryRemark, servicePriority
            null, null, null, null, null // pnodeRefs, aggregatedNodeRefs, meterReadings, usageSummaries, electricPowerQualitySummaries
        );

        // Wrap in Atom entry with description as title (IdentifiedObject fields handled by Atom layer)
        AtomEntryDto entry = new AtomEntryDto("urn:uuid:test-namespaces", "Test Service", usagePoint);

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(entry);

        // Verify namespace declarations
        assertThat(xml).contains("xmlns");
        assertThat(xml).contains("http://naesb.org/espi");
        assertThat(xml).contains("http://www.w3.org/2005/Atom"); // Atom namespace

        // Verify no legacy namespaces
        assertThat(xml).doesNotContain("legacy");
        assertThat(xml).doesNotContain("deprecated");
    }

    @Test
    @DisplayName("Should marshal special characters correctly")
    void shouldMarshalSpecialCharactersCorrectly() throws Exception {
        // Create UsagePoint with special characters in description (will be in Atom title)
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-special-chars",
            null, // roleFlags
            null, // serviceCategory
            null, // status
            null, null, null, null, null, // serviceDeliveryPoint, amiBillingReady, checkBilling, connectionState, estimatedLoad
            null, null, null, null, // grounded, isSdp, isVirtual, minimalUsageExpected
            null, null, null, null, null, // nominalServiceVoltage, outageRegion, phaseCode, ratedCurrent, ratedPower
            null, null, null, null, // readCycle, readRoute, serviceDeliveryRemark, servicePriority
            null, null, null, null, null // pnodeRefs, aggregatedNodeRefs, meterReadings, usageSummaries, electricPowerQualitySummaries
        );

        // Wrap in Atom entry with special characters in title (IdentifiedObject fields handled by Atom layer)
        AtomEntryDto entry = new AtomEntryDto("urn:uuid:test-special-chars", "Service & Co. <Electric> \"Smart\" Meter", usagePoint);

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(entry);

        // Verify XML escaping
        assertThat(xml)
            .satisfiesAnyOf(
                s -> assertThat(s).contains("&amp;"),
                s -> assertThat(s).contains("Service &amp; Co.")
            );
        assertThat(xml).contains("&lt;Electric>");  // < is escaped, > in quoted text may not be

        // Unmarshal back and verify data integrity using Jackson 3
        AtomEntryDto roundTripEntry = xmlMapper.readValue(xml, AtomEntryDto.class);

        assertThat(roundTripEntry.title()).isEqualTo(entry.title()); // Description is in Atom title
    }

    @Test
    @DisplayName("Should not throw exceptions during marshalling")
    void shouldNotThrowExceptionsDuringMarshalling() {
        // Create UsagePoint
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-no-exceptions",
            null, // roleFlags
            null, // serviceCategory
            null, // status
            null, null, null, null, null, // serviceDeliveryPoint, amiBillingReady, checkBilling, connectionState, estimatedLoad
            null, null, null, null, // grounded, isSdp, isVirtual, minimalUsageExpected
            null, null, null, null, null, // nominalServiceVoltage, outageRegion, phaseCode, ratedCurrent, ratedPower
            null, null, null, null, // readCycle, readRoute, serviceDeliveryRemark, servicePriority
            null, null, null, null, null // pnodeRefs, aggregatedNodeRefs, meterReadings, usageSummaries, electricPowerQualitySummaries
        );

        // Wrap in Atom entry with description as title (IdentifiedObject fields handled by Atom layer)
        AtomEntryDto entry = new AtomEntryDto("urn:uuid:test-no-exceptions", "Test Service", usagePoint);

        // Verify marshalling does not throw using Jackson 3
        assertThatCode(() -> xmlMapper.writeValueAsString(entry))
            .doesNotThrowAnyException();
    }
}
