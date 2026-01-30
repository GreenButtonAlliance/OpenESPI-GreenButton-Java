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
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.MeterRepository;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.greenbuttonalliance.espi.common.service.customer.MeterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for Meter management.
 * <p>
 * Per ESPI 4.0 compliance: Uses UUID v5 generation (NO random fallback).
 * Provides ONLY 6 CRUD methods.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MeterServiceImpl implements MeterService {

    private final MeterRepository repository;
    private final EspiIdGeneratorService idGenerator;

    @Override
    @Transactional
    public MeterEntity save(MeterEntity meter) {
        if (meter.getId() == null) {
            // ❌ NO random UUID fallback - ESPI requires UUID v5
            if (meter.getSerialNumber() == null) {
                throw new IllegalArgumentException(
                    "SerialNumber is required for Meter UUID generation");
            }
            UUID deterministicId = idGenerator.generateEntityId(
                "Meter", meter.getSerialNumber());
            meter.setId(deterministicId);
            log.debug("Generated UUID v5 for Meter: {}", deterministicId);
        }
        return repository.save(meter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeterEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
