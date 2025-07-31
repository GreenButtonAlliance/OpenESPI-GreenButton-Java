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

import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SubscriptionService {

	SubscriptionEntity createSubscription(String username, Set<String> roles, String clientId);

	SubscriptionEntity findByHashedId(String hashedId);

	void setRepository(SubscriptionRepository subscriptionRepository);

	SubscriptionEntity save(SubscriptionEntity subscription);

	SubscriptionEntity findById(UUID subscriptionId);

	List<UUID> findUsagePointIds(UUID subscriptionId);

	SubscriptionEntity findByAuthorizationId(UUID id);

	SubscriptionEntity addUsagePoint(SubscriptionEntity subscription,
							   UsagePointEntity usagePoint);

	UUID findRetailCustomerId(UUID subscriptionId, UUID usagePointId);

}
