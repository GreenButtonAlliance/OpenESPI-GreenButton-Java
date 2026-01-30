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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Meter DTO for ESPI 4.0 XSD compliance.
 * <p>
 * Represents a physical metering device extending EndDevice.
 * Contains all fields from Asset (12) + EndDevice (4) + Meter (3) = 19 fields total.
 * <p>
 * XSD Inheritance Chain: Object → IdentifiedObject → Asset → AssetContainer → EndDevice → Meter
 * <p>
 * Field sequence MUST match customer.xsd definition exactly.
 * ONLY contains XSD-defined fields - NO Atom metadata (handled by AtomEntryDto wrapper).
 */
@XmlRootElement(name = "Meter", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Meter", namespace = "http://naesb.org/espi/customer", propOrder = {
    // Asset fields (12) - from customer.xsd lines 650-709
    "type", "utcNumber", "serialNumber", "lotNumber", "purchasePrice", "critical",
    "electronicAddress", "lifecycle", "acceptanceTest", "initialCondition",
    "initialLossOfLife", "status",
    // EndDevice fields (4) - from customer.xsd lines 219-238
    "isVirtual", "isPan", "installCode", "amrSystem",
    // Meter fields (3) - from customer.xsd lines 250-264
    "formNumber", "meterMultipliers", "intervalLength"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeterDto {

    // ========================================
    // Asset Fields (12 fields from customer.xsd lines 650-709)
    // ========================================

    /**
     * Utility supplied name for the type of meter.
     * XSD: String256, minOccurs="0"
     */
    @XmlElement(name = "type", namespace = "http://naesb.org/espi/customer")
    private String type;

    /**
     * Uniquely identifies the meter within utility.
     * XSD: String256, minOccurs="0"
     */
    @XmlElement(name = "utcNumber", namespace = "http://naesb.org/espi/customer")
    private String utcNumber;

    /**
     * Serial number of the physical meter.
     * Used for UUID v5 generation in ESPI 4.0.
     * XSD: String256, minOccurs="0"
     */
    @XmlElement(name = "serialNumber", namespace = "http://naesb.org/espi/customer")
    private String serialNumber;

    /**
     * Lot number for the meter.
     * XSD: String256, minOccurs="0"
     */
    @XmlElement(name = "lotNumber", namespace = "http://naesb.org/espi/customer")
    private String lotNumber;

    /**
     * Purchase price of the meter in currency minor units (cents).
     * XSD: Int48, minOccurs="0"
     */
    @XmlElement(name = "purchasePrice", namespace = "http://naesb.org/espi/customer")
    private Long purchasePrice;

    /**
     * True if asset is considered critical for some reason (e.g., staffing, safety).
     * XSD: xs:boolean, minOccurs="0"
     */
    @XmlElement(name = "critical", namespace = "http://naesb.org/espi/customer")
    private Boolean critical;

    /**
     * Electronic address (email, URL, radio, etc.) for this asset.
     * XSD: ElectronicAddress (complex type), minOccurs="0"
     * Note: Singular, not a collection.
     */
    @XmlElement(name = "electronicAddress", namespace = "http://naesb.org/espi/customer")
    private ElectronicAddressDto electronicAddress;

    /**
     * Lifecycle dates for the asset.
     * XSD: LifecycleDate (complex type), minOccurs="0"
     */
    @XmlElement(name = "lifecycle", namespace = "http://naesb.org/espi/customer")
    private LifecycleDateDto lifecycle;

    /**
     * Acceptance test information.
     * XSD: AcceptanceTest (complex type), minOccurs="0"
     */
    @XmlElement(name = "acceptanceTest", namespace = "http://naesb.org/espi/customer")
    private AcceptanceTestDto acceptanceTest;

    /**
     * Condition of the asset when it was initially received.
     * XSD: String256, minOccurs="0"
     */
    @XmlElement(name = "initialCondition", namespace = "http://naesb.org/espi/customer")
    private String initialCondition;

    /**
     * Initial loss of life as a percentage.
     * XSD: PerCent (UInt16), minOccurs="0"
     */
    @XmlElement(name = "initialLossOfLife", namespace = "http://naesb.org/espi/customer")
    private Integer initialLossOfLife;

    /**
     * Status information for this asset.
     * XSD: Status (complex type), minOccurs="0"
     */
    @XmlElement(name = "status", namespace = "http://naesb.org/espi/customer")
    private StatusDto status;

    // ========================================
    // EndDevice Fields (4 fields from customer.xsd lines 219-238)
    // ========================================

    /**
     * If true, this is a virtual device (not a physical device).
     * XSD: xs:boolean, minOccurs="0"
     */
    @XmlElement(name = "isVirtual", namespace = "http://naesb.org/espi/customer")
    private Boolean isVirtual;

    /**
     * If true, this is a personal area network (PAN) device.
     * XSD: xs:boolean, minOccurs="0"
     */
    @XmlElement(name = "isPan", namespace = "http://naesb.org/espi/customer")
    private Boolean isPan;

    /**
     * Installation code for the device.
     * XSD: String256, minOccurs="0"
     */
    @XmlElement(name = "installCode", namespace = "http://naesb.org/espi/customer")
    private String installCode;

    /**
     * Automated meter reading (AMR) system identifier.
     * XSD: String256, minOccurs="0"
     */
    @XmlElement(name = "amrSystem", namespace = "http://naesb.org/espi/customer")
    private String amrSystem;

    // ========================================
    // Meter Fields (3 fields from customer.xsd lines 250-264)
    // ========================================

    /**
     * Utility meter form designation per ANSI C12.10 or other regional standards.
     * XSD: String256, minOccurs="0"
     */
    @XmlElement(name = "formNumber", namespace = "http://naesb.org/espi/customer")
    private String formNumber;

    /**
     * Collection of meter multipliers applied to the meter readings.
     * XSD: MeterMultiplier (complex type), minOccurs="0", maxOccurs="unbounded"
     */
    @XmlElement(name = "MeterMultipliers", namespace = "http://naesb.org/espi/customer")
    private List<MeterMultiplierDto> meterMultipliers;

    /**
     * Default interval length (in seconds) for interval readings.
     * XSD: UInt32, minOccurs="0"
     */
    @XmlElement(name = "intervalLength", namespace = "http://naesb.org/espi/customer")
    private Long intervalLength;
}
