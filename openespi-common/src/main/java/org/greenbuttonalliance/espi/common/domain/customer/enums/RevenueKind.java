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
 * Enumeration for RevenueKind values.
 *
 * Accounting classification of the type of revenue collected for the customer agreement,
 * typically used to break down accounts for revenue accounting.
 * Per ESPI 4.0 customer.xsd lines 2028-2073.
 */
@XmlType(name = "RevenueKind", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum RevenueKind {

	/**
	 * Residential revenue.
	 * XSD value: "residential" (line 2035)
	 */
	@XmlEnumValue("residential")
	RESIDENTIAL("residential"),

	/**
	 * Non-residential revenue.
	 * XSD value: "nonResidential" (line 2040)
	 */
	@XmlEnumValue("nonResidential")
	NON_RESIDENTIAL("nonResidential"),

	/**
	 * Commercial revenue.
	 * XSD value: "commercial" (line 2045)
	 */
	@XmlEnumValue("commercial")
	COMMERCIAL("commercial"),

	/**
	 * Industrial revenue.
	 * XSD value: "industrial" (line 2050)
	 */
	@XmlEnumValue("industrial")
	INDUSTRIAL("industrial"),

	/**
	 * Irrigation revenue.
	 * XSD value: "irrigation" (line 2055)
	 */
	@XmlEnumValue("irrigation")
	IRRIGATION("irrigation"),

	/**
	 * Streetlight revenue.
	 * XSD value: "streetLight" (line 2060)
	 */
	@XmlEnumValue("streetLight")
	STREET_LIGHT("streetLight"),

	/**
	 * Other revenue kind.
	 * XSD value: "other" (line 2065)
	 */
	@XmlEnumValue("other")
	OTHER("other");

	private final String value;

	RevenueKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static RevenueKind fromValue(String value) {
		for (RevenueKind kind : RevenueKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid RevenueKind value: " + value);
	}
}