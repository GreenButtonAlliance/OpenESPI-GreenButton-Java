/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.common.domain.usage.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Enumeration for UnitSymbolKind values.
 *
 * Code for the base unit of measure for Readings of ReadingType. Used in combination with the
 * powerOfTenMultiplier to specify the actual unit of measure. Valid values per the standard are
 * defined in UomType.
 * Per ESPI 4.0 espi.xsd lines 3934-4695.
 */
@XmlType(name = "UnitSymbolKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum UnitSymbolKind {

	/**
	 * N/A, None.
	 * XSD value: 0 (line 4062)
	 */
	@XmlEnumValue("0")
	NONE(0),

	/**
	 * Length, meter, m.
	 * XSD value: 2 (line 4086)
	 */
	@XmlEnumValue("2")
	M(2),

	/**
	 * Mass in gram, g.
	 * XSD value: 3 (line 4074)
	 */
	@XmlEnumValue("3")
	G(3),

	/**
	 * Rotational speed, Rotations per second, rev/s.
	 * XSD value: 4 (line 4548)
	 */
	@XmlEnumValue("4")
	REV_PER_SEC(4),

	/**
	 * Current, ampere, A.
	 * XSD value: 5 (line 3990)
	 */
	@XmlEnumValue("5")
	A(5),

	/**
	 * Temperature, Kelvin, K.
	 * XSD value: 6 (line 4284)
	 */
	@XmlEnumValue("6")
	K(6),

	/**
	 * Amount of substance, mole, mol.
	 * XSD value: 7 (line 4416)
	 */
	@XmlEnumValue("7")
	MOL(7),

	/**
	 * Luminous intensity, candela, cd.
	 * XSD value: 8 (line 4176)
	 */
	@XmlEnumValue("8")
	CD(8),

	/**
	 * Plane angle, degrees, deg.
	 * XSD value: 9 (line 4032)
	 */
	@XmlEnumValue("9")
	DEG(9),

	/**
	 * Plane angle, Radian (m/m), rad.
	 * XSD value: 10 (line 4038)
	 */
	@XmlEnumValue("10")
	RAD(10),

	/**
	 * Solid angle, Steradian (m2/m2), sr.
	 * XSD value: 11 (line 4560)
	 */
	@XmlEnumValue("11")
	SR(11),

	/**
	 * Radioactivity, Becquerel (1/s), Bq.
	 * XSD value: 22 (line 4158)
	 */
	@XmlEnumValue("22")
	BQ(22),

	/**
	 * Relative temperature in degrees Celsius. In the SI unit system the symbol is ºC. Electric charge is measured in coulomb that has the unit symbol C. To distinguish degree Celsius from coulomb the symbol used in the UML is degC. Reason for not using ºC is the special character º is difficult to manage in software.
	 * XSD value: 23 (line 4008)
	 */
	@XmlEnumValue("23")
	DEG_C(23),

	/**
	 * Doe equivalent, Sievert (J/kg), Sv.
	 * XSD value: 24 (line 4572)
	 */
	@XmlEnumValue("24")
	SV(24),

	/**
	 * Electric capacitance, Farad (C/V), °C.
	 * XSD value: 25 (line 3996)
	 */
	@XmlEnumValue("25")
	F(25),

	/**
	 * Time, seconds, s.
	 * XSD value: 27 (line 4014)
	 */
	@XmlEnumValue("27")
	SEC(27),

	/**
	 * Electric inductance, Henry (Wb/A), H.
	 * XSD value: 28 (line 4002)
	 */
	@XmlEnumValue("28")
	H_HENRY(28),

	/**
	 * Electric potential, Volt (W/A), V.
	 * XSD value: 29 (line 3978)
	 */
	@XmlEnumValue("29")
	V(29),

	/**
	 * Electric resistance, Ohm (V/A), O.
	 * XSD value: 30 (line 3984)
	 */
	@XmlEnumValue("30")
	OHM(30),

	/**
	 * Energy joule, (N·m = C·V = W·s), J.
	 * XSD value: 31 (line 4044)
	 */
	@XmlEnumValue("31")
	J(31),

	/**
	 * Force newton, (kg m/s²), N.
	 * XSD value: 32 (line 4050)
	 */
	@XmlEnumValue("32")
	N(32),

	/**
	 * Frequency hertz, (1/s), Hz.
	 * XSD value: 33 (line 4068)
	 */
	@XmlEnumValue("33")
	HZ(33),

	/**
	 * Illuminance lux, (lm/m²), L(uncompensated)/h.
	 * XSD value: 34 (line 4362)
	 */
	@XmlEnumValue("34")
	LX(34),

	/**
	 * Luminous flux, lumen (cd sr), Lm.
	 * XSD value: 35 (line 4356)
	 */
	@XmlEnumValue("35")
	LM(35),

	/**
	 * Magnetic flux, Weber (V s), Wb.
	 * XSD value: 36 (line 4650)
	 */
	@XmlEnumValue("36")
	WB(36),

	/**
	 * Magnetic flux density, Tesla (Wb/m2), T.
	 * XSD value: 37 (line 4578)
	 */
	@XmlEnumValue("37")
	T(37),

	/**
	 * Real power, Watt. By definition, one Watt equals one Joule per second. Electrical power may have real and reactive components. The real portion of electrical power (I²R) or VIcos?, is expressed in Watts. (See also apparent power and reactive power.), W.
	 * XSD value: 38 (line 3948)
	 */
	@XmlEnumValue("38")
	W(38),

	/**
	 * Pressure, Pascal (N/m²)(Note: the absolute or relative measurement of pressure is implied with this entry. See below for more explicit forms.), Pa.
	 * XSD value: 39 (line 4080)
	 */
	@XmlEnumValue("39")
	PA(39),

	/**
	 * Area, square meter, m².
	 * XSD value: 41 (line 4092)
	 */
	@XmlEnumValue("41")
	M2(41),

	/**
	 * Volume, cubic meter, m³.
	 * XSD value: 42 (line 4098)
	 */
	@XmlEnumValue("42")
	M3(42),

	/**
	 * Velocity, meters per second (m/s), m/s.
	 * XSD value: 43 (line 4458)
	 */
	@XmlEnumValue("43")
	M_PER_SEC(43),

	/**
	 * Acceleration, meters per second squared, m/s².
	 * XSD value: 44 (line 4464)
	 */
	@XmlEnumValue("44")
	M_PER_SEC2(44),

	/**
	 * m3PerSec, cubic meters per second, m³/s.
	 * XSD value: 45 (line 4392)
	 */
	@XmlEnumValue("45")
	M3_PER_SEC(45),

	/**
	 * Fuel efficiency, meters / cubic meter, m/m³.
	 * XSD value: 46 (line 4452)
	 */
	@XmlEnumValue("46")
	M_PER_M3(46),

	/**
	 * Moment of mass ,kilogram meter (kg·m), M.
	 * XSD value: 47 (line 4296)
	 */
	@XmlEnumValue("47")
	KG_M(47),

	/**
	 * Density, gram/cubic meter (combine with prefix multiplier "k" to form kg/ m³), g/m³.
	 * XSD value: 48 (line 4302)
	 */
	@XmlEnumValue("48")
	KG_PER_M3(48),

	/**
	 * Viscosity, meter squared / second, m²/s.
	 * XSD value: 49 (line 4368)
	 */
	@XmlEnumValue("49")
	M2_PER_SEC(49),

	/**
	 * Thermal conductivity, Watt/meter Kelvin, W/m K.
	 * XSD value: 50 (line 4668)
	 */
	@XmlEnumValue("50")
	W_PER_M_K(50),

	/**
	 * Heat capacity, Joule/Kelvin, J/K.
	 * XSD value: 51 (line 4272)
	 */
	@XmlEnumValue("51")
	J_PER_K(51),

	/**
	 * Electric conductance, Siemens (A / V = 1 / O), S.
	 * XSD value: 53 (line 4056)
	 */
	@XmlEnumValue("53")
	SIEMENS(53),

	/**
	 * Angular velocity, radians per second, rad/s.
	 * XSD value: 54 (line 4536)
	 */
	@XmlEnumValue("54")
	RAD_PER_SEC(54),

	/**
	 * Apparent power, Volt Ampere (See also real power and reactive power.), VA.
	 * XSD value: 61 (line 3942)
	 */
	@XmlEnumValue("61")
	VA(61),

	/**
	 * Reactive power, Volt Ampere reactive. The "reactive" or "imaginary" component of electrical power (VISin?). (See also real power and apparent power)., VAr.
	 * XSD value: 63 (line 3954)
	 */
	@XmlEnumValue("63")
	VAR(63),

	/**
	 * Power factor, Dimensionless, cos?.
	 * XSD value: 65 (line 4200)
	 */
	@XmlEnumValue("65")
	COS_THETA(65),

	/**
	 * Volt seconds, Volt seconds (Ws/A), Vs.
	 * XSD value: 66 (line 4644)
	 */
	@XmlEnumValue("66")
	VS(66),

	/**
	 * Volts squared, Volt squared (W2/A2), V².
	 * XSD value: 67 (line 4608)
	 */
	@XmlEnumValue("67")
	V2(67),

	/**
	 * Amp seconds, amp seconds, As.
	 * XSD value: 68 (line 4140)
	 */
	@XmlEnumValue("68")
	AS(68),

	/**
	 * Amps squared, amp squared, A2.
	 * XSD value: 69 (line 4104)
	 */
	@XmlEnumValue("69")
	A2(69),

	/**
	 * Amps squared time, square amp second, A²s.
	 * XSD value: 70 (line 4116)
	 */
	@XmlEnumValue("70")
	A2S(70),

	/**
	 * Apparent energy, Volt Ampere hours, VAh.
	 * XSD value: 71 (line 3960)
	 */
	@XmlEnumValue("71")
	VAH(71),

	/**
	 * Real energy, Watt hours, Wh.
	 * XSD value: 72 (line 3966)
	 */
	@XmlEnumValue("72")
	WH(72),

	/**
	 * Reactive energy, Volt Ampere reactive hours, VArh.
	 * XSD value: 73 (line 3972)
	 */
	@XmlEnumValue("73")
	VARH(73),

	/**
	 * Magnetic flux, Volts per Hertz, V/Hz.
	 * XSD value: 74 (line 4632)
	 */
	@XmlEnumValue("74")
	V_PER_HZ(74),

	/**
	 * Rate of change of frequency, hertz per second, Hz/s.
	 * XSD value: 75 (line 4188)
	 */
	@XmlEnumValue("75")
	HZ_PER_SEC(75),

	/**
	 * Number of characters, characters, char.
	 * XSD value: 76 (line 4182)
	 */
	@XmlEnumValue("76")
	CHAR(76),

	/**
	 * Data rate, characters per second, char/s.
	 * XSD value: 77 (line 4254)
	 */
	@XmlEnumValue("77")
	CHAR_PER_SEC(77),

	/**
	 * Turbine inertia, gram·meter2 (Combine with multiplier prefix "k" to form kg·m2.), gm².
	 * XSD value: 78 (line 4230)
	 */
	@XmlEnumValue("78")
	GM2(78),

	/**
	 * Sound pressure level, Bel, acoustic, Combine with multiplier prefix "d" to form decibels of Sound Pressure Level"dB (SPL).", B (SPL).
	 * XSD value: 79 (line 4146)
	 */
	@XmlEnumValue("79")
	B(79),

	/**
	 * Monetary unit, Generic money (Note: Specific monetary units are identified the currency class)., ¤.
	 * XSD value: 80 (line 4440)
	 */
	@XmlEnumValue("80")
	MONEY(80),

	/**
	 * Ramp rate, Watts per second, W/s.
	 * XSD value: 81 (line 4674)
	 */
	@XmlEnumValue("81")
	W_PER_SEC(81),

	/**
	 * Volumetric flow rate, Volumetric flow rate, L/s.
	 * XSD value: 82 (line 4338)
	 */
	@XmlEnumValue("82")
	LITRE_PER_SEC(82),

	/**
	 * Quantity power, Q, Q.
	 * XSD value: 100 (line 4500)
	 */
	@XmlEnumValue("100")
	Q(100),

	/**
	 * Quantity energy, Qh, Qh.
	 * XSD value: 101 (line 4530)
	 */
	@XmlEnumValue("101")
	QH(101),

	/**
	 * resistivity, ? (rho), ?m.
	 * XSD value: 102 (line 4470)
	 */
	@XmlEnumValue("102")
	OHM_M(102),

	/**
	 * A/m, magnetic field strength, Ampere per metre, A/m.
	 * XSD value: 103 (line 4134)
	 */
	@XmlEnumValue("103")
	A_PER_M(103),

	/**
	 * volt-squared hour, Volt-squared-hours, V²h.
	 * XSD value: 104 (line 4614)
	 */
	@XmlEnumValue("104")
	V2H(104),

	/**
	 * ampere-squared, Ampere-squared hour, A²h.
	 * XSD value: 105 (line 4110)
	 */
	@XmlEnumValue("105")
	A2H(105),

	/**
	 * Ampere-hours, Ampere-hours, Ah.
	 * XSD value: 106 (line 4122)
	 */
	@XmlEnumValue("106")
	AH(106),

	/**
	 * Wh/m3, energy per volume, Wh/m³.
	 * XSD value: 107 (line 4656)
	 */
	@XmlEnumValue("107")
	WH_PER_M3(107),

	/**
	 * Timestamp, time and date per ISO 8601 format, timeStamp.
	 * XSD value: 108 (line 4590)
	 */
	@XmlEnumValue("108")
	TIME_STAMP(108),

	/**
	 * State, "1" = "true", "live", "on", "high", "set"; "0" = "false", "dead", "off", "low", "cleared". Note: A Boolean value is preferred but other values may be supported, status.
	 * XSD value: 109 (line 4566)
	 */
	@XmlEnumValue("109")
	STATUS(109),

	/**
	 * Amount of substance, counter value, count.
	 * XSD value: 111 (line 4206)
	 */
	@XmlEnumValue("111")
	COUNT(111),

	/**
	 * Signal Strength, Bel-mW, normalized to 1mW. Note: to form "dBm" combine "Bm" with multiplier "d". Bm.
	 * XSD value: 113 (line 4152)
	 */
	@XmlEnumValue("113")
	BM(113),

	/**
	 * Application Value, encoded value, code.
	 * XSD value: 114 (line 4194)
	 */
	@XmlEnumValue("114")
	CODE(114),

	/**
	 * Kh-Wh, active energy metering constant, Wh/rev.
	 * XSD value: 115 (line 4662)
	 */
	@XmlEnumValue("115")
	WH_PER_REV(115),

	/**
	 * Kh-VArh, reactive energy metering constant, VArh/rev.
	 * XSD value: 116 (line 4626)
	 */
	@XmlEnumValue("116")
	VARH_PER_REV(116),

	/**
	 * Kh-Vah, apparent energy metering constant, VAh/rev.
	 * XSD value: 117 (line 4620)
	 */
	@XmlEnumValue("117")
	VAH_PER_REV(117),

	/**
	 * EndDeviceEvent, value to be interpreted as a EndDeviceEventCode, meCode.
	 * XSD value: 118 (line 4410)
	 */
	@XmlEnumValue("118")
	ME_CODE(118),

	/**
	 * Volume, cubic feet, ft³.
	 * XSD value: 119 (line 4212)
	 */
	@XmlEnumValue("119")
	FT3(119),

	/**
	 * Volume, cubic feet, ft³(compensated).
	 * XSD value: 120 (line 4218)
	 */
	@XmlEnumValue("120")
	FT3_COMPENSATED(120),

	/**
	 * Volumetric flow rate, compensated cubic feet per hour, ft³(compensated)/h.
	 * XSD value: 123 (line 4224)
	 */
	@XmlEnumValue("123")
	FT3_COMPENSATED_PER_H(123),

	/**
	 * Volumetric flow rate, cubic meters per hour, m³/h.
	 * XSD value: 125 (line 4386)
	 */
	@XmlEnumValue("125")
	M3_PER_H(125),

	/**
	 * Volumetric flow rate, compensated cubic meters per hour, ³(compensated)/h.
	 * XSD value: 126 (line 4380)
	 */
	@XmlEnumValue("126")
	M3_COMPENSATED_PER_H(126),

	/**
	 * Volumetric flow rate, uncompensated cubic meters per hour, m³(uncompensated)/h.
	 * XSD value: 127 (line 4404)
	 */
	@XmlEnumValue("127")
	M3_UNCOMPENSATED_PER_H(127),

	/**
	 * Volume, US gallons, Gal.
	 * XSD value: 128 (line 4596)
	 */
	@XmlEnumValue("128")
	US_GAL(128),

	/**
	 * Volumetric flow rate, US gallons per hour, USGal/h.
	 * XSD value: 129 (line 4602)
	 */
	@XmlEnumValue("129")
	US_GAL_PER_H(129),

	/**
	 * Volume, imperial gallons, ImperialGal.
	 * XSD value: 130 (line 4260)
	 */
	@XmlEnumValue("130")
	IMPERIAL_GAL(130),

	/**
	 * Volumetric flow rate, Imperial gallons per hour, ImperialGal/h.
	 * XSD value: 131 (line 4266)
	 */
	@XmlEnumValue("131")
	IMPERIAL_GAL_PER_H(131),

	/**
	 * Energy, British Thermal Units, BTU.
	 * XSD value: 132 (line 4164)
	 */
	@XmlEnumValue("132")
	BTU(132),

	/**
	 * Power, BTU per hour, BTU/h.
	 * XSD value: 133 (line 4170)
	 */
	@XmlEnumValue("133")
	BTU_PER_H(133),

	/**
	 * Volume, litre = dm3 = m3/1000., L.
	 * XSD value: 134 (line 4308)
	 */
	@XmlEnumValue("134")
	LITRE(134),

	/**
	 * Volumetric flow rate, litres per hour, L/h.
	 * XSD value: 137 (line 4326)
	 */
	@XmlEnumValue("137")
	LITRE_PER_H(137),

	/**
	 * Volumetric flow rate, litres (compensated) per hour, L(compensated)/h.
	 * XSD value: 138 (line 4320)
	 */
	@XmlEnumValue("138")
	LITRE_COMPENSATED_PER_H(138),

	/**
	 * Volumetric flow rate, litres (uncompensated) per hour, L(uncompensated)/h.
	 * XSD value: 139 (line 4350)
	 */
	@XmlEnumValue("139")
	LITRE_UNCOMPENSATED_PER_H(139),

	/**
	 * Pressure, Pascal, gauge pressure, PaG.
	 * XSD value: 140 (line 4482)
	 */
	@XmlEnumValue("140")
	PA_G(140),

	/**
	 * Pressure, Pounds per square inch, absolute, psiA.
	 * XSD value: 141 (line 4488)
	 */
	@XmlEnumValue("141")
	PSI_A(141),

	/**
	 * Pressure, Pounds per square inch, gauge, psiG.
	 * XSD value: 142 (line 4494)
	 */
	@XmlEnumValue("142")
	PSI_G(142),

	/**
	 * Concentration, The ratio of the volume of a solute divided by the volume of the solution., L/L.
	 * XSD value: 143 (line 4332)
	 */
	@XmlEnumValue("143")
	LITRE_PER_LITRE(143),

	/**
	 * Concentration, The ratio of the mass of a solute divided by the mass of the solution., g/g.
	 * XSD value: 144 (line 4236)
	 */
	@XmlEnumValue("144")
	G_PER_G(144),

	/**
	 * Concentration, The amount of substance concentration, (c), the amount of solvent in moles divided by the volume of solution in m³., mol/ m³.
	 * XSD value: 145 (line 4428)
	 */
	@XmlEnumValue("145")
	MOL_PER_M3(145),

	/**
	 * Concentration, Molar fraction (?), the ratio of the molar amount of a solute divided by the molar amount of the solution.,mol/mol.
	 * XSD value: 146 (line 4434)
	 */
	@XmlEnumValue("146")
	MOL_PER_MOL(146),

	/**
	 * Concentration, Molality, the amount of solute in moles and the amount of solvent in kilograms., mol/kg.
	 * XSD value: 147 (line 4422)
	 */
	@XmlEnumValue("147")
	MOL_PER_KG(147),

	/**
	 * Length, Ratio of length, m/m.
	 * XSD value: 148 (line 4446)
	 */
	@XmlEnumValue("148")
	M_PER_M(148),

	/**
	 * Time, Ratio of time (can be combined with an multiplier prefix to show rates such as a clock drift rate, e.g. "µs/s"), s/s.
	 * XSD value: 149 (line 4554)
	 */
	@XmlEnumValue("149")
	SEC_PER_SEC(149),

	/**
	 * Frequency, Rate of frequency change, Hz/Hz.
	 * XSD value: 150 (line 4248)
	 */
	@XmlEnumValue("150")
	HZ_PER_HZ(150),

	/**
	 * Voltage, Ratio of voltages (e.g. mV/V), V/V.
	 * XSD value: 151 (line 4638)
	 */
	@XmlEnumValue("151")
	V_PER_V(151),

	/**
	 * Current, Ratio of Amperages, A/A.
	 * XSD value: 152 (line 4128)
	 */
	@XmlEnumValue("152")
	A_PER_A(152),

	/**
	 * Power Factor, PF, W/VA.
	 * XSD value: 153 (line 4680)
	 */
	@XmlEnumValue("153")
	W_PER_VA(153),

	/**
	 * Amount of rotation, Revolutions, rev.
	 * XSD value: 154 (line 4542)
	 */
	@XmlEnumValue("154")
	REV(154),

	/**
	 * Pressure, Pascal, absolute pressure, PaA.
	 * XSD value: 155 (line 4476)
	 */
	@XmlEnumValue("155")
	PA_A(155),

	/**
	 * Volume, litre, with the value uncompensated for weather effects., L(uncompensated).
	 * XSD value: 156 (line 4344)
	 */
	@XmlEnumValue("156")
	LITRE_UNCOMPENSATED(156),

	/**
	 * Volume, litre, with the value compensated for weather effects, L(compensated).
	 * XSD value: 157 (line 4314)
	 */
	@XmlEnumValue("157")
	LITRE_COMPENSATED(157),

	/**
	 * Catalytic activity, katal = mol / s, kat.
	 * XSD value: 158 (line 4290)
	 */
	@XmlEnumValue("158")
	KAT(158),

	/**
	 * Time, minute = s * 60, min.
	 * XSD value: 159 (line 4020)
	 */
	@XmlEnumValue("159")
	MIN(159),

	/**
	 * Time, hour = minute * 60, h.
	 * XSD value: 160 (line 4026)
	 */
	@XmlEnumValue("160")
	H_HOUR(160),

	/**
	 * Quantity power, Q measured at 45º, Q45.
	 * XSD value: 161 (line 4506)
	 */
	@XmlEnumValue("161")
	Q45(161),

	/**
	 * Quantity power, Q measured at 60º, Q60.
	 * XSD value: 162 (line 4518)
	 */
	@XmlEnumValue("162")
	Q60(162),

	/**
	 * Quantity energy, Q measured at 45º, Q45h.
	 * XSD value: 163 (line 4512)
	 */
	@XmlEnumValue("163")
	Q45H(163),

	/**
	 * Quantity energy, Qh measured at 60º, Q60h.
	 * XSD value: 164 (line 4524)
	 */
	@XmlEnumValue("164")
	Q60H(164),

	/**
	 * Specific energy, Joules / kg, J/kg.
	 * XSD value: 165 (line 4278)
	 */
	@XmlEnumValue("165")
	J_PER_KG(165),

	/**
	 * m3uncompensated, cubic meter, with the value uncompensated for weather effects., m3(uncompensated).
	 * XSD value: 166 (line 4398)
	 */
	@XmlEnumValue("166")
	M3_UNCOMPENSATED(166),

	/**
	 * Volume, cubic meter, with the value compensated for weather effects., m3(compensated).
	 * XSD value: 167 (line 4374)
	 */
	@XmlEnumValue("167")
	M3_COMPENSATED(167),

	/**
	 * Signal Strength, Ratio of power, W/W.
	 * XSD value: 168 (line 4686)
	 */
	@XmlEnumValue("168")
	W_PER_W(168),

	/**
	 * Energy, Therm, therm.
	 * XSD value: 169 (line 4584)
	 */
	@XmlEnumValue("169")
	THERM(169);

	private final int value;

	UnitSymbolKind(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static UnitSymbolKind fromValue(int value) {
		for (UnitSymbolKind kind : UnitSymbolKind.values()) {
			if (kind.value == value) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid UnitSymbolKind value: " + value);
	}
}