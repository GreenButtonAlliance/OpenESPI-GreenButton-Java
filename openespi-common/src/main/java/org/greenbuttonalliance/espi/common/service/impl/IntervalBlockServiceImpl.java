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
import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto;
import org.greenbuttonalliance.espi.common.mapper.usage.IntervalBlockMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository;
import org.greenbuttonalliance.espi.common.service.IntervalBlockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
@RequiredArgsConstructor
public class IntervalBlockServiceImpl implements IntervalBlockService {

	private final IntervalBlockRepository intervalBlockRepository;
	private final IntervalBlockMapper intervalBlockMapper;

	@Override
	public List<IntervalBlockEntity> findAllByMeterReadingId(UUID meterReadingId) {
		return intervalBlockRepository.findAllByMeterReadingId(meterReadingId);
	}

	@Override
	public String feedFor(List<IntervalBlockEntity> intervalBlocks) {
		// TODO: Implement modern feed generation using DTOs
		log.info("Generating feed for " + intervalBlocks.size() + " interval blocks");
		return null;
	}

	@Override
	public IntervalBlockEntity findById(UUID retailCustomerId, UUID usagePointId,
			UUID meterReadingId, UUID intervalBlockId) {
		return intervalBlockRepository.findById(intervalBlockId).orElse(null);
	}

	@Override
	public String entryFor(IntervalBlockEntity intervalBlock) {
		// TODO: Implement modern entry generation using DTOs
		log.info("Generating entry for interval block: " + intervalBlock.getId());
		return null;
	}

	@Override
	public void associateByUUID(MeterReadingEntity meterReading, UUID uuid) {
		IntervalBlockEntity entity = intervalBlockRepository.findById(uuid).orElse(null);
		if (entity != null) {
			entity.setMeterReading(meterReading);
			intervalBlockRepository.save(entity);
			log.info("Associated interval block " + uuid + " with meter reading " + meterReading.getId());
		}
	}

	@Override
	public void delete(IntervalBlockEntity intervalBlock) {
		intervalBlockRepository.deleteById(intervalBlock.getId());
		log.info("Deleted interval block: " + intervalBlock.getId());
	}

	@Override
	public List<IntervalBlockEntity> findAllByMeterReading(MeterReadingEntity meterReading) {
		// TODO: Implement findAllByMeterReading query in repository
		return intervalBlockRepository.findByMeterReadingEntity(meterReading);
	}

	@Override
	public IntervalBlockEntity findByURI(String uri) {
		// TODO: Implement findByURI query in repository
		return intervalBlockRepository.findByUri(uri).orElse(null);
	}

	@Override
	public IntervalBlockEntity save(IntervalBlockEntity intervalBlock) {
		return intervalBlockRepository.save(intervalBlock);
	}


	@Override
	public void add(IntervalBlockEntity intervalBlock) {
		intervalBlockRepository.save(intervalBlock);
		log.info("Added interval block: " + intervalBlock.getId());
	}

	@Override
	public IntervalBlockEntity importResource(InputStream stream) {
		try {
			// Use JAXB to parse XML stream to DTO
			jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(IntervalBlockDto.class);
			jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
			IntervalBlockDto dto = (IntervalBlockDto) unmarshaller.unmarshal(stream);
			
			// Convert DTO to Entity using mapper
			IntervalBlockEntity entity = intervalBlockMapper.toEntity(dto);
			
			// Save and return entity
			return intervalBlockRepository.save(entity);
			
		} catch (Exception e) {
			log.error("Failed to import IntervalBlock resource", e);
			return null;
		}
	}

	@Override
	public IntervalBlockEntity findById(UUID intervalBlockId) {
		return intervalBlockRepository.findById(intervalBlockId).orElse(null);
	}
}
