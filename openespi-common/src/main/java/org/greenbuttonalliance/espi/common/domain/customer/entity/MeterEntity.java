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
import org.greenbuttonalliance.espi.common.domain.customer.common.MeterMultiplier;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
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
     * Per customer.xsd Meter.MeterMultipliers (collection of MeterMultiplier embeddables).
     */
    @ElementCollection
    @CollectionTable(name = "meter_multipliers", joinColumns = @JoinColumn(name = "meter_id"))
    @AttributeOverride(name = "kind", column = @Column(name = "multiplier_kind"))
    @AttributeOverride(name = "value", column = @Column(name = "multiplier_value"))
    private List<MeterMultiplier> meterMultipliers;

    /**
     * [extension] Current interval length specified in seconds.
     */
    @Column(name = "interval_length")
    private Long intervalLength;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                // IdentifiedObject fields
                "id = " + getId() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ", " +
                // Asset fields (via EndDevice)
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
                // EndDevice fields
                "isVirtual = " + getIsVirtual() + ", " +
                "isPan = " + getIsPan() + ", " +
                "installCode = " + getInstallCode() + ", " +
                "amrSystem = " + getAmrSystem() + ", " +
                // Meter-specific fields (per customer.xsd Meter sequence)
                "formNumber = " + getFormNumber() + ", " +
                "meterMultipliers = " + getMeterMultipliers() + ", " +
                "intervalLength = " + getIntervalLength() + ")";
    }
}