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

import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
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
public interface ReadingTypeRepository extends JpaRepository<ReadingTypeEntity, UUID> {

	// JpaRepository provides: save(), findById(), findAll(), deleteById(), etc.

	@Modifying
	@Transactional
	@Query("DELETE FROM ReadingTypeEntity r WHERE r.id = :id")
	void deleteById(@Param("id") UUID id);

	@Query("SELECT r.id FROM ReadingTypeEntity r")
	List<UUID> findAllIds();

	// findById is already provided by JpaRepository<ReadingTypeEntity, UUID>
	// Optional<ReadingTypeEntity> findById(UUID id) is inherited

	// deleteById is already provided by JpaRepository<ReadingTypeEntity, UUID>
	// void deleteById(UUID id) is inherited

	// Custom method for createOrReplaceByUUID - should be implemented in service layer

	@Query("SELECT meterReading.readingType.id FROM MeterReadingEntity meterReading WHERE meterReading.usagePoint.id = :usagePointId")
	List<UUID> findAllIdsByUsagePointId(@Param("usagePointId") UUID usagePointId);

	@Query("SELECT DISTINCT r.id FROM ReadingTypeEntity r")
	List<UUID> findAllIdsByXpath0();

	@Query("SELECT DISTINCT r.id FROM ReadingTypeEntity r WHERE r.id = :o1Id")
	Optional<UUID> findIdByXpath(@Param("o1Id") UUID o1Id);

}
