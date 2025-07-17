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

import org.greenbuttonalliance.espi.common.domain.legacy.ApplicationInformation;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.service.ApplicationInformationService;
import org.greenbuttonalliance.espi.common.service.ImportService;
import org.greenbuttonalliance.espi.common.service.ResourceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
public class ApplicationInformationServiceImpl implements
		ApplicationInformationService {

	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private ApplicationInformationRepository applicationInformationRepository;

	@Autowired
	private ResourceService resourceService;

	@Autowired
	private ImportService importService;

	@Override
	public List<ApplicationInformation> findByKind(String kind) {
		// For now, this legacy method returns empty list
		// Modern applications should use repository methods directly with entities
		logger.warn("Legacy findByKind method called - consider using repository directly");
		return new ArrayList<>();
	}

	@Override
	public ApplicationInformation findByClientId(String clientId) {
		Assert.notNull(clientId, "clientID is required");
		
		// Use Spring Data JPA repository to find by client ID
		Optional<ApplicationInformationEntity> entityOpt = applicationInformationRepository.findByClientId(clientId);
		
		if (entityOpt.isPresent()) {
			// TODO: Convert entity to legacy domain object when needed
			// For now, return null to maintain backward compatibility
			logger.info("Found ApplicationInformation entity for clientId: " + clientId);
			return null; // Placeholder until full migration
		} else {
			logger.warn("ApplicationInformation not found for clientId: " + clientId);
			return null;
		}
	}

	@Override
	public ApplicationInformation findByDataCustodianClientId(
			String dataCustodianClientId) {
		Assert.notNull(dataCustodianClientId, "dataCustodianClientId is required");
		
		// For now, this legacy method returns null
		// Modern applications should use repository methods directly with entities
		logger.warn("Legacy findByDataCustodianClientId method called - consider using repository directly");
		
		return null;
	}

	@Override
	public ApplicationInformation importResource(InputStream stream) {

		ApplicationInformation applicationInformation = null;
		try {
			importService.importData(stream, null);
			// TODO: Implement modern import logic for ApplicationInformation
			// Legacy getContent().getApplicationInformation() no longer supported
			applicationInformation = null; // Placeholder
		} catch (Exception e) {
			logger.error("Failed to import ApplicationInformation resource", e);
 		}
		return applicationInformation;
	}


	public void setResourceService(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	public ResourceService getResourceService() {
		return this.resourceService;
	}

	public void setImportService(ImportService importService) {
		this.importService = importService;
	}

	public ImportService getImportService() {
		return this.importService;
	}

}
