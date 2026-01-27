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
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAgreementRepository;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.greenbuttonalliance.espi.common.service.customer.CustomerAgreementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for CustomerAgreement management.
 *
 * Provides business logic for customer agreement operations including contract terms,
 * service agreements, and agreement lifecycle management.
 * Per Phase 24 guidelines, only ID-based operations on indexed fields are supported.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerAgreementServiceImpl implements CustomerAgreementService {

    private final CustomerAgreementRepository customerAgreementRepository;
    private final EspiIdGeneratorService espiIdGeneratorService;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAgreementEntity> findAll() {
        return customerAgreementRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerAgreementEntity> findById(UUID id) {
        return customerAgreementRepository.findById(id);
    }

    @Override
    public CustomerAgreementEntity save(CustomerAgreementEntity customerAgreement) {
        // Generate deterministic UUID v5 if not present
        if (customerAgreement.getId() == null) {
            // Require agreementId to be set for deterministic UUID v5 generation
            // Per Green Button Alliance guidelines, UUID v5 must use repeatable, unchangeable attributes
            if (customerAgreement.getAgreementId() == null || customerAgreement.getAgreementId().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "agreementId must be set before saving a new CustomerAgreement to generate deterministic UUID v5"
                );
            }
            UUID uuid5 = espiIdGeneratorService.generateEntityId("CustomerAgreement", customerAgreement.getAgreementId());
            customerAgreement.setId(uuid5);
        }
        return customerAgreementRepository.save(customerAgreement);
    }

    @Override
    public void deleteById(UUID id) {
        customerAgreementRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return customerAgreementRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return customerAgreementRepository.count();
    }
}
