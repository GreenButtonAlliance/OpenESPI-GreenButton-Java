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
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


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
    void export_atom_feed_test() throws IOException {
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        AtomEntryDto usagePointEntryDto = getUsagePointEntry(now);

        AtomEntryDto meterReadingEntryDto = getMeeterReadingEntryDto(now);

        AtomEntryDto readingEntry = getReadingEntryDto(now);

        AtomEntryDto intervalBlockEntry = getIntervlBlockEntryDto(now);

        AtomFeedDto atomFeedDto = new AtomFeedDto("urn:uuid:15B0A4ED-CCF4-4521-A0A1-9FF650EC8A6B", "Green Button Subscription Feed",
                now, now, null, List.of(usagePointEntryDto, meterReadingEntryDto, readingEntry, intervalBlockEntry));

        try (OutputStream stream = new ByteArrayOutputStream()) {
            // Commented out due to conflict in IntervalReadingDto which cannot be fixed in this task
             dtoExportService.exportAtomFeed(atomFeedDto, stream);

             System.out.println(stream.toString());
        }
    }

    private static @NonNull AtomEntryDto getIntervlBlockEntryDto(OffsetDateTime now) {
        List<IntervalReadingDto> intervalReadings = new ArrayList<>();
        intervalReadings.add(new IntervalReadingDto( 974L, null, 282L, new DateTimeIntervalDto(1330578000L, 900L), new ArrayList<>(List.of(new ReadingQualityDto( "8"))), null, null, null));

        intervalReadings.add(new IntervalReadingDto( 965L, null, 323L, new DateTimeIntervalDto(1330578900L, 900L), new ArrayList<>(List.of(new ReadingQualityDto( "7"))), null, null, null));

        intervalReadings.add(new IntervalReadingDto(294L, 884L, null, new DateTimeIntervalDto(1330579800L, 900L)));
        intervalReadings.add(new IntervalReadingDto(331L, 995L, null, new DateTimeIntervalDto(1330580700L, 900L)));

        IntervalBlockDto intervalBlockDto = new IntervalBlockDto("urn:uuid:FE9A61BB-6913-42D4-88BE-9634A218EF53",
                new DateTimeIntervalDto(1330578000L, 86400L), intervalReadings);

        List<LinkDto> intervalBlockLinks = new ArrayList<>();
        intervalBlockLinks.add(new LinkDto("self", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/MeterReading/01/IntervalBlock/173"));
        intervalBlockLinks.add(new LinkDto("up", "/espi/1_1/resource/RetailCustomer/9B6C7066/UsagePoint/5446AF3F/MeterReading/01/IntervalBlock"));

        return new AtomEntryDto("urn:uuid:FE9A61BB-6913-42D4-88BE-9634A218EF53", "Interval Block", now, now,
                intervalBlockLinks, intervalBlockDto);
    }

    private static @NonNull AtomEntryDto getReadingEntryDto(OffsetDateTime now) {
        ReadingTypeDto readingTypeDto = new ReadingTypeDto(1L, "urn:uuid:3430B025-65D5-493A-BEC2-053603C91CD7",
                null, "4", "1", null, "840", "12", "NET", "TOTAL", 900L, "NET", "KILO", "DAILY", "V", "1", "CONTINUOUS", "1", null,
                null, null);

        // AtomContentDto readingTypeDtoContent = new AtomContentDto(readingTypeDto);
        List<LinkDto> readingTypeLinkList = new ArrayList<>();
        readingTypeLinkList.add(new LinkDto("self", "/espi/1_1/resource/ReadingType/07"));
        readingTypeLinkList.add(new LinkDto("up", "/espi/1_1/resource/ReadingType"));

        return new AtomEntryDto("urn:uuid:3430B025-65D5-493A-BEC2-053603C91CD7", "Type of Meter Reading Data", now, now,
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
        usagePointEntity.setId(UUID.fromString("48C2A019-5598-4E16-B0F9-49E4FF27F5FB"));
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

        return new AtomEntryDto("urn:uuid:48C2A019-5598-4E16-B0F9-49E4FF27F5FB", "Front Electric Meter",
                now,
                now,
                usagePointList,
                usagePointDto);
    }
}