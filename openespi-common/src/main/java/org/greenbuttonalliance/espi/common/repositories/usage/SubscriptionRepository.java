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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SubscriptionEntity using Spring Data JPA query derivation.
 *
 * <p>All queries are automatically generated from method names by Spring Data JPA.
 * No explicit @Query annotations needed.</p>
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
	// JpaRepository provides: save(), findAll(), findById(), deleteById(), etc.

	/**
	 * Finds a subscription by its hashed ID.
	 * Used for API access where the hashed ID is exposed externally.
	 *
	 * @param hashedId the hashed identifier
	 * @return the subscription if found
	 */
	Optional<SubscriptionEntity> findByHashedId(String hashedId);

	/**
	 * Finds the subscriptions backed by an authorization.
	 * An authorization is the aggregate root for one or two subscriptions
	 * (Energy and, when granted, Customer/PII). Uses index: idx_subscription_authorization
	 *
	 * @param id the authorization UUID
	 * @return the subscriptions for that authorization (0, 1, or 2)
	 */
	List<SubscriptionEntity> findByAuthorization_Id(UUID id);

	/**
	 * Finds all subscriptions for a retail customer.
	 * Uses index: idx_subscription_retail_customer
	 *
	 * @param id the retail customer ID
	 * @return list of subscriptions
	 */
	List<SubscriptionEntity> findByRetailCustomer_Id(Long id);

	/**
	 * Finds all subscriptions for an application.
	 * Uses index: idx_subscription_application
	 *
	 * @param id the application information UUID
	 * @return list of subscriptions
	 */
	List<SubscriptionEntity> findByApplicationInformation_Id(UUID id);

	/**
	 * Returns the ids of the UsagePoints granted to a subscription, projected directly so callers
	 * (e.g. the resource-server scope resolver, #119) can resolve a subscription's granted set
	 * without traversing the lazy {@code usagePoints} collection outside a transaction.
	 *
	 * @param subscriptionId the subscription UUID
	 * @return the granted UsagePoint ids (empty if none / unknown subscription)
	 */
	@Query("select up.id from SubscriptionEntity s join s.usagePoints up where s.id = :subscriptionId")
	List<UUID> findUsagePointIdsBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
}
