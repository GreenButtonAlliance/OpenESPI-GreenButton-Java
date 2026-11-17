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
import lombok.NoArgsConstructor;
import org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto;
import org.greenbuttonalliance.espi.common.dto.usage.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Atom Entry DTO for Usage Domain resources (ESPI namespace).
 * <p>
 * Specialized entry for usage.xsd resources (UsagePoint, MeterReading, etc.)
 * that ensures only the espi namespace (http://naesb.org/espi) is declared.
 * <p>
 * The resource is the single child of the entry's {@code atom:content} element (#197):
 * <pre>{@code
 * <atom:entry>
 *     <atom:id>...</atom:id> ... <atom:link .../>
 *     <atom:content>
 *         <espi:UsagePoint>...</espi:UsagePoint>
 *     </atom:content>
 * </atom:entry>
 * }</pre>
 * <p>
 * ESPI profile of RFC 4287 (see {@code docs/ESPI_ATOM_PROFILE.md}): {@code atom:content} has <strong>no
 * {@code type} attribute</strong>, as in REQ.21's examples, certified feeds and the DMD validator. RFC 4287
 * 4.1.3.1 would treat it as text and require an XML media type for element content; do not add one. The
 * entry has no {@code atom:author} either (privacy: it would identify the utility).
 * <p>
 * Per NAESB ESPI standard, usage and customer domains are mutually exclusive.
 */
@XmlRootElement(name = "entry", namespace = "http://www.w3.org/2005/Atom")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UsageAtomEntry", namespace = "http://www.w3.org/2005/Atom", propOrder = {
    "atomContent"
})
@NoArgsConstructor
public class UsageAtomEntryDto extends AtomEntryDto {

    /** The {@code atom:content} element holding one usage.xsd resource. */
    @XmlElement(name = "content", namespace = "http://www.w3.org/2005/Atom")
    private Content atomContent;

    /**
     * All-args constructor.
     */
    public UsageAtomEntryDto(String id, String title, OffsetDateTime published, OffsetDateTime updated,
                            List<LinkDto> links, Object content) {
        super(id, title, published, updated, links);
        setContent(content);
    }

    /**
     * Convenience constructor for testing with auto-generated timestamps.
     *
     * @param id the entry identifier (urn:uuid:xxx format)
     * @param title the entry title
     * @param content the usage resource content
     */
    public UsageAtomEntryDto(String id, String title, Object content) {
        super(id, title);
        setContent(content);
    }

    @Override
    public Object getContent() {
        return atomContent == null ? null : atomContent.resource;
    }

    @Override
    public void setContent(Object content) {
        this.atomContent = content == null ? null : new Content(content);
    }

    /**
     * The {@code atom:content} element of a usage entry. Only usage.xsd types are listed, so JAXB declares
     * only the {@code espi} namespace (http://naesb.org/espi).
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "UsageAtomContent", namespace = "http://www.w3.org/2005/Atom")
    @NoArgsConstructor
    public static class Content {

        @XmlElements({
            @XmlElement(name = "UsagePoint", type = UsagePointDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "MeterReading", type = MeterReadingDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "IntervalBlock", type = IntervalBlockDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "ReadingType", type = ReadingTypeDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "ElectricPowerQualitySummary", type = ElectricPowerQualitySummaryDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "UsageSummary", type = UsageSummaryDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "TimeConfiguration", type = TimeConfigurationDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "ApplicationInformation", type = ApplicationInformationDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "Authorization", type = AuthorizationDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "Subscription", type = SubscriptionDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "BatchList", type = BatchListDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "LineItem", type = LineItemDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "ServiceDeliveryPoint", type = ServiceDeliveryPointDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "ReadingQuality", type = ReadingQualityDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "IntervalReading", type = IntervalReadingDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "DateTimeInterval", type = DateTimeIntervalDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "TariffRiderRef", type = TariffRiderRefDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "TariffRiderRefs", type = TariffRiderRefsDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "PnodeRef", type = PnodeRefDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "PnodeRefs", type = PnodeRefsDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "AggregatedNodeRef", type = AggregatedNodeRefDto.class, namespace = "http://naesb.org/espi"),
            @XmlElement(name = "AggregatedNodeRefs", type = AggregatedNodeRefsDto.class, namespace = "http://naesb.org/espi")
        })
        private Object resource;

        Content(Object resource) {
            this.resource = resource;
        }
    }
}
