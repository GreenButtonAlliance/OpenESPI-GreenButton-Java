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

import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for ProgramDateIdMappings management.
 *
 * Handles business logic for customer energy efficiency program date mappings.
 * Only ID-based queries are supported.
 */
public interface ProgramDateIdMappingsService {

    /**
     * Find program date ID mappings by ID.
     *
     * @param id the program date ID mappings UUID
     * @return optional containing the program date ID mappings if found
     */
    Optional<ProgramDateIdMappingsEntity> findById(UUID id);

    /**
     * Save program date ID mappings.
     *
     * @param programDateIdMappings the program date ID mappings to save
     * @return the saved program date ID mappings
     */
    ProgramDateIdMappingsEntity save(ProgramDateIdMappingsEntity programDateIdMappings);

    /**
     * Delete program date ID mappings by ID.
     *
     * @param id the program date ID mappings UUID to delete
     */
    void deleteById(UUID id);
}
