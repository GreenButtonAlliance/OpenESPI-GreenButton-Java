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

package org.greenbuttonalliance.espi.common.service.customer.impl;

import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.customer.entity.StatementEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.StatementRepository;
import org.greenbuttonalliance.espi.common.service.customer.StatementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for Statement management.
 *
 * [extension] Billing statement for provided services.
 *
 * Provides standard CRUD operations and ID-based relationship queries.
 * Per ESPI 4.0 compliance (Issue #28 Phase 19), non-ID queries removed to prevent
 * performance issues and ensure consistent API patterns.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StatementServiceImpl implements StatementService {

    private final StatementRepository statementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StatementEntity> findAll() {
        return statementRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StatementEntity> findById(UUID id) {
        return statementRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementEntity> findByCustomerId(UUID customerId) {
        return statementRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementEntity> findByCustomerAccountId(UUID customerAccountId) {
        return statementRepository.findByCustomerAccountId(customerAccountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementEntity> findByCustomerAgreementId(UUID customerAgreementId) {
        return statementRepository.findByCustomerAgreementId(customerAgreementId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementEntity> findByUsageSummaryId(UUID usageSummaryId) {
        return statementRepository.findByUsageSummaryId(usageSummaryId);
    }

    @Override
    public StatementEntity save(StatementEntity statement) {
        return statementRepository.save(statement);
    }

    @Override
    public void deleteById(UUID id) {
        statementRepository.deleteById(id);
    }
}
