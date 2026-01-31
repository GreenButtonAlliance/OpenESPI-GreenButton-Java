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

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * ServiceLocation DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents a physical location where utility services are delivered.
 * Per ESPI 4.0 customer.xsd: ServiceLocation extends WorkLocation extends Location.
 */
@XmlRootElement(name = "ServiceLocation", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceLocation", namespace = "http://naesb.org/espi/customer", propOrder = {
    // Location fields (customer.xsd lines 914-997)
    "type", "mainAddress", "secondaryAddress", "phone1", "phone2",
    "electronicAddress", "geoInfoReference", "direction", "status", "positionPoints",
    // ServiceLocation fields (customer.xsd lines 1074-1116)
    "accessMethod", "siteAccessProblem", "needsInspection", "usagePointHrefs", "outageBlock"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLocationDto implements Serializable {

    // Location fields (inherited from Location → WorkLocation → ServiceLocation)

    /**
     * Classification by utility's corporate standards and practices, relative to the location itself.
     */
    @XmlElement(name = "type", namespace = "http://naesb.org/espi/customer")
    private String type;

    /**
     * Main address of the location.
     */
    @XmlElement(name = "mainAddress", namespace = "http://naesb.org/espi/customer")
    private CustomerDto.StreetAddressDto mainAddress;

    /**
     * Secondary address of the location (e.g., PO Box with different ZIP code).
     */
    @XmlElement(name = "secondaryAddress", namespace = "http://naesb.org/espi/customer")
    private CustomerDto.StreetAddressDto secondaryAddress;

    /**
     * Primary phone number for this service location.
     */
    @XmlElement(name = "phone1", namespace = "http://naesb.org/espi/customer")
    private TelephoneNumberDto phone1;

    /**
     * Secondary phone number for this service location.
     */
    @XmlElement(name = "phone2", namespace = "http://naesb.org/espi/customer")
    private TelephoneNumberDto phone2;

    /**
     * Electronic address (email, web, etc.).
     */
    @XmlElement(name = "electronicAddress", namespace = "http://naesb.org/espi/customer")
    private ElectronicAddressDto electronicAddress;

    /**
     * Reference to geographical information source, often external to the utility.
     */
    @XmlElement(name = "geoInfoReference", namespace = "http://naesb.org/espi/customer")
    private String geoInfoReference;

    /**
     * Direction that allows field crews to quickly find a given asset.
     */
    @XmlElement(name = "direction", namespace = "http://naesb.org/espi/customer")
    private String direction;

    /**
     * Status of this location.
     */
    @XmlElement(name = "status", namespace = "http://naesb.org/espi/customer")
    private StatusDto status;

    /**
     * Sequence of position points describing this location.
     * Each point contains xPosition, yPosition, and optional zPosition coordinates.
     */
    @XmlElement(name = "positionPoints", namespace = "http://naesb.org/espi/customer")
    private List<PositionPointDto> positionPoints;

    // ServiceLocation specific fields

    /**
     * Method for the service person to access this service location.
     */
    @XmlElement(name = "accessMethod", namespace = "http://naesb.org/espi/customer")
    private String accessMethod;

    /**
     * Problems previously encountered when visiting or performing work on this location.
     */
    @XmlElement(name = "siteAccessProblem", namespace = "http://naesb.org/espi/customer")
    private String siteAccessProblem;

    /**
     * True if inspection is needed of facilities at this service location.
     */
    @XmlElement(name = "needsInspection", namespace = "http://naesb.org/espi/customer")
    private Boolean needsInspection;

    /**
     * Collection of UsagePoint resource href URLs (cross-stream reference).
     * Each string is the UsagePoint's atom:link[@rel='self']/@href value.
     * Per ESPI 4.0 customer.xsd lines 1106-1111.
     */
    @XmlElement(name = "UsagePoints", namespace = "http://naesb.org/espi/customer")
    private List<String> usagePointHrefs;

    /**
     * Outage Block Identifier (extension).
     */
    @XmlElement(name = "outageBlock", namespace = "http://naesb.org/espi/customer")
    private String outageBlock;

    /**
     * PositionPoint DTO nested class.
     * Per customer.xsd PositionPoint type (lines 1146-1180).
     * Spatial coordinates for a point in the coordinate system.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "PositionPoint", namespace = "http://naesb.org/espi/customer", propOrder = {
        "xPosition", "yPosition", "zPosition"
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionPointDto implements Serializable {
        /**
         * X axis position.
         */
        @XmlElement(name = "xPosition", namespace = "http://naesb.org/espi/customer")
        private String xPosition;

        /**
         * Y axis position.
         */
        @XmlElement(name = "yPosition", namespace = "http://naesb.org/espi/customer")
        private String yPosition;

        /**
         * Z axis position (if applicable).
         */
        @XmlElement(name = "zPosition", namespace = "http://naesb.org/espi/customer")
        private String zPosition;
    }
}