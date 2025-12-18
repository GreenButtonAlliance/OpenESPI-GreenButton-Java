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

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between ApplicationInformationEntity and ApplicationInformationDto.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 *
 * Field mappings match ESPI 4.0 XSD schema sequence (espi.xsd lines 62-246).
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    BaseMapperUtils.class
})
public interface ApplicationInformationMapper {

    /**
     * Converts an ApplicationInformationEntity to an ApplicationInformationDto.
     * Maps all OAuth 2.0 application registration fields for XML marshalling.
     *
     * @param entity the application information entity
     * @return the application information DTO
     */
    @Mapping(target = "uuid", source = "id", qualifiedByName = "uuidToString")
    // XSD fields in order
    @Mapping(target = "dataCustodianId", source = "dataCustodianId")
    @Mapping(target = "dataCustodianApplicationStatus", source = "dataCustodianApplicationStatus")
    @Mapping(target = "thirdPartyApplicationDescription", source = "thirdPartyApplicationDescription")
    @Mapping(target = "thirdPartyApplicationStatus", source = "thirdPartyApplicationStatus")
    @Mapping(target = "thirdPartyApplicationType", source = "thirdPartyApplicationType")
    @Mapping(target = "thirdPartyApplicationUse", source = "thirdPartyApplicationUse")
    @Mapping(target = "thirdPartyPhone", source = "thirdPartyPhone")
    @Mapping(target = "authorizationServerUri", source = "authorizationServerUri")
    @Mapping(target = "thirdPartyNotifyURI", source = "thirdPartyNotifyUri")
    @Mapping(target = "authorizationServerAuthorizationEndpoint", source = "authorizationServerAuthorizationEndpoint")
    @Mapping(target = "authorizationServerRegistrationEndpoint", source = "authorizationServerRegistrationEndpoint")
    @Mapping(target = "authorizationServerTokenEndpoint", source = "authorizationServerTokenEndpoint")
    @Mapping(target = "dataCustodianBulkRequestURI", source = "dataCustodianBulkRequestURI")
    @Mapping(target = "dataCustodianResourceEndpoint", source = "dataCustodianResourceEndpoint")
    @Mapping(target = "thirdPartyScopeSelectionScreenURI", source = "thirdPartyScopeSelectionScreenURI")
    @Mapping(target = "thirdPartyUserPortalScreenURI", source = "thirdPartyUserPortalScreenURI")
    @Mapping(target = "clientSecret", source = "clientSecret")
    @Mapping(target = "logoUri", source = "logoUri")
    @Mapping(target = "clientName", source = "clientName")
    @Mapping(target = "clientUri", source = "clientUri")
    @Mapping(target = "redirectUri", source = "redirectUri")
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "tosUri", source = "tosUri")
    @Mapping(target = "policyUri", source = "policyUri")
    @Mapping(target = "softwareId", source = "softwareId")
    @Mapping(target = "softwareVersion", source = "softwareVersion")
    @Mapping(target = "clientIdIssuedAt", source = "clientIdIssuedAt")
    @Mapping(target = "clientSecretExpiresAt", source = "clientSecretExpiresAt")
    @Mapping(target = "contacts", source = "contacts")
    @Mapping(target = "tokenEndpointAuthMethod", source = "tokenEndpointAuthMethod")
    @Mapping(target = "scopes", ignore = true) // Complex type conversion needed: Set<String> -> String
    @Mapping(target = "grantTypes", ignore = true) // Complex type conversion needed: Set<GrantType> -> String
    @Mapping(target = "responseTypes", source = "responseTypes")
    @Mapping(target = "registrationClientUri", source = "registrationClientUri")
    @Mapping(target = "registrationAccessToken", source = "registrationAccessToken")
    @Mapping(target = "dataCustodianScopeSelectionScreenURI", source = "dataCustodianScopeSelectionScreenURI")
    ApplicationInformationDto toDto(ApplicationInformationEntity entity);

    /**
     * Converts an ApplicationInformationDto to an ApplicationInformationEntity.
     * Maps all OAuth 2.0 application registration fields for persistence.
     *
     * @param dto the application information DTO
     * @return the application information entity
     */
    @Mapping(target = "id", source = "uuid", qualifiedByName = "stringToUuid")
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "relatedLinks", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "scope", ignore = true) // Complex type conversion needed: String -> Set<String>
    @Mapping(target = "grantTypes", ignore = true) // Complex type conversion needed: String -> Set<GrantType>
    ApplicationInformationEntity toEntity(ApplicationInformationDto dto);

    /**
     * Updates an existing ApplicationInformationEntity with data from an ApplicationInformationDto.
     * Useful for merge operations where the entity ID should be preserved.
     *
     * @param dto the source DTO
     * @param entity the target entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "relatedLinks", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "scope", ignore = true) // Complex type conversion needed: String -> Set<String>
    @Mapping(target = "grantTypes", ignore = true) // Complex type conversion needed: String -> Set<GrantType>
    void updateEntity(ApplicationInformationDto dto, @MappingTarget ApplicationInformationEntity entity);
}
