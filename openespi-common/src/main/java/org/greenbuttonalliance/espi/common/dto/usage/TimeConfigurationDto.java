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

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TimeConfiguration DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents time configuration parameters including timezone offset and
 * daylight saving time rules for energy metering systems.
 * Complies with NAESB ESPI 4.0 XSD specification.
 *
 * Field order strictly matches espi.xsd TimeConfiguration element sequence.
 *
 * @see <a href="https://www.naesb.org/ESPI_Standards.asp">NAESB ESPI 4.0</a>
 */
@XmlRootElement(name = "TimeConfiguration", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TimeConfiguration", namespace = "http://naesb.org/espi", propOrder = {
    "dstEndRule",
    "dstOffset",
    "dstStartRule",
    "tzOffset"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeConfigurationDto {

    /**
     * Internal DTO identifier (not serialized to XML).
     */
    @XmlTransient
    private Long id;

    /**
     * Resource identifier (mRID).
     */
    @XmlTransient
    private String uuid;

    /**
     * Rule to calculate end of daylight savings time in the current year.
     * Result of dstEndRule must be greater than result of dstStartRule.
     */
    @XmlElement(name = "dstEndRule", type = String.class)
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    @Getter(AccessLevel.NONE)
    private byte[] dstEndRule;

    /**
     * Daylight savings time offset from local standard time in seconds.
     */
    @XmlElement(name = "dstOffset")
    private Long dstOffset;

    /**
     * Rule to calculate start of daylight savings time in the current year.
     * Result of dstEndRule must be greater than result of dstStartRule.
     */
    @XmlElement(name = "dstStartRule", type = String.class)
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    @Getter(AccessLevel.NONE)
    private byte[] dstStartRule;

    /**
     * Local time zone offset from UTC in seconds.
     * Does not include any daylight savings time offsets.
     * Positive values are east of UTC, negative values are west of UTC.
     */
    @XmlElement(name = "tzOffset")
    private Long tzOffset;

    /**
     * Constructor with timezone offset only.
     *
     * @param tzOffset the timezone offset in seconds from UTC
     */
    public TimeConfigurationDto(Long tzOffset) {
        this(null, null, null, null, null, tzOffset);
    }

    /**
     * Constructor with UUID and timezone offset.
     *
     * @param uuid the resource identifier
     * @param tzOffset the timezone offset in seconds from UTC
     */
    public TimeConfigurationDto(String uuid, Long tzOffset) {
        this(null, uuid, null, null, null, tzOffset);
    }

    // Custom getters for defensive copying of byte arrays

    /**
     * Gets the dstEndRule with defensive copying.
     *
     * @return cloned byte array or null
     */
    public byte[] getDstEndRule() {
        return dstEndRule != null ? dstEndRule.clone() : null;
    }

    /**
     * Gets the dstStartRule with defensive copying.
     *
     * @return cloned byte array or null
     */
    public byte[] getDstStartRule() {
        return dstStartRule != null ? dstStartRule.clone() : null;
    }

    // Computed property getters

    /**
     * Gets the timezone offset in hours.
     *
     * @return timezone offset in hours, or null if not set
     */
    public Double getTzOffsetInHours() {
        return tzOffset != null ? tzOffset / 3600.0 : null;
    }

    /**
     * Gets the DST offset in hours.
     *
     * @return DST offset in hours, or null if not set
     */
    public Double getDstOffsetInHours() {
        return dstOffset != null ? dstOffset / 3600.0 : null;
    }

    /**
     * Gets the effective timezone offset including DST.
     *
     * @return total offset in seconds including DST
     */
    public Long getEffectiveOffset() {
        Long base = tzOffset != null ? tzOffset : 0L;
        Long dst = dstOffset != null ? dstOffset : 0L;
        return base + dst;
    }

    /**
     * Gets the effective timezone offset in hours including DST.
     *
     * @return total offset in hours including DST
     */
    public Double getEffectiveOffsetInHours() {
        return getEffectiveOffset() / 3600.0;
    }

    /**
     * Checks if this time configuration has DST rules defined.
     *
     * @return true if DST rules are present, false otherwise
     */
    public boolean hasDstRules() {
        return dstStartRule != null && dstStartRule.length > 0 &&
               dstEndRule != null && dstEndRule.length > 0;
    }

    /**
     * Checks if DST is currently active (has non-zero offset).
     *
     * @return true if DST offset is defined and non-zero, false otherwise
     */
    public boolean isDstActive() {
        return dstOffset != null && dstOffset != 0;
    }
}
