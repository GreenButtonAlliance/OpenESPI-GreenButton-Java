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

import lombok.*;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.customer.common.ProgramDateIdMapping;
import org.hibernate.proxy.HibernateProxy;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for ProgramDateIdMappings without JAXB concerns.
 *
 * [extension] Collection of all customer Energy Efficiency programs.
 * Per customer.xsd lines 269-283, ProgramDateIdMappings extends IdentifiedObject
 * and contains a single embedded ProgramDateIdMapping object.
 */
@Entity
@Table(name = "program_date_id_mappings")
@AssociationOverride(
    name = "relatedLinks",
    joinTable = @JoinTable(
        name = "program_date_id_mapping_related_links",
        joinColumns = @JoinColumn(name = "program_date_id_mapping_id")
    )
)
@Getter
@Setter
@NoArgsConstructor
public class ProgramDateIdMappingsEntity extends IdentifiedObject {

    /**
     * Single customer energy efficiency program date mapping.
     * Per customer.xsd, this is the only field in ProgramDateIdMappings beyond IdentifiedObject fields.
     */
    @Embedded
    private ProgramDateIdMapping programDateIdMapping;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ProgramDateIdMappingsEntity that = (ProgramDateIdMappingsEntity) o;
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
                "programDateIdMapping = " + programDateIdMapping + ", " +
                "relatedLinks = " + getRelatedLinks() + ")";
    }
}