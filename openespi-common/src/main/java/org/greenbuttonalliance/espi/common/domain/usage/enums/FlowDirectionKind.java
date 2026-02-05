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
 * Enumeration for FlowDirectionKind values.
 *
 * Direction associated with current related Readings.
 * Valid values per the standard are defined in FlowDirectionType.
 * Per ESPI 4.0 espi.xsd lines 2297-2433.
 */
@XmlType(name = "FlowDirectionKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum FlowDirectionKind {

    /**
     * Not Applicable (N/A).
     * XSD value: 0 (line 2304)
     */
    @XmlEnumValue("0")
    NONE(0),

    /**
     * "Delivered," or "Imported" as defined 61968-2.
     * Forward Active Energy is a positive kWh value as one would naturally expect to find
     * as energy is supplied by the utility and consumed at the service. Forward Reactive
     * Energy is a positive VArh value as one would naturally expect to find in the presence
     * of inductive loading. In polyphase metering, the forward energy register is incremented
     * when the sum of the phase energies is greater than zero.
     * XSD value: 1 (line 2310)
     */
    @XmlEnumValue("1")
    FORWARD(1),

    /**
     * Typically used to describe that a power factor is lagging the reference value.
     * Note 1: When used to describe VA, "lagging" describes a form of measurement where
     * reactive power is considered in all four quadrants, but real power is considered
     * only in quadrants I and IV.
     * Note 2: When used to describe power factor, the term "Lagging" implies that the PF
     * is negative. The term "lagging" in this case takes the place of the negative sign.
     * If a signed PF value is to be passed by the data producer, then the direction of
     * flow enumeration zero (none) should be used in order to avoid the possibility of
     * creating an expression that employs a double negative. The data consumer should be
     * able to tell from the sign of the data if the PF is leading or lagging. This principle
     * is analogous to the concept that "Reverse" energy is an implied negative value, and
     * to publish a negative reverse value would be ambiguous.
     * Note 3: Lagging power factors typically indicate inductive loading.
     * XSD value: 2 (line 2316)
     */
    @XmlEnumValue("2")
    LAGGING(2),

    /**
     * Typically used to describe that a power factor is leading the reference value.
     * Note: Leading power factors typically indicate capacitive loading.
     * XSD value: 3 (line 2322)
     */
    @XmlEnumValue("3")
    LEADING(3),

    /**
     * |Forward| - |Reverse|, See 61968-2.
     * Note: In some systems, the value passed as a "net" value could become negative.
     * In other systems the value passed as a "net" value is always a positive number,
     * and rolls-over and rolls-under as needed.
     * XSD value: 4 (line 2328)
     */
    @XmlEnumValue("4")
    NET(4),

    /**
     * Reactive positive quadrants. (The term "lagging" is preferred.)
     * XSD value: 5 (line 2334)
     */
    @XmlEnumValue("5")
    Q1_PLUS_Q2(5),

    /**
     * Quadrants 1 and 3.
     * XSD value: 7 (line 2340)
     */
    @XmlEnumValue("7")
    Q1_PLUS_Q3(7),

    /**
     * Quadrants 1 and 4 usually represent forward active energy.
     * XSD value: 8 (line 2346)
     */
    @XmlEnumValue("8")
    Q1_PLUS_Q4(8),

    /**
     * Q1 minus Q4.
     * XSD value: 9 (line 2352)
     */
    @XmlEnumValue("9")
    Q1_MINUS_Q4(9),

    /**
     * Quadrants 2 and 3 usually represent reverse active energy.
     * XSD value: 10 (line 2358)
     */
    @XmlEnumValue("10")
    Q2_PLUS_Q3(10),

    /**
     * Quadrants 2 and 4.
     * XSD value: 11 (line 2364)
     */
    @XmlEnumValue("11")
    Q2_PLUS_Q4(11),

    /**
     * Q2 minus Q3.
     * XSD value: 12 (line 2370)
     */
    @XmlEnumValue("12")
    Q2_MINUS_Q3(12),

    /**
     * Reactive negative quadrants. (The term "leading" is preferred.)
     * XSD value: 13 (line 2376)
     */
    @XmlEnumValue("13")
    Q3_PLUS_Q4(13),

    /**
     * Q3 minus Q2.
     * XSD value: 14 (line 2382)
     */
    @XmlEnumValue("14")
    Q3_MINUS_Q2(14),

    /**
     * Q1 only.
     * XSD value: 15 (line 2388)
     */
    @XmlEnumValue("15")
    QUADRANT_1(15),

    /**
     * Q2 only.
     * XSD value: 16 (line 2394)
     */
    @XmlEnumValue("16")
    QUADRANT_2(16),

    /**
     * Q3 only.
     * XSD value: 17 (line 2400)
     */
    @XmlEnumValue("17")
    QUADRANT_3(17),

    /**
     * Q4 only.
     * XSD value: 18 (line 2406)
     */
    @XmlEnumValue("18")
    QUADRANT_4(18),

    /**
     * Reverse Active Energy is equivalent to "Received," or "Exported" as defined in 61968-2.
     * Reverse Active Energy is a positive kWh value as one would expect to find when energy
     * is backfed by the service onto the utility network. Reverse Reactive Energy is a positive
     * VArh value as one would expect to find in the presence of capacitive loading and a leading
     * Power Factor. In polyphase metering, the reverse energy register is incremented when the
     * sum of the phase energies is less than zero.
     * Note: The value passed as a reverse value is always a positive value. It is understood
     * by the label "reverse" that it represents negative flow.
     * XSD value: 19 (line 2412)
     */
    @XmlEnumValue("19")
    REVERSE(19),

    /**
     * |Forward| + |Reverse|, See 61968-2.
     * The sum of the commodity in all quadrants Q1+Q2+Q3+Q4. In polyphase metering, the total
     * energy register is incremented when the absolute value of the sum of the phase energies
     * is greater than zero.
     * XSD value: 20 (line 2418)
     */
    @XmlEnumValue("20")
    TOTAL(20),

    /**
     * In polyphase metering, the total by phase energy register is incremented when the sum
     * of the absolute values of the phase energies is greater than zero. In single phase
     * metering, the formulas for "Total" and "Total by phase" collapse to the same expression.
     * For communication purposes however, the "Total" enumeration should be used with single
     * phase meter data.
     * XSD value: 21 (line 2424)
     */
    @XmlEnumValue("21")
    TOTAL_BY_PHASE(21);

    private final int value;

    FlowDirectionKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FlowDirectionKind fromValue(int value) {
        for (FlowDirectionKind kind : FlowDirectionKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid FlowDirectionKind value: " + value);
    }
}
