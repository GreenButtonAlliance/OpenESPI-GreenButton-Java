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

import org.greenbuttonalliance.espi.common.domain.customer.enums.SupplierKind;
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
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
 * XML marshalling/unmarshalling tests for ServiceSupplierDto.
 * Verifies Jakarta JAXB Marshaller processes JAXB annotations correctly for ESPI 4.0 customer.xsd compliance.
 * ServiceSupplier schema definition: customer.xsd lines 1159-1186.
 */
@DisplayName("ServiceSupplierDto XML Marshalling Tests")
class ServiceSupplierDtoTest {

    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        // Initialize DtoExportService with null repository/mapper (not needed for DTO-only tests)
        org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService espiIdGeneratorService =
            new org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService();
        dtoExportService = new DtoExportServiceImpl(null, null, espiIdGeneratorService);
    }

    @Test
    @DisplayName("Should export ServiceSupplier with complete realistic data")
    void shouldExportServiceSupplierWithRealisticData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceSupplierDto serviceSupplier = createFullServiceSupplierDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440000",
            "ACME Utility Company",
            now, now, null, serviceSupplier
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "ServiceSupplier Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Debug output
        System.out.println("========== ServiceSupplier XML Output ==========");
        System.out.println(xml);
        System.out.println("=================================================");

        // Assert - Basic structure, namespaces, and field values
        assertThat(xml)
            .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .contains("<atom:feed")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"")
            .contains("http://naesb.org/espi/customer")
            .contains("cust:")
            .contains("<cust:ServiceSupplier")
            .contains("</cust:ServiceSupplier>")
            .contains("<cust:Organisation")
            .contains("456 Utility Ave")
            .contains("ACME Utility Company")
            .contains("utility")
            .contains("ISS-123456789");
    }

    @Test
    @DisplayName("Should verify ServiceSupplier field order matches customer.xsd")
    void shouldVerifyServiceSupplierFieldOrder() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceSupplierDto serviceSupplier = createFullServiceSupplierDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440003",
            "Test ServiceSupplier",
            now, now, null, serviceSupplier
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify XSD element order per customer.xsd:1159-1186
        // Order: Organisation, kind, issuerIdentificationNumber, effectiveDate
        int organisationPos = xml.indexOf("<cust:Organisation");
        int kindPos = xml.indexOf("<cust:kind>");
        int issuerPos = xml.indexOf("<cust:issuerIdentificationNumber>");
        int effectiveDatePos = xml.indexOf("<cust:effectiveDate");

        assertThat(organisationPos).isGreaterThan(0).isLessThan(kindPos);
        assertThat(kindPos).isLessThan(issuerPos);
        assertThat(issuerPos).isLessThan(effectiveDatePos);
    }

    @Test
    @DisplayName("Should verify Organisation field order per customer.xsd")
    void shouldVerifyOrganisationFieldOrder() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceSupplierDto serviceSupplier = createFullServiceSupplierDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440004",
            "Test ServiceSupplier",
            now, now, null, serviceSupplier
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify Organisation field order per customer.xsd:1096-1125
        // Order: streetAddress, postalAddress, phone1, phone2, electronicAddress, organisationName
        int streetPos = xml.indexOf("<cust:streetAddress>");
        int postalPos = xml.indexOf("<cust:postalAddress>");
        int phone1Pos = xml.indexOf("<cust:phone1>");
        int phone2Pos = xml.indexOf("<cust:phone2>");
        int electronicPos = xml.indexOf("<cust:electronicAddress>");
        int orgNamePos = xml.indexOf("<cust:organisationName>");

        assertThat(streetPos).isGreaterThan(0).isLessThan(postalPos);
        assertThat(postalPos).isLessThan(phone1Pos);
        assertThat(phone1Pos).isLessThan(phone2Pos);
        assertThat(phone2Pos).isLessThan(electronicPos);
        assertThat(electronicPos).isLessThan(orgNamePos);
    }

    @Test
    @DisplayName("Should export ServiceSupplier with minimal data")
    void shouldExportServiceSupplierWithMinimalData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "123 Minimal St", "City", "ST", "12345", "USA"
        );

        OrganisationDto organisation = new OrganisationDto(
            streetAddress, null, null, null, null, "Minimal Supplier"
        );

        ServiceSupplierDto serviceSupplier = new ServiceSupplierDto(
            organisation,
            SupplierKind.UTILITY,
            null, null
        );

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440002",
            "Minimal ServiceSupplier",
            now, now, null, serviceSupplier
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert
        assertThat(xml)
            .contains("<cust:ServiceSupplier")
            .contains("Minimal Supplier")
            .contains("utility");
    }

    @Test
    @DisplayName("Should use correct customer namespace")
    void shouldUseCorrectCustomerNamespace() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceSupplierDto serviceSupplier = createMinimalServiceSupplierDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:test-id", "Test", now, now, null, serviceSupplier
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Should use customer namespace, not usage/espi namespace
        assertThat(xml)
            .contains("http://naesb.org/espi/customer")
            .contains("cust:")
            .doesNotContain("espi:ServiceSupplier") // Should NOT use espi prefix
            .contains("<cust:ServiceSupplier"); // Should use cust prefix
    }

    @Test
    @DisplayName("Should export ServiceSupplier with TelephoneNumber fields")
    void shouldExportServiceSupplierWithTelephoneNumbers() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceSupplierDto serviceSupplier = createFullServiceSupplierDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440005",
            "Test ServiceSupplier",
            now, now, null, serviceSupplier
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify phone1 and phone2 fields are present
        assertThat(xml)
            .contains("<cust:phone1>")
            .contains("<cust:countryCode>1</cust:countryCode>")
            .contains("<cust:areaCode>555</cust:areaCode>")
            .contains("<cust:localNumber>1234</cust:localNumber>")
            .contains("<cust:phone2>")
            .contains("<cust:localNumber>5678</cust:localNumber>");
    }

    @Test
    @DisplayName("Should verify TelephoneNumber field order per customer.xsd")
    void shouldVerifyTelephoneNumberFieldOrder() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        ServiceSupplierDto serviceSupplier = createFullServiceSupplierDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440006",
            "Test ServiceSupplier",
            now, now, null, serviceSupplier
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify TelephoneNumber field order per customer.xsd:1428-1478
        // Order: countryCode, areaCode, cityCode, localNumber, ext, dialOut, internationalPrefix, ituPhone
        int phone1StartPos = xml.indexOf("<cust:phone1>");
        int phone1EndPos = xml.indexOf("</cust:phone1>");

        String phone1Section = xml.substring(phone1StartPos, phone1EndPos);

        int countryCodePos = phone1Section.indexOf("<cust:countryCode>");
        int areaCodePos = phone1Section.indexOf("<cust:areaCode>");
        int localNumberPos = phone1Section.indexOf("<cust:localNumber>");

        assertThat(countryCodePos).isGreaterThan(0).isLessThan(areaCodePos);
        assertThat(areaCodePos).isLessThan(localNumberPos);
    }

    @Test
    @DisplayName("Should export ServiceSupplier with all SupplierKind values")
    void shouldExportServiceSupplierWithAllSupplierKindValues() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        // Test each SupplierKind enum value
        SupplierKind[] kinds = {
            SupplierKind.UTILITY,
            SupplierKind.RETAILER,
            SupplierKind.OTHER
        };

        for (SupplierKind kind : kinds) {
            ServiceSupplierDto serviceSupplier = createMinimalServiceSupplierDto();
            // Update kind field via new instance since DTO is immutable
            serviceSupplier = new ServiceSupplierDto(
                serviceSupplier.getOrganisation(),
                kind,
                serviceSupplier.getIssuerIdentificationNumber(),
                serviceSupplier.getEffectiveDate()
            );

            CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
                "urn:uuid:test-kind-" + kind, "Test", now, now, null, serviceSupplier
            );

            AtomFeedDto feed = new AtomFeedDto(
                "urn:uuid:feed-id", "Test Feed", now, now, null,
                List.of(entry)
            );

            // Act
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            dtoExportService.exportAtomFeed(feed, stream);
            String xml = stream.toString(StandardCharsets.UTF_8);

            // Assert
            assertThat(xml).contains(kind.toString());
        }
    }

    // Helper methods

    private ServiceSupplierDto createFullServiceSupplierDto() {
        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "456 Utility Ave", "Metropolis", "NY", "10001", "USA"
        );

        CustomerDto.StreetAddressDto postalAddress = new CustomerDto.StreetAddressDto(
            "PO Box 789", "Metropolis", "NY", "10002", "USA"
        );

        TelephoneNumberDto phone1 = new TelephoneNumberDto(
            "1", "555", null, "1234", null, null, null, null
        );

        TelephoneNumberDto phone2 = new TelephoneNumberDto(
            "1", "555", null, "5678", "200", null, null, null
        );

        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
            null, null, "info@acmeutility.com", "support@acmeutility.com",
            "https://www.acmeutility.com", null, null, null
        );

        OrganisationDto organisation = new OrganisationDto(
            streetAddress, postalAddress, phone1, phone2, electronicAddress, "ACME Utility Company"
        );

        return new ServiceSupplierDto(
            organisation,
            SupplierKind.UTILITY,
            "ISS-123456789",
            OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    private ServiceSupplierDto createMinimalServiceSupplierDto() {
        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "123 Test St", "City", "ST", "12345", "USA"
        );

        OrganisationDto organisation = new OrganisationDto(
            streetAddress, null, null, null, null, "Test Supplier Org"
        );

        return new ServiceSupplierDto(
            organisation,
            SupplierKind.UTILITY,
            null, null
        );
    }
}
