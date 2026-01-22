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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Facade implementation of DtoExportService that delegates to domain-specific export services.
 * <p>
 * This facade provides backwards compatibility with the existing DtoExportService interface
 * while leveraging the new domain-specific export services (UsageExportService, CustomerExportService)
 * for proper namespace isolation.
 * <p>
 * Domain detection logic:
 * - Inspects DTO package name to determine domain
 * - Usage domain: .dto.usage → delegates to UsageExportService
 * - Customer domain: .dto.customer → delegates to CustomerExportService
 * - AtomEntry subtypes: UsageAtomEntryDto vs CustomerAtomEntryDto
 * <p>
 * This is the primary bean for DtoExportService (marked with @Primary).
 * Existing controllers and services continue to use the DtoExportService interface
 * without code changes.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class DtoExportServiceFacade implements DtoExportService {

    private final UsageExportService usageExportService;
    private final CustomerExportService customerExportService;
    private final UsagePointRepository usagePointRepository;
    private final UsagePointMapper usagePointMapper;
    private final EspiIdGeneratorService espiIdGeneratorService;

    @Override
    public void exportUsagePointEntry(UUID usagePointId, OutputStream stream) {
        Optional<UsagePointEntity> entity = usagePointRepository.findById(usagePointId);
        if (entity.isPresent()) {
            exportUsagePointEntry(entity.get(), stream);
        } else {
            log.warn("Usage point not found: {}", usagePointId);
        }
    }

    @Override
    public void exportUsagePointsFeedByIds(List<UUID> usagePointIds, OutputStream stream) {
        List<UsagePointEntity> entities = new ArrayList<>();
        for (UUID id : usagePointIds) {
            usagePointRepository.findById(id).ifPresent(entities::add);
        }
        exportUsagePointsFeed(entities, stream);
    }

    @Override
    public void exportUsagePointEntry(UsagePointEntity usagePoint, OutputStream stream) {
        try {
            // Convert entity to DTO
            UsagePointDto dto = usagePointMapper.toDto(usagePoint);

            // Create Atom entry
            AtomEntryDto entry = createAtomEntry("Usage Point " + usagePoint.getId(), dto);

            // Export as XML using domain-specific service
            exportDto(entry, stream);

        } catch (Exception e) {
            log.error("Failed to export usage point entry: {}", e.getMessage(), e);
        }
    }

    @Override
    public void exportUsagePointsFeed(List<UsagePointEntity> usagePoints, OutputStream stream) {
        try {
            List<AtomEntryDto> entries = new ArrayList<>();

            // Convert each entity to DTO and create entry
            for (UsagePointEntity entity : usagePoints) {
                UsagePointDto dto = usagePointMapper.toDto(entity);
                AtomEntryDto entry = createAtomEntry("Usage Point " + entity.getId(), dto);
                entries.add(entry);
            }

            // Create feed
            AtomFeedDto feed = createAtomFeed("Usage Points", entries);

            // Export as XML using domain-specific service
            exportAtomFeed(feed, stream);

        } catch (Exception e) {
            log.error("Failed to export usage points feed: {}", e.getMessage(), e);
        }
    }

    /**
     * Exports any DTO as XML by delegating to the appropriate domain-specific service.
     * <p>
     * Domain detection:
     * 1. Check if DTO is AtomEntryDto subtype (UsageAtomEntryDto vs CustomerAtomEntryDto)
     * 2. If AtomFeedDto, inspect first entry to determine domain
     * 3. Otherwise, check DTO package name (.dto.usage vs .dto.customer)
     *
     * @param dto the DTO to export
     * @param stream output stream for XML
     */
    @Override
    public void exportDto(Object dto, OutputStream stream) {
        String domain = detectDomain(dto);

        switch (domain) {
            case "usage" -> usageExportService.exportDto(dto, stream);
            case "customer" -> customerExportService.exportDto(dto, stream);
            default -> {
                log.warn("Unable to determine domain for DTO: {}. Defaulting to usage domain.",
                    dto.getClass().getName());
                usageExportService.exportDto(dto, stream);
            }
        }
    }

    @Override
    public void exportAtomFeed(AtomFeedDto atomFeedDto, OutputStream stream) {
        String domain = detectDomain(atomFeedDto);

        switch (domain) {
            case "usage" -> usageExportService.exportDtoWithHeader(atomFeedDto, stream);
            case "customer" -> customerExportService.exportDtoWithHeader(atomFeedDto, stream);
            default -> {
                log.warn("Unable to determine domain for feed. Defaulting to usage domain.");
                usageExportService.exportDtoWithHeader(atomFeedDto, stream);
            }
        }
    }

    @Override
    public AtomFeedDto createAtomFeed(String title, List<AtomEntryDto> entries) {
        OffsetDateTime now = OffsetDateTime.now();

        return new AtomFeedDto(
            UUID.randomUUID().toString(),  // id
            title,                         // title
            now,                          // published
            now,                          // updated
            null,                         // links
            entries                       // entries
        );
    }

    @Override
    public AtomEntryDto createAtomEntry(String title, Object resource) {
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.now()
            .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        java.time.OffsetDateTime now = localDateTime.atOffset(java.time.ZoneOffset.UTC)
            .toZonedDateTime().toOffsetDateTime();

        // Generate a UUID5 using title and resource type as the base
        String resourceType = resource.getClass().getSimpleName();
        UUID uuid5 = espiIdGeneratorService.generateSubscriptionId(resourceType, title);
        String entryId = "urn:uuid:" + uuid5.toString();

        // Determine which AtomEntry subclass to use based on resource package
        // Per NAESB ESPI standard, usage and customer domains are mutually exclusive
        String packageName = resource.getClass().getPackage().getName();
        if (packageName.contains(".dto.customer")) {
            // Customer domain resource → CustomerAtomEntryDto (declares xmlns:cust only)
            return new CustomerAtomEntryDto(entryId, title, now, now, null, resource);
        } else {
            // Usage domain resource → UsageAtomEntryDto (declares xmlns:espi only)
            return new UsageAtomEntryDto(entryId, title, now, now, null, resource);
        }
    }

    /**
     * Detects the domain (usage vs customer) for a given DTO.
     * <p>
     * Detection strategy:
     * 1. If UsageAtomEntryDto → usage domain
     * 2. If CustomerAtomEntryDto → customer domain
     * 3. If AtomFeedDto, inspect first entry's content
     * 4. Otherwise, check DTO package name
     *
     * @param dto the DTO to inspect
     * @return "usage" or "customer"
     */
    private String detectDomain(Object dto) {
        // 1. Check AtomEntry subtypes
        if (dto instanceof UsageAtomEntryDto) {
            return "usage";
        }
        if (dto instanceof CustomerAtomEntryDto) {
            return "customer";
        }

        // 2. Check AtomFeedDto entries
        if (dto instanceof AtomFeedDto feed) {
            if (feed.getEntries() != null && !feed.getEntries().isEmpty()) {
                AtomEntryDto firstEntry = feed.getEntries().get(0);
                return detectDomain(firstEntry);
            }
        }

        // 3. Check AtomEntryDto content
        if (dto instanceof AtomEntryDto entry) {
            Object content = entry.getContent();
            if (content != null) {
                return detectDomain(content);
            }
        }

        // 4. Check package name
        String packageName = dto.getClass().getPackage().getName();
        if (packageName.contains(".dto.customer")) {
            return "customer";
        }
        if (packageName.contains(".dto.usage")) {
            return "usage";
        }

        // Default to usage domain
        return "usage";
    }
}
