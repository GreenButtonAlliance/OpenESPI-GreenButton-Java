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

package org.greenbuttonalliance.espi.common.service;

import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ReadingTypeRepository;

import java.io.InputStream;
import java.util.UUID;

public interface ReadingTypeService {

	ReadingTypeEntity findByUUID(UUID uuid);

	String feedFor(ReadingTypeEntity readingType);

	String entryFor(ReadingTypeEntity readingType);

	void associateByUUID(MeterReadingEntity meterReading, UUID uuid);

	void deleteById(long readingTypeId);

	void setReadingTypeRepository(ReadingTypeRepository repository);

	ReadingTypeEntity save(ReadingTypeEntity readingType);

	ReadingTypeEntity findById(Long readingTypeId);

	void add(ReadingTypeEntity readingType);

	void delete(ReadingTypeEntity readingType);

	ReadingTypeEntity importResource(InputStream stream);

}
