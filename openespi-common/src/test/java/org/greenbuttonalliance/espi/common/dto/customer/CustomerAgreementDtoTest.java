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
import org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto;
import org.greenbuttonalliance.espi.common.service.impl.DtoExportServiceImpl;
import org.greenbuttonalliance.espi.common.dto.customer.ElectronicAddressDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XML marshalling/unmarshalling tests for CustomerAgreementDto.
 * Verifies Jakarta JAXB processes JAXB annotations correctly for ESPI 4.0 customer.xsd compliance.
 * Phase 24: CustomerAgreement schema compliance testing.
 */
@DisplayName("CustomerAgreementDto XML Marshalling Tests")
class CustomerAgreementDtoTest {

    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService espiIdGeneratorService =
            new org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService();
        dtoExportService = new DtoExportServiceImpl(null, null, null, null, null, null, null, null, null, null, espiIdGeneratorService);
    }

    @Test
    @DisplayName("Should export CustomerAgreement with complete Document and Agreement fields")
    void shouldExportCustomerAgreementWithCompleteDocumentAndAgreementFields() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 26, 10, 30, 0, 0, ZoneOffset.UTC);

        CustomerAgreementDto customerAgreement = createFullCustomerAgreementDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:650e8400-e29b-51d4-a716-446655440000",
            "Test Customer Agreement",
            now, now, null, customerAgreement
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Customer Agreement Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Debug output
        System.out.println("========== CustomerAgreement XML Output ==========");
        System.out.println(xml);
        System.out.println("==================================================");

        // Assert - Basic structure
        assertThat(xml)
            .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .contains("<atom:feed")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        // Assert - Customer namespace
        assertThat(xml)
            .contains("http://naesb.org/espi/customer")
            .contains("cust:")
            .contains("<cust:CustomerAgreement")
            .contains("</cust:CustomerAgreement>");

        // Assert - Document fields present
        assertThat(xml)
            .contains("<cust:type>SERVICE_AGREEMENT</cust:type>")
            .contains("<cust:authorName>Utility Service Dept</cust:authorName>")
            .contains("<cust:createdDateTime")
            .contains("<cust:lastModifiedDateTime")
            .contains("<cust:revisionNumber>2.0</cust:revisionNumber>")
            .contains("<cust:electronicAddress")
            .contains("<cust:email1>service@utility.com</cust:email1>")
            .contains("<cust:subject>Residential Service Agreement</cust:subject>")
            .contains("<cust:title>Customer Service Agreement AGR-789</cust:title>");

        // Assert - Document.docStatus embedded object
        assertThat(xml)
            .contains("<cust:docStatus")
            .contains("<cust:value>FINALIZED</cust:value>");

        // Assert - Document.status embedded object
        assertThat(xml)
            .contains("<cust:status")
            .contains("<cust:value>ACTIVE</cust:value>");

        // Assert - Agreement fields present
        assertThat(xml)
            .contains("<cust:signDate")
            .contains("<cust:validityInterval");

        // Assert - CustomerAgreement fields present
        assertThat(xml)
            .contains("<cust:loadMgmt>STANDARD</cust:loadMgmt>")
            .contains("<cust:isPrePay>false</cust:isPrePay>")
            .contains("<cust:currency>USD</cust:currency>")
            .contains("<cust:agreementId>AGR-789</cust:agreementId>");
    }

    @Test
    @DisplayName("Should verify CustomerAgreement field order matches customer.xsd")
    void shouldVerifyCustomerAgreementFieldOrder() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 26, 10, 30, 0, 0, ZoneOffset.UTC);

        CustomerAgreementDto customerAgreement = createFullCustomerAgreementDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:650e8400-e29b-51d4-a716-446655440001",
            "Test Customer Agreement",
            now, now, null, customerAgreement
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify Document field order per customer.xsd (lines 819-885)
        int typePos = xml.indexOf("<cust:type");
        int authorNamePos = xml.indexOf("<cust:authorName");
        int createdDateTimePos = xml.indexOf("<cust:createdDateTime");
        int lastModifiedDateTimePos = xml.indexOf("<cust:lastModifiedDateTime");
        int revisionNumberPos = xml.indexOf("<cust:revisionNumber");
        int electronicAddressPos = xml.indexOf("<cust:electronicAddress");
        int subjectPos = xml.indexOf("<cust:subject");
        int titlePos = xml.indexOf("<cust:title");
        int docStatusPos = xml.indexOf("<cust:docStatus");

        assertThat(typePos).isGreaterThan(0).isLessThan(authorNamePos);
        assertThat(authorNamePos).isLessThan(createdDateTimePos);
        assertThat(createdDateTimePos).isLessThan(lastModifiedDateTimePos);
        assertThat(lastModifiedDateTimePos).isLessThan(revisionNumberPos);
        assertThat(revisionNumberPos).isLessThan(electronicAddressPos);
        assertThat(electronicAddressPos).isLessThan(subjectPos);
        assertThat(subjectPos).isLessThan(titlePos);
        assertThat(titlePos).isLessThan(docStatusPos);

        // Assert - Verify Document.status comes after Document.docStatus
        int statusPos = xml.indexOf("<cust:status");
        assertThat(docStatusPos).isLessThan(statusPos);

        // Assert - Verify Agreement field order per customer.xsd (lines 622-642)
        int signDatePos = xml.indexOf("<cust:signDate");
        int validityIntervalPos = xml.indexOf("<cust:validityInterval");

        assertThat(statusPos).isLessThan(signDatePos);
        assertThat(signDatePos).isLessThan(validityIntervalPos);

        // Assert - Verify CustomerAgreement field order per customer.xsd (lines 159-209)
        int loadMgmtPos = xml.indexOf("<cust:loadMgmt");
        int isPrePayPos = xml.indexOf("<cust:isPrePay");
        int currencyPos = xml.indexOf("<cust:currency");
        int agreementIdPos = xml.indexOf("<cust:agreementId");

        assertThat(validityIntervalPos).isLessThan(loadMgmtPos);
        assertThat(loadMgmtPos).isLessThan(isPrePayPos);
        assertThat(currencyPos).isLessThan(agreementIdPos);
    }

    @Test
    @DisplayName("Should use correct customer namespace")
    void shouldUseCorrectCustomerNamespace() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 26, 10, 30, 0, 0, ZoneOffset.UTC);

        CustomerAgreementDto customerAgreement = createMinimalCustomerAgreementDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:test-id", "Test", now, now, null, customerAgreement
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
            .doesNotContain("espi:CustomerAgreement")
            .contains("<cust:CustomerAgreement");
    }

    @Test
    @DisplayName("Should export ElectronicAddress with all 8 fields")
    void shouldExportElectronicAddressWithAllFields() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 26, 10, 30, 0, 0, ZoneOffset.UTC);

        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
            "192.168.1.1",
            "00:11:22:33:44:55",
            "primary@utility.com",
            "secondary@utility.com",
            "https://www.utility.com",
            "radio-freq-123",
            "admin_user",
            "secure_pass"
        );

        CustomerAgreementDto customerAgreement = new CustomerAgreementDto(
            "SERVICE",
            "Author",
            now, now,
            "1.0",
            electronicAddress,
            "Subject",
            "Title",
            null, null, null,
            now,
            new DateTimeIntervalDto(now.toEpochSecond(), 3600L),
            "LOAD_MGMT",
            false,
            null,
            "USD",
            null,
            "AGR-123"
        );

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:test-id", "Test", now, now, null, customerAgreement
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - All 8 ElectronicAddress fields per customer.xsd lines 886-936
        assertThat(xml)
            .contains("<cust:lan>192.168.1.1</cust:lan>")
            .contains("<cust:mac>00:11:22:33:44:55</cust:mac>")
            .contains("<cust:email1>primary@utility.com</cust:email1>")
            .contains("<cust:email2>secondary@utility.com</cust:email2>")
            .contains("<cust:web>https://www.utility.com</cust:web>")
            .contains("<cust:radio>radio-freq-123</cust:radio>")
            .contains("<cust:userID>admin_user</cust:userID>")
            .contains("<cust:password>secure_pass</cust:password>");
    }

    // Helper methods

    private CustomerAgreementDto createFullCustomerAgreementDto() {
        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
            "10.0.0.1",
            "AA:BB:CC:DD:EE:FF",
            "service@utility.com",
            "support@utility.com",
            "https://www.utility.com",
            null,
            "service_user",
            null
        );

        CustomerAgreementDto.StatusDto docStatus = new CustomerAgreementDto.StatusDto(
            "FINALIZED",
            OffsetDateTime.of(2025, 1, 20, 9, 0, 0, 0, ZoneOffset.UTC),
            "Document finalized",
            "Complete"
        );

        CustomerAgreementDto.StatusDto status = new CustomerAgreementDto.StatusDto(
            "ACTIVE",
            OffsetDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC),
            "Service agreement active",
            "Customer in good standing"
        );

        OffsetDateTime validityStart = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        DateTimeIntervalDto validityInterval = new DateTimeIntervalDto(
            validityStart.toEpochSecond(),
            31536000L // 1 year in seconds
        );

        return new CustomerAgreementDto(
            "SERVICE_AGREEMENT",
            "Utility Service Dept",
            OffsetDateTime.of(2024, 12, 15, 0, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2025, 1, 26, 10, 30, 0, 0, ZoneOffset.UTC),
            "2.0",
            electronicAddress,
            "Residential Service Agreement",
            "Customer Service Agreement AGR-789",
            docStatus,
            status,
            "Standard terms and conditions apply",
            OffsetDateTime.of(2025, 1, 10, 14, 0, 0, 0, ZoneOffset.UTC),
            validityInterval,
            "STANDARD",
            false,
            null,
            "USD",
            null,
            "AGR-789"
        );
    }

    private CustomerAgreementDto createMinimalCustomerAgreementDto() {
        return new CustomerAgreementDto(
            null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, "AGR-MIN"
        );
    }
}
