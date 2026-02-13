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
 * Enumeration for CRUDOperation values.
 *
 * Specifies the operation requested of this item.
 * Per ESPI 4.0 customer.xsd lines 1557-1591.
 */
@XmlType(name = "CRUDOperation", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum CRUDOperation {

	/**
	 * Create.
	 * XSD value: 0 (line 1564)
	 */
	@XmlEnumValue("0")
	CREATE(0),

	/**
	 * Read.
	 * XSD value: 1 (line 1570)
	 */
	@XmlEnumValue("1")
	READ(1),

	/**
	 * Update.
	 * XSD value: 2 (line 1576)
	 */
	@XmlEnumValue("2")
	UPDATE(2),

	/**
	 * Delete.
	 * XSD value: 3 (line 1582)
	 */
	@XmlEnumValue("3")
	DELETE(3);

	private final int value;

	CRUDOperation(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static CRUDOperation fromValue(int value) {
		for (CRUDOperation operation : CRUDOperation.values()) {
			if (operation.value == value) {
				return operation;
			}
		}
		throw new IllegalArgumentException("Invalid CRUDOperation value: " + value);
	}
}