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

package org.greenbuttonalliance.espi.common.service;

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;

import java.io.InputStream;
import java.util.List;

public interface ApplicationInformationService {

	/**
	 * @param kind
	 *            String indicating [ DATA_CUSTODIAN_ADMIN | THIRD_PARTY |
	 *            UPLOAD_ADMIN ]
	 * @return List of ApplicationInformationEntity Resources
	 */
	public List<ApplicationInformationEntity> findByKind(String kind);

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

}
