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

import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository {
	void persist(IdentifiedObject resource);

	void flush();

	List<IdentifiedObject> findAllParentsByRelatedHref(String href,
			IdentifiedObject linkable);

	List<IdentifiedObject> findAllRelated(IdentifiedObject linkable);

	<T> T findByUUID(UUID uuid, Class<T> clazz);

	UsagePointEntity findByUUID(UUID uuid);

	@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
			jakarta.persistence.NoResultException.class,
			org.springframework.dao.EmptyResultDataAccessException.class })
	void update(UsagePointEntity resource);

	<T extends IdentifiedObject> T findById(Long id, Class<T> clazz);

	<T extends IdentifiedObject> List<UUID> findAllIds(Class<T> clazz);

	<T extends IdentifiedObject> List<UUID> findAllIdsByUsagePointId(
			UUID usagePointId, Class<T> clazz);

	<T extends IdentifiedObject> List<UUID> findAllIdsByXPath(UUID id1,
			Class<T> clazz);

	<T extends IdentifiedObject> List<UUID> findAllIdsByXPath(UUID id1,
			UUID id2, Class<T> clazz);

	<T extends IdentifiedObject> List<UUID> findAllIdsByXPath(UUID id1,
			UUID id2, UUID id3, Class<T> clazz);

	<T extends IdentifiedObject> List<UUID> findAllIdsByXPath(Class<T> clazz);

	<T extends IdentifiedObject> UUID findIdByXPath(UUID id1, Class<T> clazz);

	<T extends IdentifiedObject> UUID findIdByXPath(UUID id1, UUID id2,
			Class<T> clazz);

	<T extends IdentifiedObject> UUID findIdByXPath(UUID id1, UUID id2,
			UUID id3, Class<T> clazz);

	<T extends IdentifiedObject> UUID findIdByXPath(UUID id1, UUID id2,
			UUID id3, UUID id4, Class<T> clazz);

	<T extends IdentifiedObject> T findByResourceUri(String uri, Class<T> clazz);

	@Transactional
	<T extends IdentifiedObject> void deleteById(UUID id, Class<T> clazz);

	<T extends IdentifiedObject> void deleteByXPathId(UUID id1, UUID id2,
			Class<T> clazz);

	<T extends IdentifiedObject> void deleteByXPathId(UUID id1, UUID id2,
			UUID id3, Class<T> clazz);

	<T extends IdentifiedObject> void deleteByXPathId(UUID id1, UUID id2,
			UUID id3, UUID id4, Class<T> clazz);

	<T extends IdentifiedObject> T merge(IdentifiedObject resource);

}
