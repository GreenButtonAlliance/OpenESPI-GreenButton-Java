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

import jakarta.persistence.Embeddable;

import java.math.BigInteger;

/**
 * Rational number represented as numerator / denominator.
 *
 * <p>Embeddable component used for precise fractional values in ESPI entities.
 * Both numerator and denominator are optional (nullable) per ESPI 4.0 specification.
 *
 * <p>Per ESPI 4.0 espi.xsd lines 1406-1418.
 *
 * <p>Note: JAXB annotations are on RationalNumberDto for XML marshalling.
 * This entity class is for JPA persistence only.
 *
 * @see <a href="http://naesb.org/espi">ESPI Specification</a>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RationalNumber {

	/**
	 * Numerator of the rational number.
	 *
	 * <p>Optional field (nullable). Type: xs:integer from XSD.
	 * XSD: espi.xsd line 1413
	 *
	 * <p>Note: Uses DECIMAL(38,0) column type for database compatibility while maintaining
	 * BigInteger type in Java for XSD compliance. Column type is specified in entity
	 * @AttributeOverride annotations.
	 */
	private BigInteger numerator;

	/**
	 * Denominator of the rational number.
	 *
	 * <p>Optional field (nullable). Type: assumed xs:integer from context.
	 * XSD: espi.xsd line 1414
	 *
	 * <p>Note: XSD does not explicitly specify type for denominator.
	 * Implementation assumes xs:integer based on RationalNumber semantics.
	 *
	 * <p>Uses DECIMAL(38,0) column type for database compatibility while maintaining
	 * BigInteger type in Java for XSD compliance. Column type is specified in entity
	 * @AttributeOverride annotations.
	 */
	private BigInteger denominator;

}