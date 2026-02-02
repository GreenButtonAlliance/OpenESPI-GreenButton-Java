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
import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.ProgramDateIdMappingsRepository;
import org.greenbuttonalliance.espi.common.service.customer.ProgramDateIdMappingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for ProgramDateIdMappings management.
 * <p>
 * Provides business logic for customer energy efficiency program date mappings.
 * Only ID-based queries are supported.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProgramDateIdMappingsServiceImpl implements ProgramDateIdMappingsService {

    private final ProgramDateIdMappingsRepository programDateIdMappingsRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProgramDateIdMappingsEntity> findById(UUID id) {
        return programDateIdMappingsRepository.findById(id);
    }

    @Override
    public ProgramDateIdMappingsEntity save(ProgramDateIdMappingsEntity programDateIdMappings) {
        return programDateIdMappingsRepository.save(programDateIdMappings);
    }

    @Override
    public void deleteById(UUID id) {
        programDateIdMappingsRepository.deleteById(id);
    }
}
