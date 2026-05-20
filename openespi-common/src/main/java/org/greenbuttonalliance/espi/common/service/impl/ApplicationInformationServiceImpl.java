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
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ApplicationInformationMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.service.ApplicationInformationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
@RequiredArgsConstructor
public class ApplicationInformationServiceImpl implements
		ApplicationInformationService {

	private final ApplicationInformationRepository applicationInformationRepository;
	private final ApplicationInformationMapper applicationInformationMapper;
	private final ApplicationInformationExportService applicationInformationExportService;

	@Override
	public ApplicationInformationEntity findByClientId(String clientId) {
		Assert.notNull(clientId, "clientID is required");
		
		// Use Spring Data JPA repository to find by client ID
		Optional<ApplicationInformationEntity> entityOpt = applicationInformationRepository.findByClientId(clientId);
		
		if (entityOpt.isPresent()) {
			log.info("Found ApplicationInformation entity for clientId: " + clientId);
			return entityOpt.get();
		} else {
			log.warn("ApplicationInformation not found for clientId: " + clientId);
			return null;
		}
	}

	@Override
	public ApplicationInformationEntity findByDataCustodianClientId(
			String dataCustodianClientId) {
		Assert.notNull(dataCustodianClientId, "dataCustodianClientId is required");
		
		log.info("Finding ApplicationInformation by dataCustodianClientId: " + dataCustodianClientId);
		return applicationInformationRepository.findByDataCustodianId(dataCustodianClientId).orElse(null);
	}

	@Override
	public ApplicationInformationEntity importResource(InputStream stream) {
		try {
			// Use JAXB to parse XML stream to DTO
			jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(ApplicationInformationDto.class);
			jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
			ApplicationInformationDto dto = (ApplicationInformationDto) unmarshaller.unmarshal(stream);
			
			// Convert DTO to Entity using mapper
			ApplicationInformationEntity entity = applicationInformationMapper.toEntity(dto);
			
			// Save and return entity
			return applicationInformationRepository.save(entity);
			
		} catch (Exception e) {
			log.error("Failed to import ApplicationInformation resource", e);
			return null;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ApplicationInformationEntity> findAll() {
		return applicationInformationRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public ApplicationInformationEntity findById(UUID id) {
		return applicationInformationRepository.findById(id).orElse(null);
	}

	@Override
	public ApplicationInformationEntity save(ApplicationInformationEntity entity) {
		return applicationInformationRepository.save(entity);
	}

	@Override
	public void deleteById(UUID id) {
		applicationInformationRepository.deleteById(id);
	}

	@Override
	public void export(List<ApplicationInformationEntity> entities, OutputStream outputStream) {
		List<ApplicationInformationDto> dtos = entities.stream()
				.map(applicationInformationMapper::toDto)
				.toList();
		applicationInformationExportService.exportDto(dtos, outputStream);
	}

	@Override
	public void export(ApplicationInformationEntity entity, OutputStream outputStream) {
		applicationInformationExportService.exportDto(applicationInformationMapper.toDto(entity), outputStream);
	}

	@Override
	public ApplicationInformationEntity fromDto(ApplicationInformationDto dto) {
		return applicationInformationMapper.toEntity(dto);
	}
}
