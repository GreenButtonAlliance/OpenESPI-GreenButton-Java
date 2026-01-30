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

import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Meter management.
 * <p>
 * Per ESPI 4.0 compliance: ONLY 6 CRUD methods (no custom queries).
 * Handles physical metering device operations.
 */
public interface MeterService {

    /**
     * Find all meters.
     *
     * @return list of all meters
     */
    List<MeterEntity> findAll();

    /**
     * Find meter by ID.
     *
     * @param id meter UUID
     * @return optional meter entity
     */
    Optional<MeterEntity> findById(UUID id);

    /**
     * Save meter with UUID v5 generation.
     * If meter.id is null, generates deterministic UUID v5 from serialNumber.
     *
     * @param meter meter entity to save
     * @return saved meter entity
     * @throws IllegalArgumentException if serialNumber is null when ID generation is needed
     */
    MeterEntity save(MeterEntity meter);

    /**
     * Delete meter by ID.
     *
     * @param id meter UUID to delete
     */
    void deleteById(UUID id);

    /**
     * Check if meter exists by ID.
     *
     * @param id meter UUID
     * @return true if meter exists
     */
    boolean existsById(UUID id);

    /**
     * Count total meters.
     *
     * @return total count of meters
     */
    long count();
}