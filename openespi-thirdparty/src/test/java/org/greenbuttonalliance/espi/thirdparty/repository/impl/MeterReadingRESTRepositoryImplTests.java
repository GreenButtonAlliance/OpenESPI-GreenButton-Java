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
import org.greenbuttonalliance.espi.thirdparty.repository.UsagePointRESTRepository;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeterReadingRESTRepositoryImplTests {

    @Test
    public void findByUUID() throws Exception {
        MeterReadingRESTRepositoryImpl repository = new MeterReadingRESTRepositoryImpl();

        UsagePointRESTRepository usagePointRESTRepository = mock(UsagePointRESTRepository.class);
        repository.setUsagePointRESTRepository(usagePointRESTRepository);

        // Create test data with UUID
        UUID retailCustomerId = UUID.randomUUID();
        UUID meterReadingId = UUID.randomUUID();
        
        // Create MeterReadingEntity
        MeterReadingEntity expectedMeterReading = new MeterReadingEntity();
        expectedMeterReading.setId(meterReadingId);
        
        // Create UsagePointEntity with MeterReading
        UsagePointEntity usagePoint = new UsagePointEntity();
        List<MeterReadingEntity> meterReadings = new ArrayList<>();
        meterReadings.add(expectedMeterReading);
        usagePoint.setMeterReadings(meterReadings);
        
        List<UsagePointEntity> usagePoints = new ArrayList<>();
        usagePoints.add(usagePoint);
        
        when(usagePointRESTRepository.findAllByRetailCustomerId(retailCustomerId)).thenReturn(usagePoints);

        MeterReadingEntity meterReading = repository.findByUUID(retailCustomerId, meterReadingId);

        assertThat(meterReading).isEqualTo(expectedMeterReading);

    }
}
