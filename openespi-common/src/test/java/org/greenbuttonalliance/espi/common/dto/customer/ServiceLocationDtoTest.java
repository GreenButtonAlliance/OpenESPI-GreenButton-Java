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

package org.greenbuttonalliance.espi.common.dto.customer;

import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.service.impl.DtoExportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XML marshalling/unmarshalling tests for ServiceLocationDto.
 * Verifies Jakarta JAXB Marshaller processes JAXB annotations correctly for ESPI 4.0 customer.xsd compliance.
 * Tests Phase 23 compliance: ServiceLocation extends WorkLocation extends Location.
 */
@DisplayName("ServiceLocationDto XML Marshalling Tests - Phase 23")
class ServiceLocationDtoTest {

    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        // Initialize DtoExportService with null repository/mapper (not needed for DTO-only tests)
        org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService espiIdGeneratorService =
            new org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService();
        dtoExportService = new DtoExportServiceImpl(null, null, espiIdGeneratorService);
    }

    @Test
    @DisplayName("Should export ServiceLocation with complete realistic data")
    void shouldExportServiceLocationWithRealisticData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceLocationDto serviceLocation = createFullServiceLocationDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:650e8400-e29b-51d4-a716-446655440000",
            "ACME Energy Service Location",
            now, now, null, serviceLocation
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "ServiceLocation Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Debug output
        System.out.println("========== ServiceLocation XML Output ==========");
        System.out.println(xml);
        System.out.println("===============================================");

        // Assert - Basic structure and content
        assertThat(xml)
            .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .contains("<atom:feed")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"")
            .contains("http://naesb.org/espi/customer")
            .contains("cust:")
            .contains("<cust:ServiceLocation")
            .contains("COMMERCIAL")
            .contains("456 Industrial Blvd")
            .contains("meter@example.com")
            .contains("Call office for key")
            .contains("OUTAGE-BLOCK-001");
    }

    @Test
    @DisplayName("Should verify ServiceLocation field order matches customer.xsd")
    void shouldVerifyServiceLocationFieldOrder() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceLocationDto serviceLocation = createFullServiceLocationDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:650e8400-e29b-51d4-a716-446655440001",
            "Test ServiceLocation",
            now, now, null, serviceLocation
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify XSD element order per customer.xsd:
        // Location fields: type, mainAddress, secondaryAddress, phone1, phone2, electronicAddress,
        //                  geoInfoReference, direction, status, positionPoints
        // ServiceLocation fields: accessMethod, siteAccessProblem, needsInspection, usagePointHrefs, outageBlock

        int typePos = xml.indexOf("<cust:type>");
        int mainAddressPos = xml.indexOf("<cust:mainAddress>");
        int secondaryAddressPos = xml.indexOf("<cust:secondaryAddress>");
        int phone1Pos = xml.indexOf("<cust:phone1>");
        int phone2Pos = xml.indexOf("<cust:phone2>");
        int electronicAddressPos = xml.indexOf("<cust:electronicAddress>");
        int geoInfoReferencePos = xml.indexOf("<cust:geoInfoReference>");
        int directionPos = xml.indexOf("<cust:direction>");
        int statusPos = xml.indexOf("<cust:status>");
        int accessMethodPos = xml.indexOf("<cust:accessMethod>");
        int siteAccessProblemPos = xml.indexOf("<cust:siteAccessProblem>");
        int needsInspectionPos = xml.indexOf("<cust:needsInspection>");
        int usagePointsPos = xml.indexOf("<cust:UsagePoints>");
        int outageBlockPos = xml.indexOf("<cust:outageBlock>");

        // Verify Location field order
        assertThat(typePos).isGreaterThan(0).isLessThan(mainAddressPos);
        assertThat(mainAddressPos).isLessThan(secondaryAddressPos);
        assertThat(secondaryAddressPos).isLessThan(phone1Pos);
        assertThat(phone1Pos).isLessThan(phone2Pos);
        assertThat(phone2Pos).isLessThan(electronicAddressPos);
        assertThat(electronicAddressPos).isLessThan(geoInfoReferencePos);
        assertThat(geoInfoReferencePos).isLessThan(directionPos);
        assertThat(directionPos).isLessThan(statusPos);

        // Verify ServiceLocation field order (after Location fields)
        assertThat(statusPos).isLessThan(accessMethodPos);
        assertThat(accessMethodPos).isLessThan(siteAccessProblemPos);
        assertThat(siteAccessProblemPos).isLessThan(needsInspectionPos);
        assertThat(needsInspectionPos).isLessThan(usagePointsPos);
        assertThat(usagePointsPos).isLessThan(outageBlockPos);
    }

    @Test
    @DisplayName("Should verify TelephoneNumber 8-field compliance")
    void shouldVerifyTelephoneNumber8FieldCompliance() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceLocationDto serviceLocation = createFullServiceLocationDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:650e8400-e29b-51d4-a716-446655440002",
            "Test ServiceLocation",
            now, now, null, serviceLocation
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify all 8 TelephoneNumber fields present per customer.xsd:1428-1478
        assertThat(xml)
            .contains("<cust:countryCode>1</cust:countryCode>")
            .contains("<cust:areaCode>312</cust:areaCode>")
            .containsAnyOf("<cust:cityCode>", "<cust:cityCode/>")
            .contains("<cust:localNumber>555-1000</cust:localNumber>")
            .containsAnyOf("<cust:ext>", "<cust:ext/>")
            .containsAnyOf("<cust:dialOut>", "<cust:dialOut/>")
            .containsAnyOf("<cust:internationalPrefix>", "<cust:internationalPrefix/>")
            .containsAnyOf("<cust:ituPhone>", "<cust:ituPhone/>");
    }

    @Test
    @DisplayName("Should verify Status 4-field compliance")
    void shouldVerifyStatus4FieldCompliance() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceLocationDto serviceLocation = createFullServiceLocationDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:650e8400-e29b-51d4-a716-446655440003",
            "Test ServiceLocation",
            now, now, null, serviceLocation
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify all 4 Status fields present (value, dateTime, remark, reason)
        assertThat(xml)
            .contains("<cust:status>")
            .contains("<cust:value>ACTIVE</cust:value>")
            .containsAnyOf("<cust:dateTime>", "<cust:dateTime/>")
            .contains("<cust:remark>Location is operational</cust:remark>")
            .contains("<cust:reason>Routine inspection passed</cust:reason>");
    }

    @Test
    @DisplayName("Should export ServiceLocation with minimal data")
    void shouldExportServiceLocationWithMinimalData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceLocationDto serviceLocation = createMinimalServiceLocationDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:650e8400-e29b-51d4-a716-446655440004",
            "Minimal ServiceLocation",
            now, now, null, serviceLocation
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - ServiceLocation element present (may be self-closing)
        assertThat(xml).contains("<cust:ServiceLocation");
    }

    @Test
    @DisplayName("Should verify UsagePoints cross-stream reference")
    void shouldVerifyUsagePointsCrossStreamReference() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceLocationDto serviceLocation = createFullServiceLocationDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:650e8400-e29b-51d4-a716-446655440005",
            "Test ServiceLocation",
            now, now, null, serviceLocation
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify UsagePoints collection contains href URLs
        assertThat(xml)
            .contains("<cust:UsagePoints>")
            .contains("https://api.example.com/espi/1_1/resource/UsagePoint/12345")
            .contains("https://api.example.com/espi/1_1/resource/UsagePoint/67890");
    }

    /**
     * Creates a full ServiceLocationDto with all Location and ServiceLocation fields populated.
     * Per ESPI 4.0 customer.xsd: ServiceLocation → WorkLocation → Location → IdentifiedObject.
     */
    private ServiceLocationDto createFullServiceLocationDto() {
        // Location fields
        CustomerDto.StreetAddressDto mainAddress = new CustomerDto.StreetAddressDto(
            "456 Industrial Blvd", "Chicago", "IL", "60601", "USA"
        );

        CustomerDto.StreetAddressDto secondaryAddress = new CustomerDto.StreetAddressDto(
            "PO Box 789", "Chicago", "IL", "60602", "USA"
        );

        CustomerDto.TelephoneNumberDto phone1 = new CustomerDto.TelephoneNumberDto(
            "1", "312", "773", "555-1000", "100", "9", "011", "+1-312-555-1000"
        );

        CustomerDto.TelephoneNumberDto phone2 = new CustomerDto.TelephoneNumberDto(
            "1", "312", "773", "555-2000", "200", "9", "011", "+1-312-555-2000"
        );

        CustomerDto.ElectronicAddressDto electronicAddress = new CustomerDto.ElectronicAddressDto(
            "192.168.1.100", "00:11:22:33:44:55", "meter@example.com", "support@example.com",
            "https://meter.example.com", "VHF-123", "meter_user", null
        );

        StatusDto status = new StatusDto(
            "ACTIVE",
            OffsetDateTime.of(2025, 1, 20, 14, 30, 0, 0, ZoneOffset.UTC),
            "Location is operational",
            "Routine inspection passed"
        );

        ServiceLocationDto.PositionPointDto positionPoint1 = new ServiceLocationDto.PositionPointDto(
            "41.8781", "-87.6298", null
        );

        ServiceLocationDto.PositionPointDto positionPoint2 = new ServiceLocationDto.PositionPointDto(
            "41.8782", "-87.6299", "100"
        );

        List<ServiceLocationDto.PositionPointDto> positionPoints = List.of(positionPoint1, positionPoint2);

        // ServiceLocation fields
        List<String> usagePointHrefs = List.of(
            "https://api.example.com/espi/1_1/resource/UsagePoint/12345",
            "https://api.example.com/espi/1_1/resource/UsagePoint/67890"
        );

        return new ServiceLocationDto(
            null, // id
            "650e8400-e29b-51d4-a716-446655440000", // uuid
            "COMMERCIAL", // type
            mainAddress,
            secondaryAddress,
            phone1,
            phone2,
            electronicAddress,
            "GEO-REF-12345", // geoInfoReference
            "North side of building, meter room on 2nd floor", // direction
            status,
            positionPoints,
            "Call office for key", // accessMethod
            "Guard dogs on premises", // siteAccessProblem
            true, // needsInspection
            usagePointHrefs,
            "OUTAGE-BLOCK-001" // outageBlock
        );
    }

    /**
     * Creates a minimal ServiceLocationDto with only required fields.
     */
    private ServiceLocationDto createMinimalServiceLocationDto() {
        return new ServiceLocationDto(
            null, // id
            "650e8400-e29b-51d4-a716-446655440099", // uuid
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null
        );
    }
}
