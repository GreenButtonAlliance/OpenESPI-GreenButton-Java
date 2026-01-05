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
            "Residential Electric Service",
            new byte[]{0x01, 0x04}, // Electricity consumer role flags
            null, // serviceCategory
            (short) 1, // Active status
            null, null, null, null, // measurement fields
            null, null, null, // reference fields
            null, null, null // collection fields
        );

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(usagePoint);

        // Verify XML structure
        assertThat(xml).contains("UsagePoint");
        assertThat(xml).contains("http://naesb.org/espi");
        assertThat(xml).contains("Residential Electric Service");
        assertThat(xml).containsPattern("<status[^>]*>1</status>"); // May have xmlns attribute
    }

    @Test
    @DisplayName("Should perform round-trip marshalling for UsagePointDto")
    void shouldPerformRoundTripMarshallingForUsagePoint() throws Exception {
        // Create original UsagePoint with comprehensive data
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

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(original);

        // Unmarshal back from XML using Jackson 3
        UsagePointDto roundTrip = xmlMapper.readValue(xml, UsagePointDto.class);

        // Verify data integrity survived round trip
        assertThat(roundTrip.getDescription()).isEqualTo(original.getDescription());
        assertThat(roundTrip.getStatus()).isEqualTo(original.getStatus());
        assertThat(roundTrip.getRoleFlags()).isEqualTo(original.getRoleFlags());
    }

    @Test
    @DisplayName("Should handle empty UsagePointDto without errors")
    void shouldHandleEmptyUsagePointWithoutErrors() throws Exception {
        // Create empty UsagePoint
        UsagePointDto empty = new UsagePointDto(
            null, null, null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null
        );

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(empty);

        // Should still contain basic structure
        assertThat(xml).contains("UsagePoint");
        assertThat(xml).contains("http://naesb.org/espi");

        // Unmarshal back using Jackson 3
        UsagePointDto roundTrip = xmlMapper.readValue(xml, UsagePointDto.class);

        // Should not throw exceptions
        assertThat(roundTrip).isNotNull();
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() throws Exception {
        // Create UsagePoint with some null values
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

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(withNulls);

        // Unmarshal back using Jackson 3
        UsagePointDto roundTrip = xmlMapper.readValue(xml, UsagePointDto.class);

        // Verify nulls are preserved
        assertThat(roundTrip.getDescription()).isNull();
        assertThat(roundTrip.getRoleFlags()).isNull();
        assertThat(roundTrip.getStatus()).isEqualTo(withNulls.getStatus());
    }

    @Test
    @DisplayName("Should include proper XML namespaces")
    void shouldIncludeProperXmlNamespaces() throws Exception {
        // Create UsagePoint
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-namespaces",
            "Test Service",
            null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null
        );

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(usagePoint);

        // Verify namespace declarations
        assertThat(xml).contains("xmlns");
        assertThat(xml).contains("http://naesb.org/espi");

        // Verify no legacy namespaces
        assertThat(xml).doesNotContain("legacy");
        assertThat(xml).doesNotContain("deprecated");
    }

    @Test
    @DisplayName("Should marshal special characters correctly")
    void shouldMarshalSpecialCharactersCorrectly() throws Exception {
        // Create UsagePoint with special characters
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-special-chars",
            "Service & Co. <Electric> \"Smart\" Meter",
            null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null
        );

        // Marshal to XML using Jackson 3
        String xml = xmlMapper.writeValueAsString(usagePoint);

        // Verify XML escaping
        assertThat(xml)
            .satisfiesAnyOf(
                s -> assertThat(s).contains("&amp;"),
                s -> assertThat(s).contains("Service &amp; Co.")
            );
        assertThat(xml).contains("&lt;Electric>");  // < is escaped, > in quoted text may not be

        // Unmarshal back and verify data integrity using Jackson 3
        UsagePointDto roundTrip = xmlMapper.readValue(xml, UsagePointDto.class);

        assertThat(roundTrip.getDescription()).isEqualTo(usagePoint.getDescription());
    }

    @Test
    @DisplayName("Should not throw exceptions during marshalling")
    void shouldNotThrowExceptionsDuringMarshalling() {
        // Create UsagePoint
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:test-no-exceptions",
            "Test Service",
            null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null
        );

        // Verify marshalling does not throw using Jackson 3
        assertThatCode(() -> xmlMapper.writeValueAsString(usagePoint))
            .doesNotThrowAnyException();
    }
}
