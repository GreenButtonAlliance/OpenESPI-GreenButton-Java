-- OpenESPI Authorization Server Database Schema
-- Spring Authorization Server 1.3+ Required Tables
-- MySQL Implementation

-- OAuth2 Authorization Consent
CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ESPI Application Information mapping
CREATE TABLE espi_application_info (
    id bigint NOT NULL AUTO_INCREMENT,
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
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_id (client_id),
    CONSTRAINT fk_client_id FOREIGN KEY (client_id) REFERENCES oauth2_registered_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create indexes for performance
CREATE INDEX idx_oauth2_authorization_client_principal ON oauth2_authorization (registered_client_id, principal_name);
CREATE INDEX idx_oauth2_authorization_code ON oauth2_authorization (authorization_code_value(255));
CREATE INDEX idx_oauth2_authorization_access_token ON oauth2_authorization (access_token_value(255));
CREATE INDEX idx_oauth2_authorization_refresh_token ON oauth2_authorization (refresh_token_value(255));
CREATE INDEX idx_oauth2_registered_client_id ON oauth2_registered_client (client_id);
CREATE INDEX idx_espi_application_client_id ON espi_application_info (client_id);