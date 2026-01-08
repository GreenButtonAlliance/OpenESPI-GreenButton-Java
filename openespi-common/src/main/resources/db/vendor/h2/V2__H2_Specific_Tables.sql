/*
 * OpenESPI H2-Specific Schema Migration
 * 
 * Copyright (c) 2018-2025 Green Button Alliance, Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * This migration creates H2-specific tables that contain BLOB/BINARY columns.
 * These tables are separated from the base migration due to vendor-specific
 * column type requirements.
 * 
 * Tables included:
 * - time_configurations (with BINARY columns for dst_end_rule, dst_start_rule)
 * - usage_points (with BINARY column for role_flags)
 * - time_configuration_related_links (FK dependency)
 * - usage_point_related_links (FK dependency)
 * - meter_readings (FK dependency on usage_points)
 * - meter_reading_related_links (FK dependency)
 * - interval_blocks (FK dependency on meter_readings)
 * - interval_block_related_links (FK dependency)
 * - interval_readings (FK dependency on interval_blocks - no related_links, extends Object)
 * - reading_qualities (FK dependency on interval_readings - no related_links, extends Object)
 * - usage_summaries (FK dependency on usage_points)
 * - usage_summary_related_links (FK dependency)
 * - subscription_usage_points (join table)
 * - customer schema tables (FK dependency on time_configurations)
 *
 * Total tables in this migration: 25+
 * Compatible with: H2 Database
 */

-- Service Delivery Point Table (Object-based entity, no IdentifiedObject)
-- Must be created before usage_points which references it
-- ServiceDeliveryPoint extends Object per ESPI 4.0 XSD (espi.xsd:1161)
CREATE TABLE service_delivery_points
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    sdp_name           VARCHAR(256),
    sdp_tariff_profile VARCHAR(256),
    sdp_customer_agreement VARCHAR(256)
);

CREATE INDEX idx_sdp_name ON service_delivery_points (sdp_name);
CREATE INDEX idx_sdp_tariff_profile ON service_delivery_points (sdp_tariff_profile);
CREATE INDEX idx_sdp_customer_agreement ON service_delivery_points (sdp_customer_agreement);

-- Time Configuration Table (H2 with BINARY columns)
CREATE TABLE time_configurations
(
    id              CHAR(36) PRIMARY KEY ,
    description     VARCHAR(255),
    created         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published       TIMESTAMP(6),
    up_link_rel     VARCHAR(255),
    up_link_href    VARCHAR(1024),
    up_link_type    VARCHAR(255),
    self_link_rel   VARCHAR(255),
    self_link_href  VARCHAR(1024),
    self_link_type  VARCHAR(255),

    -- Time configuration specific fields
    dst_end_rule    BINARY VARYING(255),
    dst_offset      BIGINT,
    dst_start_rule  BINARY VARYING(255),
    tz_offset       BIGINT
);

-- Create indexes for time_configurations table
CREATE INDEX idx_time_config_created ON time_configurations (created);
CREATE INDEX idx_time_config_updated ON time_configurations (updated);

-- Related Links Table for Time Configurations
CREATE TABLE time_configuration_related_links
(
    time_configuration_id CHAR(36) NOT NULL,
    related_links         VARCHAR(1024),
    FOREIGN KEY (time_configuration_id) REFERENCES time_configurations (id) ON DELETE CASCADE
);

-- Create index for time_configuration_related_links table
CREATE INDEX idx_time_config_related_links ON time_configuration_related_links (time_configuration_id);

-- Usage Point Table (H2 with BINARY column)
CREATE TABLE usage_points
(
    id                        CHAR(36) PRIMARY KEY ,
    description               VARCHAR(255),
    created                   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated                   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published                 TIMESTAMP(6),
    up_link_rel               VARCHAR(255),
    up_link_href              VARCHAR(1024),
    up_link_type              VARCHAR(255),
    self_link_rel             VARCHAR(255),
    self_link_href            VARCHAR(1024),
    self_link_type            VARCHAR(255),

    -- Usage point specific fields
    kind                      VARCHAR(50),
    status                    smallint,
    uri                       VARCHAR(1024),
    service_category          VARCHAR(50),
    service_delivery_remark   VARCHAR(255),
    role_flags                VARBINARY(255),

    -- Embedded SummaryMeasurement: estimatedLoad
    estimated_load_multiplier                                          VARCHAR(255),
    estimated_load_timestamp                                           BIGINT,
    estimated_load_uom                                                 VARCHAR(50),
    estimated_load_value                                               BIGINT,
    estimated_load_reading_type_ref                                    VARCHAR(512),

    -- Embedded SummaryMeasurement: nominalServiceVoltage
    nominal_voltage_multiplier                                         VARCHAR(255),
    nominal_voltage_timestamp                                          BIGINT,
    nominal_voltage_uom                                                VARCHAR(50),
    nominal_voltage_value                                              BIGINT,
    nominal_voltage_reading_type_ref                                   VARCHAR(512),

    -- Embedded SummaryMeasurement: ratedCurrent
    rated_current_multiplier                                           VARCHAR(255),
    rated_current_timestamp                                            BIGINT,
    rated_current_uom                                                  VARCHAR(50),
    rated_current_value                                                BIGINT,
    rated_current_reading_type_ref                                     VARCHAR(512),

    -- Embedded SummaryMeasurement: ratedPower
    rated_power_multiplier                                             VARCHAR(255),
    rated_power_timestamp                                              BIGINT,
    rated_power_uom                                                    VARCHAR(50),
    rated_power_value                                                  BIGINT,
    rated_power_reading_type_ref                                       VARCHAR(512),

    -- Foreign key relationships
    retail_customer_id        CHAR(36),
    service_delivery_point_id BIGINT,
    local_time_parameters_id  CHAR(36),
    subscription_id           CHAR(36),

    FOREIGN KEY (retail_customer_id) REFERENCES retail_customers (id) ON DELETE CASCADE,
    FOREIGN KEY (service_delivery_point_id) REFERENCES service_delivery_points (id) ON DELETE SET NULL,
    FOREIGN KEY (local_time_parameters_id) REFERENCES time_configurations (id) ON DELETE SET NULL
);

-- Create indexes for usage_points table
CREATE INDEX idx_usage_point_kind ON usage_points (kind);
CREATE INDEX idx_usage_point_status ON usage_points (status);
CREATE INDEX idx_usage_point_customer_id ON usage_points (retail_customer_id);
CREATE INDEX idx_usage_point_sdp_id ON usage_points (service_delivery_point_id);
CREATE INDEX idx_usage_point_time_config_id ON usage_points (local_time_parameters_id);
CREATE INDEX idx_usage_point_created ON usage_points (created);
CREATE INDEX idx_usage_point_updated ON usage_points (updated);

-- Related Links Table for Usage Points
CREATE TABLE usage_point_related_links
(
    usage_point_id CHAR(36) NOT NULL,
    related_links  VARCHAR(1024),
    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

-- Create index for usage_point_related_links table
CREATE INDEX idx_usage_point_related_links ON usage_point_related_links (usage_point_id);

-- PnodeRef Table (Object-based entity, no IdentifiedObject)
-- Must be created after usage_points which it references
-- PnodeRef extends Object per ESPI 4.0 XSD (espi.xsd:1539)
CREATE TABLE pnode_refs
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    apnode_type          VARCHAR(64),
    ref                  VARCHAR(256) NOT NULL,
    start_effective_date BIGINT,
    end_effective_date   BIGINT,
    usage_point_id       CHAR(36) NOT NULL,
    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

CREATE INDEX idx_pnode_ref_apnode_type ON pnode_refs (apnode_type);
CREATE INDEX idx_pnode_ref_ref ON pnode_refs (ref);
CREATE INDEX idx_pnode_ref_usage_point_id ON pnode_refs (usage_point_id);

-- Meter Reading Table
CREATE TABLE meter_readings
(
    id              CHAR(36) PRIMARY KEY ,
    description     VARCHAR(255),
    created         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published       TIMESTAMP(6),
    up_link_rel     VARCHAR(255),
    up_link_href    VARCHAR(1024),
    up_link_type    VARCHAR(255),
    self_link_rel   VARCHAR(255),
    self_link_href  VARCHAR(1024),
    self_link_type  VARCHAR(255),

    -- Foreign key relationships
    usage_point_id  CHAR(36),
    reading_type_id CHAR(36),

    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE,
    FOREIGN KEY (reading_type_id) REFERENCES reading_types (id) ON DELETE SET NULL
);

-- Create indexes for meter_readings table
CREATE INDEX idx_meter_reading_usage_point_id ON meter_readings (usage_point_id);
CREATE INDEX idx_meter_reading_reading_type_id ON meter_readings (reading_type_id);
CREATE INDEX idx_meter_reading_created ON meter_readings (created);
CREATE INDEX idx_meter_reading_updated ON meter_readings (updated);

-- Related Links Table for Meter Readings
CREATE TABLE meter_reading_related_links
(
    meter_reading_id CHAR(36) NOT NULL,
    related_links    VARCHAR(1024),
    FOREIGN KEY (meter_reading_id) REFERENCES meter_readings (id) ON DELETE CASCADE
);

-- Create index for meter_reading_related_links table
CREATE INDEX idx_meter_reading_related_links ON meter_reading_related_links (meter_reading_id);

-- Interval Block Table
CREATE TABLE interval_blocks
(
    id                CHAR(36) PRIMARY KEY ,
    description       VARCHAR(255),
    created           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published         TIMESTAMP(6),
    up_link_rel       VARCHAR(255),
    up_link_href      VARCHAR(1024),
    up_link_type      VARCHAR(255),
    self_link_rel     VARCHAR(255),
    self_link_href    VARCHAR(1024),
    self_link_type    VARCHAR(255),

    -- Interval block specific fields
    interval_duration BIGINT,
    interval_start    BIGINT,

    -- Foreign key relationships
    meter_reading_id  CHAR(36),

    FOREIGN KEY (meter_reading_id) REFERENCES meter_readings (id) ON DELETE CASCADE
);

-- Create indexes for interval_blocks table
CREATE INDEX idx_interval_block_meter_reading_id ON interval_blocks (meter_reading_id);
CREATE INDEX idx_interval_block_start ON interval_blocks (interval_start);
CREATE INDEX idx_interval_block_created ON interval_blocks (created);
CREATE INDEX idx_interval_block_updated ON interval_blocks (updated);

-- Related Links Table for Interval Blocks
CREATE TABLE interval_block_related_links
(
    interval_block_id CHAR(36) NOT NULL,
    related_links     VARCHAR(1024),
    FOREIGN KEY (interval_block_id) REFERENCES interval_blocks (id) ON DELETE CASCADE
);

-- Create index for interval_block_related_links table
CREATE INDEX idx_interval_block_related_links ON interval_block_related_links (interval_block_id);

-- Interval Reading Table (Object-based entity, no IdentifiedObject)
-- IntervalReading extends Object per ESPI 4.0 XSD (espi.xsd:1016)
-- XSD sequence: cost → ReadingQuality → timePeriod → value → consumptionTier → tou → cpp
CREATE TABLE interval_readings
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- ESPI 4.0 fields in XSD sequence order
    cost                 BIGINT,

    -- timePeriod (embedded DateTimeInterval)
    time_period_start    BIGINT,
    time_period_duration BIGINT,

    reading_value        BIGINT,
    consumption_tier     BIGINT,
    tou                  BIGINT,
    cpp                  BIGINT,

    -- Foreign key relationship (parent: IntervalBlock)
    interval_block_id    CHAR(36),

    FOREIGN KEY (interval_block_id) REFERENCES interval_blocks (id) ON DELETE CASCADE
);

-- Create indexes for interval_readings table
CREATE INDEX idx_interval_reading_block_id ON interval_readings (interval_block_id);
CREATE INDEX idx_interval_reading_time_start ON interval_readings (time_period_start);
CREATE INDEX idx_interval_reading_value ON interval_readings (reading_value);

-- Reading Quality Table (Object-based entity, no IdentifiedObject)
-- ReadingQuality extends Object per ESPI 4.0 XSD (espi.xsd:1062)
-- XSD sequence: quality
CREATE TABLE reading_qualities
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- ESPI 4.0 field
    quality              VARCHAR(50) NOT NULL,

    -- Foreign key relationship (parent: IntervalReading)
    interval_reading_id  BIGINT,

    FOREIGN KEY (interval_reading_id) REFERENCES interval_readings (id) ON DELETE CASCADE
);

-- Create indexes for reading_qualities table
CREATE INDEX idx_reading_quality_interval_reading_id ON reading_qualities (interval_reading_id);
CREATE INDEX idx_reading_quality_quality ON reading_qualities (quality);
