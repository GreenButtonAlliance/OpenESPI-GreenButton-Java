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

import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.service.impl.DtoExportServiceImpl;
import org.greenbuttonalliance.espi.common.dto.customer.ElectronicAddressDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XML marshalling/unmarshalling tests for EndDeviceDto.
 * Verifies Jakarta JAXB Marshaller processes JAXB annotations correctly for ESPI 4.0 customer.xsd compliance.
 * Follows the same pattern as CustomerDtoTest for customer domain resources.
 */
@DisplayName("EndDeviceDto XML Marshalling Tests")
class EndDeviceDtoTest {

    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService espiIdGeneratorService =
                new org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService();
        dtoExportService = new DtoExportServiceImpl(null, null, null, null, null, null, null, null, null, null, espiIdGeneratorService);
    }

    @Test
    @DisplayName("Should export EndDevice with complete realistic data")
    void shouldExportEndDeviceWithRealisticData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        EndDeviceDto endDevice = createFullEndDeviceDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:660e8400-e29b-51d4-a716-446655440000",
            "Smart Meter Device",
            now, now, null, endDevice
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "EndDevice Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Debug output
        System.out.println("========== EndDevice XML Output ==========");
        System.out.println(xml);
        System.out.println("==========================================");

        // Assert - Basic structure and namespaces
        assertThat(xml)
                .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .contains("<atom:feed")
                .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"")
                .contains("http://naesb.org/espi/customer")
                .contains("cust:")
                .contains("<cust:EndDevice")
                .contains("</cust:EndDevice>");

        // Assert - Asset fields present
        assertThat(xml)
                .contains("<cust:type>SMART_METER</cust:type>")
                .contains("<cust:serialNumber>SM-2025-001</cust:serialNumber>")
                .contains("<cust:utcNumber>UTC-12345</cust:utcNumber>")
                .contains("<cust:lotNumber>LOT-2025-Q1</cust:lotNumber>")
                .contains("<cust:purchasePrice>25000</cust:purchasePrice>")
                .contains("<cust:critical>true</cust:critical>");

        // Assert - EndDevice specific fields
        assertThat(xml)
                .contains("<cust:isVirtual>false</cust:isVirtual>")
                .contains("<cust:isPan>false</cust:isPan>")
                .contains("<cust:installCode>INST-CODE-12345</cust:installCode>")
                .contains("<cust:amrSystem>ZigBee Smart Energy 2.0</cust:amrSystem>");
    }

    @Test
    @DisplayName("Should verify EndDevice field order matches customer.xsd")
    void shouldVerifyEndDeviceFieldOrder() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        EndDeviceDto endDevice = createFullEndDeviceDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:660e8400-e29b-51d4-a716-446655440001",
            "Test EndDevice",
            now, now, null, endDevice
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify XSD element order per customer.xsd lines 577-662
        // Asset fields (12): type, utcNumber, serialNumber, lotNumber, purchasePrice, critical,
        //                     electronicAddress, lifecycle, acceptanceTest, initialCondition, initialLossOfLife, status
        // EndDevice fields (4): isVirtual, isPan, installCode, amrSystem
        int typePos = xml.indexOf("<cust:type>");
        int utcNumberPos = xml.indexOf("<cust:utcNumber>");
        int serialNumberPos = xml.indexOf("<cust:serialNumber>");
        int lotNumberPos = xml.indexOf("<cust:lotNumber>");
        int purchasePricePos = xml.indexOf("<cust:purchasePrice>");
        int criticalPos = xml.indexOf("<cust:critical>");
        int electronicAddressPos = xml.indexOf("<cust:electronicAddress>");
        int lifecyclePos = xml.indexOf("<cust:lifecycle>");
        int acceptanceTestPos = xml.indexOf("<cust:acceptanceTest>");
        int initialConditionPos = xml.indexOf("<cust:initialCondition>");
        int initialLossOfLifePos = xml.indexOf("<cust:initialLossOfLife>");
        int statusPos = xml.indexOf("<cust:status>");
        int isVirtualPos = xml.indexOf("<cust:isVirtual>");
        int isPanPos = xml.indexOf("<cust:isPan>");
        int installCodePos = xml.indexOf("<cust:installCode>");
        int amrSystemPos = xml.indexOf("<cust:amrSystem>");

        // Assert - Field ordering with chained assertions
        assertThat(typePos)
                .isGreaterThan(0)
                .isLessThan(utcNumberPos);
        assertThat(utcNumberPos).isLessThan(serialNumberPos);
        assertThat(serialNumberPos).isLessThan(lotNumberPos);
        assertThat(lotNumberPos).isLessThan(purchasePricePos);
        assertThat(purchasePricePos).isLessThan(criticalPos);
        assertThat(criticalPos).isLessThan(electronicAddressPos);
        assertThat(electronicAddressPos).isLessThan(lifecyclePos);
        assertThat(lifecyclePos).isLessThan(acceptanceTestPos);
        assertThat(acceptanceTestPos).isLessThan(initialConditionPos);
        assertThat(initialConditionPos).isLessThan(initialLossOfLifePos);
        assertThat(initialLossOfLifePos).isLessThan(statusPos);
        assertThat(statusPos).isLessThan(isVirtualPos);
        assertThat(isVirtualPos).isLessThan(isPanPos);
        assertThat(isPanPos).isLessThan(installCodePos);
        assertThat(installCodePos).isLessThan(amrSystemPos);
    }

    @Test
    @DisplayName("Should export minimal EndDevice with only required fields")
    void shouldExportMinimalEndDevice() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        EndDeviceDto endDevice = new EndDeviceDto();
        // No required fields per XSD - all fields are optional
        endDevice.setSerialNumber("MINIMAL-001");

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:660e8400-e29b-51d4-a716-446655440002",
            "Minimal EndDevice",
            now, now, null, endDevice
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Minimal Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Basic structure present even with minimal data
        assertThat(xml)
                .contains("<cust:EndDevice")
                .contains("</cust:EndDevice>")
                .contains("<cust:serialNumber>MINIMAL-001</cust:serialNumber>");
    }

    @Test
    @DisplayName("Should export EndDevice with lifecycle dates")
    void shouldExportEndDeviceWithLifecycleDates() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        LifecycleDateDto lifecycle = new LifecycleDateDto(
                now.minusDays(90),  // manufacturedDate
                now.minusDays(30)   // installationDate
        );

        EndDeviceDto endDevice = new EndDeviceDto();
        endDevice.setSerialNumber("LC-001");
        endDevice.setLifecycle(lifecycle);

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:660e8400-e29b-51d4-a716-446655440003",
            "Lifecycle EndDevice",
            now, now, null, endDevice
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Lifecycle Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Lifecycle dates present
        assertThat(xml)
                .contains("<cust:lifecycle>")
                .contains("<cust:manufacturedDate>")
                .contains("<cust:installationDate>")
                .contains("</cust:lifecycle>");
    }

    @Test
    @DisplayName("Should export EndDevice with acceptance test")
    void shouldExportEndDeviceWithAcceptanceTest() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AcceptanceTestDto acceptanceTest = new AcceptanceTestDto(
                now.minusDays(7),  // dateTime
                true,              // success
                "FIELD_INSTALLATION",  // type
                null               // remark
        );

        EndDeviceDto endDevice = new EndDeviceDto();
        endDevice.setSerialNumber("AT-001");
        endDevice.setAcceptanceTest(acceptanceTest);

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:660e8400-e29b-51d4-a716-446655440004",
            "AcceptanceTest EndDevice",
            now, now, null, endDevice
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "AcceptanceTest Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Acceptance test present
        assertThat(xml)
                .contains("<cust:acceptanceTest>")
                .contains("<cust:dateTime>")
                .contains("<cust:success>true</cust:success>")
                .contains("<cust:type>FIELD_INSTALLATION</cust:type>")
                .contains("</cust:acceptanceTest>");
    }

    /**
     * Helper method to create a fully populated EndDeviceDto for testing.
     */
    private EndDeviceDto createFullEndDeviceDto() {
        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
                "192.168.1.100",  // lan
                "00:1A:2B:3C:4D:5E",  // mac
                "meter@utility.com",  // email1
                null,  // email2
                "https://meter.utility.com",  // web
                null,  // radio
                "meter_user",  // userID
                null   // password
        );

        LifecycleDateDto lifecycle = new LifecycleDateDto(
                OffsetDateTime.now().minusDays(90),  // manufacturedDate
                OffsetDateTime.now().minusDays(30)   // installationDate
        );

        AcceptanceTestDto acceptanceTest = new AcceptanceTestDto(
                OffsetDateTime.now().minusDays(25),  // dateTime
                true,  // success
                "FIELD_INSTALLATION",  // type
                null   // remark
        );

        StatusDto status = new StatusDto(
                "ACTIVE",  // value
                OffsetDateTime.now().minusDays(24),  // dateTime
                "Device operational",  // remark
                "Installation complete"  // reason
        );

        // Create EndDeviceDto with all 16 fields (12 Asset + 4 EndDevice)
        return new EndDeviceDto(
                // Asset fields (12)
                "SMART_METER",  // type
                "UTC-12345",  // utcNumber
                "SM-2025-001",  // serialNumber
                "LOT-2025-Q1",  // lotNumber
                25000L,  // purchasePrice
                true,  // critical
                electronicAddress,
                lifecycle,
                acceptanceTest,
                "NEW",  // initialCondition
                BigDecimal.ZERO,  // initialLossOfLife
                status,
                // EndDevice fields (4)
                false,  // isVirtual
                false,  // isPan
                "INST-CODE-12345",  // installCode
                "ZigBee Smart Energy 2.0"  // amrSystem
        );
    }
}