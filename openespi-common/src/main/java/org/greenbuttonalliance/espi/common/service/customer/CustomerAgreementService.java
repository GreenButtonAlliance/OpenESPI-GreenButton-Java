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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for CustomerAgreement management.
 *
 * Handles business logic for customer agreement operations including contract terms,
 * service agreements, and agreement lifecycle management.
 * Per Phase 24 guidelines, only ID-based operations on indexed fields are supported.
 */
public interface CustomerAgreementService {

    /**
     * Find all customer agreements.
     */
    List<CustomerAgreementEntity> findAll();

    /**
     * Find customer agreement by UUID.
     */
    Optional<CustomerAgreementEntity> findById(UUID id);

    /**
     * Save customer agreement.
     */
    CustomerAgreementEntity save(CustomerAgreementEntity customerAgreement);

    /**
     * Delete customer agreement by UUID.
     */
    void deleteById(UUID id);

    /**
     * Check if customer agreement exists by UUID.
     */
    boolean existsById(UUID id);

    /**
     * Count total customer agreements.
     */
    long count();
}
