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
import org.greenbuttonalliance.espi.common.domain.customer.common.MeterMultiplier;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for Meter without JAXB concerns.
 *
 * Physical asset that performs the metering role of the usage point.
 * Used for measuring consumption and detection of events.
 *
 * Per NAESB ESPI 4.0 customer.xsd, Meter extends EndDevice (lines 243-259).
 * Implementation uses composition: embeds Asset + EndDeviceFields instead of inheritance.
 */
@Entity
@Table(name = "meters")
@AssociationOverride(
    name = "relatedLinks",
    joinTable = @JoinTable(
        name = "meter_related_links",
        joinColumns = @JoinColumn(name = "meter_id")
    )
)
@Getter
@Setter
@NoArgsConstructor
public class MeterEntity extends IdentifiedObject {

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
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MeterEntity that = (MeterEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

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