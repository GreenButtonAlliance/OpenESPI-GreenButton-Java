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
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.dto.usage.AuthorizationDto;
import org.greenbuttonalliance.espi.common.mapper.usage.AuthorizationMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
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
public class AuthorizationServiceImpl implements AuthorizationService {

	private final AuthorizationRepository authorizationRepository;
	private final UsagePointRepository usagePointRepository;
	private final DtoExportService dtoExportService;
	private final AuthorizationMapper authorizationMapper;

	@Override
	public List<AuthorizationEntity> findAllByRetailCustomerId(UUID retailCustomerId) {
		return authorizationRepository
				.findAllByRetailCustomerId(retailCustomerId);
	}

	@Override
	public List<UUID> findAllIdsByApplicationInformationId(
			UUID applicationInformationId) {
		return authorizationRepository
				.findAllIdsByApplicationInformationId(applicationInformationId);
	}

	@Override
 public AuthorizationEntity findByUUID(UUID uuid) {
        return authorizationRepository.findById(uuid).orElse(null);
    }

	@Override
	public AuthorizationEntity createAuthorizationEntity(SubscriptionEntity subscription,
			String accessToken) {
		// TODO: Implement modern authorization creation
		log.info("Creating authorization entity for subscription: " + subscription.getId());
		AuthorizationEntity authorization = new AuthorizationEntity();
		authorization.setAccessToken(accessToken);
		authorization.setSubscription(subscription);
		return authorizationRepository.save(authorization);
	}

	@Override
	public AuthorizationEntity findByState(String state) {
		return authorizationRepository.findByState(state).orElse(null);
	}

	@Override
	public AuthorizationEntity findByScope(String scope, UUID retailCustomerId) {
		return authorizationRepository.findByScope(scope, retailCustomerId).orElse(null);
	}

	@Override
	public List<AuthorizationEntity> findAll() {
		return authorizationRepository.findAll();
	}

	@Override
	public String entryFor(AuthorizationEntity authorization) {
		try {
			// TODO: Implement modern DTO export for authorization
			log.info("Generating entry for authorization: " + authorization.getId());
			return null;
			
		} catch (Exception e) {
			log.error("Failed to generate entry for authorization: " + e.getMessage(), e);
			return null;
		}
	}

	@Override
	public AuthorizationEntity findByURI(String uri) {
		List<AuthorizationEntity> results = authorizationRepository.findByResourceUri(uri);
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public String feedFor(List<AuthorizationEntity> authorizations) {
		try {
			// TODO: Implement modern DTO feed export for authorizations
			log.info("Generating feed for " + authorizations.size() + " authorizations");
			return null;
			
		} catch (Exception e) {
			log.error("Failed to generate feed for authorizations: " + e.getMessage(), e);
			return null;
		}
	}

	// persistence management services
	@Override
	public AuthorizationEntity save(AuthorizationEntity authorization) {
		return authorizationRepository.save(authorization);
	}

	// accessor services

	@Override
	public AuthorizationEntity findById(UUID authorizationId) {
		return this.authorizationRepository.findById(authorizationId).orElse(null);
	}


	@Override
	public void add(AuthorizationEntity authorization) {
		authorizationRepository.save(authorization);
		log.info("Added authorization: " + authorization.getId());
	}

	@Override
	public void delete(AuthorizationEntity authorization) {
		authorizationRepository.deleteById(authorization.getId());
		log.info("Deleted authorization: " + authorization.getId());
	}

	// import-exportResource services
	@Override
	public AuthorizationEntity importResource(InputStream stream) {
		try {
			// Use JAXB to parse XML stream to DTO
			jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(AuthorizationDto.class);
			jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
			AuthorizationDto dto = (AuthorizationDto) unmarshaller.unmarshal(stream);
			
			// Convert DTO to Entity using mapper
			AuthorizationEntity entity = authorizationMapper.toEntity(dto);
			
			// Save and return entity
			return authorizationRepository.save(entity);
			
		} catch (Exception e) {
			log.error("Failed to import Authorization resource", e);
			return null;
		}
	}

	@Override
	public AuthorizationEntity findById(UUID retailCustomerId, UUID authorizationId) {
		return this.authorizationRepository.findById(authorizationId).orElse(null);
	}

	@Override
	public AuthorizationEntity findByAccessToken(String accessToken) {
		return authorizationRepository.findByAccessToken(accessToken).orElse(null);
	}

	@Override
	public AuthorizationEntity findByRefreshToken(String refreshToken) {
		return authorizationRepository.findByRefreshToken(refreshToken).orElse(null);
	}

	@Override
	public List<UUID> findAllIdsByBulkId(String thirdParty, UUID bulkId) {
		return authorizationRepository.findAllIdsByBulkId(thirdParty, bulkId.toString());
	}

}
