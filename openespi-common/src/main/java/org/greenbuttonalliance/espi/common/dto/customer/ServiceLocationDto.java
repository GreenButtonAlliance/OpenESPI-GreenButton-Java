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
 * ServiceLocation DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents a physical location where utility services are delivered.
 * Supports Atom protocol XML wrapping.
 */
@XmlRootElement(name = "ServiceLocation", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceLocation", namespace = "http://naesb.org/espi/customer", propOrder = {
    "published", "updated", "selfLink", "upLink", "relatedLinks",
    "description", "accessMethod", "needsInspection", "siteAccessProblem",
    "positionAddress", "geoInfoReference", "direction", "customerAgreement"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLocationDto {

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

    @XmlElement(name = "accessMethod")
    private String accessMethod;

    @XmlElement(name = "needsInspection")
    private Boolean needsInspection;

    @XmlElement(name = "siteAccessProblem")
    private String siteAccessProblem;

    @XmlElement(name = "positionAddress")
    private String positionAddress;

    @XmlElement(name = "geoInfoReference")
    private String geoInfoReference;

    @XmlElement(name = "direction")
    private String direction;

    @XmlElement(name = "CustomerAgreement")
    private CustomerAgreementDto customerAgreement;

    /**
     * Minimal constructor for basic location data.
     */
    public ServiceLocationDto(String uuid, String positionAddress) {
        this(null, uuid, null, null, null, null, null, null,
             null, null, null, positionAddress, null, null, null);
    }

    /**
     * Gets the self href for this service location.
     *
     * @return self href string
     */
    public String getSelfHref() {
        return selfLink != null ? selfLink.getHref() : null;
    }

    /**
     * Gets the up href for this service location.
     *
     * @return up href string
     */
    public String getUpHref() {
        return upLink != null ? upLink.getHref() : null;
    }

    /**
     * Generates the default self href for a service location.
     *
     * @return default self href
     */
    public String generateSelfHref() {
        if (uuid != null && customerAgreement != null && customerAgreement.getUuid() != null) {
            return "/espi/1_1/resource/CustomerAgreement/" + customerAgreement.getUuid() + "/ServiceLocation/" + uuid;
        }
        return uuid != null ? "/espi/1_1/resource/ServiceLocation/" + uuid : null;
    }

    /**
     * Generates the default up href for a service location.
     *
     * @return default up href
     */
    public String generateUpHref() {
        if (customerAgreement != null && customerAgreement.getUuid() != null) {
            return "/espi/1_1/resource/CustomerAgreement/" + customerAgreement.getUuid() + "/ServiceLocation";
        }
        return "/espi/1_1/resource/ServiceLocation";
    }
}