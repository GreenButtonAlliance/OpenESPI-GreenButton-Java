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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.*;

/**
 * Abstract base class for AssetContainer without JAXB concerns.
 * 
 * Asset that is aggregation of other assets such as conductors, transformers, 
 * switchgear, land, fences, buildings, equipment, vehicles, etc.
 */
@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public abstract class AssetContainer extends Asset {
    // AssetContainer is simply an Asset that can contain other assets
    // No additional fields beyond those inherited from Asset
}