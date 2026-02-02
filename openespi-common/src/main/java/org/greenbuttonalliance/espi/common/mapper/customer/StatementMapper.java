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

package org.greenbuttonalliance.espi.common.mapper.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.StatementEntity;
import org.greenbuttonalliance.espi.common.dto.customer.StatementDto;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between StatementEntity and StatementDto.
 *
 * Maps ONLY customer.xsd Statement fields (lines 373-393):
 * - issueDateTime (requires DateTimeMapper conversion)
 * - statementRef collection (requires StatementRefMapper)
 *
 * All Atom metadata (id, description, published, updated, links) handled by
 * AtomEntryDto and LinkDto per DRY principle.
 */
@Mapper(componentModel = "spring", uses = {DateTimeMapper.class, StatementRefMapper.class})
public interface StatementMapper {

    /**
     * Maps StatementEntity to StatementDto for XML export.
     *
     * Field mappings:
     * - issueDateTime: OffsetDateTime → Long (via DateTimeMapper.mapToLong)
     * - statementRefs: List<StatementRefEntity> → List<StatementRefDto> (via StatementRefMapper)
     *
     * @param entity the StatementEntity to map
     * @return the mapped StatementDto, or null if entity is null
     */
    @Mapping(target = "issueDateTime", source = "issueDateTime", qualifiedByName = "offsetToLong")
    @Mapping(target = "statementRef", source = "statementRefs")
    StatementDto toDto(StatementEntity entity);

    /**
     * Maps StatementDto to StatementEntity for import.
     *
     * Field mappings:
     * - issueDateTime: Long → OffsetDateTime (via DateTimeMapper.mapFromLong)
     * - statementRef: List<StatementRefDto> → List<StatementRefEntity> (via StatementRefMapper)
     *
     * @param dto the StatementDto to map
     * @return the mapped StatementEntity, or null if dto is null
     */
    @Mapping(target = "issueDateTime", source = "issueDateTime", qualifiedByName = "longToOffset")
    @Mapping(target = "statementRefs", source = "statementRef")
    StatementEntity toEntity(StatementDto dto);
}
