-- H2: OAuth2 audit log (#192)
-- Migration Version: V8.0.0
-- Description: oauth2_audit_log table for the H2 test/local profile
-- Author: Green Button Alliance
--
-- MySQL and PostgreSQL create oauth2_audit_log in their V2_0_0 migration, which H2 never had.
-- The authorization purge job (AuthorizationPurgeService) writes one AUTHORIZATION_PURGED row here
-- before deleting each fully-expired oauth2_authorization row, and the consent, client-management
-- and certificate services already write to it, so H2 needs the table too.
--
-- Version: the H2 folder already holds V7_0_0, so V8_0_0 is the next unused version. Re-using the
-- empty V2 slot would put a pending migration *below* the applied V7 on any existing H2 database,
-- which Flyway's default validation rejects (and V2 means "ESPI 4.0 compliance enhancements" in
-- the MySQL/PostgreSQL folders, not just this table).
--
-- Columns match the MySQL and PostgreSQL definitions. Type mapping: ip_address is INET on
-- PostgreSQL and VARCHAR(45) on MySQL, so VARCHAR(45) here; compliance_flags/additional_data are
-- JSON on MySQL and JSONB on PostgreSQL, so H2's native JSON type.

CREATE TABLE oauth2_audit_log (
    id                BIGINT AUTO_INCREMENT NOT NULL,
    event_type        VARCHAR(50)   NOT NULL,
    client_id         VARCHAR(100)  NOT NULL,
    principal_name    VARCHAR(200)  DEFAULT NULL,
    event_timestamp   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address        VARCHAR(45)   DEFAULT NULL,
    user_agent        VARCHAR(500)  DEFAULT NULL,
    session_id        VARCHAR(128)  DEFAULT NULL,
    request_id        VARCHAR(128)  DEFAULT NULL,
    success           BOOLEAN       DEFAULT TRUE NOT NULL,
    error_code        VARCHAR(50)   DEFAULT NULL,
    error_description VARCHAR(500)  DEFAULT NULL,
    scopes_requested  VARCHAR(1000) DEFAULT NULL,
    scopes_granted    VARCHAR(1000) DEFAULT NULL,
    grant_type        VARCHAR(50)   DEFAULT NULL,
    espi_version      VARCHAR(10)   DEFAULT '4.0',
    compliance_flags  JSON          DEFAULT NULL,
    additional_data   JSON          DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_client_timestamp ON oauth2_audit_log (client_id, event_timestamp);
CREATE INDEX idx_audit_principal_timestamp ON oauth2_audit_log (principal_name, event_timestamp);
CREATE INDEX idx_audit_event_type ON oauth2_audit_log (event_type);
CREATE INDEX idx_audit_ip_address ON oauth2_audit_log (ip_address);
CREATE INDEX idx_audit_session_id ON oauth2_audit_log (session_id);
