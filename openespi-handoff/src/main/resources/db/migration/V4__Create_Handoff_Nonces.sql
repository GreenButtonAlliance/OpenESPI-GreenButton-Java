/*
 * OpenESPI Signed-Handoff Nonce Table (#122 PR C1)
 *
 * Copyright (c) 2018-2025 Green Button Alliance, Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * Single-use nonce tracking for verified SignedHandoff payloads. The receiver
 * inserts one row per consumed nonce; the PK uniqueness constraint atomically
 * detects replay attempts. A periodic sweep (out of scope here) reaps rows
 * past expires_at.
 *
 * Vendor-neutral DDL — H2 / MySQL / PostgreSQL all accept this verbatim.
 */

CREATE TABLE handoff_nonces (
    nonce        VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMP   NOT NULL,
    consumed_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY (nonce)
);

CREATE INDEX idx_handoff_nonces_expires_at ON handoff_nonces (expires_at);
