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

import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ReadingTypeMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ReadingTypeRepository;
import org.greenbuttonalliance.espi.common.service.ReadingTypeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
public class ReadingTypeServiceImpl implements ReadingTypeService {

	private final Log logger = LogFactory.getLog(getClass());

	private final ReadingTypeRepository readingTypeRepository;
	private final ReadingTypeMapper readingTypeMapper;

	public ReadingTypeServiceImpl(ReadingTypeRepository readingTypeRepository,
								  ReadingTypeMapper readingTypeMapper) {
		this.readingTypeRepository = readingTypeRepository;
		this.readingTypeMapper = readingTypeMapper;
	}

	@Override
	public void setReadingTypeRepository(ReadingTypeRepository readingTypeRepository) {
		// No-op: constructor injection used, but interface requires this method  
	}

	@Override
	public ReadingTypeEntity findByUUID(UUID uuid) {
		return readingTypeRepository.findByUuid(uuid).orElse(null);
	}

	@Override
	public ReadingTypeEntity findById(Long readingTypeId) {
		return readingTypeRepository.findById(readingTypeId).orElse(null);
	}

	@Override
	public ReadingTypeEntity save(ReadingTypeEntity readingType) {
		return readingTypeRepository.save(readingType);
	}

	@Override
	public String feedFor(ReadingTypeEntity readingType) {
		// TODO: Implement modern feed generation using DTOs
		logger.info("Generating feed for reading type: " + readingType.getId());
		return null;
	}

	@Override
	public String entryFor(ReadingTypeEntity readingType) {
		// TODO: Implement modern entry generation using DTOs
		logger.info("Generating entry for reading type: " + readingType.getId());
		return null;
	}

	@Override
	public void associateByUUID(MeterReadingEntity meterReading, UUID uuid) {
		ReadingTypeEntity entity = readingTypeRepository.findByUuid(uuid).orElse(null);
		if (entity != null) {
			entity.setMeterReadingEntity(meterReading);
			readingTypeRepository.save(entity);
			logger.info("Associated reading type " + uuid + " with meter reading " + meterReading.getId());
		}
	}

	@Override
	public void deleteById(long readingTypeId) {
		readingTypeRepository.deleteById(readingTypeId);
		logger.info("Deleted reading type with ID: " + readingTypeId);
	}

	@Override
	public void add(ReadingTypeEntity readingType) {
		readingTypeRepository.save(readingType);
		logger.info("Added reading type: " + readingType.getId());
	}

	@Override
	public void delete(ReadingTypeEntity readingType) {
		readingTypeRepository.deleteById(readingType.getId());
		logger.info("Deleted reading type: " + readingType.getId());
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
			logger.error("Failed to import ReadingType resource", e);
			return null;
		}
	}


}
