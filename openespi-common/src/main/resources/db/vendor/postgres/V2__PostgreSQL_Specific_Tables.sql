/*
 * OpenESPI PostgreSQL-Specific Schema Migration
 * 
 * Copyright (c) 2018-2025 Green Button Alliance, Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * This migration creates PostgreSQL-specific tables that contain BYTEA columns.
 * These tables are separated from the base migration due to vendor-specific
 * column type requirements.
 * 
 * Tables included:
 * - time_configurations (with BYTEA columns for dst_end_rule, dst_start_rule)
 * - usage_points (with BYTEA column for role_flags)
 * - time_configuration_related_links (FK dependency)
 * - usage_point_related_links (FK dependency)
 * - meter_readings (FK dependency on usage_points)
 * - meter_reading_related_links (FK dependency)
 * - interval_blocks (FK dependency on meter_readings)
 * - interval_block_related_links (FK dependency)
 * - interval_readings (FK dependency on interval_blocks)
 * - interval_reading_related_links (FK dependency)
 * - reading_qualities (FK dependency on interval_readings)
 * - reading_quality_related_links (FK dependency)
 * - usage_summaries (FK dependency on usage_points)
 * - usage_summary_related_links (FK dependency)
 * - subscription_usage_points (join table)
 * - customer schema tables (FK dependency on time_configurations)
 *
 * Total tables in this migration: 25+
 * Compatible with: PostgreSQL 12+
 */

-- Time Configuration Table (PostgreSQL with BYTEA columns)
CREATE TABLE time_configurations
(
    id              CHAR(36) PRIMARY KEY ,
    uuid            VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb        BIGINT,
    uuid_lsb        BIGINT,
    description     VARCHAR(255),
    created         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published       TIMESTAMP(6),
    up_link_rel     VARCHAR(255),
    up_link_href    VARCHAR(1024),
    up_link_type    VARCHAR(255),
    self_link_rel   VARCHAR(255),
    self_link_href  VARCHAR(1024),
    self_link_type  VARCHAR(255),

    -- Time configuration specific fields
    dst_end_rule    BYTEA,
    dst_offset      BIGINT,
    dst_start_rule  BYTEA,
    tz_offset       BIGINT
);

CREATE INDEX idx_time_config_uuid ON time_configurations (uuid);
CREATE INDEX idx_time_config_created ON time_configurations (created);
CREATE INDEX idx_time_config_updated ON time_configurations (updated);

-- Related Links Table for Time Configurations
CREATE TABLE time_configuration_related_links
(
    time_configuration_id CHAR(36) NOT NULL,
    related_links         VARCHAR(1024),
    FOREIGN KEY (time_configuration_id) REFERENCES time_configurations (id) ON DELETE CASCADE
);

CREATE INDEX idx_time_config_related_links ON time_configuration_related_links (time_configuration_id);

-- Usage Point Table (PostgreSQL with BYTEA column)
CREATE TABLE usage_points
(
    id                        CHAR(36) PRIMARY KEY ,
    uuid                      VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb                  BIGINT,
    uuid_lsb                  BIGINT,
    description               VARCHAR(255),
    created                   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated                   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published                 TIMESTAMP(6),
    up_link_rel               VARCHAR(255),
    up_link_href              VARCHAR(1024),
    up_link_type              VARCHAR(255),
    self_link_rel             VARCHAR(255),
    self_link_href            VARCHAR(1024),
    self_link_type            VARCHAR(255),

    -- Usage point specific fields
    kind                      VARCHAR(50),
    status                    SMALLINT,
    uri                       VARCHAR(1024),
    service_category          VARCHAR(50),
    service_delivery_remark   VARCHAR(255),
    role_flags                BYTEA,

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
    service_delivery_point_id CHAR(36),
    local_time_parameters_id  CHAR(36),
    subscription_id           CHAR(36),

    FOREIGN KEY (retail_customer_id) REFERENCES retail_customers (id) ON DELETE CASCADE,
    FOREIGN KEY (service_delivery_point_id) REFERENCES service_delivery_points (id) ON DELETE SET NULL,
    FOREIGN KEY (local_time_parameters_id) REFERENCES time_configurations (id) ON DELETE SET NULL
);

CREATE INDEX idx_usage_point_uuid ON usage_points (uuid);
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

CREATE INDEX idx_usage_point_related_links ON usage_point_related_links (usage_point_id);

