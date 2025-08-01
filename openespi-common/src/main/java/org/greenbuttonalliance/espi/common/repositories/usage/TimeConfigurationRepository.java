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

import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
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
public interface TimeConfigurationRepository extends JpaRepository<TimeConfigurationEntity, UUID> {

	@Modifying
	@Transactional
	@Query("DELETE FROM TimeConfigurationEntity t WHERE t.id = :id")
	void deleteById(@Param("id") UUID id);

	@Query("SELECT t.id FROM TimeConfigurationEntity t")
	List<UUID> findAllIds();

	@Query("SELECT usagePoint.localTimeParameters.id FROM UsagePointEntity usagePoint WHERE usagePoint.id = :usagePointId")
	List<UUID> findAllIdsByUsagePointId(@Param("usagePointId") UUID usagePointId);

	@Query("SELECT DISTINCT t.id FROM TimeConfigurationEntity t")
	List<UUID> findAllIdsByXpath0();

	@Query("SELECT DISTINCT t.id FROM TimeConfigurationEntity t WHERE t.id = :o1Id")
	Optional<UUID> findIdsByXpath(@Param("o1Id") UUID o1Id);

}
