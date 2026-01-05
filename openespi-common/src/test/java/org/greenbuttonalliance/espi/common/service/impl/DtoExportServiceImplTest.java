package org.greenbuttonalliance.espi.common.service.impl;

import org.greenbuttonalliance.espi.common.domain.common.LinkType;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;
import org.greenbuttonalliance.espi.common.dto.usage.*;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapperImpl;
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
        ReflectionTestUtils.setField(usagePointMapper, "dateTimeMapper", new DateTimeMapperImpl());
        ReflectionTestUtils.setField(usagePointMapper, "serviceDeliveryPointMapper", new ServiceDeliveryPointMapperImpl());
        ReflectionTestUtils.setField(meterReadingMapper, "dateTimeMapper", new DateTimeMapperImpl());

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

        AtomFeedDto atomFeedDto = new AtomFeedDto("urn:uuid:15B0A4ED-CCF4-5521-A0A1-9FF650EC8A6B",
                "Green Button Subscription Feed",
                now, now, null,
                List.of(usagePointEntryDto, meterReadingEntryDto, readingEntry, intervalBlockEntry));

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

        // Assert - Entry structure
        assertThat(xml).contains("<entry>");
        assertThat(xml).contains("</entry>");

        // Assert - ESPI namespace in content
        assertThat(xml).contains("xmlns:espi=\"http://naesb.org/espi\"");
        assertThat(xml).contains("<content>");
        assertThat(xml).contains("</content>");

        // Assert - All 4 entries present
        assertThat(xml).contains("Front Electric Meter");  // UsagePoint title
        assertThat(xml).contains("Meter Reading");  // MeterReading title
        assertThat(xml).contains("Type of Meter Reading Data");  // ReadingType title
        assertThat(xml).contains("Interval Block");  // IntervalBlock title
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

        // Assert - Entry has required Atom elements
        assertThat(xml).contains("<entry>");
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

        // Assert - ESPI namespace
        assertThat(xml).contains("xmlns:espi=\"http://naesb.org/espi\"");

        // Assert - UsagePoint element (with namespace declaration)
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

        // Assert - IntervalBlock element
        assertThat(xml).contains("<IntervalBlock>");
        assertThat(xml).contains("</IntervalBlock>");

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
        intervalReadings.add(new IntervalReadingDto( 974L, null, 282L, new DateTimeIntervalDto(1330578000L, 900L), new ArrayList<>(List.of(new ReadingQualityDto( "8"))), null, null, null));

        intervalReadings.add(new IntervalReadingDto( 965L, null, 323L, new DateTimeIntervalDto(1330578900L, 900L), new ArrayList<>(List.of(new ReadingQualityDto( "7"))), null, null, null));

        intervalReadings.add(new IntervalReadingDto(294L, 884L, null, new DateTimeIntervalDto(1330579800L, 900L)));
        intervalReadings.add(new IntervalReadingDto(331L, 995L, null, new DateTimeIntervalDto(1330580700L, 900L)));

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
}