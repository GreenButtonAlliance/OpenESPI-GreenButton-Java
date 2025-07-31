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

package org.greenbuttonalliance.espi.thirdparty.repository.impl;

import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.thirdparty.repository.MeterReadingRESTRepository;
import org.greenbuttonalliance.espi.thirdparty.repository.UsagePointRESTRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.xml.bind.JAXBException;
import java.util.List;
import java.util.UUID;

@Repository
public class MeterReadingRESTRepositoryImpl implements
        MeterReadingRESTRepository {

	@Autowired
	private UsagePointRESTRepository usagePointRESTRepository;

	public void setUsagePointRESTRepository(
			UsagePointRESTRepository usagePointRESTRepository) {
		this.usagePointRESTRepository = usagePointRESTRepository;
	}

	public UsagePointRESTRepository getUsagePointRESTRepository(
			UsagePointRESTRepository usagePointRESTRepository) {
		return this.usagePointRESTRepository;
	}

	@Override
	public MeterReadingEntity findByUUID(UUID retailCustomerId, UUID uuid)
			throws JAXBException {
		List<UsagePointEntity> usagePointList = usagePointRESTRepository
				.findAllByRetailCustomerId(retailCustomerId);

		return findMeterReading(usagePointList, uuid);
	}

	private MeterReadingEntity findMeterReading(List<UsagePointEntity> usagePointList,
			UUID uuid) {
		for (UsagePointEntity usagePoint : usagePointList) {
			MeterReadingEntity meterReading = findMeterReadingInUsagePoint(
					usagePoint.getMeterReadings(), uuid);
			if (meterReading != null) {
				return meterReading;
			}
		}
		return null;
	}

	private MeterReadingEntity findMeterReadingInUsagePoint(
			List<MeterReadingEntity> meterReadings, UUID uuid) {
		for (MeterReadingEntity meterReading : meterReadings) {
			if (meterReading.getId().equals(uuid)) {
				return meterReading;
			}
		}
		return null;
	}
}
