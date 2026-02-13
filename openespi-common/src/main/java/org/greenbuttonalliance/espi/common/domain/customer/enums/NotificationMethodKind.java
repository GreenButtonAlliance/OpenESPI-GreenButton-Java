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
 * Enumeration for NotificationMethodKind values.
 *
 * Method by which the customer was notified.
 * Per ESPI 4.0 customer.xsd lines 1961-1996.
 */
@XmlType(name = "NotificationMethodKind", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum NotificationMethodKind {

	/**
	 * Contacted by phone by customer service representative.
	 * XSD value: "call" (line 1968)
	 */
	@XmlEnumValue("call")
	CALL("call"),

	/**
	 * Trouble reported by email.
	 * XSD value: "email" (line 1973)
	 */
	@XmlEnumValue("email")
	EMAIL("email"),

	/**
	 * Trouble reported by letter.
	 * XSD value: "letter" (line 1978)
	 */
	@XmlEnumValue("letter")
	LETTER("letter"),

	/**
	 * Trouble reported by other means.
	 * XSD value: "other" (line 1983)
	 */
	@XmlEnumValue("other")
	OTHER("other"),

	/**
	 * Trouble reported through interactive voice response system.
	 * XSD value: "ivr" (line 1988)
	 */
	@XmlEnumValue("ivr")
	IVR("ivr");

	private final String value;

	NotificationMethodKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static NotificationMethodKind fromValue(String value) {
		for (NotificationMethodKind kind : NotificationMethodKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid NotificationMethodKind value: " + value);
	}
}