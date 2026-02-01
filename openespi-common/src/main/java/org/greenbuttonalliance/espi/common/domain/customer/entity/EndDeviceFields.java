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

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * Embeddable component for EndDevice-specific fields.
 *
 * Per NAESB ESPI 4.0 customer.xsd, EndDevice extends AssetContainer and adds these 4 fields (lines 218-238).
 * This embeddable component captures those EndDevice-specific fields for reuse in both EndDeviceEntity
 * and MeterEntity.
 */
@Embeddable
@Data
@NoArgsConstructor
public class EndDeviceFields {

    /**
     * If true, there is no physical device. As an example, a virtual meter can be defined
     * to aggregate the consumption for two or more physical meters. Otherwise, this is a
     * physical hardware device.
     */
    @Column(name = "is_virtual")
    private Boolean isVirtual;

    /**
     * If true, this is a premises area network (PAN) device.
     */
    @Column(name = "is_pan")
    private Boolean isPan;

    /**
     * Installation code.
     */
    @Column(name = "install_code", length = 256)
    private String installCode;

    /**
     * Automated meter reading (AMR) or other communication system responsible for
     * communications to this end device.
     */
    @Column(name = "amr_system", length = 256)
    private String amrSystem;
}
