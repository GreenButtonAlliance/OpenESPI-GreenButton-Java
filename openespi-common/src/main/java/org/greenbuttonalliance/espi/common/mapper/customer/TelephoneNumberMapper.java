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

import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;
import org.greenbuttonalliance.espi.common.dto.customer.TelephoneNumberDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between TelephoneNumber entity and DTO.
 */
@Mapper(componentModel = "spring")
public interface TelephoneNumberMapper {

    /**
     * Converts a TelephoneNumber entity to a DTO.
     *
     * @param entity the telephone number entity
     * @return the telephone number DTO
     */
    TelephoneNumberDto toDto(TelephoneNumber entity);

    /**
     * Converts a TelephoneNumber DTO to an entity.
     *
     * @param dto the telephone number DTO
     * @return the telephone number entity
     */
    TelephoneNumber toEntity(TelephoneNumberDto dto);
}