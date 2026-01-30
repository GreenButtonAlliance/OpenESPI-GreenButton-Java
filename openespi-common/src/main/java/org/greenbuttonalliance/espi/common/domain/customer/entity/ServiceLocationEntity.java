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

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.TelephoneNumber;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for ServiceLocation without JAXB concerns.
 * 
 * A real estate location, commonly referred to as premises.
 * 
 * This is an actual ESPI resource entity that extends IdentifiedObject directly.
 */
@Entity
@Table(name = "service_locations")
@Getter
@Setter
@NoArgsConstructor
public class
ServiceLocationEntity extends IdentifiedObject {

    // Location fields (previously inherited from Location superclass)
    
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
    @AttributeOverride(name = "streetDetail", column = @Column(name = "main_street_detail"))
    @AttributeOverride(name = "townDetail", column = @Column(name = "main_town_detail"))
    @AttributeOverride(name = "stateOrProvince", column = @Column(name = "main_state_or_province"))
    @AttributeOverride(name = "postalCode", column = @Column(name = "main_postal_code"))
    @AttributeOverride(name = "country", column = @Column(name = "main_country"))
    private StreetAddress mainAddress;

    /**
     * Secondary address of the location. For example, PO Box address may have different ZIP code than that in the 'mainAddress'.
     */
    @Embedded
    @AttributeOverride(name = "streetDetail", column = @Column(name = "secondary_street_detail"))
    @AttributeOverride(name = "townDetail", column = @Column(name = "secondary_town_detail"))
    @AttributeOverride(name = "stateOrProvince", column = @Column(name = "secondary_state_or_province"))
    @AttributeOverride(name = "postalCode", column = @Column(name = "secondary_postal_code"))
    @AttributeOverride(name = "country", column = @Column(name = "secondary_country"))
    private StreetAddress secondaryAddress;

    /**
     * Primary phone number for this service location.
     * Per customer.xsd Location.phone1 (TelephoneNumber type, lines 1428-1478).
     */
    @Embedded
    @AttributeOverride(name = "countryCode", column = @Column(name = "phone1_country_code"))
    @AttributeOverride(name = "areaCode", column = @Column(name = "phone1_area_code"))
    @AttributeOverride(name = "cityCode", column = @Column(name = "phone1_city_code"))
    @AttributeOverride(name = "localNumber", column = @Column(name = "phone1_local_number"))
    @AttributeOverride(name = "ext", column = @Column(name = "phone1_ext"))
    @AttributeOverride(name = "dialOut", column = @Column(name = "phone1_dial_out"))
    @AttributeOverride(name = "internationalPrefix", column = @Column(name = "phone1_international_prefix"))
    @AttributeOverride(name = "ituPhone", column = @Column(name = "phone1_itu_phone"))
    private TelephoneNumber phone1;

    /**
     * Secondary phone number for this service location.
     * Per customer.xsd Location.phone2 (TelephoneNumber type, lines 1428-1478).
     */
    @Embedded
    @AttributeOverride(name = "countryCode", column = @Column(name = "phone2_country_code"))
    @AttributeOverride(name = "areaCode", column = @Column(name = "phone2_area_code"))
    @AttributeOverride(name = "cityCode", column = @Column(name = "phone2_city_code"))
    @AttributeOverride(name = "localNumber", column = @Column(name = "phone2_local_number"))
    @AttributeOverride(name = "ext", column = @Column(name = "phone2_ext"))
    @AttributeOverride(name = "dialOut", column = @Column(name = "phone2_dial_out"))
    @AttributeOverride(name = "internationalPrefix", column = @Column(name = "phone2_international_prefix"))
    @AttributeOverride(name = "ituPhone", column = @Column(name = "phone2_itu_phone"))
    private TelephoneNumber phone2;

    /**
     * Electronic address.
     */
    @Embedded
    @AttributeOverride(name = "lan", column = @Column(name = "electronic_lan"))
    @AttributeOverride(name = "mac", column = @Column(name = "electronic_mac"))
    @AttributeOverride(name = "email1", column = @Column(name = "electronic_email1"))
    @AttributeOverride(name = "email2", column = @Column(name = "electronic_email2"))
    @AttributeOverride(name = "web", column = @Column(name = "electronic_web"))
    @AttributeOverride(name = "radio", column = @Column(name = "electronic_radio"))
    @AttributeOverride(name = "userID", column = @Column(name = "electronic_user_id"))
    @AttributeOverride(name = "password", column = @Column(name = "electronic_password"))
    private ElectronicAddress electronicAddress;

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
    @AttributeOverride(name = "value", column = @Column(name = "status_value"))
    @AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time"))
    @AttributeOverride(name = "remark", column = @Column(name = "status_remark"))
    @AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
    private Status status;

    // WorkLocation fields (WorkLocation is simply a Location specialized for work activities - no additional fields)

    // ServiceLocation specific fields

    /**
     * Method for the service person to access this service location. For example, a description of where to obtain 
     * a key if the facility is unmanned and secured.
     */
    @Column(name = "access_method", length = 256)
    private String accessMethod;

    /**
     * Problems previously encountered when visiting or performing work on this location. Examples include: 
     * bad dog, violent customer, verbally abusive occupant, obstructions, safety hazards, etc.
     */
    @Column(name = "site_access_problem", length = 256)
    private String siteAccessProblem;

    /**
     * True if inspection is needed of facilities at this service location. This could be requested by a customer,
     * due to suspected tampering, environmental concerns (e.g., a fire in the vicinity), or to correct incompatible data.
     */
    @Column(name = "needs_inspection")
    private Boolean needsInspection;

    /**
     * Collection of UsagePoint resource href URLs (cross-stream reference from customer.xsd to usage.xsd).
     * Stores the Atom self-link href URLs for UsagePoint resources, NOT Atom link elements.
     * Each string is the UsagePoint's atom:link[@rel='self']/@href value.
     *
     * Per ESPI 4.0 customer.xsd lines 1106-1111: UsagePoints element contains sequence of UsagePoint elements,
     * where each UsagePoint is of type xs:anyURI.
     *
     * Example: ["https://api.example.com/espi/1_1/resource/UsagePoint/12345",
     *           "https://api.example.com/espi/1_1/resource/UsagePoint/67890"]
     *
     * ServiceLocation exists in PII data stream, UsagePoint exists in non-PII data stream.
     * This collection provides cross-stream references without violating data separation.
     */
    @ElementCollection
    @CollectionTable(name = "service_location_usage_point_hrefs",
                     joinColumns = @JoinColumn(name = "service_location_id"))
    @Column(name = "usage_point_href", length = 512)
    private List<String> usagePointHrefs;

    /**
     * [extension] Outage Block Identifier
     */
    @Column(name = "outage_block", length = 32)
    private String outageBlock;

    // Explicit setter methods (Lombok should generate these, but adding for compatibility)
    
    /**
     * Sets whether this service location needs inspection.
     * @param needsInspection true if inspection is needed
     */
    public void setNeedsInspection(Boolean needsInspection) {
        this.needsInspection = needsInspection;
    }

    /**
     * Sets the site access problem description.
     * @param siteAccessProblem the access problem description
     */
    public void setSiteAccessProblem(String siteAccessProblem) {
        this.siteAccessProblem = siteAccessProblem;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ServiceLocationEntity that = (ServiceLocationEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + getId() + ", " +
                "type = " + getType() + ", " +
                "mainAddress = " + getMainAddress() + ", " +
                "secondaryAddress = " + getSecondaryAddress() + ", " +
                "electronicAddress = " + getElectronicAddress() + ", " +
                "geoInfoReference = " + getGeoInfoReference() + ", " +
                "direction = " + getDirection() + ", " +
                "status = " + getStatus() + ", " +
                "accessMethod = " + getAccessMethod() + ", " +
                "siteAccessProblem = " + getSiteAccessProblem() + ", " +
                "needsInspection = " + getNeedsInspection() + ", " +
                "usagePointHrefs = " + getUsagePointHrefs() + ", " +
                "outageBlock = " + getOutageBlock() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}