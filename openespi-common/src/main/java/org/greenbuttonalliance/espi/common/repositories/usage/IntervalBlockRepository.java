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

import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
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
public interface IntervalBlockRepository extends JpaRepository<IntervalBlockEntity, UUID> {

	// JpaRepository provides: save(), findById(), findAll(), deleteById(), etc.

	Optional<IntervalBlockEntity> findByUuid(UUID uuid);

	@Query("SELECT i.id FROM IntervalBlockEntity i")
	List<UUID> findAllIds();

	@Modifying
	@Transactional
	@Query("DELETE FROM IntervalBlockEntity i WHERE i.id = :id")
	void deleteById(@Param("id") UUID id);

	@Modifying
	@Transactional
	@Query("DELETE FROM IntervalBlockEntity i WHERE i.id = :uuid")
	void deleteByUuid(@Param("uuid") UUID uuid);

	// Custom method for createOrReplaceByUUID - should be implemented in service layer

	@Query("SELECT i FROM IntervalBlockEntity i WHERE i.meterReading.id = :meterReadingId")
	List<IntervalBlockEntity> findAllByMeterReadingId(@Param("meterReadingId") UUID meterReadingId);

	@Query("SELECT i.id FROM IntervalBlockEntity i WHERE i.meterReading.usagePoint.id = :usagePointId")
	List<UUID> findAllIdsByUsagePointId(@Param("usagePointId") UUID usagePointId);

	@Query("SELECT DISTINCT i.id FROM UsagePointEntity u, MeterReadingEntity m, IntervalBlockEntity i WHERE u.retailCustomer.id = :o1Id AND m.usagePoint.id = :o2Id AND i.meterReading.id = :o3Id")
	List<UUID> findAllIdsByXpath3(@Param("o1Id") UUID o1Id, @Param("o2Id") UUID o2Id, @Param("o3Id") UUID o3Id);

	@Query("SELECT DISTINCT i.id FROM UsagePointEntity u, MeterReadingEntity m, IntervalBlockEntity i WHERE u.retailCustomer.id = :o1Id AND m.usagePoint.id = :o2Id AND i.meterReading.id = :o3Id AND i.id = :o4Id")
	Optional<UUID> findIdByXpath(@Param("o1Id") UUID o1Id, @Param("o2Id") UUID o2Id, @Param("o3Id") UUID o3Id, @Param("o4Id") UUID o4Id);
}
