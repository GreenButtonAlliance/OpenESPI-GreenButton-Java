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

package org.greenbuttonalliance.espi.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.mapper.usage.MeterReadingMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.greenbuttonalliance.espi.common.service.MeterReadingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
@RequiredArgsConstructor
public class MeterReadingServiceImpl implements MeterReadingService {

	private final MeterReadingRepository meterReadingRepository;
	private final MeterReadingMapper meterReadingMapper;

	@Override
	public MeterReadingEntity findById(UUID retailCustomerId, UUID usagePointId,
			UUID meterReadingId) {
		// TODO: Implement scoped query for retailCustomer.usagePoint.meterReading
		return meterReadingRepository.findById(meterReadingId).orElse(null);
	}

	@Override
	public MeterReadingEntity importResource(InputStream stream) {
		try {
			// Use JAXB to parse XML stream to DTO
			jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(MeterReadingDto.class);
			jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
			MeterReadingDto dto = (MeterReadingDto) unmarshaller.unmarshal(stream);
			
			// Convert DTO to Entity using mapper
			MeterReadingEntity entity = meterReadingMapper.toEntity(dto);
			
			// Save and return entity
			return meterReadingRepository.save(entity);
			
		} catch (Exception e) {
			log.error("Failed to import MeterReading resource", e);
			return null;
		}
	}
}
