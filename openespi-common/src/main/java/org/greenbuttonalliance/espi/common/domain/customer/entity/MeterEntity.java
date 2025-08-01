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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for Meter without JAXB concerns.
 * 
 * Physical asset that performs the metering role of the usage point. 
 * Used for measuring consumption and detection of events.
 */
@Entity
@Table(name = "meters")
@Getter
@Setter
@NoArgsConstructor
public class MeterEntity extends EndDeviceEntity {

    /**
     * Meter form designation per ANSI C12.10 or other applicable standard. 
     * An alphanumeric designation denoting the circuit arrangement for which the meter is applicable 
     * and its specific terminal arrangement.
     */
    @Column(name = "form_number", length = 256)
    private String formNumber;

    /**
     * All multipliers applied at this meter.
     * TODO: Create MeterMultiplierEntity and enable this relationship
     */
    // @OneToMany(mappedBy = "meter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<MeterMultiplierEntity> meterMultipliers;

    /**
     * [extension] Current interval length specified in seconds.
     */
    @Column(name = "interval_length")
    private Long intervalLength;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MeterEntity that = (MeterEntity) o;
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
                "formNumber = " + getFormNumber() + ", " +
                "intervalLength = " + getIntervalLength() + ", " +
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
                "amrSystem = " + getAmrSystem() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}