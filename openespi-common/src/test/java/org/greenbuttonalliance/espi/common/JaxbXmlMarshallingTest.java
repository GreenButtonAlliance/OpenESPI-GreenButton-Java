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
import jakarta.xml.bind.Unmarshaller;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JAXB XML marshalling tests to verify JAXB annotation processing with ESPI data.
 * Tests marshal/unmarshal round-trip with realistic data structures using Jakarta JAXB Marshaller.
 */
@DisplayName("JAXB XML Marshalling Tests")
class JaxbXmlMarshallingTest {

    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    @BeforeEach
    void setUp() throws JAXBException {
        // Initialize Jakarta JAXB Marshaller for UsageAtomEntryDto
        JAXBContext jaxbContext = JAXBContext.newInstance(UsageAtomEntryDto.class);
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // Don't write XML declaration

        unmarshaller = jaxbContext.createUnmarshaller();
    }

    @Test
    @DisplayName("Should marshal UsagePointDto with realistic data")
    void shouldMarshalUsagePointWithRealisticData() throws Exception {
        // Create a UsagePointDto with realistic ESPI data
        UsagePointDto usagePoint = new UsagePointDto(
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
        UsageAtomEntryDto entry = new UsageAtomEntryDto("urn:uuid:test-usage-point", "Residential Electric Service", usagePoint);

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Verify XML structure with namespace prefixes
        assertThat(xml).contains("entry"); // Now wrapping in Atom entry
        assertThat(xml).contains("UsagePoint");
        assertThat(xml).contains("http://naesb.org/espi");
        assertThat(xml).contains("Residential Electric Service"); // In Atom title
        assertThat(xml).containsPattern("<(espi:)?status[^>]*>1</(espi:)?status>"); // May have espi: prefix
    }

    @Test
    @DisplayName("Should perform round-trip marshalling for UsagePointDto")
    void shouldPerformRoundTripMarshallingForUsagePoint() throws Exception {
        // Create original UsagePoint with comprehensive data
        UsagePointDto originalUsagePoint = new UsagePointDto(
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
        UsageAtomEntryDto originalEntry = new UsageAtomEntryDto("urn:uuid:commercial-gas-point", "Commercial Gas Service", originalUsagePoint);

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(originalEntry, writer);
        String xml = writer.toString();

        // Unmarshal back from XML using JAXB
        AtomEntryDto roundTripEntry = (AtomEntryDto) unmarshaller.unmarshal(new StringReader(xml));

        // Verify data integrity survived round trip
        assertThat(roundTripEntry.getTitle()).isEqualTo(originalEntry.getTitle()); // Description is in Atom title
        UsagePointDto roundTripUsagePoint = (UsagePointDto) roundTripEntry.getContent();
        assertThat(roundTripUsagePoint.getStatus()).isEqualTo(originalUsagePoint.getStatus());
        assertThat(roundTripUsagePoint.getRoleFlags()).isEqualTo(originalUsagePoint.getRoleFlags());
    }

    @Test
    @DisplayName("Should handle empty UsagePointDto without errors")
    void shouldHandleEmptyUsagePointWithoutErrors() throws Exception {
        // Create empty UsagePoint
        UsagePointDto empty = new UsagePointDto(
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
        UsageAtomEntryDto entry = new UsageAtomEntryDto(null, null, empty);

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Should still contain basic structure
        assertThat(xml).contains("entry");
        assertThat(xml).contains("UsagePoint");
        assertThat(xml).contains("http://naesb.org/espi");

        // Unmarshal back using JAXB
        AtomEntryDto roundTripEntry = (AtomEntryDto) unmarshaller.unmarshal(new StringReader(xml));

        // Should not throw exceptions
        assertThat(roundTripEntry).isNotNull();
        assertThat(roundTripEntry.getContent()).isNotNull();
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() throws Exception {
        // Create UsagePoint with some null values
        UsagePointDto withNulls = new UsagePointDto(
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
        UsageAtomEntryDto entry = new UsageAtomEntryDto("urn:uuid:test-nulls", null, withNulls);

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Unmarshal back using JAXB
        AtomEntryDto roundTripEntry = (AtomEntryDto) unmarshaller.unmarshal(new StringReader(xml));

        // Verify nulls are preserved
        assertThat(roundTripEntry.getTitle()).isNull(); // Null description is in Atom title
        UsagePointDto roundTrip = (UsagePointDto) roundTripEntry.getContent();
        assertThat(roundTrip.getRoleFlags()).isNull();
        assertThat(roundTrip.getStatus()).isEqualTo(withNulls.getStatus());
    }

    @Test
    @DisplayName("Should include proper XML namespaces")
    void shouldIncludeProperXmlNamespaces() throws Exception {
        // Create UsagePoint
        UsagePointDto usagePoint = new UsagePointDto(
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
        UsageAtomEntryDto entry = new UsageAtomEntryDto("urn:uuid:test-namespaces", "Test Service", usagePoint);

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

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
        UsageAtomEntryDto entry = new UsageAtomEntryDto("urn:uuid:test-special-chars", "Service & Co. <Electric> \"Smart\" Meter", usagePoint);

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Verify XML escaping
        assertThat(xml)
            .satisfiesAnyOf(
                s -> assertThat(s).contains("&amp;"),
                s -> assertThat(s).contains("Service &amp; Co.")
            );
        // XML entities may be escaped differently in element content vs attributes
        assertThat(xml).containsPattern("(&lt;Electric&gt;|<Electric>)");  // May or may not escape in title content

        // Unmarshal back and verify data integrity using JAXB
        AtomEntryDto roundTripEntry = (AtomEntryDto) unmarshaller.unmarshal(new StringReader(xml));

        assertThat(roundTripEntry.getTitle()).isEqualTo(entry.getTitle()); // Description is in Atom title
    }

    @Test
    @DisplayName("Should not throw exceptions during marshalling")
    void shouldNotThrowExceptionsDuringMarshalling() {
        // Create UsagePoint
        UsagePointDto usagePoint = new UsagePointDto(
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
        UsageAtomEntryDto entry = new UsageAtomEntryDto("urn:uuid:test-no-exceptions", "Test Service", usagePoint);

        // Verify marshalling does not throw using JAXB
        assertThatCode(() -> {
            StringWriter writer = new StringWriter();
            marshaller.marshal(entry, writer);
        }).doesNotThrowAnyException();
    }
}
