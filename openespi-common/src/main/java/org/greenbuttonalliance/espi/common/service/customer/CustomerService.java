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

package org.greenbuttonalliance.espi.common.service.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Customer PII data management.
 *
 * Handles Customer schema operations with proper separation from Usage data.
 * Customer data contains Personally Identifiable Information (PII) and requires
 * special handling according to NAESB REQ.21 ESPI standards.
 * <p>
 * Per ESPI 4.0 API specification, only basic CRUD operations are supported.
 * Removed methods: findByCustomerName, findByKind, findByPucNumber, findVipCustomers,
 * findCustomersWithSpecialNeeds, findByLocale, findByPriorityRange, findByOrganisationName, countByKind
 */
public interface CustomerService {

    /**
     * Find all customers.
     */
    List<CustomerEntity> findAll();

    /**
     * Find customer by ID.
     */
    Optional<CustomerEntity> findById(UUID id);

    /**
     * Save customer.
     */
    CustomerEntity save(CustomerEntity customer);

    /**
     * Delete customer by ID.
     */
    void deleteById(UUID id);

    /**
     * Check if customer exists by ID.
     */
    boolean existsById(UUID id);

    /**
     * Count total customers.
     */
    long countCustomers();
}