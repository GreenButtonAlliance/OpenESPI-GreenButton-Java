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

import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between Status entity and DTO.
 */
@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface StatusMapper {

    /**
     * Converts a Status entity to a DTO.
     *
     * @param entity the status entity
     * @return the status DTO
     */
    CustomerAccountDto.StatusDto toDto(Status entity);

    /**
     * Converts a Status DTO to an entity.
     *
     * @param dto the status DTO
     * @return the status entity
     */
    Status toEntity(CustomerAccountDto.StatusDto dto);
}
