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

package org.greenbuttonalliance.espi.common.dto.usage;

import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.LinkDto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating Subscription Atom Feeds with complete energy data.
 *
 * <p>Creates an AtomFeedDto configured for Green Button Subscription output,
 * containing all related energy data entries. This is output-only and generates
 * Atom feeds for third-party applications accessing subscribed data.</p>
 *
 * <p>Feed structure:</p>
 * <ul>
 *   <li>ID: UUID5 format (urn:uuid:xxxxxxxx-xxxx-5xxx-xxxx-xxxxxxxxxxxx)</li>
 *   <li>Title: "Green Button Energy Feed" or "Green Button Customer Feed"</li>
 *   <li>Self link: https://greenbuttondata.org/espi/1_1/resource/Subscription/{id}</li>
 *   <li>Related link: Certification URL (only if application is GBA certified)</li>
 * </ul>
 *
 * <p>Entries included (in order):</p>
 * <ol>
 *   <li>UsagePoint entries</li>
 *   <li>MeterReading entries</li>
 *   <li>ReadingType entries</li>
 *   <li>IntervalBlock entries</li>
 *   <li>TimeConfiguration entry (LocalTimeParameters)</li>
 *   <li>UsageSummary entries (if data exists)</li>
 *   <li>ElectricPowerQualitySummary entries (if data exists)</li>
 * </ol>
 */
public class SubscriptionDto {

    private static final String SELF_LINK_BASE = "https://greenbuttondata.org/espi/1_1/resource/Subscription/";
    private static final String SELF_LINK_TYPE = "espi-entry/Subscription";
    private static final String ENERGY_FEED_TITLE = "Green Button Energy Feed";
    private static final String CUSTOMER_FEED_TITLE = "Green Button Customer Feed";

    /**
     * Schema type for determining feed title and namespace.
     */
    public enum SchemaType {
        ENERGY,   // espi: namespace
        CUSTOMER  // cust: namespace
    }

    private final String subscriptionId;
    private final SchemaType schemaType;
    private final OffsetDateTime published;
    private final OffsetDateTime updated;
    private final List<LinkDto> links;
    private final List<AtomEntryDto> entries;

    /**
     * Creates a new SubscriptionDto builder.
     *
     * @param subscriptionId the UUID5 subscription ID
     * @param schemaType the schema type (ENERGY or CUSTOMER)
     * @param published the published timestamp
     * @param updated the updated timestamp
     */
    public SubscriptionDto(String subscriptionId, SchemaType schemaType,
                          OffsetDateTime published, OffsetDateTime updated) {
        this.subscriptionId = subscriptionId;
        this.schemaType = schemaType;
        this.published = published;
        this.updated = updated;
        this.links = new ArrayList<>();
        this.entries = new ArrayList<>();

        // Add self link
        this.links.add(new LinkDto("self", SELF_LINK_BASE + subscriptionId, SELF_LINK_TYPE));
    }

    /**
     * Adds the GBA certification related link if the application is certified.
     *
     * @param certificationUrl the full certification URL (base + certification ID)
     * @return this builder for chaining
     */
    public SubscriptionDto withCertificationLink(String certificationUrl) {
        if (certificationUrl != null && !certificationUrl.trim().isEmpty()) {
            this.links.add(new LinkDto("related", certificationUrl, "text/html"));
        }
        return this;
    }

    /**
     * Adds UsagePoint entries to the feed.
     *
     * @param usagePointEntries the UsagePoint entries
     * @return this builder for chaining
     */
    public SubscriptionDto withUsagePoints(List<AtomEntryDto> usagePointEntries) {
        if (usagePointEntries != null) {
            this.entries.addAll(usagePointEntries);
        }
        return this;
    }

    /**
     * Adds MeterReading entries to the feed.
     *
     * @param meterReadingEntries the MeterReading entries
     * @return this builder for chaining
     */
    public SubscriptionDto withMeterReadings(List<AtomEntryDto> meterReadingEntries) {
        if (meterReadingEntries != null) {
            this.entries.addAll(meterReadingEntries);
        }
        return this;
    }

    /**
     * Adds ReadingType entries to the feed.
     *
     * @param readingTypeEntries the ReadingType entries
     * @return this builder for chaining
     */
    public SubscriptionDto withReadingTypes(List<AtomEntryDto> readingTypeEntries) {
        if (readingTypeEntries != null) {
            this.entries.addAll(readingTypeEntries);
        }
        return this;
    }

    /**
     * Adds IntervalBlock entries to the feed.
     *
     * @param intervalBlockEntries the IntervalBlock entries
     * @return this builder for chaining
     */
    public SubscriptionDto withIntervalBlocks(List<AtomEntryDto> intervalBlockEntries) {
        if (intervalBlockEntries != null) {
            this.entries.addAll(intervalBlockEntries);
        }
        return this;
    }

    /**
     * Adds TimeConfiguration (LocalTimeParameters) entry to the feed.
     *
     * @param timeConfigurationEntry the TimeConfiguration entry
     * @return this builder for chaining
     */
    public SubscriptionDto withTimeConfiguration(AtomEntryDto timeConfigurationEntry) {
        if (timeConfigurationEntry != null) {
            this.entries.add(timeConfigurationEntry);
        }
        return this;
    }

    /**
     * Adds UsageSummary entries to the feed if data exists.
     *
     * @param usageSummaryEntries the UsageSummary entries (may be empty)
     * @return this builder for chaining
     */
    public SubscriptionDto withUsageSummaries(List<AtomEntryDto> usageSummaryEntries) {
        if (usageSummaryEntries != null && !usageSummaryEntries.isEmpty()) {
            this.entries.addAll(usageSummaryEntries);
        }
        return this;
    }

    /**
     * Adds ElectricPowerQualitySummary entries to the feed if data exists.
     *
     * @param electricPowerQualitySummaryEntries the ElectricPowerQualitySummary entries (may be empty)
     * @return this builder for chaining
     */
    public SubscriptionDto withElectricPowerQualitySummaries(List<AtomEntryDto> electricPowerQualitySummaryEntries) {
        if (electricPowerQualitySummaryEntries != null && !electricPowerQualitySummaryEntries.isEmpty()) {
            this.entries.addAll(electricPowerQualitySummaryEntries);
        }
        return this;
    }

    /**
     * Adds a generic entry to the feed.
     *
     * @param entry the Atom entry to add
     * @return this builder for chaining
     */
    public SubscriptionDto withEntry(AtomEntryDto entry) {
        if (entry != null) {
            this.entries.add(entry);
        }
        return this;
    }

    /**
     * Adds multiple generic entries to the feed.
     *
     * @param entries the list of entries to add
     * @return this builder for chaining
     */
    public SubscriptionDto withEntries(List<AtomEntryDto> entries) {
        if (entries != null) {
            this.entries.addAll(entries);
        }
        return this;
    }

    /**
     * Builds the AtomFeedDto for this subscription.
     *
     * @return configured AtomFeedDto with all entries
     */
    public AtomFeedDto toAtomFeed() {
        String id = "urn:uuid:" + subscriptionId;
        String title = schemaType == SchemaType.CUSTOMER ? CUSTOMER_FEED_TITLE : ENERGY_FEED_TITLE;

        return new AtomFeedDto(id, title, published, updated, links, entries);
    }

    // Getters for testing

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public SchemaType getSchemaType() {
        return schemaType;
    }

    public List<LinkDto> getLinks() {
        return links;
    }

    public List<AtomEntryDto> getEntries() {
        return entries;
    }

    public int getEntryCount() {
        return entries.size();
    }
}
