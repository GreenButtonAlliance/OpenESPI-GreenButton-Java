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

import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for StatementRef without JAXB concerns.
 * 
 * [extension] A sequence of references to a document associated with a Statement.
 * ESPI compliant with proper UUID identifiers and ATOM feed support.
 */
@Entity
@Table(name = "statement_refs")
@Getter
@Setter
@NoArgsConstructor
public class StatementRefEntity extends IdentifiedObject {

    /**
     * [extension] Name of document or file including filename extension if present.
     */
    @Column(name = "file_name", length = 512)
    private String fileName;

    /**
     * [extension] Document media type as published by IANA, see https://www.iana.org/assignments/media-types for more information.
     */
    @Column(name = "media_type", length = 256)
    private String mediaType;

    /**
     * [extension] URL used to access a representation of a statement, for example a bill image. 
     * Use CDATA or URL encoding to escape characters not allowed in XML.
     */
    @Column(name = "statement_url", nullable = false, length = 2048)
    private String statementURL;

    /**
     * Statement this reference belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id")
    private StatementEntity statement;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        StatementRefEntity that = (StatementRefEntity) o;
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
                "fileName = " + getFileName() + ", " +
                "mediaType = " + getMediaType() + ", " +
                "statementURL = " + getStatementURL() + ", " +
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ")";
    }
}