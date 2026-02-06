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
 * Enumeration for ServiceKind values.
 *
 * Kind of service represented by the UsagePoint.
 * Per ESPI 4.0 espi.xsd lines 3552-3622.
 */
@XmlType(name = "ServiceKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum ServiceKind {

	/**
	 * Electricity service.
	 * XSD value: 0 (line 3559)
	 */
	@XmlEnumValue("0")
	ELECTRICITY(0),

	/**
	 * Gas service.
	 * XSD value: 1 (line 3565)
	 */
	@XmlEnumValue("1")
	GAS(1),

	/**
	 * Water service.
	 * XSD value: 2 (line 3571)
	 */
	@XmlEnumValue("2")
	WATER(2),

	/**
	 * Time service.
	 * XSD value: 3 (line 3577)
	 */
	@XmlEnumValue("3")
	TIME(3),

	/**
	 * Heat service.
	 * XSD value: 4 (line 3583)
	 */
	@XmlEnumValue("4")
	HEAT(4),

	/**
	 * Refuse (waster) service.
	 * XSD value: 5 (line 3589)
	 */
	@XmlEnumValue("5")
	REFUSE(5),

	/**
	 * Sewerage service.
	 * XSD value: 6 (line 3595)
	 */
	@XmlEnumValue("6")
	SEWERAGE(6),

	/**
	 * Rates (e.g. tax, charge, toll, duty, tariff, etc.) service.
	 * XSD value: 7 (line 3601)
	 */
	@XmlEnumValue("7")
	RATES(7),

	/**
	 * TV license service.
	 * XSD value: 8 (line 3607)
	 */
	@XmlEnumValue("8")
	TV_LICENCE(8),

	/**
	 * Internet service.
	 * XSD value: 9 (line 3613)
	 */
	@XmlEnumValue("9")
	INTERNET(9);

	private final int value;

	ServiceKind(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static ServiceKind fromValue(int value) {
		for (ServiceKind kind : ServiceKind.values()) {
			if (kind.value == value) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid ServiceKind value: " + value);
	}
}