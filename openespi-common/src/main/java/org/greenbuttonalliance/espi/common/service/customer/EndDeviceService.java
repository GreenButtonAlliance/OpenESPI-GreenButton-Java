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

import org.greenbuttonalliance.espi.common.domain.customer.entity.EndDeviceEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for EndDevice data management.
 *
 * Handles EndDevice schema operations for metering devices, sensors, and other assets
 * that perform metering and monitoring functions. EndDevice represents physical or virtual
 * devices that perform one or more end device functions such as metering, load management,
 * connect/disconnect, and monitoring.
 * <p>
 * Per ESPI 4.0 API specification, only basic CRUD operations are supported.
 */
public interface EndDeviceService {

    /**
     * Find all end devices.
     */
    List<EndDeviceEntity> findAll();

    /**
     * Find end device by ID.
     */
    Optional<EndDeviceEntity> findById(UUID id);

    /**
     * Save end device.
     */
    EndDeviceEntity save(EndDeviceEntity endDevice);

    /**
     * Delete end device by ID.
     */
    void deleteById(UUID id);

    /**
     * Check if end device exists by ID.
     */
    boolean existsById(UUID id);

    /**
     * Count total end devices.
     */
    long countEndDevices();
}
