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


import jakarta.xml.bind.JAXBException;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface UsagePointService {
 
	List<UsagePointEntity> findAllByRetailCustomer(RetailCustomerEntity customer);

	void createOrReplaceByUUID(UsagePointEntity usagePoint);

	void associateByUUID(RetailCustomerEntity retailCustomer, UUID uuid);

	UsagePointEntity findByUUID(UUID uuid);

	UsagePointEntity findByHashedId(String usagePointHashedId);

	List<UsagePointEntity> findAllUpdatedFor(SubscriptionEntity subscription);

	void deleteByHashedId(String usagePointHashedId);

	List<UUID> findAllIdsForRetailCustomer(UUID id);

	String feedFor(List<UsagePointEntity> usagePoints) throws JAXBException;

	String entryFor(UsagePointEntity usagePoint);

	List<UsagePointEntity> findAllByRetailCustomer(UUID retailCustomerId);

	UsagePointEntity save(UsagePointEntity usagePoint);
 
	UsagePointEntity findById(UUID usagePointId);

	UsagePointEntity findById(UUID retailCustomerId, UUID usagePointId);

	// Legacy EntryType methods removed - incompatible with Spring Boot 3.5

	void add(UsagePointEntity usagePoint);

	void delete(UsagePointEntity usagePoint);
 
	UsagePointEntity importResource(InputStream stream);

}
