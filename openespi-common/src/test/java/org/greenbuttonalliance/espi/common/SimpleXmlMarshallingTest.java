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
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple XML marshalling tests to verify basic JAXB functionality with ESPI data.
 * Tests marshal/unmarshal round-trip with realistic data structures.
 */
@DisplayName("Simple XML Marshalling Tests")
class SimpleXmlMarshallingTest {

    private JAXBContext jaxbContext;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    @BeforeEach
    void setUp() throws JAXBException {
        // Initialize JAXB context for UsagePoint DTO
        jaxbContext = JAXBContext.newInstance(UsagePointDto.class);
        
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        
        unmarshaller = jaxbContext.createUnmarshaller();
    }

    @Test
    @DisplayName("Should marshal UsagePointDto with realistic data")
    void shouldMarshalUsagePointWithRealisticData() throws Exception {
        // Create a UsagePointDto record with realistic ESPI data
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-usage-point",
            "Residential Electric Service",
            new byte[]{0x01, 0x04}, // Electricity consumer role flags
            null, // serviceCategory
            (short) 1, // Active status
            null, null, null, null, // measurement fields
            null, null, null, // reference fields
            null, null, null // collection fields
        );
        
        // Marshal to XML
        StringWriter writer = new StringWriter();
        assertDoesNotThrow(() -> marshaller.marshal(usagePoint, writer));
        
        String xml = writer.toString();
        
        // Verify XML structure
        assertTrue(xml.contains("UsagePoint"), "XML should contain UsagePoint element");
        assertTrue(xml.contains("http://naesb.org/espi"), "XML should contain ESPI namespace");
        assertTrue(xml.contains("Residential Electric Service"), "XML should contain description");
        assertTrue(xml.contains("<espi:status>1</espi:status>"), "XML should contain status");
    }

    @Test
    @DisplayName("Should perform round-trip marshalling for UsagePointDto")
    void shouldPerformRoundTripMarshallingForUsagePoint() throws Exception {
        // Create original UsagePoint record with comprehensive data
        UsagePointDto original = new UsagePointDto(
            "urn:uuid:commercial-gas-point",
            "Commercial Gas Service",
            new byte[]{0x02, 0x08}, // Gas consumer role flags
            null, // serviceCategory
            (short) 1, // Active status
            null, null, null, null, // measurement fields
            null, null, null, // reference fields
            null, null, null // collection fields
        );
        
        // Marshal to XML
        StringWriter writer = new StringWriter();
        marshaller.marshal(original, writer);
        String xml = writer.toString();
        
        // Unmarshal back from XML
        StringReader reader = new StringReader(xml);
        UsagePointDto roundTrip = (UsagePointDto) unmarshaller.unmarshal(reader);
        
        // Verify data integrity survived round trip
        assertEquals(original.getDescription(), roundTrip.getDescription(), 
                    "Description should survive round trip");
        assertEquals(original.getStatus(), roundTrip.getStatus(), 
                    "Status should survive round trip");
        assertArrayEquals(original.getRoleFlags(), roundTrip.getRoleFlags(), 
                         "Role flags should survive round trip");
    }

    @Test
    @DisplayName("Should handle empty UsagePointDto without errors")
    void shouldHandleEmptyUsagePointWithoutErrors() throws Exception {
        // Create empty UsagePoint record
        UsagePointDto empty = new UsagePointDto(
            null, null, null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null
        );
        
        // Marshal to XML
        StringWriter writer = new StringWriter();
        assertDoesNotThrow(() -> marshaller.marshal(empty, writer));
        
        String xml = writer.toString();
        
        // Should still contain basic structure
        assertTrue(xml.contains("UsagePoint"), "XML should contain UsagePoint element");
        assertTrue(xml.contains("http://naesb.org/espi"), "XML should contain ESPI namespace");
        
        // Unmarshal back
        StringReader reader = new StringReader(xml);
        UsagePointDto roundTrip = (UsagePointDto) unmarshaller.unmarshal(reader);
        
        // Should not throw exceptions
        assertNotNull(roundTrip, "Round trip should produce valid object");
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() throws Exception {
        // Create UsagePoint record with some null values
        UsagePointDto withNulls = new UsagePointDto(
            "urn:uuid:test-nulls",
            null, // Null description
            null, // Null role flags
            null, // serviceCategory
            (short) 1, // Non-null status
            null, null, null, null, // measurement fields
            null, null, null, // reference fields
            null, null, null // collection fields
        );
        
        // Marshal to XML
        StringWriter writer = new StringWriter();
        assertDoesNotThrow(() -> marshaller.marshal(withNulls, writer));
        
        String xml = writer.toString();
        
        // Unmarshal back
        StringReader reader = new StringReader(xml);
        UsagePointDto roundTrip = (UsagePointDto) unmarshaller.unmarshal(reader);
        
        // Verify nulls are preserved
        assertNull(roundTrip.getDescription(), "Null description should be preserved");
        assertNull(roundTrip.getRoleFlags(), "Null role flags should be preserved");
        assertEquals(withNulls.getStatus(), roundTrip.getStatus(), "Non-null status should be preserved");
    }

    @Test
    @DisplayName("Should include proper XML namespaces")
    void shouldIncludeProperXmlNamespaces() throws Exception {
        // Create UsagePoint record
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-namespaces",
            "Test Service",
            null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null
        );
        
        // Marshal to XML
        StringWriter writer = new StringWriter();
        marshaller.marshal(usagePoint, writer);
        String xml = writer.toString();
        
        // Verify namespace declarations
        assertTrue(xml.contains("xmlns") && xml.contains("http://naesb.org/espi"), 
                  "XML should contain ESPI namespace declaration");
        
        // Verify no legacy namespaces
        assertFalse(xml.contains("legacy"), "XML should not contain legacy references");
        assertFalse(xml.contains("deprecated"), "XML should not contain deprecated references");
    }

    @Test
    @DisplayName("Should marshal special characters correctly")
    void shouldMarshalSpecialCharactersCorrectly() throws Exception {
        // Create UsagePoint record with special characters
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-special-chars",
            "Service & Co. <Electric> \"Smart\" Meter",
            null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null
        );
        
        // Marshal to XML
        StringWriter writer = new StringWriter();
        marshaller.marshal(usagePoint, writer);
        String xml = writer.toString();
        
        // Verify XML escaping
        assertTrue(xml.contains("&amp;") || xml.contains("Service &amp; Co."), 
                  "Ampersands should be XML escaped");
        assertTrue(xml.contains("&lt;") && xml.contains("&gt;"), 
                  "Angle brackets should be XML escaped");
        
        // Unmarshal back and verify data integrity
        StringReader reader = new StringReader(xml);
        UsagePointDto roundTrip = (UsagePointDto) unmarshaller.unmarshal(reader);
        
        assertEquals(usagePoint.getDescription(), roundTrip.getDescription(), 
                    "Special characters should survive round trip");
    }
}