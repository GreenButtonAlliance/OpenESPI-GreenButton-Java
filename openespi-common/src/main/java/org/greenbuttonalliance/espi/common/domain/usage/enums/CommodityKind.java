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
 * Enumeration for CommodityKind values.
 *
 * Code for commodity classification of Readings of ReadingType.
 * The valid values per the standard are defined in CommodityType.
 * Per ESPI 4.0 espi.xsd lines 1928-2100.
 */
@XmlType(name = "CommodityKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum CommodityKind {

    /**
     * Not Applicable.
     * XSD value: 0 (line 1935)
     */
    @XmlEnumValue("0")
    NONE(0),

    /**
     * All types of metered quantities. This type of reading comes from the meter
     * and represents a "secondary" metered value.
     * XSD value: 1 (line 1941)
     */
    @XmlEnumValue("1")
    ELECTRICITY_SECONDARY_METERED(1),

    /**
     * It is possible for a meter to be outfitted with an external VT and/or CT.
     * The meter might not be aware of these devices, and the display not compensate
     * for their presence. Ultimately, when these scalars are applied, the value that
     * represents the service value is called the "primary metered" value. The "index"
     * in sub-category 3 mirrors those of sub-category 0.
     * XSD value: 2 (line 1947)
     */
    @XmlEnumValue("2")
    ELECTRICITY_PRIMARY_METERED(2),

    /**
     * A measurement of the communication infrastructure itself.
     * XSD value: 3 (line 1953)
     */
    @XmlEnumValue("3")
    COMMUNICATION(3),

    /**
     * Air.
     * XSD value: 4 (line 1959)
     */
    @XmlEnumValue("4")
    AIR(4),

    /**
     * Insulative Gas (SF6 is found separately below).
     * XSD value: 5 (line 1965)
     */
    @XmlEnumValue("5")
    INSULATIVE_GAS(5),

    /**
     * Insulative Oil.
     * XSD value: 6 (line 1971)
     */
    @XmlEnumValue("6")
    INSULATIVE_OIL(6),

    /**
     * Natural Gas.
     * XSD value: 7 (line 1977)
     */
    @XmlEnumValue("7")
    NATURAL_GAS(7),

    /**
     * Propane C3H8.
     * XSD value: 8 (line 1983)
     */
    @XmlEnumValue("8")
    PROPANE(8),

    /**
     * Drinkable water.
     * XSD value: 9 (line 1989)
     */
    @XmlEnumValue("9")
    POTABLE_WATER(9),

    /**
     * Water in steam form, usually used for heating.
     * XSD value: 10 (line 1995)
     */
    @XmlEnumValue("10")
    STEAM(10),

    /**
     * Waste Water (Sewerage).
     * XSD value: 11 (line 2001)
     */
    @XmlEnumValue("11")
    WASTE_WATER(11),

    /**
     * This fluid is likely in liquid form. It is not necessarily water or water based.
     * The warm fluid returns cooler than when it was sent. The heat conveyed may be metered.
     * XSD value: 12 (line 2007)
     */
    @XmlEnumValue("12")
    HEATING_FLUID(12),

    /**
     * The cool fluid returns warmer than when it was sent. The heat conveyed may be metered.
     * XSD value: 13 (line 2013)
     */
    @XmlEnumValue("13")
    COOLING_FLUID(13),

    /**
     * Reclaimed water – possibly used for irrigation but not sufficiently treated to be
     * considered safe for drinking.
     * XSD value: 14 (line 2019)
     */
    @XmlEnumValue("14")
    NONPOTABLE_WATER(14),

    /**
     * Nitrous Oxides NOX.
     * XSD value: 15 (line 2025)
     */
    @XmlEnumValue("15")
    NOX(15),

    /**
     * Sulfur Dioxide SO2.
     * XSD value: 16 (line 2031)
     */
    @XmlEnumValue("16")
    SO2(16),

    /**
     * Methane CH4.
     * XSD value: 17 (line 2037)
     */
    @XmlEnumValue("17")
    CH4(17),

    /**
     * Carbon Dioxide CO2.
     * XSD value: 18 (line 2043)
     */
    @XmlEnumValue("18")
    CO2(18),

    /**
     * Carbon.
     * XSD value: 19 (line 2049)
     */
    @XmlEnumValue("19")
    CARBON(19),

    /**
     * Hexachlorocyclohexane HCH.
     * XSD value: 20 (line 2055)
     */
    @XmlEnumValue("20")
    HCH(20),

    /**
     * Perfluorocarbons PFC.
     * XSD value: 21 (line 2061)
     */
    @XmlEnumValue("21")
    PFC(21),

    /**
     * Sulfurhexafluoride SF6.
     * XSD value: 22 (line 2067)
     */
    @XmlEnumValue("22")
    SF6(22),

    /**
     * Television.
     * XSD value: 23 (line 2073)
     */
    @XmlEnumValue("23")
    TV_LICENCE(23),

    /**
     * Internet service.
     * XSD value: 24 (line 2079)
     */
    @XmlEnumValue("24")
    INTERNET(24),

    /**
     * Trash.
     * XSD value: 25 (line 2085)
     */
    @XmlEnumValue("25")
    REFUSE(25),

    /**
     * Electricity Transmission "metered" -- Service taken directly from transmission
     * system without transformation from standard transmission voltages.
     * XSD value: 26 (line 2091)
     */
    @XmlEnumValue("26")
    ELECTRICITY_TRANSMISSION_METERED(26);

    private final int value;

    CommodityKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CommodityKind fromValue(int value) {
        for (CommodityKind kind : CommodityKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid CommodityKind value: " + value);
    }
}