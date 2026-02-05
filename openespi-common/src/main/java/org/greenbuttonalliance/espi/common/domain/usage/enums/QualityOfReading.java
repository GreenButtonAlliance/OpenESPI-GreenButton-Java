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
 * Enumeration for QualityOfReading values.
 *
 * List of codes indicating the quality of the reading.
 * Per ESPI 4.0 espi.xsd lines 3457-3540.
 */
@XmlType(name = "QualityOfReading", namespace = "http://naesb.org/espi")
@XmlEnum
public enum QualityOfReading {

    /**
     * Data that has gone through all required validation checks and either passed them all
     * or has been verified.
     * XSD value: 0 (line 3464)
     */
    @XmlEnumValue("0")
    VALID(0),

    /**
     * Replaced or approved by a human.
     * XSD value: 7 (line 3470)
     */
    @XmlEnumValue("7")
    MANUALLY_EDITED(7),

    /**
     * Data value was replaced by a machine computed value based on analysis of historical
     * data using the same type of measurement.
     * XSD value: 8 (line 3476)
     */
    @XmlEnumValue("8")
    ESTIMATED_USING_REFERENCE_DAY(8),

    /**
     * Data value was computed using linear interpolation based on the readings before and
     * after it.
     * XSD value: 9 (line 3482)
     */
    @XmlEnumValue("9")
    ESTIMATED_USING_LINEAR_INTERPOLATION(9),

    /**
     * Data that has failed one or more checks.
     * XSD value: 10 (line 3488)
     */
    @XmlEnumValue("10")
    QUESTIONABLE(10),

    /**
     * Data that has been calculated (using logic or mathematical operations).
     * XSD value: 11 (line 3494)
     */
    @XmlEnumValue("11")
    DERIVED(11),

    /**
     * Data that has been calculated as a projection or forecast of future readings.
     * XSD value: 12 (line 3500)
     */
    @XmlEnumValue("12")
    PROJECTED_FORECAST(12),

    /**
     * Indicates that the quality of this reading has mixed characteristics.
     * XSD value: 13 (line 3506)
     */
    @XmlEnumValue("13")
    MIXED(13),

    /**
     * Data that has not gone through the validation.
     * XSD value: 14 (line 3512)
     */
    @XmlEnumValue("14")
    RAW(14),

    /**
     * The values have been adjusted to account for weather.
     * XSD value: 15 (line 3518)
     */
    @XmlEnumValue("15")
    NORMALIZED_FOR_WEATHER(15),

    /**
     * Specifies that a characteristic applies other than those defined.
     * XSD value: 16 (line 3524)
     */
    @XmlEnumValue("16")
    OTHER(16),

    /**
     * Data that has been validated and possibly edited and/or estimated in accordance with
     * approved procedures.
     * XSD value: 17 (line 3530)
     */
    @XmlEnumValue("17")
    VALIDATED(17),

    /**
     * Data that failed at least one of the required validation checks but was determined to
     * represent actual usage.
     * XSD value: 18 (line 3536)
     */
    @XmlEnumValue("18")
    VERIFIED(18);

    private final int value;

    QualityOfReading(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static QualityOfReading fromValue(int value) {
        for (QualityOfReading quality : QualityOfReading.values()) {
            if (quality.value == value) {
                return quality;
            }
        }
        throw new IllegalArgumentException("Invalid QualityOfReading value: " + value);
    }
}
