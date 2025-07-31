/*
 * OpenESPI Base Schema Migration (Vendor-Neutral)
 * 
 * Copyright (c) 2018-2025 Green Button Alliance, Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * This migration creates the vendor-neutral base tables for the OpenESPI database schema.
 * It includes all tables that do not require vendor-specific column types (BLOB/BINARY/BYTEA).
 * 
 * Excluded tables (handled in vendor-specific V2 migrations):
 * - time_configurations (contains BLOB columns)
 * - usage_points (contains BLOB columns)  
 * - time_configuration_related_links (FK dependency)
 * - usage_point_related_links (FK dependency)
 *
 * Total tables in this migration: 15
 * Compatible with: H2, MySQL, PostgreSQL
 */

-- IdentifiedObject Related Links Table (used by all entities extending IdentifiedObject)
CREATE TABLE identified_object_related_links
(
    identified_object_id CHAR(36) NOT NULL,
    rel                  VARCHAR(255),
    href                 VARCHAR(1024),
    link_type            VARCHAR(255)
);

CREATE INDEX idx_identified_object_related_links ON identified_object_related_links (identified_object_id);


-- Application Information Table
CREATE TABLE application_information
(
    id                              CHAR(36) PRIMARY KEY,
    uuid_msb                        BIGINT,
    uuid_lsb                        BIGINT,
    description                     VARCHAR(255),
    created                         TIMESTAMP NOT NULL,
    updated                         TIMESTAMP NOT NULL,
    published                       TIMESTAMP,
    up_link_rel                     VARCHAR(255),
    up_link_href                    TEXT,
    up_link_type                    VARCHAR(255),
    self_link_rel                   VARCHAR(255),
    self_link_href                  TEXT,
    self_link_type                  VARCHAR(255),

    -- Application specific fields
    kind                            VARCHAR(255),
    data_custodian_application_status VARCHAR(255),
    data_custodian_default_batch_resource TEXT,
    data_custodian_default_subscription_resource TEXT,
    client_name                     VARCHAR(255),
    client_id                       VARCHAR(255) NOT NULL UNIQUE,
    client_secret                   VARCHAR(255),
    client_id_issued_at             BIGINT,
    client_secret_expires_at        BIGINT,
    registration_client_uri         TEXT,
    registration_access_token       TEXT,
    redirect_uris                   TEXT,
    software_id                     VARCHAR(255),
    software_version                VARCHAR(255),
    
    -- OAuth2 and Third Party specific fields
    third_party_application_description TEXT,
    third_party_application_status VARCHAR(50),
    third_party_application_type VARCHAR(50),
    third_party_application_use VARCHAR(255),
    third_party_phone VARCHAR(50),
    authorization_server_uri TEXT,
    third_party_notify_uri TEXT,
    authorization_server_authorization_endpoint TEXT,
    authorization_server_registration_endpoint TEXT,
    authorization_server_token_endpoint TEXT,
    data_custodian_bulk_request_uri TEXT,
    data_custodian_third_party_selection_screen_uri TEXT,
    data_custodian_resource_endpoint TEXT,
    third_party_data_custodian_selection_screen_uri TEXT,
    third_party_login_screen_uri TEXT,
    third_party_scope_selection_screen_uri TEXT,
    third_party_user_portal_screen_uri TEXT,
    logo_uri TEXT,
    client_uri TEXT,
    redirect_uri TEXT,
    tos_uri TEXT,
    policy_uri TEXT,
    contacts TEXT,
    token_endpoint_auth_method VARCHAR(50),
    data_custodian_scope_selection_screen_uri TEXT,
    data_custodian_id VARCHAR(64),
    third_party_application_name VARCHAR(64) NOT NULL DEFAULT 'Default Third Party Application Name',
    response_types                  VARCHAR(255),
    grant_types                     VARCHAR(255),
    application_type                VARCHAR(50),
    jwks_uri                        TEXT,
    sector_identifier_uri           TEXT,
    subject_type                    VARCHAR(50),
    id_token_signed_response_alg    VARCHAR(50),
    id_token_encrypted_response_alg VARCHAR(50),
    id_token_encrypted_response_enc VARCHAR(50),
    userinfo_signed_response_alg    VARCHAR(50),
    userinfo_encrypted_response_alg VARCHAR(50),
    userinfo_encrypted_response_enc VARCHAR(50),
    request_object_signing_alg      VARCHAR(50),
    request_object_encryption_alg   VARCHAR(50),
    request_object_encryption_enc   VARCHAR(50),
    default_max_age                 BIGINT,
    require_auth_time               BOOLEAN              DEFAULT FALSE,
    default_acr_values              VARCHAR(255),
    initiate_login_uri              TEXT,
    request_uris                    TEXT
);

CREATE INDEX idx_application_client_id ON application_information (client_id);
CREATE INDEX idx_application_created ON application_information (created);
CREATE INDEX idx_application_updated ON application_information (updated);

-- Related Links Table for Application Information
CREATE TABLE application_information_related_links
(
    application_information_id CHAR(36) NOT NULL,
    related_links              VARCHAR(1024),
    FOREIGN KEY (application_information_id) REFERENCES application_information (id) ON DELETE CASCADE
);

CREATE INDEX idx_app_info_related_links ON application_information_related_links (application_information_id);

-- Grant Types Table for Application Information
CREATE TABLE application_information_grant_types
(
    application_information_id CHAR(36) NOT NULL,
    grant_type                 VARCHAR(255),
    FOREIGN KEY (application_information_id) REFERENCES application_information (id) ON DELETE CASCADE
);

CREATE INDEX idx_app_info_grant_types ON application_information_grant_types (application_information_id);

-- Scopes Table for Application Information
CREATE TABLE application_information_scopes
(
    application_information_id CHAR(36) NOT NULL,
    scope                      VARCHAR(255),
    FOREIGN KEY (application_information_id) REFERENCES application_information (id) ON DELETE CASCADE
);

CREATE INDEX idx_app_info_scopes ON application_information_scopes (application_information_id);

-- Retail Customer Table
CREATE TABLE retail_customers
(
    id             CHAR(36) PRIMARY KEY,
    uuid           VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb       BIGINT,
    uuid_lsb       BIGINT,
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

    -- Retail customer specific fields
    username       VARCHAR(255) UNIQUE,
    first_name     VARCHAR(255),
    last_name      VARCHAR(255),
    password       VARCHAR(255),
    enabled        BOOLEAN              DEFAULT TRUE,
    role           VARCHAR(50)          DEFAULT 'ROLE_USER',
    email          VARCHAR(100),
    phone          VARCHAR(20),
    account_created BIGINT,
    last_login     BIGINT,
    account_locked BOOLEAN              DEFAULT FALSE,
    failed_login_attempts INTEGER       DEFAULT 0
);

CREATE INDEX idx_retail_customer_uuid ON retail_customers (uuid);
CREATE INDEX idx_retail_customer_username ON retail_customers (username);
CREATE INDEX idx_retail_customer_created ON retail_customers (created);
CREATE INDEX idx_retail_customer_updated ON retail_customers (updated);

-- Related Links Table for Retail Customers
CREATE TABLE retail_customer_related_links
(
    retail_customer_id CHAR(36) NOT NULL,
    related_links      VARCHAR(1024),
    FOREIGN KEY (retail_customer_id) REFERENCES retail_customers (id) ON DELETE CASCADE
);

CREATE INDEX idx_retail_customer_related_links ON retail_customer_related_links (retail_customer_id);

-- Service Delivery Point Table
CREATE TABLE service_delivery_points
(
    id                 CHAR(36) PRIMARY KEY ,
    uuid               VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb           BIGINT,
    uuid_lsb           BIGINT,
    description        VARCHAR(255),
    created            TIMESTAMP NOT NULL,
    updated            TIMESTAMP NOT NULL,
    published          TIMESTAMP,
    up_link_rel        VARCHAR(255),
    up_link_href       VARCHAR(1024),
    up_link_type       VARCHAR(255),
    self_link_rel      VARCHAR(255),
    self_link_href     VARCHAR(1024),
    self_link_type     VARCHAR(255),

    -- Service delivery point specific fields
    sdp_mrid           VARCHAR(64),
    sdp_name           VARCHAR(256),
    sdp_tariff_profile VARCHAR(256),
    sdp_customer_agreement VARCHAR(256)
);

CREATE INDEX idx_sdp_uuid ON service_delivery_points (uuid);
CREATE INDEX idx_sdp_name ON service_delivery_points (sdp_name);
CREATE INDEX idx_sdp_tariff_profile ON service_delivery_points (sdp_tariff_profile);
CREATE INDEX idx_sdp_customer_agreement ON service_delivery_points (sdp_customer_agreement);
CREATE INDEX idx_sdp_created ON service_delivery_points (created);
CREATE INDEX idx_sdp_updated ON service_delivery_points (updated);

-- Related Links Table for Service Delivery Points
CREATE TABLE service_delivery_point_related_links
(
    service_delivery_point_id CHAR(36) NOT NULL,
    related_links             VARCHAR(1024),
    FOREIGN KEY (service_delivery_point_id) REFERENCES service_delivery_points (id) ON DELETE CASCADE
);

CREATE INDEX idx_sdp_related_links ON service_delivery_point_related_links (service_delivery_point_id);

-- Authorization Table
CREATE TABLE authorizations
(
    id                         CHAR(36) PRIMARY KEY ,
    uuid_msb                   BIGINT,
    uuid_lsb                   BIGINT,
    description                VARCHAR(255),
    created                    TIMESTAMP NOT NULL,
    updated                    TIMESTAMP NOT NULL,
    published                  TIMESTAMP,
    up_link_rel                VARCHAR(255),
    up_link_href               VARCHAR(1024),
    up_link_type               VARCHAR(255),
    self_link_rel              VARCHAR(255),
    self_link_href             VARCHAR(1024),
    self_link_type             VARCHAR(255),

    -- Authorization specific fields
    authorized_period_start    BIGINT,
    authorized_period_duration BIGINT,
    published_period_start     BIGINT,
    published_period_duration  BIGINT,
    application_information_id CHAR(36),
    retail_customer_id         CHAR(36),
    subscription_id            CHAR(36),
    access_token               VARCHAR(1024),
    refresh_token              VARCHAR(1024),
    code                       VARCHAR(1024),
    authorization_uri          VARCHAR(1024),
    ap_title                   VARCHAR(255),
    ap_description             TEXT,
    ap_duration                BIGINT,
    scope                      VARCHAR(256),
    token_type                 VARCHAR(50)          DEFAULT 'Bearer',
    expires_in                 BIGINT,
    state                      VARCHAR(256),
    error                      VARCHAR(255),
    error_description          TEXT,
    error_uri                  VARCHAR(1024),
    resource_uri               VARCHAR(1024),
    customer_resource_uri      VARCHAR(1024),
    status                     VARCHAR(50)          DEFAULT 'ACTIVE',
    expires_at                 TIMESTAMP,
    grant_type                 VARCHAR(50),
    response_type              VARCHAR(50),
    third_party                VARCHAR(255),

    FOREIGN KEY (application_information_id) REFERENCES application_information (id) ON DELETE CASCADE,
    FOREIGN KEY (retail_customer_id) REFERENCES retail_customers (id) ON DELETE CASCADE
);

CREATE INDEX idx_authorization_app_id ON authorizations (application_information_id);
CREATE INDEX idx_authorization_customer_id ON authorizations (retail_customer_id);
CREATE INDEX idx_authorization_status ON authorizations (status);
CREATE INDEX idx_authorization_expires_at ON authorizations (expires_at);
CREATE INDEX idx_authorization_created ON authorizations (created);
CREATE INDEX idx_authorization_updated ON authorizations (updated);

-- Related Links Table for Authorizations
CREATE TABLE authorization_related_links
(
    authorization_id CHAR(36) NOT NULL,
    related_links    VARCHAR(1024),
    FOREIGN KEY (authorization_id) REFERENCES authorizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_authorization_related_links ON authorization_related_links (authorization_id);

-- Reading Type Table (Independent - no foreign key dependencies)
CREATE TABLE reading_types
(
    id                      CHAR(36) PRIMARY KEY ,
    uuid                    VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb                BIGINT,
    uuid_lsb                BIGINT,
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

    -- Reading type specific fields
    accumulation_behaviour  VARCHAR(50),
    commodity               VARCHAR(50),
    consumption_tier        VARCHAR(50),
    currency                VARCHAR(3),
    data_qualifier          VARCHAR(50),
    default_quality         VARCHAR(50),
    flow_direction          VARCHAR(50),
    interval_length         BIGINT,
    kind                    VARCHAR(50),
    phase                   VARCHAR(50),
    power_of_ten_multiplier VARCHAR(50),
    time_attribute          VARCHAR(50),
    uom                     VARCHAR(50),
    cpp                     VARCHAR(50),
    measuring_period        VARCHAR(50),
    tou                     VARCHAR(50),
    argument_numerator      DECIMAL(38,0),
    argument_denominator    DECIMAL(38,0),
    interharmonic_numerator BIGINT,
    interharmonic_denominator BIGINT
);

CREATE INDEX idx_reading_type_uuid ON reading_types (uuid);
CREATE INDEX idx_reading_type_kind ON reading_types (kind);
CREATE INDEX idx_reading_type_commodity ON reading_types (commodity);
CREATE INDEX idx_reading_type_uom ON reading_types (uom);
CREATE INDEX idx_reading_type_created ON reading_types (created);
CREATE INDEX idx_reading_type_updated ON reading_types (updated);

-- Related Links Table for Reading Types
CREATE TABLE reading_type_related_links
(
    reading_type_id CHAR(36) NOT NULL,
    related_links   VARCHAR(1024),
    FOREIGN KEY (reading_type_id) REFERENCES reading_types (id) ON DELETE CASCADE
);

CREATE INDEX idx_reading_type_related_links ON reading_type_related_links (reading_type_id);

-- Subscription Table (depends only on application_information and retail_customers)
CREATE TABLE subscriptions
(
    id                             CHAR(36) PRIMARY KEY ,
    uuid                           VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb                       BIGINT,
    uuid_lsb                       BIGINT,
    description                    VARCHAR(255),
    created                        TIMESTAMP NOT NULL,
    updated                        TIMESTAMP NOT NULL,
    published                      TIMESTAMP,
    up_link_rel                    VARCHAR(255),
    up_link_href                   VARCHAR(1024),
    up_link_type                   VARCHAR(255),
    self_link_rel                  VARCHAR(255),
    self_link_href                 VARCHAR(1024),
    self_link_type                 VARCHAR(255),

    -- Subscription specific fields
    hashed_id                      VARCHAR(64),
    has_customer_matching_criteria BOOLEAN              DEFAULT FALSE,
    last_update                    TIMESTAMP,

    -- Foreign key relationships
    application_information_id     CHAR(36),
    authorization_id               CHAR(36),
    retail_customer_id             CHAR(36),

    FOREIGN KEY (application_information_id) REFERENCES application_information (id) ON DELETE CASCADE,
    FOREIGN KEY (retail_customer_id) REFERENCES retail_customers (id) ON DELETE CASCADE
);

CREATE INDEX idx_subscription_uuid ON subscriptions (uuid);
CREATE INDEX idx_subscription_app_id ON subscriptions (application_information_id);
CREATE INDEX idx_subscription_customer_id ON subscriptions (retail_customer_id);
CREATE INDEX idx_subscription_last_update ON subscriptions (last_update);
CREATE INDEX idx_subscription_created ON subscriptions (created);
CREATE INDEX idx_subscription_updated ON subscriptions (updated);

-- Related Links Table for Subscriptions
CREATE TABLE subscription_related_links
(
    subscription_id CHAR(36) NOT NULL,
    related_links   VARCHAR(1024),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE
);

CREATE INDEX idx_subscription_related_links ON subscription_related_links (subscription_id);

-- Batch List Table (Independent - no foreign key dependencies)
CREATE TABLE batch_lists
(
    id             CHAR(36) PRIMARY KEY ,
    uuid           VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb       BIGINT,
    uuid_lsb       BIGINT,
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

    -- Batch list specific fields
    resource_count INT                  DEFAULT 0
);

CREATE INDEX idx_batch_list_uuid ON batch_lists (uuid);
CREATE INDEX idx_batch_list_created ON batch_lists (created);
CREATE INDEX idx_batch_list_resource_count ON batch_lists (resource_count);
CREATE INDEX idx_batch_list_updated ON batch_lists (updated);

-- Related Links Table for Batch Lists
CREATE TABLE batch_list_related_links
(
    batch_list_id CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY (batch_list_id) REFERENCES batch_lists (id) ON DELETE CASCADE
);

CREATE INDEX idx_batch_list_related_links ON batch_list_related_links (batch_list_id);

-- Batch List Resources Collection Table
CREATE TABLE batch_list_resources
(
    batch_list_id CHAR(36)     NOT NULL,
    resource_uri  VARCHAR(512) NOT NULL,
    FOREIGN KEY (batch_list_id) REFERENCES batch_lists (id) ON DELETE CASCADE
);

CREATE INDEX idx_batch_list_resources_batch_id ON batch_list_resources (batch_list_id);
CREATE INDEX idx_batch_list_resources_uri ON batch_list_resources (resource_uri);