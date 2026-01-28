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
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * EndDevice DTO for ESPI 4.0 customer.xsd schema compliance.
 * Per customer.xsd lines 210-242.
 *
 * EndDevice is an ESPI Resource that inherits from IdentifiedObject.
 * Asset fields are embedded inline (Asset inherits from Object in ESPI standard).
 *
 * Asset that performs one or more end device functions. One type of end device is a meter
 * which can perform metering, load management, connect/disconnect, accounting functions, etc.
 * Some end devices, such as ones monitoring and controlling air conditioners, refrigerators, pool pumps
 * may be connected to a meter. All end devices may have communication capability defined by the associated
 * communication function(s). An end device may be owned by a consumer, a service provider, utility or otherwise.
 *
 * This DTO contains ONLY the 16 XSD elements from customer.xsd:
 * - 12 Asset elements: type, utcNumber, serialNumber, lotNumber, purchasePrice, critical,
 *   electronicAddress (8 fields), lifecycle (2 fields), acceptanceTest (4 fields),
 *   initialCondition, initialLossOfLife, status (4 fields)
 * - 4 EndDevice elements: isVirtual, isPan, installCode, amrSystem
 *
 * Complex types with nested fields:
 * - electronicAddress: ElectronicAddressDto (8 fields: lan, mac, email1, email2, web, radio, userID, password)
 * - lifecycle: LifecycleDateDto (2 fields: manufacturedDate, installationDate)
 * - acceptanceTest: AcceptanceTestDto (4 fields: dateTime, success, type, remark)
 * - status: StatusDto (4 fields: value, dateTime, remark, reason)
 *
 * Atom protocol fields (id, published, updated, links) are handled by CustomerAtomEntryDto wrapper.
 */
@XmlRootElement(name = "EndDevice", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EndDevice", namespace = "http://naesb.org/espi/customer", propOrder = {
    // Asset fields (12)
    "type", "utcNumber", "serialNumber", "lotNumber", "purchasePrice", "critical",
    "electronicAddress", "lifecycle", "acceptanceTest", "initialCondition",
    "initialLossOfLife", "status",
    // EndDevice fields (4)
    "isVirtual", "isPan", "installCode", "amrSystem"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndDeviceDto implements Serializable {

    @XmlTransient
    private Long id;

    @XmlAttribute(name = "mRID")
    private String uuid;

    // ==================== Asset fields (12) ====================

    /**
     * Utility-specific classification of Asset and its subtypes.
     */
    @XmlElement(name = "type", namespace = "http://naesb.org/espi/customer")
    private String type;

    /**
     * Uniquely tracked commodity (UTC) number.
     */
    @XmlElement(name = "utcNumber", namespace = "http://naesb.org/espi/customer")
    private String utcNumber;

    /**
     * Serial number of this asset.
     */
    @XmlElement(name = "serialNumber", namespace = "http://naesb.org/espi/customer")
    private String serialNumber;

    /**
     * Lot number for this asset.
     */
    @XmlElement(name = "lotNumber", namespace = "http://naesb.org/espi/customer")
    private String lotNumber;

    /**
     * Purchase price of asset.
     */
    @XmlElement(name = "purchasePrice", namespace = "http://naesb.org/espi/customer")
    private Long purchasePrice;

    /**
     * True if asset is considered critical for some reason.
     */
    @XmlElement(name = "critical", namespace = "http://naesb.org/espi/customer")
    private Boolean critical;

    /**
     * Electronic address (complex type with 8 fields).
     */
    @XmlElement(name = "electronicAddress", namespace = "http://naesb.org/espi/customer")
    private ElectronicAddressDto electronicAddress;

    /**
     * Lifecycle dates for this asset (complex type with 2 fields).
     */
    @XmlElement(name = "lifecycle", namespace = "http://naesb.org/espi/customer")
    private LifecycleDateDto lifecycle;

    /**
     * Information on acceptance test (complex type with 4 fields).
     */
    @XmlElement(name = "acceptanceTest", namespace = "http://naesb.org/espi/customer")
    private AcceptanceTestDto acceptanceTest;

    /**
     * Condition of asset in inventory or at time of installation.
     */
    @XmlElement(name = "initialCondition", namespace = "http://naesb.org/espi/customer")
    private String initialCondition;

    /**
     * Percentage of expected life for the asset when it was new; zero for new devices.
     */
    @XmlElement(name = "initialLossOfLife", namespace = "http://naesb.org/espi/customer")
    private BigDecimal initialLossOfLife;

    /**
     * Status of this asset (complex type with 4 fields).
     */
    @XmlElement(name = "status", namespace = "http://naesb.org/espi/customer")
    private StatusDto status;

    // ==================== EndDevice fields (4) ====================

    /**
     * If true, there is no physical device (e.g., virtual meter for aggregation).
     */
    @XmlElement(name = "isVirtual", namespace = "http://naesb.org/espi/customer")
    private Boolean isVirtual;

    /**
     * If true, this is a premises area network (PAN) device.
     */
    @XmlElement(name = "isPan", namespace = "http://naesb.org/espi/customer")
    private Boolean isPan;

    /**
     * Installation code.
     */
    @XmlElement(name = "installCode", namespace = "http://naesb.org/espi/customer")
    private String installCode;

    /**
     * Automated meter reading (AMR) or other communication system.
     */
    @XmlElement(name = "amrSystem", namespace = "http://naesb.org/espi/customer")
    private String amrSystem;
}
