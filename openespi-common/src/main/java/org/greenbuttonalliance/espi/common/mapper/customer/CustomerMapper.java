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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.PhoneNumberEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.mapper.BaseIdentifiedObjectMapper;
import org.greenbuttonalliance.espi.common.mapper.BaseMapperUtils;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper for converting between CustomerEntity and CustomerDto.
 * 
 * Handles the conversion between the JPA entity used for persistence and the DTO 
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class
})
public interface CustomerMapper extends BaseMapperUtils {

    /**
     * Converts a CustomerEntity to a CustomerDto.
     * Maps customer information including embedded objects.
     * 
     * @param entity the customer entity
     * @return the customer DTO
     */
    @Mapping(target = "uuid", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "organisationRole", source = ".", qualifiedByName = "mapOrganisationRole")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "specialNeed", source = "specialNeed")
    @Mapping(target = "vip", source = "vip")
    @Mapping(target = "pucNumber", source = "pucNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "locale", source = "locale")
    @Mapping(target = "customerName", source = "customerName")
    CustomerDto toDto(CustomerEntity entity);

    /**
     * Converts a CustomerDto to a CustomerEntity.
     * Maps customer information including embedded objects.
     * 
     * @param dto the customer DTO
     * @return the customer entity
     */
    @Mapping(target = "id", source = "uuid", qualifiedByName = "stringToUuid")
    @Mapping(target = "organisation", source = "organisationRole", qualifiedByName = "mapOrganisation")
    @Mapping(target = "phoneNumbers", ignore = true)
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "specialNeed", source = "specialNeed")
    @Mapping(target = "vip", source = "vip")
    @Mapping(target = "pucNumber", source = "pucNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "locale", source = "locale")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "customerAccounts", ignore = true)
    @Mapping(target = "timeConfiguration", ignore = true)
    @Mapping(target = "statements", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "relatedLinks", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "description", ignore = true)
    CustomerEntity toEntity(CustomerDto dto);

    /**
     * Updates an existing CustomerEntity with data from a CustomerDto.
     * Useful for merge operations where the entity ID should be preserved.
     * 
     * @param dto the source DTO
     * @param entity the target entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organisation", source = "organisationRole", qualifiedByName = "mapOrganisation")
    @Mapping(target = "phoneNumbers", ignore = true)
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "specialNeed", source = "specialNeed")
    @Mapping(target = "vip", source = "vip")
    @Mapping(target = "pucNumber", source = "pucNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "locale", source = "locale")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "customerAccounts", ignore = true)
    @Mapping(target = "timeConfiguration", ignore = true)
    @Mapping(target = "statements", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "relatedLinks", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "description", ignore = true)
    void updateEntity(CustomerDto dto, @MappingTarget CustomerEntity entity);

    /**
     * Maps CustomerEntity with PhoneNumberEntity list to OrganisationRoleDto.
     * Combines embedded Organisation data with separate phone number entities.
     */
    @Named("mapOrganisationRole")
    default CustomerDto.OrganisationRoleDto mapOrganisationRole(CustomerEntity entity) {
        if (entity == null || entity.getOrganisation() == null) {
            return null;
        }
        
        Organisation org = entity.getOrganisation();
        List<PhoneNumberEntity> phoneNumbers = entity.getPhoneNumbers();
        
        // Extract phone numbers by type
        CustomerDto.PhoneNumberDto phone1 = extractPhoneByType(phoneNumbers, PhoneNumberEntity.PhoneType.PRIMARY);
        CustomerDto.PhoneNumberDto phone2 = extractPhoneByType(phoneNumbers, PhoneNumberEntity.PhoneType.SECONDARY);
        
        CustomerDto.OrganisationDto orgDto = new CustomerDto.OrganisationDto(
            org.getOrganisationName(),
            mapStreetAddress(org.getStreetAddress()),
            mapStreetAddress(org.getPostalAddress()),
            phone1,
            phone2,
            mapElectronicAddress(org.getElectronicAddress())
        );
        
        return new CustomerDto.OrganisationRoleDto(orgDto);
    }

    /**
     * Maps OrganisationRoleDto to Organisation entity (without phone numbers).
     * Phone numbers are handled separately via PhoneNumberEntity.
     */
    @Named("mapOrganisation")
    default Organisation mapOrganisation(CustomerDto.OrganisationRoleDto organisationRole) {
        if (organisationRole == null || organisationRole.organisation() == null) {
            return null;
        }
        
        CustomerDto.OrganisationDto orgDto = organisationRole.organisation();
        Organisation org = new Organisation();
        org.setOrganisationName(orgDto.organisationName());
        org.setStreetAddress(mapStreetAddressFromDto(orgDto.streetAddress()));
        org.setPostalAddress(mapStreetAddressFromDto(orgDto.postalAddress()));
        org.setElectronicAddress(mapElectronicAddressFromDto(orgDto.electronicAddress()));
        
        // Phone numbers are @Transient in Organisation and managed separately
        return org;
    }

    // Helper methods for address mapping
    default CustomerDto.StreetAddressDto mapStreetAddress(Organisation.StreetAddress address) {
        if (address == null) return null;
        return new CustomerDto.StreetAddressDto(
            address.getStreetDetail(),
            address.getTownDetail(),
            address.getStateOrProvince(),
            address.getPostalCode(),
            address.getCountry()
        );
    }

    default Organisation.StreetAddress mapStreetAddressFromDto(CustomerDto.StreetAddressDto dto) {
        if (dto == null) return null;
        Organisation.StreetAddress address = new Organisation.StreetAddress();
        address.setStreetDetail(dto.streetDetail());
        address.setTownDetail(dto.townDetail());
        address.setStateOrProvince(dto.stateOrProvince());
        address.setPostalCode(dto.postalCode());
        address.setCountry(dto.country());
        return address;
    }

    default CustomerDto.ElectronicAddressDto mapElectronicAddress(Organisation.ElectronicAddress address) {
        if (address == null) return null;
        return new CustomerDto.ElectronicAddressDto(
            address.getEmail1(),
            address.getEmail2(),
            address.getWeb(),
            address.getRadio()
        );
    }

    default Organisation.ElectronicAddress mapElectronicAddressFromDto(CustomerDto.ElectronicAddressDto dto) {
        if (dto == null) return null;
        Organisation.ElectronicAddress address = new Organisation.ElectronicAddress();
        address.setEmail1(dto.email1());
        address.setEmail2(dto.email2());
        address.setWeb(dto.web());
        address.setRadio(dto.radio());
        return address;
    }

    // Helper method to extract phone number by type
    default CustomerDto.PhoneNumberDto extractPhoneByType(List<PhoneNumberEntity> phoneNumbers, PhoneNumberEntity.PhoneType type) {
        if (phoneNumbers == null) return null;
        
        return phoneNumbers.stream()
            .filter(phone -> phone.getPhoneType() == type)
            .findFirst()
            .map(phone -> new CustomerDto.PhoneNumberDto(
                phone.getAreaCode(),
                phone.getCityCode(),
                phone.getLocalNumber(),
                phone.getExtension()
            ))
            .orElse(null);
    }
}