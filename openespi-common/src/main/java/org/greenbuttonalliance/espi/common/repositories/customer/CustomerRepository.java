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

package org.greenbuttonalliance.espi.common.repositories.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for Customer entities.
 * <p>
 * Provides Customer schema specific query methods for Customer PII data access.
 * Customer data is separated from Usage data for privacy and compliance reasons.
 * <p>
 * Per ESPI 4.0 API specification, only findById is supported (provided by JpaRepository).
 * Removed queries: findByCustomerName, findByKind, findByPucNumber, findVipCustomers,
 * findCustomersWithSpecialNeeds, findByLocale, findByPriorityRange, findByOrganisationName
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
    // Only default JpaRepository methods are supported (findById, findAll, save, delete, etc.)
}