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
import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ElectricPowerQualitySummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ElectricPowerQualitySummaryRepository;
import org.greenbuttonalliance.espi.common.service.ElectricPowerQualitySummaryService;
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
public class ElectricPowerQualitySummaryServiceImpl implements
		ElectricPowerQualitySummaryService {

	private final ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository;
	private final ElectricPowerQualitySummaryMapper electricPowerQualitySummaryMapper;

	@Override
	public ElectricPowerQualitySummaryEntity findByUUID(UUID uuid) {
        return electricPowerQualitySummaryRepository.findById(uuid).orElse(null);
	}

	@Override
	public ElectricPowerQualitySummaryEntity findById(
			UUID electricPowerQualitySummaryId) {
		return electricPowerQualitySummaryRepository
				.findById(electricPowerQualitySummaryId).orElse(null);
	}

	@Override
	public ElectricPowerQualitySummaryEntity save(ElectricPowerQualitySummaryEntity electricPowerQualitySummary) {
		return electricPowerQualitySummaryRepository
				.save(electricPowerQualitySummary);
	}

	@Override
	public List<ElectricPowerQualitySummaryEntity> findAllByUsagePointEntity(
			UsagePointEntity usagePoint) {
		return electricPowerQualitySummaryRepository.findByUsagePoint(usagePoint);
	}

	@Override
	public String feedFor(
			List<ElectricPowerQualitySummaryEntity> electricPowerQualitySummaries) {
		// TODO: Implement modern feed generation using DTOs
		log.info("Generating feed for " + electricPowerQualitySummaries.size() + " electric power quality summaries");
		return null;
	}

	@Override
	public String entryFor(
			ElectricPowerQualitySummaryEntity electricPowerQualitySummary) {
		// TODO: Implement modern entry generation using DTOs
		log.info("Generating entry for electric power quality summary: " + electricPowerQualitySummary.getId());
		return null;
	}

	@Override
	public void associateByUUID(UsagePointEntity usagePoint, UUID uuid) {
		ElectricPowerQualitySummaryEntity entity = electricPowerQualitySummaryRepository.findById(uuid).orElse(null);
		if (entity != null) {
			entity.setUsagePoint(usagePoint);
			electricPowerQualitySummaryRepository.save(entity);
			log.info("Associated electric power quality summary " + uuid + " with usage point " + usagePoint.getId());
		}
	}

	@Override
	public void delete(ElectricPowerQualitySummaryEntity electricPowerQualitySummary) {
		electricPowerQualitySummaryRepository
				.deleteById(electricPowerQualitySummary.getId());
	}


	@Override
	public void add(ElectricPowerQualitySummaryEntity electricPowerQualitySummary) {
		electricPowerQualitySummaryRepository.save(electricPowerQualitySummary);
		log.info("Added electric power quality summary: " + electricPowerQualitySummary.getId());
	}

	@Override
	public ElectricPowerQualitySummaryEntity importResource(InputStream stream) {
		try {
			// Use JAXB to parse XML stream to DTO
			jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(ElectricPowerQualitySummaryDto.class);
			jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
			ElectricPowerQualitySummaryDto dto = (ElectricPowerQualitySummaryDto) unmarshaller.unmarshal(stream);
			
			// Convert DTO to Entity using mapper
			ElectricPowerQualitySummaryEntity entity = electricPowerQualitySummaryMapper.toEntity(dto);
			
			// Save and return entity
			return electricPowerQualitySummaryRepository.save(entity);
			
		} catch (Exception e) {
			log.error("Failed to import ElectricPowerQualitySummary resource", e);
			return null;
		}
	}
}
