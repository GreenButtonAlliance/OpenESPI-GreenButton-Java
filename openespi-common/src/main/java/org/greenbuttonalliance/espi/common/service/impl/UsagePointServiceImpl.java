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

import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.UsagePointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Modern UsagePoint service implementation using entity classes.
 * Legacy ATOM feed and entry processing removed for Spring Boot 3.5 compatibility.
 */
@Slf4j
@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
@RequiredArgsConstructor
public class UsagePointServiceImpl implements UsagePointService {

	private final UsagePointRepository usagePointRepository;
	private final UsagePointMapper usagePointMapper;


	@Override
	public List<UsagePointEntity> findAllByRetailCustomer(RetailCustomerEntity customer) {
		return usagePointRepository.findAllByRetailCustomerId(customer.getId());
	}

	@Override
	public UsagePointEntity findById(UUID usagePointId) {
		return usagePointRepository.findById(usagePointId).orElse(null);
	}

	@Override
	public UsagePointEntity findById(UUID retailCustomerId, UUID usagePointId) {
		// TODO: Implement scoped query for retailCustomer.usagePoint
		return usagePointRepository.findById(usagePointId).orElse(null);
	}

	@Override
	public UsagePointEntity save(UsagePointEntity up) {
		return this.usagePointRepository.save(up);
	}

	@Override
	public void createOrReplaceByUUID(UsagePointEntity usagePoint) {
		UsagePointEntity existing = usagePointRepository.findById(usagePoint.getId()).orElse(null);
		if (existing != null) {
			// Update existing entity with new values
			usagePoint.setId(existing.getId());
		}
		usagePointRepository.save(usagePoint);
		log.info("Created or replaced usage point with UUID: " + usagePoint.getId());
	}

	@Override
	public void associateByUUID(RetailCustomerEntity retailCustomer, UUID uuid) {
  UsagePointEntity usagePoint = usagePointRepository.findById(uuid).orElse(null);
		if (usagePoint != null) {
			usagePoint.setRetailCustomer(retailCustomer);
			usagePointRepository.save(usagePoint);
			log.info("Associated usage point " + uuid + " with retail customer " + retailCustomer.getId());
		}
	}

	@Override
	public UsagePointEntity findByUUID(UUID uuid) {
		return usagePointRepository.findById(uuid).orElse(null);
	}

	@Override
	public UsagePointEntity findByHashedId(String usagePointHashedId) {
		return findByUUID(UUID.fromString(usagePointHashedId));
	}

	@Override
	public List<UsagePointEntity> findAllUpdatedFor(SubscriptionEntity subscription) {
		// TODO: Implement query to find usage points updated after subscription timestamp
		return usagePointRepository.findAllUpdatedAfter(subscription.getLastUpdate());
	}

	@Override
	public void deleteByHashedId(String usagePointHashedId) {
		UsagePointEntity usagePoint = findByHashedId(usagePointHashedId);
		if (usagePoint != null) {
			usagePointRepository.deleteById(usagePoint.getId());
		}
	}

	@Override
	public List<UUID> findAllIdsForRetailCustomer(UUID id) {
		return usagePointRepository
				.findAllIdsByRetailCustomerId(id);
	}

	@Override
	public String feedFor(List<UsagePointEntity> usagePoints) throws JAXBException {
		// TODO: Implement modern feed generation using DTOs
		log.info("Generating feed for " + usagePoints.size() + " usage points");
		return null;
	}

	@Override
	public String entryFor(UsagePointEntity usagePoint) {
		// TODO: Implement modern entry generation using DTOs
		log.info("Generating entry for usage point: " + usagePoint.getId());
		return null;
	}

	@Override
	public List<UsagePointEntity> findAllByRetailCustomer(UUID retailCustomerId) {
		return usagePointRepository.findAllByRetailCustomerId(retailCustomerId);
	}

	@Override
	public void add(UsagePointEntity usagePoint) {
		usagePointRepository.save(usagePoint);
		log.info("Added usage point: " + usagePoint.getId());
	}

	@Override
	public void delete(UsagePointEntity usagePoint) {
		usagePointRepository.deleteById(usagePoint.getId());
		log.info("Deleted usage point: " + usagePoint.getId());
	}

	@Override
	public UsagePointEntity importResource(InputStream stream) {
		try {
			// Use JAXB to parse XML stream to DTO
			jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(UsagePointDto.class);
			jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
			UsagePointDto dto = (UsagePointDto) unmarshaller.unmarshal(stream);
			
			// Convert DTO to Entity using mapper
			UsagePointEntity entity = usagePointMapper.toEntity(dto);
			
			// Save and return entity
			return usagePointRepository.save(entity);
			
		} catch (Exception e) {
			log.error("Failed to import UsagePoint resource", e);
			return null;
		}
	}

	// Legacy methods removed - incompatible with Spring Boot 3.5
	// The following methods used legacy EntryType and are no longer supported:
	// - findEntryType(Long retailCustomerId, Long usagePointId)
	// - findEntryTypeIterator()
	// - findEntryType(Long usagePointId)  
	// - findEntryTypeIterator(Long retailCustomerId)
	// - findEntryTypeIterator(Long retailCustomerId, Long usagePointId)
}