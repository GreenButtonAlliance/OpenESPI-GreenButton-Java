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

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.customer.enums.ProgramDateKind;

import java.io.Serializable;

/**
 * Embeddable class for single customer energy efficiency program date mapping.
 *
 * Per customer.xsd lines 1223-1251, ProgramDateIdMapping extends Object (NOT IdentifiedObject).
 * This is an embedded component, not a standalone entity.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDateIdMapping implements Serializable {

    /**
     * Type of customer energy efficiency program date.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "program_date_type", length = 64)
    private ProgramDateKind programDateType;

    /**
     * Code value (may be alphanumeric).
     */
    @Column(name = "code", length = 64)
    private String code;

    /**
     * Name associated with code.
     */
    @Column(name = "name", length = 256)
    private String name;

    /**
     * Optional description of code.
     */
    @Column(name = "note", length = 256)
    private String note;
}
