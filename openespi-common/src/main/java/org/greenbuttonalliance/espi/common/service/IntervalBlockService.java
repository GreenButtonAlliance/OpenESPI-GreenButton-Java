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

import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface IntervalBlockService {

	List<IntervalBlockEntity> findAllByMeterReadingId(UUID meterReadingId);

	String feedFor(List<IntervalBlockEntity> intervalBlocks);

	IntervalBlockEntity findByURI(String uri);

	String entryFor(IntervalBlockEntity intervalBlock);

	void associateByUUID(MeterReadingEntity meterReading, UUID uuid);

	List<IntervalBlockEntity> findAllByMeterReading(MeterReadingEntity meterReading);

	IntervalBlockEntity save(IntervalBlockEntity intervalBlock);

	IntervalBlockEntity findById(UUID retailCustomerId, UUID usagePointId,
						   UUID meterReadingId, UUID intervalBlockId);

	void delete(IntervalBlockEntity intervalBlock);

	void add(IntervalBlockEntity intervalBlock);

	IntervalBlockEntity importResource(InputStream stream);

	IntervalBlockEntity findById(UUID intervalBlockId);

}
