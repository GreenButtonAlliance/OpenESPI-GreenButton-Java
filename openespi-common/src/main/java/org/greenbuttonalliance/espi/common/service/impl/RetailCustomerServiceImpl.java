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
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.dto.usage.RetailCustomerDto;
import org.greenbuttonalliance.espi.common.mapper.usage.RetailCustomerMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.common.service.UsagePointService;
import org.springframework.dao.EmptyResultDataAccessException;
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
public class RetailCustomerServiceImpl implements RetailCustomerService {

	private final RetailCustomerRepository retailCustomerRepository;
	private final RetailCustomerMapper retailCustomerMapper;
	private final UsagePointService usagePointService;

	@Override
	public List<RetailCustomerEntity> findAll() {
		return retailCustomerRepository.findAll();
	}

	@Override
	public RetailCustomerEntity save(RetailCustomerEntity customer) {
		if (customer.getId() == null) {
			customer.setId(UUID.randomUUID());
		}
		return retailCustomerRepository.save(customer);
	}

	@Override
	public RetailCustomerEntity findById(UUID id) {
		return retailCustomerRepository.findById(id).orElse(null);
	}

	@Override
	public RetailCustomerEntity findById(String retailCustomerId) {
		try {
			UUID id = UUID.fromString(retailCustomerId);
			return retailCustomerRepository.findById(id).orElse(null);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public RetailCustomerEntity findByHashedId(UUID retailCustomerId) {
		return findById(retailCustomerId);
	}

	@Override
	public RetailCustomerEntity findByUsername(String username) {
		try {
			return retailCustomerRepository.findByUsername(username).orElse(null);
		} catch (EmptyResultDataAccessException x) {
			log.warn("Unable to find user with username: " + username);
			return null;
		}
	}

	@Override
	public void add(RetailCustomerEntity retailCustomer) {
		retailCustomerRepository.save(retailCustomer);
		log.info("Added retail customer: " + retailCustomer.getId());
	}

	@Override
	public void delete(RetailCustomerEntity retailCustomer) {
		retailCustomerRepository.deleteById(retailCustomer.getId());
		log.info("Deleted retail customer: " + retailCustomer.getId());
	}

	@Override
	public RetailCustomerEntity importResource(InputStream stream) {
		try {
			// Use JAXB to parse XML stream to DTO
			jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(RetailCustomerDto.class);
			jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
			RetailCustomerDto dto = (RetailCustomerDto) unmarshaller.unmarshal(stream);
			
			// Convert DTO to Entity using mapper
			RetailCustomerEntity entity = retailCustomerMapper.toEntity(dto);
			
			// Save and return entity
			return retailCustomerRepository.save(entity);
			
		} catch (Exception e) {
			// Security: Log error without exposing sensitive customer data
			log.error("RetailCustomerService.importResource failed: " + e.getMessage());
			return null;
		}
	}

	@Override
	public SubscriptionEntity associateByUUID(UUID retailCustomerId, UUID uuid) {
		// TODO: Implement modern association logic using entity classes
		log.info("Associating usage point UUID " + uuid + " with retail customer " + retailCustomerId);
		
		// Use the UsagePointService to handle the association
		RetailCustomerEntity retailCustomer = findById(retailCustomerId);
		if (retailCustomer != null) {
			usagePointService.associateByUUID(retailCustomer, uuid);
		}
		
		// TODO: Return appropriate subscription entity
		return null;
	}

}
