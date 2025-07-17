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

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.legacy.TimeConfiguration;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.legacy.atom.EntryType;
import org.greenbuttonalliance.espi.common.repositories.usage.TimeConfigurationRepository;
import org.greenbuttonalliance.espi.common.utils.EntryTypeIterator;
import org.greenbuttonalliance.espi.common.utils.ExportFilter;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface TimeConfigurationService {
 
	TimeConfiguration findByUUID(UUID uuid);

	List<TimeConfiguration> findAllByRetailCustomer(RetailCustomer retailCustomer);

	String feedFor(List<TimeConfiguration> timeConfiguration);

	String entryFor(TimeConfiguration timeConfiguration);

	void associateByUUID(UsagePoint usagePoint, UUID uuid);

	TimeConfiguration importTimeConfiguration(InputStream stream);

	void deleteById(long timeConfigurationId);

	UsagePoint getUsagePoint();
 
	void setRepository(TimeConfigurationRepository timeConfigurationRepository);

	TimeConfiguration save(TimeConfiguration timeConfiguration);
 
	TimeConfiguration findById(Long timeConfigurationId);

	EntryType findEntryType(Long retailCustomerId, Long usagePointId, Long timeConfigurationId, ExportFilter params);

	EntryTypeIterator findEntryTypeIterator(Long retailCustomerId, Long usagePointId, ExportFilter params);

	void add(TimeConfiguration timeConfiguration);

	void delete(TimeConfiguration timeConfiguration);
 
	TimeConfiguration importResource(InputStream stream);

}
