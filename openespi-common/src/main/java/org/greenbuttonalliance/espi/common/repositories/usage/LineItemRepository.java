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

import org.greenbuttonalliance.espi.common.domain.usage.LineItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for LineItemEntity.
 * <p>
 * LineItem extends Object (not IdentifiedObject) in ESPI 4.0 XSD,
 * so it uses Long ID (not UUID).
 * <p>
 * Following established pattern from Phases 7-9, keeping only queries using indexed columns:
 * - findAllIds() uses primary key
 * - findAllByUsageSummaryId() uses foreign key index (usage_summary_id)
 */
@Repository
public interface LineItemRepository extends JpaRepository<LineItemEntity, Long> {

	@Query("SELECT li.id FROM LineItemEntity li")
	List<Long> findAllIds();

	@Query("SELECT li FROM LineItemEntity li WHERE li.usageSummary.id = :usageSummaryId ORDER BY li.dateTime")
	List<LineItemEntity> findAllByUsageSummaryId(@Param("usageSummaryId") UUID usageSummaryId);
}