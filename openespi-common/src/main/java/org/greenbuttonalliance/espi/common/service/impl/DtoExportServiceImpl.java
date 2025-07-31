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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Modern DTO-based export service implementation using JAXB marshalling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DtoExportServiceImpl implements DtoExportService {

    private final UsagePointRepository usagePointRepository;
    private final UsagePointMapper usagePointMapper;

    @Override
    public void exportUsagePointEntry(UUID usagePointId, OutputStream stream) {
        Optional<UsagePointEntity> entity = usagePointRepository.findById(usagePointId);
        if (entity.isPresent()) {
            exportUsagePointEntry(entity.get(), stream);
        } else {
            log.warn("Usage point not found: " + usagePointId);
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
            
            // Export as XML
            exportDto(entry, stream);
            
        } catch (Exception e) {
            log.error("Failed to export usage point entry: " + e.getMessage(), e);
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
            
            // Export as XML
            exportDto(feed, stream);
            
        } catch (Exception e) {
            log.error("Failed to export usage points feed: " + e.getMessage(), e);
        }
    }

    @Override
    public void exportDto(Object dto, OutputStream stream) {
        try {
            // Create JAXB context for DTO classes
            JAXBContext context = JAXBContext.newInstance(
                "org.greenbuttonalliance.espi.common.dto.atom:" +
                "org.greenbuttonalliance.espi.common.dto.usage:" +
                "org.greenbuttonalliance.espi.common.dto.customer"
            );
            
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            
            // Marshal DTO to stream
            marshaller.marshal(dto, stream);
            
            log.info("Successfully exported DTO of type: " + dto.getClass().getSimpleName());
            
        } catch (JAXBException e) {
            log.error("Failed to export DTO: " + e.getMessage(), e);
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
        return new AtomEntryDto(
            UUID.randomUUID().toString(),  // id
            title,                         // title
            resource                      // resource (uses convenience constructor)
        );
    }
}