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
 * Enumeration for EnrollmentStatus values.
 *
 * [extension] Current Demand Response program enrollment status.
 * Per ESPI 4.0 customer.xsd lines 1808-1833.
 */
@XmlType(name = "EnrollmentStatus", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum EnrollmentStatus {

	/**
	 * Currently NOT enrolled in the Demand Response program.
	 * XSD value: "unenrolled" (line 1815)
	 */
	@XmlEnumValue("unenrolled")
	UNENROLLED("unenrolled"),

	/**
	 * Currently enrolled in the Demand Response program.
	 * XSD value: "enrolled" (line 1820)
	 */
	@XmlEnumValue("enrolled")
	ENROLLED("enrolled"),

	/**
	 * Currently pending enrollment in the Demand Response program.
	 * XSD value: "enrolledPending" (line 1825)
	 */
	@XmlEnumValue("enrolledPending")
	ENROLLED_PENDING("enrolledPending");

	private final String value;

	EnrollmentStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static EnrollmentStatus fromValue(String value) {
		for (EnrollmentStatus status : EnrollmentStatus.values()) {
			if (status.value.equals(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid EnrollmentStatus value: " + value);
	}
}