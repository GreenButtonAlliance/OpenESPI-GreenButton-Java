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
import org.greenbuttonalliance.espi.common.mapper.usage.DateTimeIntervalMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between CustomerAgreementEntity and CustomerAgreementDto.
 * <p>
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 * <p>
 * Maps customer.xsd CustomerAgreement fields including Document and Agreement base fields.
 * Per customer.xsd lines 159-209 (CustomerAgreement), 622-642 (Agreement), 819-885 (Document).
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class,
    ElectronicAddressMapper.class,
    StatusMapper.class,
    DateTimeIntervalMapper.class
})
public interface CustomerAgreementMapper {

    /**
     * Converts a CustomerAgreementEntity to a CustomerAgreementDto.
     * Maps all Document, Agreement, and CustomerAgreement fields per customer.xsd.
     *
     * @param entity the customer agreement entity
     * @return the customer agreement DTO
     */
    @Mapping(target = "uuid", source = "id")
    // Document fields (11)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "authorName", source = "authorName")
    @Mapping(target = "createdDateTime", source = "createdDateTime")
    @Mapping(target = "lastModifiedDateTime", source = "lastModifiedDateTime")
    @Mapping(target = "revisionNumber", source = "revisionNumber")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "docStatus", source = "docStatus")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "comment", source = "comment")
    // Agreement fields (2)
    @Mapping(target = "signDate", source = "signDate")
    @Mapping(target = "validityInterval", source = "validityInterval")
    // CustomerAgreement fields (6)
    @Mapping(target = "loadMgmt", source = "loadMgmt")
    @Mapping(target = "isPrePay", source = "isPrePay")
    @Mapping(target = "shutOffDateTime", source = "shutOffDateTime")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "futureStatus", source = "futureStatus")
    @Mapping(target = "agreementId", source = "agreementId")
    CustomerAgreementDto toDto(CustomerAgreementEntity entity);

    /**
     * Converts a CustomerAgreementDto to a CustomerAgreementEntity.
     * Maps all Document, Agreement, and CustomerAgreement fields per customer.xsd.
     *
     * @param dto the customer agreement DTO
     * @return the customer agreement entity
     */
    @Mapping(target = "id", source = "uuid")
    // Document fields (11)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "authorName", source = "authorName")
    @Mapping(target = "createdDateTime", source = "createdDateTime")
    @Mapping(target = "lastModifiedDateTime", source = "lastModifiedDateTime")
    @Mapping(target = "revisionNumber", source = "revisionNumber")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "docStatus", source = "docStatus")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "comment", source = "comment")
    // Agreement fields (2)
    @Mapping(target = "signDate", source = "signDate")
    @Mapping(target = "validityInterval", source = "validityInterval")
    // CustomerAgreement fields (6)
    @Mapping(target = "loadMgmt", source = "loadMgmt")
    @Mapping(target = "isPrePay", source = "isPrePay")
    @Mapping(target = "shutOffDateTime", source = "shutOffDateTime")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "futureStatus", source = "futureStatus")
    @Mapping(target = "agreementId", source = "agreementId")
    // Entity-only fields not in DTO (relationships commented as TODO)
    // @Mapping(target = "demandResponsePrograms", ignore = true)
    // @Mapping(target = "pricingStructures", ignore = true)
    // IdentifiedObject fields (inherited) - handled by Atom layer
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    CustomerAgreementEntity toEntity(CustomerAgreementDto dto);
}
