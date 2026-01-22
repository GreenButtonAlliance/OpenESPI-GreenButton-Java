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
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.utils.OffsetDateTimeAdapter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Abstract base class for Atom Entry DTOs in Atom feeds.
 * <p>
 * Represents an individual entry within an Atom feed containing Green Button data.
 * Subclasses specialize for usage domain (espi) or customer domain (cust) to ensure
 * proper namespace declarations per NAESB ESPI standard.
 *
 * @see UsageAtomEntryDto for usage domain resources (espi namespace)
 * @see CustomerAtomEntryDto for customer domain resources (cust namespace)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AtomEntry", namespace = "http://www.w3.org/2005/Atom", propOrder = {
    "id", "title", "published", "updated", "links"
})
@Getter
@Setter
@NoArgsConstructor
public abstract class AtomEntryDto {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "published")
    @XmlJavaTypeAdapter(OffsetDateTimeAdapter.class)
    private OffsetDateTime published;

    @XmlElement(name = "updated")
    @XmlJavaTypeAdapter(OffsetDateTimeAdapter.class)
    private OffsetDateTime updated;

    @XmlElement(name = "link")
    private List<LinkDto> links;

    /**
     * ESPI resource content - subclasses define specific @XmlElements
     * for their domain (usage or customer) to ensure proper namespace declarations.
     */
    @XmlTransient
    protected Object content;

    /**
     * All-args constructor.
     */
    public AtomEntryDto(String id, String title, OffsetDateTime published, OffsetDateTime updated,
                       List<LinkDto> links, Object content) {
        this.id = id;
        this.title = title;
        this.published = published;
        this.updated = updated;
        this.links = links;
        this.content = content;
    }

    /**
     * Convenience constructor for testing with auto-generated timestamps.
     * Creates an AtomEntryDto with current timestamp and no links.
     *
     * @param id the entry identifier (urn:uuid:xxx format)
     * @param title the entry title
     * @param content the resource content
     */
    public AtomEntryDto(String id, String title, Object content) {
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();
        this(id, title, now, now, null, content);
    }


    /**
     * Gets the self link from the entry links.
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
     * Gets the up link from the entry links.
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
     * Gets the resource content from the entry.
     *
     * @return resource content or null if not available
     */
    public Object getResource() {
        return content; // != null ? content.resource() : null;
    }
}