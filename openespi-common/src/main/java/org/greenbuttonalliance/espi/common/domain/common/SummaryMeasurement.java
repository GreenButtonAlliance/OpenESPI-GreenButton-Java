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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SummaryMeasurement {

	@Column(name = "powerOfTenMultiplier")
	private String powerOfTenMultiplier;

	@Column(name = "timeStamp")
	private Long timeStamp;

	@Column(name = "uom")
	private String uom;

	@Column(name = "value")
	private Long value;

	@Column(name = "readingTypeRef")
	private String readingTypeRef;

	public SummaryMeasurement() {
	}

	public SummaryMeasurement(String powerOfTenMultiplier, Long timeStamp, String uom, Long value, String readingTypeRef) {
		this.powerOfTenMultiplier = powerOfTenMultiplier;
		this.timeStamp = timeStamp;
		this.uom = uom;
		this.value = value;
		this.readingTypeRef = readingTypeRef;
	}

	public String getPowerOfTenMultiplier() {
		return powerOfTenMultiplier;
	}

	public void setPowerOfTenMultiplier(String powerOfTenMultiplier) {
		this.powerOfTenMultiplier = powerOfTenMultiplier;
	}

	public Long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getUom() {
		return uom;
	}

	public void setUom(String uom) {
		this.uom = uom;
	}

	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
	}

	public String getReadingTypeRef() {
		return readingTypeRef;
	}

	public void setReadingTypeRef(String readingTypeRef) {
		this.readingTypeRef = readingTypeRef;
	}
}