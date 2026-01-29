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
import org.greenbuttonalliance.espi.common.domain.customer.entity.EndDeviceEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.EndDeviceRepository;
import org.greenbuttonalliance.espi.common.service.customer.EndDeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for EndDevice data management.
 *
 * Provides business logic for EndDevice schema operations including metering devices,
 * sensors, and other monitoring assets. Per ESPI 4.0 API specification, only basic
 * CRUD operations are supported.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class EndDeviceServiceImpl implements EndDeviceService {

    private final EndDeviceRepository endDeviceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EndDeviceEntity> findAll() {
        return endDeviceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EndDeviceEntity> findById(UUID id) {
        return endDeviceRepository.findById(id);
    }

    @Override
    public EndDeviceEntity save(EndDeviceEntity endDevice) {
        // Generate UUID if not present
        if (endDevice.getId() == null) {
            endDevice.setId(UUID.randomUUID());
        }
        return endDeviceRepository.save(endDevice);
    }

    @Override
    public void deleteById(UUID id) {
        endDeviceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return endDeviceRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countEndDevices() {
        return endDeviceRepository.count();
    }
}
