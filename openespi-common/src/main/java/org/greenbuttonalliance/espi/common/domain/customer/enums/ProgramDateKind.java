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

package org.greenbuttonalliance.espi.common.domain.customer.enums;

/**
 * Type of Demand Response program date based on ESPI 4.0 customer.xsd specification.
 *
 * Per customer.xsd lines 1997-2030.
 * Note: XSD uses union type, allowing both enumerated values and custom String64 values.
 *
 * Ordinal mapping:
 * 0 = CUST_DR_PROGRAM_ENROLLMENT_DATE
 * 1 = CUST_DR_PROGRAM_DE_ENROLLMENT_DATE
 * 2 = CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL
 * 3 = CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL
 */
public enum ProgramDateKind {
    /**
     * Date customer enrolled in Demand Response program.
     * Ordinal: 0
     */
    CUST_DR_PROGRAM_ENROLLMENT_DATE,

    /**
     * Date customer terminated enrollment in Demand Response program.
     * Ordinal: 1
     */
    CUST_DR_PROGRAM_DE_ENROLLMENT_DATE,

    /**
     * Earliest date customer can terminate Demand Response enrollment, regardless of financial impact.
     * Ordinal: 2
     */
    CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL,

    /**
     * Earliest date customer can terminate Demand Response enrollment, without financial impact.
     * Ordinal: 3
     */
    CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL
}
