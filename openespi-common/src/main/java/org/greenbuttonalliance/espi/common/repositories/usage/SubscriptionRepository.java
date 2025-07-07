/*
 *
 *         Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.repositories.usage;

import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
	// JpaRepository provides: save(), findAll(), findById(), deleteById(), etc.
	// Note: merge() functionality is handled by save() in Spring Data JPA

	Optional<SubscriptionEntity> findByHashedId(String hashedId);

	@Modifying
	@Transactional
	@Query("DELETE FROM SubscriptionEntity s WHERE s.id = :id")
	void deleteById(@Param("id") UUID id);

	Optional<SubscriptionEntity> findByUuid(UUID uuid);

	@Query("SELECT s FROM SubscriptionEntity s WHERE s.authorization.id = :authorizationId")
	Optional<SubscriptionEntity> findByAuthorizationId(@Param("authorizationId") UUID id);

	// Missing NamedQueries that need to be added:

	@Query("SELECT s.id FROM SubscriptionEntity s")
	List<UUID> findAllIds();

	@Query("SELECT s FROM SubscriptionEntity s WHERE s.retailCustomer.id = :retailCustomerId")
	List<SubscriptionEntity> findByRetailCustomerId(@Param("retailCustomerId") UUID retailCustomerId);

	@Query("SELECT s FROM SubscriptionEntity s WHERE s.applicationInformation.id = :applicationInformationId")
	List<SubscriptionEntity> findByApplicationInformationId(@Param("applicationInformationId") UUID applicationInformationId);

	@Query("SELECT s FROM SubscriptionEntity s WHERE s.authorization IS NOT NULL AND s.authorization.status = 'ACTIVE'")
	List<SubscriptionEntity> findActiveSubscriptions();

	@Query("SELECT DISTINCT s FROM SubscriptionEntity s JOIN s.usagePoints up WHERE up.id = :usagePointId")
	List<SubscriptionEntity> findByUsagePointId(@Param("usagePointId") UUID usagePointId);
}
