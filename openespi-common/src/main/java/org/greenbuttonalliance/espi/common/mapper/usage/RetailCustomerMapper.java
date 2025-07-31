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

package org.greenbuttonalliance.espi.common.mapper.usage;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.dto.usage.RetailCustomerDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between RetailCustomerEntity and RetailCustomerDto.
 * 
 * Handles the conversion between the JPA entity used for persistence and the DTO 
 * used for JAXB XML marshalling in the Green Button API.
 * Excludes security-sensitive fields like password from DTO mapping.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class,
    UsagePointMapper.class,
    AuthorizationMapper.class
})
public interface RetailCustomerMapper {

    /**
     * Converts a RetailCustomerEntity to a RetailCustomerDto.
     * Maps all customer fields except security-sensitive data like password.
     * 
     * @param entity the retail customer entity
     * @return the retail customer DTO
     */
    @Mapping(target = "uuid", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "accountCreated", source = "accountCreated")
    @Mapping(target = "lastLogin", source = "lastLogin")
    @Mapping(target = "accountLocked", source = "accountLocked")
    @Mapping(target = "failedLoginAttempts", source = "failedLoginAttempts")
    @Mapping(target = "usagePoints", source = "usagePoints")
    @Mapping(target = "authorizations", source = "authorizations")
    RetailCustomerDto toDto(RetailCustomerEntity entity);

    /**
     * Converts a RetailCustomerDto to a RetailCustomerEntity.
     * Maps customer fields but excludes collections to prevent cascading issues.
     * Security fields like password are not set from DTO for security reasons.
     * 
     * @param dto the retail customer DTO
     * @return the retail customer entity
     */
    @Mapping(target = "passwordSecurelyNoValidation", ignore = true)
    @Mapping(target = "passwordSecurely", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "id", source = "uuid", qualifiedByName = "stringToUuid")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "accountCreated", source = "accountCreated")
    @Mapping(target = "lastLogin", source = "lastLogin")
    @Mapping(target = "accountLocked", source = "accountLocked")
    @Mapping(target = "failedLoginAttempts", source = "failedLoginAttempts")
    @Mapping(target = "password", ignore = true) // Security: never set password from DTO
    @Mapping(target = "usagePoints", ignore = true) // Managed separately
    @Mapping(target = "authorizations", ignore = true) // Managed separately
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "updated", ignore = true)
    RetailCustomerEntity toEntity(RetailCustomerDto dto);

    /**
     * Updates an existing RetailCustomerEntity with data from a RetailCustomerDto.
     * Only updates non-security fields and preserves existing relationships.
     * 
     * @param dto the source retail customer DTO
     * @param entity the target retail customer entity to update
     */
    @Mapping(target = "passwordSecurelyNoValidation", ignore = true)
    @Mapping(target = "passwordSecurely", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "id", ignore = true) // Never change ID
    @Mapping(target = "username", source = "username")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "accountCreated", ignore = true) // Preserve original creation time
    @Mapping(target = "lastLogin", source = "lastLogin")
    @Mapping(target = "accountLocked", source = "accountLocked")
    @Mapping(target = "failedLoginAttempts", source = "failedLoginAttempts")
    @Mapping(target = "password", ignore = true) // Security: never update password from DTO
    @Mapping(target = "usagePoints", ignore = true) // Preserve existing relationships
    @Mapping(target = "authorizations", ignore = true) // Preserve existing relationships
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "updated", ignore = true)
    void updateEntityFromDto(RetailCustomerDto dto, @MappingTarget RetailCustomerEntity entity);
}