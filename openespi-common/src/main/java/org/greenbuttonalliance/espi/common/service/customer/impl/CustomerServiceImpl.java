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
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerRepository;
import org.greenbuttonalliance.espi.common.service.customer.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for Customer PII data management.
 *
 * Provides business logic for Customer schema operations with proper PII handling.
 * Per ESPI 4.0 API specification, only basic CRUD operations are supported.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerEntity> findAll() {
        return customerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerEntity> findById(UUID id) {
        return customerRepository.findById(id);
    }

    @Override
    public CustomerEntity save(CustomerEntity customer) {
        // Generate UUID if not present
        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID());
        }
        return customerRepository.save(customer);
    }

    @Override
    public void deleteById(UUID id) {
        customerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return customerRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCustomers() {
        return customerRepository.count();
    }
}