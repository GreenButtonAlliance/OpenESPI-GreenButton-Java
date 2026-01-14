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
 * - service_delivery_points (extends Object - vendor-specific BIGSERIAL)
 * - time_configurations (with BYTEA columns for dst_end_rule, dst_start_rule)
 * - usage_points (with BYTEA column for role_flags)
 * - time_configuration_related_links (FK dependency)
 * - usage_point_related_links (FK dependency)
 * - pnode_refs (extends Object - vendor-specific BIGSERIAL, FK to usage_points)
 * - aggregated_node_refs (extends Object - vendor-specific BIGSERIAL, FK to usage_points)
 * - aggregated_node_ref_pnode_refs (join table)
 * - meter_readings (FK dependency on usage_points)
 * - meter_reading_related_links (FK dependency)
 * - interval_blocks (FK dependency on meter_readings)
 * - interval_block_related_links (FK dependency)
 * - interval_readings (extends Object - vendor-specific BIGSERIAL, FK to interval_blocks)
 * - reading_qualities (extends Object - vendor-specific BIGSERIAL, FK to interval_readings)
 * - usage_summaries (FK dependency on usage_points, must precede line_items)
 * - usage_summary_related_links (FK dependency)
 * - line_items (extends Object - vendor-specific BIGSERIAL, FK to usage_summaries)
 *
 * Total tables in this migration: 25+
 * Compatible with: PostgreSQL 12+
 */

-- Service Delivery Point Table (Object-based entity, no IdentifiedObject)
-- Must be created before usage_points which references it
-- ServiceDeliveryPoint extends Object per ESPI 4.0 XSD (espi.xsd:1161)
CREATE TABLE service_delivery_points
(
    id                 BIGSERIAL PRIMARY KEY,
    sdp_name           VARCHAR(256),
    sdp_tariff_profile VARCHAR(256),
    sdp_customer_agreement VARCHAR(256)
);

CREATE INDEX idx_sdp_name ON service_delivery_points (sdp_name);
CREATE INDEX idx_sdp_tariff_profile ON service_delivery_points (sdp_tariff_profile);
CREATE INDEX idx_sdp_customer_agreement ON service_delivery_points (sdp_customer_agreement);

-- Time Configuration Table (PostgreSQL with BYTEA columns)
CREATE TABLE time_configurations
(
    id              CHAR(36) PRIMARY KEY ,
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
    service_delivery_point_id BIGINT,
    local_time_parameters_id  CHAR(36),
    subscription_id           CHAR(36),

    FOREIGN KEY (retail_customer_id) REFERENCES retail_customers (id) ON DELETE CASCADE,
    FOREIGN KEY (service_delivery_point_id) REFERENCES service_delivery_points (id) ON DELETE SET NULL,
    FOREIGN KEY (local_time_parameters_id) REFERENCES time_configurations (id) ON DELETE SET NULL
);

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


-- PnodeRef Table (Object-based entity, no IdentifiedObject)
-- Must be created after usage_points which it references
-- PnodeRef extends Object per ESPI 4.0 XSD (espi.xsd:1539)
CREATE TABLE pnode_refs
(
    id             BIGSERIAL PRIMARY KEY,
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

-- AggregatedNodeRef Table (Object-based entity, no IdentifiedObject)
-- Must be created after usage_points which it references
-- AggregatedNodeRef extends Object per ESPI 4.0 XSD (espi.xsd:1570)
CREATE TABLE aggregated_node_refs
(
    id                   BIGSERIAL PRIMARY KEY,
    anode_type           VARCHAR(64),
    ref                  VARCHAR(256) NOT NULL,
    start_effective_date BIGINT,
    end_effective_date   BIGINT,
    usage_point_id       CHAR(36) NOT NULL,
    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

CREATE INDEX idx_aggregated_node_ref_anode_type ON aggregated_node_refs (anode_type);
CREATE INDEX idx_aggregated_node_ref_ref ON aggregated_node_refs (ref);
CREATE INDEX idx_aggregated_node_ref_usage_point_id ON aggregated_node_refs (usage_point_id);

-- AggregatedNodeRef to PnodeRef Join Table (Many-to-Many)
-- Per ESPI 4.0 XSD (espi.xsd:1597), pnodeRef has minOccurs="0" maxOccurs="unbounded"
CREATE TABLE aggregated_node_ref_pnode_refs
(
    aggregated_node_ref_id BIGINT NOT NULL,
    pnode_ref_id           BIGINT NOT NULL,
    PRIMARY KEY (aggregated_node_ref_id, pnode_ref_id),
    FOREIGN KEY (aggregated_node_ref_id) REFERENCES aggregated_node_refs (id) ON DELETE CASCADE,
    FOREIGN KEY (pnode_ref_id) REFERENCES pnode_refs (id) ON DELETE CASCADE
);

CREATE INDEX idx_agg_node_ref_pnode_refs_agg ON aggregated_node_ref_pnode_refs (aggregated_node_ref_id);
CREATE INDEX idx_agg_node_ref_pnode_refs_pnode ON aggregated_node_ref_pnode_refs (pnode_ref_id);

-- Meter Reading Table
CREATE TABLE meter_readings
(
    id              CHAR(36) PRIMARY KEY ,
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

    -- Foreign key relationships
    usage_point_id  CHAR(36),
    reading_type_id CHAR(36),

    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE,
    FOREIGN KEY (reading_type_id) REFERENCES reading_types (id) ON DELETE SET NULL
);

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

CREATE INDEX idx_meter_reading_related_links ON meter_reading_related_links (meter_reading_id);

-- Interval Block Table
CREATE TABLE interval_blocks
(
    id                CHAR(36) PRIMARY KEY ,
    description       VARCHAR(255),
    created           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

CREATE INDEX idx_interval_block_related_links ON interval_block_related_links (interval_block_id);

-- Interval Reading Table (Object-based entity, no IdentifiedObject)
-- IntervalReading extends Object per ESPI 4.0 XSD (espi.xsd:1016)
-- XSD sequence: cost → ReadingQuality → timePeriod → value → consumptionTier → tou → cpp
CREATE TABLE interval_readings
(
    id                   BIGSERIAL PRIMARY KEY,

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

CREATE INDEX idx_interval_reading_block_id ON interval_readings (interval_block_id);
CREATE INDEX idx_interval_reading_time_start ON interval_readings (time_period_start);
CREATE INDEX idx_interval_reading_value ON interval_readings (reading_value);

-- Reading Quality Table (Object-based entity, no IdentifiedObject)
-- ReadingQuality extends Object per ESPI 4.0 XSD (espi.xsd:1062)
-- XSD sequence: quality
CREATE TABLE reading_qualities
(
    id                   BIGSERIAL PRIMARY KEY,

    -- ESPI 4.0 field
    quality              VARCHAR(50) NOT NULL,

    -- Foreign key relationship (parent: IntervalReading)
    interval_reading_id  BIGINT,

    FOREIGN KEY (interval_reading_id) REFERENCES interval_readings (id) ON DELETE CASCADE
);

CREATE INDEX idx_reading_quality_interval_reading_id ON reading_qualities (interval_reading_id);
CREATE INDEX idx_reading_quality_quality ON reading_qualities (quality);

-- Usage Summary Table (IdentifiedObject-based entity with UUID)
-- Must be created before line_items which references it
CREATE TABLE usage_summaries
(
    id                          CHAR(36) PRIMARY KEY ,
    description                 VARCHAR(255),
    created                     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated                     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published                   TIMESTAMP(6),
    up_link_rel                 VARCHAR(255),
    up_link_href                VARCHAR(1024),
    up_link_type                VARCHAR(255),
    self_link_rel               VARCHAR(255),
    self_link_href              VARCHAR(1024),
    self_link_type              VARCHAR(255),

    -- Usage summary specific fields
    bill_last_period            BIGINT,
    bill_to_date                BIGINT,
    cost_additional_last_period BIGINT,
    currency                    VARCHAR(3),
    quality_of_reading          VARCHAR(50),
    status_timestamp            BIGINT,

    -- Embedded DateTimeInterval: billingPeriod
    billing_period_start        BIGINT,
    billing_period_duration     BIGINT,

    -- Embedded DateTimeInterval: ratchetDemandPeriod
    ratchet_demand_period_start BIGINT,
    ratchet_demand_period_duration BIGINT,

    -- Embedded SummaryMeasurement: overallConsumptionLastPeriod
    overall_consumption_last_period_multiplier VARCHAR(255),
    overall_consumption_last_period_timestamp BIGINT,
    overall_consumption_last_period_uom VARCHAR(50),
    overall_consumption_last_period_value BIGINT,
    overall_consumption_last_period_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: currentBillingPeriodOverAllConsumption
    current_billing_period_overall_consumption_multiplier VARCHAR(255),
    current_billing_period_overall_consumption_timestamp BIGINT,
    current_billing_period_overall_consumption_uom VARCHAR(50),
    current_billing_period_overall_consumption_value BIGINT,
    current_billing_period_overall_consumption_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: currentDayLastYearNetConsumption
    current_day_last_year_net_consumption_multiplier VARCHAR(255),
    current_day_last_year_net_consumption_timestamp BIGINT,
    current_day_last_year_net_consumption_uom VARCHAR(50),
    current_day_last_year_net_consumption_value BIGINT,
    current_day_last_year_net_consumption_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: currentDayNetConsumption
    current_day_net_consumption_multiplier VARCHAR(255),
    current_day_net_consumption_timestamp BIGINT,
    current_day_net_consumption_uom VARCHAR(50),
    current_day_net_consumption_value BIGINT,
    current_day_net_consumption_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: currentDayOverallConsumption
    current_day_overall_consumption_multiplier VARCHAR(255),
    current_day_overall_consumption_timestamp BIGINT,
    current_day_overall_consumption_uom VARCHAR(50),
    current_day_overall_consumption_value BIGINT,
    current_day_overall_consumption_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: peakDemand
    peak_demand_multiplier VARCHAR(255),
    peak_demand_timestamp BIGINT,
    peak_demand_uom VARCHAR(50),
    peak_demand_value BIGINT,
    peak_demand_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: previousDayLastYearOverallConsumption
    previous_day_last_year_overall_consumption_multiplier VARCHAR(255),
    previous_day_last_year_overall_consumption_timestamp BIGINT,
    previous_day_last_year_overall_consumption_uom VARCHAR(50),
    previous_day_last_year_overall_consumption_value BIGINT,
    previous_day_last_year_overall_consumption_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: previousDayNetConsumption
    previous_day_net_consumption_multiplier VARCHAR(255),
    previous_day_net_consumption_timestamp BIGINT,
    previous_day_net_consumption_uom VARCHAR(50),
    previous_day_net_consumption_value BIGINT,
    previous_day_net_consumption_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: previousDayOverallConsumption
    previous_day_overall_consumption_multiplier VARCHAR(255),
    previous_day_overall_consumption_timestamp BIGINT,
    previous_day_overall_consumption_uom VARCHAR(50),
    previous_day_overall_consumption_value BIGINT,
    previous_day_overall_consumption_reading_type_ref VARCHAR(512),

    -- Embedded SummaryMeasurement: ratchetDemand
    ratchet_demand_multiplier VARCHAR(255),
    ratchet_demand_timestamp BIGINT,
    ratchet_demand_uom VARCHAR(50),
    ratchet_demand_value BIGINT,
    ratchet_demand_reading_type_ref VARCHAR(512),

    -- ESPI 4.0 extension fields (Phase 11)
    commodity                   INTEGER,
    tariff_profile              VARCHAR(256),
    read_cycle                  VARCHAR(256),

    -- Embedded BillingChargeSource: billingChargeSource
    billing_charge_source_agency_name VARCHAR(256),

    -- Foreign key relationships
    usage_point_id              CHAR(36),

    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

CREATE INDEX idx_usage_summary_usage_point_id ON usage_summaries (usage_point_id);
CREATE INDEX idx_usage_summary_billing_period_start ON usage_summaries (billing_period_start);
CREATE INDEX idx_usage_summary_created ON usage_summaries (created);
CREATE INDEX idx_usage_summary_updated ON usage_summaries (updated);
CREATE INDEX idx_usage_summary_commodity ON usage_summaries (commodity);
CREATE INDEX idx_usage_summary_tariff_profile ON usage_summaries (tariff_profile);
CREATE INDEX idx_usage_summary_read_cycle ON usage_summaries (read_cycle);

-- Related Links Table for Usage Summaries
CREATE TABLE usage_summary_related_links
(
    usage_summary_id CHAR(36) NOT NULL,
    related_links    VARCHAR(1024),
    FOREIGN KEY (usage_summary_id) REFERENCES usage_summaries (id) ON DELETE CASCADE
);

CREATE INDEX idx_usage_summary_related_links ON usage_summary_related_links (usage_summary_id);

-- Tariff Rider Ref Table (Object-based entity, no IdentifiedObject)
-- TariffRiderRef extends Object per ESPI 4.0 XSD (espi.xsd:1602-1627)
-- XSD sequence: riderType → enrollmentStatus → effectiveDate
-- Part of TariffRiderRefs collection in UsageSummary (Phase 11)
CREATE TABLE tariff_rider_refs
(
    id                 BIGSERIAL PRIMARY KEY,

    -- ESPI 4.0 fields in XSD sequence order
    rider_type         VARCHAR(256) NOT NULL,
    enrollment_status  VARCHAR(32) NOT NULL,
    effective_date     BIGINT NOT NULL,

    -- Foreign key relationship (parent: UsageSummary)
    usage_summary_id   CHAR(36),

    FOREIGN KEY (usage_summary_id) REFERENCES usage_summaries (id) ON DELETE CASCADE
);

CREATE INDEX idx_tariff_rider_ref_usage_summary ON tariff_rider_refs (usage_summary_id);
CREATE INDEX idx_tariff_rider_ref_rider_type ON tariff_rider_refs (rider_type);
CREATE INDEX idx_tariff_rider_ref_enrollment_status ON tariff_rider_refs (enrollment_status);

-- Line Item Table (Object-based entity, no IdentifiedObject)
-- LineItem extends Object per ESPI 4.0 XSD (espi.xsd:1449)
-- XSD sequence: amount → rounding → dateTime → note → measurement → itemKind → unitCost → itemPeriod
CREATE TABLE line_items
(
    id                           BIGSERIAL PRIMARY KEY,

    -- ESPI 4.0 fields in XSD sequence order
    amount                       BIGINT,
    rounding                     BIGINT,
    date_time                    BIGINT,
    note                         VARCHAR(256) NOT NULL,

    -- Embedded SummaryMeasurement: measurement
    measurement_multiplier       VARCHAR(255),
    measurement_timestamp        BIGINT,
    measurement_uom              VARCHAR(50),
    measurement_value            BIGINT,
    measurement_reading_type_ref VARCHAR(512),

    item_kind                    INTEGER NOT NULL,
    unit_cost                    BIGINT,

    -- Embedded DateTimeInterval: itemPeriod
    item_period_start            BIGINT,
    item_period_duration         BIGINT,

    -- Foreign key relationship (parent: UsageSummary)
    usage_summary_id             CHAR(36),

    FOREIGN KEY (usage_summary_id) REFERENCES usage_summaries (id) ON DELETE CASCADE
);

CREATE INDEX idx_line_item_usage_summary ON line_items (usage_summary_id);
CREATE INDEX idx_line_item_date_time ON line_items (date_time);
CREATE INDEX idx_line_item_amount ON line_items (amount);
