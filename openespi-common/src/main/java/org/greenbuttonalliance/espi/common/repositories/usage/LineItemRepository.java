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

@Repository
public interface LineItemRepository extends JpaRepository<LineItemEntity, UUID> {
	// JpaRepository provides: save(), findAll(), findById(), deleteById(), etc.
	// Note: merge() functionality is handled by save() in Spring Data JPA

	// All 12 original NamedQueries from LineItemEntity:

	@Query("SELECT li FROM LineItemEntity li WHERE li.usageSummary.id = :electricPowerUsageSummaryId ORDER BY li.dateTime")
	List<LineItemEntity> findByElectricPowerUsageSummaryId(@Param("electricPowerUsageSummaryId") UUID electricPowerUsageSummaryId);

	@Query("SELECT li FROM LineItemEntity li WHERE li.usageSummary.id = :usageSummaryId ORDER BY li.dateTime")
	List<LineItemEntity> findByUsageSummaryId(@Param("usageSummaryId") UUID usageSummaryId);

	@Query("SELECT li FROM LineItemEntity li WHERE li.dateTime >= :startTime AND li.dateTime <= :endTime ORDER BY li.dateTime")
	List<LineItemEntity> findByDateTimeRange(@Param("startTime") Long startTime, @Param("endTime") Long endTime);

	@Query("SELECT li FROM LineItemEntity li WHERE li.amount >= :minAmount AND li.amount <= :maxAmount ORDER BY li.amount DESC")
	List<LineItemEntity> findByAmountRange(@Param("minAmount") Long minAmount, @Param("maxAmount") Long maxAmount);

	@Query("SELECT li FROM LineItemEntity li WHERE LOWER(li.note) LIKE LOWER(CONCAT('%', :searchText, '%')) ORDER BY li.dateTime")
	List<LineItemEntity> findByNoteContaining(@Param("searchText") String searchText);

	@Query("SELECT li.id FROM LineItemEntity li")
	List<UUID> findAllIds();

	@Query("SELECT SUM(li.amount) FROM LineItemEntity li WHERE li.usageSummary.id = :electricPowerUsageSummaryId")
	Long sumAmountsByElectricPowerUsageSummary(@Param("electricPowerUsageSummaryId") UUID electricPowerUsageSummaryId);

	@Query("SELECT SUM(li.amount) FROM LineItemEntity li WHERE li.usageSummary.id = :usageSummaryId")
	Long sumAmountsByUsageSummary(@Param("usageSummaryId") UUID usageSummaryId);

	@Query("SELECT COUNT(li) FROM LineItemEntity li WHERE li.usageSummary.id = :electricPowerUsageSummaryId")
	Long countByElectricPowerUsageSummary(@Param("electricPowerUsageSummaryId") UUID electricPowerUsageSummaryId);

	@Query("SELECT COUNT(li) FROM LineItemEntity li WHERE li.usageSummary.id = :usageSummaryId")
	Long countByUsageSummary(@Param("usageSummaryId") UUID usageSummaryId);
}