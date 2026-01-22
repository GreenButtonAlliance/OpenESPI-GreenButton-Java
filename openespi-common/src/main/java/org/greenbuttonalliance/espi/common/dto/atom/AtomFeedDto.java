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

package org.greenbuttonalliance.espi.common.dto.atom;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Atom Feed DTO class for wrapping Green Button data in Atom protocol.
 * <p>
 * Represents an Atom feed containing Green Button energy or customer data entries.
 * Used for RESTful API responses following the Atom syndication format.
 * <p>
 * NOTE: @XmlSeeAlso was removed to prevent namespace pollution. Domain-specific
 * export services (UsageExportService, CustomerExportService) explicitly register
 * needed classes in JAXBContext to ensure only 2 namespaces are declared.
 */
@XmlRootElement(name = "feed", namespace = "http://www.w3.org/2005/Atom")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AtomFeed", namespace = "http://www.w3.org/2005/Atom", propOrder = {
    "id", "title", "published", "updated", "links", "entries"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtomFeedDto {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "published")
    private OffsetDateTime published;

    @XmlElement(name = "updated")
    private OffsetDateTime updated;

    @XmlElement(name = "link")
    private List<LinkDto> links;

    @XmlElement(name = "entry")
    private List<AtomEntryDto> entries;

    /**
     * Constructor for basic feed data.
     */
    public AtomFeedDto(String id, String title) {
        this(id, title, OffsetDateTime.now(), OffsetDateTime.now(), null, null);
    }

    /**
     * Gets the self link from the feed links.
     *
     * @return self link or null if not found
     */
    public LinkDto getSelfLink() {
        return links != null ? links.stream()
            .filter(link -> "self".equals(link.getRel()))
            .findFirst()
            .orElse(null) : null;
    }

    /**
     * Gets the up link from the feed links.
     *
     * @return up link or null if not found
     */
    public LinkDto getUpLink() {
        return links != null ? links.stream()
            .filter(link -> "up".equals(link.getRel()))
            .findFirst()
            .orElse(null) : null;
    }

    /**
     * Gets the total number of entries in the feed.
     *
     * @return entry count
     */
    public int getEntryCount() {
        return entries != null ? entries.size() : 0;
    }
}