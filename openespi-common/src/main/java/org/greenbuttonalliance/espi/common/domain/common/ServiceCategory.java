/*
 *
 *    Copyright (c) 2018-2025 Green Button Alliance, Inc.
 *
 *    Portions copyright (c) 2013-2018 EnergyOS.org
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

public enum ServiceCategory {
	ELECTRICITY(0L),
	GAS(1L),
	WATER(2L),
	TIME(3L),
	HEAT(4L),
	REFUSE(5L),
	SEWERAGE(6L),
	RATES(7L),
	TV_LICENSE(8L),
	INTERNET(9L);

	private final Long value;

	ServiceCategory(Long value) {
		this.value = value;
	}

	public Long getValue() {
		return value;
	}

	public static ServiceCategory fromValue(Long value) {
		for (ServiceCategory category : ServiceCategory.values()) {
			if (category.value.equals(value)) {
				return category;
			}
		}
		return null;
	}
}