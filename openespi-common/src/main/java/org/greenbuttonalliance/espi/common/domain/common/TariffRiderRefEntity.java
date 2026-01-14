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

package org.greenbuttonalliance.espi.common.domain.common;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for TariffRiderRef without JAXB concerns.
 * <p>
 * Represents a reference to a tariff rider applied to a usage summary.
 * TariffRiderRef extends Object (not IdentifiedObject) per ESPI 4.0 XSD (espi.xsd:1602),
 * so it uses Long ID with auto-increment instead of UUID.
 * <p>
 * Part of TariffRiderRefs collection in UsageSummary.
 */
@Entity
@Table(name = "tariff_rider_refs", indexes = {
    @Index(name = "idx_tariff_rider_ref_usage_summary", columnList = "usage_summary_id"),
    @Index(name = "idx_tariff_rider_ref_rider_type", columnList = "rider_type"),
    @Index(name = "idx_tariff_rider_ref_enrollment_status", columnList = "enrollment_status")
})
@Getter
@Setter
@NoArgsConstructor
public class TariffRiderRefEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key - Long ID with auto-increment.
     * TariffRiderRef extends Object, not IdentifiedObject.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    /**
     * Type of tariff rider.
     * Rate options applied to the base tariff profile.
     * Required field per ESPI 4.0 XSD.
     */
    @Column(name = "rider_type", length = 256, nullable = false)
    @NotBlank(message = "Rider type cannot be blank")
    private String riderType;

    /**
     * Retail customer's tariff rider enrollment status.
     * EnrollmentStatus enumeration values: "unenrolled", "enrolled", "enrolledPending".
     * Required field per ESPI 4.0 XSD.
     */
    @Column(name = "enrollment_status", length = 32, nullable = false)
    @NotBlank(message = "Enrollment status cannot be blank")
    private String enrollmentStatus;

    /**
     * Effective date of retail customer's tariff rider enrollment status.
     * Stored as epoch seconds (TimeType in ESPI).
     * Required field per ESPI 4.0 XSD.
     */
    @Column(name = "effective_date", nullable = false)
    @NotNull(message = "Effective date cannot be null")
    private Long effectiveDate;

    /**
     * Parent usage summary that this tariff rider ref belongs to.
     * Many tariff rider refs can belong to one usage summary.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usage_summary_id")
    private UsageSummaryEntity usageSummary;

    /**
     * Constructor with required fields.
     *
     * @param riderType the type of tariff rider
     * @param enrollmentStatus the enrollment status ("unenrolled", "enrolled", "enrolledPending")
     * @param effectiveDate the effective date as epoch seconds
     */
    public TariffRiderRefEntity(String riderType, String enrollmentStatus, Long effectiveDate) {
        this.riderType = riderType;
        this.enrollmentStatus = enrollmentStatus;
        this.effectiveDate = effectiveDate;
    }

    /**
     * Validates the tariff rider ref has all required fields.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return riderType != null && !riderType.trim().isEmpty() &&
               enrollmentStatus != null && !enrollmentStatus.trim().isEmpty() &&
               effectiveDate != null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        TariffRiderRefEntity that = (TariffRiderRefEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
            : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + id + ", " +
                "riderType = " + riderType + ", " +
                "enrollmentStatus = " + enrollmentStatus + ", " +
                "effectiveDate = " + effectiveDate + ")";
    }
}
