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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface RetailCustomerService {

	List<RetailCustomerEntity> findAll();

	RetailCustomerEntity findByHashedId(Long retailCustomerId);

	RetailCustomerEntity save(RetailCustomerEntity customer);

	RetailCustomerEntity findById(Long retailCustomerId);

	RetailCustomerEntity findById(String retailCustomerId);

	void add(RetailCustomerEntity retailCustomer);

	void delete(RetailCustomerEntity retailCustomer);

	RetailCustomerEntity importResource(InputStream stream);

	SubscriptionEntity associateByUUID(Long retailCustomerId, UUID uuId);

	RetailCustomerEntity findByUsername(String username);

}
