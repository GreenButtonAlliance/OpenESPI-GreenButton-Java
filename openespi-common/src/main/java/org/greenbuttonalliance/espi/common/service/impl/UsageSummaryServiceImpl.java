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

import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsageSummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.UsageSummaryRepository;
import org.greenbuttonalliance.espi.common.service.UsageSummaryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modern UsageSummary service implementation using entity classes.
 * Created by Donald F. Coffin on 06/28/2019 at 23:04
 * Modernized for Spring Boot 3.5 compatibility.
 */

@Service
@Transactional(rollbackFor = { jakarta.xml.bind.JAXBException.class }, noRollbackFor = {
		jakarta.persistence.NoResultException.class,
		org.springframework.dao.EmptyResultDataAccessException.class })
public class UsageSummaryServiceImpl implements UsageSummaryService {

    private final Log logger = LogFactory.getLog(getClass());

    @Autowired
    private UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private UsageSummaryMapper usageSummaryMapper;

    @Override
    public UsageSummaryEntity findByUUID(UUID uuid) {
        return usageSummaryRepository.findByUuid(uuid).orElse(null);
    }

    @Override
    public UsageSummaryEntity findById(Long usageSummaryId) {
        return usageSummaryRepository.findById(usageSummaryId).orElse(null);
    }

    @Override
    public UsageSummaryEntity save(UsageSummaryEntity usageSummary) {
        return usageSummaryRepository.save(usageSummary);
    }

    @Override
    public String feedFor(List<UsageSummaryEntity> usageSummaries) {
        // TODO: Implement modern feed generation using DTOs
        logger.info("Generating feed for " + usageSummaries.size() + " usage summaries");
        return null;
    }

    @Override
    public String entryFor(UsageSummaryEntity usageSummary) {
        // TODO: Implement modern entry generation using DTOs
        logger.info("Generating entry for usage summary: " + usageSummary.getId());
        return null;
    }

    @Override
    public void associateByUUID(UsagePointEntity usagePoint, UUID uuid) {
        UsageSummaryEntity entity = usageSummaryRepository.findByUuid(uuid).orElse(null);
        if (entity != null) {
            entity.setUsagePointEntity(usagePoint);
            usageSummaryRepository.save(entity);
            logger.info("Associated usage summary " + uuid + " with usage point " + usagePoint.getId());
        }
    }

    @Override
    public void delete(UsageSummaryEntity usageSummary) {
        usageSummaryRepository.deleteById(usageSummary.getId());
        logger.info("Deleted usage summary: " + usageSummary.getId());
    }

    @Override
    public List<UsageSummaryEntity> findAllByUsagePoint(UsagePointEntity usagePoint) {
        return usageSummaryRepository.findByUsagePointEntity(usagePoint);
    }

    @Override
    public void add(UsageSummaryEntity usageSummary) {
        usageSummaryRepository.save(usageSummary);
        logger.info("Added usage summary: " + usageSummary.getId());
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
            logger.error("Failed to import UsageSummary resource", e);
            return null;
        }
    }
}
