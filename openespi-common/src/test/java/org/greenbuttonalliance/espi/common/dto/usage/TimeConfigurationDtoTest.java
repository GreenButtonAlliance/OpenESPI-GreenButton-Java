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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * XML marshalling/unmarshalling tests for TimeConfigurationDto.
 * Verifies JAXB processes annotations correctly for ESPI 4.0 schema compliance.
 */
@DisplayName("TimeConfigurationDto XML Marshalling Tests")
class TimeConfigurationDtoTest {

    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    @BeforeEach
    void setUp() throws JAXBException {
        // Initialize Jakarta JAXB Marshaller for TimeConfigurationDto
        JAXBContext jaxbContext = JAXBContext.newInstance(TimeConfigurationDto.class);
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

        unmarshaller = jaxbContext.createUnmarshaller();
    }

    @Test
    @DisplayName("Should marshal TimeConfigurationDto with realistic timezone data")
    void shouldMarshalTimeConfigurationWithRealisticData() throws Exception {
        // Create TimeConfigurationDto with Pacific Time (UTC-8)
        TimeConfigurationDto timeConfig = new TimeConfigurationDto(
            null, // id (transient)
            "urn:uuid:550e8400-e29b-51d4-a716-446655440000", // uuid (Version-5)
            new byte[]{0x01, 0x0B, 0x05, 0x00, 0x02, 0x00}, // dstEndRule (Nov 1st, 2am)
            3600L, // dstOffset (1 hour in seconds)
            new byte[]{0x01, 0x03, 0x02, 0x00, 0x02, 0x00}, // dstStartRule (Mar 2nd, 2am)
            -28800L  // tzOffset (UTC-8 in seconds)
        );

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(timeConfig, writer);
        String xml = writer.toString();

        // Verify XML structure
        assertThat(xml).contains("TimeConfiguration");
        assertThat(xml).contains("http://naesb.org/espi");
        // Note: mRID/uuid is handled by Atom wrapper, not the ESPI resource itself
        assertThat(xml).contains("tzOffset"); // Element should be present
        assertThat(xml).contains("-28800"); // Timezone value
        assertThat(xml).contains("dstOffset");
        assertThat(xml).contains("3600");
    }

    @Test
    @DisplayName("Should perform round-trip marshalling for TimeConfigurationDto")
    void shouldPerformRoundTripMarshallingForTimeConfiguration() throws Exception {
        // Create original TimeConfiguration with Eastern Time (UTC-5)
        TimeConfigurationDto original = new TimeConfigurationDto(
            null, // id
            "urn:uuid:550e8400-e29b-51d4-a716-446655440001", // uuid (Version-5)
            new byte[]{0x01, 0x0B, 0x01, 0x00, 0x02, 0x00}, // dstEndRule
            3600L, // dstOffset
            new byte[]{0x01, 0x03, 0x08, 0x00, 0x02, 0x00}, // dstStartRule
            -18000L  // tzOffset (UTC-5)
        );

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(original, writer);
        String xml = writer.toString();

        // Unmarshal back from XML using JAXB
        TimeConfigurationDto roundTrip = (TimeConfigurationDto) unmarshaller.unmarshal(new StringReader(xml));

        // Verify data integrity survived round trip
        // Note: uuid is @XmlTransient (handled by Atom wrapper), so it won't survive round trip
        assertThat(roundTrip.getTzOffset()).isEqualTo(original.getTzOffset());
        assertThat(roundTrip.getDstOffset()).isEqualTo(original.getDstOffset());
        assertThat(roundTrip.getDstStartRule()).isEqualTo(original.getDstStartRule());
        assertThat(roundTrip.getDstEndRule()).isEqualTo(original.getDstEndRule());
    }

    @Test
    @DisplayName("Should handle TimeConfigurationDto with only timezone offset")
    void shouldHandleTimeConfigurationWithOnlyTimezoneOffset() throws Exception {
        // Create TimeConfiguration with only timezone offset (no DST)
        TimeConfigurationDto simple = new TimeConfigurationDto(
            null, // id
            "urn:uuid:550e8400-e29b-51d4-a716-446655440002", // uuid (Version-5)
            null, // dstEndRule
            null, // dstOffset
            null, // dstStartRule
            7200L  // tzOffset (UTC+2)
        );

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(simple, writer);
        String xml = writer.toString();

        // Verify XML structure
        assertThat(xml).contains("TimeConfiguration");
        assertThat(xml).contains("tzOffset"); // Element should be present
        assertThat(xml).contains("7200"); // Value should be present
        assertThat(xml).doesNotContain("dstOffset");
        assertThat(xml).doesNotContain("dstStartRule");
        assertThat(xml).doesNotContain("dstEndRule");

        // Unmarshal back using JAXB
        TimeConfigurationDto roundTrip = (TimeConfigurationDto) unmarshaller.unmarshal(new StringReader(xml));

        // Verify data integrity
        assertThat(roundTrip.getTzOffset()).isEqualTo(simple.getTzOffset());
        assertThat(roundTrip.getDstOffset()).isNull();
        assertThat(roundTrip.getDstStartRule()).isNull();
        assertThat(roundTrip.getDstEndRule()).isNull();
    }

    @Test
    @DisplayName("Should handle empty TimeConfigurationDto without errors")
    void shouldHandleEmptyTimeConfigurationWithoutErrors() throws Exception {
        // Create empty TimeConfiguration
        TimeConfigurationDto empty = new TimeConfigurationDto();

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(empty, writer);
        String xml = writer.toString();

        // Should still contain basic structure
        assertThat(xml).contains("TimeConfiguration");
        assertThat(xml).contains("http://naesb.org/espi");

        // Unmarshal back using JAXB
        TimeConfigurationDto roundTrip = (TimeConfigurationDto) unmarshaller.unmarshal(new StringReader(xml));

        // Should not throw exceptions
        assertThat(roundTrip).isNotNull();
    }

    @Test
    @DisplayName("Should include proper XML namespaces and element order")
    void shouldIncludeProperXmlNamespacesAndElementOrder() throws Exception {
        // Create TimeConfiguration with all fields
        TimeConfigurationDto timeConfig = new TimeConfigurationDto(
            null,
            "urn:uuid:550e8400-e29b-51d4-a716-446655440003", // uuid (Version-5)
            new byte[]{0x01}, // dstEndRule
            3600L, // dstOffset
            new byte[]{0x01}, // dstStartRule
            -28800L  // tzOffset
        );

        // Marshal to XML using JAXB
        StringWriter writer = new StringWriter();
        marshaller.marshal(timeConfig, writer);
        String xml = writer.toString();

        // Verify namespace declarations
        assertThat(xml).contains("xmlns");
        assertThat(xml).contains("http://naesb.org/espi");

        // Verify element order matches XSD propOrder: dstEndRule, dstOffset, dstStartRule, tzOffset
        int dstEndRulePos = xml.indexOf("dstEndRule");
        int dstOffsetPos = xml.indexOf("dstOffset");
        int dstStartRulePos = xml.indexOf("dstStartRule");
        int tzOffsetPos = xml.indexOf("tzOffset");

        assertThat(dstEndRulePos).isLessThan(dstOffsetPos);
        assertThat(dstOffsetPos).isLessThan(dstStartRulePos);
        assertThat(dstStartRulePos).isLessThan(tzOffsetPos);
    }

    @Test
    @DisplayName("Should handle byte array cloning for DST rules")
    void shouldHandleByteArrayCloningForDstRules() {
        // Create original byte arrays
        byte[] originalStartRule = new byte[]{0x01, 0x03, 0x08, 0x00, 0x02, 0x00};
        byte[] originalEndRule = new byte[]{0x01, 0x0B, 0x01, 0x00, 0x02, 0x00};

        // Create TimeConfiguration
        TimeConfigurationDto timeConfig = new TimeConfigurationDto(
            null, "urn:uuid:clone-test",
            originalEndRule, 3600L, originalStartRule, -18000L
        );

        // Get byte arrays via getters (should be cloned)
        byte[] retrievedStartRule = timeConfig.getDstStartRule();
        byte[] retrievedEndRule = timeConfig.getDstEndRule();

        // Verify arrays are equal but not same instance
        assertArrayEquals(originalStartRule, retrievedStartRule, "Start rule content should match");
        assertArrayEquals(originalEndRule, retrievedEndRule, "End rule content should match");
        assertNotSame(originalStartRule, retrievedStartRule, "Start rule should be cloned");
        assertNotSame(originalEndRule, retrievedEndRule, "End rule should be cloned");

        // Modifying retrieved arrays should not affect original
        retrievedStartRule[0] = (byte) 0xFF;
        assertNotEquals(retrievedStartRule[0], timeConfig.getDstStartRule()[0],
                       "Modifying cloned array should not affect original");
    }

    @Test
    @DisplayName("Should calculate timezone offset in hours correctly")
    void shouldCalculateTimezoneOffsetInHoursCorrectly() {
        // Test various timezone offsets
        TimeConfigurationDto utcMinusEight = new TimeConfigurationDto(-28800L); // UTC-8
        assertEquals(-8.0, utcMinusEight.getTzOffsetInHours(), 0.01, "UTC-8 should be -8 hours");

        TimeConfigurationDto utcPlusFivePointFive = new TimeConfigurationDto(19800L); // UTC+5:30 (India)
        assertEquals(5.5, utcPlusFivePointFive.getTzOffsetInHours(), 0.01, "UTC+5:30 should be 5.5 hours");

        TimeConfigurationDto utc = new TimeConfigurationDto(0L); // UTC
        assertEquals(0.0, utc.getTzOffsetInHours(), 0.01, "UTC should be 0 hours");

        TimeConfigurationDto noOffset = new TimeConfigurationDto(null, null, null, null, null, null);
        assertNull(noOffset.getTzOffsetInHours(), "Null offset should return null hours");
    }

    @Test
    @DisplayName("Should calculate DST offset in hours correctly")
    void shouldCalculateDstOffsetInHoursCorrectly() {
        TimeConfigurationDto oneHourDst = new TimeConfigurationDto(
            null, null, null, 3600L, null, -18000L
        );
        assertEquals(1.0, oneHourDst.getDstOffsetInHours(), 0.01, "3600 seconds should be 1 hour");

        TimeConfigurationDto halfHourDst = new TimeConfigurationDto(
            null, null, null, 1800L, null, -18000L
        );
        assertEquals(0.5, halfHourDst.getDstOffsetInHours(), 0.01, "1800 seconds should be 0.5 hours");

        TimeConfigurationDto noDst = new TimeConfigurationDto(-18000L);
        assertNull(noDst.getDstOffsetInHours(), "Null DST offset should return null hours");
    }

    @Test
    @DisplayName("Should calculate effective offset including DST")
    void shouldCalculateEffectiveOffsetIncludingDst() {
        // Pacific Time: UTC-8 + DST 1 hour = UTC-7
        TimeConfigurationDto pacificDst = new TimeConfigurationDto(
            null, null, null, 3600L, null, -28800L
        );
        assertEquals(-25200L, pacificDst.getEffectiveOffset(), "UTC-8 + 1hr DST should be -25200 seconds");
        assertEquals(-7.0, pacificDst.getEffectiveOffsetInHours(), 0.01, "Effective offset should be -7 hours");

        // Eastern Time without DST: UTC-5
        TimeConfigurationDto easternNoDst = new TimeConfigurationDto(-18000L);
        assertEquals(-18000L, easternNoDst.getEffectiveOffset(), "Without DST, effective equals base offset");
        assertEquals(-5.0, easternNoDst.getEffectiveOffsetInHours(), 0.01, "Effective offset should be -5 hours");
    }

    @Test
    @DisplayName("Should detect DST rules presence correctly")
    void shouldDetectDstRulesPresenceCorrectly() {
        // With DST rules
        TimeConfigurationDto withDstRules = new TimeConfigurationDto(
            null, null,
            new byte[]{0x01}, 3600L, new byte[]{0x01}, -28800L
        );
        assertTrue(withDstRules.hasDstRules(), "Should detect DST rules when present");

        // Without DST rules
        TimeConfigurationDto withoutDstRules = new TimeConfigurationDto(-28800L);
        assertFalse(withoutDstRules.hasDstRules(), "Should not detect DST rules when absent");

        // Empty DST rules
        TimeConfigurationDto emptyDstRules = new TimeConfigurationDto(
            null, null,
            new byte[]{}, 3600L, new byte[]{}, -28800L
        );
        assertFalse(emptyDstRules.hasDstRules(), "Should not detect empty DST rules");
    }

    @Test
    @DisplayName("Should detect DST active status correctly")
    void shouldDetectDstActiveStatusCorrectly() {
        // DST active
        TimeConfigurationDto dstActive = new TimeConfigurationDto(
            null, null, null, 3600L, null, -28800L
        );
        assertTrue(dstActive.isDstActive(), "Should detect active DST when offset is non-zero");

        // DST inactive
        TimeConfigurationDto dstInactive = new TimeConfigurationDto(
            null, null, null, 0L, null, -28800L
        );
        assertFalse(dstInactive.isDstActive(), "Should not detect active DST when offset is zero");

        // DST null
        TimeConfigurationDto dstNull = new TimeConfigurationDto(-28800L);
        assertFalse(dstNull.isDstActive(), "Should not detect active DST when offset is null");
    }
}
