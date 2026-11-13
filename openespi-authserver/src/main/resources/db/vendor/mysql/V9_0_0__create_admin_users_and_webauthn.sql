-- #205: Authorization Server admin user store + Spring Security WebAuthn (passkey) credential tables.
--
-- admin_users holds HUMAN administrators of the Authorization Server only. Retail customers never
-- authenticate here (they authenticate at the Data Custodian and arrive via the signed handoff).
-- Roles live in a child table (admin_user_roles) rather than a delimited column so that each grant is
-- a row the database can constrain, index and cascade-delete.

CREATE TABLE admin_users (
    id             VARCHAR(36)  NOT NULL,
    username       VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at  TIMESTAMP    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_users_username UNIQUE (username)
);

CREATE TABLE admin_user_roles (
    user_id  VARCHAR(36) NOT NULL,
    role     VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_admin_user_roles_user FOREIGN KEY (user_id) REFERENCES admin_users (id) ON DELETE CASCADE,
    CONSTRAINT ck_admin_user_roles_role CHECK (role IN ('ROLE_ADMIN', 'ROLE_DC_ADMIN'))
);

-- Spring Security 7.0.2 WebAuthn JDBC schema (org/springframework/security/user-entities-schema.sql
-- and user-credentials-schema.sql in spring-security-webauthn), adapted for MySQL/InnoDB:
--   * the VARCHAR(1000) primary keys use CHARACTER SET ascii. They hold base64url ids, and a utf8mb4
--     VARCHAR(1000) key (4000 bytes) exceeds InnoDB's 3072-byte index limit;
--   * BLOB columns for the COSE key and attestation data; TIMESTAMP NULL for created/last_used.
CREATE TABLE user_entities (
    id           VARCHAR(1000) CHARACTER SET ascii NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    display_name VARCHAR(200),
    PRIMARY KEY (id)
);

CREATE TABLE user_credentials (
    credential_id                VARCHAR(1000) CHARACTER SET ascii NOT NULL,
    user_entity_user_id          VARCHAR(1000) CHARACTER SET ascii NOT NULL,
    public_key                   BLOB          NOT NULL,
    signature_count              BIGINT,
    uv_initialized               BOOLEAN,
    backup_eligible              BOOLEAN       NOT NULL,
    authenticator_transports     VARCHAR(1000),
    public_key_credential_type   VARCHAR(100),
    backup_state                 BOOLEAN       NOT NULL,
    attestation_object           BLOB,
    attestation_client_data_json BLOB,
    created                      TIMESTAMP     NULL,
    last_used                    TIMESTAMP     NULL,
    label                        VARCHAR(1000) NOT NULL,
    PRIMARY KEY (credential_id)
);

CREATE INDEX idx_user_credentials_user ON user_credentials (user_entity_user_id);
