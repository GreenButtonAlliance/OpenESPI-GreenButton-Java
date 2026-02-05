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

package org.greenbuttonalliance.espi.common.domain.usage.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Enumeration for AccumulationKind values.
 *
 * Code indicating how value is accumulated over time for Readings of ReadingType.
 * The list of valid values per the standard are defined in AccumulationBehaviorType.
 * Per ESPI 4.0 espi.xsd lines 1851-1927.
 */
@XmlType(name = "AccumulationKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum AccumulationKind {

    /**
     * Not Applicable or implied by the unit of measure.
     * XSD value: 0 (line 1858)
     */
    @XmlEnumValue("0")
    NONE(0),

    /**
     * Bulk Quantity. Accumulation behaviour is not relevant.
     * XSD value: 1 (line 1864)
     */
    @XmlEnumValue("1")
    BULK_QUANTITY(1),

    /**
     * The sum of the previous billing period values and the present period value.
     * Note: "ContinuousCumulative" is commonly used in conjunction with "demand."
     * The "ContinuousCumulative Demand" would be the cumulative sum of the previous
     * billing period maximum demand values (as occurring with each demand reset)
     * summed with the present period maximum demand value (which has yet to be reset.)
     * XSD value: 2 (line 1870)
     */
    @XmlEnumValue("2")
    CONTINUOUS_CUMULATIVE(2),

    /**
     * The sum of the previous billing period values.
     * Note: "Cumulative" is commonly used in conjunction with "demand." Each demand
     * reset causes the maximum demand value for the present billing period (since the
     * last demand reset) to accumulate as an accumulative total of all maximum demands.
     * So instead of "zeroing" the demand register, a demand reset has the effect of
     * adding the present maximum demand to this accumulating total.
     * XSD value: 3 (line 1876)
     */
    @XmlEnumValue("3")
    CUMULATIVE(3),

    /**
     * The difference between the value at the end of the prescribed interval and the
     * beginning of the interval. This is used for incremental interval data.
     * Note: One common application would be for load profile data, another use might
     * be to report the number of events within an interval (such as the number of
     * equipment energizations within the specified period of time.)
     * XSD value: 4 (line 1882)
     */
    @XmlEnumValue("4")
    DELTA_DATA(4),

    /**
     * As if a needle is swung out on the meter face to a value to indicate the current value.
     * Note: An "indicating" value is typically measured over hundreds of milliseconds or
     * greater, or may imply a "pusher" mechanism to capture a value. Compare this to
     * "instantaneous" which is measured over a shorter period of time.
     * XSD value: 6 (line 1888)
     */
    @XmlEnumValue("6")
    INDICATING(6),

    /**
     * A form of accumulation which is selective with respect to time.
     * Note: "Summation" could be considered a specialization of "Bulk Quantity" according
     * to the rules of inheritance where "Summation" selectively accumulates pulses over a
     * timing pattern, and "BulkQuantity" accumulates pulses all of the time.
     * XSD value: 9 (line 1894)
     */
    @XmlEnumValue("9")
    SUMMATION(9),

    /**
     * A form of computation which introduces a time delay characteristic to the data value.
     * XSD value: 10 (line 1900)
     */
    @XmlEnumValue("10")
    TIME_DELAY(10),

    /**
     * Typically measured over the fastest period of time allowed by the definition of the
     * metric (usually milliseconds or tens of milliseconds.)
     * Note: "Instantaneous" was moved to attribute #3 in 61968-9Ed2 from attribute #1
     * in 61968-9Ed1.
     * XSD value: 12 (line 1906)
     */
    @XmlEnumValue("12")
    INSTANTANEOUS(12),

    /**
     * Latch the smallest value.
     * XSD value: 13 (line 1912)
     */
    @XmlEnumValue("13")
    LATCHING_QUANTITY(13),

    /**
     * When this description is applied to a metered value, it implies that the value is
     * a form of accumulation which is bounded by reset actions, but the reading value
     * itself is not reset to zero.
     * XSD value: 14 (line 1918)
     */
    @XmlEnumValue("14")
    BOUNDED_QUANTITY(14);

    private final int value;

    AccumulationKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AccumulationKind fromValue(int value) {
        for (AccumulationKind kind : AccumulationKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid AccumulationKind value: " + value);
    }
}
