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

package org.greenbuttonalliance.espi.common.dto.atom;

import com.fasterxml.jackson.annotation.*;
import jakarta.xml.bind.annotation.*;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Atom Content DTO record for content within Atom entries.
 * 
 * Represents the content section of an Atom entry containing the actual
 * Green Button resource data (Customer, UsagePoint, MeterReading, etc.).
 *
 * @deprecated AtomContentDto no longer used. Payload moved to AtomEntryDto
 */
@Deprecated
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AtomContent", namespace = "http://www.w3.org/2005/Atom")
public record AtomContentDto(
    
    @XmlAttribute(name = "type")
    String type,

    @JsonProperty("resource")
    @JacksonXmlProperty(namespace = "http://naesb.org/espi")

    @XmlElementWrapper(name = "resource", namespace = "http://naesb.org/espi")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UsagePointDto.class, name = "UsagePoint"),
            @JsonSubTypes.Type(value = MeterReadingDto.class, name = "MeterReading"),
            @JsonSubTypes.Type(value = ReadingTypeDto.class, name = "ReadingType")
    })
    @XmlAnyElement(lax = true)
    Object resource
) {
    
    /**
     * Default constructor for JAXB.
     */
    public AtomContentDto() {
        this(null, null);
    }
    
    /**
     * Constructor for XML content.
     */
    public AtomContentDto(Object resource) {
        this("application/xml", resource);
    }
    
    /**
     * Checks if the content has a resource.
     * 
     * @return true if resource is not null
     */
    public boolean hasResource() {
        return resource != null;
    }
    
    /**
     * Gets the resource cast to a specific type.
     * 
     * @param <T> the type to cast to
     * @param clazz the class to cast to
     * @return the resource cast to the specified type, or null
     */
    @SuppressWarnings("unchecked")
    public <T> T getResourceAs(Class<T> clazz) {
        if (resource != null && clazz.isInstance(resource)) {
            return (T) resource;
        }
        return null;
    }
}