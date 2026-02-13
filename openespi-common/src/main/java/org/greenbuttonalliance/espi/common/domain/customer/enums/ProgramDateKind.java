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
 * Enumeration for ProgramDateKind values.
 *
 * [extension] Type of Demand Response program date.
 * Per ESPI 4.0 customer.xsd lines 1997-2027.
 */
@XmlType(name = "ProgramDateKind", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum ProgramDateKind {

	/**
	 * Date customer enrolled in Demand Response program.
	 * XSD value: "CUST_DR_PROGRAM_ENROLLMENT_DATE" (line 2004)
	 */
	@XmlEnumValue("CUST_DR_PROGRAM_ENROLLMENT_DATE")
	CUST_DR_PROGRAM_ENROLLMENT_DATE("CUST_DR_PROGRAM_ENROLLMENT_DATE"),

	/**
	 * Date customer terminated enrollment in Demand Response program.
	 * XSD value: "CUST_DR_PROGRAM_DE_ENROLLMENT_DATE" (line 2009)
	 */
	@XmlEnumValue("CUST_DR_PROGRAM_DE_ENROLLMENT_DATE")
	CUST_DR_PROGRAM_DE_ENROLLMENT_DATE("CUST_DR_PROGRAM_DE_ENROLLMENT_DATE"),

	/**
	 * Earliest date customer can terminate Demand Response enrollment, regardless of financial impact.
	 * XSD value: "CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL" (line 2014)
	 */
	@XmlEnumValue("CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL")
	CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL("CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL"),

	/**
	 * Earliest date customer can terminate Demand Response enrollment, without financial impact.
	 * XSD value: "CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL" (line 2019)
	 */
	@XmlEnumValue("CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL")
	CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL("CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL");

	private final String value;

	ProgramDateKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static ProgramDateKind fromValue(String value) {
		for (ProgramDateKind kind : ProgramDateKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid ProgramDateKind value: " + value);
	}
}
