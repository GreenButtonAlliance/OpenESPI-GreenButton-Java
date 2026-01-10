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

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * AggregatedNodeRef DTO record for JAXB XML marshalling/unmarshalling.
 *
 * Represents a reference to an aggregated node in the electrical grid.
 * Used within UsagePoint to specify aggregated pricing/load zones.
 *
 * Per ESPI 4.0 XSD (espi.xsd:1597), pnodeRef has minOccurs="0" maxOccurs="unbounded"
 *
 * Part of the NAESB ESPI UsagePoint structure for aggregated node references.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "AggregatedNodeRef", namespace = "http://naesb.org/espi", propOrder = {
    "anodeType", "ref", "startEffectiveDate", "endEffectiveDate", "pnodeRef"
})
public record AggregatedNodeRefDto(

    String anodeType,
    String ref,
    Long startEffectiveDate,
    Long endEffectiveDate,
    List<PnodeRefDto> pnodeRef
) {
    
    /**
     * Type of the aggregated node.
     * Indicates the category or classification of the aggregated node.
     */
    @XmlElement(name = "anodeType")
    public String getAnodeType() {
        return anodeType;
    }
    
    /**
     * Reference to the aggregated node identifier.
     */
    @XmlElement(name = "ref")
    public String getRef() {
        return ref;
    }
    
    /**
     * Start effective date for the aggregated node reference validity.
     * Stored as epoch seconds (TimeType in ESPI).
     */
    @XmlElement(name = "startEffectiveDate")
    public Long getStartEffectiveDate() {
        return startEffectiveDate;
    }
    
    /**
     * End effective date for the aggregated node reference validity.
     * Stored as epoch seconds (TimeType in ESPI).
     */
    @XmlElement(name = "endEffectiveDate")
    public Long getEndEffectiveDate() {
        return endEffectiveDate;
    }
    
    /**
     * Pricing node references associated with this aggregated node.
     * Contains the underlying pricing nodes that contribute to the aggregated node.
     * Per ESPI 4.0 XSD (espi.xsd:1597), supports 0 to many pricing node references.
     */
    @XmlElement(name = "pnodeRef")
    public List<PnodeRefDto> getPnodeRef() {
        return pnodeRef != null ? pnodeRef : new ArrayList<>();
    }

    /**
     * Default constructor for JAXB.
     */
    public AggregatedNodeRefDto() {
        this(null, null, null, null, new ArrayList<>());
    }

    /**
     * Constructor with aggregated node reference and type.
     */
    public AggregatedNodeRefDto(String anodeType, String ref) {
        this(anodeType, ref, null, null, new ArrayList<>());
    }

    /**
     * Constructor with aggregated node reference, type, and pricing nodes.
     */
    public AggregatedNodeRefDto(String anodeType, String ref, List<PnodeRefDto> pnodeRef) {
        this(anodeType, ref, null, null, pnodeRef);
    }
    
    /**
     * Checks if this aggregated node reference is currently valid.
     * 
     * @return true if valid for current time
     */
    public boolean isValid() {
        long currentTime = System.currentTimeMillis() / 1000;
        return (startEffectiveDate == null || startEffectiveDate <= currentTime) && 
               (endEffectiveDate == null || endEffectiveDate >= currentTime);
    }
    
    /**
     * Creates an AggregatedNodeRef with current validity period.
     *
     * @param anodeType the type of aggregated node
     * @param ref aggregated node reference
     * @return AggregatedNodeRef valid from now
     */
    public static AggregatedNodeRefDto createCurrent(String anodeType, String ref) {
        long currentTime = System.currentTimeMillis() / 1000;
        return new AggregatedNodeRefDto(anodeType, ref, currentTime, null, new ArrayList<>());
    }

    /**
     * Creates an AggregatedNodeRef with current validity period and pricing node references.
     *
     * @param anodeType the type of aggregated node
     * @param ref aggregated node reference
     * @param pnodeRef associated pricing node references
     * @return AggregatedNodeRef valid from now
     */
    public static AggregatedNodeRefDto createCurrent(String anodeType, String ref, List<PnodeRefDto> pnodeRef) {
        long currentTime = System.currentTimeMillis() / 1000;
        return new AggregatedNodeRefDto(anodeType, ref, currentTime, null, pnodeRef);
    }

    /**
     * Creates an AggregatedNodeRef with specified validity period.
     *
     * @param anodeType the type of aggregated node
     * @param ref aggregated node reference
     * @param startEffectiveDate start of validity period (epoch seconds)
     * @param endEffectiveDate end of validity period (epoch seconds, null for indefinite)
     * @param pnodeRef associated pricing node references
     * @return AggregatedNodeRef with specified validity
     */
    public static AggregatedNodeRefDto create(String anodeType, String ref, Long startEffectiveDate, Long endEffectiveDate, List<PnodeRefDto> pnodeRef) {
        return new AggregatedNodeRefDto(anodeType, ref, startEffectiveDate, endEffectiveDate, pnodeRef);
    }
}