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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigInteger;

/**
 * Interharmonic reading represented as numerator / denominator.
 *
 * <p>Embeddable component used for harmonic and interharmonic measurements in reading types.
 * Harmonics are identified by denominator = 1. Both numerator and denominator are
 * optional (nullable) per ESPI 4.0 specification.
 *
 * <p>Per ESPI 4.0 espi.xsd lines 1419-1431.
 *
 * @see <a href="http://naesb.org/espi">ESPI Specification</a>
 */
@Embeddable
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReadingInterharmonic", namespace = "http://naesb.org/espi", propOrder = {
	"numerator",
	"denominator"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReadingInterharmonic {

	/**
	 * Numerator of the interharmonic rational number.
	 *
	 * <p>Optional field (nullable). Type: xs:integer from XSD.
	 * XSD: espi.xsd line 1426
	 *
	 * <p>Note: Uses BIGINT column type for database compatibility while maintaining
	 * BigInteger type in Java for XSD compliance. Column type is specified in entity
	 * @AttributeOverride annotations.
	 */
	@XmlElement(name = "numerator", namespace = "http://naesb.org/espi")
	private BigInteger numerator;

	/**
	 * Denominator of the interharmonic rational number.
	 * Harmonics are identified by denominator = 1.
	 *
	 * <p>Optional field (nullable). Type: assumed xs:integer from context.
	 * XSD: espi.xsd line 1427
	 *
	 * <p>Note: XSD does not explicitly specify type for denominator (schema bug).
	 * Implementation assumes xs:integer based on ReadingInterharmonic semantics.
	 *
	 * <p>Uses BIGINT column type for database compatibility while maintaining
	 * BigInteger type in Java for XSD compliance. Column type is specified in entity
	 * @AttributeOverride annotations.
	 */
	@XmlElement(name = "denominator", namespace = "http://naesb.org/espi")
	private BigInteger denominator;

}