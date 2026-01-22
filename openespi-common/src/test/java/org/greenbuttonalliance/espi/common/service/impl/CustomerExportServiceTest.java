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

package org.greenbuttonalliance.espi.common.service.impl;

import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CustomerExportService namespace handling.
 * <p>
 * Verifies that customer domain exports:
 * - Declare ONLY Atom and Customer namespaces (NO usage namespace)
 * - Use Atom as default namespace (no prefix on entry, id, title)
 * - Use cust: prefix for customer domain elements
 */
@DisplayName("CustomerExportService Namespace Tests")
class CustomerExportServiceTest {

    private CustomerExportService customerExportService;

    @BeforeEach
    void setUp() {
        customerExportService = new CustomerExportService();
        // Manually initialize since we're not using Spring context
        customerExportService.init();
    }

    @Test
    @DisplayName("Should declare ONLY customer namespace (NOT usage namespace)")
    void shouldDeclareCustomerNamespaceOnly() {
        // Arrange
        CustomerDto customer = new CustomerDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440001",  // uuid
            null,  // organisation
            null,  // kind
            "Wheelchair access required",  // specialNeed
            true,  // vip
            null,  // pucNumber
            null,  // status
            null,  // priority
            null,  // locale
            "John Doe"   // customerName
        );
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440002",
            "Customer Test",
            customer
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        customerExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Debug output
        System.out.println("\n========== Customer Domain XML Output ==========");
        System.out.println(xml);
        System.out.println("===============================================\n");

        // Assert - Customer namespace PRESENT
        assertThat(xml)
            .as("XML should declare customer namespace")
            .contains("xmlns:cust=\"http://naesb.org/espi/customer\"");

        // Assert - Usage namespace ABSENT
        assertThat(xml)
            .as("XML should NOT declare usage namespace")
            .doesNotContain("xmlns:espi=\"http://naesb.org/espi\"")
            .doesNotContain("xmlns:espi");  // Ensure no espi prefix at all

        // Assert - Atom namespace is declared with atom prefix
        assertThat(xml)
            .as("XML should declare Atom namespace with atom prefix")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        // Assert - Customer content with cust prefix
        assertThat(xml)
            .as("Customer should use cust prefix")
            .contains("<cust:Customer");
    }

    @Test
    @DisplayName("Should use atom prefix for Atom elements")
    void shouldUseAtomPrefixForAtomElements() {
        // Arrange
        CustomerDto customer = new CustomerDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440003",  // uuid
            null,  // organisation
            null,  // kind
            null,  // specialNeed
            false,  // vip
            null,  // pucNumber
            null,  // status
            null,  // priority
            "en-US",  // locale
            "Jane Smith"   // customerName
        );
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440004",
            "Atom Prefix Test",
            customer
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        customerExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Assert - Atom elements WITH atom: prefix
        assertThat(xml)
            .as("entry element should have atom prefix")
            .contains("<atom:entry");

        assertThat(xml)
            .as("id element should have atom prefix")
            .contains("<atom:id>urn:uuid:550e8400-e29b-51d4-a716-446655440004</atom:id>");

        assertThat(xml)
            .as("title element should have atom prefix")
            .contains("<atom:title>Atom Prefix Test</atom:title>");

        // Assert - NO ns3/ns5 generic prefixes on Atom elements
        assertThat(xml)
            .as("Should NOT have ns3 or ns5 generic prefixes")
            .doesNotContain("ns3:id")
            .doesNotContain("ns3:title")
            .doesNotContain("ns5:id")
            .doesNotContain("ns5:title")
            .doesNotContain("ns3:entry")
            .doesNotContain("ns5:entry");
    }

    @Test
    @DisplayName("Should use cust prefix for Customer elements")
    void shouldUseCustPrefixForCustomer() {
        // Arrange
        CustomerDto customer = new CustomerDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440005",  // uuid
            null,  // organisation
            null,  // kind
            null,  // specialNeed
            null,  // vip
            "PUC-12345",  // pucNumber
            null,  // status
            null,  // priority
            null,  // locale
            "Test Customer"   // customerName
        );
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440006",
            "CUST Prefix Test",
            customer
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        customerExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Assert
        assertThat(xml).contains("<cust:Customer>");
        assertThat(xml).contains("</cust:Customer>");
    }

    @Test
    @DisplayName("Should produce valid ESPI XML structure")
    void shouldProduceValidEspiXmlStructure() {
        // Arrange
        CustomerDto customer = new CustomerDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440007",  // uuid
            null,  // organisation
            null,  // kind
            "Hearing impaired",  // specialNeed
            true,  // vip
            "PUC-999",  // pucNumber
            null,  // status
            null,  // priority
            "en-US",  // locale
            "Debug Customer"   // customerName
        );

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440008",
            "Residential Customer - Customer Domain",
            customer
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        customerExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Debug output
        System.out.println("\n========== Complete Customer Domain XML ==========");
        System.out.println(xml);
        System.out.println("==================================================\n");

        // Assert comprehensive structure
        assertThat(xml).contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");
        assertThat(xml).contains("xmlns:cust=\"http://naesb.org/espi/customer\"");
        assertThat(xml).doesNotContain("xmlns:espi");  // No usage namespace pollution
        assertThat(xml).doesNotContain("ns3:");  // No generic prefixes
        assertThat(xml).doesNotContain("ns5:");  // No generic prefixes
        assertThat(xml).contains("<atom:entry");  // Atom elements use atom prefix
        assertThat(xml).contains("<cust:Customer>");  // Customer elements use cust prefix
        assertThat(xml).contains("urn:uuid:550e8400-e29b-51d4-a716-446655440008");
        assertThat(xml).contains("Residential Customer - Customer Domain");
    }
}
