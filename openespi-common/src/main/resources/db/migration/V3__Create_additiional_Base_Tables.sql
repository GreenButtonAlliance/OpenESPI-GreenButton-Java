/*
 * OpenESPI Additional Base Tables Migration
 *
 * Copyright (c) 2018-2025 Green Button Alliance, Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * This migration creates additional vendor-neutral tables that depend on both
 * V1 (base tables) and V2 (vendor-specific tables) to already exist.
 *
 * IMPORTANT: The following tables were moved to V2 vendor-specific files
 * because they require vendor-specific syntax OR are part of dependency chains
 * that include vendor-specific tables:
 *
 * Moved to V2:
 * - meter_readings (depends on usage_points from V2)
 * - meter_reading_related_links (FK dependency)
 * - interval_blocks (depends on meter_readings)
 * - interval_block_related_links (FK dependency)
 * - interval_readings (extends Object, requires vendor-specific auto-increment, depends on interval_blocks)
 * - reading_qualities (extends Object, requires vendor-specific auto-increment, depends on interval_readings)
 * - usage_summaries + usage_summary_related_links (moved to maintain dependency order with line_items)
 *
 * Reason: IntervalReading and ReadingQuality both extend Object (not IdentifiedObject) per ESPI 4.0 XSD
 * (espi.xsd:1016 and espi.xsd:1062), requiring Long ID with vendor-specific auto-increment syntax
 * (BIGINT AUTO_INCREMENT for MySQL/H2, BIGSERIAL for PostgreSQL). To keep the dependency chain together
 * (meter_readings → interval_blocks → interval_readings → reading_qualities), all were moved to V2 vendor files.
 *
 * LineItem also extends Object (espi.xsd:1449) and requires vendor-specific auto-increment. Since LineItem
 * has a foreign key to usage_summaries, usage_summaries was moved to V2 to maintain proper dependency order.
 *
 * Tables in this migration:
 * - subscription_usage_points (join table)
 * - aggregated_node_refs (depends on pnode_refs from V2)
 * - customer schema tables
 * - end_device schema tables
 * - service location/supplier tables
 * - statement tables
 *
 * Compatible with: H2, MySQL, PostgreSQL
 */

-- ==================================================================================
-- TABLES MOVED TO V2 VENDOR-SPECIFIC MIGRATION FILES
-- ==================================================================================
-- The following tables have been moved to V2 vendor-specific files:
--   - db/vendor/mysql/V2__MySQL_Specific_Tables.sql
--   - db/vendor/postgres/V2__PostgreSQL_Specific_Tables.sql
--   - db/vendor/h2/V2__H2_Specific_Tables.sql
--
-- Tables moved:
--   1. meter_readings + meter_reading_related_links
--   2. interval_blocks + interval_block_related_links
--   3. interval_readings (no related_links - extends Object, not IdentifiedObject)
--   4. reading_qualities (no related_links - extends Object, not IdentifiedObject)
--
-- See file header for detailed explanation.
-- ==================================================================================

-- Reading Quality Table - Moved to vendor-specific V2 migration files
-- ReadingQuality extends Object (not IdentifiedObject) per ESPI 4.0 XSD (espi.xsd:1062)
-- Table creation moved to V2 vendor files due to auto-increment syntax differences
-- See: db/vendor/mysql/V2__MySQL_Specific_Tables.sql
--      db/vendor/postgres/V2__PostgreSQL_Specific_Tables.sql
--      db/vendor/h2/V2__H2_Specific_Tables.sql

-- Usage Summary Table - Moved to vendor-specific V2 migration files
-- UsageSummary table moved to V2 migration files because LineItem (which extends Object)
-- requires vendor-specific auto-increment syntax and has a foreign key to usage_summaries.
-- To maintain proper dependency order, usage_summaries must be created before line_items.
-- Table creation moved to V2 vendor files.
-- See: db/vendor/mysql/V2__MySQL_Specific_Tables.sql
--      db/vendor/postgres/V2__PostgreSQL_Specific_Tables.sql
--      db/vendor/h2/V2__H2_Specific_Tables.sql

-- Join Table for Subscription-UsagePoint Many-to-Many Relationship
CREATE TABLE subscription_usage_points
(
    subscription_id CHAR(36) NOT NULL,
    usage_point_id  CHAR(36) NOT NULL,
    PRIMARY KEY (subscription_id, usage_point_id),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE,
    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

-- Indexes for subscription_usage_points table
CREATE INDEX idx_subscription_usage_points_subscription ON subscription_usage_points (subscription_id);
CREATE INDEX idx_subscription_usage_points_usage_point ON subscription_usage_points (usage_point_id);

-- Add foreign key constraint from authorizations to subscriptions
ALTER TABLE authorizations ADD CONSTRAINT fk_authorization_subscription
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE SET NULL;

-- Add foreign key constraint from usage_points to subscriptions
ALTER TABLE usage_points ADD CONSTRAINT fk_usage_point_subscription
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE SET NULL;

-- PnodeRef Table - Moved to vendor-specific V2 migration files
-- PnodeRef extends Object (not IdentifiedObject) per ESPI 4.0 XSD (espi.xsd:1539)
-- Table creation moved to V2 vendor files due to auto-increment syntax differences

-- AggregatedNodeRef Table - Moved to vendor-specific V2 migration files
-- AggregatedNodeRef extends Object (not IdentifiedObject) per ESPI 4.0 XSD (espi.xsd:1570)
-- Table creation moved to V2 vendor files due to auto-increment syntax differences
-- See: db/vendor/mysql/V2__MySQL_Specific_Tables.sql
--      db/vendor/postgres/V2__PostgreSQL_Specific_Tables.sql
--      db/vendor/h2/V2__H2_Specific_Tables.sql

-- Customer Table
CREATE TABLE customers
(
    id                   CHAR(36) PRIMARY KEY ,
    description          VARCHAR(255),
    created              TIMESTAMP NOT NULL,
    updated              TIMESTAMP NOT NULL,
    published            TIMESTAMP,
    up_link_rel          VARCHAR(255),
    up_link_href         VARCHAR(1024),
    up_link_type         VARCHAR(255),
    self_link_rel        VARCHAR(255),
    self_link_href       VARCHAR(1024),
    self_link_type       VARCHAR(255),

    -- Organisation embedded object columns
    customer_organisation_name           VARCHAR(255),
    customer_street_detail               VARCHAR(255),
    customer_town_detail                 VARCHAR(255),
    customer_state_or_province           VARCHAR(255),
    customer_postal_code                 VARCHAR(255),
    customer_country                     VARCHAR(255),
    customer_postal_street_detail        VARCHAR(255),
    customer_postal_town_detail          VARCHAR(255),
    customer_postal_state_or_province    VARCHAR(255),
    customer_postal_postal_code          VARCHAR(255),
    customer_postal_country              VARCHAR(255),
    -- Organisation.electronicAddress (customer.xsd lines 886-936)
    customer_lan                         VARCHAR(255),
    customer_mac                         VARCHAR(255),
    customer_email1                      VARCHAR(255),
    customer_email2                      VARCHAR(255),
    customer_web                         VARCHAR(255),
    customer_radio                       VARCHAR(255),
    customer_user_id                     VARCHAR(255),
    customer_password                    VARCHAR(255),
    -- Organisation.phone1 (customer.xsd lines 942-989)
    customer_phone1_country_code         VARCHAR(10),
    customer_phone1_area_code            VARCHAR(10),
    customer_phone1_city_code            VARCHAR(10),
    customer_phone1_local_number         VARCHAR(30),
    customer_phone1_ext                  VARCHAR(20),
    customer_phone1_dial_out             VARCHAR(10),
    customer_phone1_international_prefix VARCHAR(10),
    customer_phone1_itu_phone            VARCHAR(50),
    -- Organisation.phone2 (customer.xsd lines 990-1037)
    customer_phone2_country_code         VARCHAR(10),
    customer_phone2_area_code            VARCHAR(10),
    customer_phone2_city_code            VARCHAR(10),
    customer_phone2_local_number         VARCHAR(30),
    customer_phone2_ext                  VARCHAR(20),
    customer_phone2_dial_out             VARCHAR(10),
    customer_phone2_international_prefix VARCHAR(10),
    customer_phone2_itu_phone            VARCHAR(50),

    -- Status embedded object columns
    status_value                         VARCHAR(256),
    status_date_time                     TIMESTAMP,
    status_reason                        VARCHAR(256),

    -- Priority embedded object columns
    priority_value                       INTEGER,
    priority_rank                        INTEGER,
    priority_type                        VARCHAR(256),

    -- Customer specific fields (field order matches customer.xsd:72-112)
    kind                 VARCHAR(50),
    special_need         VARCHAR(255),
    vip                  BOOLEAN              DEFAULT FALSE,
    puc_number           VARCHAR(100),
    locale               VARCHAR(10),
    customer_name        VARCHAR(255),

    -- ATOM href reference to usage schema retail customer (NO foreign key)
    retail_customer_href VARCHAR(1024),

    -- Foreign key to time configuration
    time_configuration_id CHAR(36)
);

-- Indexes for customers table
CREATE INDEX idx_customer_kind ON customers (kind);
CREATE INDEX idx_customer_puc_number ON customers (puc_number);
CREATE INDEX idx_customer_status ON customers (status_value);
CREATE INDEX idx_customer_created ON customers (created);
CREATE INDEX idx_customer_updated ON customers (updated);

-- Related Links Table for Customers
CREATE TABLE customer_related_links
(
    customer_id CHAR(36) NOT NULL,
    rel         VARCHAR(255),
    href        VARCHAR(1024),
    link_type   VARCHAR(255),
    FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_related_links ON customer_related_links (customer_id);

-- Customer Agreement Table
CREATE TABLE customer_agreements
(
    id                         CHAR(36) PRIMARY KEY ,
    description                VARCHAR(255),
    created                    TIMESTAMP NOT NULL,
    updated                    TIMESTAMP NOT NULL,
    published                  TIMESTAMP,
    customer_agreement_up_link_rel                VARCHAR(255),
    customer_agreement_up_link_href               VARCHAR(1024),
    customer_agreement_up_link_type               VARCHAR(255),
    customer_agreement_self_link_rel              VARCHAR(255),
    customer_agreement_self_link_href             VARCHAR(1024),
    customer_agreement_self_link_type             VARCHAR(255),

    -- Document fields (customer.xsd lines 819-885) - field order matches XSD
    document_type              VARCHAR(256),
    author_name                VARCHAR(256),
    created_date_time          TIMESTAMP,
    last_modified_date_time    TIMESTAMP,
    revision_number            VARCHAR(256),
    -- Document.electronicAddress embedded object (customer.xsd lines 886-936)
    doc_lan                    VARCHAR(256),
    doc_mac                    VARCHAR(256),
    doc_email1                 VARCHAR(256),
    doc_email2                 VARCHAR(256),
    doc_web                    VARCHAR(256),
    doc_radio                  VARCHAR(256),
    doc_user_id                VARCHAR(256),
    doc_password               VARCHAR(256),
    subject                    VARCHAR(256),
    title                      VARCHAR(256),
    -- Document.docStatus embedded object (customer.xsd lines 1254-1284)
    doc_status_value           VARCHAR(256),
    doc_status_date_time       TIMESTAMP,
    doc_status_remark          VARCHAR(256),
    doc_status_reason          VARCHAR(256),
    -- Document.status embedded object (customer.xsd lines 1254-1284)
    status_value               VARCHAR(256),
    status_date_time           TIMESTAMP,
    status_remark              VARCHAR(256),
    status_reason              VARCHAR(256),
    comment                    VARCHAR(256),

    -- Agreement fields (customer.xsd lines 622-642)
    sign_date                  TIMESTAMP,
    validity_start             BIGINT,
    validity_duration          BIGINT,

    -- Customer agreement specific fields (customer.xsd lines 159-209)
    load_mgmt                  VARCHAR(256),
    is_pre_pay                 BOOLEAN,
    shut_off_date_time         TIMESTAMP,
    currency                   VARCHAR(3),
    agreement_id               VARCHAR(256)
);

-- Indexes for customer_agreements table
CREATE INDEX idx_customer_agreement_sign_date ON customer_agreements (sign_date);
CREATE INDEX idx_customer_agreement_created ON customer_agreements (created);
CREATE INDEX idx_customer_agreement_updated ON customer_agreements (updated);

-- Related Links Table for Customer Agreements
CREATE TABLE customer_agreement_related_links
(
    customer_agreement_id CHAR(36) NOT NULL,
    rel                   VARCHAR(255),
    href                  VARCHAR(1024),
    link_type             VARCHAR(255),
    FOREIGN KEY (customer_agreement_id) REFERENCES customer_agreements (id) ON DELETE CASCADE
);

-- Indexes for customer_agreement_related_links table
CREATE INDEX idx_customer_agreement_related_links ON customer_agreement_related_links (customer_agreement_id);

-- Customer Agreement Future Status Collection Table
CREATE TABLE customer_agreement_future_status
(
    customer_agreement_id CHAR(36) NOT NULL,
    status_value          VARCHAR(256),
    status_date_time      TIMESTAMP,
    status_remark         VARCHAR(256),
    status_reason         VARCHAR(256),
    FOREIGN KEY (customer_agreement_id) REFERENCES customer_agreements (id) ON DELETE CASCADE
);

-- Indexes for customer_agreement_future_status table
CREATE INDEX idx_customer_agreement_future_status ON customer_agreement_future_status (customer_agreement_id);

-- Customer Account Table
-- Per customer.xsd CustomerAccount (lines 118-158) and Document base type (lines 819-872)
-- Phase 18: Schema compliance with all Document fields
CREATE TABLE customer_accounts
(
    id               CHAR(36) PRIMARY KEY ,
    description      VARCHAR(255),
    created          TIMESTAMP NOT NULL,
    updated          TIMESTAMP NOT NULL,
    published        TIMESTAMP,
    customer_account_up_link_rel      VARCHAR(255),
    customer_account_up_link_href     VARCHAR(1024),
    customer_account_up_link_type     VARCHAR(255),
    customer_account_self_link_rel    VARCHAR(255),
    customer_account_self_link_href   VARCHAR(1024),
    customer_account_self_link_type   VARCHAR(255),

    -- Document fields (customer.xsd lines 819-872) - field order matches XSD
    document_type              VARCHAR(256),
    author_name                VARCHAR(256),
    created_date_time          TIMESTAMP,
    last_modified_date_time    TIMESTAMP,
    revision_number            VARCHAR(256),
    -- Document.electronicAddress embedded object (customer.xsd lines 886-936)
    doc_lan                    VARCHAR(256),
    doc_mac                    VARCHAR(256),
    doc_email1                 VARCHAR(256),
    doc_email2                 VARCHAR(256),
    doc_web                    VARCHAR(256),
    doc_radio                  VARCHAR(256),
    doc_user_id                VARCHAR(256),
    doc_password               VARCHAR(256),
    subject                    VARCHAR(256),
    title                      VARCHAR(256),
    -- Document.docStatus embedded object (customer.xsd lines 1254-1284)
    doc_status_value           VARCHAR(256),
    doc_status_date_time       TIMESTAMP,
    doc_status_remark          VARCHAR(512),
    doc_status_reason          VARCHAR(512),

    -- CustomerAccount specific fields (customer.xsd lines 118-158)
    billing_cycle              VARCHAR(50),
    budget_bill                VARCHAR(255),
    last_bill_amount           BIGINT,
    -- notifications collection mapped to customer_account_notifications table below
    -- contactInfo Organisation embedded object
    -- contactInfo.streetAddress
    street_detail              VARCHAR(256),
    town_detail                VARCHAR(256),
    state_or_province          VARCHAR(256),
    postal_code                VARCHAR(256),
    country                    VARCHAR(256),
    -- contactInfo.postalAddress
    postal_street_detail       VARCHAR(256),
    postal_town_detail         VARCHAR(256),
    postal_state_or_province   VARCHAR(256),
    postal_postal_code         VARCHAR(256),
    postal_country             VARCHAR(256),
    -- contactInfo.phone1 (customer.xsd TelephoneNumber lines 1428-1478)
    contact_phone1_country_code           VARCHAR(10),
    contact_phone1_area_code              VARCHAR(10),
    contact_phone1_city_code              VARCHAR(10),
    contact_phone1_local_number           VARCHAR(30),
    contact_phone1_ext                    VARCHAR(20),
    contact_phone1_dial_out               VARCHAR(10),
    contact_phone1_international_prefix   VARCHAR(10),
    contact_phone1_itu_phone              VARCHAR(50),
    -- contactInfo.phone2 (customer.xsd TelephoneNumber lines 1428-1478)
    contact_phone2_country_code           VARCHAR(10),
    contact_phone2_area_code              VARCHAR(10),
    contact_phone2_city_code              VARCHAR(10),
    contact_phone2_local_number           VARCHAR(30),
    contact_phone2_ext                    VARCHAR(20),
    contact_phone2_dial_out               VARCHAR(10),
    contact_phone2_international_prefix   VARCHAR(10),
    contact_phone2_itu_phone              VARCHAR(50),
    -- contactInfo.electronicAddress (customer.xsd lines 886-936)
    contact_lan                VARCHAR(256),
    contact_mac                VARCHAR(256),
    contact_email1             VARCHAR(256),
    contact_email2             VARCHAR(256),
    contact_web                VARCHAR(256),
    contact_radio              VARCHAR(256),
    contact_user_id            VARCHAR(256),
    contact_password           VARCHAR(256),
    -- contactInfo.organisationName
    organisation_name          VARCHAR(256),
    account_id                 VARCHAR(256),

    -- Extension fields not in customer.xsd
    is_pre_pay                 BOOLEAN              DEFAULT FALSE,

    -- Foreign key to customer
    customer_id                CHAR(36),

    FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
);

-- Indexes - Per Phase 18 guidelines, only ID-based indexes
CREATE INDEX idx_customer_account_customer_id ON customer_accounts (customer_id);
CREATE INDEX idx_customer_account_created ON customer_accounts (created);
CREATE INDEX idx_customer_account_updated ON customer_accounts (updated);

-- Related Links Table for Customer Accounts
CREATE TABLE customer_account_related_links
(
    customer_account_id CHAR(36) NOT NULL,
    rel                 VARCHAR(255),
    href                VARCHAR(1024),
    link_type           VARCHAR(255),
    FOREIGN KEY (customer_account_id) REFERENCES customer_accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_account_related_links ON customer_account_related_links (customer_account_id);

-- Customer Account Notifications Table
CREATE TABLE customer_account_notifications
(
    customer_account_id           CHAR(36) NOT NULL,
    method_kind                   VARCHAR(255),
    time                          TIMESTAMP,
    note                          VARCHAR(512),
    customer_notification_kind    VARCHAR(256),
    FOREIGN KEY (customer_account_id) REFERENCES customer_accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_account_notifications ON customer_account_notifications (customer_account_id);


-- Electric Power Quality Summary Table
CREATE TABLE electric_power_quality_summaries
(
    id                      CHAR(36) PRIMARY KEY ,
    description             VARCHAR(255),
    created                 TIMESTAMP NOT NULL,
    updated                 TIMESTAMP NOT NULL,
    published               TIMESTAMP,
    up_link_rel             VARCHAR(255),
    up_link_href            VARCHAR(1024),
    up_link_type            VARCHAR(255),
    self_link_rel           VARCHAR(255),
    self_link_href          VARCHAR(1024),
    self_link_type          VARCHAR(255),

    -- Electric power quality summary specific fields
    flicker_plt             BIGINT,
    flicker_pst             BIGINT,
    harmonic_voltage        BIGINT,
    long_interruptions      BIGINT,
    mains_voltage           BIGINT,
    measurement_protocol    SMALLINT,
    power_frequency         BIGINT,
    rapid_voltage_changes   BIGINT,
    short_interruptions     BIGINT,

    -- Embedded DateTimeInterval: summaryInterval
    summary_interval_start  BIGINT,
    summary_interval_duration BIGINT,

    supply_voltage_dips     BIGINT,
    supply_voltage_imbalance BIGINT,
    supply_voltage_variations BIGINT,
    temp_overvoltage        BIGINT,

    -- Foreign key relationships
    usage_point_id          CHAR(36),

    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

CREATE INDEX idx_epqs_usage_point_id ON electric_power_quality_summaries (usage_point_id);
CREATE INDEX idx_epqs_summary_interval_start ON electric_power_quality_summaries (summary_interval_start);
CREATE INDEX idx_epqs_created ON electric_power_quality_summaries (created);
CREATE INDEX idx_epqs_updated ON electric_power_quality_summaries (updated);

-- Related Links Table for Electric Power Quality Summaries
CREATE TABLE electric_power_quality_summary_related_links
(
    electric_power_quality_summary_id CHAR(36) NOT NULL,
    rel                               VARCHAR(255),
    href                              VARCHAR(1024),
    link_type                         VARCHAR(255),
    FOREIGN KEY (electric_power_quality_summary_id) REFERENCES electric_power_quality_summaries (id) ON DELETE CASCADE
);

CREATE INDEX idx_epqs_related_links ON electric_power_quality_summary_related_links (electric_power_quality_summary_id);

-- End Device Table
CREATE TABLE end_devices
(
    id                   CHAR(36) PRIMARY KEY ,
    description          VARCHAR(255),
    created              TIMESTAMP NOT NULL,
    updated              TIMESTAMP NOT NULL,
    published            TIMESTAMP,
    up_link_rel          VARCHAR(255),
    up_link_href         VARCHAR(1024),
    up_link_type         VARCHAR(255),
    self_link_rel        VARCHAR(255),
    self_link_href       VARCHAR(1024),
    self_link_type       VARCHAR(255),

    -- End device specific fields
    type                 VARCHAR(50),
    utc_number           VARCHAR(100),
    serial_number        VARCHAR(100),
    lot_number           VARCHAR(100),
    purchase_price       BIGINT,
    critical             BOOLEAN              DEFAULT FALSE,
    -- ElectronicAddress (customer.xsd lines 886-936)
    end_device_lan       VARCHAR(255),
    end_device_mac       VARCHAR(255),
    end_device_email1    VARCHAR(255),
    end_device_email2    VARCHAR(255),
    end_device_web       VARCHAR(255),
    end_device_radio     VARCHAR(255),
    end_device_user_id   VARCHAR(255),
    end_device_password  VARCHAR(255),
    installation_date    TIMESTAMP,
    manufactured_date    TIMESTAMP,
    purchase_date        TIMESTAMP,
    received_date        TIMESTAMP,
    retirement_date      TIMESTAMP,
    removal_date         TIMESTAMP,
    acceptance_test_date_time TIMESTAMP,
    acceptance_test_success BOOLEAN,
    acceptance_test_type VARCHAR(255),
    initial_condition    VARCHAR(255),
    initial_loss_of_life DECIMAL(5, 2),
    status_value         VARCHAR(256),
    status_date_time     TIMESTAMP,
    status_remark        VARCHAR(256),
    status_reason        VARCHAR(256),
    is_virtual           BOOLEAN              DEFAULT FALSE,
    is_pan               BOOLEAN              DEFAULT FALSE,
    install_code         VARCHAR(255),
    amr_system           VARCHAR(100)
);

CREATE INDEX idx_end_device_type ON end_devices (type);
CREATE INDEX idx_end_device_serial_number ON end_devices (serial_number);
CREATE INDEX idx_end_device_status ON end_devices (status_value);
CREATE INDEX idx_end_device_created ON end_devices (created);
CREATE INDEX idx_end_device_updated ON end_devices (updated);


-- Related Links Table for End Devices
CREATE TABLE end_device_related_links
(
    end_device_id CHAR(36) NOT NULL,
    rel           VARCHAR(255),
    href          VARCHAR(1024),
    link_type     VARCHAR(255),
    FOREIGN KEY (end_device_id) REFERENCES end_devices (id) ON DELETE CASCADE
);

CREATE INDEX idx_end_device_related_links ON end_device_related_links (end_device_id);

-- Line Item Table - Moved to vendor-specific V2 migration files
-- LineItem extends Object (not IdentifiedObject) per ESPI 4.0 XSD (espi.xsd:1449)
-- Table creation moved to V2 vendor files due to auto-increment syntax differences
-- See: db/vendor/mysql/V2__MySQL_Specific_Tables.sql
--      db/vendor/postgres/V2__PostgreSQL_Specific_Tables.sql
--      db/vendor/h2/V2__H2_Specific_Tables.sql

-- Meter Entity Table (extends IdentifiedObject, embeds Asset + EndDeviceFields)
-- Per customer.xsd, Meter extends EndDevice which extends Asset
-- Implementation uses composition: Meter embeds Asset + EndDeviceFields as @Embeddable
CREATE TABLE meters
(
    id                   CHAR(36) PRIMARY KEY ,
    description          VARCHAR(255),
    created              TIMESTAMP NOT NULL,
    updated              TIMESTAMP NOT NULL,
    published            TIMESTAMP,
    up_link_rel          VARCHAR(255),
    up_link_href         VARCHAR(1024),
    up_link_type         VARCHAR(255),
    self_link_rel        VARCHAR(255),
    self_link_href       VARCHAR(1024),
    self_link_type       VARCHAR(255),

    -- Asset fields (embedded from Asset.java) - Same structure as end_devices
    type                 VARCHAR(50),
    utc_number           VARCHAR(100),
    serial_number        VARCHAR(100),
    lot_number           VARCHAR(100),
    purchase_price       BIGINT,
    critical             BOOLEAN              DEFAULT FALSE,
    -- ElectronicAddress (customer.xsd lines 886-936)
    end_device_lan       VARCHAR(255),
    end_device_mac       VARCHAR(255),
    end_device_email1    VARCHAR(255),
    end_device_email2    VARCHAR(255),
    end_device_web       VARCHAR(255),
    end_device_radio     VARCHAR(255),
    end_device_user_id   VARCHAR(255),
    end_device_password  VARCHAR(255),
    installation_date    TIMESTAMP,
    manufactured_date    TIMESTAMP,
    purchase_date        TIMESTAMP,
    received_date        TIMESTAMP,
    retirement_date      TIMESTAMP,
    removal_date         TIMESTAMP,
    acceptance_test_date_time TIMESTAMP,
    acceptance_test_success BOOLEAN,
    acceptance_test_type VARCHAR(255),
    initial_condition    VARCHAR(255),
    initial_loss_of_life DECIMAL(5, 2),
    status_value         VARCHAR(256),
    status_date_time     TIMESTAMP,
    status_remark        VARCHAR(256),
    status_reason        VARCHAR(256),

    -- EndDevice fields (embedded from EndDeviceFields.java)
    is_virtual           BOOLEAN              DEFAULT FALSE,
    is_pan               BOOLEAN              DEFAULT FALSE,
    install_code         VARCHAR(255),
    amr_system           VARCHAR(100),

    -- Meter-specific fields (customer.xsd Meter lines 250-264)
    form_number          VARCHAR(256),
    interval_length      BIGINT
);

-- Meter Multipliers Collection Table (@ElementCollection for MeterEntity.meterMultipliers)
-- Per customer.xsd MeterMultiplier type (embedded collection)
CREATE TABLE meter_multipliers
(
    meter_id         CHAR(36) NOT NULL,
    multiplier_kind  VARCHAR(256),
    multiplier_value DECIMAL(19, 4),
    FOREIGN KEY (meter_id) REFERENCES meters (id) ON DELETE CASCADE
);

CREATE INDEX idx_meter_multipliers_meter_id ON meter_multipliers (meter_id);

-- Related Links Table for Meters
CREATE TABLE meter_related_links
(
    meter_id  CHAR(36) NOT NULL,
    rel       VARCHAR(255),
    href      VARCHAR(1024),
    link_type VARCHAR(255),
    FOREIGN KEY (meter_id) REFERENCES meters (id) ON DELETE CASCADE
);

CREATE INDEX idx_meter_related_links ON meter_related_links (meter_id);

-- Phone Number Table
CREATE TABLE phone_numbers
(
    id                   CHAR(36) PRIMARY KEY ,
    description          VARCHAR(255),
    created              TIMESTAMP NOT NULL,
    updated              TIMESTAMP NOT NULL,
    published            TIMESTAMP,
    up_link_rel          VARCHAR(255),
    up_link_href         VARCHAR(1024),
    up_link_type         VARCHAR(255),
    self_link_rel        VARCHAR(255),
    self_link_href       VARCHAR(1024),
    self_link_type       VARCHAR(255),

    -- Phone number specific fields
    country_code         VARCHAR(10),
    area_code            VARCHAR(10),
    city_code            VARCHAR(10),
    local_number         VARCHAR(20),
    extension            VARCHAR(10),
    dial_out             VARCHAR(10),
    international_prefix VARCHAR(10),
    itu_phone            VARCHAR(50),
    phone_type           VARCHAR(20),

    -- Polymorphic relationship fields
    parent_entity_uuid   VARCHAR(36),
    parent_entity_type   VARCHAR(255)
);

CREATE INDEX idx_phone_number_itu_phone ON phone_numbers (itu_phone);
CREATE INDEX idx_phone_number_created ON phone_numbers (created);
CREATE INDEX idx_phone_number_updated ON phone_numbers (updated);

-- Program Date ID Mappings Table
-- Per customer.xsd lines 269-283, ProgramDateIdMappings extends IdentifiedObject
-- and contains embedded ProgramDateIdMapping (lines 1223-1251)
CREATE TABLE program_date_id_mappings
(
    id             CHAR(36) PRIMARY KEY ,
    description    VARCHAR(255),
    created        TIMESTAMP NOT NULL,
    updated        TIMESTAMP NOT NULL,
    published      TIMESTAMP,
    up_link_rel    VARCHAR(255),
    up_link_href   VARCHAR(1024),
    up_link_type   VARCHAR(255),
    self_link_rel  VARCHAR(255),
    self_link_href VARCHAR(1024),
    self_link_type VARCHAR(255),

    -- ProgramDateIdMapping embedded fields (customer.xsd lines 1223-1251)
    program_date_type VARCHAR(64),
    code              VARCHAR(64),
    name              VARCHAR(256),
    note              VARCHAR(256)
);

-- Related Links Table for Program Date ID Mappings
CREATE TABLE program_date_id_mapping_related_links
(
    program_date_id_mapping_id CHAR(36) NOT NULL,
    rel                        VARCHAR(255),
    href                       VARCHAR(1024),
    link_type                  VARCHAR(255),
    FOREIGN KEY (program_date_id_mapping_id) REFERENCES program_date_id_mappings (id) ON DELETE CASCADE
);

CREATE INDEX idx_program_date_id_mapping_related_links ON program_date_id_mapping_related_links (program_date_id_mapping_id);


-- Service Location Table
CREATE TABLE service_locations
(
    id                  CHAR(36) PRIMARY KEY ,
    description         VARCHAR(255),
    created             TIMESTAMP NOT NULL,
    updated             TIMESTAMP NOT NULL,
    published           TIMESTAMP,
    up_link_rel         VARCHAR(255),
    up_link_href        VARCHAR(1024),
    up_link_type        VARCHAR(255),
    self_link_rel       VARCHAR(255),
    self_link_href      VARCHAR(1024),
    self_link_type      VARCHAR(255),

    -- Service location specific fields
    type                VARCHAR(256),

    -- Main address embedded object columns
    main_street_detail  VARCHAR(255),
    main_town_detail    VARCHAR(255),
    main_state_or_province VARCHAR(255),
    main_postal_code    VARCHAR(255),
    main_country        VARCHAR(255),

    -- Secondary address embedded object columns
    secondary_street_detail VARCHAR(255),
    secondary_town_detail VARCHAR(255),
    secondary_state_or_province VARCHAR(255),
    secondary_postal_code VARCHAR(255),
    secondary_country   VARCHAR(255),

    -- Electronic address embedded object columns (customer.xsd lines 886-936)
    electronic_lan      VARCHAR(255),
    electronic_mac      VARCHAR(255),
    electronic_email1   VARCHAR(255),
    electronic_email2   VARCHAR(255),
    electronic_web      VARCHAR(255),
    electronic_radio    VARCHAR(255),
    electronic_user_id  VARCHAR(255),
    electronic_password VARCHAR(255),

    -- Status embedded object columns (customer.xsd Status type - 4 fields)
    status_value        VARCHAR(256),
    status_date_time    TIMESTAMP,
    status_remark       VARCHAR(256),
    status_reason       VARCHAR(256),

    -- Phone1 embedded object columns (customer.xsd TelephoneNumber type - 8 fields)
    phone1_country_code VARCHAR(10),
    phone1_area_code    VARCHAR(10),
    phone1_city_code    VARCHAR(10),
    phone1_local_number VARCHAR(30),
    phone1_ext          VARCHAR(20),
    phone1_dial_out     VARCHAR(10),
    phone1_international_prefix VARCHAR(10),
    phone1_itu_phone    VARCHAR(50),

    -- Phone2 embedded object columns (customer.xsd TelephoneNumber type - 8 fields)
    phone2_country_code VARCHAR(10),
    phone2_area_code    VARCHAR(10),
    phone2_city_code    VARCHAR(10),
    phone2_local_number VARCHAR(30),
    phone2_ext          VARCHAR(20),
    phone2_dial_out     VARCHAR(10),
    phone2_international_prefix VARCHAR(10),
    phone2_itu_phone    VARCHAR(50),

    -- Service location specific fields
    access_method       VARCHAR(256),
    site_access_problem VARCHAR(256),
    needs_inspection    BOOLEAN              DEFAULT FALSE,
    direction           VARCHAR(256),
    geo_info_reference  VARCHAR(256),
    outage_block        VARCHAR(32)
);

CREATE INDEX idx_service_location_access_method ON service_locations (access_method);
CREATE INDEX idx_service_location_needs_inspection ON service_locations (needs_inspection);
CREATE INDEX idx_service_location_created ON service_locations (created);
CREATE INDEX idx_service_location_updated ON service_locations (updated);


-- Related Links Table for Service Locations
-- Stores Atom <link rel="related"> elements per NAESB ESPI 4.0 REQ.21.4.3.1.1.6
CREATE TABLE service_location_related_links
(
    service_location_id CHAR(36) NOT NULL,
    rel                 VARCHAR(255),
    href                VARCHAR(1024),
    link_type           VARCHAR(255),
    FOREIGN KEY (service_location_id) REFERENCES service_locations (id) ON DELETE CASCADE
);

CREATE INDEX idx_service_location_related_links ON service_location_related_links (service_location_id);


-- UsagePoint Hrefs Table for Service Locations
-- Cross-stream references from customer.xsd to usage.xsd
-- Stores UsagePoint atom:link[@rel='self']/@href URLs per customer.xsd lines 1106-1111
CREATE TABLE service_location_usage_point_hrefs
(
    service_location_id CHAR(36) NOT NULL,
    usage_point_href    VARCHAR(512),
    FOREIGN KEY (service_location_id) REFERENCES service_locations (id) ON DELETE CASCADE
);

CREATE INDEX idx_service_location_usage_point_hrefs ON service_location_usage_point_hrefs (service_location_id);


-- Service Supplier Table
CREATE TABLE service_suppliers
(
    id                           CHAR(36) PRIMARY KEY ,
    description                  VARCHAR(255),
    created                      TIMESTAMP NOT NULL,
    updated                      TIMESTAMP NOT NULL,
    published                    TIMESTAMP,
    up_link_rel                  VARCHAR(255),
    up_link_href                 VARCHAR(1024),
    up_link_type                 VARCHAR(255),
    self_link_rel                VARCHAR(255),
    self_link_href               VARCHAR(1024),
    self_link_type               VARCHAR(255),

    -- Service supplier specific fields
    kind                         VARCHAR(50),
    issuer_identification_number VARCHAR(100),
    effective_date               TIMESTAMP,

    -- Embedded Organisation object columns
    supplier_organisation_name   VARCHAR(255),
    supplier_street_detail       VARCHAR(255),
    supplier_town_detail         VARCHAR(255),
    supplier_state_or_province   VARCHAR(255),
    supplier_postal_code         VARCHAR(255),
    supplier_country             VARCHAR(255),
    supplier_postal_street_detail VARCHAR(255),
    supplier_postal_town_detail  VARCHAR(255),
    supplier_postal_state_or_province VARCHAR(255),
    supplier_postal_postal_code  VARCHAR(255),
    supplier_postal_country      VARCHAR(255),
    supplier_lan                 VARCHAR(256),
    supplier_mac                 VARCHAR(256),
    supplier_email1              VARCHAR(255),
    supplier_email2              VARCHAR(255),
    supplier_web                 VARCHAR(255),
    supplier_radio               VARCHAR(255),
    supplier_user_id             VARCHAR(256),
    supplier_password            VARCHAR(256),
    supplier_phone1_country_code         VARCHAR(10),
    supplier_phone1_area_code            VARCHAR(10),
    supplier_phone1_city_code            VARCHAR(10),
    supplier_phone1_local_number         VARCHAR(30),
    supplier_phone1_ext                  VARCHAR(20),
    supplier_phone1_dial_out             VARCHAR(10),
    supplier_phone1_international_prefix VARCHAR(10),
    supplier_phone1_itu_phone            VARCHAR(50),
    supplier_phone2_country_code         VARCHAR(10),
    supplier_phone2_area_code            VARCHAR(10),
    supplier_phone2_city_code            VARCHAR(10),
    supplier_phone2_local_number         VARCHAR(30),
    supplier_phone2_ext                  VARCHAR(20),
    supplier_phone2_dial_out             VARCHAR(10),
    supplier_phone2_international_prefix VARCHAR(10),
    supplier_phone2_itu_phone            VARCHAR(50)
);

CREATE INDEX idx_service_supplier_kind ON service_suppliers (kind);
CREATE INDEX idx_service_supplier_issuer_id ON service_suppliers (issuer_identification_number);
CREATE INDEX idx_service_supplier_created ON service_suppliers (created);
CREATE INDEX idx_service_supplier_updated ON service_suppliers (updated);


-- Related Links Table for Service Suppliers
CREATE TABLE service_supplier_related_links
(
    service_supplier_id CHAR(36) NOT NULL,
    rel                 VARCHAR(255),
    href                VARCHAR(1024),
    link_type           VARCHAR(255),
    FOREIGN KEY (service_supplier_id) REFERENCES service_suppliers (id) ON DELETE CASCADE
);

CREATE INDEX idx_service_supplier_related_links ON service_supplier_related_links (service_supplier_id);


-- Statement Table
CREATE TABLE statements
(
    id                      CHAR(36) PRIMARY KEY ,
    description             VARCHAR(255),
    created                 TIMESTAMP NOT NULL,
    updated                 TIMESTAMP NOT NULL,
    published               TIMESTAMP,
    up_link_rel             VARCHAR(255),
    up_link_href            VARCHAR(1024),
    up_link_type            VARCHAR(255),
    self_link_rel           VARCHAR(255),
    self_link_href          VARCHAR(1024),
    self_link_type          VARCHAR(255),

    -- Statement specific fields
    issue_date_time         TIMESTAMP,
    customer_id             CHAR(36),
    statement_date          BIGINT,
    billing_period_start    BIGINT,
    billing_period_duration BIGINT,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_statement_issue_date_time ON statements (issue_date_time);
CREATE INDEX idx_statement_customer_id ON statements (customer_id);
CREATE INDEX idx_statement_statement_date ON statements (statement_date);
CREATE INDEX idx_statement_billing_period_start ON statements (billing_period_start);
CREATE INDEX idx_statement_created ON statements (created);
CREATE INDEX idx_statement_updated ON statements (updated);

-- Related Links Table for Statements
CREATE TABLE statement_related_links
(
    statement_id CHAR(36) NOT NULL,
    rel          VARCHAR(255),
    href         VARCHAR(1024),
    link_type    VARCHAR(255),
    FOREIGN KEY (statement_id) REFERENCES statements (id) ON DELETE CASCADE
);

CREATE INDEX idx_statement_related_links ON statement_related_links (statement_id);

-- Statement Ref Table
CREATE TABLE statement_refs
(
    id             CHAR(36) PRIMARY KEY ,
    description    VARCHAR(255),
    created        TIMESTAMP,
    updated        TIMESTAMP,
    published      TIMESTAMP,
    up_link_rel    VARCHAR(255),
    up_link_href   VARCHAR(1024),
    up_link_type   VARCHAR(255),
    self_link_rel  VARCHAR(255),
    self_link_href VARCHAR(1024),
    self_link_type VARCHAR(255),

    -- Statement ref specific fields
    file_name      VARCHAR(512),
    media_type     VARCHAR(256),
    statement_url  VARCHAR(2048),
    statement_id   CHAR(36),
    FOREIGN KEY (statement_id) REFERENCES statements(id)
);

CREATE INDEX idx_statement_ref_statement_id ON statement_refs (statement_id);
CREATE INDEX idx_statement_ref_created ON statement_refs (created);
CREATE INDEX idx_statement_ref_updated ON statement_refs (updated);