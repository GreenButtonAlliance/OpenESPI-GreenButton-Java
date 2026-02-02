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

import org.greenbuttonalliance.espi.common.domain.customer.entity.StatementEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Statement management.
 *
 * [extension] Billing statement for provided services.
 *
 * Provides standard CRUD operations and ID-based relationship queries.
 * Per ESPI 4.0 compliance (Issue #28 Phase 19), non-ID queries removed to prevent
 * performance issues and ensure consistent API patterns.
 */
public interface StatementService {

    /**
     * Find all statements.
     *
     * @return list of all statements
     */
    List<StatementEntity> findAll();

    /**
     * Find statement by ID.
     *
     * @param id the statement UUID
     * @return Optional containing the statement if found
     */
    Optional<StatementEntity> findById(UUID id);

    /**
     * Find statements by customer ID.
     *
     * @param customerId the customer UUID
     * @return list of statements for the customer
     */
    List<StatementEntity> findByCustomerId(UUID customerId);

    /**
     * Find statements by customer account ID.
     *
     * @param customerAccountId the customer account UUID
     * @return list of statements for the customer account
     */
    List<StatementEntity> findByCustomerAccountId(UUID customerAccountId);

    /**
     * Find statements by customer agreement ID.
     *
     * @param customerAgreementId the customer agreement UUID
     * @return list of statements for the customer agreement
     */
    List<StatementEntity> findByCustomerAgreementId(UUID customerAgreementId);

    /**
     * Find statements by usage summary ID.
     *
     * @param usageSummaryId the usage summary UUID
     * @return list of statements for the usage summary
     */
    List<StatementEntity> findByUsageSummaryId(UUID usageSummaryId);

    /**
     * Save statement.
     *
     * @param statement the statement to save
     * @return the saved statement
     */
    StatementEntity save(StatementEntity statement);

    /**
     * Delete statement by ID.
     *
     * @param id the statement UUID to delete
     */
    void deleteById(UUID id);
}
