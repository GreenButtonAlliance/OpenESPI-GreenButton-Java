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

package org.greenbuttonalliance.espi.common.domain.customer.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * Abstract base class for Location types.
 * 
 * The place, scene, or point of something where someone or something has been, is, and/or will be 
 * at a given moment in time. It can be defined with one or more position points (coordinates) 
 * in a given coordinate system.
 * 
 * This is a @MappedSuperclass that provides location-specific fields but does not extend IdentifiedObject.
 * Actual ESPI resource entities that represent locations should extend IdentifiedObject directly.
 */
@MappedSuperclass
@Data
@EqualsAndHashCode
@NoArgsConstructor
@ToString
public abstract class Location implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Classification by utility's corporate standards and practices, relative to the location itself 
     * (e.g., geographical, functional accounting, etc., not a given property that happens to exist at that location).
     */
    @Column(name = "type", length = 256)
    private String type;

    /**
     * Main address of the location.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "streetDetail", column = @Column(name = "location_main_street_detail")),
        @AttributeOverride(name = "townDetail", column = @Column(name = "location_main_town_detail")),
        @AttributeOverride(name = "stateOrProvince", column = @Column(name = "location_main_state_or_province")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "location_main_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "location_main_country"))
    })
    private Organisation.StreetAddress mainAddress;

    /**
     * Secondary address of the location. For example, PO Box address may have different ZIP code than that in the 'mainAddress'.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "streetDetail", column = @Column(name = "location_secondary_street_detail")),
        @AttributeOverride(name = "townDetail", column = @Column(name = "location_secondary_town_detail")),
        @AttributeOverride(name = "stateOrProvince", column = @Column(name = "location_secondary_state_or_province")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "location_secondary_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "location_secondary_country"))
    })
    private Organisation.StreetAddress secondaryAddress;

    // PhoneNumber fields removed - phone numbers are managed separately via PhoneNumberEntity
    // to avoid JPA column mapping conflicts in embedded contexts

    /**
     * Electronic address.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "email1", column = @Column(name = "location_email1")),
        @AttributeOverride(name = "email2", column = @Column(name = "location_email2")),
        @AttributeOverride(name = "web", column = @Column(name = "location_web")),
        @AttributeOverride(name = "radio", column = @Column(name = "location_radio"))
    })
    private Organisation.ElectronicAddress electronicAddress;

    /**
     * (if applicable) Reference to geographical information source, often external to the utility.
     */
    @Column(name = "geo_info_reference", length = 256)
    private String geoInfoReference;

    /**
     * (if applicable) Direction that allows field crews to quickly find a given asset.
     */
    @Column(name = "direction", length = 256)
    private String direction;

    /**
     * Status of this location.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "status_value")),
        @AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time")),
        @AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
    })
    private CustomerEntity.Status status;

    /**
     * Sequence of position points describing this location, expressed in coordinate system 'Location.CoordinateSystem'.
     * TODO: Create PositionPointEntity and enable this relationship
     */
    // @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<PositionPointEntity> positionPoints;
}