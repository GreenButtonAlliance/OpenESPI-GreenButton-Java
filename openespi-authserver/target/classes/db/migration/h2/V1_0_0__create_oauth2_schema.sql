-- OpenESPI Authorization Server Database Schema
-- Spring Authorization Server 1.3+ Required Tables
-- H2 Implementation for Local Development

-- OAuth2 Authorization Consent
CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- OAuth2 Authorization
CREATE TABLE oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes blob DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value blob DEFAULT NULL,
    authorization_code_issued_at timestamp DEFAULT NULL,
    authorization_code_expires_at timestamp DEFAULT NULL,
    authorization_code_metadata blob DEFAULT NULL,
    access_token_value blob DEFAULT NULL,
    access_token_issued_at timestamp DEFAULT NULL,
    access_token_expires_at timestamp DEFAULT NULL,
    access_token_metadata blob DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value blob DEFAULT NULL,
    oidc_id_token_issued_at timestamp DEFAULT NULL,
    oidc_id_token_expires_at timestamp DEFAULT NULL,
    oidc_id_token_metadata blob DEFAULT NULL,
    oidc_id_token_claims varchar(2000) DEFAULT NULL,
    refresh_token_value blob DEFAULT NULL,
    refresh_token_issued_at timestamp DEFAULT NULL,
    refresh_token_expires_at timestamp DEFAULT NULL,
    refresh_token_metadata blob DEFAULT NULL,
    user_code_value blob DEFAULT NULL,
    user_code_issued_at timestamp DEFAULT NULL,
    user_code_expires_at timestamp DEFAULT NULL,
    user_code_metadata blob DEFAULT NULL,
    device_code_value blob DEFAULT NULL,
    device_code_issued_at timestamp DEFAULT NULL,
    device_code_expires_at timestamp DEFAULT NULL,
    device_code_metadata blob DEFAULT NULL,
    PRIMARY KEY (id)
);

-- OAuth2 Registered Client
CREATE TABLE oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at timestamp DEFAULT NULL,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    PRIMARY KEY (id)
);

-- ESPI Application Information mapping
CREATE TABLE espi_application_info (
    id bigint AUTO_INCREMENT NOT NULL,
    client_id varchar(100) NOT NULL,
    data_custodian_id varchar(200) DEFAULT NULL,
    data_custodian_application_status varchar(50) DEFAULT NULL,
    third_party_application_description varchar(500) DEFAULT NULL,
    third_party_application_status varchar(50) DEFAULT NULL,
    third_party_application_type varchar(50) DEFAULT NULL,
    third_party_application_use varchar(50) DEFAULT NULL,
    third_party_phone varchar(100) DEFAULT NULL,
    authorization_server_uri varchar(500) DEFAULT NULL,
    token_endpoint_auth_method varchar(50) DEFAULT NULL,
    client_name varchar(200) DEFAULT NULL,
    client_uri varchar(500) DEFAULT NULL,
    logo_uri varchar(500) DEFAULT NULL,
    policy_uri varchar(500) DEFAULT NULL,
    tos_uri varchar(500) DEFAULT NULL,
    redirect_uri varchar(500) DEFAULT NULL,
    software_id varchar(200) DEFAULT NULL,
    software_version varchar(100) DEFAULT NULL,
    client_id_issued_at timestamp DEFAULT NULL,
    client_secret_expires_at timestamp DEFAULT NULL,
    registration_client_uri varchar(500) DEFAULT NULL,
    registration_access_token varchar(1000) DEFAULT NULL,
    data_custodian_bulk_request_uri varchar(500) DEFAULT NULL,
    data_custodian_resource_endpoint varchar(500) DEFAULT NULL,
    third_party_notify_uri varchar(500) DEFAULT NULL,
    third_party_scope_selection_screen_uri varchar(500) DEFAULT NULL,
    third_party_user_portal_screen_uri varchar(500) DEFAULT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (client_id)
);

-- Create indexes for performance
CREATE INDEX idx_oauth2_authorization_client_principal ON oauth2_authorization (registered_client_id, principal_name);
CREATE INDEX idx_oauth2_registered_client_id ON oauth2_registered_client (client_id);
CREATE INDEX idx_espi_application_client_id ON espi_application_info (client_id);

-- Insert sample data for local development
INSERT INTO oauth2_registered_client (
    id, client_id, client_name, client_authentication_methods, authorization_grant_types, 
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    '1', 'data_custodian_admin', 'DataCustodian Admin', 'client_secret_basic', 'client_credentials',
    '', 'DataCustodian_Admin_Access', '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
);

INSERT INTO oauth2_registered_client (
    id, client_id, client_name, client_authentication_methods, authorization_grant_types, 
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    '2', 'third_party', 'ThirdParty Application', 'client_secret_basic', 'authorization_code,refresh_token',
    'http://localhost:9090/oauth/callback', 'FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13,openid,profile', 
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",21600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",129600.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
);