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

import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.service.impl.DtoExportServiceImpl;
import org.greenbuttonalliance.espi.common.dto.customer.ElectronicAddressDto;
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
 * XML marshalling/unmarshalling tests for CustomerDto.
 * Verifies Jakarta JAXB Marshaller processes JAXB annotations correctly for ESPI 4.0 customer.xsd compliance.
 * Follows the same pattern as DtoExportServiceImplTest for usage domain resources.
 */
@DisplayName("CustomerDto XML Marshalling Tests")
class CustomerDtoTest {

    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        // Initialize DtoExportService with null repository/mapper (not needed for DTO-only tests)
        org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService espiIdGeneratorService =
            new org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService();
        dtoExportService = new DtoExportServiceImpl(null, null, espiIdGeneratorService);
    }

    @Test
    @DisplayName("Should export Customer with complete realistic data")
    void shouldExportCustomerWithRealisticData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        CustomerDto customer = createFullCustomerDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440000",
            "ACME Energy Services Customer",
            now, now, null, customer
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Customer Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Debug output
        System.out.println("========== Customer XML Output ==========");
        System.out.println(xml);
        System.out.println("=========================================");

        // Assert - Basic structure
        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<atom:feed");
        assertThat(xml).contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        // Assert - Customer namespace (cust: prefix for customer.xsd)
        assertThat(xml).contains("http://naesb.org/espi/customer");
        assertThat(xml).contains("cust:");
        assertThat(xml).contains("<cust:Customer");
        assertThat(xml).contains("</cust:Customer>");

        // Assert - Organisation fields present
        assertThat(xml).contains("<cust:Organisation");
        assertThat(xml).contains("123 Main Street");
        assertThat(xml).contains("ACME Energy Services");

        // Assert - Customer fields present
        assertThat(xml).contains("RESIDENTIAL");
        assertThat(xml).contains("Life support required");
        assertThat(xml).contains("PUC-12345");
        assertThat(xml).contains("John Smith");
    }

    @Test
    @DisplayName("Should verify Customer field order matches customer.xsd")
    void shouldVerifyCustomerFieldOrder() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        CustomerDto customer = createFullCustomerDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440003",
            "Test Customer",
            now, now, null, customer
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify XSD element order: Organisation, kind, specialNeed, vip, pucNumber, status, priority, locale, customerName
        int organisationPos = xml.indexOf("<cust:Organisation");
        int kindPos = xml.indexOf("<cust:kind>");
        int specialNeedPos = xml.indexOf("<cust:specialNeed>");
        int vipPos = xml.indexOf("<cust:vip>");
        int pucNumberPos = xml.indexOf("<cust:pucNumber>");
        int statusPos = xml.indexOf("<cust:status>");
        int priorityPos = xml.indexOf("<cust:priority>");
        int localePos = xml.indexOf("<cust:locale>");
        int customerNamePos = xml.indexOf("<cust:customerName>");

        assertThat(organisationPos).isGreaterThan(0).isLessThan(kindPos);
        assertThat(kindPos).isLessThan(specialNeedPos);
        assertThat(specialNeedPos).isLessThan(vipPos);
        assertThat(vipPos).isLessThan(pucNumberPos);
        assertThat(pucNumberPos).isLessThan(statusPos);
        assertThat(statusPos).isLessThan(priorityPos);
        assertThat(priorityPos).isLessThan(localePos);
        assertThat(localePos).isLessThan(customerNamePos);
    }

    @Test
    @DisplayName("Should verify Organisation field order per customer.xsd")
    void shouldVerifyOrganisationFieldOrder() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        CustomerDto customer = createFullCustomerDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440004",
            "Test Customer",
            now, now, null, customer
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
    @DisplayName("Should export Customer with minimal data")
    void shouldExportCustomerWithMinimalData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        OrganisationDto organisation = new OrganisationDto(
            null, null, null, null, null, "Minimal Org"
        );

        CustomerDto customer = new CustomerDto(
            organisation,
            CustomerKind.ENTERPRISE,
            null, null, null, null, null, null, null
        );

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440002",
            "Minimal Customer",
            now, now, null, customer
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
        assertThat(xml).contains("<cust:Customer");
        assertThat(xml).contains("Minimal Org");
        assertThat(xml).contains("ENTERPRISE");
    }

    @Test
    @DisplayName("Should use correct customer namespace")
    void shouldUseCorrectCustomerNamespace() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        CustomerDto customer = createMinimalCustomerDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:test-id", "Test", now, now, null, customer
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
        assertThat(xml).contains("http://naesb.org/espi/customer");
        assertThat(xml).contains("cust:");
        assertThat(xml).doesNotContain("espi:Customer"); // Should NOT use espi prefix for Customer
        assertThat(xml).contains("<cust:Customer"); // Should use cust prefix
    }

    // Helper methods

    private CustomerDto createFullCustomerDto() {
        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "123 Main Street", "Springfield", "IL", "62701", "USA"
        );

        CustomerDto.StreetAddressDto postalAddress = new CustomerDto.StreetAddressDto(
            "PO Box 456", "Springfield", "IL", "62702", "USA"
        );

        TelephoneNumberDto phone1 = new TelephoneNumberDto(
            "1", "217", null, "555-1234", null, null, null, null
        );

        TelephoneNumberDto phone2 = new TelephoneNumberDto(
            "1", "217", null, "555-5678", "101", null, null, null
        );

        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
            null, null, "customer@example.com", "support@example.com", "https://www.example.com", null, null, null
        );

        OrganisationDto organisation = new OrganisationDto(
            streetAddress, postalAddress, phone1, phone2, electronicAddress, "ACME Energy Services"
        );

        StatusDto status = new StatusDto(
            "ACTIVE",
            OffsetDateTime.of(2025, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC),
            "Account in good standing",
            null
        );

        CustomerDto.PriorityDto priority = new CustomerDto.PriorityDto(5, 1, "STANDARD");

        return new CustomerDto(
            organisation,
            CustomerKind.RESIDENTIAL,
            "Life support required",
            true,
            "PUC-12345",
            status,
            priority,
            "en_US",
            "John Smith"
        );
    }

    private CustomerDto createMinimalCustomerDto() {
        OrganisationDto organisation = new OrganisationDto(
            null, null, null, null, null, "Test Org"
        );

        return new CustomerDto(
            organisation,
            CustomerKind.RESIDENTIAL,
            null, null, null, null, null, null, "Test Name"
        );
    }
}
