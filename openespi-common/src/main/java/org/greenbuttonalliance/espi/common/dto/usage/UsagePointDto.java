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
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "UsagePoint", namespace = "http://naesb.org/espi", propOrder = {
    "description", "roleFlags", "serviceCategory", "status", "estimatedLoad", 
    "nominalServiceVoltage", "ratedCurrent", "ratedPower", "serviceDeliveryPoint",
    "pnodeRefs", "aggregatedNodeRefs"
})
public class UsagePointDto {
    
    private String uuid;
    private String description;
    private byte[] roleFlags;
    private ServiceCategory serviceCategory;
    private Short status;
    private SummaryMeasurementDto estimatedLoad;
    private SummaryMeasurementDto nominalServiceVoltage;
    private SummaryMeasurementDto ratedCurrent;
    private SummaryMeasurementDto ratedPower;
    private ServiceDeliveryPointDto serviceDeliveryPoint;
    private PnodeRefsDto pnodeRefs;
    private AggregatedNodeRefsDto aggregatedNodeRefs;
    private Object meterReadings;  // List<MeterReadingDto> - temporarily Object for compilation
    private Object usageSummaries; // List<UsageSummaryDto> - temporarily Object for compilation  
    private Object electricPowerQualitySummaries; // List<ElectricPowerQualitySummaryDto> - temporarily Object for compilation
    
    @XmlTransient
    public String getUuid() {
        return uuid;
    }
    
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }
    
    @XmlElement(name = "roleFlags", type = String.class)
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    public byte[] getRoleFlags() {
        return roleFlags;
    }
    
    @XmlElement(name = "ServiceCategory")
    public ServiceCategory getServiceCategory() {
        return serviceCategory;
    }
    
    @XmlElement(name = "status")
    public Short getStatus() {
        return status;
    }
    
    /**
     * Estimated load for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "estimatedLoad")
    public SummaryMeasurementDto getEstimatedLoad() {
        return estimatedLoad;
    }
    
    /**
     * Nominal service voltage for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "nominalServiceVoltage")
    public SummaryMeasurementDto getNominalServiceVoltage() {
        return nominalServiceVoltage;
    }
    
    /**
     * Rated current for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "ratedCurrent")
    public SummaryMeasurementDto getRatedCurrent() {
        return ratedCurrent;
    }
    
    /**
     * Rated power for the usage point as SummaryMeasurement.
     */
    @XmlElement(name = "ratedPower")
    public SummaryMeasurementDto getRatedPower() {
        return ratedPower;
    }
    
    @XmlElement(name = "ServiceDeliveryPoint")
    public ServiceDeliveryPointDto getServiceDeliveryPoint() {
        return serviceDeliveryPoint;
    }
    
    /**
     * Array of pricing node references.
     */
    @XmlElement(name = "pnodeRefs")
    public PnodeRefsDto getPnodeRefs() {
        return pnodeRefs;
    }
    
    /**
     * Array of aggregated node references.
     */
    @XmlElement(name = "aggregatedNodeRefs")
    public AggregatedNodeRefsDto getAggregatedNodeRefs() {
        return aggregatedNodeRefs;
    }
    
    @XmlTransient
    public Object getMeterReadings() {
        return meterReadings;
    }
    
    @XmlTransient
    public Object getUsageSummaries() {
        return usageSummaries;
    }
    
    @XmlTransient
    public Object getElectricPowerQualitySummaries() {
        return electricPowerQualitySummaries;
    }
    
    // Setters for JAXB unmarshalling
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setRoleFlags(byte[] roleFlags) {
        this.roleFlags = roleFlags;
    }
    
    public void setServiceCategory(ServiceCategory serviceCategory) {
        this.serviceCategory = serviceCategory;
    }
    
    public void setStatus(Short status) {
        this.status = status;
    }
    
    public void setEstimatedLoad(SummaryMeasurementDto estimatedLoad) {
        this.estimatedLoad = estimatedLoad;
    }
    
    public void setNominalServiceVoltage(SummaryMeasurementDto nominalServiceVoltage) {
        this.nominalServiceVoltage = nominalServiceVoltage;
    }
    
    public void setRatedCurrent(SummaryMeasurementDto ratedCurrent) {
        this.ratedCurrent = ratedCurrent;
    }
    
    public void setRatedPower(SummaryMeasurementDto ratedPower) {
        this.ratedPower = ratedPower;
    }
    
    public void setServiceDeliveryPoint(ServiceDeliveryPointDto serviceDeliveryPoint) {
        this.serviceDeliveryPoint = serviceDeliveryPoint;
    }
    
    public void setPnodeRefs(PnodeRefsDto pnodeRefs) {
        this.pnodeRefs = pnodeRefs;
    }
    
    public void setAggregatedNodeRefs(AggregatedNodeRefsDto aggregatedNodeRefs) {
        this.aggregatedNodeRefs = aggregatedNodeRefs;
    }
    
    public void setMeterReadings(Object meterReadings) {
        this.meterReadings = meterReadings;
    }
    
    public void setUsageSummaries(Object usageSummaries) {
        this.usageSummaries = usageSummaries;
    }
    
    public void setElectricPowerQualitySummaries(Object electricPowerQualitySummaries) {
        this.electricPowerQualitySummaries = electricPowerQualitySummaries;
    }
    
    /**
     * Default constructor for JAXB.
     */
    public UsagePointDto() {
        // Default constructor - fields will be initialized to null/default values
    }
    
    /**
     * Full constructor.
     */
    public UsagePointDto(String uuid, String description, byte[] roleFlags, ServiceCategory serviceCategory, 
                        Short status, SummaryMeasurementDto estimatedLoad, SummaryMeasurementDto nominalServiceVoltage, 
                        SummaryMeasurementDto ratedCurrent, SummaryMeasurementDto ratedPower,
                        ServiceDeliveryPointDto serviceDeliveryPoint, PnodeRefsDto pnodeRefs, 
                        AggregatedNodeRefsDto aggregatedNodeRefs, Object meterReadings, 
                        Object usageSummaries, Object electricPowerQualitySummaries) {
        this.uuid = uuid;
        this.description = description;
        this.roleFlags = roleFlags;
        this.serviceCategory = serviceCategory;
        this.status = status;
        this.estimatedLoad = estimatedLoad;
        this.nominalServiceVoltage = nominalServiceVoltage;
        this.ratedCurrent = ratedCurrent;
        this.ratedPower = ratedPower;
        this.serviceDeliveryPoint = serviceDeliveryPoint;
        this.pnodeRefs = pnodeRefs;
        this.aggregatedNodeRefs = aggregatedNodeRefs;
        this.meterReadings = meterReadings;
        this.usageSummaries = usageSummaries;
        this.electricPowerQualitySummaries = electricPowerQualitySummaries;
    }
    
    /**
     * Minimal constructor for basic usage point data.
     */
    public UsagePointDto(String uuid, ServiceCategory serviceCategory) {
        this.uuid = uuid;
        this.serviceCategory = serviceCategory;
    }
    
    /**
     * Constructor with core ESPI elements.
     */
    public UsagePointDto(String uuid, String description, ServiceCategory serviceCategory, 
                        SummaryMeasurementDto estimatedLoad, SummaryMeasurementDto nominalServiceVoltage, 
                        SummaryMeasurementDto ratedCurrent, SummaryMeasurementDto ratedPower,
                        ServiceDeliveryPointDto serviceDeliveryPoint) {
        this.uuid = uuid;
        this.description = description;
        this.serviceCategory = serviceCategory;
        this.estimatedLoad = estimatedLoad;
        this.nominalServiceVoltage = nominalServiceVoltage;
        this.ratedCurrent = ratedCurrent;
        this.ratedPower = ratedPower;
        this.serviceDeliveryPoint = serviceDeliveryPoint;
    }
    
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