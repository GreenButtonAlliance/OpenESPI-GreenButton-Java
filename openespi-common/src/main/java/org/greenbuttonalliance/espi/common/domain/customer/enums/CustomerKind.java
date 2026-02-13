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
 * Enumeration for CustomerKind values.
 *
 * Kind of customer.
 * Per ESPI 4.0 customer.xsd lines 1687-1772.
 */
@XmlType(name = "CustomerKind", namespace = "http://naesb.org/espi/customer")
@XmlEnum
public enum CustomerKind {

	/**
	 * Residential customer.
	 * XSD value: "residential" (line 1694)
	 */
	@XmlEnumValue("residential")
	RESIDENTIAL("residential"),

	/**
	 * Residential and commercial customer.
	 * XSD value: "residentialAndCommercial" (line 1699)
	 */
	@XmlEnumValue("residentialAndCommercial")
	RESIDENTIAL_AND_COMMERCIAL("residentialAndCommercial"),

	/**
	 * Residential and streetlight customer.
	 * XSD value: "residentialAndStreetlight" (line 1704)
	 */
	@XmlEnumValue("residentialAndStreetlight")
	RESIDENTIAL_AND_STREETLIGHT("residentialAndStreetlight"),

	/**
	 * Residential streetlight or other related customer.
	 * XSD value: "residentialStreetlightOthers" (line 1709)
	 */
	@XmlEnumValue("residentialStreetlightOthers")
	RESIDENTIAL_STREETLIGHT_OTHERS("residentialStreetlightOthers"),

	/**
	 * Residential farm service customer.
	 * XSD value: "residentialFarmService" (line 1714)
	 */
	@XmlEnumValue("residentialFarmService")
	RESIDENTIAL_FARM_SERVICE("residentialFarmService"),

	/**
	 * Commercial industrial customer.
	 * XSD value: "commercialIndustrial" (line 1719)
	 */
	@XmlEnumValue("commercialIndustrial")
	COMMERCIAL_INDUSTRIAL("commercialIndustrial"),

	/**
	 * Pumping load customer.
	 * XSD value: "pumpingLoad" (line 1724)
	 */
	@XmlEnumValue("pumpingLoad")
	PUMPING_LOAD("pumpingLoad"),

	/**
	 * Wind machine customer.
	 * XSD value: "windMachine" (line 1729)
	 */
	@XmlEnumValue("windMachine")
	WIND_MACHINE("windMachine"),

	/**
	 * Customer as energy service supplier.
	 * XSD value: "energyServiceSupplier" (line 1734)
	 */
	@XmlEnumValue("energyServiceSupplier")
	ENERGY_SERVICE_SUPPLIER("energyServiceSupplier"),

	/**
	 * Customer as energy service scheduler.
	 * XSD value: "energyServiceScheduler" (line 1739)
	 */
	@XmlEnumValue("energyServiceScheduler")
	ENERGY_SERVICE_SCHEDULER("energyServiceScheduler"),

	/**
	 * Represents the owning enterprise.
	 * XSD value: "enterprise" (line 1744)
	 */
	@XmlEnumValue("enterprise")
	ENTERPRISE("enterprise"),

	/**
	 * Represents a local operator of a larger enterprise.
	 * XSD value: "regionalOperator" (line 1749)
	 */
	@XmlEnumValue("regionalOperator")
	REGIONAL_OPERATOR("regionalOperator"),

	/**
	 * A subsidiary of a larger enterprise.
	 * XSD value: "subsidiary" (line 1754)
	 */
	@XmlEnumValue("subsidiary")
	SUBSIDIARY("subsidiary"),

	/**
	 * Internal use customer.
	 * XSD value: "internalUse" (line 1759)
	 */
	@XmlEnumValue("internalUse")
	INTERNAL_USE("internalUse"),

	/**
	 * Other kind of customer.
	 * XSD value: "other" (line 1764)
	 */
	@XmlEnumValue("other")
	OTHER("other");

	private final String value;

	CustomerKind(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static CustomerKind fromValue(String value) {
		for (CustomerKind kind : CustomerKind.values()) {
			if (kind.value.equals(value)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Invalid CustomerKind value: " + value);
	}
}