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
import org.greenbuttonalliance.espi.common.service.impl.CustomerExportService;
import org.greenbuttonalliance.espi.common.dto.customer.ElectronicAddressDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CustomerDto XML marshalling to verify ESPI 4.0 customer.xsd compliance.
 * <p>
 * Validates that CustomerDto:
 * - Matches customer.xsd field sequence exactly
 * - Properly marshals all embedded types (Organisation, Status, Priority)
 * - Uses correct namespace (http://naesb.org/espi/customer)
 * - Produces valid XML with cust: prefix
 */
@DisplayName("CustomerDto XML Marshalling Tests")
class CustomerDtoMarshallingTest {

    private CustomerExportService customerExportService;

    @BeforeEach
    void setUp() {
        customerExportService = new CustomerExportService();
        customerExportService.init();
    }

    @Test
    @DisplayName("Should marshal Customer with all fields populated")
    void shouldMarshalCustomerWithAllFields() {
        // Arrange - Create comprehensive CustomerDto with all fields
        StatusDto status = new StatusDto(
            "active",
            OffsetDateTime.now(),
            "New account created",
            null
        );

        CustomerDto.PriorityDto priority = new CustomerDto.PriorityDto(
            1,    // value
            10,   // rank
            "high-priority"  // type
        );

        CustomerDto.TelephoneNumberDto phone1 = new CustomerDto.TelephoneNumberDto(
            "1",        // countryCode
            "415",      // areaCode
            "555",      // cityCode
            "1234",     // localNumber
            "100",      // ext
            null,       // dialOut
            null,       // internationalPrefix
            null        // ituPhone
        );

        CustomerDto.TelephoneNumberDto phone2 = new CustomerDto.TelephoneNumberDto(
            "1",
            "415",
            "555",
            "5678",
            null,
            null,
            null,
            null
        );

        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
            null,                        // lan
            null,                        // mac
            "customer@example.com",      // email1
            "billing@example.com",       // email2
            "https://customer.example.com",  // web
            null,                        // radio
            null,                        // userID
            null                         // password
        );

        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "123 Main Street, Apt 4B",   // streetDetail
            "San Francisco",             // townDetail
            "CA",                        // stateOrProvince
            "94102",                     // postalCode
            "USA"                        // country
        );

        CustomerDto.StreetAddressDto postalAddress = new CustomerDto.StreetAddressDto(
            "PO Box 789",
            "San Francisco",
            "CA",
            "94103",
            "USA"
        );

        CustomerDto.OrganisationDto organisation = new CustomerDto.OrganisationDto(
            streetAddress,
            postalAddress,
            phone1,
            phone2,
            electronicAddress,
            "ACME Energy Services"  // organisationName
        );

        CustomerDto customer = new CustomerDto(
            organisation,
            CustomerKind.RESIDENTIAL,
            "Wheelchair access required",  // specialNeed
            true,                          // vip
            "PUC-12345",                   // pucNumber
            status,
            priority,
            "en-US",                       // locale
            "John Q. Public"               // customerName
        );

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440001",
            "Residential Customer - Full Data",
            customer
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        customerExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Debug output
        System.out.println("\n========== Customer Full XML Output ==========");
        System.out.println(xml);
        System.out.println("==============================================\n");

        // Assert - Namespace declarations
        assertThat(xml)
            .as("Should declare customer namespace with cust prefix")
            .contains("xmlns:cust=\"http://naesb.org/espi/customer\"");

        assertThat(xml)
            .as("Should declare Atom namespace with atom prefix")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        assertThat(xml)
            .as("Should NOT declare usage namespace")
            .doesNotContain("xmlns:espi=\"http://naesb.org/espi\"");

        // Assert - Field order matches customer.xsd (organisation, kind, specialNeed, vip, pucNumber, status, priority, locale, customerName)
        assertThat(xml).contains("<cust:Customer>");

        // Organisation should come first
        int organisationIndex = xml.indexOf("<cust:Organisation>");
        assertThat(organisationIndex)
            .as("Organisation element should be present")
            .isGreaterThan(0);

        // Kind should come after Organisation
        int kindIndex = xml.indexOf("<cust:kind>");
        assertThat(kindIndex)
            .as("kind should come after Organisation")
            .isGreaterThan(organisationIndex);

        // specialNeed after kind
        int specialNeedIndex = xml.indexOf("<cust:specialNeed>");
        assertThat(specialNeedIndex)
            .as("specialNeed should come after kind")
            .isGreaterThan(kindIndex);

        // vip after specialNeed
        int vipIndex = xml.indexOf("<cust:vip>");
        assertThat(vipIndex)
            .as("vip should come after specialNeed")
            .isGreaterThan(specialNeedIndex);

        // pucNumber after vip
        int pucNumberIndex = xml.indexOf("<cust:pucNumber>");
        assertThat(pucNumberIndex)
            .as("pucNumber should come after vip")
            .isGreaterThan(vipIndex);

        // status after pucNumber
        int statusIndex = xml.indexOf("<cust:status>");
        assertThat(statusIndex)
            .as("status should come after pucNumber")
            .isGreaterThan(pucNumberIndex);

        // priority after status
        int priorityIndex = xml.indexOf("<cust:priority>");
        assertThat(priorityIndex)
            .as("priority should come after status")
            .isGreaterThan(statusIndex);

        // locale after priority
        int localeIndex = xml.indexOf("<cust:locale>");
        assertThat(localeIndex)
            .as("locale should come after priority")
            .isGreaterThan(priorityIndex);

        // customerName after locale
        int customerNameIndex = xml.indexOf("<cust:customerName>");
        assertThat(customerNameIndex)
            .as("customerName should come after locale")
            .isGreaterThan(localeIndex);

        // Assert - Field values
        assertThat(xml).contains("ACME Energy Services");
        assertThat(xml).contains("RESIDENTIAL");
        assertThat(xml).contains("Wheelchair access required");
        assertThat(xml).contains("<cust:vip>true</cust:vip>");
        assertThat(xml).contains("PUC-12345");
        assertThat(xml).contains("en-US");
        assertThat(xml).contains("John Q. Public");

        // Assert - Embedded types
        assertThat(xml).contains("<cust:Organisation>");
        assertThat(xml).contains("<cust:status>");
        assertThat(xml).contains("<cust:priority>");
        assertThat(xml).contains("<cust:streetAddress>");
        assertThat(xml).contains("<cust:postalAddress>");
        assertThat(xml).contains("<cust:electronicAddress>");
    }

    @Test
    @DisplayName("Should marshal Customer with minimal fields")
    void shouldMarshalCustomerWithMinimalFields() {
        // Arrange - Create minimal CustomerDto (only customerName required)
        CustomerDto customer = new CustomerDto(
            null,  // organisation
            null,  // kind
            null,  // specialNeed
            null,  // vip
            null,  // pucNumber
            null,  // status
            null,  // priority
            null,  // locale
            "Jane Smith"  // customerName
        );

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440011",
            "Minimal Customer",
            customer
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        customerExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Assert
        assertThat(xml).contains("<cust:Customer>");
        assertThat(xml).contains("Jane Smith");
        assertThat(xml).contains("xmlns:cust=\"http://naesb.org/espi/customer\"");
    }

    @Test
    @DisplayName("Should use cust prefix for all Customer elements")
    void shouldUseCustPrefixForAllElements() {
        // Arrange
        CustomerDto customer = new CustomerDto(
            null,
            CustomerKind.COMMERCIAL,
            "Test special need",
            false,
            "PUC-999",
            null,
            null,
            "en-GB",
            "Test Customer Inc"
        );

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:test-prefix-entry",
            "Prefix Test",
            customer
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        customerExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Assert - All customer elements should have cust: prefix
        assertThat(xml).contains("<cust:Customer>");
        assertThat(xml).contains("</cust:Customer>");
        assertThat(xml).contains("<cust:kind>");
        assertThat(xml).contains("<cust:specialNeed>");
        assertThat(xml).contains("<cust:vip>");
        assertThat(xml).contains("<cust:pucNumber>");
        assertThat(xml).contains("<cust:locale>");
        assertThat(xml).contains("<cust:customerName>");

        // Assert - NO generic ns3/ns5 prefixes
        assertThat(xml).doesNotContain("ns3:");
        assertThat(xml).doesNotContain("ns5:");
    }
}