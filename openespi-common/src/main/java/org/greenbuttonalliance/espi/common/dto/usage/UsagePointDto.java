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

import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * UsagePoint DTO record for JAXB XML marshalling/unmarshalling.
 *
 * Represents a logical point on a network at which consumption or production
 * is either physically measured (e.g., metered) or estimated (e.g., unmetered street lights).
 * Supports Atom protocol XML wrapping.
 */
@XmlRootElement(name = "UsagePoint", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UsagePoint", namespace = "http://naesb.org/espi", propOrder = {
    "description", "roleFlags", "serviceCategory", "status", "estimatedLoad",
    "nominalServiceVoltage", "ratedCurrent", "ratedPower", "serviceDeliveryPoint",
    "pnodeRefs", "aggregatedNodeRefs"
})
public record UsagePointDto(

    @XmlTransient
    String uuid,

    @XmlElement(name = "description")
    String description,

    @XmlElement(name = "roleFlags", type = String.class)
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    byte[] roleFlags,

    @XmlElement(name = "ServiceCategory")
    ServiceCategory serviceCategory,

    @XmlElement(name = "status")
    Short status,

    /**
     * Estimated load for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "estimatedLoad")
    SummaryMeasurementDto estimatedLoad,

    /**
     * Nominal service voltage for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "nominalServiceVoltage")
    SummaryMeasurementDto nominalServiceVoltage,

    /**
     * Rated current for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "ratedCurrent")
    SummaryMeasurementDto ratedCurrent,

    /**
     * Rated power for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "ratedPower")
    SummaryMeasurementDto ratedPower,

    @XmlElement(name = "ServiceDeliveryPoint")
    ServiceDeliveryPointDto serviceDeliveryPoint,

    /**
     * Array of pricing node references.
     */
    @XmlElement(name = "pnodeRefs")
    PnodeRefsDto pnodeRefs,

    /**
     * Array of aggregated node references.
     */
    @XmlElement(name = "aggregatedNodeRefs")
    AggregatedNodeRefsDto aggregatedNodeRefs,

    @XmlTransient
    Object meterReadings,  // List<MeterReadingDto> - temporarily Object for compilation

    @XmlTransient
    Object usageSummaries, // List<UsageSummaryDto> - temporarily Object for compilation

    @XmlTransient
    Object electricPowerQualitySummaries // List<ElectricPowerQualitySummaryDto> - temporarily Object for compilation

) {

    /**
     * Default constructor for JAXB.
     */
    public UsagePointDto() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Minimal constructor for basic usage point data.
     *
     * @param uuid the resource identifier
     * @param serviceCategory the service category
     */
    public UsagePointDto(String uuid, ServiceCategory serviceCategory) {
        this(uuid, null, null, serviceCategory, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor with core ESPI elements.
     *
     * @param uuid the resource identifier
     * @param description human-readable description
     * @param serviceCategory the service category
     * @param estimatedLoad estimated load measurement
     * @param nominalServiceVoltage nominal voltage measurement
     * @param ratedCurrent rated current measurement
     * @param ratedPower rated power measurement
     * @param serviceDeliveryPoint service delivery point details
     */
    public UsagePointDto(String uuid, String description, ServiceCategory serviceCategory,
                        SummaryMeasurementDto estimatedLoad, SummaryMeasurementDto nominalServiceVoltage,
                        SummaryMeasurementDto ratedCurrent, SummaryMeasurementDto ratedPower,
                        ServiceDeliveryPointDto serviceDeliveryPoint) {
        this(uuid, description, null, serviceCategory, null, estimatedLoad, nominalServiceVoltage,
             ratedCurrent, ratedPower, serviceDeliveryPoint, null, null, null, null, null);
    }

    /**
     * Override roleFlags getter to return cloned array for defensive copying.
     *
     * @return cloned byte array or null
     */
    @Override
    public byte[] roleFlags() {
        return roleFlags != null ? roleFlags.clone() : null;
    }

    // Utility methods (no @XmlTransient needed with FIELD access)

    /**
     * Generates the default self href for a usage point.
     *
     * @return default self href
     */
    public String generateSelfHref() {
        return uuid != null ? "/espi/1_1/resource/UsagePoint/" + uuid : null;
    }

    /**
     * Generates the default up href for a usage point.
     *
     * @return default up href
     */
    public String generateUpHref() {
        return "/espi/1_1/resource/UsagePoint";
    }

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
