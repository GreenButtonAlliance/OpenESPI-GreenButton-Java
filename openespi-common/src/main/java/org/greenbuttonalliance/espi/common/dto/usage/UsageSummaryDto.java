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

import org.greenbuttonalliance.espi.common.dto.common.DateTimeIntervalDto;

import org.greenbuttonalliance.espi.common.dto.BillingChargeSourceDto;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * UsageSummary DTO class for JAXB XML marshalling/unmarshalling.
 * <p>
 * Represents aggregated usage data for a usage point.
 * Per ESPI 4.0 XSD (espi.xsd:806-939), UsageSummary extends IdentifiedObject
 * and contains billing information, consumption summaries, and tariff details.
 * <p>
 * Supports Atom protocol XML wrapping with published, updated, and link metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "UsageSummary", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UsageSummary", namespace = "http://naesb.org/espi", propOrder = {
    "billingPeriod", "billLastPeriod", "billToDate",
    "costAdditionalLastPeriod", "costAdditionalDetailLastPeriod", "currency",
    "overallConsumptionLastPeriod", "currentBillingPeriodOverAllConsumption",
    "currentDayLastYearNetConsumption", "currentDayNetConsumption",
    "currentDayOverallConsumption", "peakDemand", "previousDayLastYearOverallConsumption",
    "previousDayNetConsumption", "previousDayOverallConsumption", "qualityOfReading",
    "ratchetDemand", "ratchetDemandPeriod", "statusTimeStamp", "commodity",
    "tariffProfile", "readCycle", "tariffRiderRefs", "billingChargeSource"
})
public class UsageSummaryDto {

    @XmlTransient
    private Long id;

    @XmlAttribute(name = "mRID")
    private String uuid;

    @XmlElement(name = "billingPeriod")
    private DateTimeIntervalDto billingPeriod;

    @XmlElement(name = "billLastPeriod")
    private Long billLastPeriod;

    @XmlElement(name = "billToDate")
    private Long billToDate;

    @XmlElement(name = "costAdditionalLastPeriod")
    private Long costAdditionalLastPeriod;

    @XmlElement(name = "costAdditionalDetailLastPeriod")
    private List<LineItemDto> costAdditionalDetailLastPeriod;

    @XmlElement(name = "currency")
    private String currency;

    @XmlElement(name = "overallConsumptionLastPeriod")
    private SummaryMeasurementDto overallConsumptionLastPeriod;

    @XmlElement(name = "currentBillingPeriodOverAllConsumption")
    private SummaryMeasurementDto currentBillingPeriodOverAllConsumption;

    @XmlElement(name = "currentDayLastYearNetConsumption")
    private SummaryMeasurementDto currentDayLastYearNetConsumption;

    @XmlElement(name = "currentDayNetConsumption")
    private SummaryMeasurementDto currentDayNetConsumption;

    @XmlElement(name = "currentDayOverallConsumption")
    private SummaryMeasurementDto currentDayOverallConsumption;

    @XmlElement(name = "peakDemand")
    private SummaryMeasurementDto peakDemand;

    @XmlElement(name = "previousDayLastYearOverallConsumption")
    private SummaryMeasurementDto previousDayLastYearOverallConsumption;

    @XmlElement(name = "previousDayNetConsumption")
    private SummaryMeasurementDto previousDayNetConsumption;

    @XmlElement(name = "previousDayOverallConsumption")
    private SummaryMeasurementDto previousDayOverallConsumption;

    @XmlElement(name = "qualityOfReading")
    private String qualityOfReading;

    @XmlElement(name = "ratchetDemand")
    private SummaryMeasurementDto ratchetDemand;

    @XmlElement(name = "ratchetDemandPeriod")
    private DateTimeIntervalDto ratchetDemandPeriod;

    @XmlElement(name = "statusTimeStamp")
    private Long statusTimeStamp;

    @XmlElement(name = "commodity")
    private Integer commodity;

    @XmlElement(name = "tariffProfile")
    private String tariffProfile;

    @XmlElement(name = "readCycle")
    private String readCycle;

    @XmlElement(name = "tariffRiderRefs")
    private TariffRiderRefsDto tariffRiderRefs;

    @XmlElement(name = "billingChargeSource")
    private BillingChargeSourceDto billingChargeSource;

    /**
     * Minimal constructor for basic usage summary data.
     *
     * @param uuid the resource identifier (mRID)
     * @param statusTimeStamp the status timestamp (required)
     */
    public UsageSummaryDto(String uuid, Long statusTimeStamp) {
        this(null, uuid, null, null, null, null, null, null,
             null, null, null, null, null, null, null, null,
             null, null, null, null, statusTimeStamp, null, null, null, null, null);
    }

    /**
     * Constructor with billing period and status timestamp.
     *
     * @param uuid the resource identifier (mRID)
     * @param billingPeriod the billing period
     * @param statusTimeStamp the status timestamp (required)
     */
    public UsageSummaryDto(String uuid, DateTimeIntervalDto billingPeriod, Long statusTimeStamp) {
        this(null, uuid, billingPeriod, null, null, null, null, null,
             null, null, null, null, null, null, null, null,
             null, null, null, null, statusTimeStamp, null, null, null, null, null);
    }
}
