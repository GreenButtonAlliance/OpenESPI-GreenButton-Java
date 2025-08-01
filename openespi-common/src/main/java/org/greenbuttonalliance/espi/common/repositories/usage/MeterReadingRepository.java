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

import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
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
public interface MeterReadingRepository extends JpaRepository<MeterReadingEntity, UUID> {

	// JpaRepository provides: save(), findById(), findAll(), deleteById(), etc.

	@Modifying
	@Transactional
	@Query("DELETE FROM MeterReadingEntity m WHERE m.id = :id")
	void deleteById(@Param("id") UUID id);

	@Query("SELECT m.id FROM MeterReadingEntity m")
	List<UUID> findAllIds();

	// findById is already provided by JpaRepository<MeterReadingEntity, UUID>
	// Optional<MeterReadingEntity> findById(UUID id) is inherited

	// deleteById is already provided by JpaRepository<MeterReadingEntity, UUID>
	// void deleteById(UUID id) is inherited

	// Custom method for createOrReplaceByUUID - should be implemented in service layer

	@Query("SELECT m FROM MeterReadingEntity m join m.relatedLinks link WHERE link.href = :href")
	List<MeterReadingEntity> findByRelatedHref(@Param("href") String href);

	@Query("SELECT readingType FROM ReadingTypeEntity readingType WHERE readingType.selfLink.href in (:relatedLinkHrefs)")
	List<Object> findAllRelated(@Param("relatedLinkHrefs") List<String> relatedLinkHrefs);

	@Query("SELECT m.id FROM MeterReadingEntity m WHERE m.usagePoint.id = :usagePointId")
	List<UUID> findAllIdsByUsagePointId(@Param("usagePointId") UUID usagePointId);

	@Query("SELECT DISTINCT m.id FROM UsagePointEntity u, MeterReadingEntity m WHERE u.retailCustomer.id = :o1Id AND m.usagePoint.id = :o2Id")
	List<UUID> findAllIdsByXpath2(@Param("o1Id") UUID o1Id, @Param("o2Id") UUID o2Id);

	@Query("SELECT DISTINCT m.id FROM  UsagePointEntity u, MeterReadingEntity m WHERE u.retailCustomer.id = :o1Id AND m.usagePoint.id = :o2Id AND m.id = :o3Id")
	Optional<UUID> findIdByXpath(@Param("o1Id") UUID o1Id, @Param("o2Id") UUID o2Id, @Param("o3Id") UUID o3Id);

}
