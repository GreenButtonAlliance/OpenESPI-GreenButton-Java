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
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for RetailCustomer operations.
 * RetailCustomer is an application-specific correlation table (not part of ESPI standard).
 */
@Slf4j
@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
@RequiredArgsConstructor
public class RetailCustomerServiceImpl implements RetailCustomerService {

	private final RetailCustomerRepository retailCustomerRepository;

	@Override
	public List<RetailCustomerEntity> findAll() {
		return retailCustomerRepository.findAll();
	}

	@Override
	public RetailCustomerEntity findById(Long id) {
		return retailCustomerRepository.findById(id).orElse(null);
	}

	@Override
	public RetailCustomerEntity save(RetailCustomerEntity customer) {
		return retailCustomerRepository.save(customer);
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

}
