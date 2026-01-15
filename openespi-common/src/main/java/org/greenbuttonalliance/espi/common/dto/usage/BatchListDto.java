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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for BatchList (output-only for XML marshalling).
 *
 * List of resource URIs that can be used to GET ESPI resources.
 * This supports batch operations by collecting multiple resource URIs
 * that can be processed together.
 *
 * <p>Per ESPI 4.0 XSD (espi.xsd), BatchListType contains:
 * <ul>
 *   <li>resources (xs:anyURI, unbounded) - List of resource URIs</li>
 * </ul>
 */
@XmlRootElement(name = "BatchList", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@NoArgsConstructor
public class BatchListDto {

    /**
     * List of resource URIs for batch processing.
     * Each URI points to an ESPI resource that can be retrieved or processed.
     *
     * Per XSD: minOccurs="0" maxOccurs="unbounded"
     */
    @XmlElement(name = "resources", namespace = "http://naesb.org/espi")
    private List<String> resources = new ArrayList<>();

    /**
     * Constructor with resources list.
     * Creates a defensive copy to prevent external modification.
     *
     * @param resources the list of resource URIs
     */
    public BatchListDto(List<String> resources) {
        this.resources = resources != null ? new ArrayList<>(resources) : new ArrayList<>();
    }

    /**
     * Adds a resource URI to the batch list.
     *
     * @param resourceUri the resource URI to add
     * @return this DTO for fluent chaining
     */
    public BatchListDto addResource(String resourceUri) {
        if (resourceUri != null && !resourceUri.trim().isEmpty()) {
            if (this.resources == null) {
                this.resources = new ArrayList<>();
            }
            this.resources.add(resourceUri.trim());
        }
        return this;
    }

    /**
     * Adds multiple resource URIs to the batch list.
     *
     * @param resourceUris the resource URIs to add
     * @return this DTO for fluent chaining
     */
    public BatchListDto addResources(List<String> resourceUris) {
        if (resourceUris != null && !resourceUris.isEmpty()) {
            if (this.resources == null) {
                this.resources = new ArrayList<>();
            }
            this.resources.addAll(resourceUris);
        }
        return this;
    }

    /**
     * Gets the resource count.
     *
     * @return the number of resources
     */
    public int getResourceCount() {
        return resources != null ? resources.size() : 0;
    }

    /**
     * Checks if the batch list is empty.
     *
     * @return true if no resources, false otherwise
     */
    public boolean isEmpty() {
        return resources == null || resources.isEmpty();
    }
}
