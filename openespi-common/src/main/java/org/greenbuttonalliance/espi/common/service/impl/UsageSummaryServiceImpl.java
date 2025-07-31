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

package org.greenbuttonalliance.espi.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsageSummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository;
import org.greenbuttonalliance.espi.common.service.UsageSummaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Modern UsageSummary service implementation using entity classes.
 * Created by Donald F. Coffin on 06/28/2019 at 23:04
 * Modernized for Spring Boot 3.5 compatibility.
 */

@Slf4j
@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
@RequiredArgsConstructor
public class UsageSummaryServiceImpl implements UsageSummaryService {

    private final UsageSummaryRepository usageSummaryRepository;
    private final UsageSummaryMapper usageSummaryMapper;

    @Override
    public UsageSummaryEntity findByUUID(UUID uuid) {
        return usageSummaryRepository.findById(uuid).orElse(null);
    }

    @Override
    public UsageSummaryEntity findById(UUID usageSummaryId) {
        return usageSummaryRepository.findById(usageSummaryId).orElse(null);
    }

    @Override
    public UsageSummaryEntity save(UsageSummaryEntity usageSummary) {
        return usageSummaryRepository.save(usageSummary);
    }

    @Override
    public String feedFor(List<UsageSummaryEntity> usageSummaries) {
        // TODO: Implement modern feed generation using DTOs
        log.info("Generating feed for " + usageSummaries.size() + " usage summaries");
        return null;
    }

    @Override
    public String entryFor(UsageSummaryEntity usageSummary) {
        // TODO: Implement modern entry generation using DTOs
        log.info("Generating entry for usage summary: " + usageSummary.getId());
        return null;
    }

    @Override
    public void associateByUUID(UsagePointEntity usagePoint, UUID uuid) {
        UsageSummaryEntity entity = usageSummaryRepository.findById(uuid).orElse(null);
        if (entity != null) {
            entity.setUsagePoint(usagePoint);
            usageSummaryRepository.save(entity);
            log.info("Associated usage summary " + uuid + " with usage point " + usagePoint.getId());
        }
    }

    @Override
    public void delete(UsageSummaryEntity usageSummary) {
        usageSummaryRepository.deleteById(usageSummary.getId());
        log.info("Deleted usage summary: " + usageSummary.getId());
    }

    @Override
    public List<UsageSummaryEntity> findAllByUsagePoint(UsagePointEntity usagePoint) {
        return usageSummaryRepository.findByUsagePoint(usagePoint);
    }

    @Override
    public void add(UsageSummaryEntity usageSummary) {
        usageSummaryRepository.save(usageSummary);
        log.info("Added usage summary: " + usageSummary.getId());
    }

    @Override
    public UsageSummaryEntity importResource(InputStream stream) {
        try {
            // Use JAXB to parse XML stream to DTO
            jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(UsageSummaryDto.class);
            jakarta.xml.bind.Unmarshaller unmarshaller = context.createUnmarshaller();
            UsageSummaryDto dto = (UsageSummaryDto) unmarshaller.unmarshal(stream);
            
            // Convert DTO to Entity using mapper
            UsageSummaryEntity entity = usageSummaryMapper.toEntity(dto);
            
            // Save and return entity
            return usageSummaryRepository.save(entity);
            
        } catch (Exception e) {
            log.error("Failed to import UsageSummary resource", e);
            return null;
        }
    }
}
