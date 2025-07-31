-- Meter Reading Table
CREATE TABLE meter_readings
(
    id              CHAR(36) PRIMARY KEY ,
    uuid            VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb        BIGINT,
    uuid_lsb        BIGINT,
    description     VARCHAR(255),
    created         TIMESTAMP NOT NULL,
    updated         TIMESTAMP NOT NULL,
    published       TIMESTAMP,
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

-- Indexes for meter_readings table
CREATE INDEX idx_meter_reading_uuid ON meter_readings (uuid);
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

-- Indexes for meter_reading_related_links table
CREATE INDEX idx_meter_reading_related_links ON meter_reading_related_links (meter_reading_id);

-- Interval Block Table
CREATE TABLE interval_blocks
(
    id                CHAR(36) PRIMARY KEY ,
    uuid              VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb          BIGINT,
    uuid_lsb          BIGINT,
    description       VARCHAR(255),
    created           TIMESTAMP NOT NULL,
    updated           TIMESTAMP NOT NULL,
    published         TIMESTAMP,
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

-- Indexes for interval_blocks table
CREATE INDEX idx_interval_block_uuid ON interval_blocks (uuid);
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

-- Indexes for interval_block_related_links table
CREATE INDEX idx_interval_block_related_links ON interval_block_related_links (interval_block_id);

-- Interval Reading Table
CREATE TABLE interval_readings
(
    id                   CHAR(36) PRIMARY KEY ,
    uuid                 VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb             BIGINT,
    uuid_lsb             BIGINT,
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

    -- Interval reading specific fields
    cost                 BIGINT,
    reading_value        BIGINT,
    time_period_start    BIGINT,
    time_period_duration BIGINT,
    consumption_tier     BIGINT,
    tou                  BIGINT,
    cpp                  BIGINT,

    -- Foreign key relationships
    interval_block_id    CHAR(36),

    FOREIGN KEY (interval_block_id) REFERENCES interval_blocks (id) ON DELETE CASCADE
);

-- Indexes for interval_readings table
CREATE INDEX idx_interval_reading_uuid ON interval_readings (uuid);
CREATE INDEX idx_interval_reading_interval_block_id ON interval_readings (interval_block_id);
CREATE INDEX idx_interval_reading_time_period_start ON interval_readings (time_period_start);
CREATE INDEX idx_interval_reading_value ON interval_readings (reading_value);
CREATE INDEX idx_interval_reading_created ON interval_readings (created);
CREATE INDEX idx_interval_reading_updated ON interval_readings (updated);


-- Related Links Table for Interval Readings
CREATE TABLE interval_reading_related_links
(
    interval_reading_id CHAR(36) NOT NULL,
    related_links       VARCHAR(1024),
    FOREIGN KEY (interval_reading_id) REFERENCES interval_readings (id) ON DELETE CASCADE
);

-- Indexes for interval_reading_related_links table
CREATE INDEX idx_interval_reading_related_links ON interval_reading_related_links (interval_reading_id);

-- Reading Quality Table
CREATE TABLE reading_qualities
(
    id                  CHAR(36) PRIMARY KEY ,
    uuid                VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb            BIGINT,
    uuid_lsb            BIGINT,
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

    -- Reading quality specific fields
    quality             VARCHAR(50),

    -- Foreign key relationships
    interval_reading_id CHAR(36),

    FOREIGN KEY (interval_reading_id) REFERENCES interval_readings (id) ON DELETE CASCADE
);

-- Indexes for reading_qualities table
CREATE INDEX idx_reading_quality_uuid ON reading_qualities (uuid);
CREATE INDEX idx_reading_quality_interval_reading_id ON reading_qualities (interval_reading_id);
CREATE INDEX idx_reading_quality_quality ON reading_qualities (quality);
CREATE INDEX idx_reading_quality_created ON reading_qualities (created);
CREATE INDEX idx_reading_quality_updated ON reading_qualities (updated);

-- Related Links Table for Reading Qualities
CREATE TABLE reading_quality_related_links
(
    reading_quality_id CHAR(36) NOT NULL,
    related_links      VARCHAR(1024),
    FOREIGN KEY (reading_quality_id) REFERENCES reading_qualities (id) ON DELETE CASCADE
);

-- Indexes for reading_quality_related_links table
CREATE INDEX idx_reading_quality_related_links ON reading_quality_related_links (reading_quality_id);

-- Usage Summary Table
CREATE TABLE usage_summaries
(
    id                          CHAR(36) PRIMARY KEY ,
    uuid                        VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb                    BIGINT,
    uuid_lsb                    BIGINT,
    description                 VARCHAR(255),
    created                     TIMESTAMP NOT NULL,
    updated                     TIMESTAMP NOT NULL,
    published                   TIMESTAMP,
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

    -- Foreign key relationships
    usage_point_id              CHAR(36),

    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

-- Indexes for usage_summaries table
CREATE INDEX idx_usage_summary_uuid ON usage_summaries (uuid);
CREATE INDEX idx_usage_summary_usage_point_id ON usage_summaries (usage_point_id);
CREATE INDEX idx_usage_summary_billing_period_start ON usage_summaries (billing_period_start);
CREATE INDEX idx_usage_summary_created ON usage_summaries (created);
CREATE INDEX idx_usage_summary_updated ON usage_summaries (updated);


-- Related Links Table for Usage Summaries
CREATE TABLE usage_summary_related_links
(
    usage_summary_id CHAR(36) NOT NULL,
    related_links    VARCHAR(1024),
    FOREIGN KEY (usage_summary_id) REFERENCES usage_summaries (id) ON DELETE CASCADE
);

-- Indexes for usage_summary_related_links table
CREATE INDEX idx_usage_summary_related_links ON usage_summary_related_links (usage_summary_id);

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

-- PnodeRef Table
CREATE TABLE pnode_refs
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

    -- PnodeRef specific fields
    apnode_type          VARCHAR(64),
    ref                  VARCHAR(256) NOT NULL,
    start_effective_date BIGINT,
    end_effective_date   BIGINT,

    -- Foreign key relationships
    usage_point_id       CHAR(36) NOT NULL,

    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

-- Indexes for pnode_refs table
CREATE INDEX idx_pnode_ref_uuid ON pnode_refs (uuid);
CREATE INDEX idx_pnode_ref_apnode_type ON pnode_refs (apnode_type);
CREATE INDEX idx_pnode_ref_ref ON pnode_refs (ref);
CREATE INDEX idx_pnode_ref_usage_point_id ON pnode_refs (usage_point_id);
CREATE INDEX idx_pnode_ref_created ON pnode_refs (created);
CREATE INDEX idx_pnode_ref_updated ON pnode_refs (updated);

-- Related Links Table for PnodeRefs
CREATE TABLE pnode_ref_related_links
(
    pnode_ref_id  CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY (pnode_ref_id) REFERENCES pnode_refs (id) ON DELETE CASCADE
);

-- Indexes for pnode_ref_related_links table
CREATE INDEX idx_pnode_ref_related_links ON pnode_ref_related_links (pnode_ref_id);

-- AggregatedNodeRef Table (from V1_9 migration)
CREATE TABLE aggregated_node_refs
(
    id                   CHAR(36)  PRIMARY KEY ,
    uuid                 VARCHAR(36)  NOT NULL UNIQUE,
    uuid_msb             BIGINT,
    uuid_lsb             BIGINT,
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

    -- AggregatedNodeRef specific fields
    anode_type           VARCHAR(64),
    ref                  VARCHAR(256) NOT NULL,
    start_effective_date BIGINT,
    end_effective_date   BIGINT,

    -- Foreign key relationships
    pnode_ref_id         CHAR(36) ,
    usage_point_id       CHAR(36)      NOT NULL,

    FOREIGN KEY (pnode_ref_id) REFERENCES pnode_refs (id) ON DELETE SET NULL,
    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

-- Indexes for aggregated_node_refs table
CREATE INDEX idx_aggregated_node_ref_uuid ON aggregated_node_refs (uuid);
CREATE INDEX idx_aggregated_node_ref_anode_type ON aggregated_node_refs (anode_type);
CREATE INDEX idx_aggregated_node_ref_ref ON aggregated_node_refs (ref);
CREATE INDEX idx_aggregated_node_ref_pnode_ref_id ON aggregated_node_refs (pnode_ref_id);
CREATE INDEX idx_aggregated_node_ref_usage_point_id ON aggregated_node_refs (usage_point_id);
CREATE INDEX idx_aggregated_node_ref_created ON aggregated_node_refs (created);
CREATE INDEX idx_aggregated_node_ref_updated ON aggregated_node_refs (updated);

-- Related Links Table for AggregatedNodeRefs
CREATE TABLE aggregated_node_ref_related_links
(
    aggregated_node_ref_id CHAR(36) NOT NULL,
    related_links          VARCHAR(1024),
    FOREIGN KEY (aggregated_node_ref_id) REFERENCES aggregated_node_refs (id) ON DELETE CASCADE
);

-- Indexes for aggregated_node_ref_related_links table
CREATE INDEX idx_aggregated_node_ref_related_links ON aggregated_node_ref_related_links (aggregated_node_ref_id);

-- Customer Table
CREATE TABLE customers
(
    id                   CHAR(36) PRIMARY KEY ,
    uuid_msb             BIGINT,
    uuid_lsb             BIGINT,
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
    customer_email1                      VARCHAR(255),
    customer_email2                      VARCHAR(255),
    customer_web                         VARCHAR(255),
    customer_radio                       VARCHAR(255),

    -- Status embedded object columns
    status_value                         VARCHAR(256),
    status_date_time                     TIMESTAMP,
    status_reason                        VARCHAR(256),

    -- Priority embedded object columns
    priority_value                       INTEGER,
    priority_rank                        INTEGER,
    priority_type                        VARCHAR(256),

    -- Customer specific fields
    kind                 VARCHAR(50),
    special_need         VARCHAR(255),
    vip                  BOOLEAN              DEFAULT FALSE,
    puc_number           VARCHAR(100),
    status               VARCHAR(50),
    priority             VARCHAR(50),
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
CREATE INDEX idx_customer_status ON customers (status);
CREATE INDEX idx_customer_created ON customers (created);
CREATE INDEX idx_customer_updated ON customers (updated);

-- Customer Agreement Table
CREATE TABLE customer_agreements
(
    id                         CHAR(36) PRIMARY KEY ,
    uuid                       VARCHAR(36) NOT NULL UNIQUE,
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

    -- Document fields
    created_date_time          TIMESTAMP,
    last_modified_date_time    TIMESTAMP,
    revision_number            VARCHAR(256),
    subject                    VARCHAR(256),
    title                      VARCHAR(256),
    type                       VARCHAR(256),

    -- Agreement fields
    sign_date                  TIMESTAMP,
    start                      BIGINT,
    duration                   BIGINT,

    -- Customer agreement specific fields
    load_mgmt                  VARCHAR(256),
    is_pre_pay                 BOOLEAN,
    shut_off_date_time         TIMESTAMP,
    currency                   VARCHAR(3),
    agreement_id               VARCHAR(256)
);

-- Indexes for customer_agreements table
CREATE INDEX idx_customer_agreement_uuid ON customer_agreements (uuid);
CREATE INDEX idx_customer_agreement_sign_date ON customer_agreements (sign_date);
CREATE INDEX idx_customer_agreement_created ON customer_agreements (created);
CREATE INDEX idx_customer_agreement_updated ON customer_agreements (updated);

-- Related Links Table for Customer Agreements
CREATE TABLE customer_agreement_related_links
(
    customer_agreement_id CHAR(36) NOT NULL,
    related_links         VARCHAR(1024),
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
    status_reason         VARCHAR(256),
    FOREIGN KEY (customer_agreement_id) REFERENCES customer_agreements (id) ON DELETE CASCADE
);

-- Indexes for customer_agreement_future_status table
CREATE INDEX idx_customer_agreement_future_status ON customer_agreement_future_status (customer_agreement_id);

-- Customer Account Table (with isPrePay field from V7 migration)
CREATE TABLE customer_accounts
(
    id               CHAR(36) PRIMARY KEY ,
    uuid             VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb         BIGINT,
    uuid_lsb         BIGINT,
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

    -- Document fields
    created_date_time          TIMESTAMP,
    last_modified_date_time    TIMESTAMP,
    revision_number            VARCHAR(256),
    subject                    VARCHAR(256),
    title                      VARCHAR(256),
    document_type              VARCHAR(256),

    -- Customer account specific fields
    contact_name               VARCHAR(256),
    account_id                 VARCHAR(256),
    account_number   VARCHAR(100),
    account_kind     VARCHAR(50),
    budget_bill      VARCHAR(255),
    billing_cycle    VARCHAR(50),
    last_bill_amount BIGINT,
    is_pre_pay       BOOLEAN              DEFAULT FALSE,

    -- Foreign key to customer
    customer_id      CHAR(36),

    FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_account_uuid ON customer_accounts (uuid);
CREATE INDEX idx_customer_account_number ON customer_accounts (account_number);
CREATE INDEX idx_customer_account_kind ON customer_accounts (account_kind);
CREATE INDEX idx_customer_account_customer_id ON customer_accounts (customer_id);
CREATE INDEX idx_customer_account_created ON customer_accounts (created);
CREATE INDEX idx_customer_account_updated ON customer_accounts (updated);

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
    supply_voltage_dips     BIGINT,
    supply_voltage_imbalance BIGINT,
    supply_voltage_variations BIGINT,
    temp_overvoltage        BIGINT,

    -- Embedded DateTimeInterval: summaryInterval
    summary_interval_start  BIGINT,
    summary_interval_duration BIGINT,

    -- Foreign key relationships
    usage_point_id          CHAR(36),

    FOREIGN KEY (usage_point_id) REFERENCES usage_points (id) ON DELETE CASCADE
);

CREATE INDEX idx_epqs_uuid ON electric_power_quality_summaries (uuid);
CREATE INDEX idx_epqs_usage_point_id ON electric_power_quality_summaries (usage_point_id);
CREATE INDEX idx_epqs_summary_interval_start ON electric_power_quality_summaries (uuid);
CREATE INDEX idx_epqs_created ON electric_power_quality_summaries (summary_interval_start);
CREATE INDEX idx_epqs_updated ON electric_power_quality_summaries (updated);


-- End Device Table
CREATE TABLE end_devices
(
    id                   CHAR(36) PRIMARY KEY ,
    uuid                 VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb             BIGINT,
    uuid_lsb             BIGINT,
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
    end_device_email1    VARCHAR(255),
    end_device_email2    VARCHAR(255),
    end_device_web       VARCHAR(255),
    end_device_radio     VARCHAR(255),
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
    status_reason        VARCHAR(256),
    is_virtual           BOOLEAN              DEFAULT FALSE,
    is_pan               BOOLEAN              DEFAULT FALSE,
    install_code         VARCHAR(255),
    amr_system           VARCHAR(100)
);

CREATE INDEX idx_end_device_uuid ON end_devices (uuid);
CREATE INDEX idx_end_device_type ON end_devices (type);
CREATE INDEX idx_end_device_serial_number ON end_devices (serial_number);
CREATE INDEX idx_end_device_status ON end_devices (status_value);
CREATE INDEX idx_end_device_created ON end_devices (created);
CREATE INDEX idx_end_device_updated ON end_devices (updated);


-- Related Links Table for End Devices
CREATE TABLE end_device_related_links
(
    end_device_id CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY (end_device_id) REFERENCES end_devices (id) ON DELETE CASCADE
);

CREATE INDEX idx_end_device_related_links ON end_device_related_links (end_device_id);

-- Line Item Table
CREATE TABLE line_items
(
    id               CHAR(36) PRIMARY KEY ,
    uuid             VARCHAR(36)  NOT NULL UNIQUE,
    uuid_msb         BIGINT,
    uuid_lsb         BIGINT,
    description      VARCHAR(255),
    created          TIMESTAMP NOT NULL,
    updated          TIMESTAMP NOT NULL,
    published        TIMESTAMP,
    up_link_rel      VARCHAR(255),
    up_link_href     VARCHAR(1024),
    up_link_type     VARCHAR(255),
    self_link_rel    VARCHAR(255),
    self_link_href   VARCHAR(1024),
    self_link_type   VARCHAR(255),

    -- Line item specific fields
    amount           BIGINT       NOT NULL,
    rounding         BIGINT,
    date_time        BIGINT       NOT NULL,
    note             VARCHAR(256) NOT NULL,

    -- Foreign key relationships
    usage_summary_id CHAR(36),

    FOREIGN KEY (usage_summary_id) REFERENCES usage_summaries (id) ON DELETE CASCADE
);

CREATE INDEX idx_line_item_uuid ON line_items (uuid);
CREATE INDEX idx_line_item_usage_summary ON line_items (usage_summary_id);
CREATE INDEX idx_line_item_date_time ON line_items (date_time);
CREATE INDEX idx_line_item_amount ON line_items (amount);
CREATE INDEX idx_line_item_created ON line_items (created);
CREATE INDEX idx_line_item_updated ON line_items (updated);

-- Related Links Table for Line Items
CREATE TABLE line_item_related_links
(
    line_item_id  CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY (line_item_id) REFERENCES line_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_line_item_related_links ON line_item_related_links (line_item_id);

-- Meter Entity Table (Joined inheritance from EndDevice)
CREATE TABLE meters
(
    id              CHAR(36) PRIMARY KEY,
    form_number     VARCHAR(256),
    interval_length BIGINT,
    FOREIGN KEY (id) REFERENCES end_devices (id) ON DELETE CASCADE
);

CREATE INDEX idx_meters_form_number ON meters (form_number);

-- Phone Number Table
CREATE TABLE phone_numbers
(
    id                   CHAR(36) PRIMARY KEY ,
    uuid                 VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb             BIGINT,
    uuid_lsb             BIGINT,
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

CREATE INDEX idx_phone_number_uuid ON phone_numbers (uuid);
CREATE INDEX idx_phone_number_itu_phone ON phone_numbers (itu_phone);
CREATE INDEX idx_phone_number_created ON phone_numbers (created);
CREATE INDEX idx_phone_number_updated ON phone_numbers (updated);



-- Related Links Table for Phone Numbers
CREATE TABLE phone_number_related_links
(
    phone_number_id CHAR(36) NOT NULL,
    related_links   VARCHAR(1024),
    FOREIGN KEY (phone_number_id) REFERENCES phone_numbers (id) ON DELETE CASCADE
);

CREATE INDEX idx_phone_number_related_links ON phone_number_related_links (phone_number_id);

-- Program Date ID Mappings Table
CREATE TABLE program_date_id_mappings
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

    -- Program date ID mapping specific fields
    program_date   BIGINT,
    program_id     VARCHAR(100)
);

CREATE INDEX idx_program_date_id_mapping_uuid ON program_date_id_mappings (uuid);
CREATE INDEX idx_program_date_id_mapping_program_date ON program_date_id_mappings (program_date);
CREATE INDEX idx_program_date_id_mapping_program_id ON program_date_id_mappings (program_id);
CREATE INDEX idx_program_date_id_mapping_created ON program_date_id_mappings (created);
CREATE INDEX idx_program_date_id_mapping_updated ON program_date_id_mappings (updated);

-- Related Links Table for Program Date ID Mappings
CREATE TABLE program_date_id_mapping_related_links
(
    program_date_id_mapping_id CHAR(36) NOT NULL,
    related_links              VARCHAR(1024),
    FOREIGN KEY (program_date_id_mapping_id) REFERENCES program_date_id_mappings (id) ON DELETE CASCADE
);

CREATE INDEX idx_program_date_id_mapping_related_links ON program_date_id_mapping_related_links (program_date_id_mapping_id);


-- Service Location Table
CREATE TABLE service_locations
(
    id                  CHAR(36) PRIMARY KEY ,
    uuid                VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb            BIGINT,
    uuid_lsb            BIGINT,
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

    -- Electronic address embedded object columns
    electronic_email1   VARCHAR(255),
    electronic_email2   VARCHAR(255),
    electronic_web      VARCHAR(255),
    electronic_radio    VARCHAR(255),

    -- Status embedded object columns
    status_value        VARCHAR(256),
    status_date_time    TIMESTAMP,
    status_reason       VARCHAR(256),

    -- Service location specific fields
    access_method       VARCHAR(256),
    site_access_problem VARCHAR(256),
    needs_inspection    BOOLEAN              DEFAULT FALSE,
    direction           VARCHAR(256),
    geo_info_reference  VARCHAR(256),
    outage_block        VARCHAR(32)
);

CREATE INDEX idx_service_location_uuid ON service_locations (uuid);
CREATE INDEX idx_service_location_access_method ON service_locations (access_method);
CREATE INDEX idx_service_location_needs_inspection ON service_locations (needs_inspection);
CREATE INDEX idx_service_location_created ON service_locations (created);
CREATE INDEX idx_service_location_updated ON service_locations (updated);


-- Related Links Table for Service Locations
CREATE TABLE service_location_related_links
(
    service_location_id CHAR(36) NOT NULL,
    related_links       VARCHAR(1024),
    FOREIGN KEY (service_location_id) REFERENCES service_locations (id) ON DELETE CASCADE
);

CREATE INDEX idx_service_location_related_links ON service_location_related_links (service_location_id);


-- Service Supplier Table
CREATE TABLE service_suppliers
(
    id                           CHAR(36) PRIMARY KEY ,
    uuid                         VARCHAR(36) NOT NULL UNIQUE,
    uuid_msb                     BIGINT,
    uuid_lsb                     BIGINT,
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
    supplier_email1              VARCHAR(255),
    supplier_email2              VARCHAR(255),
    supplier_web                 VARCHAR(255),
    supplier_radio               VARCHAR(255)
);

CREATE INDEX idx_service_supplier_uuid ON service_suppliers (uuid);
CREATE INDEX idx_service_supplier_kind ON service_suppliers (kind);
CREATE INDEX idx_service_supplier_issuer_id ON service_suppliers (issuer_identification_number);
CREATE INDEX idx_service_supplier_created ON service_suppliers (created);
CREATE INDEX idx_service_supplier_updated ON service_suppliers (updated);


-- Related Links Table for Service Suppliers
CREATE TABLE service_supplier_related_links
(
    service_supplier_id CHAR(36) NOT NULL,
    related_links       VARCHAR(1024),
    FOREIGN KEY (service_supplier_id) REFERENCES service_suppliers (id) ON DELETE CASCADE
);

CREATE INDEX idx_service_supplier_related_links ON service_supplier_related_links (service_supplier_id);


-- Statement Table
CREATE TABLE statements
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

    -- Statement specific fields
    issue_date_time         TIMESTAMP,
    customer_id             CHAR(36),
    statement_date          BIGINT,
    billing_period_start    BIGINT,
    billing_period_duration BIGINT,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_statement_uuid ON statements (uuid);
CREATE INDEX idx_statement_issue_date_time ON statements (issue_date_time);
CREATE INDEX idx_statement_customer_id ON statements (customer_id);
CREATE INDEX idx_statement_statement_date ON statements (statement_date);
CREATE INDEX idx_statement_billing_period_start ON statements (billing_period_start);
CREATE INDEX idx_statement_created ON statements (created);
CREATE INDEX idx_statement_updated ON statements (updated);

-- Related Links Table for Statements
CREATE TABLE statement_related_links
(
    statement_id  CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY (statement_id) REFERENCES statements (id) ON DELETE CASCADE
);

CREATE INDEX idx_statement_related_links ON statement_related_links (statement_id);

-- Statement Ref Table
CREATE TABLE statement_refs
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

    -- Statement ref specific fields
    file_name      VARCHAR(512),
    media_type     VARCHAR(256),
    statement_url  VARCHAR(2048) NOT NULL,
    statement_id   CHAR(36),
    FOREIGN KEY (statement_id) REFERENCES statements(id)
);

CREATE INDEX idx_statement_ref_uuid ON statement_refs (uuid);
CREATE INDEX idx_statement_ref_statement_id ON statement_refs (statement_id);
CREATE INDEX idx_statement_ref_created ON statement_refs (created);
CREATE INDEX idx_statement_ref_updated ON statement_refs (updated);

-- Related Links Table for Statement Refs
CREATE TABLE statement_ref_related_links
(
    statement_ref_id CHAR(36) NOT NULL,
    related_links    VARCHAR(1024),
    FOREIGN KEY (statement_ref_id) REFERENCES statement_refs (id) ON DELETE CASCADE
);

CREATE INDEX idx_statement_ref_related_links ON statement_ref_related_links (statement_ref_id);