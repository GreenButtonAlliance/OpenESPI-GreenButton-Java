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
import org.greenbuttonalliance.espi.thirdparty.service.MeterReadingRESTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.xml.bind.JAXBException;
import java.util.UUID;

@Service
public class MeterReadingRESTServiceImpl implements MeterReadingRESTService {
	@Autowired
	protected MeterReadingRESTRepository repository;

	@Override
	public MeterReadingEntity findByUUID(UUID retailCustomerId, UUID uuid)
			throws JAXBException {
		return repository.findByUUID(retailCustomerId, uuid);
	}

	public void setRepository(MeterReadingRESTRepository repository) {
		this.repository = repository;
	}

	public MeterReadingRESTRepository setRepository() {
		return this.repository;
	}
}
