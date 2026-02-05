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
 * Enumeration for TimeAttributeKind values.
 *
 * Code used to specify a particular type of time interval method for Readings of ReadingType.
 * Valid values per the standard are defined in TimeAttributeType.
 * Per ESPI 4.0 espi.xsd lines 3623-3748.
 */
@XmlType(name = "TimeAttributeKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum TimeAttributeKind {

    /**
     * Not Applicable.
     * XSD value: 0 (line 3630)
     */
    @XmlEnumValue("0")
    NONE(0),

    /**
     * 10-minute.
     * XSD value: 1 (line 3636)
     */
    @XmlEnumValue("1")
    TEN_MINUTE(1),

    /**
     * 15-minute.
     * XSD value: 2 (line 3642)
     */
    @XmlEnumValue("2")
    FIFTEEN_MINUTE(2),

    /**
     * 1-minute.
     * XSD value: 3 (line 3648)
     */
    @XmlEnumValue("3")
    ONE_MINUTE(3),

    /**
     * 24-hour.
     * XSD value: 4 (line 3654)
     */
    @XmlEnumValue("4")
    TWENTY_FOUR_HOUR(4),

    /**
     * 30-minute.
     * XSD value: 5 (line 3660)
     */
    @XmlEnumValue("5")
    THIRTY_MINUTE(5),

    /**
     * 5-minute.
     * XSD value: 6 (line 3666)
     */
    @XmlEnumValue("6")
    FIVE_MINUTE(6),

    /**
     * 60-minute.
     * XSD value: 7 (line 3672)
     */
    @XmlEnumValue("7")
    SIXTY_MINUTE(7),

    /**
     * 2-minute.
     * XSD value: 10 (line 3678)
     */
    @XmlEnumValue("10")
    TWO_MINUTE(10),

    /**
     * 3-minute.
     * XSD value: 14 (line 3684)
     */
    @XmlEnumValue("14")
    THREE_MINUTE(14),

    /**
     * Within the present period of time.
     * XSD value: 15 (line 3690)
     */
    @XmlEnumValue("15")
    PRESENT(15),

    /**
     * Shifted within the previous monthly cycle and data set.
     * XSD value: 16 (line 3696)
     */
    @XmlEnumValue("16")
    PREVIOUS(16),

    /**
     * 20-minute interval.
     * XSD value: 31 (line 3702)
     */
    @XmlEnumValue("31")
    TWENTY_MINUTE(31),

    /**
     * 60-minute Fixed Block.
     * XSD value: 50 (line 3708)
     */
    @XmlEnumValue("50")
    FIXED_BLOCK_60_MIN(50),

    /**
     * 30-minute Fixed Block.
     * XSD value: 51 (line 3714)
     */
    @XmlEnumValue("51")
    FIXED_BLOCK_30_MIN(51),

    /**
     * 20-minute Fixed Block.
     * XSD value: 52 (line 3720)
     */
    @XmlEnumValue("52")
    FIXED_BLOCK_20_MIN(52),

    /**
     * 15-minute Fixed Block.
     * XSD value: 53 (line 3726)
     */
    @XmlEnumValue("53")
    FIXED_BLOCK_15_MIN(53),

    /**
     * 10-minute Fixed Block.
     * XSD value: 54 (line 3732)
     */
    @XmlEnumValue("54")
    FIXED_BLOCK_10_MIN(54),

    /**
     * 5-minute Fixed Block.
     * XSD value: 55 (line 3738)
     */
    @XmlEnumValue("55")
    FIXED_BLOCK_5_MIN(55),

    /**
     * 1-minute Fixed Block.
     * XSD value: 56 (line 3744)
     */
    @XmlEnumValue("56")
    FIXED_BLOCK_1_MIN(56);

    private final int value;

    TimeAttributeKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TimeAttributeKind fromValue(int value) {
        for (TimeAttributeKind kind : TimeAttributeKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid TimeAttributeKind value: " + value);
    }
}