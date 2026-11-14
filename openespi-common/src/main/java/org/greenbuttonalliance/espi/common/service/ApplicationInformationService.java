/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.common.service;

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public interface ApplicationInformationService {

	/**
	 * Find an ApplicationInformationEntity resource by using it's clientId.
	 * 
	 * @param clientId
	 *            String uniquely identifying a specific ApplicationInformationEntity.clientId
	 * @return an ApplicationInformationEntity resource
	 */
	public ApplicationInformationEntity findByClientId(String clientId);

	/**
	 * Find an Application Information resource by using it's dataCustodianId.
	 * 
	 * @param dataCustodianClientId
	 * @return an ApplicationInformationEntity resource
	 */
	public ApplicationInformationEntity findByDataCustodianClientId(
			String dataCustodianClientId);

	/**
	 * Import and XML stream, unmarshalling into an ApplicationInformationEntity
	 * resource
	 * 
	 * @param stream
	 * @return an ApplicationInformationEntity resource
	 */
	public ApplicationInformationEntity importResource(InputStream stream);

	List<ApplicationInformationEntity> findAll();

	ApplicationInformationEntity findById(UUID id);

	ApplicationInformationEntity save(ApplicationInformationEntity entity);

	void deleteById(UUID id);

	/**
	 * Exports the entities as an Atom feed (#247).
	 *
	 * @param feedHref absolute URL of the feed (its self link, and the source of its UUID v5 id)
	 */
	void export(String feedHref, List<ApplicationInformationEntity> entities, OutputStream outputStream);

	void export(ApplicationInformationEntity entity, OutputStream outputStream);

	ApplicationInformationEntity fromDto(ApplicationInformationDto dto);
}
