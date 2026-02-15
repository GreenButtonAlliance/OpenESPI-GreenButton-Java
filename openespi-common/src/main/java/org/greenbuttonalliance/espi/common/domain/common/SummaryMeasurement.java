/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.domain.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.usage.enums.UnitMultiplierKind;
import org.greenbuttonalliance.espi.common.domain.usage.enums.UnitSymbolKind;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Summary measurement data embedded in usage summary entities.
 *
 * <p>Embeddable component used for aggregate measurement values with units and timestamps.
 * All fields are optional (nullable) per ESPI 4.0 specification.
 *
 * <p>Per ESPI 4.0 espi.xsd lines 1094-1129.
 *
 * @see <a href="http://naesb.org/espi">ESPI Specification</a>
 */
@Embeddable
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummaryMeasurement", namespace = "http://naesb.org/espi", propOrder = {
	"powerOfTenMultiplier",
	"timeStamp",
	"uom",
	"value",
	"readingTypeRef"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SummaryMeasurement {

	/**
	 * The multiplier part of the unit of measure, e.g. "kilo" (k).
	 *
	 * <p>Optional field (nullable). Type: UnitMultiplierKind enum.
	 * XSD: espi.xsd line 1101
	 */
	@XmlElement(name = "powerOfTenMultiplier", namespace = "http://naesb.org/espi")
	@Column(name = "power_of_ten_multiplier")
	@Enumerated(EnumType.STRING)
	private UnitMultiplierKind powerOfTenMultiplier;

	/**
	 * The date and time (if needed) of the summary measurement.
	 *
	 * <p>Optional field (nullable). Type: TimeType (seconds since Unix epoch).
	 * XSD: espi.xsd line 1106
	 */
	@XmlElement(name = "timeStamp", namespace = "http://naesb.org/espi")
	@Column(name = "time_stamp")
	private Long timeStamp;

	/**
	 * The units of the reading, e.g. "Wh".
	 *
	 * <p>Optional field (nullable). Type: UnitSymbolKind enum.
	 * XSD: espi.xsd line 1111
	 */
	@XmlElement(name = "uom", namespace = "http://naesb.org/espi")
	@Column(name = "uom")
	@Enumerated(EnumType.STRING)
	private UnitSymbolKind uom;

	/**
	 * The value of the summary measurement.
	 *
	 * <p>Optional field (nullable). Type: Int48 (48-bit signed integer).
	 * XSD: espi.xsd line 1116
	 */
	@XmlElement(name = "value", namespace = "http://naesb.org/espi")
	@Column(name = "value")
	private Long value;

	/**
	 * Reference URI to a full ReadingType resource.
	 *
	 * <p>Optional field (nullable). Type: xs:anyURI.
	 * XSD: espi.xsd line 1121
	 */
	@XmlElement(name = "readingTypeRef", namespace = "http://naesb.org/espi")
	@Column(name = "reading_type_ref")
	private String readingTypeRef;
}