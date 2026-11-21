-- Saved Batch/Subscription feeds (#89). TP-only table: V7 is the next shared version, but the Data
-- Custodian never applies this folder (as the DC-only V4 handoff nonces are never applied by the TP).
-- ON DELETE CASCADE: the TP never deletes an authorization in code (revocation only clears tokens);
-- it covers manual SQL, retail_customers -> authorizations ON DELETE CASCADE, and consent withdrawal (#138).
-- DATETIME(6), not TIMESTAMP(6): MySQL's TIMESTAMP ends in 2038 and starts after the 1970 epoch that a scope
-- without HistoryLength fetches from; DATETIME holds every range FetchRange accepts (1970 to 9999-12-31).
CREATE TABLE subscription_feed_fetches
(
    id               CHAR(36) PRIMARY KEY,
    authorization_id CHAR(36)      NOT NULL,
    fetched_at       DATETIME(6)   NOT NULL,
    resource_uri     VARCHAR(1024) NOT NULL,
    published_min    DATETIME(6),
    published_max    DATETIME(6),
    http_status      INT           NOT NULL,
    body             LONGTEXT,
    validation_mode  VARCHAR(16)   NOT NULL,
    schema_valid     BOOLEAN,
    error_count      INT           NOT NULL,
    FOREIGN KEY (authorization_id) REFERENCES authorizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_sff_authorization_fetched ON subscription_feed_fetches (authorization_id, fetched_at);
