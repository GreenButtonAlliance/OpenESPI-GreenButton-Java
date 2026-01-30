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

package org.greenbuttonalliance.espi.common.domain.customer.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Embeddable MeterMultiplier type.
 * Per customer.xsd MeterMultiplier type.
 * Extends Object (NOT IdentifiedObject) per ESPI 4.0 specification.
 * Multiplier applied at the meter.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterMultiplier implements Serializable {

    /**
     * Kind of multiplier.
     * Per customer.xsd MeterMultiplierKind enumeration.
     */
    @Column(name = "kind", length = 256)
    private String kind;

    /**
     * Multiplier value.
     */
    @Column(name = "value")
    private BigDecimal value;
}