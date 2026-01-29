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
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.utils.EspiNamespacePrefixMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Debug test for Customer Domain XML generation (Customer namespace only).
 * <p>
 * Tests CustomerAtomEntryDto with customer.xsd resources to validate:
 * - Only xmlns:cust="http://naesb.org/espi/customer" is declared
 * - xmlns:espi (usage namespace) is NOT declared
 * - Atom namespace is default (no prefix)
 * <p>
 * Per NAESB ESPI standard, usage and customer domains are mutually exclusive.
 */
@DisplayName("Customer Domain XML Debug Test - JAXB")
class CustomerXmlDebugTest {

    private JAXBContext jaxbContext;

    @BeforeEach
    void setUp() throws JAXBException {
        // Initialize Jakarta JAXB Context with CUSTOMER DOMAIN ONLY
        // This ensures ONLY cust namespace is declared, NOT espi namespace
        // Note: Excluding AtomFeedDto to prevent JAXB from discovering UsageAtomEntryDto
        jaxbContext = JAXBContext.newInstance(
                // Atom protocol classes
                org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto.class,  // ONLY customer, NOT usage
                org.greenbuttonalliance.espi.common.dto.atom.LinkDto.class,

                // Customer domain classes (http://naesb.org/espi/customer)
                org.greenbuttonalliance.espi.common.dto.customer.CustomerDto.class,
                org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto.class,
                org.greenbuttonalliance.espi.common.dto.customer.CustomerAgreementDto.class,
                org.greenbuttonalliance.espi.common.dto.customer.EndDeviceDto.class,
                org.greenbuttonalliance.espi.common.dto.customer.MeterDto.class,
                org.greenbuttonalliance.espi.common.dto.customer.ProgramDateIdMappingsDto.class,
                org.greenbuttonalliance.espi.common.dto.customer.ServiceLocationDto.class,
                org.greenbuttonalliance.espi.common.dto.customer.StatementDto.class,
                org.greenbuttonalliance.espi.common.dto.customer.StatementRefDto.class
        );
    }

    /**
     * Creates a marshaller configured for customer domain resources.
     */
    private Marshaller createMarshallerForCustomerDomain() throws JAXBException {
        // Required namespaces: Atom + cust ONLY (no espi)
        Set<String> requiredNamespaces = new HashSet<>();
        requiredNamespaces.add("http://www.w3.org/2005/Atom");
        requiredNamespaces.add("http://naesb.org/espi/customer");

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);

        // Set namespace prefix mapper
        EspiNamespacePrefixMapper prefixMapper = new EspiNamespacePrefixMapper(requiredNamespaces);
        try {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", prefixMapper);
        } catch (Exception e) {
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", prefixMapper);
        }

        return marshaller;
    }

    @Test
    @DisplayName("Should declare ONLY customer namespace (NOT usage namespace)")
    void shouldDeclareCustomerNamespaceOnly() throws Exception {
        // Arrange
        CustomerDto customer = new CustomerDto(
            null,  // organisation
            null,  // kind
            "Wheelchair access required",  // specialNeed - add actual value to force namespace usage
            true,  // vip
            null,  // pucNumber
            null,  // status
            null,  // priority
            null,  // locale
            "John Doe"   // customerName
        );
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto("urn:uuid:550e8400-e29b-51d4-a716-446655440002", "Customer Test", customer);

        // Act
        Marshaller marshaller = createMarshallerForCustomerDomain();
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

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

        // Assert - Atom namespace with atom prefix
        assertThat(xml)
            .as("XML should declare Atom namespace with atom prefix")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        // Assert - Customer content with cust prefix
        assertThat(xml)
            .as("Customer should use cust prefix")
            .contains("<cust:Customer");
    }

    @Test
    @DisplayName("Should use cust prefix for Customer elements")
    void shouldUseCustPrefixForCustomer() throws Exception {
        // Arrange
        CustomerDto customer = new CustomerDto(
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
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto("urn:uuid:550e8400-e29b-51d4-a716-446655440004", "Prefix Test", customer);

        // Act
        Marshaller marshaller = createMarshallerForCustomerDomain();
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Assert
        assertThat(xml).contains("<cust:Customer>");
        assertThat(xml).contains("</cust:Customer>");
    }

    @Test
    @DisplayName("Should use Atom as default namespace (no prefix on entry/id/title)")
    void shouldUseAtomAsDefaultNamespace() throws Exception {
        // Arrange
        CustomerDto customer = new CustomerDto(
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
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto("urn:uuid:550e8400-e29b-51d4-a716-446655440006", "Atom Test", customer);

        // Act
        Marshaller marshaller = createMarshallerForCustomerDomain();
        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Assert - Atom elements use atom prefix
        assertThat(xml)
            .as("entry element should use atom prefix")
            .contains("<atom:entry");

        assertThat(xml).contains("<atom:id>urn:uuid:550e8400-e29b-51d4-a716-446655440006</atom:id>");
        assertThat(xml).contains("<atom:title>Atom Test</atom:title>");
        assertThat(xml).doesNotContain("ns5:id");
        assertThat(xml).doesNotContain("ns5:title");
        assertThat(xml).doesNotContain("ns3:id");
        assertThat(xml).doesNotContain("ns3:title");
    }

    @Test
    @DisplayName("Debug: See complete Customer Domain XML structure")
    void debugCompleteCustomerDomainXml() throws Exception {
        // Arrange
        CustomerDto customer = new CustomerDto(
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
        Marshaller marshaller = createMarshallerForCustomerDomain();

        // Add listener for debugging
        marshaller.setListener(new Marshaller.Listener() {
            @Override
            public void beforeMarshal(Object source) {
                System.out.println("[JAXB] beforeMarshal: " + source.getClass().getSimpleName());
            }
        });

        StringWriter writer = new StringWriter();
        marshaller.marshal(entry, writer);
        String xml = writer.toString();

        // Debug output
        System.out.println("\n========== Complete Customer Domain XML ==========");
        System.out.println(xml);
        System.out.println("=================================================\n");

        // Assert comprehensive structure
        assertThat(xml).contains("xmlns:cust=\"http://naesb.org/espi/customer\"");
        assertThat(xml).doesNotContain("xmlns:espi");
        assertThat(xml).contains("<cust:Customer>");
        assertThat(xml).contains("urn:uuid:550e8400-e29b-51d4-a716-446655440008");
        assertThat(xml).contains("Residential Customer - Customer Domain");
    }
}
