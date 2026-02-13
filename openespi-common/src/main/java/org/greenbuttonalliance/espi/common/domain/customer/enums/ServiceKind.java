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

package org.greenbuttonalliance.espi.common.domain.customer.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Enumeration for ServiceKind values.
 *
 * Kind of service.
 * Per ESPI 4.0 customer.xsd lines 2074-2135.
 */
@XmlType(name = "ServiceKind", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum ServiceKind {

	/**
	 * Electricity service.
	 * XSD value: "electricity" (line 2079)
	 */
	@XmlEnumValue("electricity")
	ELECTRICITY("electricity"),

	/**
	 * Gas service.
	 * XSD value: "gas" (line 2084)
	 */
	@XmlEnumValue("gas")
	GAS("gas"),

	/**
	 * Water service.
	 * XSD value: "water" (line 2089)
	 */
	@XmlEnumValue("water")
	WATER("water"),

	/**
	 * Time service.
	 * XSD value: "time" (line 2094)
	 */
	@XmlEnumValue("time")
	TIME("time"),

	/**
	 * Heat service.
	 * XSD value: "heat" (line 2099)
	 */
	@XmlEnumValue("heat")
	HEAT("heat"),

	/**
	 * Refuse (waster) service.
	 * XSD value: "refuse" (line 2104)
	 */
	@XmlEnumValue("refuse")
	REFUSE("refuse"),

	/**
	 * Sewerage service.
	 * XSD value: "sewerage" (line 2109)
	 */
	@XmlEnumValue("sewerage")
	SEWERAGE("sewerage"),

	/**
	 * Rates (e.g. tax, charge, toll, duty, tariff, etc.) service.
	 * XSD value: "rates" (line 2114)
	 */
	@XmlEnumValue("rates")
	RATES("rates"),

	/**
	 * TV license service.
	 * XSD value: "tvLicence" (line 2119)
	 */
	@XmlEnumValue("tvLicence")
	TV_LICENCE("tvLicence"),

	/**
	 * Internet service.
	 * XSD value: "internet" (line 2124)
	 */
	@XmlEnumValue("internet")
	INTERNET("internet"),

	/**
	 * Other kind of service.
	 * XSD value: "other" (line 2129)
	 */
	@XmlEnumValue("other")
	OTHER("other");

	private final String value;

	ServiceKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static ServiceKind fromValue(String value) {
		for (ServiceKind kind : ServiceKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid ServiceKind value: " + value);
	}
}