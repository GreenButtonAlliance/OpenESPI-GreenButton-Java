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

package org.greenbuttonalliance.espi.common.repositories.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.ServiceLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for ServiceLocationEntity entities.
 * <p>
 * Manages real estate location data including addresses, access methods, and service delivery points.
 * <p>
 * Only contains queries on indexed fields to ensure efficient database access.
 * Removed non-indexed queries in Phase 23 cleanup (findLocationsThatNeedInspection,
 * findLocationsWithAccessProblems, findByMainAddressStreetContaining, findByDirectionContaining,
 * findByPhone1AreaCode).
 */
@Repository
public interface ServiceLocationRepository extends JpaRepository<ServiceLocationEntity, UUID> {

    /**
     * Find service locations by outage block.
     * Outage block is indexed for efficient lookups.
     *
     * @param outageBlock the outage block identifier
     * @return list of service locations in the outage block
     */
    @Query("SELECT sl FROM ServiceLocationEntity sl WHERE sl.outageBlock = :outageBlock")
    List<ServiceLocationEntity> findByOutageBlock(@Param("outageBlock") String outageBlock);

    /**
     * Find service locations by location type.
     * Type is indexed for efficient classification lookups.
     *
     * @param type the location type classification
     * @return list of service locations of the given type
     */
    @Query("SELECT sl FROM ServiceLocationEntity sl WHERE sl.type = :type")
    List<ServiceLocationEntity> findByType(@Param("type") String type);

    /**
     * Find service locations by geo info reference.
     * Geo info reference is indexed for efficient geographic lookups.
     *
     * @param geoInfoReference the geographical information reference
     * @return list of service locations with the given geo info reference
     */
    @Query("SELECT sl FROM ServiceLocationEntity sl WHERE sl.geoInfoReference = :geoInfoReference")
    List<ServiceLocationEntity> findByGeoInfoReference(@Param("geoInfoReference") String geoInfoReference);
}