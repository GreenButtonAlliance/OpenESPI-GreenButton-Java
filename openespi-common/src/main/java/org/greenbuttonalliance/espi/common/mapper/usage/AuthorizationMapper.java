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

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.AuthorizationDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between AuthorizationEntity and AuthorizationDto.
 * 
 * Handles the conversion between the JPA entity used for persistence and the DTO 
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class
})
public interface AuthorizationMapper {

    /**
     * Converts an AuthorizationEntity to an AuthorizationDto.
     * Maps all OAuth 2.0 and ESPI 4.0 XSD fields for XML marshalling.
     *
     * @param entity the authorization entity
     * @return the authorization DTO
     */
    @Mapping(target = "uuid", source = "id", qualifiedByName = "uuidToString")
    // XSD-compliant fields
    @Mapping(target = "authorizedPeriod", source = "authorizedPeriod")
    @Mapping(target = "publishedPeriod", source = "publishedPeriod")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "grantType", source = "grantType")
    @Mapping(target = "scope", source = "scope")
    @Mapping(target = "tokenType", source = "tokenType")
    @Mapping(target = "error", source = "error")
    @Mapping(target = "errorDescription", source = "errorDescription")
    @Mapping(target = "errorUri", source = "errorUri")
    @Mapping(target = "resourceURI", source = "resourceURI")
    @Mapping(target = "authorizationUri", source = "authorizationURI")
    @Mapping(target = "customerResourceURI", source = "customerResourceURI")
    // OAuth2 implementation fields (not in XSD)
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "authorizationCode", source = "code")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "responseType", source = "responseType")
    @Mapping(target = "thirdParty", source = "thirdParty")
    @Mapping(target = "applicationInformationId", ignore = true) // Handle separately
    @Mapping(target = "retailCustomerId", ignore = true) // Handle separately
    AuthorizationDto toDto(AuthorizationEntity entity);

    /**
     * Converts an AuthorizationDto to an AuthorizationEntity.
     * Maps all OAuth 2.0 and ESPI 4.0 XSD fields for persistence.
     *
     * @param dto the authorization DTO
     * @return the authorization entity
     */
    @Mapping(target = "id", source = "uuid", qualifiedByName = "stringToUuid")
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "description", ignore = true)
    // XSD-compliant fields
    @Mapping(target = "authorizedPeriod", source = "authorizedPeriod")
    @Mapping(target = "publishedPeriod", source = "publishedPeriod")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "grantType", source = "grantType")
    @Mapping(target = "scope", source = "scope")
    @Mapping(target = "tokenType", source = "tokenType")
    @Mapping(target = "error", source = "error")
    @Mapping(target = "errorDescription", source = "errorDescription")
    @Mapping(target = "errorUri", source = "errorUri")
    @Mapping(target = "resourceURI", source = "resourceURI")
    @Mapping(target = "authorizationURI", source = "authorizationUri")
    @Mapping(target = "customerResourceURI", source = "customerResourceURI")
    // OAuth2 implementation fields (not in XSD)
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "code", source = "authorizationCode")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "responseType", source = "responseType")
    @Mapping(target = "thirdParty", source = "thirdParty")
    @Mapping(target = "applicationInformation", ignore = true) // Complex mapping, handle separately
    @Mapping(target = "retailCustomer", ignore = true) // Complex mapping, handle separately
    @Mapping(target = "relatedLinks", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    AuthorizationEntity toEntity(AuthorizationDto dto);

    /**
     * Updates an existing AuthorizationEntity with data from an AuthorizationDto.
     * Useful for update operations where the entity ID should be preserved.
     *
     * @param dto the source DTO
     * @param entity the target entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "description", ignore = true)
    // XSD-compliant fields - will be mapped
    @Mapping(target = "authorizedPeriod", source = "authorizedPeriod")
    @Mapping(target = "publishedPeriod", source = "publishedPeriod")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "grantType", source = "grantType")
    @Mapping(target = "scope", source = "scope")
    @Mapping(target = "tokenType", source = "tokenType")
    @Mapping(target = "error", source = "error")
    @Mapping(target = "errorDescription", source = "errorDescription")
    @Mapping(target = "errorUri", source = "errorUri")
    @Mapping(target = "resourceURI", source = "resourceURI")
    @Mapping(target = "authorizationURI", source = "authorizationUri")
    @Mapping(target = "customerResourceURI", source = "customerResourceURI")
    // OAuth2 implementation fields - will be mapped
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "code", source = "authorizationCode")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "responseType", source = "responseType")
    @Mapping(target = "thirdParty", source = "thirdParty")
    // Relationships - preserve existing
    @Mapping(target = "relatedLinks", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "retailCustomer", ignore = true)
    @Mapping(target = "applicationInformation", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    void updateEntity(AuthorizationDto dto, @MappingTarget AuthorizationEntity entity);
}