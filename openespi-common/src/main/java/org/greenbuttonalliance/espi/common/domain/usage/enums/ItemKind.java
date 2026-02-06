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
 * Enumeration for ItemKind values.
 *
 * [extension] Code for classification billing line items.
 * Per ESPI 4.0 espi.xsd lines 1790-1850.
 */
@XmlType(name = "ItemKind", namespace = "http://naesb.org/espi")
@XmlEnum
public enum ItemKind {

	/**
	 * Energy Generation Fee. A charge for generation of energy.
	 * XSD value: 1 (line 1797)
	 */
	@XmlEnumValue("1")
	ENERGY_GENERATION_FEE(1),

	/**
	 * Energy Delivery Fee. A charge for delivery of energy.
	 * XSD value: 2 (line 1802)
	 */
	@XmlEnumValue("2")
	ENERGY_DELIVERY_FEE(2),

	/**
	 * Energy Usage Fee. A charge for electricity, natural gas, water consumption.
	 * XSD value: 3 (line 1807)
	 */
	@XmlEnumValue("3")
	ENERGY_USAGE_FEE(3),

	/**
	 * Administrative Fee. A fee for administrative services.
	 * XSD value: 4 (line 1812)
	 */
	@XmlEnumValue("4")
	ADMINISTRATIVE_FEE(4),

	/**
	 * Tax. A local, state, or federal energy tax.
	 * XSD value: 5 (line 1817)
	 */
	@XmlEnumValue("5")
	TAX(5),

	/**
	 * Energy Generation Credit. A credit, discount or rebate for generation of energy.
	 * XSD value: 6 (line 1822)
	 */
	@XmlEnumValue("6")
	ENERGY_GENERATION_CREDIT(6),

	/**
	 * Energy Delivery Credit. A credit, discount or rebate for delivery of energy.
	 * XSD value: 7 (line 1827)
	 */
	@XmlEnumValue("7")
	ENERGY_DELIVERY_CREDIT(7),

	/**
	 * Administrative Credit. A credit, discount or rebate for administrative services.
	 * XSD value: 8 (line 1832)
	 */
	@XmlEnumValue("8")
	ADMINISTRATIVE_CREDIT(8),

	/**
	 * Payment. A payment for a previous billing.
	 * XSD value: 9 (line 1837)
	 */
	@XmlEnumValue("9")
	PAYMENT(9),

	/**
	 * Information. An informational line item.
	 * XSD value: 10 (line 1842)
	 */
	@XmlEnumValue("10")
	INFORMATION(10);

	private final int value;

	ItemKind(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static ItemKind fromValue(int value) {
		for (ItemKind kind : ItemKind.values()) {
			if (kind.value == value) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid ItemKind value: " + value);
	}
}
