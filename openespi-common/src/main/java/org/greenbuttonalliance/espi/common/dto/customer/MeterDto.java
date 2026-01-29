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

package org.greenbuttonalliance.espi.common.dto.customer;

import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Meter DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents a meter device extending EndDevice with meter-specific functionality.
 * Supports Atom protocol XML wrapping.
 */
@XmlRootElement(name = "Meter", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Meter", namespace = "http://naesb.org/espi/customer", propOrder = {
    "published", "updated", "selfLink", "upLink", "relatedLinks",
    "description", "amrSystem", "installCode", "isPan", "installDate",
    "removedDate", "serialNumber", "formNumber", "kh", "meterMultiplier",
    "serviceLocation"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeterDto {

    @XmlTransient
    private Long id;

    @XmlAttribute(name = "mRID")
    private String uuid;

    @XmlElement(name = "published")
    private OffsetDateTime published;

    @XmlElement(name = "updated")
    private OffsetDateTime updated;

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    @XmlElementWrapper(name = "links", namespace = "http://www.w3.org/2005/Atom")
    private List<LinkDto> relatedLinks;

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    private LinkDto selfLink;

    @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
    private LinkDto upLink;

    @XmlElement(name = "description")
    private String description;

    // EndDevice fields
    @XmlElement(name = "amrSystem")
    private String amrSystem;

    @XmlElement(name = "installCode")
    private String installCode;

    @XmlElement(name = "isPan")
    private Boolean isPan;

    @XmlElement(name = "installDate")
    private OffsetDateTime installDate;

    @XmlElement(name = "removedDate")
    private OffsetDateTime removedDate;

    @XmlElement(name = "serialNumber")
    private String serialNumber;

    // Meter-specific fields
    @XmlElement(name = "formNumber")
    private String formNumber;

    @XmlElement(name = "kh")
    private Double kh;

    @XmlElement(name = "meterMultiplier")
    private Double meterMultiplier;

    @XmlElement(name = "ServiceLocation")
    private ServiceLocationDto serviceLocation;

    /**
     * Minimal constructor for basic meter data.
     */
    public MeterDto(String uuid, String serialNumber, String formNumber) {
        this(null, uuid, null, null, null, null, null, null,
             null, null, null, null, null, serialNumber, formNumber, null, null, null);
    }

    /**
     * Gets the self href for this meter.
     *
     * @return self href string
     */
    public String getSelfHref() {
        return selfLink != null ? selfLink.getHref() : null;
    }

    /**
     * Gets the up href for this meter.
     *
     * @return up href string
     */
    public String getUpHref() {
        return upLink != null ? upLink.getHref() : null;
    }
}