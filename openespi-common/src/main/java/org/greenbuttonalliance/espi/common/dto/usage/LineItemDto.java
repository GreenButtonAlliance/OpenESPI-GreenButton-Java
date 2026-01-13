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
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;

/**
 * LineItem DTO record for JAXB XML marshalling/unmarshalling.
 *
 * Represents a line item of detail for additional cost.
 * Contains billing line item details including amount, rounding, timestamp,
 * descriptive note, measurement, classification, unit cost, and period.
 *
 * Per ESPI 4.0 XSD (espi.xsd:1444-1494), LineItem extends Object (not IdentifiedObject),
 * so it has no UUID, selfLink, upLink, or other Atom metadata.
 *
 * Part of the NAESB ESPI UsageSummary structure for detailed cost breakdowns.
 */
@XmlRootElement(name = "LineItem", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "LineItem", namespace = "http://naesb.org/espi", propOrder = {
    "amount", "rounding", "dateTime", "note", "measurement", "itemKind", "unitCost", "itemPeriod"
})
public record LineItemDto(
    Long amount,
    Long rounding,
    Long dateTime,
    String note,
    SummaryMeasurementDto measurement,
    Integer itemKind,
    Long unitCost,
    DateTimeIntervalDto itemPeriod
) {

    /**
     * Cost of line item in currency minor units (e.g., cents).
     */
    @XmlElement(name = "amount")
    public Long getAmount() {
        return amount;
    }

    /**
     * Rounding adjustment applied to the line item amount.
     */
    @XmlElement(name = "rounding")
    public Long getRounding() {
        return rounding;
    }

    /**
     * Significant date/time for this line item.
     * Stored as epoch seconds (TimeType in ESPI).
     */
    @XmlElement(name = "dateTime")
    public Long getDateTime() {
        return dateTime;
    }

    /**
     * Comment or description of the line item.
     */
    @XmlElement(name = "note")
    public String getNote() {
        return note;
    }

    /**
     * Relevant measurement for the line item (extension field).
     * Contains measurement value, unit, multiplier, and reading type reference.
     */
    @XmlElement(name = "measurement")
    public SummaryMeasurementDto getMeasurement() {
        return measurement;
    }

    /**
     * Classification of the line item (extension field).
     * ItemKind enumeration values (e.g., 1=Energy Generation Fee, 2=Energy Delivery Fee, etc.).
     */
    @XmlElement(name = "itemKind")
    public Integer getItemKind() {
        return itemKind;
    }

    /**
     * Per unit cost (extension field).
     * Cost per unit of measurement.
     */
    @XmlElement(name = "unitCost")
    public Long getUnitCost() {
        return unitCost;
    }

    /**
     * Time period covered by this line item (extension field).
     * Supports pricing changes in the middle of a billing period.
     */
    @XmlElement(name = "itemPeriod")
    public DateTimeIntervalDto getItemPeriod() {
        return itemPeriod;
    }

    /**
     * Default constructor for JAXB.
     */
    public LineItemDto() {
        this(null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor with basic line item information.
     *
     * @param amount the amount in currency minor units
     * @param dateTime the significant date/time
     * @param note the descriptive note
     * @param itemKind the classification of the line item
     */
    public LineItemDto(Long amount, Long dateTime, String note, Integer itemKind) {
        this(amount, null, dateTime, note, null, itemKind, null, null);
    }

    /**
     * Checks if this line item has rounding applied.
     *
     * @return true if rounding is non-zero
     */
    public boolean hasRounding() {
        return rounding != null && rounding != 0;
    }

    /**
     * Gets the total amount including rounding.
     *
     * @return total amount, or just amount if no rounding
     */
    public Long getTotalAmount() {
        if (rounding == null) {
            return amount;
        }
        return amount != null ? amount + rounding : rounding;
    }

    /**
     * Checks if this line item represents a charge (positive amount).
     *
     * @return true if amount is positive
     */
    public boolean isCharge() {
        return amount != null && amount > 0;
    }

    /**
     * Checks if this line item represents a credit (negative amount).
     *
     * @return true if amount is negative
     */
    public boolean isCredit() {
        return amount != null && amount < 0;
    }

    /**
     * Checks if this line item has a measurement value.
     *
     * @return true if measurement is present
     */
    public boolean hasMeasurement() {
        return measurement != null;
    }

    /**
     * Checks if this line item has a time period specified.
     *
     * @return true if itemPeriod is present
     */
    public boolean hasItemPeriod() {
        return itemPeriod != null;
    }

    /**
     * Validates the line item data.
     *
     * @return true if valid (note and itemKind are required)
     */
    public boolean isValid() {
        return note != null && !note.trim().isEmpty() && itemKind != null;
    }

    /**
     * Creates a basic LineItem with required fields.
     *
     * @param amount the amount
     * @param dateTime the date/time
     * @param note the note
     * @param itemKind the item kind
     * @return new LineItemDto
     */
    public static LineItemDto create(Long amount, Long dateTime, String note, Integer itemKind) {
        return new LineItemDto(amount, null, dateTime, note, null, itemKind, null, null);
    }

    /**
     * Creates a LineItem with amount, rounding, and basic fields.
     *
     * @param amount the amount
     * @param rounding the rounding
     * @param dateTime the date/time
     * @param note the note
     * @param itemKind the item kind
     * @return new LineItemDto
     */
    public static LineItemDto createWithRounding(Long amount, Long rounding, Long dateTime, String note, Integer itemKind) {
        return new LineItemDto(amount, rounding, dateTime, note, null, itemKind, null, null);
    }
}