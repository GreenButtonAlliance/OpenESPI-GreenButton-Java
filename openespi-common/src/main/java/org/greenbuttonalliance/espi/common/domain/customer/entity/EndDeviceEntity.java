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
import lombok.Delegate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for EndDevice without JAXB concerns.
 * 
 * Asset container that performs one or more end device functions. One type of end device is a meter 
 * which can perform metering, load management, connect/disconnect, accounting functions, etc. 
 * Some end devices, such as ones monitoring and controlling air conditioners, refrigerators, pool pumps 
 * may be connected to a meter. All end devices may have communication capability defined by the associated 
 * communication function(s). An end device may be owned by a consumer, a service provider, utility or otherwise.
 * 
 * There may be a related end device function that identifies a sensor or control point within a metering 
 * application or communications systems (e.g., water, gas, electricity).
 * Some devices may use an optical port that conforms to the ANSI C12.18 standard for communications.
 * 
 * This is an actual ESPI resource entity that extends IdentifiedObject directly.
 */
@Entity
@Table(name = "end_devices")
@AssociationOverride(
    name = "relatedLinks",
    joinTable = @JoinTable(
        name = "end_device_related_links",
        joinColumns = @JoinColumn(name = "end_device_id")
    )
)
@Getter
@Setter
@NoArgsConstructor
public class EndDeviceEntity extends IdentifiedObject {

    // Asset fields (embedded component per NAESB ESPI 4.0 customer.xsd lines 643-713)
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "electronicAddress.lan", column = @Column(name = "end_device_lan")),
        @AttributeOverride(name = "electronicAddress.mac", column = @Column(name = "end_device_mac")),
        @AttributeOverride(name = "electronicAddress.email1", column = @Column(name = "end_device_email1")),
        @AttributeOverride(name = "electronicAddress.email2", column = @Column(name = "end_device_email2")),
        @AttributeOverride(name = "electronicAddress.web", column = @Column(name = "end_device_web")),
        @AttributeOverride(name = "electronicAddress.radio", column = @Column(name = "end_device_radio")),
        @AttributeOverride(name = "electronicAddress.userID", column = @Column(name = "end_device_user_id")),
        @AttributeOverride(name = "electronicAddress.password", column = @Column(name = "end_device_password")),
        @AttributeOverride(name = "status.value", column = @Column(name = "status_value")),
        @AttributeOverride(name = "status.dateTime", column = @Column(name = "status_date_time")),
        @AttributeOverride(name = "status.remark", column = @Column(name = "status_remark")),
        @AttributeOverride(name = "status.reason", column = @Column(name = "status_reason")),
        @AttributeOverride(name = "acceptanceTest.dateTime", column = @Column(name = "acceptance_test_date_time")),
        @AttributeOverride(name = "acceptanceTest.success", column = @Column(name = "acceptance_test_success")),
        @AttributeOverride(name = "acceptanceTest.type", column = @Column(name = "acceptance_test_type"))
    })
    @Delegate(excludes = {Object.class})
    private Asset asset = new Asset();

    // EndDevice-specific fields (per NAESB ESPI 4.0 customer.xsd lines 218-238)
    @Embedded
    @Delegate(excludes = {Object.class})
    private EndDeviceFields endDeviceFields = new EndDeviceFields();


    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        EndDeviceEntity that = (EndDeviceEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + getId() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ", " +
                "upLink = " + getUpLink() + ", " +
                "selfLink = " + getSelfLink() + ", " +
                "type = " + getType() + ", " +
                "utcNumber = " + getUtcNumber() + ", " +
                "serialNumber = " + getSerialNumber() + ", " +
                "lotNumber = " + getLotNumber() + ", " +
                "purchasePrice = " + getPurchasePrice() + ", " +
                "critical = " + getCritical() + ", " +
                "electronicAddress = " + getElectronicAddress() + ", " +
                "lifecycle = " + getLifecycle() + ", " +
                "acceptanceTest = " + getAcceptanceTest() + ", " +
                "initialCondition = " + getInitialCondition() + ", " +
                "initialLossOfLife = " + getInitialLossOfLife() + ", " +
                "status = " + getStatus() + ", " +
                "isVirtual = " + getIsVirtual() + ", " +
                "isPan = " + getIsPan() + ", " +
                "installCode = " + getInstallCode() + ", " +
                "amrSystem = " + getAmrSystem() + ")";
    }
}