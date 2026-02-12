/*
 * Copyright (c) 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.common.domain.common.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Enumeration for Currency values.
 *
 * Code for the currency for costs associated with this ReadingType.
 * The valid values per the standard are defined in CurrencyCode.
 * Per ESPI 4.0 espi.xsd lines 2101-2195.
 */
@XmlType(name = "Currency", namespace = "http://naesb.org/espi")
@XmlEnum
public enum Currency {

    /**
     * US dollar.
     * XSD value: 840 (line 2108)
     */
    @XmlEnumValue("840")
    USD(840),

    /**
     * European euro.
     * XSD value: 978 (line 2114)
     */
    @XmlEnumValue("978")
    EUR(978),

    /**
     * Australian dollar.
     * XSD value: 36 (line 2120)
     */
    @XmlEnumValue("36")
    AUD(36),

    /**
     * Canadian dollar.
     * XSD value: 124 (line 2126)
     */
    @XmlEnumValue("124")
    CAD(124),

    /**
     * Swiss francs.
     * XSD value: 756 (line 2132)
     */
    @XmlEnumValue("756")
    CHF(756),

    /**
     * Chinese yuan renminbi.
     * XSD value: 156 (line 2138)
     */
    @XmlEnumValue("156")
    CNY(156),

    /**
     * Danish crown.
     * XSD value: 208 (line 2144)
     */
    @XmlEnumValue("208")
    DKK(208),

    /**
     * British pound.
     * XSD value: 826 (line 2150)
     */
    @XmlEnumValue("826")
    GBP(826),

    /**
     * Japanese yen.
     * XSD value: 392 (line 2156)
     */
    @XmlEnumValue("392")
    JPY(392),

    /**
     * Norwegian crown.
     * XSD value: 578 (line 2162)
     */
    @XmlEnumValue("578")
    NOK(578),

    /**
     * Russian ruble.
     * XSD value: 643 (line 2168)
     */
    @XmlEnumValue("643")
    RUB(643),

    /**
     * Swedish crown.
     * XSD value: 752 (line 2174)
     */
    @XmlEnumValue("752")
    SEK(752),

    /**
     * India rupees.
     * XSD value: 356 (line 2180)
     */
    @XmlEnumValue("356")
    INR(356),

    /**
     * Another type of currency.
     * XSD value: 0 (line 2186)
     */
    @XmlEnumValue("0")
    OTHER(0);

    private final int value;

    Currency(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Currency fromValue(int value) {
        for (Currency kind : Currency.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid Currency value: " + value);
    }
}