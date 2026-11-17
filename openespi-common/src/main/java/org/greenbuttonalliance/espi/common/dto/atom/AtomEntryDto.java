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
     * Constructor for the Atom metadata; the subclass sets the ESPI resource ({@link #setContent}).
     */
    protected AtomEntryDto(String id, String title, OffsetDateTime published, OffsetDateTime updated,
                           List<LinkDto> links) {
        this.id = id;
        this.title = title;
        this.published = published;
        this.updated = updated;
        this.links = links;
    }

    /**
     * Constructor with the current time (to the second) as published and updated, and no links.
     */
    protected AtomEntryDto(String id, String title) {
        OffsetDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                .atOffset(ZoneOffset.UTC);
        this(id, title, now, now, null);
    }

    /**
     * The ESPI resource this entry carries ({@code espi:UsagePoint}, {@code cust:Customer}, ...). In the XML
     * it is the single child of the entry's {@code atom:content} element, as REQ.21 and certified feeds use
     * it (#197). Each subclass maps it for its own schema (usage or customer), so that only that schema's
     * namespace is declared.
     */
    public abstract Object getContent();

    public abstract void setContent(Object content);


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
     * Gets the ESPI resource carried by the entry: the same as {@link #getContent()}. It used to return a
     * base-class field that unmarshalling never set, so it was always null for a parsed entry (#197).
     *
     * @return the resource, or null if the entry has none
     */
    public Object getResource() {
        return getContent();
    }
}