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
 * XML marshalling/unmarshalling tests for CustomerAccountDto.
 * Verifies Jakarta JAXB processes JAXB annotations correctly for ESPI 4.0 customer.xsd compliance.
 * Phase 18: CustomerAccount schema compliance testing.
 */
@DisplayName("CustomerAccountDto XML Marshalling Tests")
class CustomerAccountDtoTest {

    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService espiIdGeneratorService =
            new org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService();
        dtoExportService = new DtoExportServiceImpl(null, null, null, null, null, null, null, null, null, null, espiIdGeneratorService);
    }

    @Test
    @DisplayName("Should export CustomerAccount with complete Document fields")
    void shouldExportCustomerAccountWithCompleteDocumentFields() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 22, 10, 30, 0, 0, ZoneOffset.UTC);

        CustomerAccountDto customerAccount = createFullCustomerAccountDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440000",
            "Test Customer Account",
            now, now, null, customerAccount
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Customer Account Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Debug output
        System.out.println("========== CustomerAccount XML Output ==========");
        System.out.println(xml);
        System.out.println("================================================");

        // Assert - Basic structure
        assertThat(xml)
            .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .contains("<atom:feed")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        // Assert - Customer namespace
        assertThat(xml)
            .contains("http://naesb.org/espi/customer")
            .contains("cust:")
            .contains("<cust:CustomerAccount")
            .contains("</cust:CustomerAccount>");

        // Assert - Document fields present in correct order
        assertThat(xml)
            .contains("<cust:type>BILLING</cust:type>")
            .contains("<cust:authorName>Billing System</cust:authorName>")
            .contains("<cust:createdDateTime")
            .contains("<cust:lastModifiedDateTime")
            .contains("<cust:revisionNumber>1.0</cust:revisionNumber>")
            .contains("<cust:electronicAddress")
            .contains("<cust:email1>billing@example.com</cust:email1>")
            .contains("<cust:subject>Monthly Billing Account</cust:subject>")
            .contains("<cust:title>Customer Billing Account #12345</cust:title>")
            .contains("<cust:docStatus")
            .contains("<cust:value>ACTIVE</cust:value>");

        // Assert - CustomerAccount fields present
        assertThat(xml)
            .contains("<cust:billingCycle>MONTHLY</cust:billingCycle>")
            .contains("<cust:budgetBill>STANDARD</cust:budgetBill>")
            .contains("<cust:lastBillAmount>15000</cust:lastBillAmount>")
            .contains("<cust:contactInfo>")
            .contains("<cust:organisationName>ACME Corporation</cust:organisationName>")
            .contains("<cust:accountId>ACCT-12345</cust:accountId>");
    }

    @Test
    @DisplayName("Should verify CustomerAccount field order matches customer.xsd")
    void shouldVerifyCustomerAccountFieldOrder() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 22, 10, 30, 0, 0, ZoneOffset.UTC);

        CustomerAccountDto customerAccount = createFullCustomerAccountDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440001",
            "Test Customer Account",
            now, now, null, customerAccount
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify Document field order per customer.xsd (lines 819-872)
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

        // Assert - Verify CustomerAccount field order per customer.xsd (lines 118-158)
        int billingCyclePos = xml.indexOf("<cust:billingCycle");
        int budgetBillPos = xml.indexOf("<cust:budgetBill");
        int lastBillAmountPos = xml.indexOf("<cust:lastBillAmount");
        int contactInfoPos = xml.indexOf("<cust:contactInfo");
        int accountIdPos = xml.indexOf("<cust:accountId");

        assertThat(docStatusPos).isLessThan(billingCyclePos);
        assertThat(billingCyclePos).isLessThan(budgetBillPos);
        assertThat(budgetBillPos).isLessThan(lastBillAmountPos);
        assertThat(lastBillAmountPos).isLessThan(contactInfoPos);
        assertThat(contactInfoPos).isLessThan(accountIdPos);
    }

    @Test
    @DisplayName("Should use correct customer namespace")
    void shouldUseCorrectCustomerNamespace() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.of(2025, 1, 22, 10, 30, 0, 0, ZoneOffset.UTC);

        CustomerAccountDto customerAccount = createMinimalCustomerAccountDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:test-id", "Test", now, now, null, customerAccount
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
            .doesNotContain("espi:CustomerAccount")
            .contains("<cust:CustomerAccount");
    }

    // Helper methods

    private CustomerAccountDto createFullCustomerAccountDto() {
        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
            null, null, "billing@example.com", "support@example.com", "https://www.example.com", null, null, null
        );

        CustomerAccountDto.StatusDto docStatus = new CustomerAccountDto.StatusDto(
            "ACTIVE",
            OffsetDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC),
            null,
            "Account in good standing"
        );

        OrganisationDto contactInfo = new OrganisationDto(
            new CustomerDto.StreetAddressDto("123 Main St", "Springfield", "IL", "62701", "USA"),
            null, null, null,
            new ElectronicAddressDto(null, null, "contact@acme.com", null, "https://acme.com", null, null, null),
            "ACME Corporation"
        );

        return new CustomerAccountDto(
            "BILLING",
            "Billing System",
            OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2025, 1, 22, 10, 30, 0, 0, ZoneOffset.UTC),
            "1.0",
            electronicAddress,
            "Monthly Billing Account",
            "Customer Billing Account #12345",
            docStatus,
            "MONTHLY",
            "STANDARD",
            15000L,
            null,
            contactInfo,
            "ACCT-12345"
        );
    }

    private CustomerAccountDto createMinimalCustomerAccountDto() {
        return new CustomerAccountDto(
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, "ACCT-MIN"
        );
    }
}
