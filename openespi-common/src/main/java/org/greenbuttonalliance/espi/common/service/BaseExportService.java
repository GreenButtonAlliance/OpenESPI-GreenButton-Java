/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.common.service;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.common.LinkType;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Abstract base class for domain-specific XML export services.
 * <p>
 * Provides common JAXB marshaller configuration for ESPI XML output with proper
 * namespace handling. Subclasses implement domain-specific JAXBContext initialization
 * to ensure only required namespaces are declared (2 namespace limit for JAXB 3.x).
 * <p>
 * Design rationale:
 * - JAXB 3.x cannot reliably predict default namespace with 3+ schemas
 * - Each subclass registers exactly 2 namespaces: Atom + domain (espi OR cust)
 * - This allows Atom to be the default namespace consistently
 *
 * @see UsageExportService for usage domain (Atom + espi namespaces)
 * @see CustomerExportService for customer domain (Atom + cust namespaces)
 */
@Slf4j
public abstract class BaseExportService {

    /**
     * XML declaration and stylesheet processing instruction for ESPI feeds.
     */
    protected static final String XML_HEADER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet type="text/xsl" href="GreenButtonDataStyleSheet.xslt"?>
            """;

    /**
     * Cached JAXBContext for this domain.
     * Initialized once at service startup via @PostConstruct.
     */
    private JAXBContext jaxbContext;

    /**
     * Gets the domain-specific JAXBContext.
     * <p>
     * Subclasses must register exactly 2 namespaces:
     * 1. Atom namespace (http://www.w3.org/2005/Atom)
     * 2. Domain namespace (espi OR cust)
     * <p>
     * Example for usage domain:
     * <pre>
     * return JAXBContext.newInstance(
     *     // Atom classes
     *     UsageAtomEntryDto.class,
     *     LinkDto.class,
     *     // Usage domain classes ONLY
     *     UsagePointDto.class,
     *     MeterReadingDto.class,
     *     // ... (NO customer domain classes)
     * );
     * </pre>
     *
     * @return JAXBContext configured for this domain
     * @throws JAXBException if context creation fails
     */
    protected abstract JAXBContext createJAXBContext() throws JAXBException;

    /**
     * Returns the set of namespaces for this domain.
     * <p>
     * Must return exactly 2 namespaces:
     * - Atom: http://www.w3.org/2005/Atom
     * - Domain: http://naesb.org/espi OR http://naesb.org/espi/customer
     *
     * @return set of 2 namespace URIs
     */
    protected abstract Set<String> getDomainNamespaces();

    /**
     * Initializes the JAXBContext at service startup.
     * This one-time initialization improves performance by avoiding repeated context creation.
     */
    @PostConstruct
    protected void initializeJAXBContext() {
        try {
            this.jaxbContext = createJAXBContext();
            log.info("Initialized JAXBContext for {} with namespaces: {}",
                this.getClass().getSimpleName(), getDomainNamespaces());
        } catch (JAXBException e) {
            log.error("Failed to initialize JAXBContext for {}: {}",
                this.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Failed to initialize export service", e);
        }
    }

    /**
     * Public initialization method for testing purposes.
     * Allows tests to manually initialize the service without Spring context.
     * In production, this is called automatically by @PostConstruct.
     */
    public void init() {
        initializeJAXBContext();
    }

    /**
     * Creates a configured JAXB Marshaller for this domain.
     * <p>
     * Configuration:
     * - Formatted output with proper indentation
     * - UTF-8 encoding
     * - Namespace prefixes defined in package-info.java files (atom:, espi:, cust:)
     * - Only domain namespaces are declared
     * <p>
     * Note: Namespace prefix control is achieved via @XmlSchema declarations in package-info.java
     * files rather than a custom NamespacePrefixMapper. This is simpler and more maintainable.
     *
     * @return configured Marshaller instance
     * @throws JAXBException if marshaller creation fails
     */
    protected Marshaller createMarshaller() throws JAXBException {
        if (jaxbContext == null) {
            throw new IllegalStateException("JAXBContext not initialized. Ensure @PostConstruct was called.");
        }

        Marshaller marshaller = jaxbContext.createMarshaller();

        // Standard JAXB formatting properties
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);

        return marshaller;
    }

    /**
     * Exports a DTO to XML format.
     *
     * @param dto the DTO to export (AtomEntryDto, AtomFeedDto, or ESPI resource DTO)
     * @param stream the output stream to write XML to
     * @throws RuntimeException if marshalling fails
     */
    public void exportDto(Object dto, OutputStream stream) {
        if (dto instanceof Collection<?>) {
            // A bare collection has no XML root element; JAXB fails with an obscure
            // "ImmutableCollections$ListN ... not known to this context" (#247).
            throw new IllegalArgumentException("exportDto cannot marshal a " + dto.getClass().getSimpleName()
                    + "; wrap collections in an Atom feed with exportFeed(...)");
        }
        try {
            Marshaller marshaller = createMarshaller();
            marshaller.marshal(dto, stream);
            log.debug("Successfully exported DTO of type: {}", dto.getClass().getSimpleName());
        } catch (JAXBException e) {
            log.error("Failed to marshal DTO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export DTO", e);
        }
    }

    /**
     * Exports a DTO to XML format with XML header.
     * <p>
     * Use this method for feed exports that require the XML declaration
     * and stylesheet processing instruction.
     *
     * @param dto the DTO to export
     * @param stream the output stream to write XML to
     * @throws RuntimeException if export fails
     */
    public void exportDtoWithHeader(Object dto, OutputStream stream) {
        try {
            // Write XML header
            stream.write(XML_HEADER.getBytes(StandardCharsets.UTF_8));

            // Marshal the DTO
            Marshaller marshaller = createMarshaller();
            marshaller.marshal(dto, stream);

            log.debug("Successfully exported DTO with header: {}", dto.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to export DTO with header: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export DTO with header", e);
        }
    }

    /**
     * Exports a collection of ESPI resources as an Atom {@code <feed>} (#247).
     *
     * @param feedHref   absolute URL of the feed (its self link, and the source of its UUID v5 id)
     * @param title      feed title
     * @param entities   the resources, in feed order; may be empty (an empty feed is still valid)
     * @param toContent  maps an entity to the ESPI DTO carried in its entry's {@code <content>}
     * @param stream     the output stream to write XML to
     * @param <E>        entity type
     * @see #buildFeed(String, String, List, Function)
     */
    public <E extends IdentifiedObject> void exportFeed(String feedHref, String title, List<E> entities,
                                                         Function<? super E, ?> toContent, OutputStream stream) {
        exportDto(buildFeed(feedHref, title, entities, toContent), stream);
    }

    /**
     * Builds an Atom feed whose entries carry the ESPI resources, per REQ.21:
     * <ul>
     *   <li>feed: {@code urn:uuid} id (UUID v5 of {@code feedHref}), title, published/updated (now, UTC),
     *       {@code self} link of type {@code espi-feed/<Resource>};</li>
     *   <li>entry: {@code urn:uuid:<entity id>}, title (the entity description, else the resource name),
     *       the entity's published/updated, {@code self} ({@code espi-entry/<Resource>}) and {@code up}
     *       ({@code espi-feed/<Resource>}) links, and the mapped DTO as {@code <content>}.</li>
     * </ul>
     * Stored self/up links are used when they are absolute URLs; otherwise they are derived from
     * {@code feedHref} ({@code <feedHref>/<id>} and {@code feedHref}).
     */
    public <E extends IdentifiedObject> AtomFeedDto buildFeed(String feedHref, String title, List<E> entities,
                                                              Function<? super E, ?> toContent) {
        Objects.requireNonNull(feedHref, "feedHref");
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

        List<AtomEntryDto> entries = new ArrayList<>(entities.size());
        String resource = null;
        for (E entity : entities) {
            Object content = toContent.apply(entity);
            String entryResource = resourceName(content);
            resource = resource == null ? entryResource : resource;

            String selfHref = href(entity.getSelfLink(), feedHref + "/" + entity.getId());
            String upHref = href(entity.getUpLink(), feedHref);
            List<LinkDto> links = List.of(
                    new LinkDto("self", selfHref, "espi-entry/" + entryResource),
                    new LinkDto("up", upHref, "espi-feed/" + entryResource));
            String entryTitle = entity.getDescription() != null && !entity.getDescription().isBlank()
                    ? entity.getDescription() : entryResource;
            entries.add(newEntry("urn:uuid:" + entity.getId(), entryTitle,
                    utc(entity.getPublished(), entity.getCreated(), now),
                    utc(entity.getUpdated(), entity.getCreated(), now),
                    links, content));
        }

        AtomFeedDto feed = new AtomFeedDto();
        feed.setId("urn:uuid:" + ID_GENERATOR.generateEspiId(feedHref));
        feed.setTitle(title);
        feed.setPublished(now);
        feed.setUpdated(now);
        feed.setLinks(List.of(new LinkDto("self", feedHref,
                resource != null ? "espi-feed/" + resource : null)));
        feed.setEntries(entries);
        return feed;
    }

    /**
     * Creates the Atom entry for one resource. Usage-domain exporters use {@link UsageAtomEntryDto};
     * customer-domain exporters override this to return {@code CustomerAtomEntryDto}, so the entry type
     * matches the one registered in their JAXBContext.
     */
    protected AtomEntryDto newEntry(String id, String title, OffsetDateTime published, OffsetDateTime updated,
                                    List<LinkDto> links, Object content) {
        return new UsageAtomEntryDto(id, title, published, updated, links, content);
    }

    private static final EspiIdGeneratorService ID_GENERATOR = new EspiIdGeneratorService();

    /** The ESPI resource name: the DTO's {@code @XmlRootElement} name, else its class name without "Dto". */
    static String resourceName(Object content) {
        XmlRootElement root = content.getClass().getAnnotation(XmlRootElement.class);
        if (root != null && !root.name().isBlank() && !"##default".equals(root.name())) {
            return root.name();
        }
        String simple = content.getClass().getSimpleName();
        return simple.endsWith("Dto") ? simple.substring(0, simple.length() - 3) : simple;
    }

    /**
     * A stored link is used only when it is an absolute http(s) URL. Default links generated by
     * {@link IdentifiedObject#ensureSelfLink()} are host- and context-less paths, and older rows may hold
     * {@code href="self"} (swapped arguments, fixed in #247), so those fall back to the derived href.
     */
    private static String href(LinkType link, String fallback) {
        String href = link != null ? link.getHref() : null;
        return href != null && (href.startsWith("https://") || href.startsWith("http://")) ? href : fallback;
    }

    private static OffsetDateTime utc(LocalDateTime value, LocalDateTime fallback, OffsetDateTime now) {
        LocalDateTime chosen = value != null ? value : fallback;
        return chosen != null ? chosen.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS) : now;
    }
}
