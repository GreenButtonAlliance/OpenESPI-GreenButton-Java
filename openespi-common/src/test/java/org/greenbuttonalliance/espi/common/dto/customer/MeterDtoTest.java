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

import org.greenbuttonalliance.espi.common.domain.customer.enums.MeterMultiplierKind;
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.service.impl.DtoExportServiceImpl;
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
 * XML marshalling/unmarshalling tests for MeterDto.
 * Verifies Jakarta JAXB Marshaller processes JAXB annotations correctly for ESPI 4.0 customer.xsd compliance.
 * Meter extends EndDevice which extends Asset - tests verify all 19 fields (12 Asset + 4 EndDevice + 3 Meter).
 */
@DisplayName("MeterDto XML Marshalling Tests")
class MeterDtoTest {

    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        // Initialize DtoExportService with null repository/mapper (not needed for DTO-only tests)
        org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService espiIdGeneratorService =
            new org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService();
        dtoExportService = new DtoExportServiceImpl(null, null, espiIdGeneratorService);
    }

    @Test
    @DisplayName("Should export Meter with complete realistic data")
    void shouldExportMeterWithRealisticData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        MeterDto meter = createFullMeterDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:770e8400-e29b-51d4-a716-446655440000",
            "Electric Meter Device",
            now, now, null, meter
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Meter Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Debug output
        System.out.println("========== Meter XML Output ==========");
        System.out.println(xml);
        System.out.println("======================================");

        // Assert - Basic structure and namespaces
        assertThat(xml)
                .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .contains("<atom:feed")
                .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"")
                .contains("http://naesb.org/espi/customer")
                .contains("cust:")
                .contains("<cust:Meter")
                .contains("</cust:Meter>");

        // Assert - Asset fields present (12)
        assertThat(xml)
                .contains("<cust:type>ELECTRIC_METER</cust:type>")
                .contains("<cust:serialNumber>EM-2025-12345</cust:serialNumber>")
                .contains("<cust:utcNumber>UTC-67890</cust:utcNumber>")
                .contains("<cust:lotNumber>LOT-2025-BATCH-A</cust:lotNumber>")
                .contains("<cust:purchasePrice>35000</cust:purchasePrice>")
                .contains("<cust:critical>true</cust:critical>");

        // Assert - EndDevice fields present (4)
        assertThat(xml)
                .contains("<cust:isVirtual>false</cust:isVirtual>")
                .contains("<cust:isPan>false</cust:isPan>")
                .contains("<cust:installCode>INST-METER-9876</cust:installCode>")
                .contains("<cust:amrSystem>OpenWay CENTRON</cust:amrSystem>");

        // Assert - Meter specific fields present (3)
        assertThat(xml)
                .contains("<cust:formNumber>16S</cust:formNumber>")
                .contains("<cust:intervalLength>900</cust:intervalLength>");
    }

    @Test
    @DisplayName("Should verify Meter field order matches customer.xsd")
    void shouldVerifyMeterFieldOrder() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        MeterDto meter = createFullMeterDto();
        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:770e8400-e29b-51d4-a716-446655440001",
            "Test Meter",
            now, now, null, meter
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "Test Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Verify XSD element order per customer.xsd
        // Asset fields (12): type, utcNumber, serialNumber, lotNumber, purchasePrice, critical,
        //                     electronicAddress, lifecycle, acceptanceTest, initialCondition, initialLossOfLife, status
        // EndDevice fields (4): isVirtual, isPan, installCode, amrSystem
        // Meter fields (3): formNumber, meterMultipliers, intervalLength
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
        int formNumberPos = xml.indexOf("<cust:formNumber>");
        int intervalLengthPos = xml.indexOf("<cust:intervalLength>");

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
        assertThat(amrSystemPos).isLessThan(formNumberPos);
        // meterMultipliers would be between formNumber and intervalLength (if present)
        assertThat(formNumberPos).isLessThan(intervalLengthPos);
    }

    @Test
    @DisplayName("Should export minimal Meter with only required fields")
    void shouldExportMinimalMeter() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        MeterDto meter = new MeterDto();
        // No required fields per XSD - all fields are optional
        meter.setSerialNumber("MINIMAL-METER-001");
        meter.setFormNumber("1S");

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:770e8400-e29b-51d4-a716-446655440002",
            "Minimal Meter",
            now, now, null, meter
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
                .contains("<cust:Meter")
                .contains("</cust:Meter>")
                .contains("<cust:serialNumber>MINIMAL-METER-001</cust:serialNumber>")
                .contains("<cust:formNumber>1S</cust:formNumber>");
    }

    @Test
    @DisplayName("Should export Meter with lifecycle dates")
    void shouldExportMeterWithLifecycleDates() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        LifecycleDateDto lifecycle = new LifecycleDateDto(
                now.minusDays(120),  // manufacturedDate
                now.minusDays(45)   // installationDate
        );

        MeterDto meter = new MeterDto();
        meter.setSerialNumber("LC-METER-001");
        meter.setLifecycle(lifecycle);

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:770e8400-e29b-51d4-a716-446655440003",
            "Lifecycle Meter",
            now, now, null, meter
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
    @DisplayName("Should export Meter with acceptance test")
    void shouldExportMeterWithAcceptanceTest() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AcceptanceTestDto acceptanceTest = new AcceptanceTestDto(
                now.minusDays(10),  // dateTime
                true,               // success
                "FIELD_CALIBRATION",  // type
                "Meter accuracy verified"  // remark
        );

        MeterDto meter = new MeterDto();
        meter.setSerialNumber("AT-METER-001");
        meter.setAcceptanceTest(acceptanceTest);

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:770e8400-e29b-51d4-a716-446655440004",
            "AcceptanceTest Meter",
            now, now, null, meter
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
                .contains("<cust:type>FIELD_CALIBRATION</cust:type>")
                .contains("<cust:remark>Meter accuracy verified</cust:remark>")
                .contains("</cust:acceptanceTest>");
    }

    @Test
    @DisplayName("Should export Meter with meterMultipliers collection")
    void shouldExportMeterWithMeterMultipliers() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        MeterMultiplierDto ctRatio = new MeterMultiplierDto(MeterMultiplierKind.CT_RATIO, 120.0f);
        MeterMultiplierDto ptRatio = new MeterMultiplierDto(MeterMultiplierKind.PT_RATIO, 60.0f);

        MeterDto meter = new MeterDto();
        meter.setSerialNumber("MM-METER-001");
        meter.setFormNumber("16S");
        meter.setMeterMultipliers(List.of(ctRatio, ptRatio));

        CustomerAtomEntryDto entry = new CustomerAtomEntryDto(
            "urn:uuid:770e8400-e29b-51d4-a716-446655440005",
            "MeterMultipliers Meter",
            now, now, null, meter
        );

        AtomFeedDto feed = new AtomFeedDto(
            "urn:uuid:feed-id", "MeterMultipliers Feed", now, now, null,
            List.of(entry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(feed, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - MeterMultipliers collection present
        assertThat(xml)
                .contains("<cust:MeterMultipliers>")
                .contains("<cust:kind>ctRatio</cust:kind>")
                .contains("<cust:value>120.0</cust:value>")
                .contains("</cust:MeterMultipliers>")
                .contains("<cust:MeterMultipliers>")
                .contains("<cust:kind>ptRatio</cust:kind>")
                .contains("<cust:value>60.0</cust:value>");
    }

    /**
     * Helper method to create a fully populated MeterDto for testing.
     * Creates all 19 fields: 12 Asset + 4 EndDevice + 3 Meter.
     */
    private MeterDto createFullMeterDto() {
        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
                "192.168.50.100",  // lan
                "00:1B:44:11:3A:B7",  // mac
                "meter@pge.com",  // email1
                null,  // email2
                "https://meter-portal.pge.com",  // web
                null,  // radio
                "meter_admin",  // userID
                null   // password
        );

        LifecycleDateDto lifecycle = new LifecycleDateDto(
                OffsetDateTime.now().minusDays(120),  // manufacturedDate
                OffsetDateTime.now().minusDays(45)   // installationDate
        );

        AcceptanceTestDto acceptanceTest = new AcceptanceTestDto(
                OffsetDateTime.now().minusDays(40),  // dateTime
                true,  // success
                "FIELD_CALIBRATION",  // type
                "All tests passed"   // remark
        );

        StatusDto status = new StatusDto(
                "COMMISSIONED",  // value
                OffsetDateTime.now().minusDays(39),  // dateTime
                "Meter is operational",  // remark
                "Installation and testing complete"  // reason
        );

        MeterMultiplierDto ctRatio = new MeterMultiplierDto(MeterMultiplierKind.CT_RATIO, 200.0f);
        MeterMultiplierDto ptRatio = new MeterMultiplierDto(MeterMultiplierKind.PT_RATIO, 120.0f);

        // Create MeterDto with all 19 fields (12 Asset + 4 EndDevice + 3 Meter)
        return new MeterDto(
                // Asset fields (12)
                "ELECTRIC_METER",  // type
                "UTC-67890",  // utcNumber
                "EM-2025-12345",  // serialNumber
                "LOT-2025-BATCH-A",  // lotNumber
                35000L,  // purchasePrice (in cents)
                true,  // critical
                electronicAddress,
                lifecycle,
                acceptanceTest,
                "NEW",  // initialCondition
                0,  // initialLossOfLife (0%)
                status,
                // EndDevice fields (4)
                false,  // isVirtual
                false,  // isPan
                "INST-METER-9876",  // installCode
                "OpenWay CENTRON",  // amrSystem
                // Meter fields (3)
                "16S",  // formNumber (16S = 3-wire delta, 3-phase)
                List.of(ctRatio, ptRatio),  // meterMultipliers
                900L  // intervalLength (15 minutes in seconds)
        );
    }
}
