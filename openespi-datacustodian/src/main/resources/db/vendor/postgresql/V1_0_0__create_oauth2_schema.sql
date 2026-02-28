-- OpenESPI Authorization Server OAuth2 Schema for PostgreSQL
-- Compatible with Spring Authorization Server 1.3+

-- Create oauth2_registered_client table
CREATE TABLE oauth2_registered_client (
    id VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200) DEFAULT NULL,
    client_secret_expires_at TIMESTAMP DEFAULT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris VARCHAR(1000) DEFAULT NULL,
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id)
);

-- Create unique index on client_id
CREATE UNIQUE INDEX idx_oauth2_registered_client_client_id ON oauth2_registered_client(client_id);

-- Create oauth2_authorization table
CREATE TABLE oauth2_authorization (
    id VARCHAR(100) NOT NULL,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000) DEFAULT NULL,
    attributes TEXT DEFAULT NULL,
    state VARCHAR(500) DEFAULT NULL,
    authorization_code_value TEXT DEFAULT NULL,
    authorization_code_issued_at TIMESTAMP DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP DEFAULT NULL,
    authorization_code_metadata VARCHAR(2000) DEFAULT NULL,
    access_token_value TEXT DEFAULT NULL,
    access_token_issued_at TIMESTAMP DEFAULT NULL,
    access_token_expires_at TIMESTAMP DEFAULT NULL,
    access_token_metadata VARCHAR(2000) DEFAULT NULL,
    access_token_type VARCHAR(100) DEFAULT NULL,
    access_token_scopes VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value TEXT DEFAULT NULL,
    oidc_id_token_issued_at TIMESTAMP DEFAULT NULL,
    oidc_id_token_expires_at TIMESTAMP DEFAULT NULL,
    oidc_id_token_metadata VARCHAR(2000) DEFAULT NULL,
    refresh_token_value TEXT DEFAULT NULL,
    refresh_token_issued_at TIMESTAMP DEFAULT NULL,
    refresh_token_expires_at TIMESTAMP DEFAULT NULL,
    refresh_token_metadata VARCHAR(2000) DEFAULT NULL,
    user_code_value VARCHAR(100) DEFAULT NULL,
    user_code_issued_at TIMESTAMP DEFAULT NULL,
    user_code_expires_at TIMESTAMP DEFAULT NULL,
    user_code_metadata VARCHAR(2000) DEFAULT NULL,
    device_code_value VARCHAR(100) DEFAULT NULL,
    device_code_issued_at TIMESTAMP DEFAULT NULL,
    device_code_expires_at TIMESTAMP DEFAULT NULL,
    device_code_metadata VARCHAR(2000) DEFAULT NULL,
    PRIMARY KEY (id)
);

-- Create indexes for oauth2_authorization
CREATE INDEX idx_oauth2_authorization_registered_client_id ON oauth2_authorization(registered_client_id);
CREATE INDEX idx_oauth2_authorization_principal_name ON oauth2_authorization(principal_name);

-- Create oauth2_authorization_consent table
CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- Create index for oauth2_authorization_consent
CREATE INDEX idx_oauth2_authorization_consent_principal_name ON oauth2_authorization_consent(principal_name);

-- ESPI-specific table for application information
CREATE TABLE espi_application_info (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_description TEXT,
    client_uri VARCHAR(500),
    logo_uri VARCHAR(500),
    contact_name VARCHAR(200),
    contact_phone VARCHAR(50),
    contact_email VARCHAR(200),
    tos_uri VARCHAR(500),
    policy_uri VARCHAR(500),
    software_id VARCHAR(100),
    software_version VARCHAR(50),
    software_statement TEXT,
    registration_access_token VARCHAR(500),
    registration_client_uri VARCHAR(500),
    redirect_uri VARCHAR(500),
    scope VARCHAR(1000),
    grant_types VARCHAR(500),
    response_types VARCHAR(500),
    token_endpoint_auth_method VARCHAR(100),
    third_party_notify_uri VARCHAR(500),
    third_party_user_portal_screen_uri VARCHAR(500),
    third_party_scope_selection_screen_uri VARCHAR(500),
    data_custodian_bulk_request_uri VARCHAR(500),
    data_custodian_resource_endpoint VARCHAR(500),
    authorization_server_uri VARCHAR(500),
    authorization_server_authorization_endpoint VARCHAR(500),
    authorization_server_registration_endpoint VARCHAR(500),
    authorization_server_token_endpoint VARCHAR(500),
    data_custodian_id VARCHAR(100),
    third_party_application_type VARCHAR(50),
    third_party_application_use VARCHAR(50),
    third_party_application_description TEXT,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_secret_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create indexes for espi_application_info
CREATE INDEX idx_espi_application_info_client_id ON espi_application_info(client_id);
CREATE INDEX idx_espi_application_info_uuid ON espi_application_info(uuid);

-- Create foreign key constraints
ALTER TABLE oauth2_authorization 
    ADD CONSTRAINT fk_oauth2_authorization_registered_client_id 
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id);

ALTER TABLE oauth2_authorization_consent 
    ADD CONSTRAINT fk_oauth2_authorization_consent_registered_client_id 
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id);

ALTER TABLE espi_application_info 
    ADD CONSTRAINT fk_espi_application_info_client_id 
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client(client_id);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_espi_application_info_updated_at 
    BEFORE UPDATE ON espi_application_info 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default ESPI clients
INSERT INTO oauth2_registered_client 
(id, client_id, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES 
(
    'default-datacustodian-admin',
    'data_custodian_admin',
    'DataCustodian Admin',
    'client_secret_basic',
    'client_credentials',
    '',
    'DataCustodian_Admin_Access',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
),
(
    'default-third-party',
    'third_party',
    'ThirdParty Application',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'http://localhost:8080/DataCustodian/oauth/callback,http://localhost:9090/ThirdParty/oauth/callback',
    'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",21600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",216000.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
);

-- Insert corresponding ESPI application info
INSERT INTO espi_application_info 
(uuid, client_id, client_name, client_description, scope, grant_types, response_types, token_endpoint_auth_method, third_party_application_type)
VALUES 
(
    'aaaaaaaa-1111-1111-1111-000000000001',
    'data_custodian_admin',
    'DataCustodian Admin',
    'Administrative access for DataCustodian operations',
    'DataCustodian_Admin_Access',
    'client_credentials',
    '',
    'client_secret_basic',
    'ADMIN'
),
(
    'aaaaaaaa-1111-1111-1111-000000000002',
    'third_party',
    'ThirdParty Application',
    'Third-party application for customer energy data access',
    'openid,profile,FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
    'authorization_code,refresh_token',
    'code',
    'client_secret_basic',
    'WEB'
);