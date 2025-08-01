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

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface AuthorizationService {
	// residue from random stories
	List<AuthorizationEntity> findAllByRetailCustomerId(UUID retailCustomerId);

	/**
	 * @param applicationInformationId
	 * @return List<UUID> a list of all authorizationIds that are visible from
	 *         the input applicationInformationId
	 */
	List<UUID> findAllIdsByApplicationInformationId(
			UUID applicationInformationId);

	AuthorizationEntity createAuthorizationEntity(SubscriptionEntity subscription,
									  String accessToken);

	AuthorizationEntity findByState(String state);

	AuthorizationEntity findByScope(String scope, UUID retailCustomerId);

	AuthorizationEntity findByAccessToken(String accessToken);

	List<AuthorizationEntity> findAll();

	String feedFor(List<AuthorizationEntity> authorizations);

	String entryFor(AuthorizationEntity authorization);

	AuthorizationEntity findByURI(String uri);

	// persistence management services
	AuthorizationEntity save(AuthorizationEntity authorization);

	// accessor services
	AuthorizationEntity findById(UUID authorizationId);

	void add(AuthorizationEntity authorization);

	void delete(AuthorizationEntity authorization);

	// import-exportResource services
	AuthorizationEntity importResource(InputStream stream);

	AuthorizationEntity findById(UUID retailCustomerId, UUID authorizationId);

	AuthorizationEntity findByUUID(UUID uuid);

	AuthorizationEntity findByRefreshToken(String refreshToken);

	List<UUID> findAllIdsByBulkId(String thirdParty, UUID bulkId);

}
