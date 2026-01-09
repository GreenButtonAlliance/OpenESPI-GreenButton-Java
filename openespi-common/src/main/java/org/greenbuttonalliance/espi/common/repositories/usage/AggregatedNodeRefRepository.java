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

import org.greenbuttonalliance.espi.common.domain.usage.AggregatedNodeRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AggregatedNodeRefEntity.
 * <p>
 * AggregatedNodeRef extends Object (not IdentifiedObject) in ESPI 4.0 XSD,
 * so it uses Long ID (not UUID).
 */
@Repository
public interface AggregatedNodeRefRepository extends JpaRepository<AggregatedNodeRefEntity, Long> {

    @Query("SELECT a.id FROM AggregatedNodeRefEntity a")
    List<Long> findAllIds();

    @Query("SELECT a FROM AggregatedNodeRefEntity a WHERE a.usagePoint.id = :usagePointId")
    List<AggregatedNodeRefEntity> findAllByUsagePointId(@Param("usagePointId") UUID usagePointId);
}