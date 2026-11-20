-- Saved Batch/Subscription feeds (#89). TP-only table: V7 is the next shared version, but the Data
-- Custodian never applies this folder (as the DC-only V4 handoff nonces are never applied by the TP).
-- ON DELETE CASCADE: the TP never deletes an authorization in code (revocation only clears tokens);
-- it covers manual SQL, retail_customers -> authorizations ON DELETE CASCADE, and consent withdrawal (#138).
CREATE TABLE subscription_feed_fetches
(
    id               CHAR(36) PRIMARY KEY,
    authorization_id CHAR(36)      NOT NULL,
    fetched_at       TIMESTAMP     NOT NULL,
    resource_uri     VARCHAR(1024) NOT NULL,
    published_min    TIMESTAMP,
    published_max    TIMESTAMP,
    http_status      INT           NOT NULL,
    body             TEXT,
    validation_mode  VARCHAR(16)   NOT NULL,
    schema_valid     BOOLEAN,
    error_count      INT           NOT NULL,
    FOREIGN KEY (authorization_id) REFERENCES authorizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_sff_authorization_fetched ON subscription_feed_fetches (authorization_id, fetched_at);
