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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.*;

/**
 * TimeConfiguration DTO class for JAXB XML marshalling/unmarshalling.
 *
 * PROTOTYPE: Traditional approach using mutable class with JAXB.
 * Alternative to Jackson XML record-based TimeConfigurationDtoJackson.
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
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "TimeConfiguration", namespace = "http://naesb.org/espi", propOrder = {
    "dstEndRule",
    "dstOffset",
    "dstStartRule",
    "tzOffset"
})
public class TimeConfigurationDto {

    private Long id;
    private String uuid;
    private byte[] dstEndRule;
    private Long dstOffset;
    private byte[] dstStartRule;
    private Long tzOffset;

    /**
     * Default constructor for JAXB.
     */
    public TimeConfigurationDto() {
    }

    /**
     * Constructor with timezone offset only.
     *
     * @param tzOffset the timezone offset in seconds from UTC
     */
    public TimeConfigurationDto(Long tzOffset) {
        this.tzOffset = tzOffset;
    }

    /**
     * Constructor with UUID and timezone offset.
     *
     * @param uuid the resource identifier
     * @param tzOffset the timezone offset in seconds from UTC
     */
    public TimeConfigurationDto(String uuid, Long tzOffset) {
        this.uuid = uuid;
        this.tzOffset = tzOffset;
    }

    /**
     * Full constructor with all fields.
     *
     * @param id internal DTO id
     * @param uuid the resource identifier
     * @param dstEndRule DST end rule bytes
     * @param dstOffset DST offset in seconds
     * @param dstStartRule DST start rule bytes
     * @param tzOffset timezone offset in seconds from UTC
     */
    public TimeConfigurationDto(Long id, String uuid, byte[] dstEndRule, Long dstOffset,
                               byte[] dstStartRule, Long tzOffset) {
        this.id = id;
        this.uuid = uuid;
        this.dstEndRule = dstEndRule != null ? dstEndRule.clone() : null;
        this.dstOffset = dstOffset;
        this.dstStartRule = dstStartRule != null ? dstStartRule.clone() : null;
        this.tzOffset = tzOffset;
    }

    // Getters with JAXB annotations

    @XmlTransient
    @Schema(description = "Internal DTO identifier (not serialized to XML)")
    public Long getId() {
        return id;
    }

    @XmlAttribute(name = "mRID")
    @Schema(description = "Resource identifier (mRID)", example = "550e8400-e29b-41d4-a716-446655440000")
    public String getUuid() {
        return uuid;
    }

    @XmlElement(name = "dstEndRule", namespace = "http://naesb.org/espi")
    @Schema(description = "Rule to calculate end of daylight savings time in the current year. Result of dstEndRule must be greater than result of dstStartRule.", example = "...")
    public byte[] getDstEndRule() {
        return dstEndRule != null ? dstEndRule.clone() : null;
    }

    @XmlElement(name = "dstOffset", namespace = "http://naesb.org/espi")
    @Schema(description = "Daylight savings time offset from local standard time in seconds", example = "3600")
    public Long getDstOffset() {
        return dstOffset;
    }

    @XmlElement(name = "dstStartRule", namespace = "http://naesb.org/espi")
    @Schema(description = "Rule to calculate start of daylight savings time in the current year. Result of dstEndRule must be greater than result of dstStartRule.", example = "...")
    public byte[] getDstStartRule() {
        return dstStartRule != null ? dstStartRule.clone() : null;
    }

    @XmlElement(name = "tzOffset", namespace = "http://naesb.org/espi")
    @Schema(description = "Local time zone offset from UTC in seconds. Does not include any daylight savings time offsets. Positive values are east of UTC, negative values are west of UTC.", example = "-28800")
    public Long getTzOffset() {
        return tzOffset;
    }

    // Setters for JAXB unmarshalling

    public void setId(Long id) {
        this.id = id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setDstEndRule(byte[] dstEndRule) {
        this.dstEndRule = dstEndRule != null ? dstEndRule.clone() : null;
    }

    public void setDstOffset(Long dstOffset) {
        this.dstOffset = dstOffset;
    }

    public void setDstStartRule(byte[] dstStartRule) {
        this.dstStartRule = dstStartRule != null ? dstStartRule.clone() : null;
    }

    public void setTzOffset(Long tzOffset) {
        this.tzOffset = tzOffset;
    }

    // Utility methods
    // TEMPORARY: @XmlTransient annotations prevent serialization with XmlAccessType.PROPERTY
    // TODO: Remove when converting to record with XmlAccessType.FIELD (see issue #61)

    /**
     * Gets the timezone offset in hours.
     *
     * @return timezone offset in hours, or null if not set
     */
    @XmlTransient
    public Double getTzOffsetInHours() {
        return tzOffset != null ? tzOffset / 3600.0 : null;
    }

    /**
     * Gets the DST offset in hours.
     *
     * @return DST offset in hours, or null if not set
     */
    @XmlTransient
    public Double getDstOffsetInHours() {
        return dstOffset != null ? dstOffset / 3600.0 : null;
    }

    /**
     * Gets the effective timezone offset including DST.
     *
     * @return total offset in seconds including DST
     */
    @XmlTransient
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
    @XmlTransient
    public Double getEffectiveOffsetInHours() {
        return getEffectiveOffset() / 3600.0;
    }

    /**
     * Checks if this time configuration has DST rules defined.
     *
     * @return true if DST rules are present, false otherwise
     */
    @XmlTransient
    public boolean hasDstRules() {
        return dstStartRule != null && dstStartRule.length > 0 &&
               dstEndRule != null && dstEndRule.length > 0;
    }

    /**
     * Checks if DST is currently active (has non-zero offset).
     *
     * @return true if DST offset is defined and non-zero, false otherwise
     */
    @XmlTransient
    public boolean isDstActive() {
        return dstOffset != null && dstOffset != 0;
    }
}
