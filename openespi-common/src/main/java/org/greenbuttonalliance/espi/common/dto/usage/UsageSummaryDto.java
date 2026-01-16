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

import org.greenbuttonalliance.espi.common.dto.BillingChargeSourceDto;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;

import jakarta.xml.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * UsageSummary DTO record for JAXB XML marshalling/unmarshalling.
 * <p>
 * Represents aggregated usage data for a usage point.
 * Per ESPI 4.0 XSD (espi.xsd:806-939), UsageSummary extends IdentifiedObject
 * and contains billing information, consumption summaries, and tariff details.
 * <p>
 * Supports Atom protocol XML wrapping with published, updated, and link metadata.
 */
@XmlRootElement(name = "UsageSummary", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.PROPERTY)
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
public record UsageSummaryDto(

    @XmlTransient
    Long id,

    @XmlAttribute(name = "mRID")
    String uuid,

    DateTimeIntervalDto billingPeriod,
    Long billLastPeriod,
    Long billToDate,
    Long costAdditionalLastPeriod,
    List<LineItemDto> costAdditionalDetailLastPeriod,
    String currency,
    SummaryMeasurementDto overallConsumptionLastPeriod,
    SummaryMeasurementDto currentBillingPeriodOverAllConsumption,
    SummaryMeasurementDto currentDayLastYearNetConsumption,
    SummaryMeasurementDto currentDayNetConsumption,
    SummaryMeasurementDto currentDayOverallConsumption,
    SummaryMeasurementDto peakDemand,
    SummaryMeasurementDto previousDayLastYearOverallConsumption,
    SummaryMeasurementDto previousDayNetConsumption,
    SummaryMeasurementDto previousDayOverallConsumption,
    String qualityOfReading,
    SummaryMeasurementDto ratchetDemand,
    DateTimeIntervalDto ratchetDemandPeriod,
    Long statusTimeStamp,
    Integer commodity,
    String tariffProfile,
    String readCycle,
    TariffRiderRefsDto tariffRiderRefs,
    BillingChargeSourceDto billingChargeSource
) {

    /**
     * The billing period to which the included measurements apply.
     * May also be an off-bill arbitrary period.
     */
    @XmlElement(name = "billingPeriod")
    public DateTimeIntervalDto getBillingPeriod() {
        return billingPeriod;
    }

    /**
     * The amount of the bill for the referenced billingPeriod in hundred-thousandths
     * of the currency specified in the ReadingType (e.g., 840 = USD).
     */
    @XmlElement(name = "billLastPeriod")
    public Long getBillLastPeriod() {
        return billLastPeriod;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, the bill amount as of the date the summary is generated,
     * in hundred-thousandths of the currency.
     */
    @XmlElement(name = "billToDate")
    public Long getBillToDate() {
        return billToDate;
    }

    /**
     * Additional charges from the referenced billingPeriod, in hundred-thousandths
     * of the currency specified in the ReadingType.
     */
    @XmlElement(name = "costAdditionalLastPeriod")
    public Long getCostAdditionalLastPeriod() {
        return costAdditionalLastPeriod;
    }

    /**
     * Additional charges from the referenced billingPeriod which in total add up
     * to costAdditionalLastPeriod.
     * Extension field.
     */
    @XmlElement(name = "costAdditionalDetailLastPeriod")
    public List<LineItemDto> getCostAdditionalDetailLastPeriod() {
        return costAdditionalDetailLastPeriod;
    }

    /**
     * The ISO 4217 code indicating the currency applicable to the bill amounts
     * in the summary.
     */
    @XmlElement(name = "currency")
    public String getCurrency() {
        return currency;
    }

    /**
     * The amount of energy consumed for the referenced billingPeriod.
     * Extension field.
     */
    @XmlElement(name = "overallConsumptionLastPeriod")
    public SummaryMeasurementDto getOverallConsumptionLastPeriod() {
        return overallConsumptionLastPeriod;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, the total consumption for the billing period.
     */
    @XmlElement(name = "currentBillingPeriodOverAllConsumption")
    public SummaryMeasurementDto getCurrentBillingPeriodOverAllConsumption() {
        return currentBillingPeriodOverAllConsumption;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, the amount of energy consumed one year ago interpreted
     * as same day of week same week of year (see ISO 8601) based on the day of the statusTimeStamp.
     */
    @XmlElement(name = "currentDayLastYearNetConsumption")
    public SummaryMeasurementDto getCurrentDayLastYearNetConsumption() {
        return currentDayLastYearNetConsumption;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, net consumption for the current day (delivered - received)
     * based on the day of the statusTimeStamp.
     */
    @XmlElement(name = "currentDayNetConsumption")
    public SummaryMeasurementDto getCurrentDayNetConsumption() {
        return currentDayNetConsumption;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, overall energy consumption for the current day,
     * based on the day of the statusTimeStamp.
     */
    @XmlElement(name = "currentDayOverallConsumption")
    public SummaryMeasurementDto getCurrentDayOverallConsumption() {
        return currentDayOverallConsumption;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, peak demand recorded for the current period.
     */
    @XmlElement(name = "peakDemand")
    public SummaryMeasurementDto getPeakDemand() {
        return peakDemand;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, the amount of energy consumed on the previous day
     * one year ago interpreted as same day of week same week of year (see ISO 8601)
     * based on the day of the statusTimestamp.
     */
    @XmlElement(name = "previousDayLastYearOverallConsumption")
    public SummaryMeasurementDto getPreviousDayLastYearOverallConsumption() {
        return previousDayLastYearOverallConsumption;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, net consumption for the previous day relative to
     * the day of the statusTimestamp.
     */
    @XmlElement(name = "previousDayNetConsumption")
    public SummaryMeasurementDto getPreviousDayNetConsumption() {
        return previousDayNetConsumption;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, the total consumption for the previous day based
     * on the day of the statusTimestamp.
     */
    @XmlElement(name = "previousDayOverallConsumption")
    public SummaryMeasurementDto getPreviousDayOverallConsumption() {
        return previousDayOverallConsumption;
    }

    /**
     * Indication of the quality of the summary readings.
     * QualityOfReading enumeration value.
     */
    @XmlElement(name = "qualityOfReading")
    public String getQualityOfReading() {
        return qualityOfReading;
    }

    /**
     * If the summary contains data from a current period beyond the end of the
     * referenced billingPeriod, the current ratchet demand value for the ratchet
     * demand over the ratchetDemandPeriod.
     */
    @XmlElement(name = "ratchetDemand")
    public SummaryMeasurementDto getRatchetDemand() {
        return ratchetDemand;
    }

    /**
     * The period over which the ratchet demand applies.
     */
    @XmlElement(name = "ratchetDemandPeriod")
    public DateTimeIntervalDto getRatchetDemandPeriod() {
        return ratchetDemandPeriod;
    }

    /**
     * Date/Time status of this UsageSummary.
     * Required field - TimeType (epoch seconds).
     */
    @XmlElement(name = "statusTimeStamp")
    public Long getStatusTimeStamp() {
        return statusTimeStamp;
    }

    /**
     * The commodity for this summary report.
     * CommodityKind enumeration value.
     * Extension field.
     */
    @XmlElement(name = "commodity")
    public Integer getCommodity() {
        return commodity;
    }

    /**
     * A schedule of charges; structure associated with Tariff that allows the
     * definition of complex tariff structures such as step and time of use.
     * Extension field, maximum length 256 characters.
     */
    @XmlElement(name = "tariffProfile")
    public String getTariffProfile() {
        return tariffProfile;
    }

    /**
     * Cycle day on which the meter for this usage point will normally be read.
     * Usually correlated with the billing cycle.
     * Extension field, maximum length 256 characters.
     */
    @XmlElement(name = "readCycle")
    public String getReadCycle() {
        return readCycle;
    }

    /**
     * List of rate options applied to the base tariff profile.
     * Extension field.
     */
    @XmlElement(name = "tariffRiderRefs")
    public TariffRiderRefsDto getTariffRiderRefs() {
        return tariffRiderRefs;
    }

    /**
     * Source of Billing Charge.
     * Extension field.
     */
    @XmlElement(name = "billingChargeSource")
    public BillingChargeSourceDto getBillingChargeSource() {
        return billingChargeSource;
    }

    /**
     * Default constructor for JAXB.
     */
    public UsageSummaryDto() {
        this(null, null, null, null, null, null, null, null,
             null, null, null, null, null, null, null, null,
             null, null, null, null, null, null, null, null, null, null);
    }

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
