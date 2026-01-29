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
import org.greenbuttonalliance.espi.common.dto.customer.ElectronicAddressDto;
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
 * Maps only Customer fields. IdentifiedObject fields are NOT part of the customer.xsd
 * definition and are handled by AtomFeedDto/AtomEntryDto.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    ElectronicAddressMapper.class
})
public interface CustomerMapper extends BaseMapperUtils {

    /**
     * Converts a CustomerEntity to a CustomerDto.
     * Maps customer information including embedded objects.
     *
     * @param entity the customer entity
     * @return the customer DTO
     */
    @Mapping(target = "organisation", source = ".", qualifiedByName = "mapOrganisation")
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
    @Mapping(target = "organisation", source = "organisation", qualifiedByName = "mapOrganisationFromDto")
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
    CustomerEntity toEntity(CustomerDto dto);

    /**
     * Maps CustomerEntity with PhoneNumberEntity list to OrganisationDto.
     * Combines embedded Organisation data with separate phone number entities.
     * Field order matches customer.xsd:1096-1125.
     */
    @Named("mapOrganisation")
    default CustomerDto.OrganisationDto mapOrganisation(CustomerEntity entity) {
        if (entity == null || entity.getOrganisation() == null) {
            return null;
        }

        Organisation org = entity.getOrganisation();
        List<PhoneNumberEntity> phoneNumbers = entity.getPhoneNumbers();

        // Extract phone numbers by type
        CustomerDto.TelephoneNumberDto phone1 = extractPhoneByType(phoneNumbers, PhoneNumberEntity.PhoneType.PRIMARY);
        CustomerDto.TelephoneNumberDto phone2 = extractPhoneByType(phoneNumbers, PhoneNumberEntity.PhoneType.SECONDARY);

        // Constructor order: streetAddress, postalAddress, phone1, phone2, electronicAddress, organisationName
        return new CustomerDto.OrganisationDto(
            mapStreetAddress(org.getStreetAddress()),
            mapStreetAddress(org.getPostalAddress()),
            phone1,
            phone2,
            mapElectronicAddress(org.getElectronicAddress()),
            org.getOrganisationName()
        );
    }

    /**
     * Maps OrganisationDto to Organisation entity (without phone numbers).
     * Phone numbers are handled separately via PhoneNumberEntity.
     */
    @Named("mapOrganisationFromDto")
    default Organisation mapOrganisationFromDto(CustomerDto.OrganisationDto orgDto) {
        if (orgDto == null) {
            return null;
        }

        Organisation org = new Organisation();
        org.setOrganisationName(orgDto.getOrganisationName());
        org.setStreetAddress(mapStreetAddressFromDto(orgDto.getStreetAddress()));
        org.setPostalAddress(mapStreetAddressFromDto(orgDto.getPostalAddress()));
        org.setElectronicAddress(mapElectronicAddressFromDto(orgDto.getElectronicAddress()));

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
        address.setStreetDetail(dto.getStreetDetail());
        address.setTownDetail(dto.getTownDetail());
        address.setStateOrProvince(dto.getStateOrProvince());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());
        return address;
    }

    /**
     * Helper method for mapping electronic address in custom Organisation mapping.
     * Delegates to simple field-to-field copy since ElectronicAddressMapper
     * is not directly accessible from default interface methods.
     */
    default ElectronicAddressDto mapElectronicAddress(Organisation.ElectronicAddress address) {
        if (address == null) return null;
        return new ElectronicAddressDto(
            address.getLan(),
            address.getMac(),
            address.getEmail1(),
            address.getEmail2(),
            address.getWeb(),
            address.getRadio(),
            address.getUserID(),
            address.getPassword()
        );
    }

    /**
     * Helper method for mapping electronic address from DTO in custom Organisation mapping.
     * Delegates to simple field-to-field copy since ElectronicAddressMapper
     * is not directly accessible from default interface methods.
     */
    default Organisation.ElectronicAddress mapElectronicAddressFromDto(ElectronicAddressDto dto) {
        if (dto == null) return null;
        Organisation.ElectronicAddress address = new Organisation.ElectronicAddress();
        address.setLan(dto.getLan());
        address.setMac(dto.getMac());
        address.setEmail1(dto.getEmail1());
        address.setEmail2(dto.getEmail2());
        address.setWeb(dto.getWeb());
        address.setRadio(dto.getRadio());
        address.setUserID(dto.getUserID());
        address.setPassword(dto.getPassword());
        return address;
    }

    // Helper method to extract phone number by type
    // Maps PhoneNumberEntity (old 4-field format) to TelephoneNumberDto (new 8-field format)
    default CustomerDto.TelephoneNumberDto extractPhoneByType(List<PhoneNumberEntity> phoneNumbers, PhoneNumberEntity.PhoneType type) {
        if (phoneNumbers == null) return null;

        return phoneNumbers.stream()
            .filter(phone -> phone.getPhoneType() == type)
            .findFirst()
            .map(phone -> new CustomerDto.TelephoneNumberDto(
                phone.getCountryCode(),      // 1. countryCode
                phone.getAreaCode(),          // 2. areaCode
                phone.getCityCode(),          // 3. cityCode
                phone.getLocalNumber(),       // 4. localNumber
                phone.getExtension(),         // 5. ext (mapped from extension)
                phone.getDialOut(),           // 6. dialOut
                phone.getInternationalPrefix(), // 7. internationalPrefix
                phone.getItuPhone()           // 8. ituPhone
            ))
            .orElse(null);
    }
}