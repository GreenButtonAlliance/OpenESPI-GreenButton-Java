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

import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ElectricPowerQualitySummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ElectricPowerQualitySummaryRepository;
import org.greenbuttonalliance.espi.common.service.ElectricPowerQualitySummaryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
public class ElectricPowerQualitySummaryServiceImpl implements
		ElectricPowerQualitySummaryService {

	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository;

	@Autowired
	private ElectricPowerQualitySummaryMapper electricPowerQualitySummaryMapper;

	@Override
	public ElectricPowerQualitySummaryEntity findByUUID(UUID uuid) {
		return electricPowerQualitySummaryRepository.findByUuid(uuid).orElse(null);
	}

	@Override
	public ElectricPowerQualitySummaryEntity findById(
			Long electricPowerQualitySummaryId) {
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
		// TODO: Implement findAllByUsagePoint query in repository
		return electricPowerQualitySummaryRepository.findByUsagePointEntity(usagePoint);
	}

	@Override
	public String feedFor(
			List<ElectricPowerQualitySummaryEntity> electricPowerQualitySummaries) {
		// TODO: Implement modern feed generation using DTOs
		logger.info("Generating feed for " + electricPowerQualitySummaries.size() + " electric power quality summaries");
		return null;
	}

	@Override
	public String entryFor(
			ElectricPowerQualitySummaryEntity electricPowerQualitySummary) {
		// TODO: Implement modern entry generation using DTOs
		logger.info("Generating entry for electric power quality summary: " + electricPowerQualitySummary.getId());
		return null;
	}

	@Override
	public void associateByUUID(UsagePointEntity usagePoint, UUID uuid) {
		ElectricPowerQualitySummaryEntity entity = electricPowerQualitySummaryRepository.findByUuid(uuid).orElse(null);
		if (entity != null) {
			entity.setUsagePointEntity(usagePoint);
			electricPowerQualitySummaryRepository.save(entity);
			logger.info("Associated electric power quality summary " + uuid + " with usage point " + usagePoint.getId());
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
		logger.info("Added electric power quality summary: " + electricPowerQualitySummary.getId());
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
			logger.error("Failed to import ElectricPowerQualitySummary resource", e);
			return null;
		}
	}


}
