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

package org.greenbuttonalliance.espi.common.domain.customer.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Enumeration for MeterMultiplierKind values.
 *
 * Kind of meter multiplier applied to meter readings.
 * Per ESPI 4.0 customer.xsd lines 1920-1960.
 */
@XmlType(name = "MeterMultiplierKind", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum MeterMultiplierKind {

    /**
     * Meter kh (watthour) constant.
     * The number of watthours that must be applied to the meter to cause one disk revolution
     * for an electromechanical meter or the number of watthours represented by one increment
     * pulse for an electronic meter.
     * XSD value: "kH" (line 1927)
     */
    @XmlEnumValue("kH")
    KH("kH"),

    /**
     * The ratio of the transformer's primary and secondary windings (turns) with respect to each other.
     * XSD value: "transformerRatio" (line 1932)
     */
    @XmlEnumValue("transformerRatio")
    TRANSFORMER_RATIO("transformerRatio"),

    /**
     * Register multiplier.
     * The number to multiply the register reading by in order to get kWh.
     * XSD value: "kR" (line 1937)
     */
    @XmlEnumValue("kR")
    KR("kR"),

    /**
     * Test constant.
     * XSD value: "kE" (line 1942)
     */
    @XmlEnumValue("kE")
    KE("kE"),

    /**
     * Current transformer ratio used to convert associated quantities to real measurements.
     * XSD value: "ctRatio" (line 1947)
     */
    @XmlEnumValue("ctRatio")
    CT_RATIO("ctRatio"),

    /**
     * Potential transformer ratio used to convert associated quantities to real measurements.
     * XSD value: "ptRatio" (line 1952)
     */
    @XmlEnumValue("ptRatio")
    PT_RATIO("ptRatio");

    private final String value;

    MeterMultiplierKind(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MeterMultiplierKind fromValue(String value) {
        for (MeterMultiplierKind kind : MeterMultiplierKind.values()) {
            if (kind.value.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid MeterMultiplierKind value: " + value);
    }
}
