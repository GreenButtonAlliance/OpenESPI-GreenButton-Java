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

import org.greenbuttonalliance.espi.common.domain.customer.entity.StatementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for Statement entities.
 *
 * [extension] Billing statement for provided services.
 *
 * Provides standard CRUD operations via JpaRepository and ID-based relationship queries.
 * Per ESPI 4.0 compliance (Issue #28 Phase 19), non-ID queries removed to prevent
 * performance issues and ensure consistent API patterns.
 */
@Repository
public interface StatementRepository extends JpaRepository<StatementEntity, UUID> {

    /**
     * Find statements by customer ID.
     * Supports Controller APIs for navigating Statement → Customer relationships.
     *
     * @param customerId the customer UUID
     * @return list of statements for the customer
     */
    List<StatementEntity> findByCustomerId(UUID customerId);

    /**
     * Find statements by customer account ID.
     * Supports Controller APIs for navigating Statement → CustomerAccount relationships.
     *
     * @param customerAccountId the customer account UUID
     * @return list of statements for the customer account
     */
    List<StatementEntity> findByCustomerAccountId(UUID customerAccountId);

    /**
     * Find statements by customer agreement ID.
     * Supports Controller APIs for navigating Statement → CustomerAgreement relationships.
     *
     * @param customerAgreementId the customer agreement UUID
     * @return list of statements for the customer agreement
     */
    List<StatementEntity> findByCustomerAgreementId(UUID customerAgreementId);

    /**
     * Find statements by usage summary ID.
     * Supports Controller APIs for navigating Statement → UsageSummary relationships.
     *
     * @param usageSummaryId the usage summary UUID
     * @return list of statements for the usage summary
     */
    List<StatementEntity> findByUsageSummaryId(UUID usageSummaryId);
}
