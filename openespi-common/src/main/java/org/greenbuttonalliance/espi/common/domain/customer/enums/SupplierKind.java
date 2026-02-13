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
 * Enumeration for SupplierKind values.
 *
 * Kind of supplier.
 * Per ESPI 4.0 customer.xsd lines 2231-2271.
 */
@XmlType(name = "SupplierKind", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum SupplierKind {

	/**
	 * Entity that delivers the service to the customer.
	 * XSD value: "utility" (line 2238)
	 */
	@XmlEnumValue("utility")
	UTILITY("utility"),

	/**
	 * Entity that sells the service, but does not deliver to the customer; applies to the deregulated markets.
	 * XSD value: "retailer" (line 2243)
	 */
	@XmlEnumValue("retailer")
	RETAILER("retailer"),

	/**
	 * Other kind of supplier.
	 * XSD value: "other" (line 2248)
	 */
	@XmlEnumValue("other")
	OTHER("other"),

	/**
	 * Load Serving Entity.
	 * XSD value: "lse" (line 2253)
	 */
	@XmlEnumValue("lse")
	LSE("lse"),

	/**
	 * Meter Data Management Agent.
	 * XSD value: "mdma" (line 2258)
	 */
	@XmlEnumValue("mdma")
	MDMA("mdma"),

	/**
	 * Meter Service Provider.
	 * XSD value: "msp" (line 2263)
	 */
	@XmlEnumValue("msp")
	MSP("msp");

	private final String value;

	SupplierKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static SupplierKind fromValue(String value) {
		for (SupplierKind kind : SupplierKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid SupplierKind value: " + value);
	}
}