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
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.springframework.stereotype.Service;

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
    private final org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService espiIdGeneratorService;

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
        try {
            // Determine required namespaces by inspecting the DTO content
            Set<String> requiredNamespaces = determineRequiredNamespaces(dto);

            Marshaller marshaller = createMarshaller(dto.getClass(), requiredNamespaces);
            marshaller.marshal(dto, stream);
            log.info("Successfully exported DTO of type: " + dto.getClass().getSimpleName());
        } catch (JAXBException e) {
            log.error("Failed to marshal DTO: " + e.getMessage(), e);
            throw new RuntimeException("Failed to export DTO", e);
        }
    }

    @Override
    public void exportAtomFeed(AtomFeedDto atomFeedDto, OutputStream stream) {
        try {
            // Write XML header manually
            stream.write(XML_HEADER.getBytes(StandardCharsets.UTF_8));

            // Determine required namespaces by inspecting the feed content
            Set<String> requiredNamespaces = determineRequiredNamespaces(atomFeedDto);

            // Marshal the feed
            Marshaller marshaller = createMarshaller(AtomFeedDto.class, requiredNamespaces);
            marshaller.marshal(atomFeedDto, stream);

            log.info("Successfully exported DTO of type: " + atomFeedDto.getClass().getSimpleName());
        } catch (IOException e) {
            log.error("Failed to write XML header: " + e.getMessage(), e);
            throw new RuntimeException("Failed to export Atom feed", e);
        } catch (JAXBException e) {
            log.error("Failed to marshal Atom feed: " + e.getMessage(), e);
            throw new RuntimeException("Failed to export Atom feed", e);
        }
    }

    /**
     * Determines required namespaces by inspecting DTO content.
     * Examines AtomEntryDto/AtomFeedDto content to identify whether espi or cust namespace is needed.
     *
     * @param dto the DTO to inspect
     * @return set of required namespace URIs (always includes Atom namespace)
     */
    private Set<String> determineRequiredNamespaces(Object dto) {
        Set<String> namespaces = new HashSet<>();

        // Always include Atom namespace
        namespaces.add("http://www.w3.org/2005/Atom");

        if (dto instanceof AtomEntryDto entry) {
            // Inspect the content to determine espi vs cust namespace
            Object content = entry.getContent();
            if (content != null) {
                addNamespaceForContent(content, namespaces);
            }
        } else if (dto instanceof AtomFeedDto feed) {
            // Check all entries in the feed
            if (feed.getEntries() != null) {
                for (AtomEntryDto entry : feed.getEntries()) {
                    Object content = entry.getContent();
                    if (content != null) {
                        addNamespaceForContent(content, namespaces);
                        // Per NAESB ESPI standard, all entries use same namespace
                        // so we can break after finding the first one
                        break;
                    }
                }
            }
        }

        return namespaces;
    }

    /**
     * Adds the appropriate namespace (espi or cust) based on content type.
     *
     * @param content the content object to examine
     * @param namespaces the set to add the namespace to
     */
    private void addNamespaceForContent(Object content, Set<String> namespaces) {
        String packageName = content.getClass().getPackage().getName();

        if (packageName.contains(".dto.usage")) {
            // Usage domain resources use espi namespace
            namespaces.add("http://naesb.org/espi");
        } else if (packageName.contains(".dto.customer")) {
            // Customer domain resources use cust namespace
            namespaces.add("http://naesb.org/espi/customer");
        }
    }

    /**
     * Creates a JAXB Marshaller configured for ESPI XML output.
     * Initializes JAXBContext with all ESPI resource DTO types to ensure
     * proper namespace declarations are generated when using @XmlElements.
     *
     * @param dtoClass the DTO class to marshal
     * @param requiredNamespaces set of namespace URIs that should be declared
     * @return configured Marshaller instance
     * @throws JAXBException if marshaller creation fails
     */
    private Marshaller createMarshaller(Class<?> dtoClass, Set<String> requiredNamespaces) throws JAXBException {
        // Initialize context with ALL ESPI DTO classes to ensure JAXB pre-declares all namespaces
        JAXBContext jaxbContext = JAXBContext.newInstance(
            // Atom protocol classes
            org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.LinkDto.class,

            // Usage domain classes (http://naesb.org/espi)
            org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AuthorizationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.SubscriptionDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.BatchListDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.LineItemDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ServiceDeliveryPointDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ReadingQualityDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.IntervalReadingDto.class,
            org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TariffRiderRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TariffRiderRefsDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.PnodeRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.PnodeRefsDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AggregatedNodeRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AggregatedNodeRefsDto.class,

            // Customer domain classes (http://naesb.org/espi/customer)
            org.greenbuttonalliance.espi.common.dto.customer.CustomerDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.CustomerAgreementDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.EndDeviceDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.MeterDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.ProgramDateIdMappingsDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.ServiceLocationDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.StatementDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.StatementRefDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.StatusDto.class,

            // Dynamic class parameter
            dtoClass
        );
        Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        // Set namespace prefix mapper with required namespaces
        // This ensures only the needed namespaces are declared on the root element
        org.greenbuttonalliance.espi.common.utils.EspiNamespacePrefixMapper prefixMapper =
            new org.greenbuttonalliance.espi.common.utils.EspiNamespacePrefixMapper(requiredNamespaces);

        // Use the modern Jakarta/Glassfish property key
        // This is specifically required for getContextualNamespaceDecls() to work
        try {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", prefixMapper);
        } catch (jakarta.xml.bind.PropertyException e) {
            // Fallback for older environments or different providers
            try {
                marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", prefixMapper);
            } catch (jakarta.xml.bind.PropertyException ex) {
                log.warn("Could not set namespace prefix mapper: " + ex.getMessage());
            }
        }

        // Set JAXB_FRAGMENT to false to enable proper root-level namespace declarations
        // getContextualNamespaceDecls() requires this for proper namespace handling
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);

        return marshaller;
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
        // Using the subscription pattern: combine title + resource class + timestamp for uniqueness
        String resourceType = resource.getClass().getSimpleName();
        UUID uuid5 = espiIdGeneratorService.generateSubscriptionId(resourceType, title);

        String entryId = "urn:uuid:" + uuid5.toString();

        // Determine which AtomEntry subclass to use based on resource package
        // Per NAESB ESPI standard, usage and customer domains are mutually exclusive
        String packageName = resource.getClass().getPackage().getName();
        if (packageName.contains(".dto.customer")) {
            // Customer domain resource -> CustomerAtomEntryDto (declares xmlns:cust only)
            return new CustomerAtomEntryDto(entryId, title, now, now, null, resource);
        } else {
            // Usage domain resource -> UsageAtomEntryDto (declares xmlns:espi only)
            return new UsageAtomEntryDto(entryId, title, now, now, null, resource);
        }
    }
}