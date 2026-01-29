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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between CustomerAccountEntity and CustomerAccountDto.
 * <p>
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 * <p>
 * Maps customer.xsd CustomerAccount fields including Document base fields.
 * Per customer.xsd lines 118-158 (CustomerAccount) and 819-872 (Document base type).
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class,
    ElectronicAddressMapper.class,
    OrganisationMapper.class,
    StatusMapper.class,
    AccountNotificationMapper.class
})
public interface CustomerAccountMapper {

    /**
     * Converts a CustomerAccountEntity to a CustomerAccountDto.
     * Maps all Document fields and CustomerAccount-specific fields per customer.xsd.
     *
     * @param entity the customer account entity
     * @return the customer account DTO
     */
    // Document fields
    @Mapping(target = "type", source = "type")
    @Mapping(target = "authorName", source = "authorName")
    @Mapping(target = "createdDateTime", source = "createdDateTime")
    @Mapping(target = "lastModifiedDateTime", source = "lastModifiedDateTime")
    @Mapping(target = "revisionNumber", source = "revisionNumber")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "docStatus", source = "docStatus")
    // CustomerAccount fields
    @Mapping(target = "billingCycle", source = "billingCycle")
    @Mapping(target = "budgetBill", source = "budgetBill")
    @Mapping(target = "lastBillAmount", source = "lastBillAmount")
    @Mapping(target = "notifications", source = "notifications")
    @Mapping(target = "contactInfo", source = "contactInfo")
    @Mapping(target = "accountId", source = "accountId")
    CustomerAccountDto toDto(CustomerAccountEntity entity);

    /**
     * Converts a CustomerAccountDto to a CustomerAccountEntity.
     * Maps all Document fields and CustomerAccount-specific fields per customer.xsd.
     *
     * @param dto the customer account DTO
     * @return the customer account entity
     */
    // Document fields
    @Mapping(target = "type", source = "type")
    @Mapping(target = "authorName", source = "authorName")
    @Mapping(target = "createdDateTime", source = "createdDateTime")
    @Mapping(target = "lastModifiedDateTime", source = "lastModifiedDateTime")
    @Mapping(target = "revisionNumber", source = "revisionNumber")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "docStatus", source = "docStatus")
    // CustomerAccount fields
    @Mapping(target = "billingCycle", source = "billingCycle")
    @Mapping(target = "budgetBill", source = "budgetBill")
    @Mapping(target = "lastBillAmount", source = "lastBillAmount")
    @Mapping(target = "notifications", source = "notifications")
    @Mapping(target = "contactInfo", source = "contactInfo")
    @Mapping(target = "accountId", source = "accountId")
    // Entity-only fields not in DTO
    @Mapping(target = "isPrePay", ignore = true) // Not in customer.xsd CustomerAccount
    @Mapping(target = "customer", ignore = true) // Relationship handled separately
    // IdentifiedObject fields (inherited) - handled by Atom layer
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    CustomerAccountEntity toEntity(CustomerAccountDto dto);
}
