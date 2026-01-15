/*
 *
 *         Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common.repositories.usage;

import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Modern Spring Data JPA repository for UsagePoint entities.
 * Replaces the legacy UsagePointRepositoryImpl with modern Spring Data patterns.
 * <p>
 * Phase 16c Repository Cleanup:
 * - Removed queries on non-indexed columns (uri)
 * - Removed full table scan queries (findAllIds)
 * - Converted @Query to Spring Data JPA derived queries where possible
 * - Use built-in JpaRepository methods (existsById, deleteById) instead of custom queries
 * - All remaining queries use indexed columns for optimal performance
 */
@Repository
public interface UsagePointRepository extends JpaRepository<UsagePointEntity, UUID> {

    /**
     * Find all usage points for a specific retail customer.
     * Uses indexed column: retail_customer_id
     * Converted to Spring Data JPA derived query method (Phase 16c).
     *
     * @param retailCustomerId the retail customer ID
     * @return list of usage points for the customer
     */
    List<UsagePointEntity> findAllByRetailCustomerId(Long retailCustomerId);

    /**
     * Find usage point by related href.
     * Uses indexed relationship: usage_point_related_links(usage_point_id)
     *
     * @param href the related link href
     * @return optional usage point matching the href
     */
    @Query("SELECT up FROM UsagePointEntity up JOIN up.relatedLinks rl WHERE rl.href = :href")
    Optional<UsagePointEntity> findByRelatedHref(@Param("href") String href);

    /**
     * Find all usage points updated after a given timestamp.
     * Uses indexed column: updated
     *
     * @param lastUpdate the last update timestamp
     * @return list of usage points updated after the given time
     */
    List<UsagePointEntity> findAllByUpdatedAfter(LocalDateTime lastUpdate);

    /**
     * Find all usage point IDs for a specific retail customer.
     * Uses indexed column: retail_customer_id
     *
     * @param retailCustomerId the retail customer ID
     * @return list of usage point IDs for the customer
     */
    @Query("SELECT up.id FROM UsagePointEntity up WHERE up.retailCustomer.id = :retailCustomerId")
    List<UUID> findAllIdsByRetailCustomerId(@Param("retailCustomerId") Long retailCustomerId);

    // Note: Use built-in existsById(UUID id) instead of custom existsByUuid
    // Note: Use built-in deleteById(UUID id) instead of custom deleteByUuid
}
