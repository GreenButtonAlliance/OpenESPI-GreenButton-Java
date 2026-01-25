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
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAccountRepository;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.greenbuttonalliance.espi.common.service.customer.CustomerAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for CustomerAccount management.
 *
 * Provides business logic for customer account operations including billing,
 * payment tracking, and account status management.
 * Per Phase 18 guidelines, only ID-based operations on indexed fields are supported.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerAccountServiceImpl implements CustomerAccountService {

    private final CustomerAccountRepository customerAccountRepository;
    private final EspiIdGeneratorService espiIdGeneratorService;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAccountEntity> findAll() {
        return customerAccountRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerAccountEntity> findById(UUID id) {
        return customerAccountRepository.findById(id);
    }

    @Override
    public CustomerAccountEntity save(CustomerAccountEntity customerAccount) {
        // Generate UUID v5 if not present
        if (customerAccount.getId() == null) {
            // Generate UUID v5 using accountId as the name component
            String nameComponent = customerAccount.getAccountId() != null
                ? customerAccount.getAccountId()
                : "CustomerAccount-" + System.currentTimeMillis();
            UUID uuid5 = espiIdGeneratorService.generateSubscriptionId("CustomerAccount", nameComponent);
            customerAccount.setId(uuid5);
        }
        return customerAccountRepository.save(customerAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return customerAccountRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return customerAccountRepository.count();
    }
}
