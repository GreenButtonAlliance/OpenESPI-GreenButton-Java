-- OIDC UserInfo Support for PostgreSQL
-- Adds tables and functions for storing user information for OIDC UserInfo endpoint

-- Create table for storing user information
CREATE TABLE oauth2_user_info (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(200) NOT NULL UNIQUE,
    first_name VARCHAR(100) DEFAULT NULL,
    last_name VARCHAR(100) DEFAULT NULL,
    email VARCHAR(300) DEFAULT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    locale VARCHAR(10) DEFAULT NULL,
    zone_info VARCHAR(50) DEFAULT NULL,
    customer_id VARCHAR(100) DEFAULT NULL,
    customer_type VARCHAR(50) DEFAULT NULL,
    account_number VARCHAR(100) DEFAULT NULL,
    service_territory VARCHAR(200) DEFAULT NULL,
    phone_number VARCHAR(50) DEFAULT NULL,
    phone_number_verified BOOLEAN DEFAULT FALSE,
    address JSONB DEFAULT NULL,
    birthdate DATE DEFAULT NULL,
    gender VARCHAR(20) DEFAULT NULL,
    picture_url VARCHAR(500) DEFAULT NULL,
    website_url VARCHAR(500) DEFAULT NULL,
    profile_url VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Add comments
COMMENT ON TABLE oauth2_user_info IS 'User information for OIDC UserInfo endpoint';
COMMENT ON COLUMN oauth2_user_info.username IS 'Unique username (subject identifier)';
COMMENT ON COLUMN oauth2_user_info.first_name IS 'Given name';
COMMENT ON COLUMN oauth2_user_info.last_name IS 'Family name';
COMMENT ON COLUMN oauth2_user_info.email IS 'Email address';
COMMENT ON COLUMN oauth2_user_info.email_verified IS 'Whether email has been verified';
COMMENT ON COLUMN oauth2_user_info.locale IS 'Locale preference (e.g., en-US)';
COMMENT ON COLUMN oauth2_user_info.zone_info IS 'Time zone (e.g., America/Los_Angeles)';
COMMENT ON COLUMN oauth2_user_info.customer_id IS 'DataCustodian customer identifier';
COMMENT ON COLUMN oauth2_user_info.customer_type IS 'Type of customer (RESIDENTIAL, COMMERCIAL, etc.)';
COMMENT ON COLUMN oauth2_user_info.account_number IS 'Utility account number';
COMMENT ON COLUMN oauth2_user_info.service_territory IS 'Geographic service territory';
COMMENT ON COLUMN oauth2_user_info.address IS 'JSON object containing address information';

-- Create indexes
CREATE INDEX idx_oauth2_user_info_email ON oauth2_user_info (email);
CREATE INDEX idx_oauth2_user_info_customer_id ON oauth2_user_info (customer_id);
CREATE INDEX idx_oauth2_user_info_updated_at ON oauth2_user_info (updated_at);

-- Create trigger to update updated_at timestamp
CREATE TRIGGER update_oauth2_user_info_updated_at 
    BEFORE UPDATE ON oauth2_user_info 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Create table for storing OIDC claims mapping
CREATE TABLE oidc_claims_mapping (
    id BIGSERIAL PRIMARY KEY,
    scope VARCHAR(100) NOT NULL,
    claim_name VARCHAR(100) NOT NULL,
    claim_description VARCHAR(500) DEFAULT NULL,
    essential BOOLEAN DEFAULT FALSE,
    data_source VARCHAR(50) DEFAULT 'user_info',
    espi_specific BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (scope, claim_name)
);

-- Add comments for claims mapping
COMMENT ON TABLE oidc_claims_mapping IS 'Configuration for OIDC claims returned by UserInfo endpoint';
COMMENT ON COLUMN oidc_claims_mapping.scope IS 'OAuth2 scope that enables this claim';
COMMENT ON COLUMN oidc_claims_mapping.claim_name IS 'Name of the claim in UserInfo response';
COMMENT ON COLUMN oidc_claims_mapping.essential IS 'Whether this claim is essential';
COMMENT ON COLUMN oidc_claims_mapping.data_source IS 'Source of claim data (user_info, datacustodian, etc.)';
COMMENT ON COLUMN oidc_claims_mapping.espi_specific IS 'Whether this is an ESPI-specific claim';

-- Create indexes for claims mapping
CREATE INDEX idx_oidc_claims_scope ON oidc_claims_mapping (scope);
CREATE INDEX idx_oidc_claims_enabled ON oidc_claims_mapping (enabled);
CREATE INDEX idx_oidc_claims_espi ON oidc_claims_mapping (espi_specific);

-- Insert standard OIDC claims mapping
INSERT INTO oidc_claims_mapping (scope, claim_name, claim_description, essential, data_source, espi_specific) VALUES
-- Profile scope claims
('profile', 'name', 'Full name', false, 'user_info', false),
('profile', 'given_name', 'Given name', false, 'user_info', false),
('profile', 'family_name', 'Family name', false, 'user_info', false),
('profile', 'preferred_username', 'Preferred username', false, 'user_info', false),
('profile', 'locale', 'Locale preference', false, 'user_info', false),
('profile', 'zoneinfo', 'Time zone', false, 'user_info', false),
('profile', 'updated_at', 'Last update time', false, 'user_info', false),
('profile', 'picture', 'Profile picture URL', false, 'user_info', false),
('profile', 'website', 'Website URL', false, 'user_info', false),
('profile', 'profile', 'Profile page URL', false, 'user_info', false),
('profile', 'birthdate', 'Birth date', false, 'user_info', false),
('profile', 'gender', 'Gender', false, 'user_info', false),

-- Email scope claims
('email', 'email', 'Email address', false, 'user_info', false),
('email', 'email_verified', 'Email verification status', false, 'user_info', false),

-- Phone scope claims
('phone', 'phone_number', 'Phone number', false, 'user_info', false),
('phone', 'phone_number_verified', 'Phone verification status', false, 'user_info', false),

-- Address scope claims
('address', 'address', 'Physical mailing address', false, 'user_info', false),

-- ESPI-specific claims
('FB=4_5_15', 'customer_id', 'DataCustodian customer identifier', true, 'datacustodian', true),
('FB=4_5_15', 'customer_type', 'Type of customer', false, 'datacustodian', true),
('FB=4_5_15', 'account_number', 'Utility account number', false, 'datacustodian', true),
('FB=4_5_15', 'service_territory', 'Geographic service territory', false, 'datacustodian', true),
('FB=4_5_15', 'espi_scopes', 'ESPI-specific scopes granted', true, 'computed', true),
('FB=4_5_15', 'authorized_usage_points', 'Authorized usage point IDs', true, 'datacustodian', true),
('FB=4_5_15', 'usage_point_details', 'Detailed usage point information', false, 'datacustodian', true),
('FB=4_5_15', 'datacustodian_grant_id', 'DataCustodian grant identifier', false, 'integration', true),

-- Green Button Alliance extension claims
('openid', 'gba_version', 'Green Button Alliance version', false, 'static', true),
('openid', 'espi_version', 'NAESB ESPI version', false, 'static', true),
('openid', 'data_rights', 'Granted data access rights', false, 'computed', true);

-- Create function to get claims for scope
CREATE OR REPLACE FUNCTION get_claims_for_scope(scope_name VARCHAR(100))
RETURNS TABLE (
    claim_name VARCHAR(100),
    essential BOOLEAN,
    data_source VARCHAR(50),
    espi_specific BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        ocm.claim_name,
        ocm.essential,
        ocm.data_source,
        ocm.espi_specific
    FROM oidc_claims_mapping ocm
    WHERE ocm.scope = scope_name 
    AND ocm.enabled = TRUE
    ORDER BY ocm.essential DESC, ocm.claim_name;
END;
$$ LANGUAGE plpgsql;

-- Create function to validate ESPI scopes
CREATE OR REPLACE FUNCTION validate_espi_scope(scope_value TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Validate ESPI FB scope format: FB=x_y_z;IntervalDuration=nnn;BlockDuration=xxx;HistoryLength=nn
    RETURN scope_value ~ '^FB=[0-9]+_[0-9]+_[0-9]+;IntervalDuration=[0-9]+;BlockDuration=(monthly|daily|hourly);HistoryLength=[0-9]+$';
END;
$$ LANGUAGE plpgsql;

-- Create function to extract ESPI scope parameters
CREATE OR REPLACE FUNCTION extract_espi_scope_params(scope_value TEXT)
RETURNS JSONB AS $$
DECLARE
    result JSONB := '{}'::jsonb;
    fb_version TEXT;
    interval_duration INT;
    block_duration TEXT;
    history_length INT;
BEGIN
    IF NOT validate_espi_scope(scope_value) THEN
        RETURN result;
    END IF;
    
    -- Extract FB version (e.g., "4_5_15" from "FB=4_5_15")
    fb_version := substring(scope_value from 'FB=([0-9]+_[0-9]+_[0-9]+)');
    
    -- Extract interval duration
    interval_duration := substring(scope_value from 'IntervalDuration=([0-9]+)')::INT;
    
    -- Extract block duration
    block_duration := substring(scope_value from 'BlockDuration=(monthly|daily|hourly)');
    
    -- Extract history length
    history_length := substring(scope_value from 'HistoryLength=([0-9]+)')::INT;
    
    result := jsonb_build_object(
        'fb_version', fb_version,
        'interval_duration', interval_duration,
        'block_duration', block_duration,
        'history_length', history_length
    );
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Create view for user information with computed fields
CREATE VIEW v_oauth2_user_info_complete AS
SELECT 
    oui.*,
    CASE 
        WHEN oui.first_name IS NOT NULL AND oui.last_name IS NOT NULL 
        THEN oui.first_name || ' ' || oui.last_name
        WHEN oui.first_name IS NOT NULL 
        THEN oui.first_name
        WHEN oui.last_name IS NOT NULL 
        THEN oui.last_name
        ELSE oui.username
    END as full_name,
    CASE 
        WHEN oui.customer_id IS NOT NULL THEN 'customer'
        ELSE 'user'
    END as user_type,
    dim.grant_id as datacustodian_grant_id,
    dim.usage_point_ids as authorized_usage_points,
    dim.integration_status as datacustodian_status
FROM oauth2_user_info oui
LEFT JOIN datacustodian_integration_mapping dim ON oui.customer_id = dim.customer_id;

-- Insert sample user data for testing
INSERT INTO oauth2_user_info 
(username, first_name, last_name, email, email_verified, locale, zone_info,
 customer_id, customer_type, account_number, service_territory) 
VALUES 
('customer@example.com', 'John', 'Doe', 'customer@example.com', true, 'en-US', 'America/Los_Angeles',
 'customer-123', 'RESIDENTIAL', 'ACC-789456', 'Northern California'),
 
('admin@example.com', 'Admin', 'User', 'admin@example.com', true, 'en-US', 'America/New_York',
 NULL, NULL, NULL, NULL),
 
('mobile.user@example.com', 'Jane', 'Smith', 'mobile.user@example.com', true, 'en-US', 'America/Chicago',
 'customer-456', 'RESIDENTIAL', 'ACC-123789', 'Illinois');

-- Add UserInfo audit log entry
INSERT INTO oauth2_audit_log (event_type, client_id, principal_name, success, additional_data)
VALUES ('schema_upgrade', 'system', 'migration', TRUE, 
        jsonb_build_object('version', 'V5_0_0', 'feature', 'oidc_userinfo', 
                          'description', 'Added OIDC UserInfo endpoint support', 
                          'migration_timestamp', NOW()));

-- Create function to cleanup old user info cache
CREATE OR REPLACE FUNCTION cleanup_user_info_cache()
RETURNS BIGINT AS $$
DECLARE
    updated_count BIGINT;
BEGIN
    -- Update users that haven't been updated in 24 hours from DataCustodian
    -- This is a placeholder for future cache invalidation logic
    UPDATE oauth2_user_info 
    SET updated_at = CURRENT_TIMESTAMP 
    WHERE customer_id IS NOT NULL 
    AND updated_at < CURRENT_TIMESTAMP - INTERVAL '24 hours';
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;