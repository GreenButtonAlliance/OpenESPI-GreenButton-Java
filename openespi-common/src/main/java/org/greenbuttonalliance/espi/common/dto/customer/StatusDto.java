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
 * Shared DTO representing Status information for customer entities.
 * Per ESPI 4.0 customer.xsd lines 1254-1280.
 * <p>
 * Status contains current status information relevant to an entity with 4 fields:
 * - value: Status value at dateTime
 * - dateTime: Date and time for which status applies
 * - remark: Pertinent information as free form text
 * - reason: Reason code or explanation for the status
 * <p>
 * This is a shared type used by Customer, ServiceLocation, and other customer entities.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Status", namespace = "http://naesb.org/espi/customer", propOrder = {
    "value", "dateTime", "remark", "reason"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusDto implements Serializable {

    @XmlElement(name = "value", namespace = "http://naesb.org/espi/customer")
    private String value;

    @XmlElement(name = "dateTime", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime dateTime;

    @XmlElement(name = "remark", namespace = "http://naesb.org/espi/customer")
    private String remark;

    @XmlElement(name = "reason", namespace = "http://naesb.org/espi/customer")
    private String reason;
}
