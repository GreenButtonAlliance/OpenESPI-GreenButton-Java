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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for CustomerAgreementEntity.
 * Phase 24: CustomerAgreement schema compliance.
 *
 * Only uses inherited JpaRepository methods to avoid H2 keyword conflicts.
 * Per Phase 18 pattern: findById, findAll, save, delete, count, existsById
 */
@Repository
public interface CustomerAgreementRepository extends JpaRepository<CustomerAgreementEntity, UUID> {
    // Use only inherited methods to avoid H2 keyword conflicts
    // No custom query methods required
}
