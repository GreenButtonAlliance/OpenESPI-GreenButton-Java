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

package org.greenbuttonalliance.espi.thirdparty.utils.factories;

import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;

import java.util.UUID;

public class Factory {

	public static UsagePointEntity newUsagePoint() {
		UsagePointEntity usagePoint = new UsagePointEntity();

		usagePoint.setId(UUID.fromString("7BC41774-7190-4864-841C-861AC76D46C2"));
		usagePoint.setDescription("Electric meter");
		usagePoint.setServiceCategory(ServiceCategory.ELECTRICITY);

		RetailCustomerEntity retailCustomer = new RetailCustomerEntity();
		retailCustomer.setId(UUID.randomUUID());
		usagePoint.setRetailCustomer(retailCustomer);

		usagePoint.getMeterReadings().add(newMeterReading());

		return usagePoint;
	}

	public static MeterReadingEntity newMeterReading() {
		MeterReadingEntity meterReading = new MeterReadingEntity();

		meterReading.setId(UUID.fromString("F77FBF34-A09E-4EBC-9606-FF1A59A17CAE"));
		meterReading.setDescription("Electricity consumption");

		return meterReading;
	}
}
