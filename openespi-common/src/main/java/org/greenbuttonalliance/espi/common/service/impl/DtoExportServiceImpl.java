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

import com.fasterxml.jackson.annotation.JsonInclude;
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
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DatatypeFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.util.StdDateFormat;
import tools.jackson.dataformat.xml.XmlAnnotationIntrospector;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Modern DTO-based export service implementation using JAXB marshalling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DtoExportServiceImpl implements DtoExportService {

    private final UsagePointRepository usagePointRepository;
    private final UsagePointMapper usagePointMapper;

    private final String XML_HEADER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet type="text/xsl" href="GreenButtonDataStyleSheet.xslt"?>
            """;

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

        // Create JAXB context for DTO classes
        final XmlMapper xmlMapper = createXmlMapper();

        xmlMapper.writeValue(stream, dto);

        log.info("Successfully exported DTO of type: " + dto.getClass().getSimpleName());
    }

    @Override
    public void exportAtomFeed(AtomFeedDto atomFeedDto, OutputStream stream) {

        try {
            stream.write(XML_HEADER.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Create JAXB context for DTO classes
        final XmlMapper xmlMapper = createXmlMapper();

        xmlMapper.writeValue(stream, atomFeedDto);

        log.info("Successfully exported DTO of type: " + atomFeedDto.getClass().getSimpleName());
    }

    private XmlMapper createXmlMapper() {
        AnnotationIntrospector intr = XmlAnnotationIntrospector.Pair.instance
                (new JakartaXmlBindAnnotationIntrospector(),
                        new JacksonAnnotationIntrospector());

        // Create JAXB context for DTO classes
        //2012-10-24T00:00:00Z
        return XmlMapper.xmlBuilder()
               // .configure(XmlWriteFeature.WRITE_XML_DECLARATION, true)
                .annotationIntrospector(intr)
                .addModule(new JakartaXmlBindAnnotationModule().setNonNillableInclusion(JsonInclude.Include.NON_EMPTY))
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                //.enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                //.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMP)
                .disable(XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL)
                .defaultDateFormat(new StdDateFormat())
                .build();
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