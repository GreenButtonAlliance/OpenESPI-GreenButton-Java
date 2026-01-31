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
 * Enumeration for SupplierKind values per ESPI 4.0 customer.xsd.
 *
 * Kind of supplier based on the energy market business rules.
 *
 * IMPORTANT: Sequence must match XSD exactly - ESPI uses ordinal values (0-5) for serialization.
 */
public enum SupplierKind {
    /**
     * Utility supplier (ordinal 0)
     */
    UTILITY,

    /**
     * Retail energy supplier (ordinal 1)
     */
    RETAILER,

    /**
     * Other supplier type (ordinal 2)
     */
    OTHER,

    /**
     * Load Serving Entity (ordinal 3)
     */
    LSE,

    /**
     * Meter Data Management Agent (ordinal 4)
     */
    MDMA,

    /**
     * Metering Service Provider (ordinal 5)
     */
    MSP
}