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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerAgreementDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between CustomerAgreementEntity and CustomerAgreementDto.
 * <p>
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 * <p>
 * Maps only customer.xsd CustomerAgreement fields. IdentifiedObject fields are NOT part of
 * the customer.xsd CustomerAgreement definition and are handled by AtomFeedDto/AtomEntryDto.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class
})
public interface CustomerAgreementMapper {

    /**
     * Converts a CustomerAgreementEntity to a CustomerAgreementDto.
     * Maps only customer.xsd CustomerAgreement fields.
     *
     * @param entity the customer agreement entity
     * @return the customer agreement DTO
     */
    @Mapping(target = "id", ignore = true) // IdentifiedObject field handled by Atom layer
    @Mapping(target = "signDate", source = "signDate")
    @Mapping(target = "validityInterval", ignore = true) // Complex mapping
    @Mapping(target = "customerAccount", ignore = true) // Relationship handled separately
    @Mapping(target = "serviceLocations", ignore = true) // Relationship handled separately
    @Mapping(target = "statements", ignore = true) // Relationship handled separately
    CustomerAgreementDto toDto(CustomerAgreementEntity entity);

    /**
     * Converts a CustomerAgreementDto to a CustomerAgreementEntity.
     * Maps only customer.xsd CustomerAgreement fields.
     *
     * @param dto the customer agreement DTO
     * @return the customer agreement entity
     */
    @Mapping(target = "id", ignore = true) // IdentifiedObject field handled by Atom layer
    @Mapping(target = "signDate", source = "signDate")
    @Mapping(target = "validityInterval", ignore = true) // Complex mapping
    @Mapping(target = "createdDateTime", ignore = true) // From Document
    @Mapping(target = "lastModifiedDateTime", ignore = true) // From Document
    @Mapping(target = "revisionNumber", ignore = true) // From Document
    @Mapping(target = "subject", ignore = true) // From Document
    @Mapping(target = "title", ignore = true) // From Document
    @Mapping(target = "type", ignore = true) // From Document
    CustomerAgreementEntity toEntity(CustomerAgreementDto dto);

}
