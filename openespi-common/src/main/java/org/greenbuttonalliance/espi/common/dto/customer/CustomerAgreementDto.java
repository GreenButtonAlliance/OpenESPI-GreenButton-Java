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
 * CustomerAgreement DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents an agreement between a customer and service provider.
 * Supports Atom protocol XML wrapping.
 */
@XmlRootElement(name = "CustomerAgreement", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomerAgreement", namespace = "http://naesb.org/espi/customer", propOrder = {
    "published", "updated", "selfLink", "upLink", "relatedLinks",
    "description", "signDate", "validityInterval", "customerAccount",
    "serviceLocations", "statements"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAgreementDto {

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

    @XmlElement(name = "signDate")
    private OffsetDateTime signDate;

    @XmlElement(name = "validityInterval")
    private String validityInterval;

    @XmlElement(name = "CustomerAccount")
    private CustomerAccountDto customerAccount;

    @XmlElement(name = "ServiceLocation")
    @XmlElementWrapper(name = "ServiceLocations")
    private List<ServiceLocationDto> serviceLocations;

    @XmlElement(name = "Statement")
    @XmlElementWrapper(name = "Statements")
    private List<StatementDto> statements;

    /**
     * Minimal constructor for basic agreement data.
     */
    public CustomerAgreementDto(String uuid, OffsetDateTime signDate) {
        this(null, uuid, null, null, null, null, null, null,
             signDate, null, null, null, null);
    }

    /**
     * Gets the self href for this customer agreement.
     *
     * @return self href string
     */
    public String getSelfHref() {
        return selfLink != null ? selfLink.getHref() : null;
    }

    /**
     * Gets the up href for this customer agreement.
     *
     * @return up href string
     */
    public String getUpHref() {
        return upLink != null ? upLink.getHref() : null;
    }

    /**
     * Generates the default self href for a customer agreement.
     *
     * @return default self href
     */
    public String generateSelfHref() {
        if (uuid != null && customerAccount != null && customerAccount.getUuid() != null) {
            return "/espi/1_1/resource/CustomerAccount/" + customerAccount.getUuid() + "/CustomerAgreement/" + uuid;
        }
        return uuid != null ? "/espi/1_1/resource/CustomerAgreement/" + uuid : null;
    }

    /**
     * Generates the default up href for a customer agreement.
     *
     * @return default up href
     */
    public String generateUpHref() {
        if (customerAccount != null && customerAccount.getUuid() != null) {
            return "/espi/1_1/resource/CustomerAccount/" + customerAccount.getUuid() + "/CustomerAgreement";
        }
        return "/espi/1_1/resource/CustomerAgreement";
    }
}