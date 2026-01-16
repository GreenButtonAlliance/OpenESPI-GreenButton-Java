package org.greenbuttonalliance.espi.common.service.impl;

import org.greenbuttonalliance.espi.common.domain.common.LinkType;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;
import org.greenbuttonalliance.espi.common.dto.BillingChargeSourceDto;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;
import org.greenbuttonalliance.espi.common.dto.usage.*;
import org.greenbuttonalliance.espi.common.mapper.usage.*;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class DtoExportServiceImplTest {

    private UsagePointRepository usagePointRepository;

    private UsagePointMapper usagePointMapper = new UsagePointMapperImpl();

    private MeterReadingMapper meterReadingMapper = new MeterReadingMapperImpl();
    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        // UsagePointMapper only needs serviceDeliveryPointMapper (no date fields after IdentifiedObject removal)
        ReflectionTestUtils.setField(usagePointMapper, "serviceDeliveryPointMapper", new ServiceDeliveryPointMapperImpl());

        dtoExportService = new DtoExportServiceImpl(usagePointRepository, usagePointMapper);
    }

    @Test
    @DisplayName("Should export Atom feed with valid XML structure and metadata")
    void shouldExportAtomFeedWithValidXmlStructure() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AtomEntryDto usagePointEntryDto = getUsagePointEntry(now);
        AtomEntryDto meterReadingEntryDto = getMeeterReadingEntryDto(now);
        AtomEntryDto readingEntry = getReadingEntryDto(now);
        AtomEntryDto intervalBlockEntry = getIntervlBlockEntryDto(now);
        AtomEntryDto timeConfigEntry = getTimeConfigurationEntry(now);
        AtomEntryDto usageSummaryEntry = getUsageSummaryEntry(now);
        AtomEntryDto epqsEntry = getElectricPowerQualitySummaryEntry(now);

        AtomFeedDto atomFeedDto = new AtomFeedDto("urn:uuid:15B0A4ED-CCF4-5521-A0A1-9FF650EC8A6B",
                "Green Button Subscription Feed",
                now, now, null,
                List.of(usagePointEntryDto, meterReadingEntryDto, readingEntry, intervalBlockEntry,
                        timeConfigEntry, usageSummaryEntry, epqsEntry));

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(atomFeedDto, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Print for debugging (can be removed later)
        System.out.println(xml);

        // Assert - XML Declaration
        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        // Assert - Root element
        assertThat(xml).contains("<feed xmlns=\"http://www.w3.org/2005/Atom\">");

        // Assert - Feed metadata (using Version-5 UUID)
        assertThat(xml).contains("<id>urn:uuid:15B0A4ED-CCF4-5521-A0A1-9FF650EC8A6B</id>");
        assertThat(xml).contains("<title>Green Button Subscription Feed</title>");
        assertThat(xml).contains("<updated>");
        assertThat(xml).contains("<published>");

        // Assert - Entry structure (may have namespace attributes)
        assertThat(xml).contains("<entry");  // Allow for attributes
        assertThat(xml).contains("</entry>");

        // Assert - ESPI namespace in content (URL and prefix usage)
        assertThat(xml).contains("http://naesb.org/espi");  // Namespace URL present
        assertThat(xml).contains("espi:");  // ESPI namespace prefix used
        assertThat(xml).contains("<content>");
        assertThat(xml).contains("</content>");

        // Assert - All 7 entries present
        assertThat(xml).contains("Front Electric Meter");  // UsagePoint title
        assertThat(xml).contains("Meter Reading");  // MeterReading title
        assertThat(xml).contains("Type of Meter Reading Data");  // ReadingType title
        assertThat(xml).contains("Interval Block");  // IntervalBlock title
        assertThat(xml).contains("EST Time Configuration");  // TimeConfiguration title
        assertThat(xml).contains("Monthly Usage Summary");  // UsageSummary title
        assertThat(xml).contains("Power Quality Summary");  // ElectricPowerQualitySummary title

        // Assert - UsageSummary fields with populated data
        assertThat(xml).contains("<billLastPeriod");
        assertThat(xml).contains("150000");  // billLastPeriod value
        assertThat(xml).contains("<currency");
        assertThat(xml).contains("USD");
        assertThat(xml).contains("<qualityOfReading");
        assertThat(xml).contains("14");  // qualityOfReading value
        assertThat(xml).contains("<tariffProfile");
        assertThat(xml).contains("TOU-Residential");
        assertThat(xml).contains("<tariffRiderRefs");
        assertThat(xml).contains("Green-Energy-Rider");
        assertThat(xml).contains("<billingChargeSource");
        assertThat(xml).contains("MEASURED");

        // Assert - ElectricPowerQualitySummary fields with populated data
        assertThat(xml).contains("<flickerPlt");
        assertThat(xml).contains("850");  // flickerPlt value
        assertThat(xml).contains("<harmonicVoltage");
        assertThat(xml).contains("320");  // harmonicVoltage value
        assertThat(xml).contains("<mainsVoltage");
        assertThat(xml).contains("120000");  // mainsVoltage value
        assertThat(xml).contains("<powerFrequency");
        assertThat(xml).contains("60000");  // powerFrequency value
        assertThat(xml).contains("<measurementProtocol");
        assertThat(xml).contains("<supplyVoltageImbalance");
        assertThat(xml).contains("150");  // supplyVoltageImbalance value
    }

    @Test
    @DisplayName("Should export Atom entries with proper metadata elements")
    void shouldExportAtomEntriesWithProperMetadata() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AtomEntryDto usagePointEntry = getUsagePointEntry(now);
        AtomFeedDto atomFeedDto = new AtomFeedDto(
                "urn:uuid:test-feed-id", "Test Feed", now, now, null,
                List.of(usagePointEntry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(atomFeedDto, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Entry has required Atom elements (may have namespace attributes)
        assertThat(xml).contains("<entry");  // Allow for attributes
        assertThat(xml).containsPattern("<id>urn:uuid:[0-9a-fA-F-]{36}</id>");  // UUID format
        assertThat(xml).contains("<title>");  // Should have title element
        assertThat(xml).contains("<published>");  // ISO 8601 timestamp
        assertThat(xml).contains("<updated>");  // ISO 8601 timestamp

        // Assert - Timestamps are ISO 8601 format
        assertThat(xml).containsPattern("<published>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertThat(xml).containsPattern("<updated>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("Should export ESPI UsagePoint content correctly")
    void shouldExportEspiUsagePointContent() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AtomEntryDto usagePointEntry = getUsagePointEntry(now);
        AtomFeedDto atomFeedDto = new AtomFeedDto(
                "urn:uuid:test-feed", "Test Feed", now, now, null,
                List.of(usagePointEntry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(atomFeedDto, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - ESPI namespace (URL and prefix usage)
        assertThat(xml).contains("http://naesb.org/espi");  // Namespace URL present
        assertThat(xml).contains("espi:");  // ESPI namespace prefix used

        // Assert - UsagePoint element with namespace prefix
        assertThat(xml).contains("<espi:UsagePoint");  // Opening tag (may have attributes)
        assertThat(xml).contains("</espi:UsagePoint>");

        // Assert - ServiceCategory field
        assertThat(xml).contains("ServiceCategory");
        assertThat(xml).contains("ELECTRICITY");
    }

    @Test
    @DisplayName("Should use Version-5 UUIDs in test data")
    void shouldUseVersion5UuidsInTestData() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AtomEntryDto usagePointEntry = getUsagePointEntry(now);
        AtomEntryDto readingEntry = getReadingEntryDto(now);
        AtomEntryDto intervalBlockEntry = getIntervlBlockEntryDto(now);

        AtomFeedDto atomFeedDto = new AtomFeedDto(
                "urn:uuid:15B0A4ED-CCF4-5521-A0A1-9FF650EC8A6B",  // Version-5
                "Test Feed", now, now, null,
                List.of(usagePointEntry, readingEntry, intervalBlockEntry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(atomFeedDto, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - Feed ID uses Version-5 UUID (note '5' in version field)
        assertThat(xml).contains("<id>urn:uuid:15B0A4ED-CCF4-5521-A0A1-9FF650EC8A6B</id>");

        // Assert - Entry IDs use Version-5 UUIDs
        assertThat(xml).contains("urn:uuid:48C2A019-5598-5E16-B0F9-49E4FF27F5FB");  // UsagePoint
        assertThat(xml).contains("urn:uuid:3430B025-65D5-593A-BEC2-053603C91CD7");  // ReadingType
        assertThat(xml).contains("urn:uuid:FE9A61BB-6913-52D4-88BE-9634A218EF53");  // IntervalBlock

        // Assert - Version field should be '5' (3rd group, 1st char)
        // Format: xxxxxxxx-xxxx-5xxx-xxxx-xxxxxxxxxxxx
        assertThat(xml).containsPattern("urn:uuid:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-5[0-9a-fA-F]{3}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    @Test
    @DisplayName("Should export ESPI ReadingType content correctly")
    void shouldExportEspiReadingTypeContent() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AtomEntryDto readingEntry = getReadingEntryDto(now);
        AtomFeedDto atomFeedDto = new AtomFeedDto(
                "urn:uuid:test-feed", "Test Feed", now, now, null,
                List.of(readingEntry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(atomFeedDto, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - ReadingType element (may have attributes)
        assertThat(xml).contains("<espi:ReadingType");
        assertThat(xml).contains("</espi:ReadingType>");

        // Assert - ReadingType fields
        assertThat(xml).contains("<accumulationBehaviour");
        assertThat(xml).contains("<commodity");
        assertThat(xml).contains("<dataQualifier");
        assertThat(xml).contains("<flowDirection");
        assertThat(xml).contains("<intervalLength");
    }

    @Test
    @DisplayName("Should export ESPI IntervalBlock content correctly")
    void shouldExportEspiIntervalBlockContent() throws IOException {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AtomEntryDto intervalBlockEntry = getIntervlBlockEntryDto(now);
        AtomFeedDto atomFeedDto = new AtomFeedDto(
                "urn:uuid:test-feed", "Test Feed", now, now, null,
                List.of(intervalBlockEntry)
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        dtoExportService.exportAtomFeed(atomFeedDto, stream);
        String xml = stream.toString(StandardCharsets.UTF_8);

        // Assert - IntervalBlock element (with ESPI namespace prefix)
        assertThat(xml).contains("IntervalBlock");  // IntervalBlock element present
        assertThat(xml).contains("espi:IntervalBlock>");  // With namespace prefix

        // Assert - IntervalBlock interval
        assertThat(xml).contains("<interval");
        assertThat(xml).contains("<start>");
        assertThat(xml).contains("<duration>");

        // Assert - IntervalReading elements
        assertThat(xml).contains("<IntervalReading");
        assertThat(xml).contains("<cost>");
        assertThat(xml).contains("<value>");
        assertThat(xml).contains("<timePeriod>");

        // Assert - Multiple readings present (4 readings in test data)
        int readingCount = xml.split("<IntervalReading").length - 1;
        assertThat(readingCount).isEqualTo(4);
    }

    private static @NonNull AtomEntryDto getIntervlBlockEntryDto(OffsetDateTime now) {
        List<IntervalReadingDto> intervalReadings = new ArrayList<>();
        // New constructor: cost, readingQualities, timePeriod, value, consumptionTier, tou, cpp
        intervalReadings.add(new IntervalReadingDto(974L, new ArrayList<>(List.of(new ReadingQualityDto("8"))), new DateTimeIntervalDto(1330578000L, 900L), 282L, null, null, null));

        intervalReadings.add(new IntervalReadingDto(965L, new ArrayList<>(List.of(new ReadingQualityDto("7"))), new DateTimeIntervalDto(1330578900L, 900L), 323L, null, null, null));

        // Using convenience constructor: value, cost, timePeriod
        intervalReadings.add(new IntervalReadingDto(294L, 884L, new DateTimeIntervalDto(1330579800L, 900L)));
        intervalReadings.add(new IntervalReadingDto(331L, 995L, new DateTimeIntervalDto(1330580700L, 900L)));

        IntervalBlockDto intervalBlockDto = new IntervalBlockDto("urn:uuid:FE9A61BB-6913-52D4-88BE-9634A218EF53",
                new DateTimeIntervalDto(1330578000L, 86400L), intervalReadings);

        List<LinkDto> intervalBlockLinks = new ArrayList<>();
        intervalBlockLinks.add(new LinkDto("self", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/MeterReading/01/IntervalBlock/173"));
        intervalBlockLinks.add(new LinkDto("up", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/MeterReading/01/IntervalBlock"));

        return new AtomEntryDto("urn:uuid:FE9A61BB-6913-52D4-88BE-9634A218EF53", "Interval Block", now, now,
                intervalBlockLinks, intervalBlockDto);
    }

    private static @NonNull AtomEntryDto getReadingEntryDto(OffsetDateTime now) {
        ReadingTypeDto readingTypeDto = new ReadingTypeDto(1L, "urn:uuid:3430B025-65D5-593A-BEC2-053603C91CD7",
                null, "4", "1", null, "840", "12", "NET", "TOTAL", 900L, "NET", "KILO", "DAILY", "V", "1", "CONTINUOUS", "1", null,
                null, null);

        // AtomContentDto readingTypeDtoContent = new AtomContentDto(readingTypeDto);
        List<LinkDto> readingTypeLinkList = new ArrayList<>();
        readingTypeLinkList.add(new LinkDto("self", "/espi/1_1/resource/ReadingType/07"));
        readingTypeLinkList.add(new LinkDto("up", "/espi/1_1/resource/ReadingType"));

        return new AtomEntryDto("urn:uuid:3430B025-65D5-593A-BEC2-053603C91CD7", "Type of Meter Reading Data", now, now,
                readingTypeLinkList, readingTypeDto);
    }

    private static @NonNull AtomEntryDto getMeeterReadingEntryDto(OffsetDateTime now) {
        MeterReadingDto meterReadingDto = new MeterReadingDto();

        List<LinkDto> meterReadingLinkList = new ArrayList<>();
        meterReadingLinkList.add(new LinkDto("self", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/MeterReading/01"));
        meterReadingLinkList.add(new LinkDto("up", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/MeterReading"));

        return new AtomEntryDto("urn:uuid:01", "Meter Reading", now, now, meterReadingLinkList, meterReadingDto);
    }

    AtomEntryDto getUsagePointEntry(OffsetDateTime now) {

        UsagePointEntity usagePointEntity = new UsagePointEntity();
        usagePointEntity.setId(UUID.fromString("48C2A019-5598-5E16-B0F9-49E4FF27F5FB"));
        usagePointEntity.setSelfLink(new LinkType("self", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F"));
        usagePointEntity.setUpLink(new LinkType("up", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint"));
        List<LinkType> relatedLinks = new ArrayList<>();
        relatedLinks.add(new LinkType("related","/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/MeterReading" ));
        relatedLinks.add(new LinkType("related","/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/ElectricPowerUsageSummary" ));
        relatedLinks.add(new LinkType("related","/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/ElectricPowerQualitySummary" ));
        relatedLinks.add(new LinkType("related","/espi/1_1/resource/LocalTimeParameters/01" ));
        usagePointEntity.setRelatedLinks(relatedLinks);

        usagePointEntity.setServiceCategory(ServiceCategory.ELECTRICITY);

        List<LinkDto> usagePointList = new ArrayList<>();

        usagePointList.add(new LinkDto("self","/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F" ));
        usagePointList.add(new LinkDto("up","\"/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint" ));
        usagePointList.add(new LinkDto("related","/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F" ));
        usagePointList.add(new LinkDto("related","/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/MeterReading" ));
        usagePointList.add(new LinkDto("related","/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/ElectricPowerUsageSummary" ));
        usagePointList.add(new LinkDto("related","/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/ElectricPowerQualitySummary" ));
        usagePointList.add(new LinkDto("related","/espi/1_1/resource/LocalTimeParameters/01" ));

        UsagePointDto usagePointDto = usagePointMapper.toDto(usagePointEntity);

        return new AtomEntryDto("urn:uuid:48C2A019-5598-5E16-B0F9-49E4FF27F5FB", "Front Electric Meter",
                now,
                now,
                usagePointList,
                usagePointDto);
    }

    private static @NonNull AtomEntryDto getTimeConfigurationEntry(OffsetDateTime now) {
        // TimeConfigurationDto full constructor: id, uuid, dstEndRule, dstOffset, dstStartRule, tzOffset
        TimeConfigurationDto timeConfigDto = new TimeConfigurationDto(
                null,      // id (ignored - handled by Atom layer)
                null,      // uuid (handled by Atom layer)
                null,      // dstEndRule (byte[])
                3600L,     // dstOffset (1 hour DST in seconds)
                null,      // dstStartRule (byte[])
                -18000L    // tzOffset (-5 hours in seconds = EST)
        );

        List<LinkDto> links = new ArrayList<>();
        links.add(new LinkDto("self", "/espi/1_1/resource/LocalTimeParameters/01"));
        links.add(new LinkDto("up", "/espi/1_1/resource/LocalTimeParameters"));

        return new AtomEntryDto("urn:uuid:2A0B8C3D-4E5F-5678-90AB-CDEF12345678", "EST Time Configuration",
                now, now, links, timeConfigDto);
    }

    private static @NonNull AtomEntryDto getUsageSummaryEntry(OffsetDateTime now) {
        // Canonical constructor: id, uuid, + 24 XSD fields with comprehensive test data
        UsageSummaryDto usageSummaryDto = new UsageSummaryDto(
                null,     // id
                null,     // uuid
                new DateTimeIntervalDto(1330578000L, 2592000L), // billingPeriod (30 days)
                150000L,  // billLastPeriod ($1500.00 in cents)
                175000L,  // billToDate ($1750.00 in cents)
                25000L,   // costAdditionalLastPeriod ($250.00 additional charges)
                List.of(new LineItemDto(15000L, 0L, 1332392400L, "Demand Charge", null, 2, null, null),
                        new LineItemDto(10000L, 0L, 1332392400L, "Service Fee", null, 3, null, null)),
                "USD",    // currency
                new SummaryMeasurementDto("3", 1330578000L, "72", 450000L, null), // overallConsumptionLastPeriod (450 kWh)
                new SummaryMeasurementDto("3", 1333256400L, "72", 425000L, null), // currentBillingPeriodOverAllConsumption (425 kWh)
                new SummaryMeasurementDto("3", 1330491600L, "72", 390000L, null), // currentDayLastYearNetConsumption (390 kWh)
                new SummaryMeasurementDto("3", 1333256400L, "72", 15000L, null),  // currentDayNetConsumption (15 kWh)
                new SummaryMeasurementDto("3", 1333256400L, "72", 16000L, null),  // currentDayOverallConsumption (16 kWh)
                new SummaryMeasurementDto("38", 1332392400L, "72", 5000L, null),  // peakDemand (5 kW)
                new SummaryMeasurementDto("3", 1330405200L, "72", 385000L, null), // previousDayLastYearOverallConsumption (385 kWh)
                new SummaryMeasurementDto("3", 1333170000L, "72", 14500L, null),  // previousDayNetConsumption (14.5 kWh)
                new SummaryMeasurementDto("3", 1333170000L, "72", 15500L, null),  // previousDayOverallConsumption (15.5 kWh)
                "14",     // qualityOfReading (VALID)
                new SummaryMeasurementDto("38", 1331182800L, "72", 4800L, null),  // ratchetDemand (4.8 kW)
                new DateTimeIntervalDto(1328000000L, 31536000L), // ratchetDemandPeriod (1 year)
                1333256400L, // statusTimeStamp
                1,        // commodity (ELECTRICITY_SECONDARY_METERED)
                "TOU-Residential", // tariffProfile
                "15",     // readCycle (15th of month)
                new TariffRiderRefsDto(List.of(
                        new TariffRiderRefDto("Green-Energy-Rider", "ENROLLED", 1328000000L),
                        new TariffRiderRefDto("Demand-Response-Credit", "ENROLLED", 1328000000L))),
                new BillingChargeSourceDto("MEASURED") // billingChargeSource
        );

        List<LinkDto> links = new ArrayList<>();
        links.add(new LinkDto("self", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/UsageSummary/01"));
        links.add(new LinkDto("up", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/UsageSummary"));

        return new AtomEntryDto("urn:uuid:3B1C9D4E-5F6A-5789-01BC-DEF234567890", "Monthly Usage Summary",
                now, now, links, usageSummaryDto);
    }

    private static @NonNull AtomEntryDto getElectricPowerQualitySummaryEntry(OffsetDateTime now) {
        // Canonical constructor: id, uuid, + 14 XSD fields, usagePointId = 17 parameters with comprehensive test data
        ElectricPowerQualitySummaryDto epqsDto = new ElectricPowerQualitySummaryDto(
                null,   // id
                null,   // uuid
                850L,   // flickerPlt (0.85 long-term flicker severity)
                720L,   // flickerPst (0.72 short-term flicker severity)
                320L,   // harmonicVoltage (3.20% THD)
                2L,     // longInterruptions (2 long interruptions)
                120000L, // mainsVoltage (120.000 V in millivolts)
                (short) 4, // measurementProtocol (IEC 61000-4-30)
                60000L, // powerFrequency (60.000 Hz in millihertz)
                5L,     // rapidVoltageChanges (5 rapid voltage changes)
                3L,     // shortInterruptions (3 short interruptions)
                new DateTimeIntervalDto(1330578000L, 86400L), // summaryInterval (1 day)
                8L,     // supplyVoltageDips (8 voltage dips)
                150L,   // supplyVoltageImbalance (1.50% imbalance)
                250L,   // supplyVoltageVariations (2.50% voltage variation)
                1L,     // tempOvervoltage (1 temporary overvoltage event)
                null    // usagePointId
        );

        List<LinkDto> links = new ArrayList<>();
        links.add(new LinkDto("self", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/ElectricPowerQualitySummary/01"));
        links.add(new LinkDto("up", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/ElectricPowerQualitySummary"));

        return new AtomEntryDto("urn:uuid:4C2D0E5F-6A7B-5890-12CD-EF3456789012", "Power Quality Summary",
                now, now, links, epqsDto);
    }
}