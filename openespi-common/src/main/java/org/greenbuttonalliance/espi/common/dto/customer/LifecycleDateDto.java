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

package org.greenbuttonalliance.espi.common.dto.customer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Shared DTO for LifecycleDate information.
 * Per ESPI 4.0 customer.xsd lines 991-1006.
 *
 * Lifecycle dates for asset with 2 fields:
 * - manufacturedDate: Date asset was manufactured
 * - installationDate: Date asset was installed
 *
 * Used by Asset-containing entities (EndDevice, Meter).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LifecycleDate", namespace = "http://naesb.org/espi/customer", propOrder = {
    "manufacturedDate", "installationDate"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleDateDto implements Serializable {

    @XmlElement(name = "manufacturedDate", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime manufacturedDate;

    @XmlElement(name = "installationDate", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime installationDate;
}
