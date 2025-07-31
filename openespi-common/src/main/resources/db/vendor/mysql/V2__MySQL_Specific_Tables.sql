/*
 * OpenESPI MySQL-Specific Schema Migration
 * 
 * Copyright (c) 2018-2025 Green Button Alliance, Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * This migration creates MySQL-specific tables that contain BLOB columns.
 * These tables are separated from the base migration due to vendor-specific
 * column type requirements.
 * 
 * Tables included:
 * - time_configurations (with BLOB columns for dst_end_rule, dst_start_rule)
 * - usage_points (with BLOB column for role_flags)
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
 * Compatible with: MySQL 8.0+
 */

-- Time Configuration Table (MySQL with BLOB columns)
CREATE TABLE time_configurations
(
    id              CHAR(36) PRIMARY KEY ,
    uuid            VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb        BIGINT,
    uuid_lsb        BIGINT,
    description     VARCHAR(255),
    created         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    published       DATETIME(6),
    up_link_rel     VARCHAR(255),
    up_link_href    VARCHAR(1024),
    up_link_type    VARCHAR(255),
    self_link_rel   VARCHAR(255),
    self_link_href  VARCHAR(1024),
    self_link_type  VARCHAR(255),

    -- Time configuration specific fields
    dst_end_rule    BLOB,
    dst_offset      BIGINT,
    dst_start_rule  BLOB,
    tz_offset       BIGINT,

    INDEX           idx_time_config_uuid (uuid),
    INDEX           idx_time_config_created (created),
    INDEX           idx_time_config_updated (updated)
);

-- Related Links Table for Time Configurations
CREATE TABLE time_configuration_related_links
(
    time_configuration_id CHAR(36) NOT NULL,
    related_links         VARCHAR(1024),
    FOREIGN KEY (time_configuration_id) REFERENCES time_configurations (id) ON DELETE CASCADE,
    INDEX                 idx_time_config_related_links (time_configuration_id)
);

-- Usage Point Table (MySQL with BLOB column)
CREATE TABLE usage_points
(
    id                        CHAR(36) PRIMARY KEY ,
    uuid                      VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb                  BIGINT,
    uuid_lsb                  BIGINT,
    description               VARCHAR(255),
    created                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    published                 DATETIME(6),
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
    role_flags                BLOB,

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
    FOREIGN KEY (local_time_parameters_id) REFERENCES time_configurations (id) ON DELETE SET NULL,

    INDEX                     idx_usage_point_uuid (uuid),
    INDEX                     idx_usage_point_kind (kind),
    INDEX                     idx_usage_point_status (status),
    INDEX                     idx_usage_point_customer_id (retail_customer_id),
    INDEX                     idx_usage_point_sdp_id (service_delivery_point_id),
    INDEX                     idx_usage_point_time_config_id (local_time_parameters_id),
    INDEX                     idx_usage_point_created (created),
    INDEX                     idx_usage_point_updated (updated)
);

-- Related Links Table for Usage Points
CREATE TABLE usage_point_related_links
(
    usage_point_id CHAR(36) NOT NULL,
    related_links  VARCHAR(1024),
    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE,
    INDEX          idx_usage_point_related_links (usage_point_id)
);

















