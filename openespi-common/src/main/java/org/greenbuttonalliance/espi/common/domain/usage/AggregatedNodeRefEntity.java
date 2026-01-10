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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity for AggregatedNodeRef (Aggregated Node Reference).
 *
 * Represents a reference to an aggregated node in the electrical grid used within UsagePoint.
 * Each aggregated node reference includes an associated pricing node reference.
 *
 * Note: AggregatedNodeRef does NOT extend IdentifiedObject per ESPI 4.0 specification.
 * It is not a top-level resource with selfLink/upLink/relatedLinks.
 */
@Entity
@Table(name = "aggregated_node_refs")
@Getter
@Setter
@NoArgsConstructor
public class AggregatedNodeRefEntity {

    /**
     * Primary key identifier.
     * AggregatedNodeRef extends Object (not IdentifiedObject), so uses Long ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    /**
     * Type of the aggregated node.
     * Examples: "LOAD_ZONE", "TRANSMISSION_ZONE", "DISTRIBUTION_ZONE", "MARKET_ZONE"
     */
    @Column(name = "anode_type", length = 64)
    private String anodeType;

    /**
     * Reference to the aggregated node identifier.
     * Examples: "CAISO_PGAE_VALLEY_AGG", "PATH26_AGGREGATE"
     */
    @Column(name = "ref", length = 256, nullable = false)
    private String ref;

    /**
     * Start effective date for the aggregated node reference validity.
     * Stored as epoch seconds (TimeType in ESPI).
     */
    @Column(name = "start_effective_date")
    private Long startEffectiveDate;

    /**
     * End effective date for the aggregated node reference validity.
     * Stored as epoch seconds (TimeType in ESPI). Null for indefinite validity.
     */
    @Column(name = "end_effective_date")
    private Long endEffectiveDate;

    /**
     * Associated pricing node references for this aggregated node.
     * Per ESPI 4.0 XSD (espi.xsd:1597), each aggregated node can reference 0 to many pricing nodes.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "aggregated_node_ref_pnode_refs",
        joinColumns = @JoinColumn(name = "aggregated_node_ref_id"),
        inverseJoinColumns = @JoinColumn(name = "pnode_ref_id")
    )
    private List<PnodeRefEntity> pnodeRefs = new ArrayList<>();

    /**
     * Usage point that owns this aggregated node reference.
     * Many aggregated node references can belong to one usage point.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usage_point_id", nullable = false)
    private UsagePointEntity usagePoint;

    /**
     * Constructor with all fields.
     */
    public AggregatedNodeRefEntity(String anodeType, String ref, Long startEffectiveDate, Long endEffectiveDate,
                                 List<PnodeRefEntity> pnodeRefs, UsagePointEntity usagePoint) {
        this.anodeType = anodeType;
        this.ref = ref;
        this.startEffectiveDate = startEffectiveDate;
        this.endEffectiveDate = endEffectiveDate;
        this.pnodeRefs = pnodeRefs != null ? pnodeRefs : new ArrayList<>();
        this.usagePoint = usagePoint;
    }

    /**
     * Constructor with basic fields.
     */
    public AggregatedNodeRefEntity(String anodeType, String ref, List<PnodeRefEntity> pnodeRefs, UsagePointEntity usagePoint) {
        this(anodeType, ref, null, null, pnodeRefs, usagePoint);
    }

    /**
     * Checks if this aggregated node reference is currently valid.
     * 
     * @return true if valid for current time
     */
    public boolean isValid() {
        long currentTime = System.currentTimeMillis() / 1000;
        return (startEffectiveDate == null || startEffectiveDate <= currentTime) && 
               (endEffectiveDate == null || endEffectiveDate >= currentTime);
    }

    /**
     * Gets display name for this aggregated node reference.
     * 
     * @return formatted display name
     */
    public String getDisplayName() {
        if (anodeType != null && ref != null) {
            return anodeType + ":" + ref;
        }
        return ref != null ? ref : "Unknown";
    }

    /**
     * Gets display name including the associated pricing nodes.
     *
     * @return formatted display name with pricing nodes
     */
    public String getFullDisplayName() {
        String aggregatedDisplay = getDisplayName();
        if (pnodeRefs != null && !pnodeRefs.isEmpty()) {
            String pnodeNames = pnodeRefs.stream()
                .map(PnodeRefEntity::getDisplayName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            return aggregatedDisplay + " -> [" + pnodeNames + "]";
        }
        return aggregatedDisplay;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AggregatedNodeRefEntity that = (AggregatedNodeRefEntity) o;
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
                "anodeType = " + getAnodeType() + ", " +
                "ref = " + getRef() + ", " +
                "startEffectiveDate = " + getStartEffectiveDate() + ", " +
                "endEffectiveDate = " + getEndEffectiveDate() + ")";
    }
}