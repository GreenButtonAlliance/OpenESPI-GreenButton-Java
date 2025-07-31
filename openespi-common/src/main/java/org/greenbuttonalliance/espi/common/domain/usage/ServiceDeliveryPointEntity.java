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

package org.greenbuttonalliance.espi.common.domain.usage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/**
 * JPA entity for ServiceDeliveryPoint.
 * <p>
 * Represents a physical location where energy services are delivered to a customer.
 * This is typically associated with a physical address and represents the endpoint
 * of the utility's distribution system where energy is consumed.
 * <p>
 * ServiceDeliveryPoint is now a standalone ESPI resource that extends IdentifiedObject.
 */
@Entity
@Table(name = "service_delivery_points")
@Getter
@Setter
@NoArgsConstructor
public class ServiceDeliveryPointEntity extends IdentifiedObject {
    
    /**
     * ServiceDeliveryPoint mRID - identifier for the service delivery point.
     * This is embedded within the UsagePoint but has its own identifier.
     */
    @Column(name = "sdp_mrid", length = 64)
    @Size(max = 64, message = "ServiceDeliveryPoint mRID cannot exceed 64 characters")
    private String mrid;
    

    /**
     * Human-readable name for this service delivery point.
     * Often corresponds to a physical address or location identifier.
     */
    @Column(name = "sdp_name", length = 256)
    @Size(max = 256, message = "Service delivery point name cannot exceed 256 characters")
    private String name;

    /**
     * Tariff profile identifier for this service delivery point.
     * References the rate schedule or pricing structure applicable to this location.
     */
    @Column(name = "sdp_tariff_profile", length = 256)
    @Size(max = 256, message = "Tariff profile cannot exceed 256 characters")
    private String tariffProfile;

    /**
     * Customer agreement identifier for this service delivery point.
     * References the contractual agreement between utility and customer.
     */
    @Column(name = "sdp_customer_agreement", length = 256)
    @Size(max = 256, message = "Customer agreement cannot exceed 256 characters")
    private String customerAgreement;

    /**
     * Constructor with mRID and basic information.
     * 
     * @param mrid the mRID identifier for the service delivery point
     * @param name the name of the service delivery point
     */
    public ServiceDeliveryPointEntity(String mrid, String name) {
        this.mrid = mrid;
        this.name = name;
    }

    /**
     * Constructor with mRID and full service delivery point information.
     * 
     * @param mrid the mRID identifier
     * @param description human-readable description
     * @param name the name of the service delivery point
     * @param tariffProfile the tariff profile identifier
     * @param customerAgreement the customer agreement identifier
     */
    public ServiceDeliveryPointEntity(String mrid, String description, String name, 
                                    String tariffProfile, String customerAgreement) {
        this.mrid = mrid;
        setDescription(description);
        this.name = name;
        this.tariffProfile = tariffProfile;
        this.customerAgreement = customerAgreement;
    }

    /**
     * Gets a display name for this service delivery point.
     * Uses the name if available, otherwise creates a default display name.
     * 
     * @return display name string
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        return "Service Delivery Point " + (mrid != null ? mrid : "Unknown");
    }

    /**
     * Checks if this service delivery point has a tariff profile assigned.
     * 
     * @return true if tariff profile is set, false otherwise
     */
    public boolean hasTariffProfile() {
        return tariffProfile != null && !tariffProfile.trim().isEmpty();
    }

    /**
     * Checks if this service delivery point has a customer agreement assigned.
     * 
     * @return true if customer agreement is set, false otherwise
     */
    public boolean hasCustomerAgreement() {
        return customerAgreement != null && !customerAgreement.trim().isEmpty();
    }

    /**
     * Validates the service delivery point configuration.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        // A service delivery point is considered valid if it has at least a name or mRID
        return (name != null && !name.trim().isEmpty()) || 
               (mrid != null && !mrid.trim().isEmpty());
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ServiceDeliveryPointEntity that = (ServiceDeliveryPointEntity) o;
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
                "mrid = " + getMrid() + ", " +
                "name = " + getName() + ", " +
                "tariffProfile = " + getTariffProfile() + ", " +
                "customerAgreement = " + getCustomerAgreement() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}