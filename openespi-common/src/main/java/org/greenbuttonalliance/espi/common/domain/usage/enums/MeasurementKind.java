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
 * Enumeration for MeasurementKind values.
 *
 * Name of physical measurement representing the type of measurement data being reported.
 * This enum includes various measurement types ranging from electrical quantities (power,
 * voltage, current) to reliability metrics (IEEE 1366), environmental measurements
 * (temperature), and other physical quantities (volume, distance, etc.).
 * Per ESPI 4.0 espi.xsd lines 2434-2865.
 */
@XmlType(name = "MeasurementKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum MeasurementKind {

    /**
     * Not Applicable.
     * XSD value: 0 (line 2441)
     */
    @XmlEnumValue("0")
    NONE(0),

    /**
     * Apparent Power Factor.
     * XSD value: 2 (line 2447)
     */
    @XmlEnumValue("2")
    APPARENT_POWER_FACTOR(2),

    /**
     * Currency.
     * XSD value: 3 (line 2453)
     */
    @XmlEnumValue("3")
    CURRENCY(3),

    /**
     * Current.
     * XSD value: 4 (line 2459)
     */
    @XmlEnumValue("4")
    CURRENT(4),

    /**
     * Current Angle.
     * XSD value: 5 (line 2465)
     */
    @XmlEnumValue("5")
    CURRENT_ANGLE(5),

    /**
     * Current Imbalance.
     * XSD value: 6 (line 2471)
     */
    @XmlEnumValue("6")
    CURRENT_IMBALANCE(6),

    /**
     * Date.
     * XSD value: 7 (line 2477)
     */
    @XmlEnumValue("7")
    DATE(7),

    /**
     * Demand.
     * XSD value: 8 (line 2483)
     */
    @XmlEnumValue("8")
    DEMAND(8),

    /**
     * Distance.
     * XSD value: 9 (line 2489)
     */
    @XmlEnumValue("9")
    DISTANCE(9),

    /**
     * Distortion Volt Amperes.
     * XSD value: 10 (line 2495)
     */
    @XmlEnumValue("10")
    DISTORTION_VOLT_AMPERES(10),

    /**
     * Energization.
     * XSD value: 11 (line 2501)
     */
    @XmlEnumValue("11")
    ENERGIZATION(11),

    /**
     * Energy.
     * XSD value: 12 (line 2507)
     */
    @XmlEnumValue("12")
    ENERGY(12),

    /**
     * Energization Load Side.
     * XSD value: 13 (line 2513)
     */
    @XmlEnumValue("13")
    ENERGIZATION_LOAD_SIDE(13),

    /**
     * Fan.
     * XSD value: 14 (line 2519)
     */
    @XmlEnumValue("14")
    FAN(14),

    /**
     * Frequency.
     * XSD value: 15 (line 2525)
     */
    @XmlEnumValue("15")
    FREQUENCY(15),

    /**
     * Funds (Duplication of "currency").
     * XSD value: 16 (line 2531)
     */
    @XmlEnumValue("16")
    FUNDS(16),

    /**
     * IEEE 1366 ASAI (Average Service Availability Index).
     * XSD value: 17 (line 2537)
     */
    @XmlEnumValue("17")
    IEEE1366_ASAI(17),

    /**
     * IEEE 1366 ASIDI (Average System Interruption Duration Index).
     * XSD value: 18 (line 2543)
     */
    @XmlEnumValue("18")
    IEEE1366_ASIDI(18),

    /**
     * IEEE 1366 ASIFI (Average System Interruption Frequency Index).
     * XSD value: 19 (line 2549)
     */
    @XmlEnumValue("19")
    IEEE1366_ASIFI(19),

    /**
     * IEEE 1366 CAIDI (Customer Average Interruption Duration Index).
     * XSD value: 20 (line 2555)
     */
    @XmlEnumValue("20")
    IEEE1366_CAIDI(20),

    /**
     * IEEE 1366 CAIFI (Customer Average Interruption Frequency Index).
     * XSD value: 21 (line 2561)
     */
    @XmlEnumValue("21")
    IEEE1366_CAIFI(21),

    /**
     * IEEE 1366 CEMIn (Customers Experiencing Multiple Interruptions).
     * XSD value: 22 (line 2567)
     */
    @XmlEnumValue("22")
    IEEE1366_CEMIN(22),

    /**
     * IEEE 1366 CEMSMIn (Customers Experiencing Multiple Sustained Interruptions and Momentary Interruption events).
     * XSD value: 23 (line 2573)
     */
    @XmlEnumValue("23")
    IEEE1366_CEMSMIN(23),

    /**
     * IEEE 1366 CTAIDI (Customer Total Average Interruption Duration Index).
     * XSD value: 24 (line 2579)
     */
    @XmlEnumValue("24")
    IEEE1366_CTAIDI(24),

    /**
     * IEEE 1366 MAIFI (Momentary Average Interruption Frequency Index).
     * XSD value: 25 (line 2585)
     */
    @XmlEnumValue("25")
    IEEE1366_MAIFI(25),

    /**
     * IEEE 1366 MAIFIe (Momentary Average Interruption Event Frequency Index).
     * XSD value: 26 (line 2591)
     */
    @XmlEnumValue("26")
    IEEE1366_MAIFIE(26),

    /**
     * IEEE 1366 SAIDI (System Average Interruption Duration Index).
     * XSD value: 27 (line 2597)
     */
    @XmlEnumValue("27")
    IEEE1366_SAIDI(27),

    /**
     * IEEE 1366 SAIFI (System Average Interruption Frequency Index).
     * XSD value: 28 (line 2603)
     */
    @XmlEnumValue("28")
    IEEE1366_SAIFI(28),

    /**
     * Line Losses.
     * XSD value: 31 (line 2609)
     */
    @XmlEnumValue("31")
    LINE_LOSSES(31),

    /**
     * Losses.
     * XSD value: 32 (line 2615)
     */
    @XmlEnumValue("32")
    LOSSES(32),

    /**
     * Negative Sequence.
     * XSD value: 33 (line 2621)
     */
    @XmlEnumValue("33")
    NEGATIVE_SEQUENCE(33),

    /**
     * Phasor Power Factor.
     * XSD value: 34 (line 2627)
     */
    @XmlEnumValue("34")
    PHASOR_POWER_FACTOR(34),

    /**
     * Phasor Reactive Power.
     * XSD value: 35 (line 2633)
     */
    @XmlEnumValue("35")
    PHASOR_REACTIVE_POWER(35),

    /**
     * Positive Sequence.
     * XSD value: 36 (line 2639)
     */
    @XmlEnumValue("36")
    POSITIVE_SEQUENCE(36),

    /**
     * Power.
     * XSD value: 37 (line 2645)
     */
    @XmlEnumValue("37")
    POWER(37),

    /**
     * Power Factor.
     * XSD value: 38 (line 2651)
     */
    @XmlEnumValue("38")
    POWER_FACTOR(38),

    /**
     * Quantity Power.
     * XSD value: 40 (line 2657)
     */
    @XmlEnumValue("40")
    QUANTITY_POWER(40),

    /**
     * Sag (Voltage Dip).
     * XSD value: 41 (line 2663)
     */
    @XmlEnumValue("41")
    SAG(41),

    /**
     * Swell.
     * XSD value: 42 (line 2669)
     */
    @XmlEnumValue("42")
    SWELL(42),

    /**
     * Switch Position.
     * XSD value: 43 (line 2675)
     */
    @XmlEnumValue("43")
    SWITCH_POSITION(43),

    /**
     * Tap Position.
     * XSD value: 44 (line 2681)
     */
    @XmlEnumValue("44")
    TAP_POSITION(44),

    /**
     * Tariff Rate.
     * XSD value: 45 (line 2687)
     */
    @XmlEnumValue("45")
    TARIFF_RATE(45),

    /**
     * Temperature.
     * XSD value: 46 (line 2693)
     */
    @XmlEnumValue("46")
    TEMPERATURE(46),

    /**
     * Total Harmonic Distortion.
     * XSD value: 47 (line 2699)
     */
    @XmlEnumValue("47")
    TOTAL_HARMONIC_DISTORTION(47),

    /**
     * Transformer Losses.
     * XSD value: 48 (line 2705)
     */
    @XmlEnumValue("48")
    TRANSFORMER_LOSSES(48),

    /**
     * Unipede Voltage Dip 10 to 15.
     * XSD value: 49 (line 2711)
     */
    @XmlEnumValue("49")
    UNIPEDE_VOLTAGE_DIP_10_TO_15(49),

    /**
     * Unipede Voltage Dip 15 to 30.
     * XSD value: 50 (line 2717)
     */
    @XmlEnumValue("50")
    UNIPEDE_VOLTAGE_DIP_15_TO_30(50),

    /**
     * Unipede Voltage Dip 30 to 60.
     * XSD value: 51 (line 2723)
     */
    @XmlEnumValue("51")
    UNIPEDE_VOLTAGE_DIP_30_TO_60(51),

    /**
     * Unipede Voltage Dip 60 to 90.
     * XSD value: 52 (line 2729)
     */
    @XmlEnumValue("52")
    UNIPEDE_VOLTAGE_DIP_60_TO_90(52),

    /**
     * Unipede Voltage Dip 90 to 100.
     * XSD value: 53 (line 2735)
     */
    @XmlEnumValue("53")
    UNIPEDE_VOLTAGE_DIP_90_TO_100(53),

    /**
     * Voltage.
     * XSD value: 54 (line 2741)
     */
    @XmlEnumValue("54")
    VOLTAGE(54),

    /**
     * Voltage Angle.
     * XSD value: 55 (line 2747)
     */
    @XmlEnumValue("55")
    VOLTAGE_ANGLE(55),

    /**
     * Voltage Excursion.
     * XSD value: 56 (line 2753)
     */
    @XmlEnumValue("56")
    VOLTAGE_EXCURSION(56),

    /**
     * Voltage Imbalance.
     * XSD value: 57 (line 2759)
     */
    @XmlEnumValue("57")
    VOLTAGE_IMBALANCE(57),

    /**
     * Volume (Clarified from Ed. 1. to indicate fluid volume).
     * XSD value: 58 (line 2765)
     */
    @XmlEnumValue("58")
    VOLUME(58),

    /**
     * Zero Flow Duration.
     * XSD value: 59 (line 2771)
     */
    @XmlEnumValue("59")
    ZERO_FLOW_DURATION(59),

    /**
     * Zero Sequence.
     * XSD value: 60 (line 2777)
     */
    @XmlEnumValue("60")
    ZERO_SEQUENCE(60),

    /**
     * Distortion Power Factor.
     * XSD value: 64 (line 2783)
     */
    @XmlEnumValue("64")
    DISTORTION_POWER_FACTOR(64),

    /**
     * Frequency Excursion (Usually expressed as a "count").
     * XSD value: 81 (line 2789)
     */
    @XmlEnumValue("81")
    FREQUENCY_EXCURSION(81),

    /**
     * Application Context.
     * XSD value: 90 (line 2795)
     */
    @XmlEnumValue("90")
    APPLICATION_CONTEXT(90),

    /**
     * Ap Title.
     * XSD value: 91 (line 2801)
     */
    @XmlEnumValue("91")
    AP_TITLE(91),

    /**
     * Asset Number.
     * XSD value: 92 (line 2807)
     */
    @XmlEnumValue("92")
    ASSET_NUMBER(92),

    /**
     * Bandwidth.
     * XSD value: 93 (line 2813)
     */
    @XmlEnumValue("93")
    BANDWIDTH(93),

    /**
     * Battery Voltage.
     * XSD value: 94 (line 2819)
     */
    @XmlEnumValue("94")
    BATTERY_VOLTAGE(94),

    /**
     * Broadcast Address.
     * XSD value: 95 (line 2825)
     */
    @XmlEnumValue("95")
    BROADCAST_ADDRESS(95),

    /**
     * Device Address Type 1.
     * XSD value: 96 (line 2831)
     */
    @XmlEnumValue("96")
    DEVICE_ADDRESS_TYPE_1(96),

    /**
     * Device Address Type 2.
     * XSD value: 97 (line 2837)
     */
    @XmlEnumValue("97")
    DEVICE_ADDRESS_TYPE_2(97),

    /**
     * Device Address Type 3.
     * XSD value: 98 (line 2843)
     */
    @XmlEnumValue("98")
    DEVICE_ADDRESS_TYPE_3(98),

    /**
     * Device Address Type 4.
     * XSD value: 99 (line 2849)
     */
    @XmlEnumValue("99")
    DEVICE_ADDRESS_TYPE_4(99),

    /**
     * Device Class.
     * XSD value: 100 (line 2855)
     */
    @XmlEnumValue("100")
    DEVICE_CLASS(100),

    /**
     * Electronic Serial Number.
     * XSD value: 101 (line 2861)
     */
    @XmlEnumValue("101")
    ELECTRONIC_SERIAL_NUMBER(101),

    /**
     * End Device ID.
     * XSD value: 102 (line 2867)
     */
    @XmlEnumValue("102")
    END_DEVICE_ID(102),

    /**
     * Group Address Type 1.
     * XSD value: 103 (line 2873)
     */
    @XmlEnumValue("103")
    GROUP_ADDRESS_TYPE_1(103),

    /**
     * Group Address Type 2.
     * XSD value: 104 (line 2879)
     */
    @XmlEnumValue("104")
    GROUP_ADDRESS_TYPE_2(104),

    /**
     * Group Address Type 3.
     * XSD value: 105 (line 2885)
     */
    @XmlEnumValue("105")
    GROUP_ADDRESS_TYPE_3(105),

    /**
     * Group Address Type 4.
     * XSD value: 106 (line 2891)
     */
    @XmlEnumValue("106")
    GROUP_ADDRESS_TYPE_4(106),

    /**
     * IP Address.
     * XSD value: 107 (line 2897)
     */
    @XmlEnumValue("107")
    IP_ADDRESS(107),

    /**
     * MAC Address.
     * XSD value: 108 (line 2903)
     */
    @XmlEnumValue("108")
    MAC_ADDRESS(108),

    /**
     * Mfg Assigned Configuration ID.
     * XSD value: 109 (line 2909)
     */
    @XmlEnumValue("109")
    MFG_ASSIGNED_CONFIGURATION_ID(109),

    /**
     * Mfg Assigned Physical Serial Number.
     * XSD value: 110 (line 2915)
     */
    @XmlEnumValue("110")
    MFG_ASSIGNED_PHYSICAL_SERIAL_NUMBER(110),

    /**
     * Mfg Assigned Product Number.
     * XSD value: 111 (line 2921)
     */
    @XmlEnumValue("111")
    MFG_ASSIGNED_PRODUCT_NUMBER(111),

    /**
     * Mfg Assigned Unique Communication Address.
     * XSD value: 112 (line 2927)
     */
    @XmlEnumValue("112")
    MFG_ASSIGNED_UNIQUE_COMMUNICATION_ADDRESS(112),

    /**
     * Multicast Address.
     * XSD value: 113 (line 2933)
     */
    @XmlEnumValue("113")
    MULTICAST_ADDRESS(113),

    /**
     * One Way Address.
     * XSD value: 114 (line 2939)
     */
    @XmlEnumValue("114")
    ONE_WAY_ADDRESS(114),

    /**
     * Signal Strength.
     * XSD value: 115 (line 2945)
     */
    @XmlEnumValue("115")
    SIGNAL_STRENGTH(115),

    /**
     * Two Way Address.
     * XSD value: 116 (line 2951)
     */
    @XmlEnumValue("116")
    TWO_WAY_ADDRESS(116),

    /**
     * Signal to Noise Ratio (moved here from Attribute #9 UOM).
     * XSD value: 117 (line 2957)
     */
    @XmlEnumValue("117")
    SIGNAL_TO_NOISE_RATIO(117),

    /**
     * Alarm.
     * XSD value: 118 (line 2963)
     */
    @XmlEnumValue("118")
    ALARM(118),

    /**
     * Battery Carryover.
     * XSD value: 119 (line 2969)
     */
    @XmlEnumValue("119")
    BATTERY_CARRYOVER(119),

    /**
     * Data Overflow Alarm.
     * XSD value: 120 (line 2975)
     */
    @XmlEnumValue("120")
    DATA_OVERFLOW_ALARM(120),

    /**
     * Demand Limit.
     * XSD value: 121 (line 2981)
     */
    @XmlEnumValue("121")
    DEMAND_LIMIT(121),

    /**
     * Demand Reset (usually expressed as a count as part of a billing cycle).
     * XSD value: 122 (line 2987)
     */
    @XmlEnumValue("122")
    DEMAND_RESET(122),

    /**
     * Diagnostic.
     * XSD value: 123 (line 2993)
     */
    @XmlEnumValue("123")
    DIAGNOSTIC(123),

    /**
     * Emergency Limit.
     * XSD value: 124 (line 2999)
     */
    @XmlEnumValue("124")
    EMERGENCY_LIMIT(124),

    /**
     * Encoder Tamper.
     * XSD value: 125 (line 3005)
     */
    @XmlEnumValue("125")
    ENCODER_TAMPER(125),

    /**
     * IEEE 1366 Momentary Interruption.
     * XSD value: 126 (line 3011)
     */
    @XmlEnumValue("126")
    IEEE1366_MOMENTARY_INTERRUPTION(126),

    /**
     * IEEE 1366 Momentary Interruption Event.
     * XSD value: 127 (line 3017)
     */
    @XmlEnumValue("127")
    IEEE1366_MOMENTARY_INTERRUPTION_EVENT(127),

    /**
     * IEEE 1366 Sustained Interruption.
     * XSD value: 128 (line 3023)
     */
    @XmlEnumValue("128")
    IEEE1366_SUSTAINED_INTERRUPTION(128),

    /**
     * Interruption Behaviour.
     * XSD value: 129 (line 3029)
     */
    @XmlEnumValue("129")
    INTERRUPTION_BEHAVIOUR(129),

    /**
     * Inversion Tamper.
     * XSD value: 130 (line 3035)
     */
    @XmlEnumValue("130")
    INVERSION_TAMPER(130),

    /**
     * Load Interrupt.
     * XSD value: 131 (line 3041)
     */
    @XmlEnumValue("131")
    LOAD_INTERRUPT(131),

    /**
     * Load Shed.
     * XSD value: 132 (line 3047)
     */
    @XmlEnumValue("132")
    LOAD_SHED(132),

    /**
     * Maintenance.
     * XSD value: 133 (line 3053)
     */
    @XmlEnumValue("133")
    MAINTENANCE(133),

    /**
     * Physical Tamper.
     * XSD value: 134 (line 3059)
     */
    @XmlEnumValue("134")
    PHYSICAL_TAMPER(134),

    /**
     * Power Loss Tamper.
     * XSD value: 135 (line 3065)
     */
    @XmlEnumValue("135")
    POWER_LOSS_TAMPER(135),

    /**
     * Power Outage.
     * XSD value: 136 (line 3071)
     */
    @XmlEnumValue("136")
    POWER_OUTAGE(136),

    /**
     * Power Quality.
     * XSD value: 137 (line 3077)
     */
    @XmlEnumValue("137")
    POWER_QUALITY(137),

    /**
     * Power Restoration.
     * XSD value: 138 (line 3083)
     */
    @XmlEnumValue("138")
    POWER_RESTORATION(138),

    /**
     * Programmed.
     * XSD value: 139 (line 3089)
     */
    @XmlEnumValue("139")
    PROGRAMMED(139),

    /**
     * Push Button.
     * XSD value: 140 (line 3095)
     */
    @XmlEnumValue("140")
    PUSHBUTTON(140),

    /**
     * Relay Activation.
     * XSD value: 141 (line 3101)
     */
    @XmlEnumValue("141")
    RELAY_ACTIVATION(141),

    /**
     * Relay Cycle (usually expressed as a count).
     * XSD value: 142 (line 3107)
     */
    @XmlEnumValue("142")
    RELAY_CYCLE(142),

    /**
     * Removal Tamper.
     * XSD value: 143 (line 3113)
     */
    @XmlEnumValue("143")
    REMOVAL_TAMPER(143),

    /**
     * Reprogramming Tamper.
     * XSD value: 144 (line 3119)
     */
    @XmlEnumValue("144")
    REPROGRAMMING_TAMPER(144),

    /**
     * Reverse Rotation Tamper.
     * XSD value: 145 (line 3125)
     */
    @XmlEnumValue("145")
    REVERSE_ROTATION_TAMPER(145),

    /**
     * Switch Armed.
     * XSD value: 146 (line 3131)
     */
    @XmlEnumValue("146")
    SWITCH_ARMED(146),

    /**
     * Switch Disabled.
     * XSD value: 147 (line 3137)
     */
    @XmlEnumValue("147")
    SWITCH_DISABLED(147),

    /**
     * Tamper.
     * XSD value: 148 (line 3143)
     */
    @XmlEnumValue("148")
    TAMPER(148),

    /**
     * Watchdog Timeout.
     * XSD value: 149 (line 3149)
     */
    @XmlEnumValue("149")
    WATCHDOG_TIMEOUT(149),

    /**
     * Customer's bill for the previous billing period (Currency).
     * XSD value: 150 (line 3155)
     */
    @XmlEnumValue("150")
    BILL_LAST_PERIOD(150),

    /**
     * Customer's bill, as known thus far within the present billing period (Currency).
     * XSD value: 151 (line 3161)
     */
    @XmlEnumValue("151")
    BILL_TO_DATE(151),

    /**
     * Customer's bill for the (Currency).
     * XSD value: 152 (line 3167)
     */
    @XmlEnumValue("152")
    BILL_CARRYOVER(152),

    /**
     * Monthly fee for connection to commodity.
     * XSD value: 153 (line 3173)
     */
    @XmlEnumValue("153")
    CONNECTION_FEE(153),

    /**
     * Audible Volume (Sound).
     * XSD value: 154 (line 3179)
     */
    @XmlEnumValue("154")
    AUDIBLE_VOLUME(154),

    /**
     * Volumetric Flow.
     * XSD value: 155 (line 3185)
     */
    @XmlEnumValue("155")
    VOLUMETRIC_FLOW(155);

    private final int value;

    MeasurementKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MeasurementKind fromValue(int value) {
        for (MeasurementKind kind : MeasurementKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid MeasurementKind value: " + value);
    }
}
