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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.dto.usage.RetailCustomerDto;
import org.greenbuttonalliance.espi.common.mapper.usage.RetailCustomerMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.service.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
public class RetailCustomerServiceImpl implements RetailCustomerService {

	private final Log logger = LogFactory.getLog(getClass());

	private final RetailCustomerRepository retailCustomerRepository;
	private final RetailCustomerMapper retailCustomerMapper;
	private final AuthorizationService authorizationService;
	private final SubscriptionService subscriptionService;
	private final UsagePointService usagePointService;

	public RetailCustomerServiceImpl(RetailCustomerRepository retailCustomerRepository,
									 RetailCustomerMapper retailCustomerMapper,
									 AuthorizationService authorizationService,
									 SubscriptionService subscriptionService,
									 UsagePointService usagePointService) {
		this.retailCustomerRepository = retailCustomerRepository;
		this.retailCustomerMapper = retailCustomerMapper;
		this.authorizationService = authorizationService;
		this.subscriptionService = subscriptionService;
		this.usagePointService = usagePointService;
	}

	@Override
	public List<RetailCustomerEntity> findAll() {
		return retailCustomerRepository.findAll();
	}

	@Override
	public RetailCustomerEntity save(RetailCustomerEntity customer) {
		if (customer.getUuid() == null) {
			customer.setUuid(UUID.randomUUID());
		}
		return retailCustomerRepository.save(customer);
	}

	@Override
	public RetailCustomerEntity findById(Long id) {
		return retailCustomerRepository.findById(id).orElse(null);
	}

	@Override
	public RetailCustomerEntity findById(String retailCustomerId) {
		try {
			Long id = Long.parseLong(retailCustomerId);
			return retailCustomerRepository.findById(id).orElse(null);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public RetailCustomerEntity findByHashedId(Long retailCustomerId) {
		return findById(retailCustomerId);
	}

	@Override
	public RetailCustomerEntity findByUsername(String username) {
		try {
			return retailCustomerRepository.findByUsername(username).orElse(null);
		} catch (EmptyResultDataAccessException x) {
			logger.warn("Unable to find user with username: " + username);
			return null;
		}
	}

	@Override
	public void add(RetailCustomerEntity retailCustomer) {
		retailCustomerRepository.save(retailCustomer);
		logger.info("Added retail customer: " + retailCustomer.getId());
	}

	@Override
	public void delete(RetailCustomerEntity retailCustomer) {
		retailCustomerRepository.deleteById(retailCustomer.getId());
		logger.info("Deleted retail customer: " + retailCustomer.getId());
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
			logger.error("RetailCustomerService.importResource failed: " + e.getMessage());
			return null;
		}
	}

	@Override
	public SubscriptionEntity associateByUUID(Long retailCustomerId, UUID uuid) {
		// TODO: Implement modern association logic using entity classes
		logger.info("Associating usage point UUID " + uuid + " with retail customer " + retailCustomerId);
		
		// Use the UsagePointService to handle the association
		RetailCustomerEntity retailCustomer = findById(retailCustomerId);
		if (retailCustomer != null) {
			usagePointService.associateByUUID(retailCustomer, uuid);
		}
		
		// TODO: Return appropriate subscription entity
		return null;
	}

}
