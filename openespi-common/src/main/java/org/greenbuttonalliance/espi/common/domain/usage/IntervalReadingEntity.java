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
import lombok.*;
import org.greenbuttonalliance.espi.common.domain.common.DateTimeInterval;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.hibernate.annotations.BatchSize;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for IntervalReading without JAXB concerns.
 * <p>
 * Represents a specific value measured by a meter or other asset.
 * Each reading is associated with a specific ReadingType and contains
 * cost, value, consumption tier, time-of-use, and critical peak pricing information.
 */
@Entity
@Table(name = "interval_readings")
@Getter
@Setter
@NoArgsConstructor
public class IntervalReadingEntity extends IdentifiedObject {

    private static final long serialVersionUID = 1L;

    /**
     * Cost associated with this interval reading.
     * Represents the monetary cost for this measurement period.
     */
    @Column(name = "cost")
    private Long cost;

    /**
     * The actual measured value for this interval.
     * This is the primary measurement data.
     */
    @Column(name = "reading_value") //value is a SQL key word
    private Long value;

    /**
     * Consumption tier indicator.
     * Used for tiered pricing structures.
     */
    @Column(name = "consumption_tier")
    private Long consumptionTier;

    /**
     * Time-of-use indicator.
     * Used for time-based pricing structures.
     */
    @Column(name = "tou")
    private Long tou;

    /**
     * Critical peak pricing indicator.
     * Used for demand response pricing programs.
     */
    @Column(name = "cpp")
    private Long cpp;

    /**
     * Time period for this interval reading.
     * Embedded value object containing start time and duration.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "start", column = @Column(name = "time_period_start")),
        @AttributeOverride(name = "duration", column = @Column(name = "time_period_duration"))
    })
    private DateTimeInterval timePeriod;

    /**
     * Interval block that contains this reading.
     * Many interval readings belong to one interval block.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interval_block_id")
    private IntervalBlockEntity intervalBlock;

    /**
     * Reading quality indicators for this interval reading.
     * One-to-many relationship with cascade and orphan removal.
     * -- GETTER --
     *  Gets the reading qualities collection.
     *  Lombok @Data should generate this, but added manually for compilation.
     *
     * @return the list of reading qualities

     */
    @OneToMany(mappedBy = "intervalReading", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<ReadingQualityEntity> readingQualities = new ArrayList<>();

    /**
     * Constructor with core reading data.
     * 
     * @param value the measured value
     * @param cost the cost for this reading
     * @param timePeriod the time period for this reading
     */
    public IntervalReadingEntity(Long value, Long cost, DateTimeInterval timePeriod) {
        this.value = value;
        this.cost = cost;
        this.timePeriod = timePeriod;
    }

    /**
     * Adds a reading quality to this interval reading.
     * Manages bidirectional relationship.
     * 
     * @param readingQuality the reading quality to add
     */
    public void addReadingQuality(ReadingQualityEntity readingQuality) {
        if (readingQuality != null) {
            if (!readingQualities.contains(readingQuality)) {
                readingQualities.add(readingQuality);
            }
            if (readingQuality.getIntervalReading() != this) {
                readingQuality.setIntervalReading(this);
            }
        }
    }

    /**
     * Removes a reading quality from this interval reading.
     * Manages bidirectional relationship.
     * 
     * @param readingQuality the reading quality to remove
     */
    public void removeReadingQuality(ReadingQualityEntity readingQuality) {
        if (readingQuality != null) {
            readingQualities.remove(readingQuality);
            if (readingQuality.getIntervalReading() == this) {
                readingQuality.setIntervalReading(null);
            }
        }
    }

    /**
     * Gets the consumption tier as an integer.
     * 
     * @return consumption tier as integer, or null if not set
     */
    public Integer getConsumptionTierAsInt() {
        return consumptionTier != null ? consumptionTier.intValue() : null;
    }

    /**
     * Sets the consumption tier from an integer value.
     * 
     * @param tier the consumption tier as integer
     */
    public void setConsumptionTierAsInt(Integer tier) {
        this.consumptionTier = tier != null ? tier.longValue() : null;
    }

    /**
     * Gets the time-of-use as an integer.
     * 
     * @return time-of-use as integer, or null if not set
     */
    public Integer getTouAsInt() {
        return tou != null ? tou.intValue() : null;
    }

    /**
     * Sets the time-of-use from an integer value.
     * 
     * @param touValue the time-of-use as integer
     */
    public void setTouAsInt(Integer touValue) {
        this.tou = touValue != null ? touValue.longValue() : null;
    }

    /**
     * Gets the critical peak pricing as an integer.
     * 
     * @return critical peak pricing as integer, or null if not set
     */
    public Integer getCppAsInt() {
        return cpp != null ? cpp.intValue() : null;
    }

    /**
     * Sets the critical peak pricing from an integer value.
     * 
     * @param cppValue the critical peak pricing as integer
     */
    public void setCppAsInt(Integer cppValue) {
        this.cpp = cppValue != null ? cppValue.longValue() : null;
    }

    /**
     * Merges data from another IntervalReadingEntity.
     * Updates all measurement values and relationships.
     * 
     * @param other the other interval reading entity to merge from
     */
    public void merge(IntervalReadingEntity other) {
        if (other != null) {
            this.cost = other.cost;
            this.value = other.value;
            this.consumptionTier = other.consumptionTier;
            this.tou = other.tou;
            this.cpp = other.cpp;
            this.timePeriod = other.timePeriod;
            
            // Replace reading qualities - bidirectional relationship setup handled by applications
            if (other.readingQualities != null) {
                this.readingQualities = new ArrayList<>(other.readingQualities);
            }
            
            // Update interval block if provided
            if (other.intervalBlock != null) {
                this.intervalBlock = other.intervalBlock;
            }
        }
    }

    /**
     * Clears all relationships when unlinking the entity.
     * Simplified - applications handle relationship cleanup.
     */
    public void unlink() {
        // Simple collection clearing - applications handle bidirectional cleanup
        readingQualities.clear();
        
        // Clear relationships with simple field assignment
        this.intervalBlock = null;
    }

    /**
     * Checks if this reading has any quality indicators.
     * 
     * @return true if there are reading qualities, false otherwise
     */
    public boolean hasQualityIndicators() {
        return readingQualities != null && !readingQualities.isEmpty();
    }

    /**
     * Gets the total number of quality indicators.
     * 
     * @return count of reading qualities
     */
    public int getQualityIndicatorCount() {
        return readingQualities != null ? readingQualities.size() : 0;
    }

    /**
     * Checks if this reading represents energy consumption (positive value).
     * 
     * @return true if value is positive, false otherwise
     */
    public boolean isConsumption() {
        return value != null && value > 0;
    }

    /**
     * Checks if this reading represents energy production (negative value).
     * 
     * @return true if value is negative, false otherwise
     */
    public boolean isProduction() {
        return value != null && value < 0;
    }

    /**
     * Gets the absolute value of the reading.
     * 
     * @return absolute value, or null if value is not set
     */
    public Long getAbsoluteValue() {
        return value != null ? Math.abs(value) : null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        IntervalReadingEntity that = (IntervalReadingEntity) o;
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
                "cost = " + getCost() + ", " +
                "value = " + getValue() + ", " +
                "consumptionTier = " + getConsumptionTier() + ", " +
                "tou = " + getTou() + ", " +
                "cpp = " + getCpp() + ", " +
                "timePeriod = " + getTimePeriod() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}