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

import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Created by Donald F. Coffin on 06/28/2019 at 23:01
 */
public interface UsageSummaryService {

    UsageSummaryEntity findByUUID(UUID uuid);

    List<UsageSummaryEntity> findAllByUsagePoint(UsagePointEntity usagePoint);

    String feedFor(List<UsageSummaryEntity> usageSummaries);

    String entryFor(UsageSummaryEntity usageSummary);

    void associateByUUID(UsagePointEntity usagePoint, UUID uuid);

    void persist(UsageSummaryEntity usageSummary);

    UsageSummaryEntity findById(Long usageSummaryId);

    void add(UsageSummaryEntity usageSummary);

    void delete(UsageSummaryEntity usageSummary);

    UsageSummaryEntity importResource(InputStream stream);
}
