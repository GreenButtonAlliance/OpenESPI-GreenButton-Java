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

package org.greenbuttonalliance.espi.common.dto.usage;

import org.greenbuttonalliance.espi.common.domain.common.AmiBillingReadyKind;
import org.greenbuttonalliance.espi.common.domain.common.PhaseCodeKind;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.common.UsagePointConnectedKind;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UsagePoint DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents a logical point on a network at which consumption or production
 * is either physically measured (e.g., metered) or estimated (e.g., unmetered street lights).
 * Supports Atom protocol XML wrapping.
 */
@XmlRootElement(name = "UsagePoint", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UsagePoint", namespace = "http://naesb.org/espi", propOrder = {
    "roleFlags", "serviceCategory", "status", "serviceDeliveryPoint",
    "amiBillingReady", "checkBilling", "connectionState", "estimatedLoad",
    "grounded", "isSdp", "isVirtual", "minimalUsageExpected",
    "nominalServiceVoltage", "outageRegion", "phaseCode", "ratedCurrent",
    "ratedPower", "readCycle", "readRoute", "serviceDeliveryRemark",
    "servicePriority", "pnodeRefs", "aggregatedNodeRefs"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsagePointDto {

    @XmlElement(name = "roleFlags", type = String.class)
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    private byte[] roleFlags;

    @XmlElement(name = "ServiceCategory")
    private ServiceCategory serviceCategory;

    @XmlElement(name = "status")
    private Short status;

    @XmlElement(name = "ServiceDeliveryPoint")
    private ServiceDeliveryPointDto serviceDeliveryPoint;

    /**
     * Lifecycle states of the metering installation with respect to readiness for billing via AMI reads.
     * Per ESPI 4.0 XSD: [extension] AmiBillingReadyKind enum.
     */
    @XmlElement(name = "amiBillingReady")
    private AmiBillingReadyKind amiBillingReady;

    /**
     * True if there is a reason to suspect that a previous billing may have been performed with erroneous data.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     */
    @XmlElement(name = "checkBilling")
    private Boolean checkBilling;

    /**
     * State of the usage point with respect to connection to the network.
     * Per ESPI 4.0 XSD: [extension] UsagePointConnectedKind enum.
     */
    @XmlElement(name = "connectionState")
    private UsagePointConnectedKind connectionState;

    /**
     * Estimated load for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "estimatedLoad")
    private SummaryMeasurementDto estimatedLoad;

    /**
     * True if grounded.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     */
    @XmlElement(name = "grounded")
    private Boolean grounded;

    /**
     * True if this usage point is a service delivery point.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     */
    @XmlElement(name = "isSdp")
    private Boolean isSdp;

    /**
     * True if this usage point is virtual (no physical location exists).
     * Per ESPI 4.0 XSD: [extension] boolean field.
     */
    @XmlElement(name = "isVirtual")
    private Boolean isVirtual;

    /**
     * True if minimal or zero usage is expected at this usage point.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     */
    @XmlElement(name = "minimalUsageExpected")
    private Boolean minimalUsageExpected;

    /**
     * Nominal service voltage for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "nominalServiceVoltage")
    private SummaryMeasurementDto nominalServiceVoltage;

    /**
     * Outage region in which this usage point is located.
     * Per ESPI 4.0 XSD: [extension] String256 field.
     */
    @XmlElement(name = "outageRegion")
    private String outageRegion;

    /**
     * Phase code indicating number of wires and specific nominal phases.
     * Per ESPI 4.0 XSD: [extension] PhaseCodeKind enum.
     */
    @XmlElement(name = "phaseCode")
    private PhaseCodeKind phaseCode;

    /**
     * Rated current for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "ratedCurrent")
    private SummaryMeasurementDto ratedCurrent;

    /**
     * Rated power for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "ratedPower")
    private SummaryMeasurementDto ratedPower;

    /**
     * Cycle day on which the meter will normally be read.
     * Per ESPI 4.0 XSD: [extension] String256 field.
     */
    @XmlElement(name = "readCycle")
    private String readCycle;

    /**
     * Route identifier for meter reading purposes.
     * Per ESPI 4.0 XSD: [extension] String256 field.
     */
    @XmlElement(name = "readRoute")
    private String readRoute;

    /**
     * Remarks about this usage point.
     * Per ESPI 4.0 XSD: [extension] String256 field.
     */
    @XmlElement(name = "serviceDeliveryRemark")
    private String serviceDeliveryRemark;

    /**
     * Priority of service for this usage point.
     * Per ESPI 4.0 XSD: [extension] String32 field.
     */
    @XmlElement(name = "servicePriority")
    private String servicePriority;

    /**
     * Array of pricing node references.
     */
    @XmlElement(name = "pnodeRefs")
    private PnodeRefsDto pnodeRefs;

    /**
     * Array of aggregated node references.
     */
    @XmlElement(name = "aggregatedNodeRefs")
    private AggregatedNodeRefsDto aggregatedNodeRefs;

    @XmlTransient
    private Object meterReadings;  // List<MeterReadingDto> - temporarily Object for compilation

    @XmlTransient
    private Object usageSummaries; // List<UsageSummaryDto> - temporarily Object for compilation

    @XmlTransient
    private Object electricPowerQualitySummaries; // List<ElectricPowerQualitySummaryDto> - temporarily Object for compilation

    /**
     * Override getRoleFlags to return cloned array for defensive copying.
     * Lombok @Getter will be overridden by this explicit method.
     *
     * @return cloned byte array or null
     */
    public byte[] getRoleFlags() {
        return roleFlags != null ? roleFlags.clone() : null;
    }

    // Utility methods (no @XmlTransient needed with FIELD access)

    /**
     * Gets the total number of meter readings.
     *
     * @return meter reading count
     */
    public int getMeterReadingCount() {
        return 0; // Temporarily disabled for compilation
    }

    /**
     * Gets the total number of usage summaries.
     *
     * @return usage summary count
     */
    public int getUsageSummaryCount() {
        return 0; // Temporarily disabled for compilation
    }
}
