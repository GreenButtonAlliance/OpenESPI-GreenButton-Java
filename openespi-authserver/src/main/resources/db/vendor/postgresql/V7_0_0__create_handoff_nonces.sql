-- PostgreSQL: Single-use nonce tracking for SignedHandoff payloads (#122 PR C3)
-- Migration Version: V7.0.0
-- Description: handoff_nonces table for AS-side replay protection
-- Author: Green Button Alliance
--
-- The AS-side AuthorizeContinueController consumes the nonce embedded in each
-- verified return handoff. The receiver inserts one row per consumed nonce;
-- the PK uniqueness constraint atomically detects replay attempts. A periodic
-- sweep (out of scope here) reaps rows past expires_at.
--
-- Schema is identical to the openespi-handoff module's canonical V4 migration;
-- this file exists because AS's Flyway is configured per-vendor only and does
-- not scan classpath:db/migration.

CREATE TABLE handoff_nonces (
    nonce        VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMP   NOT NULL,
    consumed_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY (nonce)
);

CREATE INDEX idx_handoff_nonces_expires_at ON handoff_nonces (expires_at);
