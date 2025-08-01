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

import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectricPowerQualitySummaryRepository extends JpaRepository<ElectricPowerQualitySummaryEntity, UUID> {

	// JpaRepository provides: save(), findById(), findAll(), deleteById(), etc.

	// findById is already provided by JpaRepository<ElectricPowerQualitySummaryEntity, UUID>
	// Optional<ElectricPowerQualitySummaryEntity> findById(UUID id) is inherited

	List<ElectricPowerQualitySummaryEntity> findByUsagePoint(UsagePointEntity usagePoint);

	@Query("SELECT e.id FROM ElectricPowerQualitySummaryEntity e")
	List<UUID> findAllIds();

	@Modifying
	@Transactional
	@Query("DELETE FROM ElectricPowerQualitySummaryEntity e WHERE e.id = :id")
	void deleteById(@Param("id") UUID id);

	// deleteById is already provided by JpaRepository<ElectricPowerQualitySummaryEntity, UUID>
	// void deleteById(UUID id) is inherited

	// Custom method for createOrReplaceByUUID - should be implemented in service layer

	@Query("SELECT e.id FROM ElectricPowerQualitySummaryEntity e WHERE e.usagePoint.id = :usagePointId")
	List<UUID> findAllIdsByUsagePointId(@Param("usagePointId") UUID usagePointId);

	@Query("SELECT e.id FROM UsagePointEntity u, ElectricPowerQualitySummaryEntity e WHERE u.retailCustomer.id = :o1Id AND e.usagePoint.id = :o2Id")
	List<UUID> findAllIdsByXpath2(@Param("o1Id") UUID o1Id, @Param("o2Id") UUID o2Id);

	@Query("SELECT e.id FROM UsagePointEntity u, ElectricPowerQualitySummaryEntity e WHERE u.retailCustomer.id = :o1Id AND e.usagePoint.id = :o2Id AND e.id = :o3Id")
	Optional<UUID> findIdByXpath(@Param("o1Id") UUID o1Id, @Param("o2Id") UUID o2Id, @Param("o3Id") UUID o3Id);

}
