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
 * Enumeration for UnitMultiplierKind values.
 *
 * The power of ten unit multipliers.
 * Per ESPI 4.0 espi.xsd lines 3368-3456.
 */
@XmlType(name = "UnitMultiplierKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum UnitMultiplierKind {

    /**
     * Pico 10**-12.
     * XSD value: -12 (line 3375)
     */
    @XmlEnumValue("-12")
    PICO(-12),

    /**
     * Nano 10**-9.
     * XSD value: -9 (line 3381)
     */
    @XmlEnumValue("-9")
    NANO(-9),

    /**
     * Micro 10**-6.
     * XSD value: -6 (line 3387)
     */
    @XmlEnumValue("-6")
    MICRO(-6),

    /**
     * Milli 10**-3.
     * XSD value: -3 (line 3393)
     */
    @XmlEnumValue("-3")
    MILLI(-3),

    /**
     * Centi 10**-2.
     * XSD value: -2 (line 3399)
     */
    @XmlEnumValue("-2")
    CENTI(-2),

    /**
     * Deci 10**-1.
     * XSD value: -1 (line 3405)
     */
    @XmlEnumValue("-1")
    DECI(-1),

    /**
     * Kilo 10**3.
     * XSD value: 3 (line 3411)
     */
    @XmlEnumValue("3")
    KILO(3),

    /**
     * Mega 10**6.
     * XSD value: 6 (line 3417)
     */
    @XmlEnumValue("6")
    MEGA(6),

    /**
     * Giga 10**9.
     * XSD value: 9 (line 3423)
     */
    @XmlEnumValue("9")
    GIGA(9),

    /**
     * Tera 10**12.
     * XSD value: 12 (line 3429)
     */
    @XmlEnumValue("12")
    TERA(12),

    /**
     * Not Applicable or "x1".
     * XSD value: 0 (line 3435)
     */
    @XmlEnumValue("0")
    NONE(0),

    /**
     * Deca 10**1.
     * XSD value: 1 (line 3441)
     */
    @XmlEnumValue("1")
    DECA(1),

    /**
     * Hecto 10**2.
     * XSD value: 2 (line 3447)
     */
    @XmlEnumValue("2")
    HECTO(2);

    private final int value;

    UnitMultiplierKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static UnitMultiplierKind fromValue(int value) {
        for (UnitMultiplierKind kind : UnitMultiplierKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid UnitMultiplierKind value: " + value);
    }
}