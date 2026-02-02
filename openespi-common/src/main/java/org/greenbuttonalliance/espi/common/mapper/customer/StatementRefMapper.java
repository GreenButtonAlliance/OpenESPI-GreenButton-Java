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

import org.greenbuttonalliance.espi.common.domain.customer.entity.StatementRefEntity;
import org.greenbuttonalliance.espi.common.dto.customer.StatementRefDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between StatementRefEntity and StatementRefDto.
 *
 * StatementRef is a simple embeddable with 3 fields (fileName, mediaType, statementURL).
 * Direct field mapping - no transformations needed.
 *
 * Per customer.xsd lines 285-307:
 * - StatementRef extends Object (not IdentifiedObject)
 * - No id, links, or metadata fields
 * - Simple 1:1 field mapping
 */
@Mapper(componentModel = "spring")
public interface StatementRefMapper {

    /**
     * Maps StatementRefEntity to StatementRefDto for XML export.
     *
     * @param entity the StatementRefEntity to map
     * @return the mapped StatementRefDto, or null if entity is null
     */
    StatementRefDto toDto(StatementRefEntity entity);

    /**
     * Maps StatementRefDto to StatementRefEntity for import.
     *
     * @param dto the StatementRefDto to map
     * @return the mapped StatementRefEntity, or null if dto is null
     */
    StatementRefEntity toEntity(StatementRefDto dto);
}
