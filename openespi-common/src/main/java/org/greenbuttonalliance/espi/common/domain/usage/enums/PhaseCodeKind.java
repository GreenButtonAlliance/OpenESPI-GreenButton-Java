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
 * Enumeration for PhaseCodeKind values.
 *
 * Enumeration of phase identifiers. Allows designation of phases for both transmission and
 * distribution equipment, circuits and loads. Residential and small commercial loads are often
 * served from single-phase, or split-phase, secondary circuits. Phases 1 and 2 refer to hot
 * wires that are 180 degrees out of phase, while N refers to the neutral wire. Through
 * single-phase transformer connections, these secondary circuits may be served from one or
 * two of the primary phases A, B, and C. For three-phase loads, use the A, B, C phase codes
 * instead of s12N.
 * Per ESPI 4.0 espi.xsd lines 3195-3339.
 */
@XmlType(name = "PhaseCodeKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum PhaseCodeKind {

    /**
     * ABC to Neutral.
     * XSD value: 225 (line 3202)
     */
    @XmlEnumValue("225")
    ABCN(225),

    /**
     * Involving all phases.
     * XSD value: 224 (line 3208)
     */
    @XmlEnumValue("224")
    ABC(224),

    /**
     * AB to Neutral.
     * XSD value: 193 (line 3214)
     */
    @XmlEnumValue("193")
    ABN(193),

    /**
     * Phases A, C and neutral.
     * XSD value: 41 (line 3220)
     */
    @XmlEnumValue("41")
    ACN(41),

    /**
     * BC to neutral.
     * XSD value: 97 (line 3226)
     */
    @XmlEnumValue("97")
    BCN(97),

    /**
     * Phases A to B.
     * XSD value: 132 (line 3232)
     */
    @XmlEnumValue("132")
    AB(132),

    /**
     * Phases A and C.
     * XSD value: 96 (line 3238)
     */
    @XmlEnumValue("96")
    AC(96),

    /**
     * Phases B to C.
     * XSD value: 66 (line 3244)
     */
    @XmlEnumValue("66")
    BC(66),

    /**
     * Phases A to neutral.
     * XSD value: 129 (line 3250)
     */
    @XmlEnumValue("129")
    AN(129),

    /**
     * Phases B to neutral.
     * XSD value: 65 (line 3256)
     */
    @XmlEnumValue("65")
    BN(65),

    /**
     * Phases C to neutral.
     * XSD value: 33 (line 3262)
     */
    @XmlEnumValue("33")
    CN(33),

    /**
     * Phase A.
     * XSD value: 128 (line 3268)
     */
    @XmlEnumValue("128")
    A(128),

    /**
     * Phase B.
     * XSD value: 64 (line 3274)
     */
    @XmlEnumValue("64")
    B(64),

    /**
     * Phase C.
     * XSD value: 32 (line 3280)
     */
    @XmlEnumValue("32")
    C(32),

    /**
     * Neutral.
     * XSD value: 16 (line 3286)
     */
    @XmlEnumValue("16")
    N(16),

    /**
     * Phase S2 to neutral.
     * XSD value: 272 (line 3292)
     */
    @XmlEnumValue("272")
    S2N(272),

    /**
     * Phase S1, S2 to neutral.
     * XSD value: 784 (line 3298)
     */
    @XmlEnumValue("784")
    S12N_784(784),

    /**
     * Phase S1 to Neutral.
     * XSD value: 528 (line 3304)
     */
    @XmlEnumValue("528")
    S1N(528),

    /**
     * Phase S2.
     * XSD value: 256 (line 3310)
     */
    @XmlEnumValue("256")
    S2(256),

    /**
     * Phase S1 to S2.
     * XSD value: 768 (line 3316)
     */
    @XmlEnumValue("768")
    S12(768),

    /**
     * Phase S1, S2 to N.
     * XSD value: 769 (line 3322)
     */
    @XmlEnumValue("769")
    S12N_769(769),

    /**
     * Not applicable to any phase.
     * XSD value: 0 (line 3328)
     */
    @XmlEnumValue("0")
    NONE(0),

    /**
     * Phase A current relative to Phase A voltage.
     * XSD value: 136 (line 3334)
     */
    @XmlEnumValue("136")
    A_TO_AV(136);

    private final int value;

    PhaseCodeKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PhaseCodeKind fromValue(int value) {
        for (PhaseCodeKind kind : PhaseCodeKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid PhaseCodeKind value: " + value);
    }
}
