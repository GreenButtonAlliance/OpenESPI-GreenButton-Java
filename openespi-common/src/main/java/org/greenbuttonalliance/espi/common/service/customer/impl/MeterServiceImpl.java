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
import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.MeterRepository;
import org.greenbuttonalliance.espi.common.service.customer.MeterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for Meter management.
 * 
 * Provides business logic for physical metering device operations.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MeterServiceImpl implements MeterService {

    private final MeterRepository meterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findAll() {
        return meterRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeterEntity> findById(UUID id) {
        return meterRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeterEntity> findByUuid(String uuid) {
        return meterRepository.findById(UUID.fromString(uuid));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeterEntity> findBySerialNumber(String serialNumber) {
        return meterRepository.findBySerialNumber(serialNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findByFormNumber(String formNumber) {
        return meterRepository.findByFormNumber(formNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findByUtcNumber(String utcNumber) {
        return meterRepository.findByUtcNumber(utcNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findVirtualMeters() {
        return meterRepository.findVirtualMeters();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findPhysicalMeters() {
        return meterRepository.findPhysicalMeters();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findPanDevices() {
        return meterRepository.findPanDevices();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findByAmrSystem(String amrSystem) {
        return meterRepository.findByAmrSystem(amrSystem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findByInstallCode(String installCode) {
        return meterRepository.findByInstallCode(installCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findByIntervalLengthGreaterThan(Long seconds) {
        return meterRepository.findByIntervalLengthGreaterThan(seconds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findCriticalMeters() {
        return meterRepository.findCriticalMeters();
    }

    @Override
    public MeterEntity save(MeterEntity meter) {
        // Generate UUID if not present
        if (meter.getId() == null) {
            meter.setId(UUID.randomUUID());
        }
        return meterRepository.save(meter);
    }

    @Override
    public void deleteById(UUID id) {
        meterRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySerialNumber(String serialNumber) {
        return meterRepository.findBySerialNumber(serialNumber).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public long countMeters() {
        return meterRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countVirtualMeters() {
        return meterRepository.findVirtualMeters().size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPhysicalMeters() {
        return meterRepository.findPhysicalMeters().size();
    }
}