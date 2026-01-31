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
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.entity.ServiceLocationEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Status;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.dto.customer.ServiceLocationDto;
import org.greenbuttonalliance.espi.common.dto.customer.StatusDto;
import org.greenbuttonalliance.espi.common.mapper.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between ServiceLocationEntity and ServiceLocationDto.
 *
 * Maps only ServiceLocation fields. IdentifiedObject fields are NOT part of the customer.xsd
 * definition and are handled by AtomFeedDto/AtomEntryDto.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {
    DateTimeMapper.class,
    ElectronicAddressMapper.class,
    TelephoneNumberMapper.class
})
public interface ServiceLocationMapper {

    /**
     * Converts a ServiceLocationEntity to a ServiceLocationDto.
     * Maps all Location and ServiceLocation fields including embedded objects.
     *
     * @param entity the service location entity
     * @return the service location DTO
     */
    @Mapping(target = "type", source = "type")
    @Mapping(target = "mainAddress", source = "mainAddress")
    @Mapping(target = "secondaryAddress", source = "secondaryAddress")
    @Mapping(target = "phone1", source = "phone1")
    @Mapping(target = "phone2", source = "phone2")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "geoInfoReference", source = "geoInfoReference")
    @Mapping(target = "direction", source = "direction")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "positionPoints", expression = "java(null)")  // TODO: Implement when PositionPointEntity is created
    @Mapping(target = "accessMethod", source = "accessMethod")
    @Mapping(target = "siteAccessProblem", source = "siteAccessProblem")
    @Mapping(target = "needsInspection", source = "needsInspection")
    @Mapping(target = "usagePointHrefs", source = "usagePointHrefs")
    @Mapping(target = "outageBlock", source = "outageBlock")
    ServiceLocationDto toDto(ServiceLocationEntity entity);

    /**
     * Converts a ServiceLocationDto to a ServiceLocationEntity.
     * Maps all Location and ServiceLocation fields including embedded objects.
     *
     * @param dto the service location DTO
     * @return the service location entity
     */
    @Mapping(target = "type", source = "type")
    @Mapping(target = "mainAddress", source = "mainAddress")
    @Mapping(target = "secondaryAddress", source = "secondaryAddress")
    @Mapping(target = "phone1", source = "phone1")
    @Mapping(target = "phone2", source = "phone2")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "geoInfoReference", source = "geoInfoReference")
    @Mapping(target = "direction", source = "direction")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "accessMethod", source = "accessMethod")
    @Mapping(target = "siteAccessProblem", source = "siteAccessProblem")
    @Mapping(target = "needsInspection", source = "needsInspection")
    @Mapping(target = "usagePointHrefs", source = "usagePointHrefs")
    @Mapping(target = "outageBlock", source = "outageBlock")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "selfLink", ignore = true)
    @Mapping(target = "upLink", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    ServiceLocationEntity toEntity(ServiceLocationDto dto);

    // Helper methods for embedded type conversions

    /**
     * Maps StreetAddress entity to CustomerDto.StreetAddressDto.
     */
    default CustomerDto.StreetAddressDto map(StreetAddress address) {
        if (address == null) return null;
        return new CustomerDto.StreetAddressDto(
            address.getStreetDetail(),
            address.getTownDetail(),
            address.getStateOrProvince(),
            address.getPostalCode(),
            address.getCountry()
        );
    }

    /**
     * Maps CustomerDto.StreetAddressDto to StreetAddress entity.
     */
    default StreetAddress map(CustomerDto.StreetAddressDto dto) {
        if (dto == null) return null;
        StreetAddress address = new StreetAddress();
        address.setStreetDetail(dto.getStreetDetail());
        address.setTownDetail(dto.getTownDetail());
        address.setStateOrProvince(dto.getStateOrProvince());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());
        return address;
    }


    /**
     * Maps Status entity to StatusDto.
     */
    default StatusDto mapStatus(Status status) {
        if (status == null) return null;
        return new StatusDto(
            status.getValue(),
            status.getDateTime(),
            status.getRemark(),
            status.getReason()
        );
    }

    /**
     * Maps StatusDto to Status entity.
     */
    default Status mapStatus(StatusDto dto) {
        if (dto == null) return null;
        return new Status(
            dto.getValue(),
            dto.getDateTime(),
            dto.getRemark(),
            dto.getReason()
        );
    }

    /**
     * Maps list of PositionPoint entities to DTOs.
     * TODO: Implement when PositionPointEntity is created.
     */
    default List<ServiceLocationDto.PositionPointDto> mapPositionPoints(List<?> entities) {
        // Placeholder - return null until PositionPointEntity is implemented
        return null;
    }

    /**
     * Maps list of PositionPointDto to entities.
     * TODO: Implement when PositionPointEntity is created.
     */
    default List<?> mapPositionPointsToEntities(List<ServiceLocationDto.PositionPointDto> dtos) {
        // Placeholder - return null until PositionPointEntity is implemented
        return null;
    }
}
