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

package org.greenbuttonalliance.espi.common.service;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

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
}
