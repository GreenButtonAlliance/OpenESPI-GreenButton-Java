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

import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to see what XML is actually being generated.
 */
@DisplayName("XML Debug Test")
class XmlDebugTest {

    private JAXBContext jaxbContext;
    private Marshaller marshaller;

    @BeforeEach
    void setUp() throws JAXBException {
        jaxbContext = JAXBContext.newInstance(UsagePointDto.class);
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    }

    @Test
    @DisplayName("Debug: See what XML is actually generated")
    void debugXmlOutput() throws Exception {
        // Create a simple UsagePointDto
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:debug-test",
            "Debug Service",
            new byte[]{0x01}, // Simple role flag
            null, // serviceCategory
            (short) 1, // Active status
            null, null, null, null, // measurement fields
            null, null, null, // reference fields
            null, null, null // collection fields
        );
        
        // Marshal to XML
        StringWriter writer = new StringWriter();
        marshaller.marshal(usagePoint, writer);
        String xml = writer.toString();
        
        // Print the actual XML for debugging
        System.out.println("Generated XML:");
        System.out.println("==============");
        System.out.println(xml);
        System.out.println("==============");
        
        // Basic checks
        assertNotNull(xml);
        assertFalse(xml.trim().isEmpty());
        assertTrue(xml.contains("UsagePoint"), "Should contain UsagePoint element");
    }
}