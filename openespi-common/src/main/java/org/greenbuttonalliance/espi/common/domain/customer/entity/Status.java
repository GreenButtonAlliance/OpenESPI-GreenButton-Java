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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Embeddable class for Status information.
 *
 * Current status information relevant to an entity.
 * Per customer.xsd lines 1149-1173.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Status implements Serializable {

    /**
     * Status value.
     */
    @Column(name = "status_value", length = 256)
    private String value;

    /**
     * Date and time status was last changed.
     */
    @Column(name = "status_date_time")
    private OffsetDateTime dateTime;

    /**
     * Pertinent information regarding the current value, as free form text.
     */
    @Column(name = "status_remark", length = 256)
    private String remark;

    /**
     * Reason for status change.
     */
    @Column(name = "status_reason", length = 256)
    private String reason;
}
