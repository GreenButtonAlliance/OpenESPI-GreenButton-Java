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
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ReadingTypeMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ReadingTypeRepository;
import org.greenbuttonalliance.espi.common.service.ReadingTypeService;
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
public class ReadingTypeServiceImpl implements ReadingTypeService {

	private final ReadingTypeRepository readingTypeRepository;
	private final ReadingTypeMapper readingTypeMapper;

	@Override
	public ReadingTypeEntity findByUUID(UUID uuid) {
  return readingTypeRepository.findById(uuid).orElse(null);
	}

	@Override
	public ReadingTypeEntity findById(UUID readingTypeId) {
		return readingTypeRepository.findById(readingTypeId).orElse(null);
	}

	@Override
	public ReadingTypeEntity save(ReadingTypeEntity readingType) {
		return readingTypeRepository.save(readingType);
	}

	@Override
	public String feedFor(ReadingTypeEntity readingType) {
		// TODO: Implement modern feed generation using DTOs
		log.info("Generating feed for reading type: " + readingType.getId());
		return null;
	}

	@Override
	public String entryFor(ReadingTypeEntity readingType) {
		// TODO: Implement modern entry generation using DTOs
		log.info("Generating entry for reading type: " + readingType.getId());
		return null;
	}

	@Override
	public void associateByUUID(MeterReadingEntity meterReading, UUID uuid) {
		ReadingTypeEntity entity = readingTypeRepository.findById(uuid).orElse(null);
		if (entity != null) {
			meterReading.setReadingType(entity);
			// Note: MeterReading should be saved by the calling service
			log.info("Associated reading type " + uuid + " with meter reading " + meterReading.getId());
		}
	}

	@Override
	public void deleteById(UUID readingTypeId) {
		readingTypeRepository.deleteById(readingTypeId);
		log.info("Deleted reading type with ID: " + readingTypeId);
	}

	@Override
	public void add(ReadingTypeEntity readingType) {
		readingTypeRepository.save(readingType);
		log.info("Added reading type: " + readingType.getId());
	}

	@Override
	public void delete(ReadingTypeEntity readingType) {
		readingTypeRepository.deleteById(readingType.getId());
		log.info("Deleted reading type: " + readingType.getId());
	}

	@Override
	public ReadingTypeEntity importResource(InputStream stream) {
		try {
			// Use JAXB to parse XML stream to DTO
			jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(ReadingTypeDto.class);
			jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
			ReadingTypeDto dto = (ReadingTypeDto) unmarshaller.unmarshal(stream);
			
			// Convert DTO to Entity using mapper
			ReadingTypeEntity entity = readingTypeMapper.toEntity(dto);
			
			// Save and return entity
			return readingTypeRepository.save(entity);
			
		} catch (Exception e) {
			log.error("Failed to import ReadingType resource", e);
			return null;
		}
	}
}
