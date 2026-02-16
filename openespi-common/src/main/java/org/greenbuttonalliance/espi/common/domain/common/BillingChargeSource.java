/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Embeddable value object for BillingChargeSource.
 *
 * <p>Information about the source of billing charge.
 * Contains a single agencyName field identifying the billing agency.
 * Embedded within UsageSummary entity.
 *
 * <p>Per ESPI 4.0 espi.xsd lines 1628-1643.
 *
 * <p>Note: JAXB annotations are on BillingChargeSourceDto for XML marshalling.
 * This entity class is for JPA persistence only.
 *
 * @see <a href="http://naesb.org/espi">ESPI Specification</a>
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingChargeSource implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Name of the billing source agency.
	 *
	 * <p>Optional field (nullable). Maximum length 256 characters per String256 type.
	 * XSD: espi.xsd line 1635
	 */
	@Column(name = "billing_charge_source_agency_name", length = 256)
	private String agencyName;

	/**
	 * Checks if this billing charge source has a value.
	 *
	 * @return true if agency name is present
	 */
	public boolean hasValue() {
		return agencyName != null && !agencyName.trim().isEmpty();
	}
}
