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
 * Enumeration for DataQualifierKind values.
 *
 * Code describing a salient attribute of Readings of ReadingType.
 * Valid values per the standard are defined in DataQualifierType.
 * Per ESPI 4.0 espi.xsd lines 2196-2296.
 */
@XmlType(name = "DataQualifierKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum DataQualifierKind {

    /**
     * Not Applicable.
     * XSD value: 0 (line 2203)
     */
    @XmlEnumValue("0")
    NONE(0),

    /**
     * Average value.
     * XSD value: 2 (line 2209)
     */
    @XmlEnumValue("2")
    AVERAGE(2),

    /**
     * The value represents an amount over which a threshold was exceeded.
     * XSD value: 4 (line 2215)
     */
    @XmlEnumValue("4")
    EXCESS(4),

    /**
     * The value represents a programmed threshold.
     * XSD value: 5 (line 2221)
     */
    @XmlEnumValue("5")
    HIGH_THRESHOLD(5),

    /**
     * The value represents a programmed threshold.
     * XSD value: 7 (line 2227)
     */
    @XmlEnumValue("7")
    LOW_THRESHOLD(7),

    /**
     * The highest value observed.
     * XSD value: 8 (line 2233)
     */
    @XmlEnumValue("8")
    MAXIMUM(8),

    /**
     * The smallest value observed.
     * XSD value: 9 (line 2239)
     */
    @XmlEnumValue("9")
    MINIMUM(9),

    /**
     * Nominal.
     * XSD value: 11 (line 2245)
     */
    @XmlEnumValue("11")
    NOMINAL(11),

    /**
     * Normal.
     * XSD value: 12 (line 2251)
     */
    @XmlEnumValue("12")
    NORMAL(12),

    /**
     * The second highest value observed.
     * XSD value: 16 (line 2257)
     */
    @XmlEnumValue("16")
    SECOND_MAXIMUM(16),

    /**
     * The second smallest value observed.
     * XSD value: 17 (line 2263)
     */
    @XmlEnumValue("17")
    SECOND_MINIMUM(17),

    /**
     * The third highest value observed.
     * XSD value: 23 (line 2269)
     */
    @XmlEnumValue("23")
    THIRD_MAXIMUM(23),

    /**
     * The fourth highest value observed.
     * XSD value: 24 (line 2275)
     */
    @XmlEnumValue("24")
    FOURTH_MAXIMUM(24),

    /**
     * The fifth highest value observed.
     * XSD value: 25 (line 2281)
     */
    @XmlEnumValue("25")
    FIFTH_MAXIMUM(25),

    /**
     * The accumulated sum.
     * XSD value: 26 (line 2287)
     */
    @XmlEnumValue("26")
    SUM(26);

    private final int value;

    DataQualifierKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DataQualifierKind fromValue(int value) {
        for (DataQualifierKind kind : DataQualifierKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid DataQualifierKind value: " + value);
    }
}
