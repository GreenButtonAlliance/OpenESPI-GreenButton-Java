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
import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.TimeConfigurationRepository;
import org.greenbuttonalliance.espi.common.service.TimeConfigurationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TimeConfigurationServiceImpl implements TimeConfigurationService {

	private final TimeConfigurationRepository timeConfigurationRepository;

	public TimeConfigurationRepository getRepository() {
		return timeConfigurationRepository;
	}

	@Override
	public TimeConfigurationEntity findByUUID(UUID uuid) {
        return timeConfigurationRepository.findById(uuid).orElse(null);
	}

	public TimeConfigurationEntity findById(UUID timeConfigurationId) {
		return timeConfigurationRepository.findById(timeConfigurationId).orElse(null);
	}

	@Override
	public TimeConfigurationEntity save(TimeConfigurationEntity timeConfiguration) {
		return timeConfigurationRepository.save(timeConfiguration);
	}

	@Override
	public List<TimeConfigurationEntity> findAllByRetailCustomer(
			RetailCustomerEntity retailCustomer) {
		// TODO: Implement query to find time configurations by retail customer
		return new ArrayList<>();
	}

	@Override
	public void associateByUUID(UsagePointEntity usagePoint, UUID uuid) {
		TimeConfigurationEntity timeConfiguration = findByUUID(uuid);
		if (timeConfiguration != null && usagePoint != null) {
			usagePoint.setLocalTimeParameters(timeConfiguration);
			// Note: Application handles bidirectional relationship management
		}
	}

	@Override
	public TimeConfigurationEntity importTimeConfiguration(InputStream stream) {
		// TODO: Implement modern import logic using DTOs
		return null;
	}

	@Override
	public void deleteById(UUID timeConfigurationId) {
		timeConfigurationRepository.deleteById(timeConfigurationId);
	}

	@Override
	public UsagePointEntity getUsagePoint() {
		// TODO: Implement logic to get usage point
		return null;
	}

	// Legacy EntryType methods removed - modern architecture uses DTOs and export services

	@Override
	public void add(TimeConfigurationEntity timeConfiguration) {
		timeConfigurationRepository.save(timeConfiguration);
	}

	@Override
	public void delete(TimeConfigurationEntity timeConfiguration) {
		timeConfigurationRepository.deleteById(timeConfiguration.getId());
	}

	@Override
	public TimeConfigurationEntity importResource(InputStream stream) {
		try {
			// TODO: Implement modern JAXB import pattern using DTOs
			// Similar to other modernized services:
			// 1. Create JAXBContext for TimeConfigurationDto
			// 2. Unmarshal stream to DTO
			// 3. Use mapper to convert DTO to entity
			// 4. Save and return entity
			log.info("TimeConfiguration import using legacy method - needs modern DTO implementation");
			return null;
		} catch (Exception e) {
			log.error("Failed to import TimeConfiguration resource: " + e.getMessage(), e);
			return null;
		}
	}

}
