/*
 *
 *      Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

/*
 * Migration V4: Create Time Configurations Table
 *
 * Creates the time_configurations table to support TimeConfiguration entity.
 * TimeConfiguration represents timezone and daylight saving time parameters
 * used by both usage and customer schemas in NAESB ESPI 4.0.
 *
 * Column order matches ESPI 4.0 XSD propOrder for TimeConfiguration:
 * - dstEndRule
 * - dstOffset
 * - dstStartRule
 * - tzOffset
 *
 * Compatible with: H2, MySQL, PostgreSQL
 */

-- Time Configurations Table
CREATE TABLE time_configurations
(
    id          CHAR(36) PRIMARY KEY,
    description VARCHAR(255),
    created     TIMESTAMP NOT NULL,
    updated     TIMESTAMP NOT NULL,
    published   TIMESTAMP,

    -- Link fields from IdentifiedObject
    up_link_rel    VARCHAR(255),
    up_link_href   VARCHAR(1024),
    up_link_type   VARCHAR(255),
    self_link_rel  VARCHAR(255),
    self_link_href VARCHAR(1024),
    self_link_type VARCHAR(255),

    -- TimeConfiguration specific fields (XSD propOrder)
    dst_end_rule   BLOB,
    dst_offset     BIGINT,
    dst_start_rule BLOB,
    tz_offset      BIGINT
);

-- Indexes for time_configurations table
CREATE INDEX idx_time_config_created ON time_configurations (created);
CREATE INDEX idx_time_config_updated ON time_configurations (updated);
CREATE INDEX idx_time_config_tz_offset ON time_configurations (tz_offset);
