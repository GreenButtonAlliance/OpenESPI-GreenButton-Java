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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Pure JPA/Hibernate entity for Statement without JAXB concerns.
 * 
 * [extension] Billing statement for provided services.
 */
@Entity
@Table(name = "statements")
@AssociationOverride(
    name = "relatedLinks",
    joinTable = @JoinTable(
        name = "statement_related_links",
        joinColumns = @JoinColumn(name = "statement_id")
    )
)
@Getter
@Setter
@NoArgsConstructor
public class StatementEntity extends IdentifiedObject {

    /**
     * [extension] Date and time at which a billing statement was issued.
     */
    @Column(name = "issue_date_time")
    private OffsetDateTime issueDateTime;

    /**
     * [extension] Contains document reference metadata needed to access a document representation of a billing statement.
     * StatementRef extends Object (not IdentifiedObject), so stored as @ElementCollection.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "statement_refs", joinColumns = @JoinColumn(name = "statement_id"))
    private List<StatementRefEntity> statementRefs;
    
    /**
     * Customer associated with this statement.
     * Many statements can belong to one customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    /**
     * Customer account associated with this statement.
     * Many statements can belong to one customer account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id")
    private CustomerAccountEntity customerAccount;

    /**
     * Customer agreement associated with this statement.
     * Many statements can belong to one customer agreement.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_agreement_id")
    private CustomerAgreementEntity customerAgreement;

    /**
     * Usage summary associated with this statement.
     * Many statements can belong to one usage summary.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usage_summary_id")
    private org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity usageSummary;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        StatementEntity that = (StatementEntity) o;
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
                "description = " + getDescription() + ", " +
                "created = " + getCreated() + ", " +
                "updated = " + getUpdated() + ", " +
                "published = " + getPublished() + ", " +
                "upLink = " + getUpLink() + ", " +
                "selfLink = " + getSelfLink() + ", " +
                "issueDateTime = " + getIssueDateTime() + ", " +
                "relatedLinks = " + getRelatedLinks() + ")";
    }
}