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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.xml.bind.annotation.*;
import org.greenbuttonalliance.espi.common.dto.customer.*;
import org.greenbuttonalliance.espi.common.dto.usage.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Atom Entry DTO record for individual entries in Atom feeds.
 * <p>
 * Represents an individual entry within an Atom feed containing Green Button data.
 * Used to wrap individual resources (usage points, customers, etc.) in Atom format.
 */
@XmlRootElement(name = "entry", namespace = "http://www.w3.org/2005/Atom")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AtomEntry", namespace = "http://www.w3.org/2005/Atom", propOrder = {
    "id", "title", "published", "updated", "links", "content"
})
public record AtomEntryDto(

    @XmlElement(name = "id", namespace = "http://www.w3.org/2005/Atom")
    String id,

    @XmlElement(name = "title", namespace = "http://www.w3.org/2005/Atom")
    String title,

    @XmlElement(name = "published", namespace = "http://www.w3.org/2005/Atom")
    OffsetDateTime published,

    @XmlElement(name = "updated", namespace = "http://www.w3.org/2005/Atom")
    OffsetDateTime updated,

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    List<LinkDto> links,

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT,
            property = "type")
    @JsonSubTypes({
            // ESPI Usage resources (espi: namespace - top-level IdentifiedObject-based)
            @JsonSubTypes.Type(value = ApplicationInformationDto.class, name = "espi:ApplicationInformation"),
            @JsonSubTypes.Type(value = AuthorizationDto.class, name = "espi:Authorization"),
            @JsonSubTypes.Type(value = ElectricPowerQualitySummaryDto.class, name = "espi:ElectricPowerQualitySummary"),
            @JsonSubTypes.Type(value = IntervalBlockDto.class, name = "espi:IntervalBlock"),
            @JsonSubTypes.Type(value = MeterReadingDto.class, name = "espi:MeterReading"),
            @JsonSubTypes.Type(value = ReadingTypeDto.class, name = "espi:ReadingType"),
            @JsonSubTypes.Type(value = TimeConfigurationDto.class, name = "espi:TimeConfiguration"),
            @JsonSubTypes.Type(value = UsagePointDto.class, name = "espi:UsagePoint"),
            @JsonSubTypes.Type(value = UsageSummaryDto.class, name = "espi:UsageSummary"),
            // ESPI Customer resources (cust: namespace - top-level IdentifiedObject-based)
            @JsonSubTypes.Type(value = CustomerDto.class, name = "cust:Customer"),
            @JsonSubTypes.Type(value = CustomerAccountDto.class, name = "cust:CustomerAccount"),
            @JsonSubTypes.Type(value = CustomerAgreementDto.class, name = "cust:CustomerAgreement"),
            @JsonSubTypes.Type(value = EndDeviceDto.class, name = "cust:EndDevice"),
            @JsonSubTypes.Type(value = MeterDto.class, name = "cust:Meter"),
            @JsonSubTypes.Type(value = ProgramDateIdMappingsDto.class, name = "cust:ProgramDateIdMappings"),
            // Note: TimeConfigurationDto supports BOTH espi: and cust: namespaces (same type in both schemas)
            @JsonSubTypes.Type(value = TimeConfigurationDto.class, name = "cust:TimeConfiguration"),
            @JsonSubTypes.Type(value = ServiceLocationDto.class, name = "cust:ServiceLocation"),
            @JsonSubTypes.Type(value = StatementDto.class, name = "cust:Statement")
            // TODO: Add when ServiceSupplierDto is implemented:
            // @JsonSubTypes.Type(value = ServiceSupplierDto.class, name = "cust:ServiceSupplier")
    })
    @XmlAnyElement(lax = true)
    @XmlElement(name = "content", namespace = "http://www.w3.org/2005/Atom")
    Object content,

    @XmlAttribute(name = "xmlns:espi")
    String espiNamespace,

    @XmlAttribute(name = "xmlns:cust")
    String custNamespace
) {

    /**
     * Compact constructor that auto-computes namespace attributes based on content type.
     */
    public AtomEntryDto {
        // Auto-compute namespaces if not provided
        if (content != null && espiNamespace == null && custNamespace == null) {
            String packageName = content.getClass().getPackageName();
            if (packageName.contains("dto.usage")) {
                espiNamespace = "http://naesb.org/espi";
            } else if (packageName.contains("dto.customer")) {
                custNamespace = "http://naesb.org/espi/customer";
            }
        }
    }

    /**
     * Constructor for basic entry data without namespace (auto-computed).
     */
    public AtomEntryDto(String id, String title, OffsetDateTime published,
                       OffsetDateTime updated, List<LinkDto> links, Object content) {
        this(id, title, published, updated, links, content, null, null);
    }

    /**
     * Constructor for basic entry data with auto-generated timestamps.
     */
    public AtomEntryDto(String id, String title, Object resource) {
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

        this(id, title, now, now, null,
             new AtomContentDto("application/xml", resource), null, null);
    }
    
    /**
     * Gets the self link from the entry links.
     * 
     * @return self link or null if not found
     */
    public LinkDto getSelfLink() {
        return links != null ? links.stream()
            .filter(link -> "self".equals(link.rel()))
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
            .filter(link -> "up".equals(link.rel()))
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