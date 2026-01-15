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

package org.greenbuttonalliance.espi.common.mapper.usage;

import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.usage.SubscriptionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Mapper for converting SubscriptionEntity to AtomFeedDto.
 *
 * <p>This is an output-only mapper that creates Atom feeds for Subscription data.
 * It handles certification link injection based on application configuration.</p>
 *
 * <p>Note: This mapper does not support DTO-to-Entity conversion as
 * Subscription feeds are output-only.</p>
 */
@Component
public class SubscriptionMapper {

    @Value("${greenbutton.certified:false}")
    private boolean certified;

    @Value("${greenbutton.certification-id:}")
    private String certificationId;

    @Value("${greenbutton.certification-base-url:https://cert.greenbuttonalliance.org/certificate/}")
    private String certificationBaseUrl;

    /**
     * Creates a SubscriptionDto builder for the given entity.
     *
     * <p>The returned builder can be used to add entries before calling toAtomFeed().</p>
     *
     * @param entity the subscription entity
     * @param schemaType the schema type (ENERGY or CUSTOMER)
     * @return a SubscriptionDto builder with self link and certification link (if applicable)
     */
    public SubscriptionDto toDto(SubscriptionEntity entity, SubscriptionDto.SchemaType schemaType) {
        if (entity == null) {
            return null;
        }

        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC);

        String subscriptionId = entity.getId() != null ? entity.getId().toString() : null;

        SubscriptionDto dto = new SubscriptionDto(subscriptionId, schemaType, now, now);

        // Add certification link if application is certified
        if (certified && certificationId != null && !certificationId.trim().isEmpty()) {
            String certificationUrl = certificationBaseUrl + certificationId;
            dto.withCertificationLink(certificationUrl);
        }

        return dto;
    }

    /**
     * Creates an AtomFeedDto directly from a SubscriptionEntity with provided entries.
     *
     * <p>This is a convenience method that combines toDto() and toAtomFeed().</p>
     *
     * @param entity the subscription entity
     * @param schemaType the schema type (ENERGY or CUSTOMER)
     * @param entries the list of Atom entries to include in the feed
     * @return configured AtomFeedDto
     */
    public AtomFeedDto toAtomFeed(SubscriptionEntity entity, SubscriptionDto.SchemaType schemaType,
                                   List<AtomEntryDto> entries) {
        SubscriptionDto dto = toDto(entity, schemaType);
        if (dto == null) {
            return null;
        }
        return dto.withEntries(entries).toAtomFeed();
    }

    /**
     * Checks if the application is GBA certified.
     *
     * @return true if certified, false otherwise
     */
    public boolean isCertified() {
        return certified;
    }

    /**
     * Gets the full certification URL if application is certified.
     *
     * @return certification URL or null if not certified
     */
    public String getCertificationUrl() {
        if (certified && certificationId != null && !certificationId.trim().isEmpty()) {
            return certificationBaseUrl + certificationId;
        }
        return null;
    }
}
