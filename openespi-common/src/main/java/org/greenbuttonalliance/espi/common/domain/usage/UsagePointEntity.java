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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.greenbuttonalliance.espi.common.domain.common.AmiBillingReadyKind;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.common.PhaseCodeKind;
import org.greenbuttonalliance.espi.common.domain.common.ServiceCategory;
import org.greenbuttonalliance.espi.common.domain.common.SummaryMeasurement;
import org.greenbuttonalliance.espi.common.domain.common.UsagePointConnectedKind;
import org.hibernate.annotations.BatchSize;
import org.hibernate.proxy.HibernateProxy;

import java.util.*;

/**
 * Pure JPA/Hibernate entity for UsagePoint without JAXB concerns.
 * <p>
 * Represents a logical point on a network at which consumption or production 
 * is either physically measured (e.g., metered) or estimated (e.g., unmetered street lights).
 */
@Entity
@Table(name = "usage_points")
@Getter
@Setter
@NoArgsConstructor
public class UsagePointEntity extends IdentifiedObject {

    private static final long serialVersionUID = 1L;

    // ==================== XSD Sequence Order (per ESPI 4.0 espi.xsd) ====================
    // Fields ordered to match exact XSD element sequence for UsagePoint complexType

    /**
     * Role flags for the usage point (hex binary representation).
     * XSD Position 1: roleFlags
     */
    @Column(name = "role_flags")
    private byte[] roleFlags;

    /**
     * Service category for this usage point.
     * Required field indicating the type of service.
     * XSD Position 2: ServiceCategory
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", nullable = false)
    private ServiceCategory serviceCategory;

    /**
     * Status of the usage point.
     * XSD Position 3: status
     */
    @Column(name = "status")
    private Short status;

    // XSD Position 4: serviceDeliveryPoint - See relationship section below

    /**
     * Tracks the lifecycle of the metering installation at a usage point with respect to
     * readiness for billing via advanced metering infrastructure reads.
     * Per ESPI 4.0 XSD: [extension] AmiBillingReadyKind enum.
     * XSD Position 5: amiBillingReady
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ami_billing_ready", length = 32)
    private AmiBillingReadyKind amiBillingReady;

    /**
     * True if as a result of an inspection or otherwise, there is a reason to suspect
     * that a previous billing may have been performed with erroneous data.
     * Value should be reset once this potential discrepancy has been resolved.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     * XSD Position 6: checkBilling
     */
    @Column(name = "check_billing")
    private Boolean checkBilling;

    /**
     * State of the usage point with respect to connection to the network.
     * Per ESPI 4.0 XSD: [extension] UsagePointConnectedKind enum.
     * XSD Position 7: connectionState
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "connection_state", length = 32)
    private UsagePointConnectedKind connectionState;

    /**
     * Estimated load for this usage point as SummaryMeasurement.
     * Contains value, unit of measure, multiplier, and reading type reference.
     * XSD Position 8: estimatedLoad
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "powerOfTenMultiplier", column = @Column(name = "estimated_load_multiplier", columnDefinition = "SMALLINT")),
        @AttributeOverride(name = "timeStamp", column = @Column(name = "estimated_load_timestamp")),
        @AttributeOverride(name = "uom", column = @Column(name = "estimated_load_uom")),
        @AttributeOverride(name = "value", column = @Column(name = "estimated_load_value")),
        @AttributeOverride(name = "readingTypeRef", column = @Column(name = "estimated_load_reading_type_ref", length = 512))
    })
    private SummaryMeasurement estimatedLoad;

    /**
     * True if grounded.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     * XSD Position 9: grounded
     */
    @Column(name = "grounded")
    private Boolean grounded;

    /**
     * If true, this usage point is a service delivery point, i.e., a usage point
     * where the ownership of the service changes hands.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     * XSD Position 10: isSdp
     */
    @Column(name = "is_sdp")
    private Boolean isSdp;

    /**
     * If true, this usage point is virtual, i.e., no physical location exists in the
     * network where a meter could be located to collect the meter readings.
     * For example, one may define a virtual usage point to serve as an aggregation of
     * usage for all of a company's premises distributed widely across the distribution territory.
     * Otherwise, the usage point is physical, i.e., there is a logical point in the network
     * where a meter could be located to collect meter readings.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     * XSD Position 11: isVirtual
     */
    @Column(name = "is_virtual")
    private Boolean isVirtual;

    /**
     * If true, minimal or zero usage is expected at this usage point for situations such as
     * premises vacancy, logical or physical disconnect.
     * It is used for readings validation and estimation.
     * Per ESPI 4.0 XSD: [extension] boolean field.
     * XSD Position 12: minimalUsageExpected
     */
    @Column(name = "minimal_usage_expected")
    private Boolean minimalUsageExpected;

    /**
     * Nominal service voltage for this usage point as SummaryMeasurement.
     * Contains value, unit of measure, multiplier, and reading type reference.
     * XSD Position 13: nominalServiceVoltage
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "powerOfTenMultiplier", column = @Column(name = "nominal_voltage_multiplier")),
        @AttributeOverride(name = "timeStamp", column = @Column(name = "nominal_voltage_timestamp")),
        @AttributeOverride(name = "uom", column = @Column(name = "nominal_voltage_uom")),
        @AttributeOverride(name = "value", column = @Column(name = "nominal_voltage_value")),
        @AttributeOverride(name = "readingTypeRef", column = @Column(name = "nominal_voltage_reading_type_ref", length = 512))
    })
    private SummaryMeasurement nominalServiceVoltage;

    /**
     * Outage region in which this usage point is located.
     * Per ESPI 4.0 XSD: [extension] String256 field (max length 256).
     * XSD Position 14: outageRegion
     */
    @Column(name = "outage_region", length = 256)
    private String outageRegion;

    /**
     * Phase code. Number of wires and specific nominal phases can be deduced from
     * enumeration literal values. For example, ABCN is three-phase, four-wire,
     * s12n (splitSecondary12N) is single-phase, three-wire, and s1n and s2n are
     * single-phase, two-wire.
     * Per ESPI 4.0 XSD: [extension] PhaseCodeKind enum.
     * XSD Position 15: phaseCode
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase_code", length = 32)
    private PhaseCodeKind phaseCode;

    /**
     * Rated current for this usage point as SummaryMeasurement.
     * Contains value, unit of measure, multiplier, and reading type reference.
     * XSD Position 16: ratedCurrent
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "powerOfTenMultiplier", column = @Column(name = "rated_current_multiplier")),
        @AttributeOverride(name = "timeStamp", column = @Column(name = "rated_current_timestamp")),
        @AttributeOverride(name = "uom", column = @Column(name = "rated_current_uom")),
        @AttributeOverride(name = "value", column = @Column(name = "rated_current_value")),
        @AttributeOverride(name = "readingTypeRef", column = @Column(name = "rated_current_reading_type_ref", length = 512))
    })
    private SummaryMeasurement ratedCurrent;

    /**
     * Rated power for this usage point as SummaryMeasurement.
     * Contains value, unit of measure, multiplier, and reading type reference.
     * XSD Position 17: ratedPower
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "powerOfTenMultiplier", column = @Column(name = "rated_power_multiplier")),
        @AttributeOverride(name = "timeStamp", column = @Column(name = "rated_power_timestamp")),
        @AttributeOverride(name = "uom", column = @Column(name = "rated_power_uom")),
        @AttributeOverride(name = "value", column = @Column(name = "rated_power_value")),
        @AttributeOverride(name = "readingTypeRef", column = @Column(name = "rated_power_reading_type_ref", length = 512))
    })
    private SummaryMeasurement ratedPower;

    /**
     * Cycle day on which the meter for this usage point will normally be read.
     * Usually correlated with the billing cycle.
     * Per ESPI 4.0 XSD: [extension] String256 field (max length 256).
     * XSD Position 18: readCycle
     */
    @Column(name = "read_cycle", length = 256)
    private String readCycle;

    /**
     * Identifier of the route to which this usage point is assigned for purposes of meter reading.
     * Typically used to configure hand held meter reading systems prior to collection of reads.
     * Per ESPI 4.0 XSD: [extension] String256 field (max length 256).
     * XSD Position 19: readRoute
     */
    @Column(name = "read_route", length = 256)
    private String readRoute;

    /**
     * Remarks about this usage point, for example the reason for it being rated with a non-nominal priority.
     * Per ESPI 4.0 XSD: [extension] String256 field (max length 256).
     * XSD Position 20: serviceDeliveryRemark
     */
    @Column(name = "service_delivery_remark", length = 256)
    private String serviceDeliveryRemark;

    /**
     * Priority of service for this usage point.
     * Note that usage points at the same service location can have different priorities.
     * Per ESPI 4.0 XSD: [extension] String32 field (max length 32).
     * XSD Position 21: servicePriority
     */
    @Column(name = "service_priority", length = 32)
    private String servicePriority;

    // XSD Position 22-23: pnodeRefs and aggregateNodeRefs - See relationship sections below

    // ==================== Legacy Fields (NOT in ESPI 4.0 XSD) ====================
    // TODO Phase 16c: Review if these fields should be removed or mapped to XSD elements

    /**
     * URI for this usage point.
     * Used for external references and linking.
     * NOTE: This field is NOT in ESPI 4.0 XSD - legacy field for review.
     */
    @Column(name = "uri")
    private String uri;

    // ==================== JPA Relationships ====================

    /**
     * Service delivery point associated with this usage point.
     * ServiceDeliveryPoint is now a standalone ESPI resource.
     */
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_delivery_point_id")
    private ServiceDeliveryPointEntity serviceDeliveryPoint;

    /**
     * Retail customer that owns this usage point.
     * Many usage points can belong to one retail customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retail_customer_id")
    private RetailCustomerEntity retailCustomer;

    /**
     * Time configuration parameters for this usage point.
     * Many usage points can share the same time configuration.
     */
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "local_time_parameters_id")
    private TimeConfigurationEntity localTimeParameters;

    /**
     * Meter readings associated with this usage point.
     * One-to-many relationship with cascade and orphan removal.
     * Optimized with lazy loading and batch size to prevent N+1 queries.
     */
    @OneToMany(mappedBy = "usagePoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<MeterReadingEntity> meterReadings = new ArrayList<>();

    // ElectricPowerUsageSummary relationships removed - deprecated resource

    /**
     * Usage summaries for this usage point.
     * One-to-many relationship with cascade and orphan removal.
     * Optimized with lazy loading and batch size to prevent N+1 queries.
     */
    @OneToMany(mappedBy = "usagePoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 15)
    private List<UsageSummaryEntity> usageSummaries = new ArrayList<>();

    /**
     * Electric power quality summaries for this usage point.
     * One-to-many relationship with cascade and orphan removal.
     * Optimized with lazy loading and batch size to prevent N+1 queries.
     */
    @OneToMany(mappedBy = "usagePoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<ElectricPowerQualitySummaryEntity> electricPowerQualitySummaries = new ArrayList<>();

    /**
     * Subscriptions that include this usage point.
     * Many-to-many relationship mapped by the subscriptions side.
     * Optimized with lazy loading and batch size to prevent N+1 queries.
     */
    @ManyToMany(mappedBy = "usagePoints", fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    private Set<SubscriptionEntity> subscriptions = new HashSet<>();

    /**
     * Single subscription reference for this usage point.
     * Optional one-to-one relationship.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private SubscriptionEntity subscription;

    /**
     * Pricing node references for this usage point.
     * One-to-many relationship with cascade and orphan removal.
     * Optimized with lazy loading and batch size to prevent N+1 queries.
     */
    @OneToMany(mappedBy = "usagePoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 30)
    private List<PnodeRefEntity> pnodeRefs = new ArrayList<>();

    /**
     * Aggregated node references for this usage point.
     * One-to-many relationship with cascade and orphan removal.
     * Optimized with lazy loading and batch size to prevent N+1 queries.
     */
    @OneToMany(mappedBy = "usagePoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 30)
    private List<AggregatedNodeRefEntity> aggregatedNodeRefs = new ArrayList<>();

    /**
     * Manual getter for ID field (Lombok issue workaround).
     * 
     * @return the entity ID
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Generates the self href for this usage point.
     * 
     * @return self href string
     */
    public String getSelfHref() {
        return getUpHref() + "/" + getHashedId();
    }

    /**
     * Generates the up href for this usage point.
     * 
     * @return up href string pointing to the retail customer
     */
    public String getUpHref() {
        if (retailCustomer != null) {
            return "RetailCustomer/" + retailCustomer.getId() + "/UsagePoint";
        }
        return "/espi/1_1/resource/UsagePoint";
    }

    // Note: Meter reading collection accessors are generated by Lombok @Data
    // Bidirectional relationship management methods removed - handled by DataCustodian/ThirdParty applications

    // ElectricPowerUsageSummary management methods removed - deprecated resource

    // Note: Usage summary collection accessors are generated by Lombok @Data
    // Bidirectional relationship management methods removed - handled by DataCustodian/ThirdParty applications

    // Note: Electric power quality summary collection accessors are generated by Lombok @Data
    // Bidirectional relationship management methods removed - handled by DataCustodian/ThirdParty applications

    // Note: Subscription collection accessors are generated by Lombok @Data
    // Bidirectional relationship management methods removed - handled by DataCustodian/ThirdParty applications

    /**
     * Overrides the default self href generation to use usage point specific logic.
     * 
     * @return self href for this usage point
     */
    @Override
    protected String generateDefaultSelfHref() {
        return getSelfHref();
    }

    /**
     * Overrides the default up href generation to use usage point specific logic.
     * 
     * @return up href for this usage point
     */
    @Override
    protected String generateDefaultUpHref() {
        return getUpHref();
    }

    /**
     * Merges data from another UsagePointEntity.
     * 
     * @param other the other usage point entity to merge from
     */
    public void merge(UsagePointEntity other) {
        if (other != null) {
            super.merge(other);
            this.serviceCategory = other.serviceCategory;
            this.status = other.status;
            this.roleFlags = other.roleFlags;
            this.uri = other.uri;
        }
    }

    /**
     * Clears all relationships when unlinking the entity.
     */
    public void unlink() {
        clearRelatedLinks();
        electricPowerQualitySummaries.clear();
        // electricPowerUsageSummaries removed - deprecated resource
        usageSummaries.clear();
        meterReadings.clear();
        pnodeRefs.clear();
        aggregatedNodeRefs.clear();
        retailCustomer = null;
        subscriptions.clear();
        subscription = null;
    }

    // Note: equals() and hashCode() methods are generated by Lombok @Data annotation

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        UsagePointEntity that = (UsagePointEntity) o;
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
                "roleFlags = " + getRoleFlags() + ", " +
                "serviceCategory = " + getServiceCategory() + ", " +
                "status = " + getStatus() + ", " +
                "uri = " + getUri() + ", " +
                "estimatedLoad = " + getEstimatedLoad() + ", " +
                "nominalServiceVoltage = " + getNominalServiceVoltage() + ", " +
                "ratedCurrent = " + getRatedCurrent() + ", " +
                "ratedPower = " + getRatedPower() + ", " +
                "localTimeParameters = " + getLocalTimeParameters() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}