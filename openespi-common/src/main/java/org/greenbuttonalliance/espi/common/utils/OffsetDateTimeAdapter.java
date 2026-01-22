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

package org.greenbuttonalliance.espi.common.utils;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JAXB XML adapter for marshalling and unmarshalling OffsetDateTime instances.
 * <p>
 * OffsetDateTime does not have a default no-arg constructor, which JAXB requires.
 * This adapter converts between OffsetDateTime and ISO-8601 formatted strings
 * for XML marshalling/unmarshalling.
 * <p>
 * Usage: Apply to fields/properties using {@code @XmlJavaTypeAdapter(OffsetDateTimeAdapter.class)}
 */
public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

    /**
     * Unmarshal from XML string to OffsetDateTime.
     * Handles null/empty strings gracefully.
     *
     * @param value ISO-8601 formatted string from XML
     * @return OffsetDateTime instance, or null if input is null/empty
     */
    @Override
    public OffsetDateTime unmarshal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Marshal from OffsetDateTime to XML string.
     * Handles null values gracefully.
     *
     * @param value OffsetDateTime instance
     * @return ISO-8601 formatted string, or null if input is null
     */
    @Override
    public String marshal(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
