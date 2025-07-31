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
import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Customer PII data management.
 * 
 * Handles Customer schema operations with proper separation from Usage data.
 * Customer data contains Personally Identifiable Information (PII) and requires
 * special handling according to NAESB REQ.21 ESPI standards.
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
     * Find customer by customer name.
     */
    Optional<CustomerEntity> findByCustomerName(String customerName);

    /**
     * Find customers by kind.
     */
    List<CustomerEntity> findByKind(CustomerKind kind);

    /**
     * Find customer by PUC number.
     */
    Optional<CustomerEntity> findByPucNumber(String pucNumber);

    /**
     * Find VIP customers.
     */
    List<CustomerEntity> findVipCustomers();

    /**
     * Find customers with special needs.
     */
    List<CustomerEntity> findCustomersWithSpecialNeeds();

    /**
     * Find customers by locale.
     */
    List<CustomerEntity> findByLocale(String locale);

    /**
     * Find customers by priority range.
     */
    List<CustomerEntity> findByPriorityRange(Integer minPriority, Integer maxPriority);

    /**
     * Find customers by organisation name.
     */
    List<CustomerEntity> findByOrganisationName(String organisationName);

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

    /**
     * Count customers by kind.
     */
    long countByKind(CustomerKind kind);
}