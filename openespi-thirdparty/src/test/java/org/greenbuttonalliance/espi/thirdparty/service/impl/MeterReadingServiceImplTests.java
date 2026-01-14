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

package org.greenbuttonalliance.espi.thirdparty.service.impl;

import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.thirdparty.repository.MeterReadingRESTRepository;
import org.greenbuttonalliance.espi.thirdparty.utils.factories.Factory;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeterReadingServiceImplTests {

	private MeterReadingRESTRepository repository;
	private MeterReadingRESTServiceImpl service;

	@BeforeEach
    public void before() {
		service = new MeterReadingRESTServiceImpl();
		repository = mock(MeterReadingRESTRepository.class);
		service.setRepository(repository);
	}

	@Test
	public void findByUUID_returnsMeterReading() throws JAXBException {
		MeterReadingEntity meterReading = Factory.newMeterReading();
		Long retailCustomerId = 1000003L;
		UUID meterReadingId = UUID.randomUUID();

		when(repository.findByUUID(eq(retailCustomerId), eq(meterReadingId))).thenReturn(
				meterReading);

		assertEquals(meterReading, service.findByUUID(retailCustomerId, meterReadingId));
	}
}
